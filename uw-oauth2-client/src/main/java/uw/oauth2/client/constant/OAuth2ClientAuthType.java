package uw.oauth2.client.constant;

/**
 * OAuth2授权类型枚举，区分授权场景，会编入stateId的第二段。
 * <p>
 * 实际存入stateId时使用枚举名的小写形式（auth / qrcode）。
 *
 * @author axeon
 */
public enum OAuth2ClientAuthType {
    /**
     * 标准验证方式。
     */
    AUTH,

    /**
     * 扫码验证方式。
     */
    QRCODE;
}