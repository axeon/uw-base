package uw.task.entity;


import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 队列任务执行统计聚合。
 *
 * <p>按 taskId 在 {@link uw.task.util.TaskStatsService} 中聚合单次或多次队列任务执行的计数与耗时，
 * 周期性随主机状态报告上报至 task-center 后清空，并附带上报瞬间的队列长度与消费者数。
 * 所有计数/耗时字段为 {@link AtomicInteger}，支持多消费者线程并发累加。</p>
 */
public class TaskRunnerStats implements Serializable {


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
     * 配置失败计数（如超过流量限制）。
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
     * 队列等待耗时累计（毫秒，queueDate 到 consumeDate）。
     */
    private final AtomicInteger timeWaitQueue = new AtomicInteger();

    /**
     * 限速/延时等待耗时累计（毫秒，consumeDate 到 runDate）。
     */
    private final AtomicInteger timeWaitDelay = new AtomicInteger();

    /**
     * 运行耗时累计（毫秒，runDate 到 finishDate）。
     */
    private final AtomicInteger timeRun = new AtomicInteger();

    /**
     * 上报瞬间的队列积压消息数（非累计，由服务端被动查询填充）。
     */
    private int queueSize;

    /**
     * 上报瞬间的消费者数量（非累计，由服务端被动查询填充）。
     */
    private int consumerNum;


    /**
     * @param taskId 任务配置 id
     */
    public TaskRunnerStats(long taskId) {
        this.taskId = taskId;
    }

    /**
     * 原子累加本次执行的各项计数与耗时，返回自身便于链式调用。
     *
     * @param numAll          本次执行总计数
     * @param numFailProgram  程序失败计数
     * @param numFailConfig   配置失败计数
     * @param numFailData     数据失败计数
     * @param numFailPartner  对方失败计数
     * @param timeWaitQueue   本次队列等待耗时（毫秒）
     * @param timeWaitDelay   本次限速/延时等待耗时（毫秒）
     * @param timeRun         本次运行耗时（毫秒）
     * @return 当前对象（链式）
     */
    public TaskRunnerStats addMetrics(int numAll, int numFailProgram, int numFailConfig, int numFailData, int numFailPartner, int timeWaitQueue, int timeWaitDelay, int timeRun) {
        this.numAll.addAndGet(numAll);
        this.numFailConfig.addAndGet(numFailConfig);
        this.numFailPartner.addAndGet(numFailPartner);
        this.numFailProgram.addAndGet(numFailProgram);
        this.numFailData.addAndGet(numFailData);
        this.timeWaitQueue.addAndGet(timeWaitQueue);
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

    public int getTimeWaitQueue() {
        return timeWaitQueue.get();
    }

    public void setTimeWaitQueue(int timeWaitQueue) {
        this.timeWaitQueue.set(timeWaitQueue);
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
