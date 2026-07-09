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
import uw.task.listener.CronerTaskListener;
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
        stopTask(configKey);
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
                // Redis 故障致 nextId 失败：id 保持 0，trigger 回调的 id>0 守门会跳过日志/统计落库；
                // 此处 WARN 让运维可见（不抛出，runTask 仍执行，避免 Redis 抖动静默吞掉整轮调度且无任何痕迹）
                log.warn("Croner[{}] nextId 失败,本轮跳过日志/统计: {}", taskCronerConfig.getTaskClass(), e.getMessage());
            }
            taskCronerLog.setTaskClass(taskCronerConfig.getTaskClass());
            taskCronerLog.setTaskParam(taskCronerConfig.getTaskParam());
            taskCronerLog.setTaskCron(taskCronerConfig.getTaskCron());
            taskCronerLog.setRunType(taskCronerConfig.getRunType());
            taskCronerLog.setRunTarget(taskCronerConfig.getRunTarget());
            taskCronerLog.setTaskId(taskCronerConfig.getId());
            taskCronerLog.setRunDate(SystemClock.nowDate());
            // 执行监听器操作
            List<CronerTaskListener> cronerListenerList = taskListenerManager.getCronerListenerList();
            for (CronerTaskListener listener : cronerListenerList) {
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
            } catch (TaskDataException e) {
                // 出现TaskDataException，说明是数据错误。
                taskCronerLog.setState(TaskData.STATE_FAIL_DATA);
                resultData = ExceptionUtils.exceptionToString(e);
            } catch (TaskPartnerException e) {
                // 出现TaskPartnerException，说明是合作方的错误。
                taskCronerLog.setState(TaskData.STATE_FAIL_PARTNER);
                resultData = ExceptionUtils.exceptionToString(e);
            } catch (Throwable e) {
                resultData = ExceptionUtils.exceptionToString(e);
                taskCronerLog.setState(TaskData.STATE_FAIL_PROGRAM);
                log.error(e.getMessage(), e);
            }
            // 执行监听器操作
            for (CronerTaskListener listener : cronerListenerList) {
                try {
                    listener.onPostExecute(taskCronerLog);
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                }
            }
            taskCronerLog.setFinishDate(SystemClock.nowDate());
            taskCronerLog.setResultData(resultData);
            taskCronerLog.setRefObject(null);
        }, triggerContext -> {
            // 下次计划执行日期。
            Instant nextExec = cronTrigger.nextExecution(triggerContext);
            // 从闭包变量取出本次执行的日志实例，并立即清理引用，避免对象滞留至下一轮调度。
            TaskCronerLog taskCronerLog = logHolder[0];
            logHolder[0] = null;
            if (taskCronerLog != null && taskCronerLog.getId() > 0) {
                //写入下次计划执行时间。
                if (nextExec != null) {
                    taskCronerLog.setNextDate(Date.from(nextExec));
                    if (log.isDebugEnabled()) {
                        log.debug("正在调度定时任务ID:[{}], CRONER:[{}], CRON:[{}], 下次执行时间:[{}].", taskCronerConfig.getId(), taskCronerConfig.getTaskClass(), cronTrigger.getExpression(),
                                nextExec.toString());
                    }
                }
                // 在此处写入本次执行的信息
                if (triggerContext.lastScheduledExecution() != null) {
                    taskCronerLog.setScheduleDate(Date.from(triggerContext.lastScheduledExecution()));
                }
                int timeWait = 0, timeRun = 0;
                if (taskCronerLog.getScheduleDate() != null) {
                    // 特定情况下 timeWait<0 为时钟误差，统一 Math.max(0,...) 兜底；Math.min 防溢出（对齐 Delayer/Runner）
                    timeWait = TaskStatsService.toIntClamped(taskCronerLog.getRunDate().getTime() - taskCronerLog.getScheduleDate().getTime());
                }
                timeRun = TaskStatsService.toIntClamped(taskCronerLog.getFinishDate().getTime() - taskCronerLog.getRunDate().getTime());
                //开始输出统计数据。
                int[] failCounters = TaskStatsService.toFailCounters(taskCronerLog.getState());
                TaskStatsService.updateCronerStats(taskCronerConfig.getId(), 1, failCounters[0], failCounters[1], failCounters[2], failCounters[3], timeWait, timeRun);
                // 入库（对齐 Runner/Delayer：logLevel > NONE 才发日志）
                if (taskCronerConfig.getLogLevel() > TaskRunnerConfig.TASK_LOG_TYPE_NONE) {
                    taskApiClient.sendTaskCronerLog(taskCronerConfig.getId(), taskCronerLog);
                }
            }
            return nextExec;
        });
        this.cronerTasks.put(configKey, future);
        return true;
    }

    /**
     * 停止一个定时任务：cancel(false) 不中断正在执行的任务（配置覆盖/降级切换让在途跑完，不丢），
     * 仅取消未来调度，并从 {@link #cronerTasks} map 中移除。
     * <p>注意：主机级停机用 {@link #suspendAllCroners()}（cancel(true) 立即停），与本方法 cancel(false) 区分。</p>
     *
     * @param configKey 配置键（见 {@link TaskMetaInfoManager#getCronerConfigKey(TaskCronerConfig)}）
     * @return 成功取消返回 true；map 中无对应调度返回 false
     */
    public boolean stopTask(String configKey) {
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
     * 暂停所有定时任务（cancel future + 清空 cronerTasks），但<b>不关闭调度线程池</b>。
     * <p>用于主机被禁用(state=0)时停任务却保留 executorService/taskScheduler，
     * 以便解除禁用后 {@link #configureTask} 能重新调度。区别于 {@link #stopAllTaskCroner}
     *（后者 shutdownNow 线程池，仅供 @PreDestroy 应用关闭时使用）。</p>
     */
    public void suspendAllCroners() {
        log.info("All TaskCroner Suspended....");
        for (Entry<String, ScheduledFuture<?>> kv : this.cronerTasks.entrySet()) {
            kv.getValue().cancel(true);
        }
        this.cronerTasks.clear();
    }

    /**
     * 停止所有定时任务并关闭调度线程池：逐个 cancel(true) 已注册的 future，再 shutdownNow 线程池。
     */
    public void stopAllTaskCroner() {
        log.info("All TaskCroner Destroy....");
        for (Entry<String, ScheduledFuture<?>> kv : this.cronerTasks.entrySet()) {
            // 任务停止
            kv.getValue().cancel(true);
        }
        if (this.executorService != null) {
            this.executorService.shutdownNow();
        }
    }

}
