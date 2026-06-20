package uw.task.exception;

/**
 * 任务框架内部运行期异常。
 *
 * <p>由框架自身抛出，用于标识框架级的运行错误，例如：
 * <ul>
 *   <li>本地任务执行入口遇到非本地任务类型（{@link uw.task.TaskFactory#runTaskLocal}）；</li>
 *   <li>RabbitMQ channelMax 资源耗尽、RPC 调用超时等导致任务派发失败。</li>
 * </ul>
 * 此异常<b>不</b>对应任务执行的业务状态（不映射到 STATE_FAIL_* ），而是框架在派发/调度阶段的非预期失败。</p>
 *
 * @author axeon
 */
public class TaskRuntimeException extends RuntimeException {

    /**
     * 构造一个不带详细信息的运行期异常。
     */
    public TaskRuntimeException() {
        super();
    }

    /**
     * 构造一个带错误信息的运行期异常。
     *
     * @param msg 错误信息
     */
    public TaskRuntimeException(String msg) {
        super(msg);
    }

    /**
     * 构造一个由指定原因引发的运行期异常。
     *
     * @param nestedThrowable 根因异常
     */
    public TaskRuntimeException(Throwable nestedThrowable) {
        super(nestedThrowable);
    }

    /**
     * 构造一个带错误信息和根因的运行期异常。
     *
     * @param msg             错误信息
     * @param nestedThrowable 根因异常
     */
    public TaskRuntimeException(String msg, Throwable nestedThrowable) {
        super(msg, nestedThrowable);
    }

}