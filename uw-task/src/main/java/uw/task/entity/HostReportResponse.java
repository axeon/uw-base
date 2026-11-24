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
     * 默认构造器。
     */
    public HostReportResponse() {
    }

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

    /**
     * 获取主机记录 id（首次上报时由服务端分配，后续上报回传以定位记录）。
     *
     * @return 主机记录 id（首次上报时由服务端分配，后续上报回传以定位记录）
     */
    public long getId() {
        return id;
    }

    /**
     * 设置主机记录 id（首次上报时由服务端分配，后续上报回传以定位记录）。
     *
     * @param id 主机记录 id（首次上报时由服务端分配，后续上报回传以定位记录）
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * 获取主机 IP。
     *
     * @return 主机 IP
     */
    public String getHostIp() {
        return hostIp;
    }

    /**
     * 设置主机 IP。
     *
     * @param hostIp 主机 IP
     */
    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    /**
     * 获取主机状态（由服务端设定）。
     *
     * @return 主机状态（由服务端设定）
     */
    public int getState() {
        return state;
    }

    /**
     * 设置主机状态（由服务端设定）。
     *
     * @param state 主机状态（由服务端设定）
     */
    public void setState(int state) {
        this.state = state;
    }
}
