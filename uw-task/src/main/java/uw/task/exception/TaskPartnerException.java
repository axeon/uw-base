package uw.task.exception;

/**
 * 任务异常，用于抛出接口方错误导致的异常。
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