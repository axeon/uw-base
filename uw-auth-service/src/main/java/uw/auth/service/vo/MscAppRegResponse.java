package uw.auth.service.vo;

import java.util.Map;

/**
 * App注册响应
 */
public class MscAppRegResponse {

    /**
     * 初始状态，或者待更新状态。
     */
    public static final int STATE_INIT = 0;

    /**
     * 已注册成功状态。
     */
    public static final int STATE_SUCCESS = 1;

    /**
     * 新注册状态。
     */
    public static final int STATE_REGISTRY = 2;

    /**
     * 失败状态。
     */
    public static final int STATE_FAIL = -1;

    /**
     * 成功标识: 0:不存在/待更新; 1:已注册; 2:新注册; -1:失败
     */
    private int state;

    /**
     * 错误消息
     */
    private String msg;

    /**
     * 注册成功的AppId
     */
    private long appId;

    /**
     * 应用权限注册数据
     */
    private Map<String, Integer> appPerm;

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public long getAppId() {
        return appId;
    }

    public void setAppId(long appId) {
        this.appId = appId;
    }

    public Map<String, Integer> getAppPerm() {
        return appPerm;
    }

    public void setAppPerm(Map<String, Integer> appPerm) {
        this.appPerm = appPerm;
    }
}
