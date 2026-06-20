package uw.mfa.constant;

/**
 * TOTP使用的HMAC算法枚举。
 * <p>对应RFC 6238 TOTP规范中支持的哈希算法，默认使用SHA1以兼容Google Authenticator等主流App。</p>
 */
public enum HmacAlgorithm {

    /**
     * HmacSHA1算法，TOTP默认算法，兼容性最佳（Google Authenticator/Microsoft Authenticator等）。
     */
    SHA1("HmacSHA1", "SHA1"),

    /**
     * HmacSHA256算法，安全性更高，部分Authenticator App支持。
     */
    SHA256("HmacSHA256", "SHA256"),

    /**
     * HmacSHA512算法，安全性最高，兼容性较差。
     */
    SHA512("HmacSHA512", "SHA512");

    /**
     * JCE标准算法名，用于 {@link javax.crypto.Mac#getInstance(String)}。
     */
    private final String value;

    /**
     * otpauth URI中的algorithm标签值（如SHA1/SHA256/SHA512）。
     */
    private final String label;

    HmacAlgorithm(String value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 获取JCE标准算法名。
     *
     * @return 算法名，如HmacSHA1
     */
    public String getValue() {
        return value;
    }

    /**
     * 获取otpauth URI中的algorithm标签值。
     *
     * @return 标签值，如SHA1
     */
    public String getLabel() {
        return label;
    }
}
