package uw.task.exception;

/**
 * 任务数据异常。
 *
 * <p>在 {@code runTask} 中抛出此异常，表示任务入参数据本身存在问题（如格式非法、缺失、业务规则不满足等），
 * 属于"不可通过重试解决"的错误。框架捕获后会标记任务状态为
 * {@link uw.task.TaskData#STATE_FAIL_DATA}，<b>不会触发自动重试</b>。</p>
 *
 * <p>与之相对：{@link TaskPartnerException}（合作方异常，会触发重试）。</p>
 *
 * @author axeon
 */
public class TaskDataException extends RuntimeException {

    /**
     * 构造一个不带详细信息的任务数据异常。
     */
    public TaskDataException() {
        super();
    }

    /**
     * 构造一个带错误信息的任务数据异常。
     *
     * @param msg 错误信息
     */
    public TaskDataException(String msg) {
        super(msg);
    }

    /**
     * 构造一个由指定原因引发的任务数据异常。
     *
     * @param nestedThrowable 根因异常
     */
    public TaskDataException(Throwable nestedThrowable) {
        super(nestedThrowable);
    }

    /**
     * 构造一个带错误信息和根因的任务数据异常。
     *
     * @param msg             错误信息
     * @param nestedThrowable 根因异常
     */
    public TaskDataException(String msg, Throwable nestedThrowable) {
        super(msg, nestedThrowable);
    }

}