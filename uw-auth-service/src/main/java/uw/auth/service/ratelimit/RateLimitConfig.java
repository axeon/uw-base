package uw.auth.service.ratelimit;

import uw.auth.service.constant.RateLimitTarget;

/**
 * 限速信息。
 * 考虑到限速控制信息并不多，所以直接使用List结构存储。
 *
 * @author axeon
 */
public class RateLimitConfig {

    /**
     * 限定目标值
     */
    private String url;

    /**
     * 限定目标
     */
    private RateLimitTarget target = RateLimitTarget.NONE;

    /**
     * 限定值
     */
    private int requests;

    /**
     * 限定秒数。
     */
    private int seconds;


    public RateLimitConfig() {
    }

    public RateLimitConfig(String url, RateLimitTarget target, int requests, int seconds) {
        this.url = url;
        this.target = target;
        this.requests = requests;
        this.seconds = seconds;
    }

    /**
     * 检查是否合法。
     *
     * @return
     */
    public boolean checkValid() {
        if (requests == 0 || seconds == 0 || target == RateLimitTarget.NONE || url == null || url.length() == 0) {
            return false;
        } else {
            return true;
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public RateLimitTarget getTarget() {
        return target;
    }

    public void setTarget(RateLimitTarget target) {
        this.target = target;
    }

    public int getRequests() {
        return requests;
    }

    public void setRequests(int requests) {
        this.requests = requests;
    }

    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

}
