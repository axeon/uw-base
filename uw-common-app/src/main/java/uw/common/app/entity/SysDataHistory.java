package uw.common.app.entity;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonRawValue;
import io.swagger.v3.oas.annotations.media.Schema;
import uw.dao.DataEntity;
import uw.dao.annotation.ColumnMeta;
import uw.dao.annotation.TableMeta;

/**
 * SysDataHistory实体类
 * 系统数据历史
 *
 * @author axeon
 */
@TableMeta(tableName="sys_data_history",tableType="table")
@Schema(title = "系统数据历史", description = "系统数据历史")
public class SysDataHistory implements DataEntity,Serializable{


    /**
     * ID
     */
    @ColumnMeta(columnName="id", dataType="long", dataSize=19, nullable=false, primaryKey=true)
    @Schema(title = "ID", description = "ID", maxLength=19, nullable=false )
    private long id;

    /**
     * saasId
     */
    @ColumnMeta(columnName="saas_id", dataType="long", dataSize=19, nullable=false, primaryKey=true)
    @Schema(title = "saasId", description = "saasId", maxLength=19, nullable=false )
    private long saasId;

    /**
     * 商户ID
     */
    @ColumnMeta(columnName="mch_id", dataType="long", dataSize=19, nullable=true)
    @Schema(title = "商户ID", description = "商户ID", maxLength=19, nullable=true )
    private long mchId;

    /**
     * 用户ID
     */
    @ColumnMeta(columnName="user_id", dataType="long", dataSize=19, nullable=false)
    @Schema(title = "用户ID", description = "用户ID", maxLength=19, nullable=false )
    private long userId;

    /**
     * 用户类型
     */
    @ColumnMeta(columnName="user_type", dataType="int", dataSize=10, nullable=true)
    @Schema(title = "用户类型", description = "用户类型", maxLength=10, nullable=true )
    private int userType;

    /**
     * 用户的组ID
     */
    @ColumnMeta(columnName="group_id", dataType="long", dataSize=19, nullable=true)
    @Schema(title = "用户的组ID", description = "用户的组ID", maxLength=19, nullable=true )
    private long groupId;

    /**
     * 用户名称
     */
    @ColumnMeta(columnName="user_name", dataType="String", dataSize=100, nullable=true)
    @Schema(title = "用户名称", description = "用户名称", maxLength=100, nullable=true )
    private String userName;

    /**
     * 用户昵称
     */
    @ColumnMeta(columnName="nick_name", dataType="String", dataSize=100, nullable=true)
    @Schema(title = "用户昵称", description = "用户昵称", maxLength=100, nullable=true )
    private String nickName;

    /**
     * 真实名称
     */
    @ColumnMeta(columnName="real_name", dataType="String", dataSize=100, nullable=true)
    @Schema(title = "真实名称", description = "真实名称", maxLength=100, nullable=true )
    private String realName;

    /**
     * 实体类
     */
    @ColumnMeta(columnName="entity_class", dataType="String", dataSize=100, nullable=true)
    @Schema(title = "实体类", description = "实体类", maxLength=100, nullable=true )
    private String entityClass;

    /**
     * 实体ID
     */
    @ColumnMeta(columnName="entity_id", dataType="String", dataSize=100, nullable=true)
    @Schema(title = "实体ID", description = "实体ID", maxLength=100, nullable=true )
    private String entityId;

    /**
     * 实体名
     */
    @ColumnMeta(columnName="entity_name", dataType="String", dataSize=200, nullable=true)
    @Schema(title = "实体名", description = "实体名", maxLength=200, nullable=true )
    private String entityName;

    /**
     * 实体数据
     */
    @ColumnMeta(columnName="entity_data", dataType="String", dataSize=1073741824, nullable=true)
    @Schema(title = "实体数据", description = "实体数据", maxLength=1073741824, nullable=true )
    @JsonRawValue(value = false)
    private String entityData;

    /**
     * 实体修改信息
     */
    @ColumnMeta(columnName="entity_update_info", dataType="String", dataSize=2147483646, nullable=true)
    @Schema(title = "实体修改信息", description = "实体修改信息", maxLength=2147483646, nullable=true )
    private String entityUpdateInfo;

    /**
     * 备注信息
     */
    @ColumnMeta(columnName="remark", dataType="String", dataSize=65535, nullable=true)
    @Schema(title = "备注信息", description = "备注信息", maxLength=65535, nullable=true )
    private String remark;

    /**
     * 用户IP
     */
    @ColumnMeta(columnName="user_ip", dataType="String", dataSize=50, nullable=true)
    @Schema(title = "用户IP", description = "用户IP", maxLength=50, nullable=true )
    private String userIp;

    /**
     * 创建日期
     */
    @ColumnMeta(columnName="create_date", dataType="java.util.Date", dataSize=23, nullable=true)
    @Schema(title = "创建日期", description = "创建日期", maxLength=23, nullable=true )
    private java.util.Date createDate;

    /**
     * 轻量级状态下更新列表list.
     */
    private transient Set<String> UPDATED_COLUMN = null;

    /**
     * 更新的信息.
     */
    private transient StringBuilder UPDATED_INFO = null;


    /**
     * 获得实体的表名。
     */
    @Override
    public String ENTITY_TABLE(){
         return "sys_data_history";
       }

    /**
     * 获得实体的表注释。
     */
    @Override
    public String ENTITY_NAME(){
          return "系统数据历史";
       }

    /**
     * 获得主键
     */
    @Override
    public Serializable ENTITY_ID(){
          return getId();
       }


    /**
     * 获取更改的字段列表.
     */
    @Override
    public Set<String> GET_UPDATED_COLUMN() {
        return UPDATED_COLUMN;
    }

    /**
     * 获取文本更新信息.
     */
    @Override
    public String GET_UPDATED_INFO() {
        if (this.UPDATED_INFO == null) {
            return null;
        } else {
            return this.UPDATED_INFO.toString();
        }
    }

    /**
     * 清除更新信息.
     */
    @Override
    public void CLEAR_UPDATED_INFO() {
        UPDATED_COLUMN = null;
        UPDATED_INFO = null;
    }

    /**
     * 初始化set相关的信息.
     */
    private void _INIT_UPDATE_INFO() {
        this.UPDATED_COLUMN = new HashSet<String>();
        this.UPDATED_INFO = new StringBuilder("表sys_data_history主键\"" + 
        this.id+ "\"更新为:\r\n");
    }


    /**
     * 获取ID。
     */
    public long getId(){
        return this.id;
    }

    /**
     * 获取saasId。
     */
    public long getSaasId(){
        return this.saasId;
    }

    /**
     * 获取商户ID。
     */
    public long getMchId(){
        return this.mchId;
    }

    /**
     * 获取用户ID。
     */
    public long getUserId(){
        return this.userId;
    }

    /**
     * 获取用户类型。
     */
    public int getUserType(){
        return this.userType;
    }

    /**
     * 获取用户的组ID。
     */
    public long getGroupId(){
        return this.groupId;
    }

    /**
     * 获取用户名称。
     */
    public String getUserName(){
        return this.userName;
    }

    /**
     * 获取用户昵称。
     */
    public String getNickName(){
        return this.nickName;
    }

    /**
     * 获取真实名称。
     */
    public String getRealName(){
        return this.realName;
    }

    /**
     * 获取实体类。
     */
    public String getEntityClass(){
        return this.entityClass;
    }

    /**
     * 获取实体ID。
     */
    public String getEntityId(){
        return this.entityId;
    }

    /**
     * 获取实体名。
     */
    public String getEntityName(){
        return this.entityName;
    }

    /**
     * 获取实体数据。
     */
    public String getEntityData(){
        return this.entityData;
    }

    /**
     * 获取实体修改信息。
     */
    public String getEntityUpdateInfo(){
        return this.entityUpdateInfo;
    }

    /**
     * 获取备注信息。
     */
    public String getRemark(){
        return this.remark;
    }

    /**
     * 获取用户IP。
     */
    public String getUserIp(){
        return this.userIp;
    }

    /**
     * 获取创建日期。
     */
    public java.util.Date getCreateDate(){
        return this.createDate;
    }


    /**
     * 设置ID。
     */
    public void setId(long id){
        if (!Objects.equals(this.id, id)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("id");
            this.UPDATED_INFO.append("id:\"").append(this.id).append("\"=>\"").append(id).append("\"\n");
            this.id = id;
        }
    }

    /**
     *  设置ID链式调用。
     */
    public SysDataHistory id(long id){
        setId(id);
        return this;
        }

    /**
     * 设置saasId。
     */
    public void setSaasId(long saasId){
        if (!Objects.equals(this.saasId, saasId)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("saas_id");
            this.UPDATED_INFO.append("saas_id:\"").append(this.saasId).append("\"=>\"").append(saasId).append("\"\n");
            this.saasId = saasId;
        }
    }

    /**
     *  设置saasId链式调用。
     */
    public SysDataHistory saasId(long saasId){
        setSaasId(saasId);
        return this;
        }

    /**
     * 设置商户ID。
     */
    public void setMchId(long mchId){
        if (!Objects.equals(this.mchId, mchId)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("mch_id");
            this.UPDATED_INFO.append("mch_id:\"").append(this.mchId).append("\"=>\"").append(mchId).append("\"\n");
            this.mchId = mchId;
        }
    }

    /**
     *  设置商户ID链式调用。
     */
    public SysDataHistory mchId(long mchId){
        setMchId(mchId);
        return this;
        }

    /**
     * 设置用户ID。
     */
    public void setUserId(long userId){
        if (!Objects.equals(this.userId, userId)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("user_id");
            this.UPDATED_INFO.append("user_id:\"").append(this.userId).append("\"=>\"").append(userId).append("\"\n");
            this.userId = userId;
        }
    }

    /**
     *  设置用户ID链式调用。
     */
    public SysDataHistory userId(long userId){
        setUserId(userId);
        return this;
        }

    /**
     * 设置用户类型。
     */
    public void setUserType(int userType){
        if (!Objects.equals(this.userType, userType)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("user_type");
            this.UPDATED_INFO.append("user_type:\"").append(this.userType).append("\"=>\"").append(userType).append("\"\n");
            this.userType = userType;
        }
    }

    /**
     *  设置用户类型链式调用。
     */
    public SysDataHistory userType(int userType){
        setUserType(userType);
        return this;
        }

    /**
     * 设置用户的组ID。
     */
    public void setGroupId(long groupId){
        if (!Objects.equals(this.groupId, groupId)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("group_id");
            this.UPDATED_INFO.append("group_id:\"").append(this.groupId).append("\"=>\"").append(groupId).append("\"\n");
            this.groupId = groupId;
        }
    }

    /**
     *  设置用户的组ID链式调用。
     */
    public SysDataHistory groupId(long groupId){
        setGroupId(groupId);
        return this;
        }

    /**
     * 设置用户名称。
     */
    public void setUserName(String userName){
        if (!Objects.equals(this.userName, userName)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("user_name");
            this.UPDATED_INFO.append("user_name:\"").append(this.userName).append("\"=>\"").append(userName).append("\"\n");
            this.userName = userName;
        }
    }

    /**
     *  设置用户名称链式调用。
     */
    public SysDataHistory userName(String userName){
        setUserName(userName);
        return this;
        }

    /**
     * 设置用户昵称。
     */
    public void setNickName(String nickName){
        if (!Objects.equals(this.nickName, nickName)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("nick_name");
            this.UPDATED_INFO.append("nick_name:\"").append(this.nickName).append("\"=>\"").append(nickName).append("\"\n");
            this.nickName = nickName;
        }
    }

    /**
     *  设置用户昵称链式调用。
     */
    public SysDataHistory nickName(String nickName){
        setNickName(nickName);
        return this;
        }

    /**
     * 设置真实名称。
     */
    public void setRealName(String realName){
        if (!Objects.equals(this.realName, realName)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("real_name");
            this.UPDATED_INFO.append("real_name:\"").append(this.realName).append("\"=>\"").append(realName).append("\"\n");
            this.realName = realName;
        }
    }

    /**
     *  设置真实名称链式调用。
     */
    public SysDataHistory realName(String realName){
        setRealName(realName);
        return this;
        }

    /**
     * 设置实体类。
     */
    public void setEntityClass(String entityClass){
        if (!Objects.equals(this.entityClass, entityClass)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("entity_class");
            this.UPDATED_INFO.append("entity_class:\"").append(this.entityClass).append("\"=>\"").append(entityClass).append("\"\n");
            this.entityClass = entityClass;
        }
    }

    /**
     *  设置实体类链式调用。
     */
    public SysDataHistory entityClass(String entityClass){
        setEntityClass(entityClass);
        return this;
        }

    /**
     * 设置实体ID。
     */
    public void setEntityId(String entityId){
        if (!Objects.equals(this.entityId, entityId)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("entity_id");
            this.UPDATED_INFO.append("entity_id:\"").append(this.entityId).append("\"=>\"").append(entityId).append("\"\n");
            this.entityId = entityId;
        }
    }

    /**
     *  设置实体ID链式调用。
     */
    public SysDataHistory entityId(String entityId){
        setEntityId(entityId);
        return this;
        }

    /**
     * 设置实体名。
     */
    public void setEntityName(String entityName){
        if (!Objects.equals(this.entityName, entityName)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("entity_name");
            this.UPDATED_INFO.append("entity_name:\"").append(this.entityName).append("\"=>\"").append(entityName).append("\"\n");
            this.entityName = entityName;
        }
    }

    /**
     *  设置实体名链式调用。
     */
    public SysDataHistory entityName(String entityName){
        setEntityName(entityName);
        return this;
        }

    /**
     * 设置实体数据。
     */
    public void setEntityData(String entityData){
        if (!Objects.equals(this.entityData, entityData)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("entity_data");
            this.UPDATED_INFO.append("entity_data:\"").append(this.entityData).append("\"=>\"").append(entityData).append("\"\n");
            this.entityData = entityData;
        }
    }

    /**
     *  设置实体数据链式调用。
     */
    public SysDataHistory entityData(String entityData){
        setEntityData(entityData);
        return this;
        }

    /**
     * 设置实体修改信息。
     */
    public void setEntityUpdateInfo(String entityUpdateInfo){
        if (!Objects.equals(this.entityUpdateInfo, entityUpdateInfo)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("entity_update_info");
            this.UPDATED_INFO.append("entity_update_info:\"").append(this.entityUpdateInfo).append("\"=>\"").append(entityUpdateInfo).append("\"\n");
            this.entityUpdateInfo = entityUpdateInfo;
        }
    }

    /**
     *  设置实体修改信息链式调用。
     */
    public SysDataHistory entityUpdateInfo(String entityUpdateInfo){
        setEntityUpdateInfo(entityUpdateInfo);
        return this;
        }

    /**
     * 设置备注信息。
     */
    public void setRemark(String remark){
        if (!Objects.equals(this.remark, remark)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("remark");
            this.UPDATED_INFO.append("remark:\"").append(this.remark).append("\"=>\"").append(remark).append("\"\n");
            this.remark = remark;
        }
    }

    /**
     *  设置备注信息链式调用。
     */
    public SysDataHistory remark(String remark){
        setRemark(remark);
        return this;
        }

    /**
     * 设置用户IP。
     */
    public void setUserIp(String userIp){
        if (!Objects.equals(this.userIp, userIp)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("user_ip");
            this.UPDATED_INFO.append("user_ip:\"").append(this.userIp).append("\"=>\"").append(userIp).append("\"\n");
            this.userIp = userIp;
        }
    }

    /**
     *  设置用户IP链式调用。
     */
    public SysDataHistory userIp(String userIp){
        setUserIp(userIp);
        return this;
        }

    /**
     * 设置创建日期。
     */
    public void setCreateDate(java.util.Date createDate){
        if (!Objects.equals(this.createDate, createDate)){
            if (this.UPDATED_COLUMN == null) {
                _INIT_UPDATE_INFO();
            }
            this.UPDATED_COLUMN.add("create_date");
            this.UPDATED_INFO.append("create_date:\"").append(this.createDate).append("\"=>\"").append(createDate).append("\"\n");
            this.createDate = createDate;
        }
    }

    /**
     *  设置创建日期链式调用。
     */
    public SysDataHistory createDate(java.util.Date createDate){
        setCreateDate(createDate);
        return this;
        }

    /**
     * 重载toString方法.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("id:\"" + this.id + "\"\r\n");
        sb.append("saas_id:\"" + this.saasId + "\"\r\n");
        sb.append("mch_id:\"" + this.mchId + "\"\r\n");
        sb.append("user_id:\"" + this.userId + "\"\r\n");
        sb.append("user_type:\"" + this.userType + "\"\r\n");
        sb.append("group_id:\"" + this.groupId + "\"\r\n");
        sb.append("user_name:\"" + this.userName + "\"\r\n");
        sb.append("nick_name:\"" + this.nickName + "\"\r\n");
        sb.append("real_name:\"" + this.realName + "\"\r\n");
        sb.append("entity_class:\"" + this.entityClass + "\"\r\n");
        sb.append("entity_id:\"" + this.entityId + "\"\r\n");
        sb.append("entity_name:\"" + this.entityName + "\"\r\n");
        sb.append("entity_data:\"" + this.entityData + "\"\r\n");
        sb.append("entity_update_info:\"" + this.entityUpdateInfo + "\"\r\n");
        sb.append("remark:\"" + this.remark + "\"\r\n");
        sb.append("user_ip:\"" + this.userIp + "\"\r\n");
        sb.append("create_date:\"" + this.createDate + "\"\r\n");
        return sb.toString();
    }

}