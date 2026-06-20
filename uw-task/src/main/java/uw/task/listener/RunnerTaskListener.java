package uw.task.listener;

import uw.task.TaskData;

/**
 * 队列任务（TaskRunner）的监听器。
 *
 * <p>实现此接口并通过 {@link uw.task.TaskListenerManager#addRunnerListener(RunnerTaskListener)} 注册后，
 * 框架在队列任务执行完成后回调 {@link #onPostExecute}（用于埋点、链路追踪、自定义指标采集等）。
 * 注意：当前框架在队列任务执行流程中<b>仅调用 {@code onPostExecute}</b>，
 * {@code onPreExecute} 保留为接口契约，暂未被框架调用。
 * 监听器内抛出的异常会被框架吞掉（仅记录日志），不会影响任务执行流程。</p>
 *
 * @author axeon
 */
public interface RunnerTaskListener {

    /**
     * 任务执行前的回调（当前框架未在队列任务流程中调用，保留为接口契约）。
     *
     * @param data 本次执行的任务数据（此时仅包含入参与队列信息，结果尚未产生）
     */
    void onPreExecute(TaskData data);

    /**
     * 任务执行后的回调。
     *
     * @param data 本次执行的任务数据（已包含执行结果、状态、耗时等信息）
     */
    void onPostExecute(TaskData data);

}
