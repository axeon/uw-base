package uw.task.container;

import java.util.Set;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import uw.common.util.KryoUtils;
import uw.common.util.SystemClock;
import uw.task.TaskData;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 延时任务 Redis zset 搬运器（091/低版本环境的延时承载通道，与 MQ 原生延迟自动二选一）。
 *
 * <p>定位（261120 用户定案"一切全自动化"）：延时任务与队列任务统一为 TaskRunner 一种类型，
 * {@code TaskData.taskDelay > 0} 即延时任务。投递路由在 {@code TaskFactory.sendToQueue} 内自动判定：
 * 1.0 + RabbitMQ 4.4+ 且目标队列为 quorum → MQ 消息级原生延迟（本容器不参与）；
 * 否则 → 本容器：submit 写 zset（score=到期时间），mover 到期原子 pop 后转投 MQ 目标队列
 * （delay 清零，由对应 TaskRunner 消费执行——执行语义统一归消费端）。</p>
 *
 * <p>本容器只做<b>时间搬运</b>，不做执行——无限速/重试/日志/监听器（这些由 TaskRunner 消费端统一承担）。
 * 执行模型为 <b>Leader 单例</b>（261120 定案）：仅 Leader 实例运行 mover 线程（与 croner SINGLETON 共用
 * TaskLeaderElector 选举，60s 检查周期里同步启停），消除多实例并发竞争——每次批量 pop 不与其他实例交错，
 * Redis 扫描频次降为单实例量级。选主切换的短暂重叠窗口由 Lua 原子 pop 兜底（不会重复搬运），
 * Leader 空窗期内任务滞留 zset（不丢，新 Leader 接续搬运）。MQ 故障写回保留 ranTimes 自然累计，
 * 上限 {@value #MAX_TRANSFER_RETRY} 次后放弃并记 error。</p>
 *
 * @author axeon
 */
public class TaskDelayTransferContainer {

    private static final Logger log = LoggerFactory.getLogger(TaskDelayTransferContainer.class);

    /**
     * zset key（project 维度单一分片：所有延时任务共一个 zset，队列名编码在 member 前缀，
     * 搬运 O(1) 次 EVALSHA/轮——多队列分片在大规模队列数下轮询成本线性膨胀，单 zset 消除之）。
     */
    private final String zsetKey;


    /**
     * Redis 操作模板（String key + byte[] value，Kryo 序列化 TaskData）。
     */
    private final RedisTemplate<String, byte[]> redisTemplate;

    /**
     * 投递中心（到期转投 MQ 用其 sendImmediate 即时入口）。与 SendCenter 互指，装配期经
     * {@link #bindSendCenter(TaskSendCenter)} 单次回填破构造环（单线程装配，无并发可见性问题）。
     */
    private TaskSendCenter taskSendCenter;

    /**
     * Lua 原子 pop 脚本：ZRANGEBYSCORE 取出到期成员并 ZREM 删除，避免多实例重复取与查删竞态。
     * KEYS[1]=zset key，ARGV[1]=当前时间戳(now)，ARGV[2]=批量数。
     */
    @SuppressWarnings("unchecked")
    private static final DefaultRedisScript<List<byte[]>> POP_SCRIPT = new DefaultRedisScript<>("local members = redis.call('ZRANGEBYSCORE', KEYS[1], 0, ARGV[1], 'LIMIT', 0, ARGV[2]) " + "if #members > 0 then for i=1,#members do redis.call('ZREM', KEYS[1], members[i]) end end " + "return members", (Class<List<byte[]>>) (Class<?>) List.class);

    /**
     * mover 单次批量。
     */
    private static final int BATCH = 100;

    /**
     * 空转轮询间隔（毫秒）。
     */
    private static final long IDLE_INTERVAL_MS = 1000;

    /**
     * 搬运重试上限（ranTimes 自然累计；MQ 持续不可用时防止无限写回循环）。
     */
    private static final int MAX_TRANSFER_RETRY = 20;

    /**
     * mover 运行标记。
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * mover 线程。
     */
    private Thread moverThread;

    /**
     * 构造。
     *
     * @param redisTemplate     uw-task 私有 Redis 模板（String key + byte[] value）
     * @param taskSendCenter    任务投递中心（sendImmediate 即时投递）
     */
    public TaskDelayTransferContainer(RedisTemplate<String, byte[]> redisTemplate, TaskSendCenter taskSendCenter) {
        this(redisTemplate, null, taskSendCenter);
    }

    /**
     * @param redisTemplate uw-task 私有 Redis 模板（String key + byte[] value）
     * @param taskProject   任务项目名（zset 注册表/分片的 project 维度隔离键，null 走 _default 域）
     * @param taskSendCenter 任务投递中心（sendImmediate 即时投递）
     */
    public TaskDelayTransferContainer(RedisTemplate<String, byte[]> redisTemplate, String taskProject, TaskSendCenter taskSendCenter) {
        this.redisTemplate = redisTemplate;
        this.taskSendCenter = taskSendCenter;
        String ns = "uw-task-delay:" + (taskProject == null || taskProject.isBlank() ? "_default" : taskProject);
        this.zsetKey = ns;
    }

    /**
     * 装配期回填投递中心（Transfer↔SendCenter 互指破环：先建 Transfer（SendCenter 可为 null），
     * 再建 SendCenter 后单次回填；运行期不可再改）。
     *
     * @param taskSendCenter 任务投递中心
     */
    public void bindSendCenter(TaskSendCenter taskSendCenter) {
        this.taskSendCenter = taskSendCenter;
    }

    /**
     * Leader 身份变化时启停 mover（由 60s 选举检查任务调用；当选启动，落选停止）。
     * <p>幂等：重复同向调用无副作用。落选时 mover 完成当前批次后退出（interrupt 打断空闲 sleep），
     * 在途/未搬运任务留 zset，由新 Leader 接续。</p>
     *
     * @param leader 当前实例是否 Leader
     */
    public void setLeader(boolean leader) {
        if (leader) {
            if (running.compareAndSet(false, true)) {
                moverThread = new Thread(this::moveLoop, "uw-task-delay-mover");
                moverThread.setDaemon(true);
                moverThread.start();
                log.info("TaskDelayTransferContainer: 当选 Leader，mover 已启动");
            }
        } else {
            if (running.compareAndSet(true, false)) {
                if (moverThread != null) {
                    moverThread.interrupt();
                }
                log.info("TaskDelayTransferContainer: 失去 Leader，mover 已停止");
            }
        }
    }

    /**
     * 停止 mover（bean 销毁回调；在途任务留 zset，重启后继续搬运不丢）。
     */
    public void shutdown() {
        running.set(false);
        if (moverThread != null) {
            moverThread.interrupt();
        }
    }

    /**
     * 查询 project 延时积压总量（单 zset ZCARD）。按队列细分走 MQ 侧队列深度。
     *
     * @return project 全部延时任务数；查询失败返回 -1
     */
    public long getBacklogSize() {
        return getTotalBacklog();
    }

    /**
     * 查询 project 延时积压总数（运维观测：整体延时水位）。
     *
     * @return zset 大小；查询失败返回 -1
     */
    public long getTotalBacklog() {
        try {
            Long size = redisTemplate.opsForZSet().size(zsetKey);
            return size == null ? 0 : size;
        } catch (Exception e) {
            log.warn("TaskDelayTransfer: 查询延时积压失败: {}", e.getMessage());
            return -1;
        }
    }

    /**
     * 提交一个延时任务（score=queueDate+taskDelay=到期时间）。
     * <p>member 格式 = queueName + NUL + Kryo(TaskData)：project 单 zset 下队列名随成员携带，POP 后按前缀拆分路由。</p>
     *
     * @param taskData  延时任务（taskClass/taskDelay/queueDate 已由 TaskSendCenter 设置）
     * @param queueName 到期后转投的目标 MQ 队列名
     */
    public void submit(TaskData<?, ?> taskData, String queueName) {
        // member 直接 = Kryo(TaskData)：队列名不编码——TaskData 自带 taskClass/taskTag/runTarget，
        // 搬运时经 TaskSendCenter 全路由重derive（与即时投递同一条路，单一事实源）。
        // queueName 参数保留：与 TaskSendCenter 调用点对齐（其值即 getFitQueue 结果，仅用于校验一致）。
        double score = taskData.getQueueDate().getTime() + taskData.getTaskDelay();
        redisTemplate.opsForZSet().add(zsetKey, KryoUtils.serialize(taskData), score);
    }

    /**
     * mover 主循环：单 zset 一次 Lua 原子 pop 到期任务（批量 ≤{@value BATCH}）→ 按前缀拆分队列 → 转投 MQ（delay 取负标记已搬运）。
     */
    private void moveLoop() {
        while (running.get()) {
            int moved = 0;
            try {
                byte[] nowArg = String.valueOf(SystemClock.now()).getBytes();
                byte[] batchArg = String.valueOf(BATCH).getBytes();
                List<byte[]> members = redisTemplate.execute(POP_SCRIPT, Collections.singletonList(zsetKey), nowArg, batchArg);
                if (members != null && !members.isEmpty()) {
                    for (Object member : members) {
                        byte[] bytes = (byte[]) member;
                        TaskData<?, ?> task;
                        try {
                            task = KryoUtils.deserialize(bytes, TaskData.class);
                        } catch (Exception e) {
                            log.warn("TaskDelayTransfer: 反序列化失败,跳过(len={}): {}", bytes.length, e.toString());
                            continue;
                        }
                        moved += transfer(task);
                    }
                }
                if (moved == 0) {
                    Thread.sleep(IDLE_INTERVAL_MS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Throwable e) {
                if (running.get()) {
                    log.error("TaskDelayTransfer: mover 异常: {}", e.getMessage(), e);
                    try {
                        Thread.sleep(IDLE_INTERVAL_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }

    /**
     * 转投单条任务到目标 MQ 队列（delay 取负标记"已到期搬运"）；失败写回 zset 延迟重试。
     *
     * @return 1=成功转投；0=失败已写回
     */
    private int transfer(TaskData<?, ?> task) {
        // taskDelay 取负标记"已到期搬运"（所有延时判定均为严格 >0，负值安全走即时分支）——
        // 经 taskSendCenter.sendToQueue 全路由（与即时投递同一条路：getFitQueue 由 taskClass 重derive 队列名，
        // 单一事实源，submit/搬运两侧无队列名编码冗余）。
        task.setTaskDelay(-Math.abs(task.getTaskDelay()));
        try {
            taskSendCenter.sendToQueue(task);
            return 1;
        } catch (Exception e) {
            // MQ 不可用：写回 zset 延迟 5s 重试；搬运侧递增 ranTimes（消费端未执行不会累加，此处补计），
            // 超上限放弃防无限循环
            task.setRanTimes(task.getRanTimes() + 1);
            if (task.getRanTimes() <= MAX_TRANSFER_RETRY) {
                task.setQueueDate(SystemClock.nowDate());
                task.setTaskDelay(5000);
                redisTemplate.opsForZSet().add(zsetKey, KryoUtils.serialize(task), SystemClock.now() + 5000);
                log.warn("TaskDelayTransfer: 转投失败写回重试, taskClass={}, ranTimes={}: {}", task.getTaskClass(), task.getRanTimes(), e.getMessage());
            } else {
                log.error("TaskDelayTransfer: 转投重试达上限({})放弃, taskClass={}, id={}", MAX_TRANSFER_RETRY, task.getTaskClass(), task.getId());
            }
            return 0;
        }
    }
}
