package uw.task.container;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
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
import uw.task.util.MiscUtils;
import uw.task.util.TaskGlobalLocker;
import uw.task.util.TaskSequenceManager;
import uw.task.util.TaskStatsService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
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

    private static final Logger log = LoggerFactory.getLogger( TaskCronerContainer.class );
    /**
     * cronerTask任务索引。
     */
    private final Map<Long, ScheduledFuture<?>> cronerTasks = new ConcurrentHashMap<>( 128 );
    private org.springframework.scheduling.TaskScheduler taskScheduler;
    private ScheduledExecutorService executorService;
    /**
     * 选举
     */
    private TaskGlobalLocker taskGlobalLocker;

    /**
     * 服务端API
     */
    private TaskApiClient taskApiClient;

    /**
     * 监听管理器。
     */
    private TaskListenerManager taskListenerManager;

    /**
     * 全局序列发生器。
     */
    private TaskSequenceManager taskSequenceManager;

    /**
     * 任务配置
     */
    private TaskProperties taskProperties;

    /**
     * TaskCronerLog的ThreadLocal。
     */
    private ThreadLocal<TaskCronerLog> cronerLogHolder = new ThreadLocal();

    public TaskCronerContainer(TaskProperties taskProperties, TaskApiClient taskApiClient, TaskSequenceManager taskSequenceManager, TaskListenerManager taskListenerManager,
                               TaskGlobalLocker taskGlobalLocker) {
        this.taskGlobalLocker = taskGlobalLocker;
        this.taskApiClient = taskApiClient;
        this.taskListenerManager = taskListenerManager;
        this.taskSequenceManager = taskSequenceManager;
        this.taskProperties = taskProperties;
        // 如果禁用任务注册，则croner线程数设置为1，节省资源。
        if (!taskProperties.isEnableRegistry()) {
            taskProperties.setCronerThreadNum( 1 );
        }
        executorService = Executors.newScheduledThreadPool( taskProperties.getCronerThreadNum(),
                new ThreadFactoryBuilder().setDaemon( true ).setNameFormat( "TaskCroner-%d" ).build() );
        log.info( "TaskCronerContainer start with [{}] threads...", taskProperties.getCronerThreadNum() );
        taskScheduler = new ConcurrentTaskScheduler( executorService );
    }

    /**
     * 配置任务
     *
     * @param taskCroner
     * @param taskCronerConfig
     * @return
     */
    public boolean configureTask(TaskCroner taskCroner, TaskCronerConfig taskCronerConfig) {

        if (taskCronerConfig == null) {
            log.warn( "配置信息不存在，无法启动CRONER!" );
            return false;
        }
        if (this.taskProperties.getCronerThreadNum() < 1) {
            log.warn( "CRONER线程数设置异常，无法启动!" );
            return false;
        }
        //尝试关闭已有任务
        stopTask( taskCronerConfig.getId() );
        // 标记删除的，直接返回了。
        if (taskCronerConfig.getState() < 1) {
            log.warn( "任务状态不符，无法启动！ID:{}, CRONER:{}, CRON:{}.", taskCronerConfig.getId(), taskCronerConfig.getTaskClass(), taskCronerConfig.getTaskCron() );
            return false;
        }
        CronTrigger cronTrigger = new CronTrigger( taskCronerConfig.getTaskCron() );

        ScheduledFuture<?> future = this.taskScheduler.schedule( () -> {
            // 判断全局唯一条件
            if (taskCronerConfig.getRunType() == TaskCronerConfig.RUN_TYPE_SINGLETON && !taskGlobalLocker.isLock()) {
                if (log.isDebugEnabled()) {
                    log.debug( "非许可运行实例，直接返回。。。" );
                }
                return;
            }
            // 任务逻辑
            TaskCronerLog taskCronerLog = new TaskCronerLog( taskCronerConfig.getLogLevel(), taskCronerConfig.getLogLimitSize() );
            cronerLogHolder.set( taskCronerLog );
            taskCronerLog.setId( taskSequenceManager.nextId( "TaskCronerLog" ) );
            taskCronerLog.setTaskClass( taskCronerConfig.getTaskClass() );
            taskCronerLog.setTaskParam( taskCronerConfig.getTaskParam() );
            taskCronerLog.setTaskCron( taskCronerConfig.getTaskCron() );
            taskCronerLog.setRunType( taskCronerConfig.getRunType() );
            taskCronerLog.setRunTarget( taskCronerConfig.getRunTarget() );
            taskCronerLog.setTaskId( taskCronerConfig.getId() );
            taskCronerLog.setRunDate( new Date() );
            // 执行监听器操作
            ArrayList<CronerTaskListener> cronerListenerList = taskListenerManager.getCronerListenerList();
            if (cronerListenerList != null && cronerListenerList.size() > 0) {
                for (CronerTaskListener listener : cronerListenerList) {
                    try {
                        listener.onPreExecute( taskCronerLog );
                    } catch (Throwable e) {
                        log.error( e.getMessage(), e );
                    }
                }
            }

            String resultData;
            try {
                resultData = taskCroner.runTask( taskCronerLog );
                taskCronerLog.setState( TaskData.STATE_SUCCESS );
            } catch (TaskPartnerException e) {
                // 出现TaskPartnerException，说明是合作方的错误。
                taskCronerLog.setState( TaskData.STATE_FAIL_PARTNER );
                resultData = MiscUtils.exceptionToString( e );
            } catch (TaskDataException e) {
                // 出现TaskDataException，说明是数据错误。
                taskCronerLog.setState( TaskData.STATE_FAIL_DATA );
                resultData = MiscUtils.exceptionToString( e );
            } catch (Throwable e) {
                resultData = MiscUtils.exceptionToString( e );
                taskCronerLog.setState( TaskData.STATE_FAIL_PROGRAM );
                log.error( e.getMessage(), e );
            }
            // 执行监听器操作
            if (cronerListenerList != null && cronerListenerList.size() > 0) {
                for (CronerTaskListener listener : cronerListenerList) {
                    try {
                        listener.onPostExecute( taskCronerLog );
                    } catch (Throwable e) {
                        log.error( e.getMessage(), e );
                    }
                }
            }
            taskCronerLog.setFinishDate( new Date() );
            taskCronerLog.setResultData( resultData );
            taskCronerLog.setRefObject( null );
        }, triggerContext -> {
            // 下次计划执行日期。
            Instant nextExec = cronTrigger.nextExecution( triggerContext );
            //通过threadLocal获取日志实例，并立即删除。
            TaskCronerLog taskCronerLog = cronerLogHolder.get();
            cronerLogHolder.remove();
            if (taskCronerLog != null && taskCronerLog.getId() > 0) {
                //写入下次计划执行时间。
                if (nextExec != null) {
                    taskCronerLog.setNextDate( Date.from( nextExec ) );
                    if (log.isDebugEnabled()) {
                        log.debug( "正在调度ID:[{}], CRONER:[{}], CRON:[{}], 下次执行时间:[{}].", taskCronerConfig.getId(), taskCronerConfig.getTaskClass(), cronTrigger.getExpression(),
                                nextExec.toString() );
                    }
                }
                // 在此处写入本次执行的信息
                if (triggerContext.lastScheduledExecution() != null) {
                    taskCronerLog.setScheduleDate( Date.from( triggerContext.lastScheduledExecution() ) );
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
                TaskStatsService.updateCronerStats( taskCronerConfig.getId(), 1, taskCronerConfig.getState() == TaskData.STATE_FAIL_PROGRAM ? 1 : 0,
                        taskCronerConfig.getState() == TaskData.STATE_FAIL_CONFIG ? 1 : 0, taskCronerConfig.getState() == TaskData.STATE_FAIL_DATA ? 1 : 0,
                        taskCronerConfig.getState() == TaskData.STATE_FAIL_PARTNER ? 1 : 0, timeWait, timeRun );
                // 入库
                taskApiClient.sendTaskCronerLog( taskCronerConfig.getId(), taskCronerLog );
            }
            return nextExec;
        } );
        this.cronerTasks.put( taskCronerConfig.getId(), future );
        return true;
    }

    /**
     * 停止一个任务。
     *
     * @param id 任务编号
     */
    public boolean stopTask(long id) {
        ScheduledFuture<?> future = this.cronerTasks.get( id );
        if (future == null) {
            return false;
        }
        future.cancel( true );
        return true;
    }

    /**
     * 销毁所有的task
     */
    public void stopAllTaskCroner() {
        log.info( "All TaskCroner Destroy...." );
        for (Entry<Long, ScheduledFuture<?>> kv : this.cronerTasks.entrySet()) {
            // 任务停止
            kv.getValue().cancel( true );
        }
        if (this.executorService != null) {
            this.executorService.shutdownNow();
        }
    }

}
