package uw.auth.service.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Date;

/**
 * MscUser实体类
 * MSC用户
 *
 * @author axeon
 */
@Schema(title = "MSC用户", description = "MSC用户")
public class MscUserVo implements Serializable {

    public MscUserVo() {
    }

    public MscUserVo(long id, long saasId, int userType, long mchId, long groupId, int isMaster, String username, String realName, String nickName, String mobile, String email, String wxId, int userGrade, String remark, Date lastLogonDate, String lastLogonIp, int logonCount, Date createDate, Date modifyDate, int state) {
        this.id = id;
        this.saasId = saasId;
        this.userType = userType;
        this.mchId = mchId;
        this.groupId = groupId;
        this.isMaster = isMaster;
        this.username = username;
        this.realName = realName;
        this.nickName = nickName;
        this.mobile = mobile;
        this.email = email;
        this.wxId = wxId;
        this.userGrade = userGrade;
        this.remark = remark;
        this.lastLogonDate = lastLogonDate;
        this.lastLogonIp = lastLogonIp;
        this.logonCount = logonCount;
        this.createDate = createDate;
        this.modifyDate = modifyDate;
        this.state = state;
    }

    /**
     * 用户Id
     */
    @Schema(title = "用户Id", description = "用户Id")
    private long id;

    /**
     * 运营商Id
     */
    @Schema(title = "saasId", description = "saasId")
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
     * 真实名称
     */
    @Schema(title = "真实名称", description = "真实名称")
    private String realName;

    /**
     * 别名 [用于业务前台匿名]
     */
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
     * 备注
     */
    @Schema(title = "备注", description = "备注")
    private String remark;

    /**
     * 最后登录时间
     */
    @Schema(title = "最后登录时间", description = "最后登录时间")
    private java.util.Date lastLogonDate;

    /**
     * 最后登录ip
     */
    @Schema(title = "最后登录ip", description = "最后登录ip")
    private String lastLogonIp;

    /**
     * 登录次数
     */
    @Schema(title = "登录次数", description = "登录次数")
    private int logonCount;

    /**
     * 创建日期
     */
    @Schema(title = "创建日期", description = "创建日期")
    private java.util.Date createDate;

    /**
     * 修改日期
     */
    @Schema(title = "修改日期", description = "修改日期")
    private java.util.Date modifyDate;

    /**
     * 状态：-1: 删除 0: 冻结 1: 正常
     */
    @Schema(title = "状态：-1: 删除 0: 冻结 1: 正常", description = "状态：-1: 删除 0: 冻结 1: 正常")
    private int state;

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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Date getLastLogonDate() {
        return lastLogonDate;
    }

    public void setLastLogonDate(Date lastLogonDate) {
        this.lastLogonDate = lastLogonDate;
    }

    public String getLastLogonIp() {
        return lastLogonIp;
    }

    public void setLastLogonIp(String lastLogonIp) {
        this.lastLogonIp = lastLogonIp;
    }

    public int getLogonCount() {
        return logonCount;
    }

    public void setLogonCount(int logonCount) {
        this.logonCount = logonCount;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getModifyDate() {
        return modifyDate;
    }

    public void setModifyDate(Date modifyDate) {
        this.modifyDate = modifyDate;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
