package uw.mfa.constant;

/**
 * Hmac算法枚举。
 */
public enum HmacAlgorithm {

    SHA1( "HmacSHA1", "SHA1" ),
    SHA256( "HmacSHA256", "SHA256" ),
    SHA512( "HmacSHA512", "SHA512" );

    private final String value;
    private final String label;

    HmacAlgorithm(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }
}
