package uw.task.entity;

import java.io.Serializable;
import java.util.List;

/**
 * TaskHostStats 实体类
 */
public class TaskHostStats implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
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
     * jvm内存总数
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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public int getAppPort() {
        return appPort;
    }

    public void setAppPort(int appPort) {
        this.appPort = appPort;
    }

    public String getAppHost() {
        return appHost;
    }

    public void setAppHost(String appHost) {
        this.appHost = appHost;
    }

    public String getTaskProject() {
        return taskProject;
    }

    public void setTaskProject(String taskProject) {
        this.taskProject = taskProject;
    }

    public String getRunTarget() {
        return runTarget;
    }

    public void setRunTarget(String runTarget) {
        this.runTarget = runTarget;
    }

    public int getCronerNum() {
        return cronerNum;
    }

    public void setCronerNum(int cronerNum) {
        this.cronerNum = cronerNum;
    }

    public int getRunnerNum() {
        return runnerNum;
    }

    public void setRunnerNum(int runnerNum) {
        this.runnerNum = runnerNum;
    }

    public long getJvmMemMax() {
        return jvmMemMax;
    }

    public void setJvmMemMax(long jvmMemMax) {
        this.jvmMemMax = jvmMemMax;
    }

    public long getJvmMemTotal() {
        return jvmMemTotal;
    }

    public void setJvmMemTotal(long jvmMemTotal) {
        this.jvmMemTotal = jvmMemTotal;
    }

    public long getJvmMemFree() {
        return jvmMemFree;
    }

    public void setJvmMemFree(long jvmMemFree) {
        this.jvmMemFree = jvmMemFree;
    }

    public int getThreadActive() {
        return threadActive;
    }

    public void setThreadActive(int threadActive) {
        this.threadActive = threadActive;
    }

    public int getThreadPeak() {
        return threadPeak;
    }

    public void setThreadPeak(int threadPeak) {
        this.threadPeak = threadPeak;
    }

    public int getThreadDaemon() {
        return threadDaemon;
    }

    public void setThreadDaemon(int threadDaemon) {
        this.threadDaemon = threadDaemon;
    }

    public long getThreadStarted() {
        return threadStarted;
    }

    public void setThreadStarted(long threadStarted) {
        this.threadStarted = threadStarted;
    }

    public List<TaskCronerStats> getTaskCronerStatsList() {
        return taskCronerStatsList;
    }

    public void setTaskCronerStatsList(List<TaskCronerStats> taskCronerStatsList) {
        this.taskCronerStatsList = taskCronerStatsList;
    }

    public List<TaskRunnerStats> getTaskRunnerStatsList() {
        return taskRunnerStatsList;
    }

    public void setTaskRunnerStatsList(List<TaskRunnerStats> taskRunnerStatsList) {
        this.taskRunnerStatsList = taskRunnerStatsList;
    }
}
