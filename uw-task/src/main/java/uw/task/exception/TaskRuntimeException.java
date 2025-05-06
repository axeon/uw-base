package uw.task.exception;

/**
 * 任务运行期异常。当前仅用于运行本地任务使用。
 *
 * @author axeon
 */
public class TaskRuntimeException extends RuntimeException {

    public TaskRuntimeException() {
        super();
    }

    public TaskRuntimeException(String msg) {
        super(msg);
    }

    public TaskRuntimeException(Throwable nestedThrowable) {
        super(nestedThrowable);
    }

    public TaskRuntimeException(String msg, Throwable nestedThrowable) {
        super(msg, nestedThrowable);
    }

}