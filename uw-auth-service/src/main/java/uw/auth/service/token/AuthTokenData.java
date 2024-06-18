package uw.auth.service.token;

import io.swagger.v3.oas.annotations.media.Schema;
import uw.auth.service.ipblock.IpRange;
import uw.auth.service.ratelimit.RateLimitConfig;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * 用户令牌。
 *
 * @author axeon
 */
public class AuthTokenData {

    /**
     * 运营商Id
     */
    @Schema(title = "saasId", description = "saasId")
    private long saasId;

    /**
     * 用户登录类型
     */
    @Schema(title = "用户类型", description = "用户类型")
    private int userType;

    /**
     * 用户Id
     */
    @Schema(title = "用户Id", description = "用户Id")
    private long userId;

    /**
     * 商户Id
     */
    @Schema(title = "商户编号", description = "商户编号")
    private long mchId;

    /**
     * 所属用户组ID
     */
    @Schema(title = "所属用户组ID", description = "所属用户组ID")
    private long groupId;


    /**
     * 是否是管理者。
     */
    @Schema(title = "是否是管理者", description = "是否是管理者")
    private int isMaster;

    /**
     * 登录名。
     */
    @Schema(title = "登录名", description = "登录名")
    private String userName;

    /**
     * 真实名称
     */
    @Schema(title = "真实名称", description = "真实名称")
    private String realName;

    /**
     * 别名 [用于业务前台匿名]
     */
    @Schema(title = "别名 [用于业务前台匿名]", description = "别名 [用于业务前台匿名]")
    private String nickName;

    /**
     * 手机号码
     */
    @Schema(title = "手机号码", description = "手机号码")
    private String mobile;

    /**
     * email
     */
    @Schema(title = "email", description = "email")
    private String email;

    /**
     * 微信ID
     */
    @Schema(title = "微信ID", description = "微信ID")
    private String wxId;

    /**
     * 用户级别
     */
    @Schema(title = "用户级别", description = "用户级别")
    private int userGrade;

    /**
     * 权限描述
     */
    private TokenPerm tokenPerm;

    /**
     * Token过期时间
     */
    private long expireAt;

    /**
     * Token创建时间
     */
    private long createAt;

    public AuthTokenData() {

    }

    public AuthTokenData(long saasId, int userType, long userId, long mchId, long groupId, int isMaster, String userName, String nickName, String realName, String mobile,
                         String email, String wxId, int userGrade) {
        this.saasId = saasId;
        this.userType = userType;
        this.userId = userId;
        this.mchId = mchId;
        this.groupId = groupId;
        this.isMaster = isMaster;
        this.userName = userName;
        this.realName = realName;
        this.nickName = nickName;
        this.mobile = mobile;
        this.email = email;
        this.wxId = wxId;
        this.userGrade = userGrade;
    }

    private AuthTokenData(Builder builder) {
        setSaasId( builder.saasId );
        setUserType( builder.userType );
        setUserId( builder.userId );
        setMchId( builder.mchId );
        setGroupId( builder.groupId );
        setIsMaster( builder.isMaster );
        setUserName( builder.userName );
        setRealName( builder.realName );
        setNickName( builder.nickName );
        setMobile( builder.mobile );
        setEmail( builder.email );
        setWxId( builder.wxId );
        setUserGrade( builder.userGrade );
        setTokenPerm( builder.tokenPerm );
        setExpireAt( builder.expireAt );
        setCreateAt( builder.createAt );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(AuthTokenData copy) {
        Builder builder = new Builder();
        builder.saasId = copy.getSaasId();
        builder.userType = copy.getUserType();
        builder.userId = copy.getUserId();
        builder.mchId = copy.getMchId();
        builder.groupId = copy.getGroupId();
        builder.isMaster = copy.getIsMaster();
        builder.userName = copy.getUserName();
        builder.realName = copy.getRealName();
        builder.nickName = copy.getNickName();
        builder.mobile = copy.getMobile();
        builder.email = copy.getEmail();
        builder.wxId = copy.getWxId();
        builder.userGrade = copy.getUserGrade();
        builder.tokenPerm = copy.getTokenPerm();
        builder.expireAt = copy.getExpireAt();
        builder.createAt = copy.getCreateAt();
        return builder;
    }

    @Override
    public String toString() {
        return new StringJoiner( ", ", AuthTokenData.class.getSimpleName() + "[", "]" )
                .add( "saasId=" + saasId )
                .add( "userType=" + userType )
                .add( "userId=" + userId )
                .add( "mchId=" + mchId )
                .add( "groupId=" + groupId )
                .add( "isMaster=" + isMaster )
                .add( "userName='" + userName + "'" )
                .add( "realName='" + realName + "'" )
                .add( "nickName='" + nickName + "'" )
                .add( "mobile='" + mobile + "'" )
                .add( "email='" + email + "'" )
                .add( "wxId='" + wxId + "'" )
                .add( "userGrade=" + userGrade )
                .add( "tokenPerm=" + tokenPerm )
                .add( "expireAt=" + expireAt )
                .add( "createAt=" + createAt )
                .toString();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expireAt;
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

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public int getIsMaster() {
        return isMaster;
    }

    public void setIsMaster(int isMaster) {
        this.isMaster = isMaster;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWxId() {
        return wxId;
    }

    public void setWxId(String wxId) {
        this.wxId = wxId;
    }

    public int getUserGrade() {
        return userGrade;
    }

    public void setUserGrade(int userGrade) {
        this.userGrade = userGrade;
    }

    public TokenPerm getTokenPerm() {
        return tokenPerm;
    }

    public void setTokenPerm(TokenPerm tokenPerm) {
        this.tokenPerm = tokenPerm;
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

    /**
     * 用户权限信息[存在于Redis中]
     *
     * @author axeon
     * @since 2018-08-21
     */
    public static class TokenPerm {

        private static final long serialVersionUID = -7105425886373032625L;

        /**
         * ip过滤类型。
         */
        private int ipFilterType;

        /**
         * ip封装范围。
         */
        private List<IpRange> ipRanges;

        /**
         * 限速信息。
         */
        private List<RateLimitConfig> rateLimits;

        /**
         * 权限描述
         */
        private Set<Integer> userPerms;

        /**
         * 用户配置。
         */
        private Map<String, String> userConfigMap;

        public TokenPerm() {
        }

        public int getIpFilterType() {
            return ipFilterType;
        }

        public void setIpFilterType(int ipFilterType) {
            this.ipFilterType = ipFilterType;
        }

        public List<IpRange> getIpRanges() {
            return ipRanges;
        }

        public void setIpRanges(List<IpRange> ipRanges) {
            this.ipRanges = ipRanges;
        }

        public List<RateLimitConfig> getRateLimits() {
            return rateLimits;
        }

        public void setRateLimits(List<RateLimitConfig> rateLimits) {
            this.rateLimits = rateLimits;
        }

        public Set<Integer> getUserPerms() {
            return userPerms;
        }

        public void setUserPerms(Set<Integer> userPerms) {
            this.userPerms = userPerms;
        }

        public Map<String, String> getUserConfigMap() {
            return userConfigMap;
        }

        public void setUserConfigMap(Map<String, String> userConfigMap) {
            this.userConfigMap = userConfigMap;
        }

        /**
         * 获得配置参数。
         *
         * @param paramName
         * @return
         */
        public String getUserConfigValue(String paramName) {
            if (this.userConfigMap == null) {
                return "";
            }
            return this.userConfigMap.getOrDefault( paramName, "" );
        }

        /**
         * 获得配置参数。
         *
         * @param paramName
         * @return
         */
        public boolean getUserConfigBooleanValue(String paramName) {
            if (this.userConfigMap == null) {
                return false;
            }
            String value = this.userConfigMap.getOrDefault( paramName, "0" );
            return value.equals( "1" ) || value.equalsIgnoreCase( "true" );
        }

        /**
         * 获得配置参数。
         *
         * @param paramName
         * @return
         */
        public int getUserConfigIntValue(String paramName) {
            int value = 0;
            if (this.userConfigMap == null) {
                return value;
            }
            try {
                value = Integer.parseInt( this.userConfigMap.getOrDefault( paramName, "0" ) );
            } catch (Exception e) {
            }
            return value;
        }

        /**
         * 获得配置参数。
         *
         * @param paramName
         * @return
         */
        public long getUserConfigLongValue(String paramName) {
            long value = 0;
            if (this.userConfigMap == null) {
                return value;
            }
            try {
                value = Long.parseLong( this.userConfigMap.getOrDefault( paramName, "0" ) );
            } catch (Exception e) {
            }
            return value;
        }

        /**
         * 获得配置参数。
         *
         * @param paramName
         * @return
         */
        public float getUserConfigFloatValue(String paramName) {
            float value = 0;
            if (this.userConfigMap == null) {
                return value;
            }
            try {
                value = Float.parseFloat( this.userConfigMap.getOrDefault( paramName, "0" ) );
            } catch (Exception e) {
            }
            return value;
        }

        /**
         * 获得配置参数。
         *
         * @param paramName
         * @return
         */
        public double getUserConfigDoubleValue(String paramName) {
            double value = 0;
            if (this.userConfigMap == null) {
                return value;
            }
            try {
                value = Double.parseDouble( this.userConfigMap.getOrDefault( paramName, "0" ) );
            } catch (Exception e) {
            }
            return value;
        }
    }

    public static final class Builder {
        private long saasId;
        private int userType;
        private long userId;
        private long mchId;
        private long groupId;
        private int isMaster;
        private String userName;
        private String realName;
        private String nickName;
        private String mobile;
        private String email;
        private String wxId;
        private int userGrade;
        private TokenPerm tokenPerm;
        private long expireAt;
        private long createAt;
        private String token;

        private Builder() {
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

        public Builder groupId(long groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder isMaster(int isMaster) {
            this.isMaster = isMaster;
            return this;
        }

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder realName(String realName) {
            this.realName = realName;
            return this;
        }

        public Builder nickName(String nickName) {
            this.nickName = nickName;
            return this;
        }

        public Builder mobile(String mobile) {
            this.mobile = mobile;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder wxId(String wxId) {
            this.wxId = wxId;
            return this;
        }

        public Builder userGrade(int userGrade) {
            this.userGrade = userGrade;
            return this;
        }

        public Builder tokenPerm(TokenPerm tokenPerm) {
            this.tokenPerm = tokenPerm;
            return this;
        }

        public Builder expireAt(long expireAt) {
            this.expireAt = expireAt;
            return this;
        }

        public Builder createAt(long createAt) {
            this.createAt = createAt;
            return this;
        }

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public AuthTokenData build() {
            return new AuthTokenData( this );
        }
    }
}
