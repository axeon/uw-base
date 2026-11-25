package uw.task.entity;

import uw.task.TaskRunner;

import java.io.Serializable;
import java.util.Date;

/**
 * 队列任务配置。
 *
 * <p>由 {@link TaskRunner#initConfig()} 产出默认值，首次注册时上传到 task-center，
 * 之后以服务端下发的动态配置为准。支持多实例：同一 taskClass 通过不同 taskTag 区分。
 * 关键配置：queueType（队列形态 classic/quorum）、rateLimitType/Value/Time/Wait（限速）、
 * consumerNum/prefetchNum（消费并发）、retryTimesByPartner/Overrated/Program（重试）、各类 alert 阈值。</p>
 *
 * @author axeon
 * @version $Revision: 1.00 $ $Date: 2017-05-03 14:00:50
 */
public class TaskRunnerConfig implements Serializable {

    /**
     * 默认预取数。quorum 队列每批确认都要等 WAL 落盘后才放行下一批投递，预取 1 时每条消息一次落盘
     * （dev 实测约 450~550 msg/s）；10 把在途消息数放大 10 倍，同时仍限制单消费者的内存占用与失败时的重投范围。
     */
    public static final int DEFAULT_PREFETCH_NUM = 10;

    /**
     * 默认消费并发。
     */
    public static final int DEFAULT_CONSUMER_NUM = 1;

    /**
     * 默认限速次数（每个限速时间窗口内允许的次数）。
     */
    public static final int DEFAULT_RATE_LIMIT_VALUE = 10;

    /**
     * 默认限速时间窗口（秒）。
     */
    public static final int DEFAULT_RATE_LIMIT_TIME = 1;

    /**
     * 默认限速超限后的最长等待（秒）。
     */
    public static final int DEFAULT_RATE_LIMIT_WAIT = 30;

    /**
     * 默认运行目标。
     */
    public static final String DEFAULT_RUN_TARGET = "default";

    /**
     * classic 队列：单副本、吞吐最优，适用于即时消费。
     */
    public static final int QUEUE_TYPE_CLASSIC = 0;

    /**
     * quorum 队列：Raft 复制；原生延迟消息（x-opt-delivery-time，RabbitMQ 4.4+）仅 quorum 支持。
     */
    public static final int QUEUE_TYPE_QUORUM = 1;

    /**
     * 限速类型：不限速
     */
    public static final int RATE_LIMIT_NONE = 0;

    /**
     * 限速类型：本地进程限速
     */
    public static final int RATE_LIMIT_LOCAL = 1;

    /**
     * 限速类型：本地TASK限速
     */
    public static final int RATE_LIMIT_LOCAL_TASK = 2;

    /**
     * 限速类型：本地TASK+TAG限速
     */
    public static final int RATE_LIMIT_LOCAL_TASK_TAG = 3;

    /**
     * 限速类型：全局主机HOST限速
     */
    public static final int RATE_LIMIT_GLOBAL_HOST = 4;

    /**
     * 限速类型：全局TAG限速
     */
    public static final int RATE_LIMIT_GLOBAL_TAG = 5;

    /**
     * 限速类型：全局TASK限速
     */
    public static final int RATE_LIMIT_GLOBAL_TASK = 6;

    /**
     * 限速类型：全局TAG+HOST限速
     */
    public static final int RATE_LIMIT_GLOBAL_TAG_HOST = 7;

    /**
     * 限速类型：全局TASK+IP限速
     */
    public static final int RATE_LIMIT_GLOBAL_TASK_HOST = 8;

    /**
     * 限速类型：全局TASK+TAG限速
     */
    public static final int RATE_LIMIT_GLOBAL_TASK_TAG = 9;

    /**
     * 限速类型：全局TASK+TAG+IP限速
     */
    public static final int RATE_LIMIT_GLOBAL_TASK_TAG_HOST = 10;

    /**
     * 什么都不记录
     */
    public static final int TASK_LOG_TYPE_NONE = -1;

    /**
     * 记录日志
     */
    public static final int TASK_LOG_TYPE_RECORD = 0;

    /**
     * 记录日志,含请求参数
     */
    public static final int TASK_LOG_TYPE_RECORD_TASK_PARAM = 1;

    /**
     * 记录日志,含返回参数
     */
    public static final int TASK_LOG_TYPE_RECORD_RESULT_DATA = 2;

    /**
     * 记录全部日志
     */
    public static final int TASK_LOG_TYPE_RECORD_ALL = 3;

    /**
     * 任务配置 id（服务端分配）。
     */
    private long id;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务描述
     */
    private String taskDesc;

    /**
     * 执行类信息
     */
    private String taskClass;

    /**
     * 执行类TAG，可能用于区分子任务
     */
    private String taskTag;

    /**
     * 队列形态：QUEUE_TYPE_CLASSIC / QUEUE_TYPE_QUORUM。
     * <p>默认 quorum（用户定案 261120、261124 复核保留）：数据安全、原生延迟消息唯一载体，
     * 吞吐靠调大 prefetchNum 与 broker 侧流控调优解决；追求极致吞吐且接受单副本风险时选 classic（classic 队列的消息默认非持久化不写盘，见 TaskFactory.buildTaskQueueMessage）。</p>
     */
    private int queueType = QUEUE_TYPE_QUORUM;

    /**
     * 消费者的数量
     */
    private int consumerNum = DEFAULT_CONSUMER_NUM;

    /**
     * 预取任务数（在途未确认消息上限，默认 {@value #DEFAULT_PREFETCH_NUM}；quorum 队列吞吐敏感场景建议 ≥50）。
     */
    private int prefetchNum = DEFAULT_PREFETCH_NUM;

    /**
     * 详见流量限制类型说明。
     */
    private int rateLimitType = RATE_LIMIT_NONE;

    /**
     * 流量限定数值，默认为10次
     */
    private int rateLimitValue = DEFAULT_RATE_LIMIT_VALUE;

    /**
     * 流量限定时间(S)，默认为1秒
     */
    private int rateLimitTime = DEFAULT_RATE_LIMIT_TIME;

    /**
     * 当发生流量限制时，等待的秒数，默认30秒
     */
    private int rateLimitWait = DEFAULT_RATE_LIMIT_WAIT;

    /**
     * 超过流量限制时的<b>额外</b>重试次数，默认 0 表示不重试、直接放弃任务。
     * <p>实现说明（见 {@code TaskRunnerContainer.process()}）：以 {@code ranTimes <= retryTimesByOverrated}
     * 作为是否继续重试的判定，{@code ranTimes} 在每次进入 process 时自增（含首次执行）。
     * 设为 N 时，任务总计执行 {@code N+1} 次（1 次初始 + N 次重试）；N=0 时仅执行 1 次不重试。</p>
     */
    private int retryTimesByOverrated = 0;

    /**
     * 对方接口错误时的<b>额外</b>重试次数，默认 0 表示不重试、直接放弃任务。
     * <p>同 {@link #retryTimesByOverrated}，设为 N 时总计执行 N+1 次（1 次初始 + N 次重试）。</p>
     */
    private int retryTimesByPartner = 0;

    /**
     * 程序自身异常时的<b>额外</b>重试次数，默认 0 表示不重试、直接放弃任务。
     * <p>同 {@link #retryTimesByOverrated}，设为 N 时总计执行 N+1 次（1 次初始 + N 次重试）。
     * 程序异常（NPE/状态错等）也可配置自动重试。</p>
     */
    private int retryTimesByProgram = 0;

    /**
     * 运行目标，不用指定，由程序运行期配置。
     */
    private String runTarget = DEFAULT_RUN_TARGET;

    /**
     * 失败率
     */
    private int alertFailRate;

    /**
     * 接口失败率
     */
    private int alertFailPartnerRate;

    /**
     * 程序失败率
     */
    private int alertFailProgramRate;

    /**
     * 配置失败率
     */
    private int alertFailConfigRate;

    /**
     * 数据失败率
     */
    private int alertFailDataRate;

    /**
     * 队列超长度
     */
    private int alertQueueOversize;

    /**
     * 队列等待超时
     */
    private int alertQueueTimeout;

    /**
     * 等待超时
     */
    private int alertWaitTimeout;

    /**
     * 运行超时
     */
    private int alertRunTimeout;

    /**
     * 创建日期。
     */
    private Date createDate;

    /**
     * 修改日期。
     */
    private Date modifyDate;

    /**
     * 状态值
     */
    private int state = 1;

    /**
     * 详见日志类型说明
     */
    private int logLevel = TASK_LOG_TYPE_RECORD;

    /**
     * 日志字符串字段大小限制: 0 表示无限制
     */
    private int logLimitSize = 0;

    /**
     * 默认构造器。
     */
    public TaskRunnerConfig() {
    }

    /**
     * 带任务名称的构造器。
     *
     * @param taskName 任务名称
     */
    public TaskRunnerConfig(String taskName) {
        this.taskName = taskName;
    }

    /**
     * 由 Builder 构建实例。
     * @param builder 已设置好字段的 Builder
     */
    private TaskRunnerConfig(Builder builder) {
        setId(builder.id);
        setTaskName(builder.taskName);
        setTaskDesc(builder.taskDesc);
        setTaskClass(builder.taskClass);
        setTaskTag(builder.taskTag);
        setQueueType(builder.queueType);
        setConsumerNum(builder.consumerNum);
        setPrefetchNum(builder.prefetchNum);
        setRateLimitType(builder.rateLimitType);
        setRateLimitValue(builder.rateLimitValue);
        setRateLimitTime(builder.rateLimitTime);
        setRateLimitWait(builder.rateLimitWait);
        setRetryTimesByOverrated(builder.retryTimesByOverrated);
        setRetryTimesByPartner(builder.retryTimesByPartner);
        setRetryTimesByProgram(builder.retryTimesByProgram);
        setRunTarget(builder.runTarget);
        setAlertFailRate(builder.alertFailRate);
        setAlertFailPartnerRate(builder.alertFailPartnerRate);
        setAlertFailProgramRate(builder.alertFailProgramRate);
        setAlertFailConfigRate(builder.alertFailConfigRate);
        setAlertFailDataRate(builder.alertFailDataRate);
        setAlertQueueOversize(builder.alertQueueOversize);
        setAlertQueueTimeout(builder.alertQueueTimeout);
        setAlertWaitTimeout(builder.alertWaitTimeout);
        setAlertRunTimeout(builder.alertRunTimeout);
        setCreateDate(builder.createDate);
        setModifyDate(builder.modifyDate);
        setState(builder.state);
        setLogLevel(builder.logLevel);
        setLogLimitSize(builder.logLimitSize);
    }

    /**
     * 创建空 Builder。
     *
     * @return 空白 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * builder 模式，带 taskName 参数。
     *
     * @param taskName 任务名称
     * @return 新的 Builder
     */
    public static Builder builder(String taskName) {
        return new Builder().taskName(taskName);
    }

    /**
     * 基于已有配置复制出一个 Builder（用于局部修改后重建）。
     *
     * @param copy 源配置
     * @return 复制了源配置字段的 Builder
     */
    public static Builder builder(TaskRunnerConfig copy) {
        Builder builder = new Builder();
        builder.id = copy.getId();
        builder.taskName = copy.getTaskName();
        builder.taskDesc = copy.getTaskDesc();
        builder.taskClass = copy.getTaskClass();
        builder.taskTag = copy.getTaskTag();
        builder.queueType = copy.getQueueType();
        builder.consumerNum = copy.getConsumerNum();
        builder.prefetchNum = copy.getPrefetchNum();
        builder.rateLimitType = copy.getRateLimitType();
        builder.rateLimitValue = copy.getRateLimitValue();
        builder.rateLimitTime = copy.getRateLimitTime();
        builder.rateLimitWait = copy.getRateLimitWait();
        builder.retryTimesByOverrated = copy.getRetryTimesByOverrated();
        builder.retryTimesByPartner = copy.getRetryTimesByPartner();
        builder.retryTimesByProgram = copy.getRetryTimesByProgram();
        builder.runTarget = copy.getRunTarget();
        builder.alertFailRate = copy.getAlertFailRate();
        builder.alertFailPartnerRate = copy.getAlertFailPartnerRate();
        builder.alertFailProgramRate = copy.getAlertFailProgramRate();
        builder.alertFailConfigRate = copy.getAlertFailConfigRate();
        builder.alertFailDataRate = copy.getAlertFailDataRate();
        builder.alertQueueOversize = copy.getAlertQueueOversize();
        builder.alertQueueTimeout = copy.getAlertQueueTimeout();
        builder.alertWaitTimeout = copy.getAlertWaitTimeout();
        builder.alertRunTimeout = copy.getAlertRunTimeout();
        builder.createDate = copy.getCreateDate();
        builder.modifyDate = copy.getModifyDate();
        builder.state = copy.getState();
        builder.logLevel = copy.getLogLevel();
        builder.logLimitSize = copy.getLogLimitSize();
        return builder;
    }

    /**
     * 获取任务配置 id。
     *
     * @return 任务配置 id
     */
    public long getId() {
        return id;
    }

    /**
     * 设置任务配置 id。
     *
     * @param id 任务配置 id
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * 获取任务名称。
     *
     * @return 任务名称
     */
    public String getTaskName() {
        return taskName;
    }

    /**
     * 设置任务名称。
     *
     * @param taskName 任务名称
     */
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    /**
     * 获取任务描述。
     *
     * @return 任务描述
     */
    public String getTaskDesc() {
        return taskDesc;
    }

    /**
     * 设置任务描述。
     *
     * @param taskDesc 任务描述
     */
    public void setTaskDesc(String taskDesc) {
        this.taskDesc = taskDesc;
    }

    /**
     * 获取执行类信息。
     *
     * @return 执行类信息
     */
    public String getTaskClass() {
        return taskClass;
    }

    /**
     * 设置执行类信息。
     *
     * @param taskClass 执行类信息
     */
    public void setTaskClass(String taskClass) {
        this.taskClass = taskClass;
    }

    /**
     * 获取执行类TAG，可能用于区分子任务。
     *
     * @return 执行类TAG，可能用于区分子任务
     */
    public String getTaskTag() {
        return taskTag;
    }

    /**
     * 设置执行类TAG，可能用于区分子任务。
     *
     * @param taskTag 执行类TAG，可能用于区分子任务
     */
    public void setTaskTag(String taskTag) {
        this.taskTag = taskTag;
    }

    /**
     * 获取队列形态：QUEUE_TYPE_CLASSIC / QUEUE_TYPE_QUORUM。<p>默认 quorum（用户定案 261120）：Raft 复制安全、原生延迟消息唯一载体；追求极致吞吐且接受单副本风险时选 classic（classic 队列的消息默认非持久化不写盘，见 TaskFactory.buildTaskQueueMessage）。</p>。
     *
     * @return 队列形态：QUEUE_TYPE_CLASSIC / QUEUE_TYPE_QUORUM。<p>默认 quorum（用户定案 261120）：Raft 复制安全、原生延迟消息唯一载体；追求极致吞吐且接受单副本风险时选 classic（classic 队列的消息默认非持久化不写盘，见 TaskFactory.buildTaskQueueMessage）。</p>
     */
    public int getQueueType() {
        return queueType;
    }

    /**
     * 设置队列形态：QUEUE_TYPE_CLASSIC / QUEUE_TYPE_QUORUM。<p>默认 quorum（用户定案 261120）：Raft 复制安全、原生延迟消息唯一载体；追求极致吞吐且接受单副本风险时选 classic（classic 队列的消息默认非持久化不写盘，见 TaskFactory.buildTaskQueueMessage）。</p>。
     *
     * @param queueType 队列形态：QUEUE_TYPE_CLASSIC / QUEUE_TYPE_QUORUM。<p>默认 quorum（用户定案 261120）：Raft 复制安全、原生延迟消息唯一载体；追求极致吞吐且接受单副本风险时选 classic（classic 队列的消息默认非持久化不写盘，见 TaskFactory.buildTaskQueueMessage）。</p>
     */
    public void setQueueType(int queueType) {
        this.queueType = queueType;
    }

    /**
     * 获取消费者的数量。
     *
     * @return 消费者的数量
     */
    public int getConsumerNum() {
        return consumerNum;
    }

    /**
     * 设置消费者的数量。
     *
     * @param consumerNum 消费者的数量
     */
    public void setConsumerNum(int consumerNum) {
        this.consumerNum = consumerNum;
    }

    /**
     * 获取预取任务数。
     *
     * @return 预取任务数
     */
    public int getPrefetchNum() {
        return prefetchNum;
    }

    /**
     * 设置预取任务数。
     *
     * @param prefetchNum 预取任务数
     */
    public void setPrefetchNum(int prefetchNum) {
        this.prefetchNum = prefetchNum;
    }

    /**
     * 获取详见流量限制类型说明。
     *
     * @return 详见流量限制类型说明
     */
    public int getRateLimitType() {
        return rateLimitType;
    }

    /**
     * 设置详见流量限制类型说明。
     *
     * @param rateLimitType 详见流量限制类型说明
     */
    public void setRateLimitType(int rateLimitType) {
        this.rateLimitType = rateLimitType;
    }

    /**
     * 获取流量限定数值，默认为10次。
     *
     * @return 流量限定数值，默认为10次
     */
    public int getRateLimitValue() {
        return rateLimitValue;
    }

    /**
     * 设置流量限定数值，默认为10次。
     *
     * @param rateLimitValue 流量限定数值，默认为10次
     */
    public void setRateLimitValue(int rateLimitValue) {
        this.rateLimitValue = rateLimitValue;
    }

    /**
     * 获取流量限定时间(S)，默认为1秒。
     *
     * @return 流量限定时间(S)，默认为1秒
     */
    public int getRateLimitTime() {
        return rateLimitTime;
    }

    /**
     * 设置流量限定时间(S)，默认为1秒。
     *
     * @param rateLimitTime 流量限定时间(S)，默认为1秒
     */
    public void setRateLimitTime(int rateLimitTime) {
        this.rateLimitTime = rateLimitTime;
    }

    /**
     * 获取当发生流量限制时，等待的秒数，默认30秒。
     *
     * @return 当发生流量限制时，等待的秒数，默认30秒
     */
    public int getRateLimitWait() {
        return rateLimitWait;
    }

    /**
     * 设置当发生流量限制时，等待的秒数，默认30秒。
     *
     * @param rateLimitWait 当发生流量限制时，等待的秒数，默认30秒
     */
    public void setRateLimitWait(int rateLimitWait) {
        this.rateLimitWait = rateLimitWait;
    }

    /**
     * 获取超过流量限制时的<b>额外</b>重试次数，默认 0 表示不重试、直接放弃任务。<p>实现说明（见 {@code TaskRunnerContainer.process()}）：以 {@code ranTimes <= retryTimesByOverrated}作为是否继续重试的判定，{@code ranTimes} 在每次进入 process 时自增（含首次执行）。设为 N 时，任务总计执行 {@code N+1} 次（1 次初始 + N 次重试）；N=0 时仅执行 1 次不重试。</p>。
     *
     * @return 超过流量限制时的<b>额外</b>重试次数，默认 0 表示不重试、直接放弃任务。<p>实现说明（见 {@code TaskRunnerContainer.process()}）：以 {@code ranTimes <= retryTimesByOverrated}作为是否继续重试的判定，{@code ranTimes} 在每次进入 process 时自增（含首次执行）。设为 N 时，任务总计执行 {@code N+1} 次（1 次初始 + N 次重试）；N=0 时仅执行 1 次不重试。</p>
     */
    public int getRetryTimesByOverrated() {
        return retryTimesByOverrated;
    }

    /**
     * 设置超过流量限制时的<b>额外</b>重试次数，默认 0 表示不重试、直接放弃任务。<p>实现说明（见 {@code TaskRunnerContainer.process()}）：以 {@code ranTimes <= retryTimesByOverrated}作为是否继续重试的判定，{@code ranTimes} 在每次进入 process 时自增（含首次执行）。设为 N 时，任务总计执行 {@code N+1} 次（1 次初始 + N 次重试）；N=0 时仅执行 1 次不重试。</p>。
     *
     * @param retryTimesByOverrated 超过流量限制时的<b>额外</b>重试次数，默认 0 表示不重试、直接放弃任务。<p>实现说明（见 {@code TaskRunnerContainer.process()}）：以 {@code ranTimes <= retryTimesByOverrated}作为是否继续重试的判定，{@code ranTimes} 在每次进入 process 时自增（含首次执行）。设为 N 时，任务总计执行 {@code N+1} 次（1 次初始 + N 次重试）；N=0 时仅执行 1 次不重试。</p>
     */
    public void setRetryTimesByOverrated(int retryTimesByOverrated) {
        this.retryTimesByOverrated = retryTimesByOverrated;
    }

    /**
     * 获取对方接口错误时的<b>额外</b>重试次数，默认 0 表示不重试、直接放弃任务。<p>同 {@link #retryTimesByOverrated}，设为 N 时总计执行 N+1 次（1 次初始 + N 次重试）。</p>。
     *
     * @return 对方接口错误时的<b>额外</b>重试次数，默认 0 表示不重试、直接放弃任务。<p>同 {@link #retryTimesByOverrated}，设为 N 时总计执行 N+1 次（1 次初始 + N 次重试）。</p>
     */
    public int getRetryTimesByPartner() {
        return retryTimesByPartner;
    }

    /**
     * 设置对方接口错误时的<b>额外</b>重试次数，默认 0 表示不重试、直接放弃任务。<p>同 {@link #retryTimesByOverrated}，设为 N 时总计执行 N+1 次（1 次初始 + N 次重试）。</p>。
     *
     * @param retryTimesByPartner 对方接口错误时的<b>额外</b>重试次数，默认 0 表示不重试、直接放弃任务。<p>同 {@link #retryTimesByOverrated}，设为 N 时总计执行 N+1 次（1 次初始 + N 次重试）。</p>
     */
    public void setRetryTimesByPartner(int retryTimesByPartner) {
        this.retryTimesByPartner = retryTimesByPartner;
    }

    /**
     * 获取程序自身异常时的<b>额外</b>重试次数，默认 0 表示不重试、直接放弃任务。<p>同 {@link #retryTimesByOverrated}，设为 N 时总计执行 N+1 次（1 次初始 + N 次重试）。程序异常（NPE/状态错等）也可配置自动重试。</p>。
     *
     * @return 程序自身异常时的<b>额外</b>重试次数，默认 0 表示不重试、直接放弃任务。<p>同 {@link #retryTimesByOverrated}，设为 N 时总计执行 N+1 次（1 次初始 + N 次重试）。程序异常（NPE/状态错等）也可配置自动重试。</p>
     */
    public int getRetryTimesByProgram() {
        return retryTimesByProgram;
    }

    /**
     * 设置程序自身异常时的<b>额外</b>重试次数，默认 0 表示不重试、直接放弃任务。<p>同 {@link #retryTimesByOverrated}，设为 N 时总计执行 N+1 次（1 次初始 + N 次重试）。程序异常（NPE/状态错等）也可配置自动重试。</p>。
     *
     * @param retryTimesByProgram 程序自身异常时的<b>额外</b>重试次数，默认 0 表示不重试、直接放弃任务。<p>同 {@link #retryTimesByOverrated}，设为 N 时总计执行 N+1 次（1 次初始 + N 次重试）。程序异常（NPE/状态错等）也可配置自动重试。</p>
     */
    public void setRetryTimesByProgram(int retryTimesByProgram) {
        this.retryTimesByProgram = retryTimesByProgram;
    }

    /**
     * 获取运行目标，不用指定，由程序运行期配置。
     *
     * @return 运行目标，不用指定，由程序运行期配置
     */
    public String getRunTarget() {
        return runTarget;
    }

    /**
     * 设置运行目标，不用指定，由程序运行期配置。
     *
     * @param runTarget 运行目标，不用指定，由程序运行期配置
     */
    public void setRunTarget(String runTarget) {
        this.runTarget = runTarget;
    }

    /**
     * 获取失败率。
     *
     * @return 失败率
     */
    public int getAlertFailRate() {
        return alertFailRate;
    }

    /**
     * 设置失败率。
     *
     * @param alertFailRate 失败率
     */
    public void setAlertFailRate(int alertFailRate) {
        this.alertFailRate = alertFailRate;
    }

    /**
     * 获取接口失败率。
     *
     * @return 接口失败率
     */
    public int getAlertFailPartnerRate() {
        return alertFailPartnerRate;
    }

    /**
     * 设置接口失败率。
     *
     * @param alertFailPartnerRate 接口失败率
     */
    public void setAlertFailPartnerRate(int alertFailPartnerRate) {
        this.alertFailPartnerRate = alertFailPartnerRate;
    }

    /**
     * 获取程序失败率。
     *
     * @return 程序失败率
     */
    public int getAlertFailProgramRate() {
        return alertFailProgramRate;
    }

    /**
     * 设置程序失败率。
     *
     * @param alertFailProgramRate 程序失败率
     */
    public void setAlertFailProgramRate(int alertFailProgramRate) {
        this.alertFailProgramRate = alertFailProgramRate;
    }

    /**
     * 获取数据失败率。
     *
     * @return 数据失败率
     */
    public int getAlertFailDataRate() {
        return alertFailDataRate;
    }

    /**
     * 设置数据失败率。
     *
     * @param alertFailDataRate 数据失败率
     */
    public void setAlertFailDataRate(int alertFailDataRate) {
        this.alertFailDataRate = alertFailDataRate;
    }

    /**
     * 获取配置失败率。
     *
     * @return 配置失败率
     */
    public int getAlertFailConfigRate() {
        return alertFailConfigRate;
    }

    /**
     * 设置配置失败率。
     *
     * @param alertFailConfigRate 配置失败率
     */
    public void setAlertFailConfigRate(int alertFailConfigRate) {
        this.alertFailConfigRate = alertFailConfigRate;
    }

    /**
     * 获取队列超长度。
     *
     * @return 队列超长度
     */
    public int getAlertQueueOversize() {
        return alertQueueOversize;
    }

    /**
     * 设置队列超长度。
     *
     * @param alertQueueOversize 队列超长度
     */
    public void setAlertQueueOversize(int alertQueueOversize) {
        this.alertQueueOversize = alertQueueOversize;
    }

    /**
     * 获取队列等待超时。
     *
     * @return 队列等待超时
     */
    public int getAlertQueueTimeout() {
        return alertQueueTimeout;
    }

    /**
     * 设置队列等待超时。
     *
     * @param alertQueueTimeout 队列等待超时
     */
    public void setAlertQueueTimeout(int alertQueueTimeout) {
        this.alertQueueTimeout = alertQueueTimeout;
    }

    /**
     * 获取等待超时。
     *
     * @return 等待超时
     */
    public int getAlertWaitTimeout() {
        return alertWaitTimeout;
    }

    /**
     * 设置等待超时。
     *
     * @param alertWaitTimeout 等待超时
     */
    public void setAlertWaitTimeout(int alertWaitTimeout) {
        this.alertWaitTimeout = alertWaitTimeout;
    }

    /**
     * 获取运行超时。
     *
     * @return 运行超时
     */
    public int getAlertRunTimeout() {
        return alertRunTimeout;
    }

    /**
     * 设置运行超时。
     *
     * @param alertRunTimeout 运行超时
     */
    public void setAlertRunTimeout(int alertRunTimeout) {
        this.alertRunTimeout = alertRunTimeout;
    }

    /**
     * 获取创建日期。
     *
     * @return 创建日期
     */
    public Date getCreateDate() {
        return createDate;
    }

    /**
     * 设置创建日期。
     *
     * @param createDate 创建日期
     */
    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    /**
     * 获取修改日期。
     *
     * @return 修改日期
     */
    public Date getModifyDate() {
        return modifyDate;
    }

    /**
     * 设置修改日期。
     *
     * @param modifyDate 修改日期
     */
    public void setModifyDate(Date modifyDate) {
        this.modifyDate = modifyDate;
    }

    /**
     * 获取状态值。
     *
     * @return 状态值
     */
    public int getState() {
        return state;
    }

    /**
     * 设置状态值。
     *
     * @param state 状态值
     */
    public void setState(int state) {
        this.state = state;
    }

    /**
     * 获取详见日志类型说明。
     *
     * @return 详见日志类型说明
     */
    public int getLogLevel() {
        return logLevel;
    }

    /**
     * 设置详见日志类型说明。
     *
     * @param logLevel 详见日志类型说明
     */
    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    /**
     * 获取日志字符串字段大小限制: 0 表示无限制。
     *
     * @return 日志字符串字段大小限制: 0 表示无限制
     */
    public int getLogLimitSize() {
        return logLimitSize;
    }

    /**
     * 设置日志字符串字段大小限制: 0 表示无限制。
     *
     * @param logLimitSize 日志字符串字段大小限制: 0 表示无限制
     */
    public void setLogLimitSize(int logLimitSize) {
        this.logLimitSize = logLimitSize;
    }

    /**
     * {@code TaskRunnerConfig} builder static inner class.
     */
    public static final class Builder {

        /**
         * 任务配置 id（服务端分配）。
         */
        private long id;

        /**
         * 任务名称
         */
        private String taskName;

        /**
         * 任务描述
         */
        private String taskDesc;

        /**
         * 执行类信息
         */
        private String taskClass;

        /**
         * 执行类TAG，可能用于区分子任务
         */
        private String taskTag;

        /**
         * 任务队列类型。
         */
        private int queueType = QUEUE_TYPE_QUORUM;

        /**
         * 延迟类型。
         */

        /**
         * 消费者的数量
         */
        private int consumerNum = DEFAULT_CONSUMER_NUM;

        /**
         * 预取任务数。
         */
        private int prefetchNum = DEFAULT_PREFETCH_NUM;

        /**
         * 详见流量限制类型说明。
         */
        private int rateLimitType = RATE_LIMIT_NONE;

        /**
         * 流量限定数值，默认为10次
         */
        private int rateLimitValue = DEFAULT_RATE_LIMIT_VALUE;

        /**
         * 流量限定时间(S)，默认为1秒
         */
        private int rateLimitTime = DEFAULT_RATE_LIMIT_TIME;

        /**
         * 当发生流量限制时，等待的秒数，默认30秒
         */
        private int rateLimitWait = DEFAULT_RATE_LIMIT_WAIT;

        /**
         * 超过流量限制重试次数，默认不再重试，放弃任务。
         */
        private int retryTimesByOverrated = 0;

        /**
         * 对方接口错误重试次数，默认不再重试，放弃任务。
         */
        private int retryTimesByPartner = 0;

        /**
         * 程序异常重试次数，默认不再重试，放弃任务。
         */
        private int retryTimesByProgram = 0;

        /**
         * 运行目标，不用指定，由程序运行期配置。
         */
        private String runTarget = DEFAULT_RUN_TARGET;

        /**
         * 失败率
         */
        private int alertFailRate;

        /**
         * 接口失败率
         */
        private int alertFailPartnerRate;

        /**
         * 程序失败率
         */
        private int alertFailProgramRate;

        /**
         * 配置失败率
         */
        private int alertFailConfigRate;

        /**
         * 数据失败率
         */
        private int alertFailDataRate;

        /**
         * 队列超长度
         */
        private int alertQueueOversize;

        /**
         * 队列等待超时
         */
        private int alertQueueTimeout;

        /**
         * 等待超时
         */
        private int alertWaitTimeout;

        /**
         * 运行超时
         */
        private int alertRunTimeout;

        /**
         * 创建日期。
         */
        private Date createDate;

        /**
         * 修改日期。
         */
        private Date modifyDate;

        /**
         * 状态值
         */
        private int state = 1;

        /**
         * 详见日志类型说明
         */
        private int logLevel = TASK_LOG_TYPE_RECORD;

        /**
         * 日志字符串字段大小限制: 0 表示无限制
         */
        private int logLimitSize = 0;

        /**
         * Builder 构造器（仅经 builder(...) 工厂方法创建）。
         */
        private Builder() {
        }

        /**
         * builder模式，带taskClass参数。
         *
         * @param taskClass 执行类信息
         * @return 新的 Builder
         */
        public static Builder builder(String taskClass) {
            return new Builder().taskClass(taskClass);
        }

        /**
         * builder模式，带taskClass参数。
         *
         * @param taskClass 执行类信息
         * @return 新的 Builder
         */
        public static Builder builder(Class taskClass) {
            return new Builder().taskClass(taskClass.getName());
        }

        /**
         * 设置任务名称。
         *
         * @param taskName 任务名称
         * @return this（链式调用）
         */
        public Builder taskName(String taskName) {
            this.taskName = taskName;
            return this;
        }

        /**
         * 设置任务描述。
         *
         * @param taskDesc 任务描述
         * @return this（链式调用）
         */
        public Builder taskDesc(String taskDesc) {
            this.taskDesc = taskDesc;
            return this;
        }

        /**
         * 设置执行类信息。
         *
         * @param taskClass 执行类信息
         * @return this（链式调用）
         */
        public Builder taskClass(String taskClass) {
            this.taskClass = taskClass;
            return this;
        }

        /**
         * 设置执行类TAG，可能用于区分子任务。
         *
         * @param taskTag 执行类TAG，可能用于区分子任务
         * @return this（链式调用）
         */
        public Builder taskTag(String taskTag) {
            this.taskTag = taskTag;
            return this;
        }

        /**
         * 设置队列形态：QUEUE_TYPE_CLASSIC / QUEUE_TYPE_QUORUM。<p>默认 quorum（用户定案 261120）：Raft 复制安全、原生延迟消息唯一载体；追求极致吞吐且接受单副本风险时选 classic（classic 队列的消息默认非持久化不写盘，见 TaskFactory.buildTaskQueueMessage）。</p>。
         *
         * @param queueType 队列形态：QUEUE_TYPE_CLASSIC / QUEUE_TYPE_QUORUM。<p>默认 quorum（用户定案 261120）：Raft 复制安全、原生延迟消息唯一载体；追求极致吞吐且接受单副本风险时选 classic（classic 队列的消息默认非持久化不写盘，见 TaskFactory.buildTaskQueueMessage）。</p>
         * @return this（链式调用）
         */
        public Builder queueType(int queueType) {
            this.queueType = queueType;
            return this;
        }

        /**
         * 设置消费者的数量。
         *
         * @param consumerNum 消费者的数量
         * @return this（链式调用）
         */
        public Builder consumerNum(int consumerNum) {
            this.consumerNum = consumerNum;
            return this;
        }

        /**
         * 设置预取任务数。
         *
         * @param prefetchNum 预取任务数
         * @return this（链式调用）
         */
        public Builder prefetchNum(int prefetchNum) {
            this.prefetchNum = prefetchNum;
            return this;
        }

        /**
         * 设置详见流量限制类型说明。
         *
         * @param rateLimitType 详见流量限制类型说明
         * @return this（链式调用）
         */
        public Builder rateLimitType(int rateLimitType) {
            this.rateLimitType = rateLimitType;
            return this;
        }

        /**
         * 设置流量限定数值，默认为10次。
         *
         * @param rateLimitValue 流量限定数值，默认为10次
         * @return this（链式调用）
         */
        public Builder rateLimitValue(int rateLimitValue) {
            this.rateLimitValue = rateLimitValue;
            return this;
        }

        /**
         * 设置流量限定时间(S)，默认为1秒。
         *
         * @param rateLimitTime 流量限定时间(S)，默认为1秒
         * @return this（链式调用）
         */
        public Builder rateLimitTime(int rateLimitTime) {
            this.rateLimitTime = rateLimitTime;
            return this;
        }

        /**
         * 设置当发生流量限制时，等待的秒数，默认30秒。
         *
         * @param rateLimitWait 当发生流量限制时，等待的秒数，默认30秒
         * @return this（链式调用）
         */
        public Builder rateLimitWait(int rateLimitWait) {
            this.rateLimitWait = rateLimitWait;
            return this;
        }

        /**
         * 设置超过流量限制时的<b>额外</b>重试次数，默认 0 表示不重试、直接放弃任务。<p>实现说明（见 {@code TaskRunnerContainer.process()}）：以 {@code ranTimes <= retryTimesByOverrated}作为是否继续重试的判定，{@code ranTimes} 在每次进入 process 时自增（含首次执行）。设为 N 时，任务总计执行 {@code N+1} 次（1 次初始 + N 次重试）；N=0 时仅执行 1 次不重试。</p>。
         *
         * @param retryTimesByOverrated 超过流量限制时的<b>额外</b>重试次数，默认 0 表示不重试、直接放弃任务。<p>实现说明（见 {@code TaskRunnerContainer.process()}）：以 {@code ranTimes <= retryTimesByOverrated}作为是否继续重试的判定，{@code ranTimes} 在每次进入 process 时自增（含首次执行）。设为 N 时，任务总计执行 {@code N+1} 次（1 次初始 + N 次重试）；N=0 时仅执行 1 次不重试。</p>
         * @return this（链式调用）
         */
        public Builder retryTimesByOverrated(int retryTimesByOverrated) {
            this.retryTimesByOverrated = retryTimesByOverrated;
            return this;
        }

        /**
         * 设置对方接口错误时的额外重试次数。
         *
         * @param retryTimesByPartner 对方接口错误时的额外重试次数
         * @return this（链式调用）
         */
        public Builder retryTimesByPartner(int retryTimesByPartner) {
            this.retryTimesByPartner = retryTimesByPartner;
            return this;
        }

        /**
         * 设置程序自身异常时的额外重试次数。
         *
         * @param retryTimesByProgram 程序自身异常时的额外重试次数
         * @return this（链式调用）
         */
        public Builder retryTimesByProgram(int retryTimesByProgram) {
            this.retryTimesByProgram = retryTimesByProgram;
            return this;
        }

        /**
         * 设置运行目标，不用指定，由程序运行期配置。
         *
         * @param runTarget 运行目标，不用指定，由程序运行期配置
         * @return this（链式调用）
         */
        public Builder runTarget(String runTarget) {
            this.runTarget = runTarget;
            return this;
        }

        /**
         * 设置失败率。
         *
         * @param alertFailRate 失败率
         * @return this（链式调用）
         */
        public Builder alertFailRate(int alertFailRate) {
            this.alertFailRate = alertFailRate;
            return this;
        }

        /**
         * 设置接口失败率。
         *
         * @param alertFailPartnerRate 接口失败率
         * @return this（链式调用）
         */
        public Builder alertFailPartnerRate(int alertFailPartnerRate) {
            this.alertFailPartnerRate = alertFailPartnerRate;
            return this;
        }

        /**
         * 设置程序失败率。
         *
         * @param alertFailProgramRate 程序失败率
         * @return this（链式调用）
         */
        public Builder alertFailProgramRate(int alertFailProgramRate) {
            this.alertFailProgramRate = alertFailProgramRate;
            return this;
        }

        /**
         * 设置配置失败率。
         *
         * @param alertFailConfigRate 配置失败率
         * @return this（链式调用）
         */
        public Builder alertFailConfigRate(int alertFailConfigRate) {
            this.alertFailConfigRate = alertFailConfigRate;
            return this;
        }

        /**
         * 设置数据失败率。
         *
         * @param alertFailDataRate 数据失败率
         * @return this（链式调用）
         */
        public Builder alertFailDataRate(int alertFailDataRate) {
            this.alertFailDataRate = alertFailDataRate;
            return this;
        }

        /**
         * 设置队列超长度。
         *
         * @param alertQueueOversize 队列超长度
         * @return this（链式调用）
         */
        public Builder alertQueueOversize(int alertQueueOversize) {
            this.alertQueueOversize = alertQueueOversize;
            return this;
        }

        /**
         * 设置队列等待超时。
         *
         * @param alertQueueTimeout 队列等待超时
         * @return this（链式调用）
         */
        public Builder alertQueueTimeout(int alertQueueTimeout) {
            this.alertQueueTimeout = alertQueueTimeout;
            return this;
        }

        /**
         * 设置等待超时。
         *
         * @param alertWaitTimeout 等待超时
         * @return this（链式调用）
         */
        public Builder alertWaitTimeout(int alertWaitTimeout) {
            this.alertWaitTimeout = alertWaitTimeout;
            return this;
        }

        /**
         * 设置运行超时。
         *
         * @param alertRunTimeout 运行超时
         * @return this（链式调用）
         */
        public Builder alertRunTimeout(int alertRunTimeout) {
            this.alertRunTimeout = alertRunTimeout;
            return this;
        }

        /**
         * 设置创建日期。
         *
         * @param createDate 创建日期
         * @return this（链式调用）
         */
        public Builder createDate(Date createDate) {
            this.createDate = createDate;
            return this;
        }

        /**
         * 设置修改日期。
         *
         * @param modifyDate 修改日期
         * @return this（链式调用）
         */
        public Builder modifyDate(Date modifyDate) {
            this.modifyDate = modifyDate;
            return this;
        }

        /**
         * 设置状态值。
         *
         * @param state 状态值
         * @return this（链式调用）
         */
        public Builder state(int state) {
            this.state = state;
            return this;
        }

        /**
         * 设置详见日志类型说明。
         *
         * @param logLevel 详见日志类型说明
         * @return this（链式调用）
         */
        public Builder logLevel(int logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        /**
         * 设置日志字符串字段大小限制: 0 表示无限制。
         *
         * @param logLimitSize 日志字符串字段大小限制: 0 表示无限制
         * @return this（链式调用）
         */
        public Builder logLimitSize(int logLimitSize) {
            this.logLimitSize = logLimitSize;
            return this;
        }

        /**
         * 按已设置的参数构建实例。
         *
         * @return 构建出的对象
         */
        public TaskRunnerConfig build() {
            return new TaskRunnerConfig(this);
        }
    }
}
