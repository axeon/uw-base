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
import uw.task.conf.TaskProperties;
import uw.task.entity.TaskCronerConfig;
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
 * 跑TaskCroner的容器。
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
    private final Map<Long, ScheduledFuture<?>> cronerTasks = new ConcurrentHashMap<>(128);

    /**
     * 任务调度器。
     */
    private final org.springframework.scheduling.TaskScheduler taskScheduler;
    /**
     * 执行器。
     */
    private final ScheduledExecutorService executorService;
    /**
     * 全局任务锁。
     */
    private final TaskGlobalLocker taskGlobalLocker;
    /**
     * 服务端API
     */
    private final TaskApiClient taskApiClient;
    /**
     * 监听管理器。
     */
    private final TaskListenerManager taskListenerManager;
    /**
     * 全局序列发生器。
     */
    private final TaskSequenceManager taskSequenceManager;
    /**
     * 任务配置
     */
    private final TaskProperties taskProperties;

    public TaskCronerContainer(TaskProperties taskProperties, TaskApiClient taskApiClient, TaskSequenceManager taskSequenceManager, TaskListenerManager taskListenerManager,
                               TaskGlobalLocker taskGlobalLocker) {
        this.taskGlobalLocker = taskGlobalLocker;
        this.taskApiClient = taskApiClient;
        this.taskListenerManager = taskListenerManager;
        this.taskSequenceManager = taskSequenceManager;
        this.taskProperties = taskProperties;
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
        //尝试关闭已有任务
        stopTask(taskCronerConfig.getId());
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
            taskCronerLog.setId(taskSequenceManager.nextId("TaskCronerLog"));
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
            } catch (TaskPartnerException e) {
                // 出现TaskPartnerException，说明是合作方的错误。
                taskCronerLog.setState(TaskData.STATE_FAIL_PARTNER);
                resultData = ExceptionUtils.exceptionToString(e);
            } catch (TaskDataException e) {
                // 出现TaskDataException，说明是数据错误。
                taskCronerLog.setState(TaskData.STATE_FAIL_DATA);
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
                    timeWait = (int) (taskCronerLog.getRunDate().getTime() - taskCronerLog.getScheduleDate().getTime());
                    //特定情况下，time wait<0，我认为是计算误差，在此进行修正。
                    if (timeWait < 0) {
                        timeWait = 0;
                    }
                }
                timeRun = (int) (taskCronerLog.getFinishDate().getTime() - taskCronerLog.getRunDate().getTime());
                //开始输出统计数据。
                TaskStatsService.updateCronerStats(taskCronerConfig.getId(), 1, taskCronerLog.getState() == TaskData.STATE_FAIL_PROGRAM ? 1 : 0,
                        taskCronerLog.getState() == TaskData.STATE_FAIL_CONFIG ? 1 : 0, taskCronerLog.getState() == TaskData.STATE_FAIL_DATA ? 1 : 0,
                        taskCronerLog.getState() == TaskData.STATE_FAIL_PARTNER ? 1 : 0, timeWait, timeRun);
                // 入库
                taskApiClient.sendTaskCronerLog(taskCronerConfig.getId(), taskCronerLog);
            }
            return nextExec;
        });
        this.cronerTasks.put(taskCronerConfig.getId(), future);
        return true;
    }

    /**
     * 停止一个任务。
     *
     * @param id 任务编号
     */
    public boolean stopTask(long id) {
        ScheduledFuture<?> future = this.cronerTasks.get(id);
        if (future == null) {
            return false;
        }
        future.cancel(true);
        return true;
    }

    /**
     * 销毁所有的task
     */
    public void stopAllTaskCroner() {
        log.info("All TaskCroner Destroy....");
        for (Entry<Long, ScheduledFuture<?>> kv : this.cronerTasks.entrySet()) {
            // 任务停止
            kv.getValue().cancel(true);
        }
        if (this.executorService != null) {
            this.executorService.shutdownNow();
        }
    }

}
