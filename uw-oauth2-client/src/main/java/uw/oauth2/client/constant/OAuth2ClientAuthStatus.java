package uw.oauth2.client.constant;

/**
 * OAuth2授权状态枚举，描述stateId在授权流程中的生命周期阶段。
 * <p>
 * 典型流转：
 * <ul>
 *     <li>网页授权：SCANNED →（换Token成功）→ CONFIRMED；</li>
 *     <li>扫码授权：WAITING → SCANNED → CONFIRMED；</li>
 *     <li>任意阶段超时 → EXPIRED，失败 → FAILED。</li>
 * </ul>
 *
 * @author axeon
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