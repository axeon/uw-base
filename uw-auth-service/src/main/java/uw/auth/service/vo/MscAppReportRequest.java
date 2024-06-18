package uw.auth.service.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * MscApp报告。
 * MSC应用主机
 *
 * @author axeon
 */
@Schema(title = "MSC应用报告", description = "MSC应用报告")
public class MscAppReportRequest implements Serializable {

    /**
     * id
     */
    @Schema(title = "id", description = "id")
    private long id;

    /**
     * appId
     */
    @Schema(title = "appId", description = "appId")
    private long appId;

    /**
     * 应用名称
     */
    @Schema(title = "应用名称", description = "应用名称")
    private String appName;

    /**
     * 应用版本
     */
    @Schema(title = "应用版本", description = "应用版本")
    private String appVersion;

    /**
     * app主机
     */
    @Schema(title = "app主机", description = "app主机")
    private String appHost;

    /**
     * app端口
     */
    @Schema(title = "app端口", description = "app端口")
    private int appPort;

    /**
     * 在线rpc户数
     */
    @Schema(title = "在线rpc户数", description = "在线rpc户数")
    private int userRpcNum;

    /**
     * 在线root数
     */
    @Schema(title = "在线root数", description = "在线root数")
    private int userRootNum;

    /**
     * 在线ops数
     */
    @Schema(title = "在线ops数", description = "在线ops数")
    private int userOpsNum;

    /**
     * 在线admin数
     */
    @Schema(title = "在线admin数", description = "在线admin数")
    private int userAdminNum;

    /**
     * 在线saas用户
     */
    @Schema(title = "在线saas用户", description = "在线saas用户")
    private int userSaasNum;

    /**
     * 在线会员数
     */
    @Schema(title = "在线会员数", description = "在线会员数")
    private int userGuestNum;

    /**
     * 访问计数
     */
    @Schema(title = "访问计数", description = "访问计数")
    private long accessCount;

    /**
     * jvm内存总数
     */
    @Schema(title = "jvm内存总数", description = "jvm内存总数")
    private long jvmMemMax;

    /**
     * jvm内存总数
     */
    @Schema(title = "jvm内存总数", description = "jvm内存总数")
    private long jvmMemTotal;

    /**
     * jvm空闲内存
     */
    @Schema(title = "jvm空闲内存", description = "jvm空闲内存")
    private long jvmMemFree;

    /**
     * 活跃线程
     */
    @Schema(title = "活跃线程", description = "活跃线程")
    private int threadActive;

    /**
     * 峰值线程
     */
    @Schema(title = "峰值线程", description = "峰值线程")
    private int threadPeak;

    /**
     * 守护线程
     */
    @Schema(title = "守护线程", description = "守护线程")
    private int threadDaemon;

    /**
     * 累计启动线程
     */
    @Schema(title = "累计启动线程", description = "累计启动线程")
    private long threadStarted;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getAppId() {
        return appId;
    }

    public void setAppId(long appId) {
        this.appId = appId;
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

    public String getAppHost() {
        return appHost;
    }

    public void setAppHost(String appHost) {
        this.appHost = appHost;
    }

    public int getAppPort() {
        return appPort;
    }

    public void setAppPort(int appPort) {
        this.appPort = appPort;
    }

    public int getUserRpcNum() {
        return userRpcNum;
    }

    public void setUserRpcNum(int userRpcNum) {
        this.userRpcNum = userRpcNum;
    }

    public int getUserRootNum() {
        return userRootNum;
    }

    public void setUserRootNum(int userRootNum) {
        this.userRootNum = userRootNum;
    }

    public int getUserOpsNum() {
        return userOpsNum;
    }

    public void setUserOpsNum(int userOpsNum) {
        this.userOpsNum = userOpsNum;
    }

    public int getUserAdminNum() {
        return userAdminNum;
    }

    public void setUserAdminNum(int userAdminNum) {
        this.userAdminNum = userAdminNum;
    }

    public int getUserSaasNum() {
        return userSaasNum;
    }

    public void setUserSaasNum(int userSaasNum) {
        this.userSaasNum = userSaasNum;
    }

    public int getUserGuestNum() {
        return userGuestNum;
    }

    public void setUserGuestNum(int userGuestNum) {
        this.userGuestNum = userGuestNum;
    }

    public long getAccessCount() {
        return accessCount;
    }

    public void setAccessCount(long accessCount) {
        this.accessCount = accessCount;
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
}