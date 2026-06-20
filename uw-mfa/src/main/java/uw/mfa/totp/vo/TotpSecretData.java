package uw.mfa.totp.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * TOTP密钥数据。
 * <p>包含Base32密钥、otpauth URI与可选的二维码图片Base64，用于TOTP绑定流程。</p>
 */
@Schema(title = "Totp密钥数据", description = "Totp密钥数据")
public class TotpSecretData {

    /**
     * Base32编码的TOTP密钥（需安全存储，用于后续验证码校验）。
     */
    @Schema(title = "密钥", description = "密钥")
    private String secret;

    /**
     * otpauth协议URI（otpauth://totp/...），可供前端自行生成二维码。
     */
    @Schema(title = "二维码链接", description = "二维码链接")
    private String uri;

    /**
     * 二维码图片Base64（PNG格式），未启用二维码生成时为null。
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
