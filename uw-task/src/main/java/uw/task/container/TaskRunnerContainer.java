package uw.task.container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.util.SystemClock;
import uw.task.TaskData;
import uw.task.TaskFactory;
import uw.task.TaskListenerManager;
import uw.task.TaskRunner;
import uw.task.api.TaskApiClient;
import uw.task.conf.TaskMetaInfoManager;
import uw.task.conf.TaskProperties;
import uw.task.entity.TaskRunnerConfig;
import uw.task.entity.TaskRunnerLog;
import uw.task.exception.TaskDataException;
import uw.task.exception.TaskPartnerException;
import uw.task.listener.RunnerTaskListener;
import uw.task.util.MiscUtils;
import uw.task.util.TaskGlobalRateLimiter;
import uw.task.util.TaskLocalRateLimiter;
import uw.task.util.TaskStatsService;

import java.util.ArrayList;

/**
 * 在此处接受MQ信息，并进行处理。
 *
 * @author axeon
 */
public class TaskRunnerContainer {

    private static final Logger log = LoggerFactory.getLogger( TaskRunnerContainer.class );


    /**
     * 任务配置
     */
    private final TaskProperties taskProperties;

    /**
     * 服务端任务API
     */
    private final TaskApiClient taskApiClient;

    /**
     * 任务信息管理器。
     */
    private final TaskMetaInfoManager taskMetaInfoManager;

    /**
     * 本地流量限制服务
     */
    private final TaskLocalRateLimiter taskLocalRateLimiter;

    /**
     * 全局流量限制服务
     */
    private final TaskGlobalRateLimiter taskGlobalRateLimiter;

    /**
     * 监听管理器。
     */
    private final TaskListenerManager taskListenerManager;

    /**
     * 默认构造器。
     *
     * @param taskProperties
     * @param taskApiClient
     * @param taskLocalRateLimiter
     * @param taskGlobalRateLimiter
     * @param taskListenerManager
     */
    public TaskRunnerContainer(TaskProperties taskProperties, TaskApiClient taskApiClient, TaskLocalRateLimiter taskLocalRateLimiter,
                               TaskGlobalRateLimiter taskGlobalRateLimiter, TaskListenerManager taskListenerManager, TaskMetaInfoManager taskMetaInfoManager) {
        this.taskProperties = taskProperties;
        this.taskApiClient = taskApiClient;
        this.taskLocalRateLimiter = taskLocalRateLimiter;
        this.taskGlobalRateLimiter = taskGlobalRateLimiter;
        this.taskListenerManager = taskListenerManager;
        this.taskMetaInfoManager = taskMetaInfoManager;
    }


    /**
     * 执行任务
     *
     * @param taskData
     * @return
     */
    @SuppressWarnings("ALL")
    public TaskData process(TaskData taskData) {
        if (taskData == null) {
            log.warn( "正在处理的TaskData为NULL，请检查日志." );
            return null;
        }
        // 设置开始消费时间
        taskData.setConsumeDate( SystemClock.nowDate() );
        // 获取任务实例
        TaskRunner<?, ?> taskRunner = taskMetaInfoManager.getRunnerInstance( taskData.getTaskClass() );
        // 获取任务设置数据
        TaskRunnerConfig taskConfig = taskMetaInfoManager.getRunnerConfigByData( taskData );
        // 增加执行信息
        taskData.setRanTimes( taskData.getRanTimes() + 1 );

        // 限制标记，0时说明无限制
        long noLimitFlag = 0;
        // 对于RPC调用和本地调用来说，不受任何流控限制。
        if (taskData.getRunType() != TaskData.RUN_TYPE_GLOBAL_RPC && taskData.getRunType() != TaskData.RUN_TYPE_LOCAL) {
            if (taskConfig.getRateLimitType() != TaskRunnerConfig.RATE_LIMIT_NONE) {
                if (taskConfig.getRateLimitType() == TaskRunnerConfig.RATE_LIMIT_LOCAL) {
                    // 进程内限制
                    boolean flag = taskLocalRateLimiter.tryAcquire( "", taskConfig.getRateLimitValue(), taskConfig.getRateLimitTime(), taskConfig.getRateLimitWait(), 1 );
                    noLimitFlag = flag ? 0 : -1;
                } else if (taskConfig.getRateLimitType() == TaskRunnerConfig.RATE_LIMIT_LOCAL_TASK) {
                    // 进程内+任务名限制
                    boolean flag = taskLocalRateLimiter.tryAcquire( taskData.getTaskClass(), taskConfig.getRateLimitValue(), taskConfig.getRateLimitTime(),
                            taskConfig.getRateLimitWait(), 1 );
                    noLimitFlag = flag ? 0 : -1;
                } else if (taskConfig.getRateLimitType() == TaskRunnerConfig.RATE_LIMIT_LOCAL_TASK_TAG) {
                    // 进程内+任务名限制
                    String locker = taskData.getTaskClass() + "$" + taskData.getRateLimitTag();
                    boolean flag = taskLocalRateLimiter.tryAcquire( locker, taskConfig.getRateLimitValue(), taskConfig.getRateLimitTime(), taskConfig.getRateLimitWait(), 1 );
                    noLimitFlag = flag ? 0 : -1;
                } else {
                    StringBuilder locker = new StringBuilder( 50 ).append( taskData.getTaskClass() );
                    switch (taskConfig.getRateLimitType()) {
                        case TaskRunnerConfig.RATE_LIMIT_GLOBAL_TAG:
                            locker.append( "$" ).append( taskData.getRateLimitTag() );
                            break;
                        case TaskRunnerConfig.RATE_LIMIT_GLOBAL_HOST:
                            locker.append( "$@" ).append( taskProperties.getAppHost() );
                            break;
                        case TaskRunnerConfig.RATE_LIMIT_GLOBAL_TASK:
                            locker.append( "$" );
                            break;
                        case TaskRunnerConfig.RATE_LIMIT_GLOBAL_TAG_HOST:
                            locker.append( "$" ).append( taskData.getRateLimitTag() ).append( "@" ).append( taskProperties.getAppHost() );
                            break;
                        case TaskRunnerConfig.RATE_LIMIT_GLOBAL_TASK_TAG:
                            locker.append( "$" ).append( taskData.getRateLimitTag() );
                            break;
                        case TaskRunnerConfig.RATE_LIMIT_GLOBAL_TASK_HOST:
                            locker.append( "$@" ).append( taskProperties.getAppHost() );
                            break;
                        case TaskRunnerConfig.RATE_LIMIT_GLOBAL_TASK_TAG_HOST:
                            locker.append( "$" ).append( taskData.getRateLimitTag() ).append( "@" ).append( taskProperties.getAppHost() );
                            break;
                    }
                    // 全局流量限制
                    // 检查是否超过流量限制
                    // 开始进行延时等待
                    long end = SystemClock.now() + taskConfig.getRateLimitWait() * 1000;
                    while (SystemClock.now() <= end) {
                        noLimitFlag = taskGlobalRateLimiter.tryAcquire( locker.toString(), taskConfig.getRateLimitValue(), taskConfig.getRateLimitTime(), 1 );
                        if (noLimitFlag == 0) {
                            break;
                        }
                        try {
                            Thread.sleep( noLimitFlag );
                        } catch (InterruptedException e) {
                            log.error( e.getMessage(), e );
                        }
                    }
                }
            }

        }
        // 执行任务延时设定，必须是无延时队列功能，才会执行线程延时。
        if (taskConfig.getDelayType() == TaskRunnerConfig.TYPE_DELAY_OFF && taskData.getTaskDelay() > 0 && taskData.getQueueDate() != null) {
            long delaySleep = taskData.getTaskDelay() - (SystemClock.now() - taskData.getQueueDate().getTime());
            if (delaySleep > 0) {
                try {
                    Thread.sleep( delaySleep );
                } catch (InterruptedException e) {
                    log.error( e.getMessage(), e );
                }
            }
        }
        // 设置开始执行时间
        taskData.setRunDate( SystemClock.nowDate() );
        // 如果允许，则开始执行。
        if (noLimitFlag == 0) {
            ArrayList<RunnerTaskListener> runnerListenerList = taskListenerManager.getRunnerListenerList();
            try {
                // 执行任务
                taskData.setResultData( taskRunner.runTask( taskData ) );
                taskData.setState( TaskData.STATE_SUCCESS );
            } catch (TaskDataException e) {
                // 出现TaskDataException，说明是数据错误。
                taskData.setState( TaskData.STATE_FAIL_DATA );
                taskData.setErrorInfo( MiscUtils.exceptionToString( e ) );
            } catch (TaskPartnerException e) {
                // 出现TaskPartnerException，说明是合作方的错误。
                taskData.setState( TaskData.STATE_FAIL_PARTNER );
                taskData.setErrorInfo( MiscUtils.exceptionToString( e ) );
            } catch (Throwable e) {
                // 设置异常状态
                taskData.setState( TaskData.STATE_FAIL_PROGRAM );
                // 设置异常信息，自动屏蔽spring自己的输出。
                taskData.setErrorInfo( MiscUtils.exceptionToString( e ) );
                log.error( e.getMessage(), e );
            }
            // 执行监听器操作
            if (runnerListenerList != null && runnerListenerList.size() > 0) {
                try {
                    for (RunnerTaskListener listener : runnerListenerList) {
                        listener.onPostExecute( taskData );
                    }
                } catch (Throwable e) {
                    log.error( e.getMessage(), e );
                }
            }
            // 清除refObject。
            taskData.setRefObject( null );
        } else {
            taskData.setErrorInfo( "RateLimit!!!" + taskConfig.getRateLimitValue() + "/" + taskConfig.getRateLimitTime() + "s, Wait " + taskConfig.getRateLimitWait() + "s!" );
            taskData.setState( TaskData.STATE_FAIL_CONFIG );
        }

        // 不管如何，都给设定结束日期。
        taskData.setFinishDate( SystemClock.nowDate() );
        //开始输出统计数据。
        TaskStatsService.updateRunnerStats( taskConfig.getId(), 1,
                taskData.getState() == TaskData.STATE_FAIL_PROGRAM ? 1 : 0,
                taskData.getState() == TaskData.STATE_FAIL_CONFIG ? 1 : 0,
                taskData.getState() == TaskData.STATE_FAIL_DATA ? 1 : 0,
                taskData.getState() == TaskData.STATE_FAIL_PARTNER ? 1 : 0,
                (int) (taskData.getConsumeDate().getTime() - taskData.getQueueDate().getTime()),
                (int) (taskData.getRunDate().getTime() - taskData.getConsumeDate().getTime()),
                (int) (taskData.getFinishDate().getTime() - taskData.getRunDate().getTime()) );
        // 保存日志与统计信息
        int logLevel = taskConfig.getLogLevel();
        if (logLevel > TaskRunnerConfig.TASK_LOG_TYPE_NONE) {
            TaskRunnerLog log = new TaskRunnerLog( taskData );
            log.setLogLevel( logLevel );
            log.setLogLimitSize( taskConfig.getLogLimitSize() );
            log.setTaskId( taskConfig.getId() );
            taskApiClient.sendTaskRunnerLog( log );
        }

        if (taskData.getRunType() == TaskData.RUN_TYPE_GLOBAL_RPC || taskData.getRunType() == TaskData.RUN_TYPE_LOCAL) {
            // 如果异常，根据任务设置，重新跑
            if (taskData.getRetryType() == TaskData.RETRY_TYPE_AUTO) {
                if (taskData.getState() > TaskData.STATE_SUCCESS) {
                    //设置任务延时，原计划使用Math.pow(2,x)的，后来决定不用了
                    taskData.setTaskDelay( taskData.getRanTimes() * taskProperties.getTaskRpcRetryDelay() );
                    if (taskData.getState() == TaskData.STATE_FAIL_CONFIG) {
                        if (taskData.getRanTimes() < taskConfig.getRetryTimesByOverrated()) {
                            taskData = TaskFactory.getInstance().runTaskLocal( cleanTaskInfo( taskData ) );
                        }
                    } else if (taskData.getState() == TaskData.STATE_FAIL_PARTNER) {
                        if (taskData.getRanTimes() < taskConfig.getRetryTimesByPartner()) {
                            taskData = TaskFactory.getInstance().runTaskLocal( cleanTaskInfo( taskData ) );
                        }
                    }
                }
            }
            return taskData;
        } else {
            // 如果异常，根据任务设置，重新跑
            if (taskData.getRetryType() == TaskData.RETRY_TYPE_AUTO) {
                if (taskData.getState() > TaskData.STATE_SUCCESS) {
                    //设置任务延时，原计划使用Math.pow(2,x)的，后来决定不用了
                    taskData.setTaskDelay( taskData.getRanTimes() * taskProperties.getTaskQueueRetryDelay() );
                    if (taskData.getState() == TaskData.STATE_FAIL_CONFIG) {
                        if (taskData.getRanTimes() < taskConfig.getRetryTimesByOverrated()) {
                            TaskFactory.getInstance().sendToQueue( cleanTaskInfo( taskData ) );
                        }
                    } else if (taskData.getState() == TaskData.STATE_FAIL_PARTNER) {
                        if (taskData.getRanTimes() < taskConfig.getRetryTimesByPartner()) {
                            TaskFactory.getInstance().sendToQueue( cleanTaskInfo( taskData ) );
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 清理任务信息。
     *
     * @param srcData
     */
    private TaskData cleanTaskInfo(TaskData srcData) {
        TaskData taskData = srcData.copy();
        //先清除一些任务信息。
        taskData.setRefObject( null );
        taskData.setConsumeDate( null );
        taskData.setRunDate( null );
        taskData.setFinishDate( null );
        taskData.setResultData( null );
        taskData.setErrorInfo( null );
        taskData.setState( TaskData.STATE_UNKNOWN );
        return taskData;
    }

}
