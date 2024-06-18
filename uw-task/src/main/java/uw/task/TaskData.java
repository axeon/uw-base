package uw.task;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.Date;

/**
 * TaskData用于任务执行的传值。以为任务完成后返回结构。 TaskParam和ResultData可通过泛型参数制定具体类型。
 * TP,TD应和TaskRunner的泛型参数完全一致，否则会导致运行时出错。
 *
 * @author axeon
 */
public class TaskData<TP, RD> implements Serializable {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 1333167065535557828L;

    /**
     * 任务状态:未设置
     */
    public static final int STATE_UNKNOWN = 0;

    /**
     * 任务状态:成功
     */
    public static final int STATE_SUCCESS = 1;

    /**
     * 任务状态:程序错误
     */
    public static final int STATE_FAIL_PROGRAM = 2;

    /**
     * 任务状态:配置错误，如超过流量限制
     */
    public static final int STATE_FAIL_CONFIG = 3;

    /**
     * 任务状态:第三方接口错误
     */
    public static final int STATE_FAIL_PARTNER = 4;

    /**
     * 任务状态:数据错误
     */
    public static final int STATE_FAIL_DATA = 5;

    /**
     * 运行模式：本地运行
     */
    public static final int RUN_TYPE_LOCAL = 1;

    /**
     * 运行模式：全局运行
     */
    public static final int RUN_TYPE_GLOBAL = 3;

    /**
     * 运行模式：全局运行RPC返回结果
     */
    public static final int RUN_TYPE_GLOBAL_RPC = 5;

    /**
     * 运行模式：自动运行RPC返回结果，使用此模式，会自动选择本地还远程运行模式。
     */
    public static final int RUN_TYPE_AUTO_RPC = 6;

    /**
     * 自动重试[为了兼容,默认开启重试]
     */
    public static final int RETRY_TYPE_AUTO = 0;

    /**
     * 用户手工重试
     */
    public static final int RETRY_TYPE_MANUAL = 1;

    /**
     * id，此序列值由框架自动生成，无需手工设置。
     */
    private long id;

    /**
     * 关联TAG，由调用方设定，用于第三方统计信息。
     */
    private String refTag;

    /**
     * 关联id，由调用方根据需要设置，用于第三方统计信息。
     */
    private long refId;

    /**
     * 关联子id，由调用方根据需要设置，用于第三方统计信息。
     */
    private long refSubId;

    /**
     * 关联对象，此对象不存入数据库，但可以通过Listener来访问。
     */
    @JsonIgnore
    private Object refObject;

    /**
     * 流量限制TAG。
     */
    private String rateLimitTag;

    /**
     * 需要执行的类名，此数值必须由调用方设置。
     */
    private String taskClass = "";

    /**
     * 任务标签，用于细分任务队列，支持多实例运行。
     */
    private String taskTag = "";

    /**
     * 延迟队列延迟时间
     */
    private long taskDelay;

    /**
     * 执行参数，此数值必须有调用方设置。
     */
    private TP taskParam;

    /**
     * 任务运行类型，默认为自动RPC，根据情况选择本地还是远程运行。
     */
    private int runType = RUN_TYPE_AUTO_RPC;

    /**
     * 重试类型
     */
    private int retryType;

    /**
     * 指定运行目标。
     */
    private String runTarget = "default";

    /**
     * 进入队列时间，此信息由框架自动设置。
     */
    private Date queueDate;

    /**
     * 开始消费时间，此信息由框架自动设置。
     */
    private Date consumeDate;

    /**
     * 开始运行时间，此信息由框架自动设置。
     */
    private Date runDate;

    /**
     * 运行结束日期，此信息由框架自动设置。
     */
    private Date finishDate;

    /**
     * 执行信息，用于存储框架自动设置。
     */
    private RD resultData;

    /**
     * 出错信息
     */
    private String errorInfo;

    /**
     * 已经执行的次数，此信息由框架自动设置。
     */
    private int ranTimes;

    /**
     * 执行状态，此信息由框架根据异常自动设置。
     */
    private int state;

    /**
     * 默认构造器。
     */
    public TaskData() {
    }

    /**
     * 带taskClass的构造器。
     *
     * @param taskClass
     */
    public TaskData(String taskClass) {
        this.taskClass = taskClass;
    }

    /**
     * 带taskClass和任务参数的构造器。
     *
     * @param taskClass
     * @param taskParam
     */
    public TaskData(String taskClass, TP taskParam) {
        this.taskClass = taskClass;
        this.taskParam = taskParam;
    }

    private TaskData(Builder builder) {
        setId(builder.id);
        setRefTag(builder.refTag);
        setRefId(builder.refId);
        setRefSubId(builder.refSubId);
        setRefObject(builder.refObject);
        setRateLimitTag(builder.rateLimitTag);
        setTaskClass(builder.taskClass);
        setTaskTag(builder.taskTag);
        setTaskDelay(builder.taskDelay);
        setTaskParam((TP) builder.taskParam);
        setRunType(builder.runType);
        setRetryType(builder.retryType);
        setRunTarget(builder.runTarget);
        setQueueDate(builder.queueDate);
        setConsumeDate(builder.consumeDate);
        setRunDate(builder.runDate);
        setFinishDate(builder.finishDate);
        setResultData((RD) builder.resultData);
        setErrorInfo(builder.errorInfo);
        setRanTimes(builder.ranTimes);
        setState(builder.state);
    }

    /**
     * builder模式。
     *
     * @return
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * builder模式，带taskClass参数。
     *
     * @param taskClass
     * @return
     */
    public static Builder builder(String taskClass) {
        return new Builder().taskClass(taskClass);
    }

    /**
     * builder模式，带taskClass参数。
     *
     * @param taskClass
     * @return
     */
    public static Builder builder(Class taskClass) {
        return new Builder().taskClass(taskClass.getName());
    }

    /**
     * builder模式，带taskClass和taskParam参数。
     *
     * @param taskClass
     * @return
     */
    public static Builder builder(String taskClass, Object taskParam) {
        return new Builder().taskClass(taskClass).taskParam(taskParam);
    }


    /**
     * builder模式，带taskClass和taskParam参数。
     *
     * @param taskClass
     * @return
     */
    public static Builder builder(Class taskClass, Object taskParam) {
        return new Builder().taskClass(taskClass.getName()).taskParam(taskParam);
    }


    public static Builder builder(TaskData copy) {
        Builder builder = new Builder();
        builder.id = copy.getId();
        builder.refTag = copy.getRefTag();
        builder.refId = copy.getRefId();
        builder.refSubId = copy.getRefSubId();
        builder.refObject = copy.getRefObject();
        builder.rateLimitTag = copy.getRateLimitTag();
        builder.taskClass = copy.getTaskClass();
        builder.taskTag = copy.getTaskTag();
        builder.taskDelay = copy.getTaskDelay();
        builder.taskParam = copy.getTaskParam();
        builder.runType = copy.getRunType();
        builder.retryType = copy.getRetryType();
        builder.runTarget = copy.getRunTarget();
        builder.queueDate = copy.getQueueDate();
        builder.consumeDate = copy.getConsumeDate();
        builder.runDate = copy.getRunDate();
        builder.finishDate = copy.getFinishDate();
        builder.resultData = copy.getResultData();
        builder.errorInfo = copy.getErrorInfo();
        builder.ranTimes = copy.getRanTimes();
        builder.state = copy.getState();
        return builder;
    }


    /**
     * 复制一份当前实例。
     *
     * @return
     */
    public TaskData copy() {
        TaskData taskData = new TaskData();
        taskData.setId(this.id);
        taskData.setRefTag(this.refTag);
        taskData.setRefId(this.refId);
        taskData.setRefSubId(this.refSubId);
        taskData.setRefObject(this.refObject);
        taskData.setRateLimitTag(this.rateLimitTag);
        taskData.setTaskClass(this.taskClass);
        taskData.setTaskTag(this.taskTag);
        taskData.setTaskDelay(this.taskDelay);
        taskData.setTaskParam(this.taskParam);
        taskData.setRunType(this.runType);
        taskData.setRetryType(this.retryType);
        taskData.setRunTarget(this.runTarget);
        taskData.setQueueDate(this.queueDate);
        taskData.setConsumeDate(this.consumeDate);
        taskData.setRunDate(this.runDate);
        taskData.setFinishDate(this.finishDate);
        taskData.setResultData(this.resultData);
        taskData.setErrorInfo(this.errorInfo);
        taskData.setRanTimes(this.ranTimes);
        taskData.setState(this.state);
        return taskData;
    }

    /**
     * @return the id
     */
    public long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * @return the refId
     */
    public long getRefId() {
        return refId;
    }

    /**
     * @param refId the refId to set
     */
    public void setRefId(long refId) {
        this.refId = refId;
    }

    /**
     * @return the refSubId
     */
    public long getRefSubId() {
        return refSubId;
    }

    /**
     * @param refSubId the refSubId to set
     */
    public void setRefSubId(long refSubId) {
        this.refSubId = refSubId;
    }

    /**
     * @return the refTag
     */
    public String getRefTag() {
        return refTag;
    }

    /**
     * @param refTag the refTag to set
     */
    public void setRefTag(String refTag) {
        this.refTag = refTag;
    }

    /**
     * @return the rateLimitTag
     */
    public String getRateLimitTag() {
        return rateLimitTag;
    }

    /**
     * @param rateLimitTag the rateLimitTag to set
     */
    public void setRateLimitTag(String rateLimitTag) {
        this.rateLimitTag = rateLimitTag;
    }

    /**
     * @return the refObject
     */
    public Object getRefObject() {
        return refObject;
    }

    /**
     * @param refObject the refObject to set
     */
    public void setRefObject(Object refObject) {
        this.refObject = refObject;
    }

    /**
     * @return the taskClass
     */
    public String getTaskClass() {
        return taskClass;
    }

    /**
     * @param taskClass the taskClass to set
     */
    public void setTaskClass(String taskClass) {
        this.taskClass = taskClass;
    }

    /**
     * @return the taskTag
     */
    public String getTaskTag() {
        return taskTag;
    }


    public long getTaskDelay() {
        return taskDelay;
    }

    public void setTaskDelay(long taskDelay) {
        this.taskDelay = taskDelay;
    }

    /**
     * @param taskTag the taskTag to set
     */
    public void setTaskTag(String taskTag) {
        this.taskTag = taskTag;
    }

    /**
     * @return the taskParam
     */
    public TP getTaskParam() {
        return taskParam;
    }

    /**
     * @param taskParam the taskParam to set
     */
    public void setTaskParam(TP taskParam) {
        this.taskParam = taskParam;
    }

    /**
     * @return the runType
     */
    public int getRunType() {
        return runType;
    }

    /**
     * @param runType the runType to set
     */
    public void setRunType(int runType) {
        this.runType = runType;
    }

    public int getRetryType() {
        return retryType;
    }

    public void setRetryType(int retryType) {
        this.retryType = retryType;
    }

    /**
     * @return the runTarget
     */
    public String getRunTarget() {
        return runTarget;
    }

    /**
     * @param runTarget the runTarget to set
     */
    public void setRunTarget(String runTarget) {
        this.runTarget = runTarget;
    }
    /**
     * @return the queueDate
     */
    public Date getQueueDate() {
        return queueDate;
    }

    /**
     * @param queueDate the queueDate to set
     */
    public void setQueueDate(Date queueDate) {
        this.queueDate = queueDate;
    }

    /**
     * @return the consumeDate
     */
    public Date getConsumeDate() {
        return consumeDate;
    }

    /**
     * @param consumeDate the consumeDate to set
     */
    public void setConsumeDate(Date consumeDate) {
        this.consumeDate = consumeDate;
    }

    /**
     * @return the runDate
     */
    public Date getRunDate() {
        return runDate;
    }

    /**
     * @param runDate the runDate to set
     */
    public void setRunDate(Date runDate) {
        this.runDate = runDate;
    }

    /**
     * @return the finishDate
     */
    public Date getFinishDate() {
        return finishDate;
    }

    /**
     * @param finishDate the finishDate to set
     */
    public void setFinishDate(Date finishDate) {
        this.finishDate = finishDate;
    }

    /**
     * @return the resultData
     */
    public RD getResultData() {
        return resultData;
    }

    /**
     * @param resultData the resultData to set
     */
    public void setResultData(RD resultData) {
        this.resultData = resultData;
    }

    /**
     * @return the errorInfo
     */
    public String getErrorInfo() {
        return errorInfo;
    }

    /**
     * @param errorInfo the errorInfo to set
     */
    public void setErrorInfo(String errorInfo) {
        this.errorInfo = errorInfo;
    }

    /**
     * @return the ranTimes
     */
    public int getRanTimes() {
        return ranTimes;
    }

    /**
     * @param ranTimes the ranTimes to set
     */
    public void setRanTimes(int ranTimes) {
        this.ranTimes = ranTimes;
    }

    /**
     * @return the status
     */
    public int getState() {
        return state;
    }

    /**
     * @param status the status to set
     */
    public void setState(int status) {
        this.state = status;
    }


    /**
     * {@code TaskData} builder static inner class.
     */
    public static final class Builder<TP, RD> {

        /**
         * id，此序列值由框架自动生成，无需手工设置。
         */
        private long id;

        /**
         * 关联TAG，由调用方设定，用于第三方统计信息。
         */
        private String refTag;

        /**
         * 关联id，由调用方根据需要设置，用于第三方统计信息。
         */
        private long refId;

        /**
         * 关联子id，由调用方根据需要设置，用于第三方统计信息。
         */
        private long refSubId;

        /**
         * 关联对象，此对象不存入数据库，但可以通过Listener来访问。
         */
        private Object refObject;

        /**
         * 流量限制TAG。
         */
        private String rateLimitTag;

        /**
         * 需要执行的类名，此数值必须由调用方设置。
         */
        private String taskClass = "";

        /**
         * 任务标签，用于细分任务队列，支持多实例运行。
         */
        private String taskTag = "";

        /**
         * 队列类型。
         */
        private int queueType;

        /**
         * 延迟队列延迟时间
         */
        private long taskDelay;

        /**
         * 执行参数，此数值必须有调用方设置。
         */
        private TP taskParam;

        /**
         * 任务运行类型，默认为自动RPC，根据情况选择本地还是远程运行。
         */
        private int runType = RUN_TYPE_AUTO_RPC;

        /**
         * 重试类型
         */
        private int retryType;

        /**
         * 指定运行目标。
         */
        private String runTarget = "default";

        /**
         * 任务运行时主机IP，此信息由框架自动设置。
         */
        private String hostIp;

        /**
         * 任务运行时主机ID（可能为docker的ContainerID），此信息由框架自动设置。
         */
        private String hostId;

        /**
         * 进入队列时间，此信息由框架自动设置。
         */
        private Date queueDate;

        /**
         * 开始消费时间，此信息由框架自动设置。
         */
        private Date consumeDate;

        /**
         * 开始运行时间，此信息由框架自动设置。
         */
        private Date runDate;

        /**
         * 运行结束日期，此信息由框架自动设置。
         */
        private Date finishDate;

        /**
         * 执行信息，用于存储框架自动设置。
         */
        private RD resultData;

        /**
         * 出错信息
         */
        private String errorInfo;

        /**
         * 已经执行的次数，此信息由框架自动设置。
         */
        private int ranTimes;

        /**
         * 执行状态，此信息由框架根据异常自动设置。
         */
        private int state;

        private Builder() {
        }

        /**
         * Sets the {@code id} and returns a reference to this Builder enabling method chaining.
         *
         * @param id the {@code id} to set
         * @return a reference to this Builder
         */
        public Builder id(long id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the {@code refTag} and returns a reference to this Builder enabling method chaining.
         *
         * @param refTag the {@code refTag} to set
         * @return a reference to this Builder
         */
        public Builder refTag(String refTag) {
            this.refTag = refTag;
            return this;
        }

        /**
         * Sets the {@code refId} and returns a reference to this Builder enabling method chaining.
         *
         * @param refId the {@code refId} to set
         * @return a reference to this Builder
         */
        public Builder refId(long refId) {
            this.refId = refId;
            return this;
        }

        /**
         * Sets the {@code refSubId} and returns a reference to this Builder enabling method chaining.
         *
         * @param refSubId the {@code refSubId} to set
         * @return a reference to this Builder
         */
        public Builder refSubId(long refSubId) {
            this.refSubId = refSubId;
            return this;
        }

        /**
         * Sets the {@code refObject} and returns a reference to this Builder enabling method chaining.
         *
         * @param refObject the {@code refObject} to set
         * @return a reference to this Builder
         */
        public Builder refObject(Object refObject) {
            this.refObject = refObject;
            return this;
        }

        /**
         * Sets the {@code rateLimitTag} and returns a reference to this Builder enabling method chaining.
         *
         * @param rateLimitTag the {@code rateLimitTag} to set
         * @return a reference to this Builder
         */
        public Builder rateLimitTag(String rateLimitTag) {
            this.rateLimitTag = rateLimitTag;
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
         * Sets the {@code taskDelay} and returns a reference to this Builder enabling method chaining.
         *
         * @param taskDelay the {@code taskDelay} to set
         * @return a reference to this Builder
         */
        public Builder taskDelay(long taskDelay) {
            this.taskDelay = taskDelay;
            return this;
        }

        /**
         * Sets the {@code taskParam} and returns a reference to this Builder enabling method chaining.
         *
         * @param taskParam the {@code taskParam} to set
         * @return a reference to this Builder
         */
        public Builder taskParam(TP taskParam) {
            this.taskParam = taskParam;
            return this;
        }

        /**
         * Sets the {@code runType} and returns a reference to this Builder enabling method chaining.
         *
         * @param runType the {@code runType} to set
         * @return a reference to this Builder
         */
        public Builder runType(int runType) {
            this.runType = runType;
            return this;
        }

        /**
         * Sets the {@code retryType} and returns a reference to this Builder enabling method chaining.
         *
         * @param retryType the {@code retryType} to set
         * @return a reference to this Builder
         */
        public Builder retryType(int retryType) {
            this.retryType = retryType;
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
         * Sets the {@code hostIp} and returns a reference to this Builder enabling method chaining.
         *
         * @param hostIp the {@code hostIp} to set
         * @return a reference to this Builder
         */
        public Builder hostIp(String hostIp) {
            this.hostIp = hostIp;
            return this;
        }

        /**
         * Sets the {@code hostId} and returns a reference to this Builder enabling method chaining.
         *
         * @param hostId the {@code hostId} to set
         * @return a reference to this Builder
         */
        public Builder hostId(String hostId) {
            this.hostId = hostId;
            return this;
        }

        /**
         * Sets the {@code queueDate} and returns a reference to this Builder enabling method chaining.
         *
         * @param queueDate the {@code queueDate} to set
         * @return a reference to this Builder
         */
        public Builder queueDate(Date queueDate) {
            this.queueDate = queueDate;
            return this;
        }

        /**
         * Sets the {@code consumeDate} and returns a reference to this Builder enabling method chaining.
         *
         * @param consumeDate the {@code consumeDate} to set
         * @return a reference to this Builder
         */
        public Builder consumeDate(Date consumeDate) {
            this.consumeDate = consumeDate;
            return this;
        }

        /**
         * Sets the {@code runDate} and returns a reference to this Builder enabling method chaining.
         *
         * @param runDate the {@code runDate} to set
         * @return a reference to this Builder
         */
        public Builder runDate(Date runDate) {
            this.runDate = runDate;
            return this;
        }

        /**
         * Sets the {@code finishDate} and returns a reference to this Builder enabling method chaining.
         *
         * @param finishDate the {@code finishDate} to set
         * @return a reference to this Builder
         */
        public Builder finishDate(Date finishDate) {
            this.finishDate = finishDate;
            return this;
        }

        /**
         * Sets the {@code resultData} and returns a reference to this Builder enabling method chaining.
         *
         * @param resultData the {@code resultData} to set
         * @return a reference to this Builder
         */
        public Builder resultData(RD resultData) {
            this.resultData = resultData;
            return this;
        }

        /**
         * Sets the {@code errorInfo} and returns a reference to this Builder enabling method chaining.
         *
         * @param errorInfo the {@code errorInfo} to set
         * @return a reference to this Builder
         */
        public Builder errorInfo(String errorInfo) {
            this.errorInfo = errorInfo;
            return this;
        }

        /**
         * Sets the {@code ranTimes} and returns a reference to this Builder enabling method chaining.
         *
         * @param ranTimes the {@code ranTimes} to set
         * @return a reference to this Builder
         */
        public Builder ranTimes(int ranTimes) {
            this.ranTimes = ranTimes;
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
         * Returns a {@code TaskData} built from the parameters previously set.
         *
         * @return a {@code TaskData} built with parameters of this {@code TaskData.Builder}
         */
        public TaskData build() {
            return new TaskData(this);
        }
    }
}
