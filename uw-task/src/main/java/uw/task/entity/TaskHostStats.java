package uw.task.entity;

import java.io.Serializable;
import java.util.List;

/**
 * 主机状态上报数据。
 *
 * <p>由任务执行主机周期性采集并上报到 task-center（见 {@code TaskServiceRegistrar.reportHostInfo}），
 * 含应用标识、JVM 内存与线程指标、注册的任务数量、以及各任务的执行统计（{@link TaskCronerStats}/{@link TaskRunnerStats}）。</p>
 *
 * @author axeon
 */
public class TaskHostStats implements Serializable {

    /**
     * 默认构造器。
     */
    public TaskHostStats() {
    }

    /**
     * 主机记录 id（首次上报时为 0，服务端分配后回传，后续上报携带以定位记录）。
     */
    private long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用版本
     */
    private String appVersion;

    /**
     * app主机
     */
    private String appHost;

    /**
     * app端口
     */
    private int appPort;

    /**
     * 任务项目
     */
    private String taskProject;

    /**
     * 运行目标
     */
    private String runTarget;

    /**
     * 定时任务数量
     */
    private int cronerNum;

    /**
     * 队列任务数量
     */
    private int runnerNum;

    /**
     * 延时任务积压数（zset 待搬运数，project 级）：时间轮中尚未到期的任务总量。
     */
    private long delayBacklog;

    /**
     * JVM最大内存（Runtime.maxMemory，堆上限）
     */
    private long jvmMemMax;

    /**
     * jvm内存总数
     */
    private long jvmMemTotal;

    /**
     * jvm空闲内存
     */
    private long jvmMemFree;

    /**
     * 活跃线程
     */
    private int threadActive;

    /**
     * 峰值线程
     */
    private int threadPeak;

    /**
     * 守护线程
     */
    private int threadDaemon;

    /**
     * 累计启动线程
     */
    private long threadStarted;

    /**
     * croner运行统计信息。
     */
    private List<TaskCronerStats> taskCronerStatsList;

    /**
     * runner运行统计信息。
     */
    private List<TaskRunnerStats> taskRunnerStatsList;

    /**
     * 获取主机记录 id（首次上报时为 0，服务端分配后回传，后续上报携带以定位记录）。
     *
     * @return 主机记录 id（首次上报时为 0，服务端分配后回传，后续上报携带以定位记录）
     */
    public long getId() {
        return id;
    }

    /**
     * 设置主机记录 id（首次上报时为 0，服务端分配后回传，后续上报携带以定位记录）。
     *
     * @param id 主机记录 id（首次上报时为 0，服务端分配后回传，后续上报携带以定位记录）
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * 获取应用名称。
     *
     * @return 应用名称
     */
    public String getAppName() {
        return appName;
    }

    /**
     * 设置应用名称。
     *
     * @param appName 应用名称
     */
    public void setAppName(String appName) {
        this.appName = appName;
    }

    /**
     * 获取应用版本。
     *
     * @return 应用版本
     */
    public String getAppVersion() {
        return appVersion;
    }

    /**
     * 设置应用版本。
     *
     * @param appVersion 应用版本
     */
    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    /**
     * 获取app端口。
     *
     * @return app端口
     */
    public int getAppPort() {
        return appPort;
    }

    /**
     * 设置app端口。
     *
     * @param appPort app端口
     */
    public void setAppPort(int appPort) {
        this.appPort = appPort;
    }

    /**
     * 获取app主机。
     *
     * @return app主机
     */
    public String getAppHost() {
        return appHost;
    }

    /**
     * 设置app主机。
     *
     * @param appHost app主机
     */
    public void setAppHost(String appHost) {
        this.appHost = appHost;
    }

    /**
     * 获取任务项目。
     *
     * @return 任务项目
     */
    public String getTaskProject() {
        return taskProject;
    }

    /**
     * 设置任务项目。
     *
     * @param taskProject 任务项目
     */
    public void setTaskProject(String taskProject) {
        this.taskProject = taskProject;
    }

    /**
     * 获取运行目标。
     *
     * @return 运行目标
     */
    public String getRunTarget() {
        return runTarget;
    }

    /**
     * 设置运行目标。
     *
     * @param runTarget 运行目标
     */
    public void setRunTarget(String runTarget) {
        this.runTarget = runTarget;
    }

    /**
     * 获取定时任务数量。
     *
     * @return 定时任务数量
     */
    public int getCronerNum() {
        return cronerNum;
    }

    /**
     * 设置定时任务数量。
     *
     * @param cronerNum 定时任务数量
     */
    public void setCronerNum(int cronerNum) {
        this.cronerNum = cronerNum;
    }

    /**
     * 获取队列任务数量。
     *
     * @return 队列任务数量
     */
    public int getRunnerNum() {
        return runnerNum;
    }

    /**
     * 设置队列任务数量。
     *
     * @param runnerNum 队列任务数量
     */
    public void setRunnerNum(int runnerNum) {
        this.runnerNum = runnerNum;
    }

    /**
     * 获取延时任务积压数（zset 待搬运数，project 级）：时间轮中尚未到期的任务总量。
     *
     * @return 延时任务积压数（zset 待搬运数，project 级）：时间轮中尚未到期的任务总量
     */
    public long getDelayBacklog() {
        return delayBacklog;
    }

    /**
     * 设置延时任务积压数（zset 待搬运数，project 级）：时间轮中尚未到期的任务总量。
     *
     * @param delayBacklog 延时任务积压数（zset 待搬运数，project 级）：时间轮中尚未到期的任务总量
     */
    public void setDelayBacklog(long delayBacklog) {
        this.delayBacklog = delayBacklog;
    }

    /**
     * 获取JVM最大内存（Runtime.maxMemory，堆上限）。
     *
     * @return JVM最大内存（Runtime.maxMemory，堆上限）
     */
    public long getJvmMemMax() {
        return jvmMemMax;
    }

    /**
     * 设置JVM最大内存（Runtime.maxMemory，堆上限）。
     *
     * @param jvmMemMax JVM最大内存（Runtime.maxMemory，堆上限）
     */
    public void setJvmMemMax(long jvmMemMax) {
        this.jvmMemMax = jvmMemMax;
    }

    /**
     * 获取jvm内存总数。
     *
     * @return jvm内存总数
     */
    public long getJvmMemTotal() {
        return jvmMemTotal;
    }

    /**
     * 设置jvm内存总数。
     *
     * @param jvmMemTotal jvm内存总数
     */
    public void setJvmMemTotal(long jvmMemTotal) {
        this.jvmMemTotal = jvmMemTotal;
    }

    /**
     * 获取jvm空闲内存。
     *
     * @return jvm空闲内存
     */
    public long getJvmMemFree() {
        return jvmMemFree;
    }

    /**
     * 设置jvm空闲内存。
     *
     * @param jvmMemFree jvm空闲内存
     */
    public void setJvmMemFree(long jvmMemFree) {
        this.jvmMemFree = jvmMemFree;
    }

    /**
     * 获取活跃线程。
     *
     * @return 活跃线程
     */
    public int getThreadActive() {
        return threadActive;
    }

    /**
     * 设置活跃线程。
     *
     * @param threadActive 活跃线程
     */
    public void setThreadActive(int threadActive) {
        this.threadActive = threadActive;
    }

    /**
     * 获取峰值线程。
     *
     * @return 峰值线程
     */
    public int getThreadPeak() {
        return threadPeak;
    }

    /**
     * 设置峰值线程。
     *
     * @param threadPeak 峰值线程
     */
    public void setThreadPeak(int threadPeak) {
        this.threadPeak = threadPeak;
    }

    /**
     * 获取守护线程。
     *
     * @return 守护线程
     */
    public int getThreadDaemon() {
        return threadDaemon;
    }

    /**
     * 设置守护线程。
     *
     * @param threadDaemon 守护线程
     */
    public void setThreadDaemon(int threadDaemon) {
        this.threadDaemon = threadDaemon;
    }

    /**
     * 获取累计启动线程。
     *
     * @return 累计启动线程
     */
    public long getThreadStarted() {
        return threadStarted;
    }

    /**
     * 设置累计启动线程。
     *
     * @param threadStarted 累计启动线程
     */
    public void setThreadStarted(long threadStarted) {
        this.threadStarted = threadStarted;
    }

    /**
     * 获取croner运行统计信息。
     *
     * @return croner运行统计信息
     */
    public List<TaskCronerStats> getTaskCronerStatsList() {
        return taskCronerStatsList;
    }

    /**
     * 设置croner运行统计信息。
     *
     * @param taskCronerStatsList croner运行统计信息
     */
    public void setTaskCronerStatsList(List<TaskCronerStats> taskCronerStatsList) {
        this.taskCronerStatsList = taskCronerStatsList;
    }

    /**
     * 获取runner运行统计信息。
     *
     * @return runner运行统计信息
     */
    public List<TaskRunnerStats> getTaskRunnerStatsList() {
        return taskRunnerStatsList;
    }

    /**
     * 设置runner运行统计信息。
     *
     * @param taskRunnerStatsList runner运行统计信息
     */
    public void setTaskRunnerStatsList(List<TaskRunnerStats> taskRunnerStatsList) {
        this.taskRunnerStatsList = taskRunnerStatsList;
    }
}
