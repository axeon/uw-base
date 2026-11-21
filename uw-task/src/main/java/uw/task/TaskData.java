package uw.task;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.Date;

/**
 * TaskData用于任务执行的传值。以为任务完成后返回结构。 TaskParam和ResultData可通过泛型参数制定具体类型。
 * TP,TD应和TaskRunner的泛型参数完全一致，否则会导致运行时出错。
 *
 * @author axeon
 */
public class TaskData<TP, RD> implements Serializable, KryoSerializable {

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

    private TaskData(Builder<TP, RD> builder) {
        setRefTag(builder.refTag);
        setRefId(builder.refId);
        setRefSubId(builder.refSubId);
        setRefObject(builder.refObject);
        setRateLimitTag(builder.rateLimitTag);
        setTaskClass(builder.taskClass);
        setTaskTag(builder.taskTag);
        setTaskDelay(builder.taskDelay);
        setTaskParam(builder.taskParam);
        setRunType(builder.runType);
        setRetryType(builder.retryType);
        setRunTarget(builder.runTarget);
    }

    /**
     * builder模式。
     *
     * @return
     */
    public static <TP, RD> Builder<TP, RD> builder() {
        return new Builder<TP, RD>();
    }

    /**
     * builder模式，带taskClass参数。
     *
     * @param taskClass
     * @return
     */
    public static <TP, RD> Builder<TP, RD> builder(String taskClass) {
        return new Builder<TP, RD>().taskClass(taskClass);
    }

    /**
     * builder模式，带taskClass参数。
     *
     * @param taskClass
     * @return
     */
    public static <TP, RD> Builder<TP, RD> builder(Class<? extends TaskRunner<TP, RD>> taskClass) {
        return new Builder<TP, RD>().taskClass(taskClass.getName());
    }

    /**
     * builder 模式，带 taskClass 和 taskParam 参数。
     *
     * @param taskClass 任务类全限定名
     * @param taskParam 任务参数
     * @return Builder
     */
    public static <TP, RD> Builder<TP, RD> builder(String taskClass, TP taskParam) {
        return new Builder<TP, RD>().taskClass(taskClass).taskParam(taskParam);
    }


    /**
     * builder 模式，带 taskClass 和 taskParam 参数。
     *
     * @param taskClass 任务类（TaskRunner 子类）
     * @param taskParam 任务参数
     * @return Builder
     */
    public static <TP, RD> Builder<TP, RD> builder(Class<? extends TaskRunner<TP, RD>> taskClass, TP taskParam) {
        return new Builder<TP, RD>().taskClass(taskClass.getName()).taskParam(taskParam);
    }


    /**
     * 基于已有 TaskData 复制出一个 Builder（用于局部修改后重建，如重试 cleanForRetry 后 resubmit）。
     *
     * @param copy 源任务数据
     * @return Builder
     */
    public static <TP, RD> Builder<TP, RD> builder(TaskData<TP, RD> copy) {
        Builder<TP, RD> builder = new Builder<TP, RD>();
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
        return builder;
    }


    /**
     * 复制一份当前实例。
     *
     * @return
     */
    public TaskData<TP, RD> copy() {
        TaskData<TP, RD> taskData = new TaskData<TP, RD>();
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
     * 为重试清场运行期字段：清 consumeDate/runDate/finishDate/resultData/errorInfo，state 置 UNKNOWN。
     * <p>不清 id/taskClass/taskTag/taskParam/queueDate/taskDelay/retryType（重试仍需要）。
     * 也不清 refObject（内存态字段，由调用方按需清理）。
     */
    public void cleanForRetry() {
        this.consumeDate = null;
        this.runDate = null;
        this.finishDate = null;
        this.resultData = null;
        this.errorInfo = null;
        this.state = STATE_UNKNOWN;
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

    /**
     * @param taskTag the taskTag to set
     */
    public void setTaskTag(String taskTag) {
        this.taskTag = taskTag;
    }

    public long getTaskDelay() {
        return taskDelay;
    }

    public void setTaskDelay(long taskDelay) {
        this.taskDelay = taskDelay;
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
     * Kryo 手工序列化：按固定顺序写原语字段（实现 {@link KryoSerializable}）。
     * <p>
     * 相比 FieldSerializer 反射，省去类名/字段名开销，体积更小、速度更快（参考 {@code KryoUtils} JMH 实测）。
     * <b>refObject 刻意不写入</b>：它是内存态关联对象（{@code @JsonIgnore}、注释"不存入数据库"），
     * 不应跨 MQ 传输——任意 Object 跨网络会带来体积不可控、序列化失败（抛 KryoException 中断投递）、潜在信息泄露。
     * taskParam/resultData 为任意类型，用 {@code kryo.writeClassAndObject} 携带类型信息。
     * <p>
     * <b>读写顺序必须严格一致</b>，错位会导致反序列化数据损坏。
     *
     * @param kryo   kryo 实例（用于 writeClassAndObject 写嵌套对象）
     * @param output kryo 输出流
     */
    @Override
    public void write(Kryo kryo, Output output) {
        output.writeVarLong(id, true);
        output.writeString(refTag == null ? "" : refTag);
        output.writeVarLong(refId, true);
        output.writeVarLong(refSubId, true);
        // refObject 刻意不写（内存态字段，不跨 MQ 传输）
        output.writeString(rateLimitTag == null ? "" : rateLimitTag);
        output.writeString(taskClass == null ? "" : taskClass);
        output.writeString(taskTag == null ? "" : taskTag);
        output.writeVarLong(taskDelay, true);
        kryo.writeClassAndObject(output, taskParam);
        output.writeVarInt(runType, true);
        output.writeVarInt(retryType, true);
        output.writeString(runTarget == null ? "" : runTarget);
        output.writeVarLong(queueDate == null ? 0L : queueDate.getTime(), true);
        output.writeVarLong(consumeDate == null ? 0L : consumeDate.getTime(), true);
        output.writeVarLong(runDate == null ? 0L : runDate.getTime(), true);
        output.writeVarLong(finishDate == null ? 0L : finishDate.getTime(), true);
        kryo.writeClassAndObject(output, resultData);
        output.writeString(errorInfo == null ? "" : errorInfo);
        output.writeVarInt(ranTimes, true);
        output.writeVarInt(state, true);
    }

    /**
     * Kryo 手工反序列化：按 {@link #write} 的相同顺序读取填充字段。
     * <p>
     * taskParam/resultData 用 {@code kryo.readClassAndObject} 还原，泛型擦除下为 Object，按需强转。
     * Date 以 0 为 null 哨兵（业务不会出现 1970-01-01 时间戳）。
     *
     * @param kryo  kryo 实例
     * @param input kryo 输入流
     */
    @Override
    @SuppressWarnings("unchecked")
    public void read(Kryo kryo, Input input) {
        id = input.readVarLong(true);
        refTag = input.readString();
        refId = input.readVarLong(true);
        refSubId = input.readVarLong(true);
        rateLimitTag = input.readString();
        taskClass = input.readString();
        taskTag = input.readString();
        taskDelay = input.readVarLong(true);
        taskParam = (TP) kryo.readClassAndObject(input);
        runType = input.readVarInt(true);
        retryType = input.readVarInt(true);
        runTarget = input.readString();
        long v;
        v = input.readVarLong(true);
        queueDate = v == 0L ? null : new Date(v);
        v = input.readVarLong(true);
        consumeDate = v == 0L ? null : new Date(v);
        v = input.readVarLong(true);
        runDate = v == 0L ? null : new Date(v);
        v = input.readVarLong(true);
        finishDate = v == 0L ? null : new Date(v);
        resultData = (RD) kryo.readClassAndObject(input);
        errorInfo = input.readString();
        ranTimes = input.readVarInt(true);
        state = input.readVarInt(true);
    }


    /**
     * {@code TaskData} builder static inner class.
     */
    public static final class Builder<TP, RD> {

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

        private Builder() {
        }


        /**
         * Sets the {@code refTag} and returns a reference to this Builder enabling method chaining.
         *
         * @param refTag the {@code refTag} to set
         * @return a reference to this Builder
         */
        public Builder<TP, RD> refTag(String refTag) {
            this.refTag = refTag;
            return this;
        }

        /**
         * Sets the {@code refId} and returns a reference to this Builder enabling method chaining.
         *
         * @param refId the {@code refId} to set
         * @return a reference to this Builder
         */
        public Builder<TP, RD> refId(long refId) {
            this.refId = refId;
            return this;
        }

        /**
         * Sets the {@code refSubId} and returns a reference to this Builder enabling method chaining.
         *
         * @param refSubId the {@code refSubId} to set
         * @return a reference to this Builder
         */
        public Builder<TP, RD> refSubId(long refSubId) {
            this.refSubId = refSubId;
            return this;
        }

        /**
         * Sets the {@code refObject} and returns a reference to this Builder enabling method chaining.
         *
         * @param refObject the {@code refObject} to set
         * @return a reference to this Builder
         */
        public Builder<TP, RD> refObject(Object refObject) {
            this.refObject = refObject;
            return this;
        }

        /**
         * Sets the {@code rateLimitTag} and returns a reference to this Builder enabling method chaining.
         *
         * @param rateLimitTag the {@code rateLimitTag} to set
         * @return a reference to this Builder
         */
        public Builder<TP, RD> rateLimitTag(String rateLimitTag) {
            this.rateLimitTag = rateLimitTag;
            return this;
        }

        /**
         * Sets the {@code taskClass} and returns a reference to this Builder enabling method chaining.
         *
         * @param taskClass the {@code taskClass} to set
         * @return a reference to this Builder
         */
        public Builder<TP, RD> taskClass(String taskClass) {
            this.taskClass = taskClass;
            return this;
        }

        /**
         * Sets the {@code taskTag} and returns a reference to this Builder enabling method chaining.
         *
         * @param taskTag the {@code taskTag} to set
         * @return a reference to this Builder
         */
        public Builder<TP, RD> taskTag(String taskTag) {
            this.taskTag = taskTag;
            return this;
        }

        /**
         * Sets the {@code taskDelay} and returns a reference to this Builder enabling method chaining.
         *
         * @param taskDelay the {@code taskDelay} to set
         * @return a reference to this Builder
         */
        public Builder<TP, RD> taskDelay(long taskDelay) {
            this.taskDelay = taskDelay;
            return this;
        }

        /**
         * Sets the {@code taskParam} and returns a reference to this Builder enabling method chaining.
         *
         * @param taskParam the {@code taskParam} to set
         * @return a reference to this Builder
         */
        public Builder<TP, RD> taskParam(TP taskParam) {
            this.taskParam = taskParam;
            return this;
        }

        /**
         * Sets the {@code runType} and returns a reference to this Builder enabling method chaining.
         *
         * @param runType the {@code runType} to set
         * @return a reference to this Builder
         */
        public Builder<TP, RD> runType(int runType) {
            this.runType = runType;
            return this;
        }

        /**
         * Sets the {@code retryType} and returns a reference to this Builder enabling method chaining.
         *
         * @param retryType the {@code retryType} to set
         * @return a reference to this Builder
         */
        public Builder<TP, RD> retryType(int retryType) {
            this.retryType = retryType;
            return this;
        }

        /**
         * Sets the {@code runTarget} and returns a reference to this Builder enabling method chaining.
         *
         * @param runTarget the {@code runTarget} to set
         * @return a reference to this Builder
         */
        public Builder<TP, RD> runTarget(String runTarget) {
            this.runTarget = runTarget;
            return this;
        }

        /**
         * Returns a {@code TaskData} built from the parameters previously set.
         *
         * @return a {@code TaskData} built with parameters of this {@code TaskData.Builder}
         */
        public TaskData<TP, RD> build() {
            return new TaskData<TP, RD>(this);
        }
    }
}
