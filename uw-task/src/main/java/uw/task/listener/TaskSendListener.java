package uw.task.listener;

import uw.task.TaskData;

/**
 * 任务投递监听器（sendToQueue 投递链路侧，与执行侧 TaskRunnerListener/TaskCronerListener 互补）。
 *
 * <p>实现此接口并通过 {@link uw.task.TaskListenerManager#addSendListener(TaskSendListener)} 注册后，
 * 框架在延时任务入 zset、即时投递最终失败（退避重试耗尽）时回调对应方法，可用于投递侧埋点、
 * 告警上报等。监听器内抛出的异常会被框架吞掉（仅记录日志），不影响投递流程。</p>
 *
 * @author axeon
 */
public interface TaskSendListener {

    /**
     * 延时任务提交到 Redis zset 搬运（MQ 原生延迟不可用路径）。
     *
     * @param taskData  延时任务（id/queueDate 已写入，taskDelay&gt;0）
     * @param queueName 到期转投的目标 MQ 队列名
     */
    void onDelaySubmit(TaskData<?, ?> taskData, String queueName);

    /**
     * 即时投递最终失败（退避重试全部耗尽或线程被中断退出重试，即将抛 TaskRuntimeException 前）。
     *
     * @param taskData 投递失败的任务
     * @param queueName 目标队列名
     * @param attempts 已尝试次数（等于重试上限；0=线程被中断退出，停机场景）
     */
    void onSendFail(TaskData<?, ?> taskData, String queueName, int attempts);
}
