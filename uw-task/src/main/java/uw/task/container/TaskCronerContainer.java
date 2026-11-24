package uw.task.container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronExpression;
import uw.common.util.ExceptionUtils;
import uw.common.util.SystemClock;
import uw.task.TaskCroner;
import uw.task.TaskData;
import uw.task.TaskListenerManager;
import uw.task.api.TaskApiClient;
import uw.task.conf.TaskMetaInfoManager;
import uw.task.conf.UwTaskProperties;
import uw.task.constant.TaskConstants;
import uw.task.entity.TaskCronerConfig;
import uw.task.entity.TaskCronerLog;
import uw.task.listener.TaskCronerListener;
import uw.task.util.TaskLeaderElector;
import uw.task.util.TaskSequence;
import uw.task.util.TaskStatsService;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 定时任务容器：虚拟线程 + {@link CronExpression} 一次性自续期调度 {@link TaskCroner}。
 *
 * <p>每个 TaskCronerConfig 对应一个 {@link CronerSchedule}（active 门闩 + 当前 future），缓存在 {@link #cronerTasks} 中。
 * 调度方式为自续期：每轮按 cron 算出下次触发时间，提交一个"睡到触发点"的虚拟线程；任务体跑完后在 active 门闩通过时
 * 再次计算并续期下一轮。自续期保证每个配置同时最多一个在途执行（全局并发=活跃配置数，天然有界），
 * 因此 VT 执行器无需并发上限——挂起等待在虚拟线程上零成本。不使用 Spring TaskScheduler 的 Trigger 回调通道——
 * cancel 后回调被跳过的契约坑随之消失，stats/日志只在一个地方（任务体内）落盘。</p>
 *
 * <p>{@code RUN_TYPE_SINGLETON} 的任务仅在 Leader 实例上执行（通过 {@link TaskLeaderElector#isLeader(String)} 判定）。</p>
 *
 * @author axeon
 */
public class TaskCronerContainer {

    /**
     * 日志器。
     */
    private static final Logger log = LoggerFactory.getLogger(TaskCronerContainer.class);
    /**
     * cronerTask任务索引。
     */
    private final Map<String, CronerSchedule> cronerTasks = new ConcurrentHashMap<>();

    /**
     * croner 执行器（每 croner 一个虚拟线程，睡到触发点再执行；无需并发上限，见类注释）。
     */
    private final ExecutorService executorService;
    /**
     * 任务配置
     */
    private final UwTaskProperties taskProperties;
    /**
     * 服务端API
     */
    private final TaskApiClient taskApiClient;
    /**
     * 任务元信息管理器
     */
    private final TaskMetaInfoManager taskMetaInfoManager;
    /**
     * 监听管理器。
     */
    private final TaskListenerManager taskListenerManager;

    /**
     * 构造定时任务容器并初始化虚拟线程执行器。
     *
     * @param taskProperties       任务全局配置
     * @param taskApiClient        服务端任务 API 客户端
     * @param taskMetaInfoManager  任务元信息管理器（提供 getCronerConfigKey 计算配置键）
     * @param taskListenerManager  任务监听管理器（pre/postExecute 回调）
     */
    public TaskCronerContainer(UwTaskProperties taskProperties, TaskApiClient taskApiClient, TaskMetaInfoManager taskMetaInfoManager,
                               TaskListenerManager taskListenerManager) {
        this.taskProperties = taskProperties;
        this.taskApiClient = taskApiClient;
        this.taskMetaInfoManager = taskMetaInfoManager;
        this.taskListenerManager = taskListenerManager;
        executorService = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("uw-task-croner-", 0).factory());
        log.info("TaskCronerContainer 已启动（虚拟线程调度）....");
    }

    /**
     * 按 croner 配置注册并调度一个定时任务。
     * <p>若该配置 id 已有调度则先停旧调度（active 门闩关闭，在途跑完后续期被拒，future cancel(false)）；
     * 配置无效（null/线程数异常/状态禁用/cron 不可解析）时跳过并返回 false。
     * 每轮执行按 runType 判定是否需 Leader 身份（SINGLETON），执行结果与统计在任务体内统一落库。</p>
     *
     * @param taskCroner       任务实例
     * @param taskCronerConfig 任务配置
     * @return 成功注册返回 true，配置无效或状态禁用返回 false
     */
    public boolean configureTask(TaskCroner taskCroner, TaskCronerConfig taskCronerConfig) {

        if (taskCronerConfig == null) {
            log.warn("定时任务配置信息不存在，无法启动CRONER!");
            return false;
        }
        // configKey 索引（taskClass#taskParam$runTarget）：本地降级(id=0)与 center(真实id)同 configKey，覆盖时 stop old + new 不错位
        String configKey = taskMetaInfoManager.getCronerConfigKey(taskCronerConfig);
        //尝试关闭已有任务（先关闩再 cancel(false)：不中断正在执行的 croner，当前跑完、门闩已关不续期——切换不丢在途）
        stop(configKey);
        // 标记删除的，直接返回了。
        if (taskCronerConfig.getState() < 1) {
            log.warn("定时任务状态不符，无法启动！ID:{}, CRONER:{}, CRON:{}.", taskCronerConfig.getId(), taskCronerConfig.getTaskClass(), taskCronerConfig.getTaskCron());
            return false;
        }
        // cron 解析（6 域 Spring cron 语法）：不可解析直接拒绝注册
        CronExpression cronExpression;
        try {
            cronExpression = CronExpression.parse(taskCronerConfig.getTaskCron());
        } catch (IllegalArgumentException e) {
            log.warn("CRON表达式非法，无法启动！ID:{}, CRONER:{}, CRON:{}.", taskCronerConfig.getId(), taskCronerConfig.getTaskClass(), taskCronerConfig.getTaskCron());
            return false;
        }
        CronerSchedule schedule = new CronerSchedule();
        cronerTasks.put(configKey, schedule);
        scheduleNext(taskCroner, taskCronerConfig, cronExpression, configKey, schedule);
        return true;
    }

    /**
     * 自续期核心：按 cron 算出下次触发时间，提交一次性任务；任务体跑完后若 active 门闩仍开则续期下一轮。
     *
     * @param taskCroner       任务实例
     * @param taskCronerConfig 任务配置
     * @param cronExpression   cron 表达式（已解析）
     * @param configKey        配置键（cronerTasks 索引）
     * @param schedule         本配置的调度句柄（active 门闩 + 当前 future）
     */
    private void scheduleNext(TaskCroner taskCroner, TaskCronerConfig taskCronerConfig, CronExpression cronExpression,
                              String configKey, CronerSchedule schedule) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime next = cronExpression.next(now);
        if (next == null) {
            // 防御分支：Spring 6 域 cron 无年份域、正常表达式恒有未来触发点；若 cron 语义扩展（年份域）出现 next==null，任务自然终止并摘除索引
            log.warn("CRON表达式无未来触发时间，任务终止！ID:{}, CRONER:{}, CRON:{}.", taskCronerConfig.getId(), taskCronerConfig.getTaskClass(), cronExpression.toString());
            cronerTasks.remove(configKey, schedule);
            return;
        }
        // 本次计划执行时间：直接作为参数传入本轮任务体（scheduleDate，供 timeWait=runDate-scheduleDate 计算）
        Date scheduleDate = Date.from(next.toInstant());
        // delay 用毫秒直减（now 与 next 同为系统时区 ZonedDateTime，差值与时区无关）
        long delayMs = Math.max(0, next.toInstant().toEpochMilli() - now.toInstant().toEpochMilli());
        // synchronized(schedule)：与 stop/suspendAll 的"关闩+cancel"互斥，杜绝孤儿续期（详见锁内注释）。
        // 若无此锁，续期线程检查 active 通过后、future 赋值前 stop 插入——stop cancel 的是旧 future，
        // 新排的一轮成为孤儿（map 已移除、门闩已关却仍会多发一次）。锁内"检查+排期+赋值"原子化后，
        // stop 要么先关闩（续期见 false 直接返回），要么必 cancel 到刚排入的 future。
        synchronized (schedule) {
            if (!schedule.active.get()) {
                return;
            }
            schedule.future = executorService.submit(() -> {
                try {
                    // 虚拟线程睡到触发点（挂起零成本，每 croner 一个 VT，自续期单飞天然有界）
                    TimeUnit.MILLISECONDS.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    // suspendAll/destroyAll 的 cancel(true)：立即停，不执行本轮也不续期
                    return;
                }
                // 睡醒后复核门闩：stop 的 cancel(false) 打不断睡眠，靠此处拦截多发的一轮
                if (!schedule.active.get()) {
                    return;
                }
                try {
                    runCroner(taskCroner, taskCronerConfig, cronExpression, scheduleDate);
                } catch (Throwable e) {
                    // runCroner 内部已 catch runTask/listener 异常，此处兜的是框架自身意外（如 stats 聚合出错）：
                    // 吞掉仅记录，保证续期链不断——否则该配置的调度会静默死亡。
                    log.error("Croner[{}] 单轮执行框架层异常,已吞掉保续期链: {}", taskCronerConfig.getTaskClass(), e.getMessage(), e);
                }
                // 续期：active 门闩通过才排下一轮（stop/suspendAll/destroyAll 已关闩或已被新配置覆盖时不续）
                if (schedule.active.get()) {
                    scheduleNext(taskCroner, taskCronerConfig, cronExpression, configKey, schedule);
                }
            });
        }
    }

    /**
     * 单轮任务体：leader 判定、执行、异常分类、listener 回调、stats 与日志落库。
     *
     * @param taskCroner       任务实例
     * @param taskCronerConfig 任务配置
     * @param cronExpression   cron 表达式（计算日志的 nextDate）
     * @param scheduleDate     本轮计划执行时间
     */
    private void runCroner(TaskCroner taskCroner, TaskCronerConfig taskCronerConfig, CronExpression cronExpression,
                           Date scheduleDate) {
        // 判断全局唯一条件
        if (taskCronerConfig.getRunType() == TaskCronerConfig.RUN_TYPE_SINGLETON && !TaskLeaderElector.isLeader(taskProperties.getTaskProject())) {
            if (log.isDebugEnabled()) {
                log.debug("定时任务全局单实例运行时非Leader身份，直接返回。。。");
            }
            return;
        }
        // 任务逻辑
        TaskCronerLog taskCronerLog = new TaskCronerLog(taskCronerConfig.getLogLevel(), taskCronerConfig.getLogLimitSize());
        try {
            taskCronerLog.setId(TaskSequence.nextId(TaskConstants.KEY_CRONER_LOG));
        } catch (Exception e) {
            // Redis 故障致 nextId 失败：id 保持 0，下方 id>0 守门会跳过日志落库（统计已无条件上报）；
            // 此处 WARN 让运维可见（不抛出，runTask 仍执行，避免 Redis 抖动静默吞掉整轮调度且无任何痕迹）
            log.warn("Croner[{}] nextId 失败,本轮跳过日志: {}", taskCronerConfig.getTaskClass(), e.getMessage());
        }
        taskCronerLog.setTaskClass(taskCronerConfig.getTaskClass());
        taskCronerLog.setTaskParam(taskCronerConfig.getTaskParam());
        taskCronerLog.setTaskCron(taskCronerConfig.getTaskCron());
        taskCronerLog.setRunType(taskCronerConfig.getRunType());
        taskCronerLog.setRunTarget(taskCronerConfig.getRunTarget());
        taskCronerLog.setTaskId(taskCronerConfig.getId());
        taskCronerLog.setRunDate(SystemClock.nowDate());
        // scheduleDate 在 runTask 前即写入 log（参数直传，任务体内可直接读取计划执行时间）
        taskCronerLog.setScheduleDate(scheduleDate);
        // 执行监听器操作
        List<TaskCronerListener> cronerListenerList = taskListenerManager.getCronerListenerList();
        for (TaskCronerListener listener : cronerListenerList) {
            try {
                listener.onPreExecute(taskCronerLog);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }

        String resultData;
        try {
            resultData = taskCroner.runTask(taskCronerLog);
            taskCronerLog.setState(TaskData.STATE_SUCCESS);
        } catch (Throwable e) {
            // 异常分类复用 mapFailureState：TaskDataException→DATA / TaskPartnerException→PARTNER / 其余→PROGRAM（对齐 Runner）
            int failState = TaskStatsService.mapFailureState(e);
            taskCronerLog.setState(failState);
            resultData = ExceptionUtils.exceptionToString(e);
            if (failState == TaskData.STATE_FAIL_PROGRAM) {
                log.error(e.getMessage(), e);
            }
        }
        taskCronerLog.setFinishDate(SystemClock.nowDate());
        taskCronerLog.setResultData(resultData);
        taskCronerLog.setRefObject(null);
        // 执行监听器操作（finishDate/resultData 已就位——listener 契约：onPostExecute 时结果与耗时已可用）
        for (TaskCronerListener listener : cronerListenerList) {
            try {
                listener.onPostExecute(taskCronerLog);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }
        // timeWait<0 为时钟误差，Math.max(0,...) 兜底（对齐 Runner）
        int timeWait = TaskStatsService.toIntClamped(taskCronerLog.getRunDate().getTime() - scheduleDate.getTime());
        int timeRun = TaskStatsService.toIntClamped(taskCronerLog.getFinishDate().getTime() - taskCronerLog.getRunDate().getTime());
        int[] failCounters = TaskStatsService.toFailCounters(taskCronerLog.getState());
        TaskStatsService.updateCronerStats(taskCronerConfig.getId(), 1, failCounters[0], failCounters[1], failCounters[2], failCounters[3], timeWait, timeRun);
        // 日志落库：nextDate 基于执行完成时刻重算下次触发时间。
        if (taskCronerLog.getId() > 0 && taskCronerLog.getRunDate() != null && taskCronerLog.getFinishDate() != null) {
            try {
                ZonedDateTime nextExec = cronExpression.next(ZonedDateTime.now());
                if (nextExec != null) {
                    taskCronerLog.setNextDate(Date.from(nextExec.toInstant()));
                    if (log.isDebugEnabled()) {
                        log.debug("正在调度定时任务ID:[{}], CRONER:[{}], CRON:[{}], 下次执行时间:[{}].", taskCronerConfig.getId(), taskCronerConfig.getTaskClass(), cronExpression.toString(), nextExec.toString());
                    }
                }
            } catch (Exception ignore) {
                // cron 计算异常（极少），nextDate 留空不影响日志主体落盘
            }
            if (taskCronerConfig.getLogLevel() > TaskCronerConfig.TASK_LOG_TYPE_NONE) {
                taskApiClient.sendTaskCronerLog(taskCronerConfig.getId(), taskCronerLog);
            }
        }
    }

    /**
     * 停止一个定时任务：先关闭 active 门闩（在途跑完后不续期），再 cancel(false) 不中断正在执行的任务
     * （配置覆盖/降级切换让在途跑完，不丢），仅取消未来调度，并从 {@link #cronerTasks} map 中移除。
     * <p>注意：主机级停机用 {@link #suspendAll()}（cancel(true) 立即停），与本方法 cancel(false) 区分。</p>
     *
     * @param configKey 配置键（见 {@link TaskMetaInfoManager#getCronerConfigKey(TaskCronerConfig)}）
     * @return 成功取消返回 true；map 中无对应调度返回 false
     */
    public boolean stop(String configKey) {
        CronerSchedule schedule = cronerTasks.get(configKey);
        if (schedule == null) {
            return false;
        }
        // synchronized(schedule)：与 scheduleNext 的"检查+排期+赋值"互斥（见 scheduleNext 注释），杜绝孤儿续期
        synchronized (schedule) {
            // 先关闩再 cancel(false)：不中断正在执行的 croner，当前跑完、门闩已关不续期
            schedule.active.set(false);
            // cancel(false)：不打断睡着的/在跑的 croner（配置覆盖/降级切换时让在途跑完；睡醒后门闩拦截多发）
            Future<?> future = schedule.future;
            if (future != null) {
                future.cancel(false);
            }
        }
        cronerTasks.remove(configKey);
        return true;
    }

    /**
     * 暂停所有定时任务（禁用，保池）：关闩 + cancel future + 清空 cronerTasks，不关闭调度线程池。
     * <p>用于主机被禁用(state=0)时停任务却保留 executorService，以便解除禁用后重新调度。</p>
     */
    public void suspendAll() {
        log.info("所有TaskCroner已暂停....");
        for (Entry<String, CronerSchedule> kv : this.cronerTasks.entrySet()) {
            CronerSchedule schedule = kv.getValue();
            // synchronized(schedule)：与 scheduleNext 的"检查+排期+赋值"互斥（见 scheduleNext 注释），杜绝孤儿续期
            synchronized (schedule) {
                schedule.active.set(false);
                Future<?> future = schedule.future;
                if (future != null) {
                    future.cancel(true);
                }
            }
        }
        cronerTasks.clear();
    }

    /**
     * 关闭所有定时任务并关闭 VT 执行器（应用关闭时调用）。
     */
    public void shutdown() {
        log.info("所有TaskCroner已销毁....");
        suspendAll();
        executorService.shutdownNow();
    }

    /**
     * 单个 croner 配置的调度句柄：active 门闩控制自续期，future 为当前一次性任务。
     */
    private static final class CronerSchedule {

        /**
         * 数据句柄（随调度创建，禁止外部实例化）。
         */
        private CronerSchedule() {
        }

        /**
         * 自续期门闩：stop/suspendAll 关闭后，在途任务跑完不再排下一轮。
         */
        final AtomicBoolean active = new AtomicBoolean(true);
        /**
         * 当前待触发的虚拟线程任务（volatile：scheduleNext 在续期线程写入，stop/suspendAll 可能并发读）。
         */
        volatile Future<?> future;
    }
}
