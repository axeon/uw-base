package uw.task.exception;

/**
 * 任务伙伴异常，用于抛出因接口方/合作方错误导致的异常。
 * <p>
 * 当外部接口调用失败、网络错误、返回不符合预期等场景，用以与内部数据错误
 * （{@link TaskDataException}）区分，便于 uw-task 调度框架按"外部原因 vs 内部原因"
 * 采取不同的重试与告警策略。
 * <p>
 * <b>关于本类的存在</b>：uw-httpclient 的 {@code HttpRequestException} 继承自本类，
 * 以便在 uw-task 环境内被识别为"合作伙伴/网络错误"。本类与 {@link TaskDataException}
 * 一样，是 uw-task 同包名的本地桥接副本，使 uw-httpclient 不必强依赖 uw-task；
 * 详见 {@link TaskDataException} 的说明。
 *
 * @author axeon
 * @see TaskDataException
 */
public class TaskPartnerException extends RuntimeException {

    /**
     * 构造一个不带消息和原因的伙伴异常。
     */
    public TaskPartnerException() {
        super();
    }

    /**
     * 构造一个带消息的伙伴异常。
     *
     * @param msg 异常消息。
     */
    public TaskPartnerException(String msg) {
        super(msg);
    }

    /**
     * 构造一个带原因的伙伴异常。
     *
     * @param nestedThrowable 原始异常。
     */
    public TaskPartnerException(Throwable nestedThrowable) {
        super(nestedThrowable);
    }

    /**
     * 构造一个带消息和原因的伙伴异常。
     *
     * @param msg             异常消息。
     * @param nestedThrowable 原始异常。
     */
    public TaskPartnerException(String msg, Throwable nestedThrowable) {
        super(msg, nestedThrowable);
    }

}