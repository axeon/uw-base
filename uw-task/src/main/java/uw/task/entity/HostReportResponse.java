package uw.task.entity;

import java.io.Serializable;

/**
 * 主机状态报告的响应。
 *
 * <p>任务执行主机周期性上报状态后（见 {@code TaskServiceRegistrar.reportHostInfo}），
 * 由 task-center 返回此对象，回填主机记录 id 及当前状态，供下一次上报携带。</p>
 *
 * @author axeon
 */
public class HostReportResponse implements Serializable {

    /**
     * 主机记录 id（首次上报时由服务端分配，后续上报回传以定位记录）。
     */
    private long id;

    /**
     * 主机 IP。
     */
    private String hostIp;

    /**
     * 主机状态（由服务端设定）。
     */
    private int state;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}