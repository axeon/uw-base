package uw.auth.service.vo;


import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户信息和权限信息vo。
 */
public class MscUserRegister {

    /**
     * 用户Id
     */
    @Schema(title = "用户Id", description = "用户Id")
    private long id;

    /**
     * 运营商Id
     */
    @Schema(title = "运营商Id", description = "运营商Id")
    private long saasId;

    /**
     * 用户类型
     */
    @Schema(title = "用户类型", description = "用户类型")
    private int userType;

    /**
     * 商户编号
     */
    @Schema(title = "商户编号", description = "商户编号")
    private long mchId;

    /**
     * 所属用户组ID
     */
    @Schema(title = "所属用户组ID", description = "所属用户组ID")
    private long groupId;

    /**
     * 是否管理员
     */
    @Schema(title = "是否管理员", description = "是否管理员")
    private int isMaster;

    /**
     * 登录名称
     */
    @Schema(title = "登录名称", description = "登录名称")
    private String username;

    /**
     * 登录密码
     */
    @Schema(title = "登录密码", description = "登录密码")
    private String password;

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
     * 证件类型
     */
    @Schema(title = "证件类型", description = "证件类型")
    private int idType;

    /**
     * 证件信息
     */
    @Schema(title = "证件信息", description = "证件信息")
    private String idInfo;

    /**
     * 用户头像
     */
    @Schema(title = "用户头像", description = "用户头像")
    private String userIcon;

    /**
     * 用户级别
     */
    @Schema(title = "用户级别", description = "用户级别")
    private int userGrade;

    /**
     * 备注
     */
    @Schema(title = "备注", description = "备注")
    private String remark;

    /**
     * 用户权限ID列表
     */
    @Schema(title = "用户权限ID列表", description = "用户权限ID列表")
    private String userPerm;

    /**
     * 用户资源权限json
     */
    @Schema(title = "用户资源权限json", description = "用户资源权限json")
    private String userConfig;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public int getIdType() {
        return idType;
    }

    public void setIdType(int idType) {
        this.idType = idType;
    }

    public String getIdInfo() {
        return idInfo;
    }

    public void setIdInfo(String idInfo) {
        this.idInfo = idInfo;
    }

    public String getUserIcon() {
        return userIcon;
    }

    public void setUserIcon(String userIcon) {
        this.userIcon = userIcon;
    }

    public int getUserGrade() {
        return userGrade;
    }

    public void setUserGrade(int userGrade) {
        this.userGrade = userGrade;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getUserPerm() {
        return userPerm;
    }

    public void setUserPerm(String userPerm) {
        this.userPerm = userPerm;
    }

    public String getUserConfig() {
        return userConfig;
    }

    public void setUserConfig(String userConfig) {
        this.userConfig = userConfig;
    }
}
