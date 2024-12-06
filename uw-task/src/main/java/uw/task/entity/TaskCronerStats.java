package uw.task.entity;


import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TaskCronerMetrics汇总类。
 */
public class TaskCronerStats implements Serializable {


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
     * 超时等待
     */
    private final AtomicInteger timeWait = new AtomicInteger();

    /**
     * 运行时间
     */
    private final AtomicInteger timeRun = new AtomicInteger();

    public TaskCronerStats(long taskId) {
        this.taskId = taskId;
    }

    public TaskCronerStats addMetrics(int numAll, int numFailProgram, int numFailConfig, int numFailData, int numFailPartner, int timeWait, int timeRun) {
        this.numAll.addAndGet(numAll);
        this.numFailConfig.addAndGet(numFailConfig);
        this.numFailPartner.addAndGet(numFailPartner);
        this.numFailProgram.addAndGet(numFailProgram);
        this.numFailData.addAndGet(numFailData);
        this.timeWait.addAndGet(timeWait);
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

    public int getTimeWait() {
        return timeWait.get();
    }

    public void setTimeWait(int timeWait) {
        this.timeWait.set(timeWait);
    }

    public int getTimeRun() {
        return timeRun.get();
    }

    public void setTimeRun(int timeRun) {
        this.timeRun.set(timeRun);
    }


}
