package uw.task.entity;

import java.io.Serializable;

/**
 * 主机报告Response。
 *
 * @author axeon
 */
public class HostReportResponse implements Serializable {

    /**
     * id
     */
    private long id;

    /**
     * hostIp
     */
    private String hostIp;

    /**
     * 状态
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