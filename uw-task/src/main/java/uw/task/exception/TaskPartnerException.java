package uw.task.exception;

/**
 * 任务合作方（第三方接口）异常。
 *
 * <p>在 {@code runTask} 中抛出此异常，表示任务所依赖的合作方/第三方接口出现错误（如超时、返回错误码、限流等），
 * 属于"可能通过重试解决"的瞬时性错误。框架捕获后会标记任务状态为
 * {@link uw.task.TaskData#STATE_FAIL_PARTNER}，并<b>根据配置 {@code retryTimesByPartner} 触发自动重试</b>。</p>
 *
 * <p>与之相对：{@link TaskDataException}（数据异常，不触发重试）。</p>
 *
 * @author axeon
 */
public class TaskPartnerException extends RuntimeException {

    /**
     * 构造一个不带详细信息的合作方异常。
     */
    public TaskPartnerException() {
        super();
    }

    /**
     * 构造一个带错误信息的合作方异常。
     *
     * @param msg 错误信息
     */
    public TaskPartnerException(String msg) {
        super(msg);
    }

    /**
     * 构造一个由指定原因引发的异常。
     *
     * @param nestedThrowable 根因异常
     */
    public TaskPartnerException(Throwable nestedThrowable) {
        super(nestedThrowable);
    }

    /**
     * 构造一个带错误信息和根因的异常。
     *
     * @param msg             错误信息
     * @param nestedThrowable 根因异常
     */
    public TaskPartnerException(String msg, Throwable nestedThrowable) {
        super(msg, nestedThrowable);
    }

}