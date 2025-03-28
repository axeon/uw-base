package uw.mfa.totp.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Totp密钥数据。
 */
@Schema(title = "Totp密钥数据", description = "Totp密钥数据")
public class TotpSecretData {

    /**
     * 密钥
     */
    @Schema(title = "密钥", description = "密钥")
    private String secret;

    /**
     * 二维码链接
     */
    @Schema(title = "二维码链接", description = "二维码链接")
    private String uri;

    /**
     * 二维码图片base64
     */
    @Schema(title = "二维码图片base64", description = "二维码图片base64")
    private String qr;

    public TotpSecretData() {
    }

    public TotpSecretData(String secret, String uri, String qr) {
        this.secret = secret;
        this.uri = uri;
        this.qr = qr;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getQr() {
        return qr;
    }

    public void setQr(String qr) {
        this.qr = qr;
    }
}
