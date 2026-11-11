package uw.task.exception;

/**
 * 任务合作方（第三方接口）异常：表示所依赖的合作方/第三方接口出错（超时、网络错误、返回错误码、限流等），
 * 属于"重试可能解决"的瞬时性错误。
 *
 * <p>uw-task 在 {@code runTask} 中捕获后标记任务状态为 {@code TaskData.STATE_FAIL_PARTNER}，
 * 并<b>按配置 {@code retryTimesByPartner} 自动重试</b>；与之相对的是 {@link TaskDataException}（数据异常，不重试）。</p>
 *
 * <p>本类放在 uw-common（包名沿用 {@code uw.task.exception}）：uw-httpclient 的 {@code HttpRequestException}
 * 继承本类，uw-task 按本类判定失败类型，两者都依赖 uw-common，全平台只此一份。</p>
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
     * 构造一个由指定原因引发的合作方异常。
     *
     * @param nestedThrowable 根因异常
     */
    public TaskPartnerException(Throwable nestedThrowable) {
        super(nestedThrowable);
    }

    /**
     * 构造一个带错误信息和根因的合作方异常。
     *
     * @param msg             错误信息
     * @param nestedThrowable 根因异常
     */
    public TaskPartnerException(String msg, Throwable nestedThrowable) {
        super(msg, nestedThrowable);
    }

}
