package uw.oauth2.client.constant;

/**
 * 验证状态。
 */
public enum OAuth2ClientAuthStatus {
    /**
     * 等待
     */
    WAITING,
    /**
     * 已扫码，等待确认
     */
    SCANNED,
    /**
     * 登录已确认
     */
    CONFIRMED,
    /**
     * 已过期
     */
    EXPIRED,
    /**
     * 登录失败
     */
    FAILED;

}