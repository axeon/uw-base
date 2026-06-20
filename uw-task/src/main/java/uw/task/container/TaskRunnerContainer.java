package uw.task.container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.util.ExceptionUtils;
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
import uw.task.util.TaskGlobalRateLimiter;
import uw.task.util.TaskLocalRateLimiter;
import uw.task.util.TaskStatsService;

import java.util.List;

/**
 * 在此处接受MQ信息，并进行处理。
 *
 * @author axeon
 */
public class TaskRunnerContainer {

    private static final Logger log = LoggerFactory.getLogger(TaskRunnerContainer.class);


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
     * 执行单个队列任务（MQ 消费端 / 本地执行 / RPC 服务端的统一入口）。
     * <p>
     * 完整流程：
     * <ol>
     *   <li>记录消费时间，定位 runner 实例与配置；runner 缺失则标记 STATE_FAIL_PROGRAM；</li>
     *   <li>按配置的限速类型执行流控（LOCAL 系列用进程令牌桶，GLOBAL 系列用 Redis 固定窗口，
     *       超限则在 rateLimitWait 内轮询等待，仍超限标记 STATE_FAIL_CONFIG）；</li>
     *   <li>无延迟队列时按 taskDelay 做线程内延时；</li>
     *   <li>调用 {@code runTask}，按异常类型映射状态（PARTNER/DATA/PROGRAM）；</li>
     *   <li>触发 RunnerTaskListener、更新统计、按 logLevel 写日志；</li>
     *   <li>失败时按 retryType/retryTimes 自动重试：LOCAL/RPC 走 {@link TaskFactory#runTaskLocal}，
     *       GLOBAL 走 {@link TaskFactory#sendToQueue}。</li>
     * </ol>
     * RPC/LOCAL 模式返回携带结果的 taskData；GLOBAL 模式（MQ 消费）返回 null（结果已随重试/日志处理）。</p>
     *
     * @param taskData 任务数据
     * @return RPC/LOCAL 模式返回 taskData（含结果）；GLOBAL 模式返回 null；入参为 null 时返回 null
     */
    @SuppressWarnings("ALL")
    public TaskData process(TaskData taskData) {
        if (taskData == null) {
            log.warn("正在处理的TaskData为NULL，请检查日志.");
            return null;
        }
        // 设置开始消费时间
        taskData.setConsumeDate(SystemClock.nowDate());
        // 获取任务实例
        TaskRunner<?, ?> taskRunner = taskMetaInfoManager.getRunnerInstance(taskData.getTaskClass());
        if (taskRunner == null) {
            log.error("未找到TaskRunner实例: {}", taskData.getTaskClass());
            taskData.setState(TaskData.STATE_FAIL_PROGRAM);
            taskData.setErrorInfo("TaskRunner not found: " + taskData.getTaskClass());
            taskData.setFinishDate(SystemClock.nowDate());
            return taskData;
        }
        // 获取任务设置数据
        TaskRunnerConfig taskConfig = taskMetaInfoManager.getRunnerConfigByData(taskData);
        // 增加执行信息
        taskData.setRanTimes(taskData.getRanTimes() + 1);

        // 限制标记，0时说明无限制
        long noLimitFlag = 0;
        // 对于RPC调用和本地调用来说，不受任何流控限制。
        if (taskData.getRunType() != TaskData.RUN_TYPE_GLOBAL_RPC && taskData.getRunType() != TaskData.RUN_TYPE_LOCAL) {
            if (taskConfig.getRateLimitType() != TaskRunnerConfig.RATE_LIMIT_NONE) {
                if (taskConfig.getRateLimitType() == TaskRunnerConfig.RATE_LIMIT_LOCAL) {
                    // 进程内限制
                    boolean flag = taskLocalRateLimiter.tryAcquire("", taskConfig.getRateLimitValue(), taskConfig.getRateLimitTime(), taskConfig.getRateLimitWait(), 1);
                    noLimitFlag = flag ? 0 : -1;
                } else if (taskConfig.getRateLimitType() == TaskRunnerConfig.RATE_LIMIT_LOCAL_TASK) {
                    // 进程内+任务名限制
                    boolean flag = taskLocalRateLimiter.tryAcquire(taskData.getTaskClass(), taskConfig.getRateLimitValue(), taskConfig.getRateLimitTime(),
                            taskConfig.getRateLimitWait(), 1);
                    noLimitFlag = flag ? 0 : -1;
                } else if (taskConfig.getRateLimitType() == TaskRunnerConfig.RATE_LIMIT_LOCAL_TASK_TAG) {
                    // 进程内+任务名限制
                    String locker = taskData.getTaskClass() + "$" + taskData.getRateLimitTag();
                    boolean flag = taskLocalRateLimiter.tryAcquire(locker, taskConfig.getRateLimitValue(), taskConfig.getRateLimitTime(), taskConfig.getRateLimitWait(), 1);
                    noLimitFlag = flag ? 0 : -1;
                } else {
                    // 限流 key 拼接规则：
                    // - GLOBAL_TASK* 系列（TASK/TASK_TAG/TASK_HOST/TASK_TAG_HOST）：key 含 taskClass 前缀，
                    //   即"按任务隔离"配额。
                    // - GLOBAL_TAG / GLOBAL_HOST / GLOBAL_TAG_HOST：key 不含 taskClass，
                    //   即"跨任务共享"配额——多个不同 task 只要 tag/host 相同即共享同一限流池。
                    //   这是限流的常见场景（如多个任务都对接同一第三方接口，需共享 QPS 上限）。
                    int rateLimitType = taskConfig.getRateLimitType();
                    boolean taskScoped = rateLimitType == TaskRunnerConfig.RATE_LIMIT_GLOBAL_TASK
                            || rateLimitType == TaskRunnerConfig.RATE_LIMIT_GLOBAL_TASK_TAG
                            || rateLimitType == TaskRunnerConfig.RATE_LIMIT_GLOBAL_TASK_HOST
                            || rateLimitType == TaskRunnerConfig.RATE_LIMIT_GLOBAL_TASK_TAG_HOST;
                    StringBuilder locker = new StringBuilder(50);
                    if (taskScoped) {
                        locker.append(taskData.getTaskClass());
                    }
                    switch (rateLimitType) {
                        case TaskRunnerConfig.RATE_LIMIT_GLOBAL_TAG:
                        case TaskRunnerConfig.RATE_LIMIT_GLOBAL_TASK_TAG:
                            locker.append("$").append(taskData.getRateLimitTag());
                            break;
                        case TaskRunnerConfig.RATE_LIMIT_GLOBAL_HOST:
                        case TaskRunnerConfig.RATE_LIMIT_GLOBAL_TASK_HOST:
                            locker.append("$@").append(taskProperties.getAppHost());
                            break;
                        case TaskRunnerConfig.RATE_LIMIT_GLOBAL_TASK:
                            locker.append("$");
                            break;
                        case TaskRunnerConfig.RATE_LIMIT_GLOBAL_TAG_HOST:
                        case TaskRunnerConfig.RATE_LIMIT_GLOBAL_TASK_TAG_HOST:
                            locker.append("$").append(taskData.getRateLimitTag()).append("@").append(taskProperties.getAppHost());
                            break;
                        default:
                            break;
                    }
                    // 全局流量限制
                    // 检查是否超过流量限制
                    // 开始进行延时等待
                    long end = SystemClock.now() + taskConfig.getRateLimitWait() * 1000;
                    while (SystemClock.now() <= end) {
                        noLimitFlag = taskGlobalRateLimiter.tryAcquire(locker.toString(), taskConfig.getRateLimitValue(), taskConfig.getRateLimitTime(), 1);
                        if (noLimitFlag == 0) {
                            break;
                        }
                        try {
                            Thread.sleep(noLimitFlag);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
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
                    Thread.sleep(delaySleep);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        // 设置开始执行时间
        taskData.setRunDate(SystemClock.nowDate());
        // 如果允许，则开始执行。
        if (noLimitFlag == 0) {
            List<RunnerTaskListener> runnerListenerList = taskListenerManager.getRunnerListenerList();
            try {
                // 执行任务
                taskData.setResultData(taskRunner.runTask(taskData));
                taskData.setState(TaskData.STATE_SUCCESS);
            } catch (TaskDataException e) {
                // 出现TaskDataException，说明是数据错误。
                taskData.setState(TaskData.STATE_FAIL_DATA);
                taskData.setErrorInfo(ExceptionUtils.exceptionToString(e));
            } catch (TaskPartnerException e) {
                // 出现TaskPartnerException，说明是合作方的错误。
                taskData.setState(TaskData.STATE_FAIL_PARTNER);
                taskData.setErrorInfo(ExceptionUtils.exceptionToString(e));
            } catch (Throwable e) {
                // 设置异常状态
                taskData.setState(TaskData.STATE_FAIL_PROGRAM);
                // 设置异常信息，自动屏蔽spring自己的输出。
                taskData.setErrorInfo(ExceptionUtils.exceptionToString(e));
                log.error(e.getMessage(), e);
            }
            // 执行监听器操作
            if (runnerListenerList != null && runnerListenerList.size() > 0) {
                try {
                    for (RunnerTaskListener listener : runnerListenerList) {
                        listener.onPostExecute(taskData);
                    }
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                }
            }
            // 清除refObject。
            taskData.setRefObject(null);
        } else {
            taskData.setErrorInfo("RateLimit!!!" + taskConfig.getRateLimitValue() + "/" + taskConfig.getRateLimitTime() + "s, Wait " + taskConfig.getRateLimitWait() + "s!");
            taskData.setState(TaskData.STATE_FAIL_CONFIG);
        }

        // 不管如何，都给设定结束日期。
        taskData.setFinishDate(SystemClock.nowDate());
        //开始输出统计数据。
        TaskStatsService.updateRunnerStats(taskConfig.getId(), 1,
                taskData.getState() == TaskData.STATE_FAIL_PROGRAM ? 1 : 0,
                taskData.getState() == TaskData.STATE_FAIL_CONFIG ? 1 : 0,
                taskData.getState() == TaskData.STATE_FAIL_DATA ? 1 : 0,
                taskData.getState() == TaskData.STATE_FAIL_PARTNER ? 1 : 0,
                (int) (taskData.getConsumeDate().getTime() - (taskData.getQueueDate() != null ? taskData.getQueueDate().getTime() : taskData.getConsumeDate().getTime())),
                (int) (taskData.getRunDate().getTime() - taskData.getConsumeDate().getTime()),
                (int) (taskData.getFinishDate().getTime() - taskData.getRunDate().getTime()));
        // 保存日志与统计信息
        int logLevel = taskConfig.getLogLevel();
        if (logLevel > TaskRunnerConfig.TASK_LOG_TYPE_NONE) {
            TaskRunnerLog log = new TaskRunnerLog(taskData);
            log.setLogLevel(logLevel);
            log.setLogLimitSize(taskConfig.getLogLimitSize());
            log.setTaskId(taskConfig.getId());
            taskApiClient.sendTaskRunnerLog(log);
        }

        if (taskData.getRunType() == TaskData.RUN_TYPE_GLOBAL_RPC || taskData.getRunType() == TaskData.RUN_TYPE_LOCAL) {
            // 如果异常，根据任务设置，重新跑
            if (taskData.getRetryType() == TaskData.RETRY_TYPE_AUTO) {
                if (taskData.getState() > TaskData.STATE_SUCCESS) {
                    //设置任务延时，原计划使用Math.pow(2,x)的，后来决定不用了
                    taskData.setTaskDelay(taskData.getRanTimes() * taskProperties.getTaskRpcRetryDelay());
                    if (taskData.getState() == TaskData.STATE_FAIL_CONFIG) {
                        // 重试判定用 <=：ranTimes 含本次执行，N 次额外重试意味着 ranTimes 可达 N+1。
                        // 即 retryTimesByOverrated=N 时，总计执行 N+1 次（1 次初始 + N 次重试）。
                        if (taskData.getRanTimes() <= taskConfig.getRetryTimesByOverrated()) {
                            taskData = TaskFactory.getInstance().runTaskLocal(cleanTaskInfo(taskData));
                        }
                    } else if (taskData.getState() == TaskData.STATE_FAIL_PARTNER) {
                        if (taskData.getRanTimes() <= taskConfig.getRetryTimesByPartner()) {
                            taskData = TaskFactory.getInstance().runTaskLocal(cleanTaskInfo(taskData));
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
                    taskData.setTaskDelay(taskData.getRanTimes() * taskProperties.getTaskQueueRetryDelay());
                    if (taskData.getState() == TaskData.STATE_FAIL_CONFIG) {
                        // 重试判定用 <=：ranTimes 含本次执行，N 次额外重试意味着 ranTimes 可达 N+1。
                        if (taskData.getRanTimes() <= taskConfig.getRetryTimesByOverrated()) {
                            TaskFactory.getInstance().sendToQueue(cleanTaskInfo(taskData));
                        }
                    } else if (taskData.getState() == TaskData.STATE_FAIL_PARTNER) {
                        if (taskData.getRanTimes() <= taskConfig.getRetryTimesByPartner()) {
                            TaskFactory.getInstance().sendToQueue(cleanTaskInfo(taskData));
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
        taskData.setRefObject(null);
        taskData.setConsumeDate(null);
        taskData.setRunDate(null);
        taskData.setFinishDate(null);
        taskData.setResultData(null);
        taskData.setErrorInfo(null);
        taskData.setState(TaskData.STATE_UNKNOWN);
        return taskData;
    }

}
