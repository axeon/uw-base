package uw.task.entity;

import uw.task.util.TaskStatsService;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 定时任务执行统计聚合。
 *
 * <p>按 taskId 在 {@link TaskStatsService} 中聚合单次或多次定时任务执行的计数与耗时，
 * 周期性随主机状态报告上报至 task-center 后清空。所有计数/耗时字段为 {@link AtomicInteger}，
 * 支持多线程（多个 croner 执行线程）并发累加。</p>
 */
public class TaskCronerStats implements Serializable {

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
     * 等待耗时累计（毫秒，scheduleDate 到 runDate）。
     */
    private final AtomicInteger timeWait = new AtomicInteger();

    /**
     * 运行耗时累计（毫秒，runDate 到 finishDate）。
     */
    private final AtomicInteger timeRun = new AtomicInteger();

    /**
     * 构建任务统计对象。
     *
     * @param taskId 任务配置 id
     */
    public TaskCronerStats(long taskId) {
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
     * @param timeWait        本次等待耗时（毫秒）
     * @param timeRun         本次运行耗时（毫秒）
     * @return 当前对象（链式）
     */
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

    /**
     * 获取任务配置 id。
     *
     * @return 任务配置 id
     */
    public long getTaskId() {
        return taskId;
    }

    /**
     * 设置任务配置 id。
     *
     * @param taskId 任务配置 id
     */
    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    /**
     * 获取执行总计数。
     *
     * @return 执行总计数
     */
    public int getNumAll() {
        return numAll.get();
    }

    /**
     * 设置执行总计数。
     *
     * @param numAll 执行总计数
     */
    public void setNumAll(int numAll) {
        this.numAll.set(numAll);
    }

    /**
     * 获取程序失败计数。
     *
     * @return 程序失败计数
     */
    public int getNumFailProgram() {
        return numFailProgram.get();
    }

    /**
     * 设置程序失败计数。
     *
     * @param numFailProgram 程序失败计数
     */
    public void setNumFailProgram(int numFailProgram) {
        this.numFailProgram.set(numFailProgram);
    }

    /**
     * 获取配置失败计数（如超过流量限制）。
     *
     * @return 配置失败计数（如超过流量限制）
     */
    public int getNumFailConfig() {
        return numFailConfig.get();
    }

    /**
     * 设置配置失败计数（如超过流量限制）。
     *
     * @param numFailConfig 配置失败计数（如超过流量限制）
     */
    public void setNumFailConfig(int numFailConfig) {
        this.numFailConfig.set(numFailConfig);
    }

    /**
     * 获取数据失败计数。
     *
     * @return 数据失败计数
     */
    public int getNumFailData() {
        return numFailData.get();
    }

    /**
     * 设置数据失败计数。
     *
     * @param numFailData 数据失败计数
     */
    public void setNumFailData(int numFailData) {
        this.numFailData.set(numFailData);
    }

    /**
     * 获取对方（合作方）失败计数。
     *
     * @return 对方（合作方）失败计数
     */
    public int getNumFailPartner() {
        return numFailPartner.get();
    }

    /**
     * 设置对方（合作方）失败计数。
     *
     * @param numFailPartner 对方（合作方）失败计数
     */
    public void setNumFailPartner(int numFailPartner) {
        this.numFailPartner.set(numFailPartner);
    }

    /**
     * 获取等待耗时累计（毫秒，scheduleDate 到 runDate）。
     *
     * @return 等待耗时累计（毫秒，scheduleDate 到 runDate）
     */
    public int getTimeWait() {
        return timeWait.get();
    }

    /**
     * 设置等待耗时累计（毫秒，scheduleDate 到 runDate）。
     *
     * @param timeWait 等待耗时累计（毫秒，scheduleDate 到 runDate）
     */
    public void setTimeWait(int timeWait) {
        this.timeWait.set(timeWait);
    }

    /**
     * 获取运行耗时累计（毫秒，runDate 到 finishDate）。
     *
     * @return 运行耗时累计（毫秒，runDate 到 finishDate）
     */
    public int getTimeRun() {
        return timeRun.get();
    }

    /**
     * 设置运行耗时累计（毫秒，runDate 到 finishDate）。
     *
     * @param timeRun 运行耗时累计（毫秒，runDate 到 finishDate）
     */
    public void setTimeRun(int timeRun) {
        this.timeRun.set(timeRun);
    }
}
