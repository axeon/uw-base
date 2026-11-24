package uw.task.listener;

import uw.task.TaskData;
import uw.task.TaskListenerManager;

/**
 * 队列任务（TaskRunner）的监听器。
 *
 * <p>实现此接口并通过 {@link TaskListenerManager#addRunnerListener(TaskRunnerListener)} 注册后，
 * 框架在队列任务执行前后回调 {@link #onPreExecute} / {@link #onPostExecute}（用于埋点、链路追踪、自定义指标采集等）。
 * 限速等待超时（未实际执行）的任务不触发 onPreExecute，仅触发 onPostExecute。
 * 监听器内抛出的异常会被框架吞掉（仅记录日志），不会影响任务执行流程。</p>
 *
 * @author axeon
 */
public interface TaskRunnerListener {

    /**
     * 任务执行前的回调。
     *
     * @param data 本次执行的任务数据（此时仅包含入参与队列信息，结果尚未产生）
     */
    void onPreExecute(TaskData<?, ?> data);

    /**
     * 任务执行后的回调。
     *
     * @param data 本次执行的任务数据（已包含执行结果、状态、耗时等信息）
     */
    void onPostExecute(TaskData<?, ?> data);
}
