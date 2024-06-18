package uw.auth.service.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Date;

/**
 * MscUserGroup实体类
 * MSC用户组
 *
 * @author axeon
 */
@Schema(title = "MSC用户组", description = "MSC用户组")
public class MscUserGroupVo implements Serializable {

    public MscUserGroupVo() {
    }

    public MscUserGroupVo(long id, long saasId, long mchId, int userType, String groupName, String groupDesc, String groupPerm, String groupConfig, Date createDate, Date modifyDate, int state) {
        this.id = id;
        this.saasId = saasId;
        this.mchId = mchId;
        this.userType = userType;
        this.groupName = groupName;
        this.groupDesc = groupDesc;
        this.groupPerm = groupPerm;
        this.groupConfig = groupConfig;
        this.createDate = createDate;
        this.modifyDate = modifyDate;
        this.state = state;
    }

    /**
     * 主键
     */
    @Schema(title = "主键", description = "主键")
    private long id;

    /**
     * 运营商编号。0为全局权限。
     */
    @Schema(title = "saasId", description = "saasId")
    private long saasId;

    /**
     * 商户编号。0运营商1默认供应商2默认分销商
     */
    @Schema(title = "商户编号。0运营商1默认供应商2默认分销商", description = "商户编号。0运营商1默认供应商2默认分销商")
    private long mchId;

    /**
     * 用户类型
     */
    @Schema(title = "用户类型", description = "用户类型")
    private int userType;

    /**
     * 分组名称
     */
    @Schema(title = "分组名称", description = "分组名称")
    private String groupName;

    /**
     * 分组描述
     */
    @Schema(title = "分组描述", description = "分组描述")
    private String groupDesc;

    /**
     * 分组默认权限
     */
    @Schema(title = "分组默认权限", description = "分组默认权限")
    private String groupPerm;

    /**
     * 分组默认配置
     */
    @Schema(title = "分组默认配置", description = "分组默认配置")
    private String groupConfig;

    /**
     * 创建时间
     */
    @Schema(title = "创建时间", description = "创建时间")
    private java.util.Date createDate;

    /**
     * 修改时间
     */
    @Schema(title = "修改时间", description = "修改时间")
    private java.util.Date modifyDate;

    /**
     * 状态值: -1: 删除 0: 冻结 1: 正常
     */
    @Schema(title = "状态值: -1: 删除 0: 冻结 1: 正常", description = "状态值: -1: 删除 0: 冻结 1: 正常")
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

    public long getMchId() {
        return mchId;
    }

    public void setMchId(long mchId) {
        this.mchId = mchId;
    }

    public int getUserType() {
        return userType;
    }

    public void setUserType(int userType) {
        this.userType = userType;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupDesc() {
        return groupDesc;
    }

    public void setGroupDesc(String groupDesc) {
        this.groupDesc = groupDesc;
    }

    public String getGroupPerm() {
        return groupPerm;
    }

    public void setGroupPerm(String groupPerm) {
        this.groupPerm = groupPerm;
    }

    public String getGroupConfig() {
        return groupConfig;
    }

    public void setGroupConfig(String groupConfig) {
        this.groupConfig = groupConfig;
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
