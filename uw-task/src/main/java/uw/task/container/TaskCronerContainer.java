package uw.task.container;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import uw.common.util.ExceptionUtils;
import uw.common.util.SystemClock;
import uw.task.TaskCroner;
import uw.task.TaskData;
import uw.task.TaskListenerManager;
import uw.task.api.TaskApiClient;
import uw.task.conf.TaskMetaInfoManager;
import uw.task.conf.TaskProperties;
import uw.task.entity.TaskCronerConfig;
import uw.task.entity.TaskRunnerConfig;
import uw.task.entity.TaskCronerLog;
import uw.task.exception.TaskDataException;
import uw.task.exception.TaskPartnerException;
import uw.task.listener.TaskCronerListener;
import uw.task.util.TaskGlobalLocker;
import uw.task.util.TaskSequenceManager;
import uw.task.util.TaskStatsService;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/**
 * 定时任务容器：基于 Spring {@link org.springframework.scheduling.TaskScheduler} + {@link CronTrigger} 调度 {@link TaskCroner}。
 *
 * <p>每个 TaskCronerConfig 对应一个 {@link ScheduledFuture}，缓存在 {@link #cronerTasks} 中。
 * {@code RUN_TYPE_SINGLETON} 的任务仅在 Leader 实例上执行（通过 {@link TaskGlobalLocker#isLeader()} 判定）。
 * 执行结果、统计、下次计划时间在 TriggerTask 回调中统一落库。</p>
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
    private final Map<String, ScheduledFuture<?>> cronerTasks = new ConcurrentHashMap<>(128);

    /**
     * 任务调度器。
     */
    private final org.springframework.scheduling.TaskScheduler taskScheduler;
    /**
     * 执行器。
     */
    private final ScheduledExecutorService executorService;
    /**
     * 任务配置
     */
    private final TaskProperties taskProperties;
    /**
     * 服务端API
     */
    private final TaskApiClient taskApiClient;
    /**
     * 任务元信息管理器
     */
    private final TaskMetaInfoManager taskMetaInfoManager;
    /**
     * 全局序列发生器。
     */
    private final TaskSequenceManager taskSequenceManager;
    /**
     * 全局任务锁。
     */
    private final TaskGlobalLocker taskGlobalLocker;
    /**
     * 监听管理器。
     */
    private final TaskListenerManager taskListenerManager;

    /**
     * 构造定时任务容器并初始化调度线程池。
     * <p>若 {@code enableRegistry=false}（非后台任务主机），croner 线程数按 1 计以节省资源；
     * 不回写 taskProperties 避免篡改共享配置对象影响其他读取方。</p>
     *
     * @param taskProperties       任务全局配置
     * @param taskApiClient        服务端任务 API 客户端
     * @param taskMetaInfoManager  任务元信息管理器（提供 getCronerConfigKey 计算配置键）
     * @param taskSequenceManager  全局发号器（生成 TaskCronerLog id）
     * @param taskGlobalLocker     全局 Leader 锁（SINGLETON 任务用）
     * @param taskListenerManager  任务监听管理器（pre/postExecute 回调）
     */
    public TaskCronerContainer(TaskProperties taskProperties, TaskApiClient taskApiClient, TaskMetaInfoManager taskMetaInfoManager, TaskSequenceManager taskSequenceManager,
                               TaskGlobalLocker taskGlobalLocker, TaskListenerManager taskListenerManager) {
        this.taskProperties = taskProperties;
        this.taskApiClient = taskApiClient;
        this.taskMetaInfoManager = taskMetaInfoManager;
        this.taskSequenceManager = taskSequenceManager;
        this.taskGlobalLocker = taskGlobalLocker;
        this.taskListenerManager = taskListenerManager;
        // 如果禁用任务注册，则croner线程数按 1 计，节省资源。
        // 注意：不回写 taskProperties，避免篡改共享配置对象影响其他读取方。
        int cronerThreadNum = taskProperties.isEnableRegistry() ? taskProperties.getCronerThreadNum() : 1;
        executorService = Executors.newScheduledThreadPool(cronerThreadNum,
                new ThreadFactoryBuilder().setDaemon(true).setNameFormat("TaskCroner-%d").build());
        log.info("TaskCronerContainer start with [{}] threads...", cronerThreadNum);
        taskScheduler = new ConcurrentTaskScheduler(executorService);
    }

    /**
     * 按 croner 配置注册并调度一个定时任务。
     * <p>若该配置 id 已有调度则先停止旧调度；配置无效（null/线程数异常/状态禁用）时跳过并返回 false。
     * 通过 {@link CronTrigger} 注册到调度器，每次执行时按 runType 判定是否需 Leader 身份（SINGLETON），
     * 执行结果与统计通过闭包变量在 Trigger 回调中落库。</p>
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
        if (this.taskProperties.getCronerThreadNum() < 1) {
            log.warn("CRONER线程数设置异常，无法启动!");
            return false;
        }
        // configKey 索引（taskClass#taskParam$runTarget）：本地降级(id=0)与 center(真实id)同 configKey，覆盖时 stop old + new 不错位
        String configKey = taskMetaInfoManager.getCronerConfigKey(taskCronerConfig);
        //尝试关闭已有任务（cancel(false)：不中断正在执行的 croner，当前跑完、new 接管未来调度——切换不丢在途）
        stop(configKey);
        // 标记删除的，直接返回了。
        if (taskCronerConfig.getState() < 1) {
            log.warn("定时任务状态不符，无法启动！ID:{}, CRONER:{}, CRON:{}.", taskCronerConfig.getId(), taskCronerConfig.getTaskClass(), taskCronerConfig.getTaskCron());
            return false;
        }
        CronTrigger cronTrigger = new CronTrigger(taskCronerConfig.getTaskCron());

        // 任务体(Runnable)与 Trigger.nextExecution 回调在同一次 schedule() 调用内，
        // 共享此闭包变量传递本次执行的 TaskCronerLog，替代原先的 ThreadLocal。
        // 好处：不依赖"调度器在同线程内连续执行任务体与 trigger 回调"这一隐式契约，
        // 且作用域局限在单次调度闭包内，无跨任务/跨线程串扰与残留风险。
        final TaskCronerLog[] logHolder = new TaskCronerLog[1];
        // 本次计划执行时间（由 trigger 回调写入上次计算的 nextExec，供下次 Runnable 体内落盘 stats 时作 scheduleDate）。
        // stats 从 trigger 回调移到 Runnable 体内，避免 cancel 后 Spring ReschedulingRunnable 跳过回调致该轮统计丢失。
        final Date[] scheduleDateHolder = new Date[1];

        ScheduledFuture<?> future = this.taskScheduler.schedule(() -> {
            // 判断全局唯一条件
            if (taskCronerConfig.getRunType() == TaskCronerConfig.RUN_TYPE_SINGLETON && !taskGlobalLocker.isLeader()) {
                if (log.isDebugEnabled()) {
                    log.debug("定时任务全局单实例运行时非Leader身份，直接返回。。。");
                }
                return;
            }
            // 任务逻辑
            TaskCronerLog taskCronerLog = new TaskCronerLog(taskCronerConfig.getLogLevel(), taskCronerConfig.getLogLimitSize());
            logHolder[0] = taskCronerLog;
            try {
                taskCronerLog.setId(taskSequenceManager.nextId("TaskCronerLog"));
            } catch (Exception e) {
                // Redis 故障致 nextId 失败：id 保持 0，trigger 回调的 id>0 守门会跳过日志落库（统计已无条件上报）；
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
                // 异常分类复用 mapFailureState：TaskDataException→DATA / TaskPartnerException→PARTNER / 其余→PROGRAM（对齐 Runner/Delayer）
                int failState = TaskStatsService.mapFailureState(e);
                taskCronerLog.setState(failState);
                resultData = ExceptionUtils.exceptionToString(e);
                if (failState == TaskData.STATE_FAIL_PROGRAM) {
                    log.error(e.getMessage(), e);
                }
            }
            // 执行监听器操作
            for (TaskCronerListener listener : cronerListenerList) {
                try {
                    listener.onPostExecute(taskCronerLog);
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                }
            }
            taskCronerLog.setFinishDate(SystemClock.nowDate());
            taskCronerLog.setResultData(resultData);
            taskCronerLog.setRefObject(null);
            // stats 在 Runnable 体内落盘（不依赖 trigger 回调）：cancel 后 Spring ReschedulingRunnable 的 finally 会跳过 trigger 回调，
            // 若 stats 放回调则该轮统计丢失（监控盲区）。scheduleDate 取闭包传递的上次 nextExec（本次计划时间）。
            Date scheduleDate = scheduleDateHolder[0];
            int timeWait = 0;
            if (scheduleDate != null) {
                taskCronerLog.setScheduleDate(scheduleDate);
                // timeWait<0 为时钟误差，Math.max(0,...) 兜底（对齐 Delayer/Runner）
                timeWait = TaskStatsService.toIntClamped(taskCronerLog.getRunDate().getTime() - scheduleDate.getTime());
            }
            int timeRun = TaskStatsService.toIntClamped(taskCronerLog.getFinishDate().getTime() - taskCronerLog.getRunDate().getTime());
            int[] failCounters = TaskStatsService.toFailCounters(taskCronerLog.getState());
            TaskStatsService.updateCronerStats(taskCronerConfig.getId(), 1, failCounters[0], failCounters[1], failCounters[2], failCounters[3], timeWait, timeRun);
            // 日志落库移入 Runnable（cancel 后 Spring ReschedulingRunnable 跳过 trigger 回调，日志不再丢失）。
            // nextDate 用 cronTrigger 基于本次执行算下次计划时间（近似 trigger 回调的 nextExec）。
            if (taskCronerLog.getId() > 0 && scheduleDate != null && taskCronerLog.getRunDate() != null && taskCronerLog.getFinishDate() != null) {
                try {
                    Instant nextExec = cronTrigger.nextExecution(new org.springframework.scheduling.support.SimpleTriggerContext(
                            scheduleDate.toInstant(), taskCronerLog.getRunDate().toInstant(), taskCronerLog.getFinishDate().toInstant()));
                    if (nextExec != null) {
                        taskCronerLog.setNextDate(Date.from(nextExec));
                        if (log.isDebugEnabled()) {
                            log.debug("正在调度定时任务ID:[{}], CRONER:[{}], CRON:[{}], 下次执行时间:[{}].", taskCronerConfig.getId(), taskCronerConfig.getTaskClass(), cronTrigger.getExpression(), nextExec.toString());
                        }
                    }
                } catch (Exception ignore) {
                    // cron 计算异常（极少），nextDate 留空不影响日志主体落盘
                }
                if (taskCronerConfig.getLogLevel() > TaskCronerConfig.TASK_LOG_TYPE_NONE) {
                    taskApiClient.sendTaskCronerLog(taskCronerConfig.getId(), taskCronerLog);
                }
            }
        }, triggerContext -> {
            // 下次计划执行日期。
            Instant nextExec = cronTrigger.nextExecution(triggerContext);
            // 记录下次计划时间到闭包，供下次 Runnable 体内落盘 stats 时作 scheduleDate（timeWait=runDate-scheduleDate）。
            // stats 已移到 Runnable 体内（cancel 不丢统计），trigger 回调仅传递 scheduleDate + 日志落库（nextDate=nextExec）。
            if (nextExec != null) {
                scheduleDateHolder[0] = Date.from(nextExec);
            }
            // logHolder 仅清理引用（stats + log 已在 Runnable 体内落盘，trigger 回调不再处理日志，cancel 不丢）。
            logHolder[0] = null;
            return nextExec;
        });
        this.cronerTasks.put(configKey, future);
        return true;
    }

    /**
     * 停止一个定时任务：cancel(false) 不中断正在执行的任务（配置覆盖/降级切换让在途跑完，不丢），
     * 仅取消未来调度，并从 {@link #cronerTasks} map 中移除。
     * <p>注意：主机级停机用 {@link #suspendAll()}（cancel(true) 立即停），与本方法 cancel(false) 区分。</p>
     *
     * @param configKey 配置键（见 {@link TaskMetaInfoManager#getCronerConfigKey(TaskCronerConfig)}）
     * @return 成功取消返回 true；map 中无对应调度返回 false
     */
    public boolean stop(String configKey) {
        ScheduledFuture<?> future = this.cronerTasks.get(configKey);
        if (future == null) {
            return false;
        }
        // cancel(false)：不中断正在执行的 croner（配置覆盖/降级切换时让在途跑完），仅取消未来调度
        future.cancel(false);
        this.cronerTasks.remove(configKey);
        return true;
    }

    /**
     * 暂停所有定时任务（禁用，保池）：cancel future + 清空 cronerTasks，不关闭调度线程池。
     * <p>用于主机被禁用(state=0)时停任务却保留 executorService/taskScheduler，以便解除禁用后重新调度。</p>
     */
    public void suspendAll() {
        log.info("All TaskCroner Suspended....");
        for (Entry<String, ScheduledFuture<?>> kv : this.cronerTasks.entrySet()) {
            kv.getValue().cancel(true);
        }
        this.cronerTasks.clear();
    }

    /**
     * 销毁所有定时任务（@PreDestroy，关池）。
     */
    public void destroyAll() {
        log.info("All TaskCroner Destroy....");
        suspendAll();
        executorService.shutdownNow();
    }

}
