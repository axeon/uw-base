package uw.task.entity;

import java.io.Serializable;
import java.util.Date;

/**
 * taskRunnerConfig实体类。
 *
 * @author axeon
 * @version $Revision: 1.00 $ $Date: 2017-05-03 14:00:50
 */
public class TaskRunnerConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项目队列。
     */
    public static final int TYPE_QUEUE_PROJECT = 0;

    /**
     * 项目优先级队列。
     */
    public static final int TYPE_QUEUE_PROJECT_PRIORITY = 1;

    /**
     * 任务组队列。
     */
    public static final int TYPE_QUEUE_GROUP = 2;

    /**
     * 任务组优先级队列。
     */
    public static final int TYPE_QUEUE_GROUP_PRIORITY = 3;

    /**
     * 任务队列。
     */
    public static final int TYPE_QUEUE_TASK = 5;

    /**
     * 延迟任务类型。
     */
    public static final int TYPE_DELAY_ON = 1;

    /**
     * 非延迟任务类型。
     */
    public static final int TYPE_DELAY_OFF = 0;

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
    private int queueType;

    /**
     * 延迟类型。
     */
    private int delayType = TYPE_DELAY_OFF;

    /**
     * 消费者的数量
     */
    private int consumerNum = 1;

    /**
     * 预取任务数。
     */
    private int prefetchNum = 1;

    /**
     * 详见流量限制类型说明。
     */
    private int rateLimitType = RATE_LIMIT_NONE;

    /**
     * 流量限定数值，默认为10次
     */
    private int rateLimitValue = 10;

    /**
     * 流量限定时间(S)，默认为1秒
     */
    private int rateLimitTime = 1;

    /**
     * 当发生流量限制时，等待的秒数，默认300秒
     */
    private int rateLimitWait = 30;

    /**
     * 超过流量限制重试次数，默认不在重试，放弃任务。
     */
    private int retryTimesByOverrated = 0;

    /**
     * 对方接口错误重试次数，默认不再重试，放弃任务。
     */
    private int retryTimesByPartner = 0;

    /**
     * 运行目标，不用指定，由程序运行期配置。
     */
    private String runTarget = "default";

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

    public TaskRunnerConfig() {
    }

    public TaskRunnerConfig(String taskName) {
        this.taskName = taskName;
    }

    private TaskRunnerConfig(Builder builder) {
        setId(builder.id);
        setTaskName(builder.taskName);
        setTaskDesc(builder.taskDesc);
        setTaskClass(builder.taskClass);
        setTaskTag(builder.taskTag);
        setQueueType(builder.queueType);
        setDelayType(builder.delayType);
        setConsumerNum(builder.consumerNum);
        setPrefetchNum(builder.prefetchNum);
        setRateLimitType(builder.rateLimitType);
        setRateLimitValue(builder.rateLimitValue);
        setRateLimitTime(builder.rateLimitTime);
        setRateLimitWait(builder.rateLimitWait);
        setRetryTimesByOverrated(builder.retryTimesByOverrated);
        setRetryTimesByPartner(builder.retryTimesByPartner);
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

    public static Builder builder() {
        return new Builder();
    }


    /**
     * builder模式，带taskName参数。
     *
     * @param taskName
     * @return
     */
    public static Builder builder(String taskName) {
        return new Builder().taskName(taskName);
    }

    public static Builder builder(TaskRunnerConfig copy) {
        Builder builder = new Builder();
        builder.id = copy.getId();
        builder.taskName = copy.getTaskName();
        builder.taskDesc = copy.getTaskDesc();
        builder.taskClass = copy.getTaskClass();
        builder.taskTag = copy.getTaskTag();
        builder.queueType = copy.getQueueType();
        builder.delayType = copy.getDelayType();
        builder.consumerNum = copy.getConsumerNum();
        builder.prefetchNum = copy.getPrefetchNum();
        builder.rateLimitType = copy.getRateLimitType();
        builder.rateLimitValue = copy.getRateLimitValue();
        builder.rateLimitTime = copy.getRateLimitTime();
        builder.rateLimitWait = copy.getRateLimitWait();
        builder.retryTimesByOverrated = copy.getRetryTimesByOverrated();
        builder.retryTimesByPartner = copy.getRetryTimesByPartner();
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

    public int getQueueType() {
        return queueType;
    }

    public void setQueueType(int queueType) {
        this.queueType = queueType;
    }

    public int getDelayType() {
        return delayType;
    }

    public void setDelayType(int delayType) {
        this.delayType = delayType;
    }

    public int getConsumerNum() {
        return consumerNum;
    }

    public void setConsumerNum(int consumerNum) {
        this.consumerNum = consumerNum;
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

    public String getRunTarget() {
        return runTarget;
    }

    public void setRunTarget(String runTarget) {
        this.runTarget = runTarget;
    }

    public int getAlertFailRate() {
        return alertFailRate;
    }

    public void setAlertFailRate(int alertFailRate) {
        this.alertFailRate = alertFailRate;
    }

    public int getAlertFailPartnerRate() {
        return alertFailPartnerRate;
    }

    public void setAlertFailPartnerRate(int alertFailPartnerRate) {
        this.alertFailPartnerRate = alertFailPartnerRate;
    }

    public int getAlertFailProgramRate() {
        return alertFailProgramRate;
    }

    public void setAlertFailProgramRate(int alertFailProgramRate) {
        this.alertFailProgramRate = alertFailProgramRate;
    }

    public int getAlertFailDataRate() {
        return alertFailDataRate;
    }

    public void setAlertFailDataRate(int alertFailDataRate) {
        this.alertFailDataRate = alertFailDataRate;
    }

    public int getAlertFailConfigRate() {
        return alertFailConfigRate;
    }

    public void setAlertFailConfigRate(int alertFailConfigRate) {
        this.alertFailConfigRate = alertFailConfigRate;
    }

    public int getAlertQueueOversize() {
        return alertQueueOversize;
    }

    public void setAlertQueueOversize(int alertQueueOversize) {
        this.alertQueueOversize = alertQueueOversize;
    }

    public int getAlertQueueTimeout() {
        return alertQueueTimeout;
    }

    public void setAlertQueueTimeout(int alertQueueTimeout) {
        this.alertQueueTimeout = alertQueueTimeout;
    }

    public int getAlertWaitTimeout() {
        return alertWaitTimeout;
    }

    public void setAlertWaitTimeout(int alertWaitTimeout) {
        this.alertWaitTimeout = alertWaitTimeout;
    }

    public int getAlertRunTimeout() {
        return alertRunTimeout;
    }

    public void setAlertRunTimeout(int alertRunTimeout) {
        this.alertRunTimeout = alertRunTimeout;
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

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
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

    /**
     * {@code TaskRunnerConfig} builder static inner class.
     */
    public static final class Builder {

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
        private int queueType;

        /**
         * 延迟类型。
         */
        private int delayType = TYPE_DELAY_OFF;

        /**
         * 消费者的数量
         */
        private int consumerNum = 1;

        /**
         * 预取任务数。
         */
        private int prefetchNum = 1;

        /**
         * 详见流量限制类型说明。
         */
        private int rateLimitType = RATE_LIMIT_NONE;

        /**
         * 流量限定数值，默认为10次
         */
        private int rateLimitValue = 10;

        /**
         * 流量限定时间(S)，默认为1秒
         */
        private int rateLimitTime = 1;

        /**
         * 当发生流量限制时，等待的秒数，默认300秒
         */
        private int rateLimitWait = 30;

        /**
         * 超过流量限制重试次数，默认不在重试，放弃任务。
         */
        private int retryTimesByOverrated = 0;

        /**
         * 对方接口错误重试次数，默认不再重试，放弃任务。
         */
        private int retryTimesByPartner = 0;

        /**
         * 运行目标，不用指定，由程序运行期配置。
         */
        private String runTarget = "default";

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


        private Builder() {
        }

        /**
         * builder模式，带taskClass参数。
         *
         * @param taskClass
         * @return
         */
        public static Builder builder(String taskClass) {
            return new Builder().taskClass( taskClass );
        }

        /**
         * builder模式，带taskClass参数。
         *
         * @param taskClass
         * @return
         */
        public static Builder builder(Class taskClass) {
            return new Builder().taskClass( taskClass.getName() );
        }

        /**
         * Sets the {@code taskName} and returns a reference to this Builder enabling method chaining.
         *
         * @param taskName the {@code taskName} to set
         * @return a reference to this Builder
         */
        public Builder taskName(String taskName) {
            this.taskName = taskName;
            return this;
        }

        /**
         * Sets the {@code taskDesc} and returns a reference to this Builder enabling method chaining.
         *
         * @param taskDesc the {@code taskDesc} to set
         * @return a reference to this Builder
         */
        public Builder taskDesc(String taskDesc) {
            this.taskDesc = taskDesc;
            return this;
        }

        /**
         * Sets the {@code taskClass} and returns a reference to this Builder enabling method chaining.
         *
         * @param taskClass the {@code taskClass} to set
         * @return a reference to this Builder
         */
        public Builder taskClass(String taskClass) {
            this.taskClass = taskClass;
            return this;
        }

        /**
         * Sets the {@code taskTag} and returns a reference to this Builder enabling method chaining.
         *
         * @param taskTag the {@code taskTag} to set
         * @return a reference to this Builder
         */
        public Builder taskTag(String taskTag) {
            this.taskTag = taskTag;
            return this;
        }

        /**
         * Sets the {@code queueType} and returns a reference to this Builder enabling method chaining.
         *
         * @param queueType the {@code queueType} to set
         * @return a reference to this Builder
         */
        public Builder queueType(int queueType) {
            this.queueType = queueType;
            return this;
        }

        /**
         * Sets the {@code delayType} and returns a reference to this Builder enabling method chaining.
         *
         * @param delayType the {@code delayType} to set
         * @return a reference to this Builder
         */
        public Builder delayType(int delayType) {
            this.delayType = delayType;
            return this;
        }

        /**
         * Sets the {@code consumerNum} and returns a reference to this Builder enabling method chaining.
         *
         * @param consumerNum the {@code consumerNum} to set
         * @return a reference to this Builder
         */
        public Builder consumerNum(int consumerNum) {
            this.consumerNum = consumerNum;
            return this;
        }

        /**
         * Sets the {@code prefetchNum} and returns a reference to this Builder enabling method chaining.
         *
         * @param prefetchNum the {@code prefetchNum} to set
         * @return a reference to this Builder
         */
        public Builder prefetchNum(int prefetchNum) {
            this.prefetchNum = prefetchNum;
            return this;
        }

        /**
         * Sets the {@code rateLimitType} and returns a reference to this Builder enabling method chaining.
         *
         * @param rateLimitType the {@code rateLimitType} to set
         * @return a reference to this Builder
         */
        public Builder rateLimitType(int rateLimitType) {
            this.rateLimitType = rateLimitType;
            return this;
        }

        /**
         * Sets the {@code rateLimitValue} and returns a reference to this Builder enabling method chaining.
         *
         * @param rateLimitValue the {@code rateLimitValue} to set
         * @return a reference to this Builder
         */
        public Builder rateLimitValue(int rateLimitValue) {
            this.rateLimitValue = rateLimitValue;
            return this;
        }

        /**
         * Sets the {@code rateLimitTime} and returns a reference to this Builder enabling method chaining.
         *
         * @param rateLimitTime the {@code rateLimitTime} to set
         * @return a reference to this Builder
         */
        public Builder rateLimitTime(int rateLimitTime) {
            this.rateLimitTime = rateLimitTime;
            return this;
        }

        /**
         * Sets the {@code rateLimitWait} and returns a reference to this Builder enabling method chaining.
         *
         * @param rateLimitWait the {@code rateLimitWait} to set
         * @return a reference to this Builder
         */
        public Builder rateLimitWait(int rateLimitWait) {
            this.rateLimitWait = rateLimitWait;
            return this;
        }

        /**
         * Sets the {@code retryTimesByOverrated} and returns a reference to this Builder enabling method chaining.
         *
         * @param retryTimesByOverrated the {@code retryTimesByOverrated} to set
         * @return a reference to this Builder
         */
        public Builder retryTimesByOverrated(int retryTimesByOverrated) {
            this.retryTimesByOverrated = retryTimesByOverrated;
            return this;
        }

        /**
         * Sets the {@code retryTimesByPartner} and returns a reference to this Builder enabling method chaining.
         *
         * @param retryTimesByPartner the {@code retryTimesByPartner} to set
         * @return a reference to this Builder
         */
        public Builder retryTimesByPartner(int retryTimesByPartner) {
            this.retryTimesByPartner = retryTimesByPartner;
            return this;
        }

        /**
         * Sets the {@code runTarget} and returns a reference to this Builder enabling method chaining.
         *
         * @param runTarget the {@code runTarget} to set
         * @return a reference to this Builder
         */
        public Builder runTarget(String runTarget) {
            this.runTarget = runTarget;
            return this;
        }

        /**
         * Sets the {@code alertFailRate} and returns a reference to this Builder enabling method chaining.
         *
         * @param alertFailRate the {@code alertFailRate} to set
         * @return a reference to this Builder
         */
        public Builder alertFailRate(int alertFailRate) {
            this.alertFailRate = alertFailRate;
            return this;
        }

        /**
         * Sets the {@code alertFailPartnerRate} and returns a reference to this Builder enabling method chaining.
         *
         * @param alertFailPartnerRate the {@code alertFailPartnerRate} to set
         * @return a reference to this Builder
         */
        public Builder alertFailPartnerRate(int alertFailPartnerRate) {
            this.alertFailPartnerRate = alertFailPartnerRate;
            return this;
        }

        /**
         * Sets the {@code alertFailProgramRate} and returns a reference to this Builder enabling method chaining.
         *
         * @param alertFailProgramRate the {@code alertFailProgramRate} to set
         * @return a reference to this Builder
         */
        public Builder alertFailProgramRate(int alertFailProgramRate) {
            this.alertFailProgramRate = alertFailProgramRate;
            return this;
        }

        /**
         * Sets the {@code alertFailConfigRate} and returns a reference to this Builder enabling method chaining.
         *
         * @param alertFailConfigRate the {@code alertFailConfigRate} to set
         * @return a reference to this Builder
         */
        public Builder alertFailConfigRate(int alertFailConfigRate) {
            this.alertFailConfigRate = alertFailConfigRate;
            return this;
        }

        /**
         * Sets the {@code alertFailDataRate} and returns a reference to this Builder enabling method chaining.
         *
         * @param alertFailDataRate the {@code alertFailDataRate} to set
         * @return a reference to this Builder
         */
        public Builder alertFailDataRate(int alertFailDataRate) {
            this.alertFailDataRate = alertFailDataRate;
            return this;
        }

        /**
         * Sets the {@code alertQueueOversize} and returns a reference to this Builder enabling method chaining.
         *
         * @param alertQueueOversize the {@code alertQueueOversize} to set
         * @return a reference to this Builder
         */
        public Builder alertQueueOversize(int alertQueueOversize) {
            this.alertQueueOversize = alertQueueOversize;
            return this;
        }

        /**
         * Sets the {@code alertQueueTimeout} and returns a reference to this Builder enabling method chaining.
         *
         * @param alertQueueTimeout the {@code alertQueueTimeout} to set
         * @return a reference to this Builder
         */
        public Builder alertQueueTimeout(int alertQueueTimeout) {
            this.alertQueueTimeout = alertQueueTimeout;
            return this;
        }

        /**
         * Sets the {@code alertWaitTimeout} and returns a reference to this Builder enabling method chaining.
         *
         * @param alertWaitTimeout the {@code alertWaitTimeout} to set
         * @return a reference to this Builder
         */
        public Builder alertWaitTimeout(int alertWaitTimeout) {
            this.alertWaitTimeout = alertWaitTimeout;
            return this;
        }

        /**
         * Sets the {@code alertRunTimeout} and returns a reference to this Builder enabling method chaining.
         *
         * @param alertRunTimeout the {@code alertRunTimeout} to set
         * @return a reference to this Builder
         */
        public Builder alertRunTimeout(int alertRunTimeout) {
            this.alertRunTimeout = alertRunTimeout;
            return this;
        }

        /**
         * Sets the {@code createDate} and returns a reference to this Builder enabling method chaining.
         *
         * @param createDate the {@code createDate} to set
         * @return a reference to this Builder
         */
        public Builder createDate(Date createDate) {
            this.createDate = createDate;
            return this;
        }

        /**
         * Sets the {@code modifyDate} and returns a reference to this Builder enabling method chaining.
         *
         * @param modifyDate the {@code modifyDate} to set
         * @return a reference to this Builder
         */
        public Builder modifyDate(Date modifyDate) {
            this.modifyDate = modifyDate;
            return this;
        }

        /**
         * Sets the {@code state} and returns a reference to this Builder enabling method chaining.
         *
         * @param state the {@code state} to set
         * @return a reference to this Builder
         */
        public Builder state(int state) {
            this.state = state;
            return this;
        }

        /**
         * Sets the {@code logLevel} and returns a reference to this Builder enabling method chaining.
         *
         * @param logLevel the {@code logLevel} to set
         * @return a reference to this Builder
         */
        public Builder logLevel(int logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        /**
         * Sets the {@code logLimitSize} and returns a reference to this Builder enabling method chaining.
         *
         * @param logLimitSize the {@code logLimitSize} to set
         * @return a reference to this Builder
         */
        public Builder logLimitSize(int logLimitSize) {
            this.logLimitSize = logLimitSize;
            return this;
        }

        /**
         * Returns a {@code TaskRunnerConfig} built from the parameters previously set.
         *
         * @return a {@code TaskRunnerConfig} built with parameters of this {@code TaskRunnerConfig.Builder}
         */
        public TaskRunnerConfig build() {
            return new TaskRunnerConfig(this);
        }
    }
}
