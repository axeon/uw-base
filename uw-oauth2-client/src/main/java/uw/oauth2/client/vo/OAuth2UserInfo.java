package uw.oauth2.client.vo;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Map;

/**
 * OAuth2用户信息。
 */
@Schema(title = "OAuth2用户信息", description = "OAuth2用户信息")
public class OAuth2UserInfo implements Serializable {

    /**
     * 认证提供者。
     */
    @Schema(title = "三方代码", description = "三方代码", example = "google")
    private String providerCode;

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
     * 用户名。
     */
    @Schema(title = "用户名", description = "用户名", example = "testuser")
    private String username;

    /**
     * 邮箱。
     */
    @Schema(title = "邮箱", description = "邮箱", example = "test@example.com")
    private String email;

    /**
     * 手机号。
     */
    @Schema(title = "手机号", description = "手机号", example = "13800138000")
    private String phone;

    /**
     * 头像URL。
     */
    @Schema(title = "头像URL", description = "头像URL", example = "https://example.com/avatar.jpg")
    // 基本映射，实际解析时会处理多个可能的字段（picture、avatar_url、headimgurl等）
    private String avatar;

    /**
     * 性别。
     */
    @Schema(title = "性别", description = "性别", example = "male")
    private String gender;

    /**
     * 地区。
     */
    @Schema(title = "地区", description = "地区", example = "中国")
    private String area;

    /**
     * 地址。
     */
    @Schema(title = "地址", description = "地址", example = "中国")
    private String address;

    /**
     * 原始用户信息。
     */
    @Schema(title = "原始用户信息", description = "原始用户信息")
    @JsonAnySetter
    private Map<String, Object> rawParams;

    public OAuth2UserInfo() {
    }

    private OAuth2UserInfo(Builder builder) {
        setProviderCode(builder.providerCode);
        setOpenId(builder.openId);
        setUnionId(builder.unionId);
        setUsername(builder.username);
        setEmail(builder.email);
        setPhone(builder.phone);
        setAvatar(builder.avatar);
        setGender(builder.gender);
        setArea(builder.area);
        setAddress(builder.address);
        setRawParams(builder.rawParams);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(OAuth2UserInfo copy) {
        Builder builder = new Builder();
        builder.providerCode = copy.getProviderCode();
        builder.openId = copy.getOpenId();
        builder.unionId = copy.getUnionId();
        builder.username = copy.getUsername();
        builder.email = copy.getEmail();
        builder.phone = copy.getPhone();
        builder.avatar = copy.getAvatar();
        builder.gender = copy.getGender();
        builder.area = copy.getArea();
        builder.address = copy.getAddress();
        builder.rawParams = copy.getRawParams();
        return builder;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
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

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Map<String, Object> getRawParams() {
        return rawParams;
    }

    public void setRawParams(Map<String, Object> rawParams) {
        this.rawParams = rawParams;
    }

    public static final class Builder {
        private String providerCode;
        private String openId;
        private String unionId;
        private String username;
        private String email;
        private String phone;
        private String avatar;
        private String gender;
        private String area;
        private String address;
        private Map<String, Object> rawParams;

        private Builder() {
        }

        public Builder providerCode(String providerCode) {
            this.providerCode = providerCode;
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

        public Builder gender(String gender) {
            this.gender = gender;
            return this;
        }

        public Builder area(String area) {
            this.area = area;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder rawParams(Map<String, Object> rawParams) {
            this.rawParams = rawParams;
            return this;
        }

        public OAuth2UserInfo build() {
            return new OAuth2UserInfo(this);
        }
    }
}