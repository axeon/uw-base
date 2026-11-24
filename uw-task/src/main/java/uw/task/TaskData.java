package uw.task;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.Date;

/**
 * 队列任务的传值与结果容器。TP/RD 必须与 {@link TaskRunner} 的泛型参数完全一致，否则运行时出错。
 * <p>经 Kryo 手工序列化跨 MQ/Redis 传输（见 {@link #write}）：调用方设置 taskClass/taskParam 等业务字段，
 * id/queueDate/runType 等运行期字段由框架写入，请勿在多次调用间复用同一对象。</p>
 *
 * @param <TP> 任务参数类型
 * @param <RD> 返回结果类型
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
     * 重试类型：自动重试（默认）
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
     * 要执行的 TaskRunner 类全限定名，必须由调用方设置。
     */
    private String taskClass = "";

    /**
     * 任务标签，用于细分任务队列，支持多实例运行。
     */
    private String taskTag = "";

    /**
     * 延时毫秒数：&gt;0 即延时任务（载体自动路由）；负值为框架内部"已到期搬运"标记（走即时投递分支），调用方勿设。
     */
    private long taskDelay;

    /**
     * 执行参数，必须由调用方设置。
     */
    private TP taskParam;

    /**
     * 任务运行类型，默认 AUTO_RPC：本机有匹配 runner 则本地运行，否则走远程 RPC。
     */
    private int runType = RUN_TYPE_AUTO_RPC;

    /**
     * 重试类型，默认 AUTO（失败后按 runner 配置的 retryTimes 自动重试）。
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
     * @param taskClass TaskRunner 类全限定名
     */
    public TaskData(String taskClass) {
        this.taskClass = taskClass;
    }

    /**
     * 带taskClass和任务参数的构造器。
     *
     * @param taskClass TaskRunner 类全限定名
     * @param taskParam 任务参数
     */
    public TaskData(String taskClass, TP taskParam) {
        this.taskClass = taskClass;
        this.taskParam = taskParam;
    }

    /**
     * 由 Builder 构建实例。
     *
     * @param builder 已设置好业务字段的 Builder
     */
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
     * 创建空 Builder。
     *
     * @param <TP> 任务参数类型
     * @param <RD> 返回结果类型
     * @return Builder
     */
    public static <TP, RD> Builder<TP, RD> builder() {
        return new Builder<TP, RD>();
    }

    /**
     * 创建带 taskClass 的 Builder。
     *
     * @param taskClass TaskRunner 类（Class 形式或全限定名）
     * @param <TP>      任务参数类型
     * @param <RD>      返回结果类型
     * @return Builder
     */
    public static <TP, RD> Builder<TP, RD> builder(String taskClass) {
        return new Builder<TP, RD>().taskClass(taskClass);
    }

    /**
     * 创建带 taskClass 的 Builder。
     *
     * @param taskClass TaskRunner 类（Class 形式或全限定名）
     * @param <TP>      任务参数类型
     * @param <RD>      返回结果类型
     * @return Builder
     */
    public static <TP, RD> Builder<TP, RD> builder(Class<? extends TaskRunner<TP, RD>> taskClass) {
        return new Builder<TP, RD>().taskClass(taskClass.getName());
    }

    /**
     * 创建带 taskClass 和 taskParam 的 Builder。
     *
     * @param taskClass 任务类全限定名
     * @param taskParam 任务参数
     * @param <TP>      任务参数类型
     * @param <RD>      返回结果类型
     * @return Builder
     */
    public static <TP, RD> Builder<TP, RD> builder(String taskClass, TP taskParam) {
        return new Builder<TP, RD>().taskClass(taskClass).taskParam(taskParam);
    }

    /**
     * 创建带 taskClass 和 taskParam 的 Builder。
     *
     * @param taskClass 任务类（TaskRunner 子类）
     * @param taskParam 任务参数
     * @param <TP>      任务参数类型
     * @param <RD>      返回结果类型
     * @return Builder
     */
    public static <TP, RD> Builder<TP, RD> builder(Class<? extends TaskRunner<TP, RD>> taskClass, TP taskParam) {
        return new Builder<TP, RD>().taskClass(taskClass.getName()).taskParam(taskParam);
    }

    /**
     * 基于已有 TaskData 复制出一个 Builder（业务字段同源，运行期字段不带入，可局部修改后重建）。
     *
     * @param copy 源任务数据
     * @param <TP> 任务参数类型
     * @param <RD> 返回结果类型
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
     * 复制一份当前实例（含全部运行期字段）。
     *
     * @return 复制出的新实例
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
     * 获取任务 id（框架自动生成，无需手工设置）。
     *
     * @return 任务 id（框架自动生成）
     */
    public long getId() {
        return id;
    }

    /**
     * 设置任务 id。
     *
     * @param id 任务 id
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * 获取关联id，由调用方根据需要设置，用于第三方统计信息。
     *
     * @return 关联id，由调用方根据需要设置，用于第三方统计信息
     */
    public long getRefId() {
        return refId;
    }

    /**
     * 设置关联id，由调用方根据需要设置，用于第三方统计信息。
     *
     * @param refId 关联id，由调用方根据需要设置，用于第三方统计信息
     */
    public void setRefId(long refId) {
        this.refId = refId;
    }

    /**
     * 获取关联子id，由调用方根据需要设置，用于第三方统计信息。
     *
     * @return 关联子id，由调用方根据需要设置，用于第三方统计信息
     */
    public long getRefSubId() {
        return refSubId;
    }

    /**
     * 设置关联子id，由调用方根据需要设置，用于第三方统计信息。
     *
     * @param refSubId 关联子id，由调用方根据需要设置，用于第三方统计信息
     */
    public void setRefSubId(long refSubId) {
        this.refSubId = refSubId;
    }

    /**
     * 获取关联TAG，由调用方设定，用于第三方统计信息。
     *
     * @return 关联TAG，由调用方设定，用于第三方统计信息
     */
    public String getRefTag() {
        return refTag;
    }

    /**
     * 设置关联TAG，由调用方设定，用于第三方统计信息。
     *
     * @param refTag 关联TAG，由调用方设定，用于第三方统计信息
     */
    public void setRefTag(String refTag) {
        this.refTag = refTag;
    }

    /**
     * 获取流量限制TAG。
     *
     * @return 流量限制TAG
     */
    public String getRateLimitTag() {
        return rateLimitTag;
    }

    /**
     * 设置流量限制TAG。
     *
     * @param rateLimitTag 流量限制TAG
     */
    public void setRateLimitTag(String rateLimitTag) {
        this.rateLimitTag = rateLimitTag;
    }

    /**
     * 返回关联对象（仅在调用方进程内有效，不序列化）。
     *
     * @return 关联对象
     */
    public Object getRefObject() {
        return refObject;
    }

    /**
     * 设置关联对象（仅在调用方进程内有效，不序列化）。
     *
     * @param refObject 关联对象
     */
    public void setRefObject(Object refObject) {
        this.refObject = refObject;
    }

    /**
     * 获取要执行的 TaskRunner 类全限定名，必须由调用方设置。
     *
     * @return 要执行的 TaskRunner 类全限定名，必须由调用方设置
     */
    public String getTaskClass() {
        return taskClass;
    }

    /**
     * 设置要执行的 TaskRunner 类全限定名，必须由调用方设置。
     *
     * @param taskClass 要执行的 TaskRunner 类全限定名，必须由调用方设置
     */
    public void setTaskClass(String taskClass) {
        this.taskClass = taskClass;
    }

    /**
     * 获取任务标签，用于细分任务队列，支持多实例运行。
     *
     * @return 任务标签，用于细分任务队列，支持多实例运行
     */
    public String getTaskTag() {
        return taskTag;
    }

    /**
     * 设置任务标签，用于细分任务队列，支持多实例运行。
     *
     * @param taskTag 任务标签，用于细分任务队列，支持多实例运行
     */
    public void setTaskTag(String taskTag) {
        this.taskTag = taskTag;
    }

    /**
     * 返回延时毫秒数（&gt;0 延时任务；负值为框架内部搬运标记）。
     *
     * @return 延时毫秒数（&gt;0 延时任务；负值为框架内部搬运标记）
     */
    public long getTaskDelay() {
        return taskDelay;
    }

    /**
     * 设置延时毫秒数。
     *
     * @param taskDelay 延时毫秒数（&gt;0 即延时任务）
     */
    public void setTaskDelay(long taskDelay) {
        this.taskDelay = taskDelay;
    }

    /**
     * 获取执行参数，必须由调用方设置。
     *
     * @return 执行参数，必须由调用方设置
     */
    public TP getTaskParam() {
        return taskParam;
    }

    /**
     * 设置执行参数，必须由调用方设置。
     *
     * @param taskParam 执行参数，必须由调用方设置
     */
    public void setTaskParam(TP taskParam) {
        this.taskParam = taskParam;
    }

    /**
     * 获取任务运行类型，默认 AUTO_RPC：本机有匹配 runner 则本地运行，否则走远程 RPC。
     *
     * @return 任务运行类型，默认 AUTO_RPC：本机有匹配 runner 则本地运行，否则走远程 RPC
     */
    public int getRunType() {
        return runType;
    }

    /**
     * 设置任务运行类型，默认 AUTO_RPC：本机有匹配 runner 则本地运行，否则走远程 RPC。
     *
     * @param runType 任务运行类型，默认 AUTO_RPC：本机有匹配 runner 则本地运行，否则走远程 RPC
     */
    public void setRunType(int runType) {
        this.runType = runType;
    }

    /**
     * 返回重试类型。
     *
     * @return 重试类型
     */
    public int getRetryType() {
        return retryType;
    }

    /**
     * 设置重试类型。
     *
     * @param retryType 重试类型
     */
    public void setRetryType(int retryType) {
        this.retryType = retryType;
    }

    /**
     * 获取指定运行目标。
     *
     * @return 指定运行目标
     */
    public String getRunTarget() {
        return runTarget;
    }

    /**
     * 设置指定运行目标。
     *
     * @param runTarget 指定运行目标
     */
    public void setRunTarget(String runTarget) {
        this.runTarget = runTarget;
    }

    /**
     * 获取进入队列时间，此信息由框架自动设置。
     *
     * @return 进入队列时间，此信息由框架自动设置
     */
    public Date getQueueDate() {
        return queueDate;
    }

    /**
     * 设置进入队列时间，此信息由框架自动设置。
     *
     * @param queueDate 进入队列时间，此信息由框架自动设置
     */
    public void setQueueDate(Date queueDate) {
        this.queueDate = queueDate;
    }

    /**
     * 获取开始消费时间，此信息由框架自动设置。
     *
     * @return 开始消费时间，此信息由框架自动设置
     */
    public Date getConsumeDate() {
        return consumeDate;
    }

    /**
     * 设置开始消费时间，此信息由框架自动设置。
     *
     * @param consumeDate 开始消费时间，此信息由框架自动设置
     */
    public void setConsumeDate(Date consumeDate) {
        this.consumeDate = consumeDate;
    }

    /**
     * 获取开始运行时间，此信息由框架自动设置。
     *
     * @return 开始运行时间，此信息由框架自动设置
     */
    public Date getRunDate() {
        return runDate;
    }

    /**
     * 设置开始运行时间，此信息由框架自动设置。
     *
     * @param runDate 开始运行时间，此信息由框架自动设置
     */
    public void setRunDate(Date runDate) {
        this.runDate = runDate;
    }

    /**
     * 获取运行结束日期，此信息由框架自动设置。
     *
     * @return 运行结束日期，此信息由框架自动设置
     */
    public Date getFinishDate() {
        return finishDate;
    }

    /**
     * 设置运行结束日期，此信息由框架自动设置。
     *
     * @param finishDate 运行结束日期，此信息由框架自动设置
     */
    public void setFinishDate(Date finishDate) {
        this.finishDate = finishDate;
    }

    /**
     * 获取执行信息，用于存储框架自动设置。
     *
     * @return 执行信息，用于存储框架自动设置
     */
    public RD getResultData() {
        return resultData;
    }

    /**
     * 设置执行信息，用于存储框架自动设置。
     *
     * @param resultData 执行信息，用于存储框架自动设置
     */
    public void setResultData(RD resultData) {
        this.resultData = resultData;
    }

    /**
     * 获取出错信息。
     *
     * @return 出错信息
     */
    public String getErrorInfo() {
        return errorInfo;
    }

    /**
     * 设置出错信息。
     *
     * @param errorInfo 出错信息
     */
    public void setErrorInfo(String errorInfo) {
        this.errorInfo = errorInfo;
    }

    /**
     * 获取已经执行的次数，此信息由框架自动设置。
     *
     * @return 已经执行的次数，此信息由框架自动设置
     */
    public int getRanTimes() {
        return ranTimes;
    }

    /**
     * 设置已经执行的次数，此信息由框架自动设置。
     *
     * @param ranTimes 已经执行的次数，此信息由框架自动设置
     */
    public void setRanTimes(int ranTimes) {
        this.ranTimes = ranTimes;
    }

    /**
     * 获取执行状态，此信息由框架根据异常自动设置。
     *
     * @return 执行状态，此信息由框架根据异常自动设置
     */
    public int getState() {
        return state;
    }

    /**
     * 设置执行状态，此信息由框架根据异常自动设置。
     *
     * @param status 执行状态，此信息由框架根据异常自动设置
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
     * {@code TaskData} builder static inner class。
     *
     * @param <TP> 任务参数类型
     * @param <RD> 返回结果类型
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
         * TaskRunner 类全限定名，必须设置。
         */
        private String taskClass = "";

        /**
         * 任务标签，用于细分任务队列，支持多实例运行。
         */
        private String taskTag = "";

        /**
         * 延时毫秒数（&gt;0 即延时任务）。
         */
        private long taskDelay;

        /**
         * 执行参数，必须设置。
         */
        private TP taskParam;

        /**
         * 任务运行类型，默认为自动RPC，根据情况选择本地还是远程运行。
         */
        private int runType = RUN_TYPE_AUTO_RPC;

        /**
         * 重试类型，默认 AUTO。
         */
        private int retryType;

        /**
         * 指定运行目标。
         */
        private String runTarget = "default";

        /**
         * Builder 构造器（仅经 builder(...) 工厂方法创建）。
         */
        private Builder() {
        }

        /**
         * 设置关联TAG，由调用方设定，用于第三方统计信息。
         *
         * @param refTag 关联TAG，由调用方设定，用于第三方统计信息
         * @return this（链式调用）
         */
        public Builder<TP, RD> refTag(String refTag) {
            this.refTag = refTag;
            return this;
        }

        /**
         * 设置关联id，由调用方根据需要设置，用于第三方统计信息。
         *
         * @param refId 关联id，由调用方根据需要设置，用于第三方统计信息
         * @return this（链式调用）
         */
        public Builder<TP, RD> refId(long refId) {
            this.refId = refId;
            return this;
        }

        /**
         * 设置关联子id，由调用方根据需要设置，用于第三方统计信息。
         *
         * @param refSubId 关联子id，由调用方根据需要设置，用于第三方统计信息
         * @return this（链式调用）
         */
        public Builder<TP, RD> refSubId(long refSubId) {
            this.refSubId = refSubId;
            return this;
        }

        /**
         * 设置关联对象，此对象不存入数据库，但可以通过Listener来访问。
         *
         * @param refObject 关联对象，此对象不存入数据库，但可以通过Listener来访问
         * @return this（链式调用）
         */
        public Builder<TP, RD> refObject(Object refObject) {
            this.refObject = refObject;
            return this;
        }

        /**
         * 设置流量限制TAG。
         *
         * @param rateLimitTag 流量限制TAG
         * @return this（链式调用）
         */
        public Builder<TP, RD> rateLimitTag(String rateLimitTag) {
            this.rateLimitTag = rateLimitTag;
            return this;
        }

        /**
         * 设置要执行的 TaskRunner 类全限定名，必须由调用方设置。
         *
         * @param taskClass 要执行的 TaskRunner 类全限定名，必须由调用方设置
         * @return this（链式调用）
         */
        public Builder<TP, RD> taskClass(String taskClass) {
            this.taskClass = taskClass;
            return this;
        }

        /**
         * 设置任务标签，用于细分任务队列，支持多实例运行。
         *
         * @param taskTag 任务标签，用于细分任务队列，支持多实例运行
         * @return this（链式调用）
         */
        public Builder<TP, RD> taskTag(String taskTag) {
            this.taskTag = taskTag;
            return this;
        }

        /**
         * 设置延时毫秒数：&gt;0 即延时任务（载体自动路由）；负值为框架内部"已到期搬运"标记（走即时投递分支），调用方勿设。
         *
         * @param taskDelay 延时毫秒数：&gt;0 即延时任务（载体自动路由）；负值为框架内部"已到期搬运"标记（走即时投递分支），调用方勿设
         * @return this（链式调用）
         */
        public Builder<TP, RD> taskDelay(long taskDelay) {
            this.taskDelay = taskDelay;
            return this;
        }

        /**
         * 设置执行参数，必须由调用方设置。
         *
         * @param taskParam 执行参数，必须由调用方设置
         * @return this（链式调用）
         */
        public Builder<TP, RD> taskParam(TP taskParam) {
            this.taskParam = taskParam;
            return this;
        }

        /**
         * 设置任务运行类型，默认 AUTO_RPC：本机有匹配 runner 则本地运行，否则走远程 RPC。
         *
         * @param runType 任务运行类型，默认 AUTO_RPC：本机有匹配 runner 则本地运行，否则走远程 RPC
         * @return this（链式调用）
         */
        public Builder<TP, RD> runType(int runType) {
            this.runType = runType;
            return this;
        }

        /**
         * 设置重试类型，默认 AUTO（失败后按 runner 配置的 retryTimes 自动重试）。
         *
         * @param retryType 重试类型，默认 AUTO（失败后按 runner 配置的 retryTimes 自动重试）
         * @return this（链式调用）
         */
        public Builder<TP, RD> retryType(int retryType) {
            this.retryType = retryType;
            return this;
        }

        /**
         * 设置指定运行目标。
         *
         * @param runTarget 指定运行目标
         * @return this（链式调用）
         */
        public Builder<TP, RD> runTarget(String runTarget) {
            this.runTarget = runTarget;
            return this;
        }

        /**
         * 按已设置的参数构建实例。
         *
         * @return 构建出的对象
         */
        public TaskData<TP, RD> build() {
            return new TaskData<TP, RD>(this);
        }
    }
}
