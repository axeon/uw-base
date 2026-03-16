package uw.oauth2.client.vo;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Map;

/**
 * OAuth2访问令牌信息。
 */
@Schema(title = "OAuth2访问令牌信息", description = "OAuth2访问令牌信息")
public class OAuth2Token implements Serializable {

    /**
     * 访问令牌
     */
    @Schema(title = "访问令牌", description = "访问令牌")
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * 刷新令牌
     */
    @Schema(title = "刷新令牌", description = "刷新令牌")
    @JsonProperty("refresh_token")
    private String refreshToken;

    /**
     * 令牌类型
     */
    @Schema(title = "令牌类型", description = "令牌类型", example = "Bearer")
    @JsonProperty("token_type")
    private String tokenType;

    /**
     * 令牌过期时间（秒）
     */
    @Schema(title = "令牌过期时间", description = "令牌过期时间（秒）", example = "3600")
    @JsonProperty("expires_in")
    private long expiresIn;

    /**
     * 令牌作用域
     */
    @Schema(title = "令牌作用域", description = "令牌作用域", example = "openid email profile")
    @JsonProperty("scope")
    private String scope;

    /**
     * ID令牌（OpenID Connect）
     */
    @Schema(title = "ID令牌", description = "ID令牌（OpenID Connect）")
    @JsonProperty("id_token")
    private String idToken;

    /**
     * 错误代码
     */
    @Schema(title = "错误代码", description = "错误代码")
    @JsonProperty("error")
    private String error;

    /**
     * 错误描述
     */
    @Schema(title = "错误描述", description = "错误描述")
    @JsonProperty("error_description")
    private String errorDescription;

    /**
     * 错误URI
     */
    @Schema(title = "错误URI", description = "错误URI")
    @JsonProperty("error_uri")
    private String errorUri;

    /**
     * 令牌创建时间
     */
    @Schema(title = "令牌创建时间", description = "令牌创建时间")
    @JsonProperty("create_time")
    private long createTime;

    /**
     * 刷新令牌过期时间（秒）
     */
    @Schema(title = "刷新令牌过期时间", description = "刷新令牌过期时间（秒）")
    @JsonProperty("refresh_token_expires_in")
    private long refreshTokenExpiresIn;

    /**
     * 令牌颁发时间戳
     */
    @Schema(title = "令牌颁发时间戳", description = "令牌颁发时间戳")
    @JsonProperty("issued_at")
    private long issuedAt;

    /**
     * 三方用户ID。
     */
    @Schema(title = "三方用户ID", description = "三方用户ID", example = "1234567890")
    private String openId;

    /**
     * 三方统一ID。
     */
    @Schema(title = "三方统一ID", description = "三方统一ID", example = "1234567890")
    private String unionId;

    /**
     * 用户名
     */
    @Schema(title = "用户名", description = "用户名")
    private String username;

    /**
     * 邮箱
     */
    @Schema(title = "邮箱", description = "邮箱")
    private String email;

    /**
     * 手机号
     */
    @Schema(title = "手机号", description = "手机号")
    private String phone;

    /**
     * 头像
     */
    @Schema(title = "头像", description = "头像")
    private String avatar;

    /**
     * 原始信息
     */
    @Schema(title = "原始信息", description = "原始信息")
    @JsonAnySetter
    private Map<String, Object> rawParams;

    public OAuth2Token() {
    }

    private OAuth2Token(Builder builder) {
        setAccessToken(builder.accessToken);
        setRefreshToken(builder.refreshToken);
        setTokenType(builder.tokenType);
        setExpiresIn(builder.expiresIn);
        setScope(builder.scope);
        setIdToken(builder.idToken);
        setError(builder.error);
        setErrorDescription(builder.errorDescription);
        setErrorUri(builder.errorUri);
        setCreateTime(builder.createTime);
        setRefreshTokenExpiresIn(builder.refreshTokenExpiresIn);
        setIssuedAt(builder.issuedAt);
        setOpenId(builder.openId);
        setUnionId(builder.unionId);
        setUsername(builder.username);
        setEmail(builder.email);
        setPhone(builder.phone);
        setAvatar(builder.avatar);
        setRawParams(builder.rawParams);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(OAuth2Token copy) {
        Builder builder = new Builder();
        builder.accessToken = copy.getAccessToken();
        builder.refreshToken = copy.getRefreshToken();
        builder.tokenType = copy.getTokenType();
        builder.expiresIn = copy.getExpiresIn();
        builder.scope = copy.getScope();
        builder.idToken = copy.getIdToken();
        builder.error = copy.getError();
        builder.errorDescription = copy.getErrorDescription();
        builder.errorUri = copy.getErrorUri();
        builder.createTime = copy.getCreateTime();
        builder.refreshTokenExpiresIn = copy.getRefreshTokenExpiresIn();
        builder.issuedAt = copy.getIssuedAt();
        builder.openId = copy.getOpenId();
        builder.unionId = copy.getUnionId();
        builder.username = copy.getUsername();
        builder.email = copy.getEmail();
        builder.phone = copy.getPhone();
        builder.avatar = copy.getAvatar();
        builder.rawParams = copy.getRawParams();
        return builder;
    }

    /**
     * 转换为用户信息
     *
     * @return
     */
    public OAuth2UserInfo toUserInfo() {
        return OAuth2UserInfo.builder()
                .openId(this.getOpenId())
                .unionId(this.getUnionId())
                .username(this.getUsername())
                .email(this.getEmail())
                .phone(this.getPhone())
                .avatar(this.getAvatar())
                .build();
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public String getErrorUri() {
        return errorUri;
    }

    public void setErrorUri(String errorUri) {
        this.errorUri = errorUri;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getRefreshTokenExpiresIn() {
        return refreshTokenExpiresIn;
    }

    public void setRefreshTokenExpiresIn(long refreshTokenExpiresIn) {
        this.refreshTokenExpiresIn = refreshTokenExpiresIn;
    }

    public long getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(long issuedAt) {
        this.issuedAt = issuedAt;
    }

    public String getOpenId() {
        return openId;
    }

    public void setOpenId(String openId) {
        this.openId = openId;
    }

    public String getUnionId() {
        return unionId;
    }

    public void setUnionId(String unionId) {
        this.unionId = unionId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Map<String, Object> getRawParams() {
        return rawParams;
    }

    public void setRawParams(Map<String, Object> rawParams) {
        this.rawParams = rawParams;
    }

    public static final class Builder {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private long expiresIn;
        private String scope;
        private String idToken;
        private String error;
        private String errorDescription;
        private String errorUri;
        private long createTime;
        private long refreshTokenExpiresIn;
        private long issuedAt;
        private String openId;
        private String unionId;
        private String username;
        private String email;
        private String phone;
        private String avatar;
        private Map<String, Object> rawParams;

        private Builder() {
        }

        public Builder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public Builder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public Builder tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        public Builder expiresIn(long expiresIn) {
            this.expiresIn = expiresIn;
            return this;
        }

        public Builder scope(String scope) {
            this.scope = scope;
            return this;
        }

        public Builder idToken(String idToken) {
            this.idToken = idToken;
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public Builder errorDescription(String errorDescription) {
            this.errorDescription = errorDescription;
            return this;
        }

        public Builder errorUri(String errorUri) {
            this.errorUri = errorUri;
            return this;
        }

        public Builder createTime(long createTime) {
            this.createTime = createTime;
            return this;
        }

        public Builder refreshTokenExpiresIn(long refreshTokenExpiresIn) {
            this.refreshTokenExpiresIn = refreshTokenExpiresIn;
            return this;
        }

        public Builder issuedAt(long issuedAt) {
            this.issuedAt = issuedAt;
            return this;
        }

        public Builder openId(String openId) {
            this.openId = openId;
            return this;
        }

        public Builder unionId(String unionId) {
            this.unionId = unionId;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder avatar(String avatar) {
            this.avatar = avatar;
            return this;
        }

        public Builder rawParams(Map<String, Object> rawParams) {
            this.rawParams = rawParams;
            return this;
        }

        public OAuth2Token build() {
            return new OAuth2Token(this);
        }
    }
}