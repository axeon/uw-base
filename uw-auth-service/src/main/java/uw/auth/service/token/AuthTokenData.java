package uw.auth.service.token;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;
import java.util.Set;

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
     * 登录IP
     */
    @Schema(title = "登录IP", description = "登录IP")
    private String userIp;

    /**
     * 用户级别
     */
    @Schema(title = "用户级别", description = "用户级别")
    private int userGrade;

    /**
     * Token过期时间
     */
    private long expireAt;

    /**
     * Token创建时间
     */
    private long createAt;

    /**
     * 权限ID集合。
     */
    private Set<Integer> permSet;

    /**
     * 用户配置。
     */
    private Map<String, String> configMap;

    public AuthTokenData() {

    }

    public AuthTokenData(long saasId, int userType, long userId, long mchId, long groupId, int isMaster, String userName, String nickName, String realName, String mobile,
                         String email, String userIp, int userGrade) {
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
        this.userIp = userIp;
        this.userGrade = userGrade;
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

    public String getUserIp() {
        return userIp;
    }

    public void setUserIp(String userIp) {
        this.userIp = userIp;
    }

    public int getUserGrade() {
        return userGrade;
    }

    public void setUserGrade(int userGrade) {
        this.userGrade = userGrade;
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

    public Set<Integer> getPermSet() {
        return permSet;
    }

    public void setPermSet(Set<Integer> permSet) {
        this.permSet = permSet;
    }

    public Map<String, String> getConfigMap() {
        return configMap;
    }

    public void setConfigMap(Map<String, String> configMap) {
        this.configMap = configMap;
    }


    /**
     * 获取用户配置参数。
     * @param paramName
     * @return
     */
    public String getConfig( String paramName) {
        return getConfig( paramName, null);
    }

    /**
     * 获取用户配置参数。
     *
     * @param paramName    参数名字
     * @param defaultValue 集合没值时或者报异常，默认返回值
     * @return 参数的值
     */
    private String getConfig( String paramName, String defaultValue) {
        if (configMap == null || configMap.isEmpty()) {
            return defaultValue;
        }
        String temp = configMap.get( paramName );
        if (temp != null && !temp.isEmpty()) {
            return temp;
        }
        return defaultValue;
    }

    /**
     * 获取用户配置参数(int)
     *
     * @param paramName 参数名字
     * @return 参数的值
     */
    public int getIntConfig( String paramName) {
        return getIntConfig( paramName, 0);
    }

    /**
     * 获取用户配置参数(int)
     *
     * @param paramName    参数名字
     * @param defaultValue 集合没值时或者报异常，默认的返回值
     * @return 参数的值
     */
    private int getIntConfig( String paramName, int defaultValue) {
        if (configMap == null || configMap.isEmpty()) {
            return defaultValue;
        }
        String temp = configMap.get( paramName );
        if (temp != null && !temp.isEmpty()) {
            try {
                return Integer.parseInt( temp );
            } catch (Exception ignored) {
            }
        }
        return defaultValue;
    }

    /**
     * 获取用户配置参数(long)
     *
     * @param paramName    参数名字
     * @return 参数的值
     */
    public long getLongConfig( String paramName) {
        return getLongConfig( paramName, 0);
    }

    /**
     * 获取用户配置参数(long)
     *
     * @param paramName    参数名字
     * @param defaultValue 集合没值时或者报异常，默认的返回值
     * @return 参数的值
     */
    public long getLongConfig( String paramName, long defaultValue) {
        if (configMap == null || configMap.isEmpty()) {
            return defaultValue;
        }
        String temp = configMap.get( paramName );
        if (temp != null && !temp.isEmpty()) {
            try {
                return Long.parseLong( temp );
            } catch (Exception ignored) {
            }
        }
        return defaultValue;
    }

    /**
     * 获取用户配置参数(double)
     *
     * @param paramName    参数名字
     * @return 参数的值
     */
    public double getDoubleConfig( String paramName) {
        return getDoubleConfig( paramName, 0);
    }

    /**
     * 获取用户配置参数(double)
     *
     * @param paramName    参数名字
     * @param defaultValue 集合没值时或者报异常，默认的返回值
     * @return 参数的值
     */
    public double getDoubleConfig( String paramName, double defaultValue) {
        if (configMap == null || configMap.isEmpty()) {
            return defaultValue;
        }
        String temp = configMap.get( paramName );
        if (temp != null && !temp.isEmpty()) {
            try {
                return Double.parseDouble( temp );
            } catch (Exception ignored) {
            }
        }
        return defaultValue;
    }

    /**
     * 获取用户配置参数(float)
     *
     * @param paramName    参数名字
     * @return 参数的值
     */
    public float getFloatConfig( String paramName) {
        return getFloatConfig( paramName, 0);
    }

    /**
     * 获取用户配置参数(float)
     *
     * @param paramName    参数名字
     * @param defaultValue 集合没值时或者报异常，默认的返回值
     * @return 参数的值
     */
    public float getFloatConfig( String paramName, float defaultValue) {
        if (configMap == null || configMap.isEmpty()) {
            return defaultValue;
        }
        String temp = configMap.get( paramName );
        if (temp != null && !temp.isEmpty()) {
            try {
                return Float.parseFloat( temp );
            } catch (Exception ignored) {
            }
        }
        return defaultValue;
    }

    /**
     * 获取用户配置参数(boolean)
     *
     * @param paramName    参数名字
     * @return 参数的值
     */
    public boolean getBooleanConfig( String paramName) {
        return getBooleanConfig( paramName, false);
    }

    /**
     * 获取用户配置参数(boolean)
     *
     * @param paramName    参数名字
     * @param defaultValue 集合没值时或者报异常，默认的返回值
     * @return 参数的值
     */
    public boolean getBooleanConfig( String paramName, boolean defaultValue) {
        if (configMap == null || configMap.isEmpty()) {
            return defaultValue;
        }
        String temp = configMap.get( paramName );
        if (temp != null && !temp.isEmpty()) {
            try {
                return Boolean.parseBoolean( temp );
            } catch (Exception ignored) {
            }
        }
        return defaultValue;
    }


}
