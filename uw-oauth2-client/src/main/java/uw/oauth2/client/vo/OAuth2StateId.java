package uw.oauth2.client.vo;

/**
 * OAuth2状态ID，作为OAuth2授权流程中的state参数。
 * <p>
 * 由三段组成，使用 {@code ":"} 分隔（避免与providerCode中可能出现的下划线冲突），
 * 格式为：{@code providerCode:authType:seqId}，例如 {@code google:auth:1234567890}。
 * <p>
 * 用途：
 * <ul>
 *     <li>防止CSRF攻击——授权回调时比对state是否合法；</li>
 *     <li>承载Provider信息——回调时可不传providerCode，从state解析；</li>
 *     <li>区分授权场景——authType为auth（网页授权）或qrcode（扫码授权）。</li>
 * </ul>
 *
 * @author axeon
 */
public class OAuth2StateId {

    /**
     * 分隔符。使用":"避免与providerCode中可能出现的下划线冲突。
     */
    private static final String SEPARATOR = ":";

    /**
     * Provider名称（如google、wechat、apple等）。
     */
    private String providerCode;

    /**
     * 认证类型，取值为auth（网页授权）或qrcode（扫码授权）。
     */
    private String authType;

    /**
     * 顺序ID，由Snowflake生成，保证全局唯一。
     */
    private String seqId;

    /**
     * 全参构造函数。
     *
     * @param providerCode Provider名称
     * @param authType     认证类型（auth/qrcode）
     * @param seqId        顺序ID
     */
    public OAuth2StateId(String providerCode, String authType, String seqId) {
        this.providerCode = providerCode;
        this.authType = authType;
        this.seqId = seqId;
    }

    /**
     * 无参构造函数，解析失败时返回空对象。
     */
    public OAuth2StateId() {
    }

    /**
     * 解析状态ID字符串为OAuth2StateId对象。
     * <p>
     * 格式必须为 {@code providerCode:authType:seqId}，三段缺一不可；
     * 解析失败（null、段数不为3）时返回字段全为null的空对象。
     *
     * @param authStateId 状态ID字符串
     * @return 状态ID对象；入参非法时返回空对象（各字段为null）
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

    /**
     * 序列化为状态ID字符串。
     *
     * @return 格式为 providerCode:authType:seqId 的字符串
     */
    public String toString() {
        return providerCode + SEPARATOR + authType + SEPARATOR + seqId;
    }

    /**
     * 获取Provider名称。
     *
     * @return Provider名称
     */
    public String getProviderCode() {
        return providerCode;
    }

    /**
     * 设置Provider名称。
     *
     * @param providerCode Provider名称
     */
    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    /**
     * 获取认证类型。
     *
     * @return 认证类型（auth/qrcode）
     */
    public String getAuthType() {
        return authType;
    }

    /**
     * 设置认证类型。
     *
     * @param authType 认证类型（auth/qrcode）
     */
    public void setAuthType(String authType) {
        this.authType = authType;
    }

    /**
     * 获取顺序ID。
     *
     * @return 顺序ID
     */
    public String getSeqId() {
        return seqId;
    }

    /**
     * 设置顺序ID。
     *
     * @param seqId 顺序ID
     */
    public void setSeqId(String seqId) {
        this.seqId = seqId;
    }
}
