package uw.task.listener;

import uw.task.entity.TaskCronerLog;

/**
 * 定时任务（TaskCroner）的监听器。
 *
 * <p>实现此接口并通过 {@link uw.task.TaskListenerManager#addCronerListener(CronerTaskListener)} 注册后，
 * 框架在每次定时任务执行的前后会回调对应方法，可用于埋点、链路追踪、自定义指标采集等。
 * 监听器内抛出的异常会被框架吞掉（仅记录日志），不会影响任务执行流程。</p>
 *
 * @author axeon
 */
public interface CronerTaskListener {

    /**
     * 任务执行前的回调。
     *
     * @param data 本次执行的日志对象（此时仅包含任务配置与计划执行信息，结果尚未产生）
     */
    void onPreExecute(TaskCronerLog data);

    /**
     * 任务执行后的回调。
     *
     * @param data 本次执行的日志对象（已包含执行结果、状态、耗时等信息）
     */
    void onPostExecute(TaskCronerLog data);

}
