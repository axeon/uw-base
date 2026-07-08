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
import uw.task.util.TaskRateLimitHelper;
import uw.task.util.TaskStatsService;

import java.util.List;

/**
 * 队列任务执行容器：MQ 消费端 / 本地执行 / RPC 服务端的统一执行入口。
 *
 * <p>由 {@link TaskFactory} 的各个 run* 方法或 RabbitMQ MessageListenerAdapter 调用 {@link #process}，
 * 统一处理限速、延时、异常分类、监听器回调、统计与日志、失败重试。支持三种 runType：
 * {@code RUN_TYPE_GLOBAL}（MQ 消费）、{@code RUN_TYPE_LOCAL}（本地同步）、{@code RUN_TYPE_GLOBAL_RPC}（RPC 同步）。</p>
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
     * @param taskProperties       任务全局配置
     * @param taskApiClient        服务端任务 API 客户端（发送日志等）
     * @param taskLocalRateLimiter 进程内限速器（LOCAL 系列限速）
     * @param taskGlobalRateLimiter 全局限速器（GLOBAL 系列限速，基于 Redis）
     * @param taskListenerManager  任务监听管理器（pre/postExecute 回调）
     * @param taskMetaInfoManager  任务元信息管理器（runner 实例与配置索引）
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
                    // GLOBAL 系列：限流 key 拼接规则见 TaskRateLimitHelper.buildGlobalLocker。
                    String locker = TaskRateLimitHelper.buildGlobalLocker(
                            taskConfig.getRateLimitType(), taskData.getTaskClass(), taskData.getRateLimitTag(), taskProperties.getAppHost());
                    // 全局流量限制
                    // 检查是否超过流量限制
                    // 开始进行延时等待
                    // rateLimitWait 为 int（秒），乘 1000L 转毫秒时必须用 long 字面量，避免 int 溢出
                    long end = SystemClock.now() + taskConfig.getRateLimitWait() * 1000L;
                    while (SystemClock.now() <= end) {
                        noLimitFlag = taskGlobalRateLimiter.tryAcquire(locker, taskConfig.getRateLimitValue(), taskConfig.getRateLimitTime(), 1);
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
        int[] failCounters = TaskStatsService.toFailCounters(taskData.getState());
        TaskStatsService.updateRunnerStats(taskConfig.getId(), 1, failCounters[0], failCounters[1], failCounters[2], failCounters[3],
                TaskStatsService.toIntClamped(taskData.getConsumeDate().getTime() - (taskData.getQueueDate() != null ? taskData.getQueueDate().getTime() : taskData.getConsumeDate().getTime())),
                TaskStatsService.toIntClamped(taskData.getRunDate().getTime() - taskData.getConsumeDate().getTime()),
                TaskStatsService.toIntClamped(taskData.getFinishDate().getTime() - taskData.getRunDate().getTime()));
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
        taskData.cleanForRetry();
        return taskData;
    }

}
