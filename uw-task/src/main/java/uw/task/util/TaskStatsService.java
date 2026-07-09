package uw.task.util;

import uw.task.TaskData;
import uw.task.entity.TaskDelayerStats;
import uw.task.entity.TaskCronerStats;
import uw.task.entity.TaskRunnerStats;
import uw.task.exception.TaskDataException;
import uw.task.exception.TaskPartnerException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务运行统计聚合服务。
 *
 * <p>按 taskId 聚合 croner / runner / delayer 的执行计数与耗时，供主机状态报告周期性采集上报。</p>
 *
 * <h3>并发模型</h3>
 * 单读多写：{@code updateXxxStats} 由多个任务执行线程并发调用，{@code getXxxStats} 仅由主机状态上报
 * 的单一调度线程（每 180 秒一次）调用。
 * <ul>
 *   <li><b>写（update）</b>：{@link ConcurrentHashMap#compute} 保证同一 taskId 的累加串行化，
 *       叠加 {@link TaskCronerStats}/{@link TaskRunnerStats} 内部 {@code AtomicInteger} 字段，
 *       多线程累加无需外部锁即可保证原子性与可见性。</li>
 *   <li><b>读（get）</b>：{@code new ArrayList<>(map.values())} 拷贝弱一致快照后 {@code clear()}，
 *       两步非原子。<b>已知并接受的漂移</b>：若快照遍历完成到 clear 之间恰好有写线程写入新样本，
 *       该样本既未被本次快照采集、又被 clear 清除，会丢失（少上报一次单次执行计数）。
 *       监控场景下此概率极低且对曲线/告警无实质影响，故不引入外部锁——
 *       避免写端（高频任务执行路径）被全局锁串行化。</li>
 * </ul>
 * <p><b>复用约束</b>：本服务面向监控聚合，接受样本丢失。若未来复用到不允许丢计数的场景（对账、计费等），
 * 需改为加锁或"读时原子转移"语义。</p>
 */
public class TaskStatsService {

    /**
     * croner数据存储map。
     */
    private static final ConcurrentHashMap<Long, TaskCronerStats> CRONER_STATS_MAP = new ConcurrentHashMap<>();

    /**
     * runner数据存储map。
     */
    private static final ConcurrentHashMap<Long, TaskRunnerStats> RUNNER_STATS_MAP = new ConcurrentHashMap<>();

    /**
     * delayer数据存储map。
     */
    private static final ConcurrentHashMap<Long, TaskDelayerStats> DELAYER_STATS_MAP = new ConcurrentHashMap<>();


    /**
     * 更新 croner 统计（聚合到内存 map，reportHostInfo 上报时取快照清空）。
     *
     * @param taskId         任务配置 id
     * @param numAll         执行总计数
     * @param numFailProgram 程序失败计数
     * @param numFailConfig  配置失败计数
     * @param numFailData    数据失败计数
     * @param numFailPartner 合作方失败计数
     * @param timeWait       等待耗时（毫秒）
     * @param timeRun        运行耗时（毫秒）
     */
    public static void updateCronerStats(long taskId, int numAll, int numFailProgram, int numFailConfig, int numFailData, int numFailPartner, int timeWait, int timeRun) {
        CRONER_STATS_MAP.compute(taskId, (k, v) -> {
            if (v != null) {
                return v.addMetrics(numAll, numFailProgram, numFailConfig, numFailData, numFailPartner, timeWait, timeRun);
            } else {
                return new TaskCronerStats(k).addMetrics(numAll, numFailProgram, numFailConfig, numFailData, numFailPartner, timeWait, timeRun);
            }
        });
    }

    /**
     * 获取 Croner 统计快照列表，同时清空 map。
     *
     * @return 快照列表
     */
    public static List<TaskCronerStats> getCronerStats() {
        List<TaskCronerStats> list = new ArrayList<>(CRONER_STATS_MAP.values());
        CRONER_STATS_MAP.clear();
        return list;
    }


    /**
     * 更新 runner 统计（聚合到内存 map，reportHostInfo 上报时取快照清空）。
     *
     * @param taskId         任务配置 id
     * @param numAll         执行总计数
     * @param numFailProgram 程序失败计数
     * @param numFailConfig  配置失败计数
     * @param numFailData    数据失败计数
     * @param numFailPartner 合作方失败计数
     * @param timeWaitQueue  队列等待耗时（毫秒）
     * @param timeWaitDelay  限速等待耗时（毫秒）
     * @param timeRun        运行耗时（毫秒）
     */
    public static void updateRunnerStats(long taskId, int numAll, int numFailProgram, int numFailConfig, int numFailData, int numFailPartner, int timeWaitQueue, int timeWaitDelay, int timeRun) {
        RUNNER_STATS_MAP.compute(taskId, (k, v) -> {
            if (v != null) {
                return v.addMetrics(numAll, numFailProgram, numFailConfig, numFailData, numFailPartner, timeWaitQueue, timeWaitDelay, timeRun);
            } else {
                return new TaskRunnerStats(k).addMetrics(numAll, numFailProgram, numFailConfig, numFailData, numFailPartner, timeWaitQueue, timeWaitDelay, timeRun);
            }
        });
    }

    /**
     * 获取 Runner 统计快照列表，同时清空 map。
     *
     * @return 快照列表
     */
    public static List<TaskRunnerStats> getRunnerStats() {
        List<TaskRunnerStats> list = new ArrayList<>(RUNNER_STATS_MAP.values());
        RUNNER_STATS_MAP.clear();
        return list;
    }


    /**
     * 更新DelayerMetrics。
     *
     * @param taskId          任务配置 id
     * @param numAll          执行总计数
     * @param numFailProgram  程序失败计数
     * @param numFailConfig   配置失败计数（如连续限速超限放弃）
     * @param numFailData     数据失败计数
     * @param numFailPartner  对方失败计数
     * @param timeWaitDelay   实际延迟等待耗时（runAt→consumeDate，毫秒）
     * @param timeRun         运行耗时（毫秒）
     */
    public static void updateDelayerStats(long taskId, int numAll, int numFailProgram, int numFailConfig, int numFailData, int numFailPartner, int timeWaitDelay, int timeRun) {
        DELAYER_STATS_MAP.compute(taskId, (k, v) -> {
            if (v != null) {
                return v.addMetrics(numAll, numFailProgram, numFailConfig, numFailData, numFailPartner, timeWaitDelay, timeRun);
            } else {
                return new TaskDelayerStats(k).addMetrics(numAll, numFailProgram, numFailConfig, numFailData, numFailPartner, timeWaitDelay, timeRun);
            }
        });
    }

    /**
     * 获取DelayerStats列表，同时清空map。
     *
     * @return 快照列表
     */
    public static List<TaskDelayerStats> getDelayerStats() {
        List<TaskDelayerStats> list = new ArrayList<>(DELAYER_STATS_MAP.values());
        DELAYER_STATS_MAP.clear();
        return list;
    }

    /**
     * 将任务状态映射为 4 元失败计数数组：[program, config, data, partner]。
     *
     * @param state 任务状态（TaskData.STATE_FAIL_*）
     * @return 长度 4 的 int 数组，对应位置为 1（命中）或 0
     */
    public static int[] toFailCounters(int state) {
        return new int[]{
                state == TaskData.STATE_FAIL_PROGRAM ? 1 : 0,
                state == TaskData.STATE_FAIL_CONFIG ? 1 : 0,
                state == TaskData.STATE_FAIL_DATA ? 1 : 0,
                state == TaskData.STATE_FAIL_PARTNER ? 1 : 0
        };
    }

    /**
     * 将执行异常映射为任务失败状态。
     * <p>统一 TaskRunner / TaskCroner / TaskDelayer 三处 catch 链的异常分类逻辑：
     * {@link TaskDataException}→DATA、{@link TaskPartnerException}→PARTNER、其余→PROGRAM。
     * 调用方据此 setState，并用 {@code ExceptionUtils.exceptionToString(e)} 写入 errorInfo / resultData。</p>
     *
     * @param e 任务执行抛出的异常
     * @return 对应的 STATE_FAIL_* 常量
     */
    public static int mapFailureState(Throwable e) {
        if (e instanceof TaskDataException) {
            return TaskData.STATE_FAIL_DATA;
        } else if (e instanceof TaskPartnerException) {
            return TaskData.STATE_FAIL_PARTNER;
        } else {
            return TaskData.STATE_FAIL_PROGRAM;
        }
    }

    /**
     * 将 long 安全转为 int：负值截 0，超 Integer.MAX_VALUE 截 MAX_VALUE。
     *
     * @param value 原始 long 值
     * @return 安全的 int 值
     */
    public static int toIntClamped(long value) {
        return Math.clamp(value, 0, Integer.MAX_VALUE);
    }

}
