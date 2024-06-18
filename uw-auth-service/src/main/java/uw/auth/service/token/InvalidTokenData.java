package uw.auth.service.token;

import java.util.StringJoiner;

/**
 * 非法tokenData。
 *
 * @author axeon
 */
public class InvalidTokenData {

    /**
     * 非法类型。
     * 0 logout 退出登录。
     * 1 double login 双登录。
     */
    private int invalidType;

    /**
     * 非法时间。
     */
    private long invalidDate;

    /**
     * 运营商Id
     */
    private long saasId;

    /**
     * 用户登录类型
     */
    private int userType;

    /**
     * 用户Id
     */
    private long userId;

    /**
     * 商户Id
     */
    private long mchId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * token信息。
     */
    private String token;

    /**
     * 通知信息。
     */
    private String notice;

    private InvalidTokenData(Builder builder) {
        setInvalidType(builder.invalidType);
        setInvalidDate(builder.invalidDate);
        setSaasId(builder.saasId);
        setUserType(builder.userType);
        setUserId(builder.userId);
        setMchId(builder.mchId);
        setUserName(builder.userName);
        setToken(builder.token);
        setNotice(builder.notice);
    }

    public InvalidTokenData() {

    }

    public InvalidTokenData(AuthTokenData authToken, int invalidType, String notice) {
        this.invalidType = invalidType;
        this.invalidDate = System.currentTimeMillis();
        this.saasId = authToken.getSaasId();
        this.userType = authToken.getUserType();
        this.userId = authToken.getUserId();
        this.mchId = authToken.getMchId();
        this.userName = authToken.getUserName();
        this.notice = notice;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(InvalidTokenData copy) {
        Builder builder = new Builder();
        builder.invalidType = copy.getInvalidType();
        builder.invalidDate = copy.getInvalidDate();
        builder.saasId = copy.getSaasId();
        builder.userType = copy.getUserType();
        builder.userId = copy.getUserId();
        builder.mchId = copy.getMchId();
        builder.userName = copy.getUserName();
        builder.token = copy.getToken();
        builder.notice = copy.getNotice();
        return builder;
    }

    @Override
    public String toString() {
        return new StringJoiner( ", ", InvalidTokenData.class.getSimpleName() + "[", "]" )
                .add( "invalidType=" + invalidType )
                .add( "invalidDate=" + invalidDate )
                .add( "saasId=" + saasId )
                .add( "userType=" + userType )
                .add( "userId=" + userId )
                .add( "mchId=" + mchId )
                .add( "userName='" + userName + "'" )
                .add( "token='" + token + "'" )
                .add( "notice='" + notice + "'" )
                .toString();
    }

    public int getInvalidType() {
        return invalidType;
    }

    public void setInvalidType(int invalidType) {
        this.invalidType = invalidType;
    }

    public long getInvalidDate() {
        return invalidDate;
    }

    public void setInvalidDate(long invalidDate) {
        this.invalidDate = invalidDate;
    }

    public long getSaasId() {
        return saasId;
    }

    public void setSaasId(long saasId) {
        this.saasId = saasId;
    }

    public int getUserType() {
        return userType;
    }

    public void setUserType(int userType) {
        this.userType = userType;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }

    public static final class Builder {
        private int invalidType;
        private long invalidDate;
        private long saasId;
        private int userType;
        private long userId;
        private long mchId;
        private String userName;
        private String token;
        private String notice;

        private Builder() {
        }

        public Builder invalidType(int invalidType) {
            this.invalidType = invalidType;
            return this;
        }

        public Builder invalidDate(long invalidDate) {
            this.invalidDate = invalidDate;
            return this;
        }

        public Builder saasId(long saasId) {
            this.saasId = saasId;
            return this;
        }

        public Builder userType(int userType) {
            this.userType = userType;
            return this;
        }

        public Builder userId(long userId) {
            this.userId = userId;
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

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder notice(String notice) {
            this.notice = notice;
            return this;
        }

        public InvalidTokenData build() {
            return new InvalidTokenData(this);
        }
    }
}
