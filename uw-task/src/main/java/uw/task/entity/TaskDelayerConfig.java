package uw.task.entity;

import java.io.Serializable;
import java.util.Date;

/**
 * 延迟任务配置（TaskDelayerData），uw-task 与 TaskRunner/TaskCroner 对等的第三种任务类型。
 *
 * <p>由 {@code TaskDelayer.initConfig()} 产出默认值，首次注册时上传到 task-center，
 * 之后以服务端下发的动态配置为准。多实例区分三元组与 TaskRunner 一致：taskClass + taskTag + runTarget。</p>
 *
 * <p>关键配置：consumerNum（执行并发）、rateLimit*（限速，复用 TaskRunner 的限速体系）、
 * retryTimes*（业务失败重试）、pollInterval/prefetchNum（zset poll 节奏与批量）、
 * alert*（报警阈值，alertDelayOvertime 为延迟任务特有的"实际执行晚于 runAt"超时指标）、logLevel（日志）。</p>
 *
 * <p>限速类型常量复用 {@link TaskRunnerConfig#RATE_LIMIT_NONE} 等系列；日志类型常量复用
 * {@link TaskRunnerConfig#TASK_LOG_TYPE_RECORD} 等系列，语义与 TaskRunner 完全一致。</p>
 *
 * @author axeon
 */
public class TaskDelayerConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 配置主键 id（由 task-center 分配，0 表示尚未注册到服务端）。
     */
    private long id;

    /**
     * 任务名称。
     */
    private String taskName;

    /**
     * 任务描述。
     */
    private String taskDesc;

    /**
     * 执行类全限定名（TaskDelayer 实现类），由框架强制设置。
     */
    private String taskClass;

    /**
     * 任务标签，用于区分子任务/多实例。
     */
    private String taskTag;

    /**
     * 运行目标，由框架运行期设置。
     */
    private String runTarget = "default";

    /**
     * poll 间隔秒数（poll 命中数据则连续 poll，空才 sleep 此值）。
     */
    private long pollInterval = 3;

    /**
     * 单次 poll 最大条数（实际取 min(此值, 空闲并发许可数)）。
     */
    private int prefetchNum = 50;

    /**
     * 执行线程池大小（每实例并发执行到期任务数）。
     */
    private int consumerNum = 1;

    /**
     * 限速类型，取 {@link TaskRunnerConfig#RATE_LIMIT_NONE} 等常量。
     */
    private int rateLimitType = TaskRunnerConfig.RATE_LIMIT_NONE;

    /**
     * 限速窗口内配额上限。
     */
    private int rateLimitValue = 10;

    /**
     * 限速窗口长度（秒）。
     */
    private int rateLimitTime = 1;

    /**
     * 限速不足时最长等待秒数。
     */
    private int rateLimitWait = 30;

    /**
     * 连续限速超限后放弃的次数上限（防死循环），超过则标记 STATE_FAIL_CONFIG。
     */
    private int retryTimesByOverrated = 3;

    /**
     * 合作方异常额外重试次数（N 时总计执行 N+1 次）。
     */
    private int retryTimesByPartner = 0;

    /**
     * 程序异常额外重试次数。
     */
    private int retryTimesByProgram = 0;

    /**
     * 实际执行晚于 runAt 的平均超时报警阈值（毫秒）。
     */
    private int alertDelayOvertime;

    /**
     * 总失败率报警阈值（百分比）。
     */
    private int alertFailRate;

    /**
     * 程序失败率报警阈值。
     */
    private int alertFailProgramRate;

    /**
     * 合作方失败率报警阈值。
     */
    private int alertFailPartnerRate;

    /**
     * 平均运行耗时报警阈值（毫秒）。
     */
    private int alertRunTimeout;

    /**
     * 日志类型，取 {@link TaskRunnerConfig#TASK_LOG_TYPE_RECORD} 等常量。
     */
    private int logLevel = TaskRunnerConfig.TASK_LOG_TYPE_RECORD;

    /**
     * 日志字符串字段大小限制，0 表示无限制。
     */
    private int logLimitSize = 0;

    /**
     * 状态值，1=启用。
     */
    private int state = 1;

    /**
     * 创建日期。
     */
    private Date createDate;

    /**
     * 修改日期。
     */
    private Date modifyDate;

    public TaskDelayerConfig() {
    }

    public TaskDelayerConfig(String taskName) {
        this.taskName = taskName;
    }

    private TaskDelayerConfig(Builder builder) {
        setId(builder.id);
        setTaskName(builder.taskName);
        setTaskDesc(builder.taskDesc);
        setTaskClass(builder.taskClass);
        setTaskTag(builder.taskTag);
        setRunTarget(builder.runTarget);
        setConsumerNum(builder.consumerNum);
        setPollInterval(builder.pollInterval);
        setPrefetchNum(builder.prefetchNum);
        setRateLimitType(builder.rateLimitType);
        setRateLimitValue(builder.rateLimitValue);
        setRateLimitTime(builder.rateLimitTime);
        setRateLimitWait(builder.rateLimitWait);
        setRetryTimesByOverrated(builder.retryTimesByOverrated);
        setRetryTimesByPartner(builder.retryTimesByPartner);
        setRetryTimesByProgram(builder.retryTimesByProgram);
        setAlertDelayOvertime(builder.alertDelayOvertime);
        setAlertFailRate(builder.alertFailRate);
        setAlertFailProgramRate(builder.alertFailProgramRate);
        setAlertFailPartnerRate(builder.alertFailPartnerRate);
        setAlertRunTimeout(builder.alertRunTimeout);
        setLogLevel(builder.logLevel);
        setLogLimitSize(builder.logLimitSize);
        setState(builder.state);
        setCreateDate(builder.createDate);
        setModifyDate(builder.modifyDate);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(String taskName) {
        return new Builder().taskName(taskName);
    }

    /**
     * 基于已有配置复制出一个 Builder（用于局部修改后重建）。
     */
    public static Builder builder(TaskDelayerConfig copy) {
        Builder builder = new Builder();
        builder.id = copy.getId();
        builder.taskName = copy.getTaskName();
        builder.taskDesc = copy.getTaskDesc();
        builder.taskClass = copy.getTaskClass();
        builder.taskTag = copy.getTaskTag();
        builder.runTarget = copy.getRunTarget();
        builder.consumerNum = copy.getConsumerNum();
        builder.pollInterval = copy.getPollInterval();
        builder.prefetchNum = copy.getPrefetchNum();
        builder.rateLimitType = copy.getRateLimitType();
        builder.rateLimitValue = copy.getRateLimitValue();
        builder.rateLimitTime = copy.getRateLimitTime();
        builder.rateLimitWait = copy.getRateLimitWait();
        builder.retryTimesByOverrated = copy.getRetryTimesByOverrated();
        builder.retryTimesByPartner = copy.getRetryTimesByPartner();
        builder.retryTimesByProgram = copy.getRetryTimesByProgram();
        builder.alertDelayOvertime = copy.getAlertDelayOvertime();
        builder.alertFailRate = copy.getAlertFailRate();
        builder.alertFailProgramRate = copy.getAlertFailProgramRate();
        builder.alertFailPartnerRate = copy.getAlertFailPartnerRate();
        builder.alertRunTimeout = copy.getAlertRunTimeout();
        builder.logLevel = copy.getLogLevel();
        builder.logLimitSize = copy.getLogLimitSize();
        builder.state = copy.getState();
        builder.createDate = copy.getCreateDate();
        builder.modifyDate = copy.getModifyDate();
        return builder;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskDesc() {
        return taskDesc;
    }

    public void setTaskDesc(String taskDesc) {
        this.taskDesc = taskDesc;
    }

    public String getTaskClass() {
        return taskClass;
    }

    public void setTaskClass(String taskClass) {
        this.taskClass = taskClass;
    }

    public String getTaskTag() {
        return taskTag;
    }

    public void setTaskTag(String taskTag) {
        this.taskTag = taskTag;
    }

    public String getRunTarget() {
        return runTarget;
    }

    public void setRunTarget(String runTarget) {
        this.runTarget = runTarget;
    }

    public int getConsumerNum() {
        return consumerNum;
    }

    public void setConsumerNum(int consumerNum) {
        this.consumerNum = consumerNum;
    }

    public long getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(long pollInterval) {
        this.pollInterval = pollInterval;
    }

    public int getPrefetchNum() {
        return prefetchNum;
    }

    public void setPrefetchNum(int prefetchNum) {
        this.prefetchNum = prefetchNum;
    }

    public int getRateLimitType() {
        return rateLimitType;
    }

    public void setRateLimitType(int rateLimitType) {
        this.rateLimitType = rateLimitType;
    }

    public int getRateLimitValue() {
        return rateLimitValue;
    }

    public void setRateLimitValue(int rateLimitValue) {
        this.rateLimitValue = rateLimitValue;
    }

    public int getRateLimitTime() {
        return rateLimitTime;
    }

    public void setRateLimitTime(int rateLimitTime) {
        this.rateLimitTime = rateLimitTime;
    }

    public int getRateLimitWait() {
        return rateLimitWait;
    }

    public void setRateLimitWait(int rateLimitWait) {
        this.rateLimitWait = rateLimitWait;
    }

    public int getRetryTimesByOverrated() {
        return retryTimesByOverrated;
    }

    public void setRetryTimesByOverrated(int retryTimesByOverrated) {
        this.retryTimesByOverrated = retryTimesByOverrated;
    }

    public int getRetryTimesByPartner() {
        return retryTimesByPartner;
    }

    public void setRetryTimesByPartner(int retryTimesByPartner) {
        this.retryTimesByPartner = retryTimesByPartner;
    }

    public int getRetryTimesByProgram() {
        return retryTimesByProgram;
    }

    public void setRetryTimesByProgram(int retryTimesByProgram) {
        this.retryTimesByProgram = retryTimesByProgram;
    }

    public int getAlertDelayOvertime() {
        return alertDelayOvertime;
    }

    public void setAlertDelayOvertime(int alertDelayOvertime) {
        this.alertDelayOvertime = alertDelayOvertime;
    }

    public int getAlertFailRate() {
        return alertFailRate;
    }

    public void setAlertFailRate(int alertFailRate) {
        this.alertFailRate = alertFailRate;
    }

    public int getAlertFailProgramRate() {
        return alertFailProgramRate;
    }

    public void setAlertFailProgramRate(int alertFailProgramRate) {
        this.alertFailProgramRate = alertFailProgramRate;
    }

    public int getAlertFailPartnerRate() {
        return alertFailPartnerRate;
    }

    public void setAlertFailPartnerRate(int alertFailPartnerRate) {
        this.alertFailPartnerRate = alertFailPartnerRate;
    }

    public int getAlertRunTimeout() {
        return alertRunTimeout;
    }

    public void setAlertRunTimeout(int alertRunTimeout) {
        this.alertRunTimeout = alertRunTimeout;
    }

    public int getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    public int getLogLimitSize() {
        return logLimitSize;
    }

    public void setLogLimitSize(int logLimitSize) {
        this.logLimitSize = logLimitSize;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getModifyDate() {
        return modifyDate;
    }

    public void setModifyDate(Date modifyDate) {
        this.modifyDate = modifyDate;
    }

    /**
     * {@code TaskDelayerConfig} builder static inner class.
     */
    public static final class Builder {

        private long id;
        private String taskName;
        private String taskDesc;
        private String taskClass;
        private String taskTag;
        private String runTarget = "default";
        private int consumerNum = 1;
        private long pollInterval = 3;
        private int prefetchNum = 50;
        private int rateLimitType = TaskRunnerConfig.RATE_LIMIT_NONE;
        private int rateLimitValue = 10;
        private int rateLimitTime = 1;
        private int rateLimitWait = 30;
        private int retryTimesByOverrated = 3;
        private int retryTimesByPartner = 0;
        private int retryTimesByProgram = 0;
        private int alertDelayOvertime;
        private int alertFailRate;
        private int alertFailProgramRate;
        private int alertFailPartnerRate;
        private int alertRunTimeout;
        private int logLevel = TaskRunnerConfig.TASK_LOG_TYPE_RECORD;
        private int logLimitSize = 0;
        private int state = 1;
        private Date createDate;
        private Date modifyDate;

        private Builder() {
        }

        public Builder id(long id) {
            this.id = id;
            return this;
        }

        public Builder taskName(String taskName) {
            this.taskName = taskName;
            return this;
        }

        public Builder taskDesc(String taskDesc) {
            this.taskDesc = taskDesc;
            return this;
        }

        public Builder taskClass(String taskClass) {
            this.taskClass = taskClass;
            return this;
        }

        public Builder taskClass(Class<?> taskClass) {
            this.taskClass = taskClass.getName();
            return this;
        }

        public Builder taskTag(String taskTag) {
            this.taskTag = taskTag;
            return this;
        }

        public Builder runTarget(String runTarget) {
            this.runTarget = runTarget;
            return this;
        }

        public Builder consumerNum(int consumerNum) {
            this.consumerNum = consumerNum;
            return this;
        }

        public Builder pollInterval(long pollInterval) {
            this.pollInterval = pollInterval;
            return this;
        }

        public Builder prefetchNum(int prefetchNum) {
            this.prefetchNum = prefetchNum;
            return this;
        }

        public Builder rateLimitType(int rateLimitType) {
            this.rateLimitType = rateLimitType;
            return this;
        }

        public Builder rateLimitValue(int rateLimitValue) {
            this.rateLimitValue = rateLimitValue;
            return this;
        }

        public Builder rateLimitTime(int rateLimitTime) {
            this.rateLimitTime = rateLimitTime;
            return this;
        }

        public Builder rateLimitWait(int rateLimitWait) {
            this.rateLimitWait = rateLimitWait;
            return this;
        }

        public Builder retryTimesByOverrated(int retryTimesByOverrated) {
            this.retryTimesByOverrated = retryTimesByOverrated;
            return this;
        }

        public Builder retryTimesByPartner(int retryTimesByPartner) {
            this.retryTimesByPartner = retryTimesByPartner;
            return this;
        }

        public Builder retryTimesByProgram(int retryTimesByProgram) {
            this.retryTimesByProgram = retryTimesByProgram;
            return this;
        }

        public Builder alertDelayOvertime(int alertDelayOvertime) {
            this.alertDelayOvertime = alertDelayOvertime;
            return this;
        }

        public Builder alertFailRate(int alertFailRate) {
            this.alertFailRate = alertFailRate;
            return this;
        }

        public Builder alertFailProgramRate(int alertFailProgramRate) {
            this.alertFailProgramRate = alertFailProgramRate;
            return this;
        }

        public Builder alertFailPartnerRate(int alertFailPartnerRate) {
            this.alertFailPartnerRate = alertFailPartnerRate;
            return this;
        }

        public Builder alertRunTimeout(int alertRunTimeout) {
            this.alertRunTimeout = alertRunTimeout;
            return this;
        }

        public Builder logLevel(int logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        public Builder logLimitSize(int logLimitSize) {
            this.logLimitSize = logLimitSize;
            return this;
        }

        public Builder state(int state) {
            this.state = state;
            return this;
        }

        public Builder createDate(Date createDate) {
            this.createDate = createDate;
            return this;
        }

        public Builder modifyDate(Date modifyDate) {
            this.modifyDate = modifyDate;
            return this;
        }

        public TaskDelayerConfig build() {
            return new TaskDelayerConfig(this);
        }
    }
}
