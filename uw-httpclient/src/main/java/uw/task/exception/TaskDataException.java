package uw.task.exception;

/**
 * 数据异常，用于抛出各种数据错误的信息。
 * <p>
 * 作为任务执行过程中数据相关错误（解析失败、格式不符、约束冲突等）的统一运行时异常基类。
 *
 * @author axeon
 */
public class TaskDataException extends RuntimeException {

    public TaskDataException() {
        super();
    }

    public TaskDataException(String msg) {
        super(msg);
    }

    public TaskDataException(Throwable nestedThrowable) {
        super(nestedThrowable);
    }

    public TaskDataException(String msg, Throwable nestedThrowable) {
        super(msg, nestedThrowable);
    }

}