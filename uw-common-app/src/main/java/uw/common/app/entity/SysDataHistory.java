package uw.common.app.entity;

import com.fasterxml.jackson.annotation.JsonRawValue;
import io.swagger.v3.oas.annotations.media.Schema;
import uw.common.util.JsonUtils;
import uw.dao.DataEntity;
import uw.dao.DataUpdateInfo;
import uw.dao.annotation.ColumnMeta;
import uw.dao.annotation.TableMeta;

import java.io.Serializable;


/**
 * SysDataHistory实体类
 * 系统数据历史
 *
 * @author axeon
 */
@TableMeta(tableName = "sys_data_history", tableType = "table")
@Schema(title = "系统数据历史", description = "系统数据历史")
public class SysDataHistory implements DataEntity, Serializable {


    /**
     * ID
     */
    @ColumnMeta(columnName = "id", dataType = "long", dataSize = 19, nullable = false, primaryKey = true)
    @Schema(title = "ID", description = "ID", maxLength = 19, nullable = false)
    private long id;

    /**
     * saasId
     */
    @ColumnMeta(columnName = "saas_id", dataType = "long", dataSize = 19, nullable = false, primaryKey = true)
    @Schema(title = "saasId", description = "saasId", maxLength = 19, nullable = false)
    private long saasId;

    /**
     * 商户ID
     */
    @ColumnMeta(columnName = "mch_id", dataType = "long", dataSize = 19, nullable = true)
    @Schema(title = "商户ID", description = "商户ID", maxLength = 19, nullable = true)
    private long mchId;

    /**
     * 用户ID
     */
    @ColumnMeta(columnName = "user_id", dataType = "long", dataSize = 19, nullable = false)
    @Schema(title = "用户ID", description = "用户ID", maxLength = 19, nullable = false)
    private long userId;

    /**
     * 用户类型
     */
    @ColumnMeta(columnName = "user_type", dataType = "int", dataSize = 10, nullable = true)
    @Schema(title = "用户类型", description = "用户类型", maxLength = 10, nullable = true)
    private int userType;

    /**
     * 用户的组ID
     */
    @ColumnMeta(columnName = "group_id", dataType = "long", dataSize = 19, nullable = true)
    @Schema(title = "用户的组ID", description = "用户的组ID", maxLength = 19, nullable = true)
    private long groupId;

    /**
     * 用户名称
     */
    @ColumnMeta(columnName = "user_name", dataType = "String", dataSize = 100, nullable = true)
    @Schema(title = "用户名称", description = "用户名称", maxLength = 100, nullable = true)
    private String userName;

    /**
     * 用户昵称
     */
    @ColumnMeta(columnName = "nick_name", dataType = "String", dataSize = 100, nullable = true)
    @Schema(title = "用户昵称", description = "用户昵称", maxLength = 100, nullable = true)
    private String nickName;

    /**
     * 真实名称
     */
    @ColumnMeta(columnName = "real_name", dataType = "String", dataSize = 100, nullable = true)
    @Schema(title = "真实名称", description = "真实名称", maxLength = 100, nullable = true)
    private String realName;

    /**
     * 实体类
     */
    @ColumnMeta(columnName = "entity_class", dataType = "String", dataSize = 100, nullable = true)
    @Schema(title = "实体类", description = "实体类", maxLength = 100, nullable = true)
    private String entityClass;

    /**
     * 实体ID
     */
    @ColumnMeta(columnName = "entity_id", dataType = "String", dataSize = 100, nullable = true)
    @Schema(title = "实体ID", description = "实体ID", maxLength = 100, nullable = true)
    private String entityId;

    /**
     * 实体名
     */
    @ColumnMeta(columnName = "entity_name", dataType = "String", dataSize = 200, nullable = true)
    @Schema(title = "实体名", description = "实体名", maxLength = 200, nullable = true)
    private String entityName;

    /**
     * 实体数据
     */
    @ColumnMeta(columnName = "entity_data", dataType = "String", dataSize = 1073741824, nullable = true)
    @Schema(title = "实体数据", description = "实体数据", maxLength = 1073741824, nullable = true)
    @JsonRawValue(value = false)
    private String entityData;

    /**
     * 实体修改信息
     */
    @ColumnMeta(columnName = "entity_update_info", dataType = "String", dataSize = 2147483646, nullable = true)
    @Schema(title = "实体修改信息", description = "实体修改信息", maxLength = 2147483646, nullable = true)
    private String entityUpdateInfo;

    /**
     * 备注信息
     */
    @ColumnMeta(columnName = "remark", dataType = "String", dataSize = 65535, nullable = true)
    @Schema(title = "备注信息", description = "备注信息", maxLength = 65535, nullable = true)
    private String remark;

    /**
     * 用户IP
     */
    @ColumnMeta(columnName = "user_ip", dataType = "String", dataSize = 50, nullable = true)
    @Schema(title = "用户IP", description = "用户IP", maxLength = 50, nullable = true)
    private String userIp;

    /**
     * 创建日期
     */
    @ColumnMeta(columnName = "create_date", dataType = "java.util.Date", dataSize = 23, nullable = true)
    @Schema(title = "创建日期", description = "创建日期", maxLength = 23, nullable = true)
    private java.util.Date createDate;

    /**
     * 数据更新信息.
     */
    private transient DataUpdateInfo _UPDATED_INFO = null;

    /**
     * 是否加载完成.
     */
    private transient boolean _IS_LOADED;

    /**
     * 获得实体的表名。
     */
    @Override
    public String ENTITY_TABLE() {
        return "sys_data_history";
    }

    /**
     * 获得实体的表注释。
     */
    @Override
    public String ENTITY_NAME() {
        return "系统数据历史";
    }

    /**
     * 获得主键
     */
    @Override
    public Serializable ENTITY_ID() {
        return getId();
    }

    /**
     * 获取更新信息.
     */
    @Override
    public DataUpdateInfo GET_UPDATED_INFO() {
        return this._UPDATED_INFO;
    }

    /**
     * 清除更新信息.
     */
    @Override
    public void CLEAR_UPDATED_INFO() {
        _UPDATED_INFO = null;
    }


    /**
     * 获取ID。
     */
    public long getId() {
        return this.id;
    }

    /**
     * 设置ID。
     */
    public void setId(long id) {
        _UPDATED_INFO = DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "id", this.id, id, !_IS_LOADED);
        this.id = id;
    }

    /**
     * 获取saasId。
     */
    public long getSaasId() {
        return this.saasId;
    }

    /**
     * 设置saasId。
     */
    public void setSaasId(long saasId) {
        _UPDATED_INFO = DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "saasId", this.saasId, saasId, !_IS_LOADED);
        this.saasId = saasId;
    }

    /**
     * 获取商户ID。
     */
    public long getMchId() {
        return this.mchId;
    }

    /**
     * 设置商户ID。
     */
    public void setMchId(long mchId) {
        _UPDATED_INFO = DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "mchId", this.mchId, mchId, !_IS_LOADED);
        this.mchId = mchId;
    }

    /**
     * 获取用户ID。
     */
    public long getUserId() {
        return this.userId;
    }

    /**
     * 设置用户ID。
     */
    public void setUserId(long userId) {
        _UPDATED_INFO = DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "userId", this.userId, userId, !_IS_LOADED);
        this.userId = userId;
    }

    /**
     * 获取用户类型。
     */
    public int getUserType() {
        return this.userType;
    }

    /**
     * 设置用户类型。
     */
    public void setUserType(int userType) {
        _UPDATED_INFO = DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "userType", this.userType, userType, !_IS_LOADED);
        this.userType = userType;
    }

    /**
     * 获取用户的组ID。
     */
    public long getGroupId() {
        return this.groupId;
    }

    /**
     * 设置用户的组ID。
     */
    public void setGroupId(long groupId) {
        _UPDATED_INFO = DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "groupId", this.groupId, groupId, !_IS_LOADED);
        this.groupId = groupId;
    }

    /**
     * 获取用户名称。
     */
    public String getUserName() {
        return this.userName;
    }

    /**
     * 设置用户名称。
     */
    public void setUserName(String userName) {
        _UPDATED_INFO = DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "userName", this.userName, userName, !_IS_LOADED);
        this.userName = userName;
    }

    /**
     * 获取用户昵称。
     */
    public String getNickName() {
        return this.nickName;
    }

    /**
     * 设置用户昵称。
     */
    public void setNickName(String nickName) {
        _UPDATED_INFO = DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "nickName", this.nickName, nickName, !_IS_LOADED);
        this.nickName = nickName;
    }

    /**
     * 获取真实名称。
     */
    public String getRealName() {
        return this.realName;
    }

    /**
     * 设置真实名称。
     */
    public void setRealName(String realName) {
        _UPDATED_INFO = DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "realName", this.realName, realName, !_IS_LOADED);
        this.realName = realName;
    }

    /**
     * 获取实体类。
     */
    public String getEntityClass() {
        return this.entityClass;
    }

    /**
     * 设置实体类。
     */
    public void setEntityClass(String entityClass) {
        _UPDATED_INFO = DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "entityClass", this.entityClass, entityClass, !_IS_LOADED);
        this.entityClass = entityClass;
    }

    /**
     * 获取实体ID。
     */
    public String getEntityId() {
        return this.entityId;
    }

    /**
     * 设置实体ID。
     */
    public void setEntityId(String entityId) {
        _UPDATED_INFO = DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "entityId", this.entityId, entityId, !_IS_LOADED);
        this.entityId = entityId;
    }

    /**
     * 获取实体名。
     */
    public String getEntityName() {
        return this.entityName;
    }

    /**
     * 设置实体名。
     */
    public void setEntityName(String entityName) {
        _UPDATED_INFO = DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "entityName", this.entityName, entityName, !_IS_LOADED);
        this.entityName = entityName;
    }

    /**
     * 获取实体数据。
     */
    public String getEntityData() {
        return this.entityData;
    }

    /**
     * 设置实体数据。
     */
    public void setEntityData(String entityData) {
        _UPDATED_INFO = DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "entityData", this.entityData, entityData, !_IS_LOADED);
        this.entityData = entityData;
    }

    /**
     * 获取实体修改信息。
     */
    public String getEntityUpdateInfo() {
        return this.entityUpdateInfo;
    }

    /**
     * 设置实体修改信息。
     */
    public void setEntityUpdateInfo(String entityUpdateInfo) {
        _UPDATED_INFO = DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "entityUpdateInfo", this.entityUpdateInfo, entityUpdateInfo, !_IS_LOADED);
        this.entityUpdateInfo = entityUpdateInfo;
    }

    /**
     * 获取备注信息。
     */
    public String getRemark() {
        return this.remark;
    }

    /**
     * 设置备注信息。
     */
    public void setRemark(String remark) {
        _UPDATED_INFO = DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "remark", this.remark, remark, !_IS_LOADED);
        this.remark = remark;
    }

    /**
     * 获取用户IP。
     */
    public String getUserIp() {
        return this.userIp;
    }

    /**
     * 设置用户IP。
     */
    public void setUserIp(String userIp) {
        _UPDATED_INFO = DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "userIp", this.userIp, userIp, !_IS_LOADED);
        this.userIp = userIp;
    }

    /**
     * 获取创建日期。
     */
    public java.util.Date getCreateDate() {
        return this.createDate;
    }

    /**
     * 设置创建日期。
     */
    public void setCreateDate(java.util.Date createDate) {
        _UPDATED_INFO = DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "createDate", this.createDate, createDate, !_IS_LOADED);
        this.createDate = createDate;
    }

    /**
     * 设置ID链式调用。
     */
    public SysDataHistory id(long id) {
        setId(id);
        return this;
    }

    /**
     * 设置saasId链式调用。
     */
    public SysDataHistory saasId(long saasId) {
        setSaasId(saasId);
        return this;
    }

    /**
     * 设置商户ID链式调用。
     */
    public SysDataHistory mchId(long mchId) {
        setMchId(mchId);
        return this;
    }

    /**
     * 设置用户ID链式调用。
     */
    public SysDataHistory userId(long userId) {
        setUserId(userId);
        return this;
    }

    /**
     * 设置用户类型链式调用。
     */
    public SysDataHistory userType(int userType) {
        setUserType(userType);
        return this;
    }

    /**
     * 设置用户的组ID链式调用。
     */
    public SysDataHistory groupId(long groupId) {
        setGroupId(groupId);
        return this;
    }

    /**
     * 设置用户名称链式调用。
     */
    public SysDataHistory userName(String userName) {
        setUserName(userName);
        return this;
    }

    /**
     * 设置用户昵称链式调用。
     */
    public SysDataHistory nickName(String nickName) {
        setNickName(nickName);
        return this;
    }

    /**
     * 设置真实名称链式调用。
     */
    public SysDataHistory realName(String realName) {
        setRealName(realName);
        return this;
    }

    /**
     * 设置实体类链式调用。
     */
    public SysDataHistory entityClass(String entityClass) {
        setEntityClass(entityClass);
        return this;
    }

    /**
     * 设置实体ID链式调用。
     */
    public SysDataHistory entityId(String entityId) {
        setEntityId(entityId);
        return this;
    }

    /**
     * 设置实体名链式调用。
     */
    public SysDataHistory entityName(String entityName) {
        setEntityName(entityName);
        return this;
    }

    /**
     * 设置实体数据链式调用。
     */
    public SysDataHistory entityData(String entityData) {
        setEntityData(entityData);
        return this;
    }

    /**
     * 设置实体修改信息链式调用。
     */
    public SysDataHistory entityUpdateInfo(String entityUpdateInfo) {
        setEntityUpdateInfo(entityUpdateInfo);
        return this;
    }

    /**
     * 设置备注信息链式调用。
     */
    public SysDataHistory remark(String remark) {
        setRemark(remark);
        return this;
    }

    /**
     * 设置用户IP链式调用。
     */
    public SysDataHistory userIp(String userIp) {
        setUserIp(userIp);
        return this;
    }

    /**
     * 设置创建日期链式调用。
     */
    public SysDataHistory createDate(java.util.Date createDate) {
        setCreateDate(createDate);
        return this;
    }

    /**
     * 重载toString方法.
     */
    @Override
    public String toString() {
        return JsonUtils.toString(this);
    }

}