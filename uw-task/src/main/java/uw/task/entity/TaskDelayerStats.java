package uw.task.entity;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 延迟任务执行统计聚合。
 *
 * <p>按 taskId 在 {@link uw.task.util.TaskStatsService} 中聚合单次或多次延迟任务执行的计数与耗时，
 * 周期性随主机状态报告上报至 task-center 后清空，并附带上报瞬间的 zset 积压数与执行线程数。
 * 所有计数/耗时字段为 {@link AtomicInteger}，支持多 worker 线程并发累加。</p>
 *
 * <p>与 {@link TaskRunnerStats} 的区别：延迟任务不经 RabbitMQ 队列，无队列等待（timeWaitQueue），
 * 用 {@link #timeWaitDelay}（runAt→consumeDate，即到期到被 poll 的实际延迟等待）反映 poll/线程池是否积压。</p>
 *
 * @author axeon
 */
public class TaskDelayerStats implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务配置 id。
     */
    private long taskId;

    /**
     * 执行总计数。
     */
    private final AtomicInteger numAll = new AtomicInteger();

    /**
     * 程序失败计数。
     */
    private final AtomicInteger numFailProgram = new AtomicInteger();

    /**
     * 配置失败计数（如连续限速超限放弃）。
     */
    private final AtomicInteger numFailConfig = new AtomicInteger();

    /**
     * 数据失败计数。
     */
    private final AtomicInteger numFailData = new AtomicInteger();

    /**
     * 对方（合作方）失败计数。
     */
    private final AtomicInteger numFailPartner = new AtomicInteger();

    /**
     * 实际延迟等待耗时累计（毫秒，runAt 到 consumeDate），反映 poll 间隔与线程池积压。
     */
    private final AtomicInteger timeWaitDelay = new AtomicInteger();

    /**
     * 运行耗时累计（毫秒，runDate 到 finishDate）。
     */
    private final AtomicInteger timeRun = new AtomicInteger();

    /**
     * 上报瞬间的 zset 积压消息数（非累计，由服务端被动查询填充）。
     */
    private int queueSize;

    /**
     * 上报瞬间的执行线程数（非累计，由服务端被动查询填充）。
     */
    private int consumerNum;

    public TaskDelayerStats() {
    }

    public TaskDelayerStats(long taskId) {
        this.taskId = taskId;
    }

    /**
     * 原子累加本次执行的各项计数与耗时，返回自身便于链式调用。
     *
     * @param numAll         本次执行总计数
     * @param numFailProgram 程序失败计数
     * @param numFailConfig  配置失败计数
     * @param numFailData    数据失败计数
     * @param numFailPartner 对方失败计数
     * @param timeWaitDelay  本次实际延迟等待耗时（毫秒）
     * @param timeRun        本次运行耗时（毫秒）
     * @return 当前对象（链式）
     */
    public TaskDelayerStats addMetrics(int numAll, int numFailProgram, int numFailConfig, int numFailData, int numFailPartner, int timeWaitDelay, int timeRun) {
        this.numAll.addAndGet(numAll);
        this.numFailProgram.addAndGet(numFailProgram);
        this.numFailConfig.addAndGet(numFailConfig);
        this.numFailData.addAndGet(numFailData);
        this.numFailPartner.addAndGet(numFailPartner);
        this.timeWaitDelay.addAndGet(timeWaitDelay);
        this.timeRun.addAndGet(timeRun);
        return this;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public int getNumAll() {
        return numAll.get();
    }

    public void setNumAll(int numAll) {
        this.numAll.set(numAll);
    }

    public int getNumFailProgram() {
        return numFailProgram.get();
    }

    public void setNumFailProgram(int numFailProgram) {
        this.numFailProgram.set(numFailProgram);
    }

    public int getNumFailConfig() {
        return numFailConfig.get();
    }

    public void setNumFailConfig(int numFailConfig) {
        this.numFailConfig.set(numFailConfig);
    }

    public int getNumFailData() {
        return numFailData.get();
    }

    public void setNumFailData(int numFailData) {
        this.numFailData.set(numFailData);
    }

    public int getNumFailPartner() {
        return numFailPartner.get();
    }

    public void setNumFailPartner(int numFailPartner) {
        this.numFailPartner.set(numFailPartner);
    }

    public int getTimeWaitDelay() {
        return timeWaitDelay.get();
    }

    public void setTimeWaitDelay(int timeWaitDelay) {
        this.timeWaitDelay.set(timeWaitDelay);
    }

    public int getTimeRun() {
        return timeRun.get();
    }

    public void setTimeRun(int timeRun) {
        this.timeRun.set(timeRun);
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public int getConsumerNum() {
        return consumerNum;
    }

    public void setConsumerNum(int consumerNum) {
        this.consumerNum = consumerNum;
    }
}
