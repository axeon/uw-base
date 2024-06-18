package uw.auth.service.ratelimit;

import uw.auth.service.constant.RateLimitTarget;

/**
 * 限速信息。
 * 考虑到限速控制信息并不多，所以直接使用List结构存储。
 *
 * @author axeon
 */
public class RateLimitInfo {

    /**
     * 限定目标值，已经合成uri。
     */
    private String uri;

    /**
     * 限定目标
     */
    private RateLimitTarget target = RateLimitTarget.NONE;

    /**
     * 限定值
     */
    private int requests;

    /**
     * 限定秒数
     */
    private int seconds;


    public RateLimitInfo() {
    }

    public RateLimitTarget getTarget() {
        return target;
    }

    public void setTarget(RateLimitTarget target) {
        this.target = target;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
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


    @Override
    public String toString() {
        return new StringBuilder(30).append(target).append(":").append(requests).append("/").append(seconds).toString();
    }
}
