package uw.auth.service.token;

import uw.common.util.SystemClock;

/**
 * 用户刷新令牌数据。
 *
 * @author axeon
 */
public class RefreshTokenData {

    /**
     * Token过期时间
     */
    private long expireAt;

    /**
     * Token创建时间
     */
    private long createAt;

    /**
     * 用户Id
     */
    private long userId;

    /**
     * 用户登录类型
     */
    private int userType;

    /**
     * 运营商Id
     */
    private long saasId;

    /**
     * 商户Id
     */
    private long mchId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 客户端密文。
     */
    private byte[] clientSecret;


    public RefreshTokenData() {

    }


    private RefreshTokenData(Builder builder) {
        setExpireAt(builder.expireAt);
        setCreateAt(builder.createAt);
        setUserId(builder.userId);
        setUserType(builder.userType);
        setSaasId(builder.saasId);
        setMchId(builder.mchId);
        setUserName(builder.userName);
        setClientSecret(builder.clientSecret);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(RefreshTokenData copy) {
        Builder builder = new Builder();
        builder.expireAt = copy.getExpireAt();
        builder.createAt = copy.getCreateAt();
        builder.userId = copy.getUserId();
        builder.userType = copy.getUserType();
        builder.saasId = copy.getSaasId();
        builder.mchId = copy.getMchId();
        builder.userName = copy.getUserName();
        builder.clientSecret = copy.getClientSecret();
        return builder;
    }
    public boolean isExpired() {
        return SystemClock.now() >= expireAt;
    }
    public long getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(long expireAt) {
        this.expireAt = expireAt;
    }

    public long getCreateAt() {
        return createAt;
    }

    public void setCreateAt(long createAt) {
        this.createAt = createAt;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public int getUserType() {
        return userType;
    }

    public void setUserType(int userType) {
        this.userType = userType;
    }

    public long getSaasId() {
        return saasId;
    }

    public void setSaasId(long saasId) {
        this.saasId = saasId;
    }

    public long getMchId() {
        return mchId;
    }

    public void setMchId(long mchId) {
        this.mchId = mchId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public byte[] getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(byte[] clientSecret) {
        this.clientSecret = clientSecret;
    }

    public static final class Builder {
        private long expireAt;
        private long createAt;
        private long userId;
        private int userType;
        private long saasId;
        private long mchId;
        private String userName;
        private byte[] clientSecret;

        private Builder() {
        }

        public Builder expireAt(long expireAt) {
            this.expireAt = expireAt;
            return this;
        }

        public Builder createAt(long createAt) {
            this.createAt = createAt;
            return this;
        }

        public Builder userId(long userId) {
            this.userId = userId;
            return this;
        }

        public Builder userType(int userType) {
            this.userType = userType;
            return this;
        }

        public Builder saasId(long saasId) {
            this.saasId = saasId;
            return this;
        }

        public Builder mchId(long mchId) {
            this.mchId = mchId;
            return this;
        }

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder clientSecret(byte[] clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }

        public RefreshTokenData build() {
            return new RefreshTokenData(this);
        }
    }
}
