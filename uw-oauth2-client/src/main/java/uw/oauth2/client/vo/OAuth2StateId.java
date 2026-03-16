package uw.oauth2.client.vo;

/**
 * OAuth2状态ID。
 *
 * @author axeon
 */
public class OAuth2StateId {

    /**
     * 分隔符
     */
    private static final String SEPARATOR = "_";

    /**
     * Provider名称
     */
    private String providerCode;

    /**
     * 认证类型
     */
    private String authType;

    /**
     * 顺序ID
     */
    private String seqId;

    public OAuth2StateId(String providerCode, String authType, String seqId) {
        this.providerCode = providerCode;
        this.authType = authType;
        this.seqId = seqId;
    }

    public OAuth2StateId() {
    }

    /**
     * 解析状态ID
     *
     * @param authStateId 状态ID
     * @return 状态ID对象
     */
    public static OAuth2StateId parse(String authStateId) {
        if (authStateId == null) {
            return new OAuth2StateId();
        }
        String[] params = authStateId.split(SEPARATOR);
        if (params.length != 3) {
            return new OAuth2StateId();
        }
        return new OAuth2StateId(params[0], params[1], params[2]);
    }

    public String toString() {
        return providerCode + SEPARATOR + authType + SEPARATOR + seqId;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getSeqId() {
        return seqId;
    }

    public void setSeqId(String seqId) {
        this.seqId = seqId;
    }
}
