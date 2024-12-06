package uw.task.entity;


import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TaskRunnerMetrics汇总类。
 */
public class TaskRunnerStats implements Serializable {


    /**
     * taskId
     */
    private long taskId;

    /**
     * 执行总计数
     */
    private final AtomicInteger numAll = new AtomicInteger();

    /**
     * 程序失败计数
     */
    private final AtomicInteger numFailProgram = new AtomicInteger();

    /**
     * 配置失败计数
     */
    private final AtomicInteger numFailConfig = new AtomicInteger();

    /**
     * 数据失败计数
     */
    private final AtomicInteger numFailData = new AtomicInteger();

    /**
     * 对方失败计数
     */
    private final AtomicInteger numFailPartner = new AtomicInteger();

    /**
     * 队列等待时间
     */
    private final AtomicInteger timeWaitQueue = new AtomicInteger();

    /**
     * 超时等待时间
     */
    private final AtomicInteger timeWaitDelay = new AtomicInteger();

    /**
     * 运行时间
     */
    private final AtomicInteger timeRun = new AtomicInteger();

    /**
     * 当前队列长度。
     */
    private int queueSize;

    /**
     * 消费者数量。
     */
    private int consumerNum;


    public TaskRunnerStats(long taskId) {
        this.taskId = taskId;
    }

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
