package uw.task.exception;

/**
 * 任务伙伴异常，用于抛出因接口方/合作方错误导致的异常。
 * <p>
 * 当外部接口调用失败、返回不符合预期等场景，用以与内部数据错误区分。
 *
 * @author axeon
 */
public class TaskPartnerException extends RuntimeException {

    public TaskPartnerException() {
        super();
    }

    public TaskPartnerException(String msg) {
        super(msg);
    }

    public TaskPartnerException(Throwable nestedThrowable) {
        super(nestedThrowable);
    }

    public TaskPartnerException(String msg, Throwable nestedThrowable) {
        super(msg, nestedThrowable);
    }

}