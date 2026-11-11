package uw.task.exception;

/**
 * 任务数据异常：表示数据本身有问题（格式非法、缺失、业务规则不满足、解析失败等），属于"重试也解决不了"的错误。
 *
 * <p>uw-task 在 {@code runTask} 中捕获后标记任务状态为 {@code TaskData.STATE_FAIL_DATA}，<b>不会触发自动重试</b>；
 * 与之相对的是 {@link TaskPartnerException}（合作方异常，会触发重试）。</p>
 *
 * <p>本类放在 uw-common（包名沿用 {@code uw.task.exception}）：uw-httpclient 的 {@code DataMapperException}
 * 继承本类，uw-task 按本类判定失败类型，两者都依赖 uw-common，全平台只此一份。</p>
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
