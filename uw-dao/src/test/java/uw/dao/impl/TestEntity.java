package uw.dao.impl;

import io.swagger.v3.oas.annotations.media.Schema;
import uw.dao.DataEntity;
import uw.dao.annotation.ColumnMeta;
import uw.dao.annotation.TableMeta;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * MscApp实体类
 * MSC应用
 *
 * @author axeon
 */
@TableMeta(tableName="msc_app",tableType="table")
@Schema(title = "MSC应用", description = "MSC应用")
public class TestEntity implements DataEntity,Serializable{


	/**
	 * 主键
	 */
	@ColumnMeta(columnName="id", dataType="long", dataSize=19, nullable=false, primaryKey=true)
	@Schema(title = "主键", description = "主键")
	private long id;

	/**
	 * 应用名称
	 */
	@ColumnMeta(columnName="app_name", dataType="String", dataSize=255, nullable=false)
	@Schema(title = "应用名称", description = "应用名称")
	private String appName;

	/**
	 * 应用显示名称
	 */
	@ColumnMeta(columnName="app_label", dataType="String", dataSize=255, nullable=true)
	@Schema(title = "应用显示名称", description = "应用显示名称")
	private String appLabel;

	/**
	 * 应用版本
	 */
	@ColumnMeta(columnName="app_version", dataType="String", dataSize=255, nullable=true)
	@Schema(title = "应用版本", description = "应用版本")
	private String appVersion;

	/**
	 * 应用显示顺序[ASC]
	 */
	@ColumnMeta(columnName="app_rank", dataType="int", dataSize=10, nullable=false)
	@Schema(title = "应用显示顺序[ASC]", description = "应用显示顺序[ASC]")
	private int appRank;

	/**
	 * 应用app接收用户登录,注册信息回调
	 */
	@ColumnMeta(columnName="callback_url", dataType="String", dataSize=500, nullable=true)
	@Schema(title = "应用app接收用户登录,注册信息回调", description = "应用app接收用户登录,注册信息回调")
	private String callbackUrl;

	/**
	 * 用户成功登录重定向地址
	 */
	@ColumnMeta(columnName="redirect_url", dataType="String", dataSize=500, nullable=true)
	@Schema(title = "用户成功登录重定向地址", description = "用户成功登录重定向地址")
	private String redirectUrl;

	/**
	 * 备注
	 */
	@ColumnMeta(columnName="remark", dataType="String", dataSize=255, nullable=true)
	@Schema(title = "备注", description = "备注")
	private String remark;

	/**
	 * 创建时间
	 */
	@ColumnMeta(columnName="create_date", dataType="java.util.Date", dataSize=23, nullable=true)
	@Schema(title = "创建时间", description = "创建时间")
	private java.util.Date createDate;

	/**
	 * 修改时间
	 */
	@ColumnMeta(columnName="modify_date", dataType="java.util.Date", dataSize=23, nullable=true)
	@Schema(title = "修改时间", description = "修改时间")
	private java.util.Date modifyDate;

	/**
	 * 最后更新时间
	 */
	@ColumnMeta(columnName="last_update", dataType="java.util.Date", dataSize=23, nullable=false)
	@Schema(title = "最后更新时间", description = "最后更新时间")
	private java.util.Date lastUpdate;

	/**
	 * 应用状态1: 上线; 0: 下线 -1:删除
	 */
	@ColumnMeta(columnName="state", dataType="int", dataSize=10, nullable=false)
	@Schema(title = "应用状态1: 上线; 0: 下线 -1:删除", description = "应用状态1: 上线; 0: 下线 -1:删除")
	private int state;

	/**
	 * 是否显示
	 */
	@ColumnMeta(columnName="display_state", dataType="int", dataSize=10, nullable=true)
	@Schema(title = "是否显示", description = "是否显示")
	private int displayState;

	/**
	 * 轻量级状态下更新列表list.
	 */
	public transient Set<String> UPDATED_COLUMN = null;

    /**
	 * 更新的信息.
	 */
    private transient StringBuilder UPDATED_INFO = null;

	/**
	 * 获取实体的表名。
	 *
	 * @return 表名
	 */
	@Override
	public String ENTITY_TABLE() {
		return "msc_app";
	}

	/**
	 * 获取实体的名称。
	 *
	 * @return 实体名称
	 */
	@Override
	public String ENTITY_NAME() {
		return "mscApp";
	}

	/**
	 * 获取实体的ID。
	 *
	 * @return 表名
	 */
	@Override
	public Serializable ENTITY_ID() {
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
     * 得到_INFO.
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
     * 清理_INFO和UPDATED_COLUMN信息.
     */
    public void CLEAR_UPDATED_INFO() {
        UPDATED_COLUMN = null;
        UPDATED_INFO = null;
	}

	/**
	 * 初始化set相关的信息.
	 */
	private void _INIT_UPDATE_INFO() {
		this.UPDATED_COLUMN = new HashSet<String>();
		this.UPDATED_INFO = new StringBuilder("表msc_app主键\"" + 
		this.id+ 
		this.id+ "\"更新为:\r\n");
	}


	/**
	 * 获取主键。
	 */
	public long getId(){
		return this.id;
	}

	/**
	 * 获取应用名称。
	 */
	public String getAppName(){
		return this.appName;
	}

	/**
	 * 获取应用显示名称。
	 */
	public String getAppLabel(){
		return this.appLabel;
	}

	/**
	 * 获取应用版本。
	 */
	public String getAppVersion(){
		return this.appVersion;
	}

	/**
	 * 获取应用显示顺序[ASC]。
	 */
	public int getAppRank(){
		return this.appRank;
	}

	/**
	 * 获取应用app接收用户登录,注册信息回调。
	 */
	public String getCallbackUrl(){
		return this.callbackUrl;
	}

	/**
	 * 获取用户成功登录重定向地址。
	 */
	public String getRedirectUrl(){
		return this.redirectUrl;
	}

	/**
	 * 获取备注。
	 */
	public String getRemark(){
		return this.remark;
	}

	/**
	 * 获取创建时间。
	 */
	public java.util.Date getCreateDate(){
		return this.createDate;
	}

	/**
	 * 获取修改时间。
	 */
	public java.util.Date getModifyDate(){
		return this.modifyDate;
	}

	/**
	 * 获取最后更新时间。
	 */
	public java.util.Date getLastUpdate(){
		return this.lastUpdate;
	}

	/**
	 * 获取应用状态1: 上线; 0: 下线 -1:删除。
	 */
	public int getState(){
		return this.state;
	}

	/**
	 * 获取是否显示。
	 */
	public int getDisplayState(){
		return this.displayState;
	}


	/**
	 * 设置主键。
	 */
	public void setId(long id){
		if ((!String.valueOf(this.id).equals(String.valueOf(id)))) {
			if (this.UPDATED_COLUMN == null) {
				_INIT_UPDATE_INFO();
			}
			this.UPDATED_COLUMN.add("id");
			this.UPDATED_INFO.append("id:\"" + this.id+ "\"=>\""
                + id + "\"\r\n");
			this.id = id;
		}
	}

	/**
	 * 设置应用名称。
	 */
	public void setAppName(String appName){
		if ((!String.valueOf(this.appName).equals(String.valueOf(appName)))) {
			if (this.UPDATED_COLUMN == null) {
				_INIT_UPDATE_INFO();
			}
			this.UPDATED_COLUMN.add("app_name");
			this.UPDATED_INFO.append("app_name:\"" + this.appName+ "\"=>\""
                + appName + "\"\r\n");
			this.appName = appName;
		}
	}

	/**
	 * 设置应用显示名称。
	 */
	public void setAppLabel(String appLabel){
		if ((!String.valueOf(this.appLabel).equals(String.valueOf(appLabel)))) {
			if (this.UPDATED_COLUMN == null) {
				_INIT_UPDATE_INFO();
			}
			this.UPDATED_COLUMN.add("app_label");
			this.UPDATED_INFO.append("app_label:\"" + this.appLabel+ "\"=>\""
                + appLabel + "\"\r\n");
			this.appLabel = appLabel;
		}
	}

	/**
	 * 设置应用版本。
	 */
	public void setAppVersion(String appVersion){
		if ((!String.valueOf(this.appVersion).equals(String.valueOf(appVersion)))) {
			if (this.UPDATED_COLUMN == null) {
				_INIT_UPDATE_INFO();
			}
			this.UPDATED_COLUMN.add("app_version");
			this.UPDATED_INFO.append("app_version:\"" + this.appVersion+ "\"=>\""
                + appVersion + "\"\r\n");
			this.appVersion = appVersion;
		}
	}

	/**
	 * 设置应用显示顺序[ASC]。
	 */
	public void setAppRank(int appRank){
		if ((!String.valueOf(this.appRank).equals(String.valueOf(appRank)))) {
			if (this.UPDATED_COLUMN == null) {
				_INIT_UPDATE_INFO();
			}
			this.UPDATED_COLUMN.add("app_rank");
			this.UPDATED_INFO.append("app_rank:\"" + this.appRank+ "\"=>\""
                + appRank + "\"\r\n");
			this.appRank = appRank;
		}
	}

	/**
	 * 设置应用app接收用户登录,注册信息回调。
	 */
	public void setCallbackUrl(String callbackUrl){
		if ((!String.valueOf(this.callbackUrl).equals(String.valueOf(callbackUrl)))) {
			if (this.UPDATED_COLUMN == null) {
				_INIT_UPDATE_INFO();
			}
			this.UPDATED_COLUMN.add("callback_url");
			this.UPDATED_INFO.append("callback_url:\"" + this.callbackUrl+ "\"=>\""
                + callbackUrl + "\"\r\n");
			this.callbackUrl = callbackUrl;
		}
	}

	/**
	 * 设置用户成功登录重定向地址。
	 */
	public void setRedirectUrl(String redirectUrl){
		if ((!String.valueOf(this.redirectUrl).equals(String.valueOf(redirectUrl)))) {
			if (this.UPDATED_COLUMN == null) {
				_INIT_UPDATE_INFO();
			}
			this.UPDATED_COLUMN.add("redirect_url");
			this.UPDATED_INFO.append("redirect_url:\"" + this.redirectUrl+ "\"=>\""
                + redirectUrl + "\"\r\n");
			this.redirectUrl = redirectUrl;
		}
	}

	/**
	 * 设置备注。
	 */
	public void setRemark(String remark){
		if ((!String.valueOf(this.remark).equals(String.valueOf(remark)))) {
			if (this.UPDATED_COLUMN == null) {
				_INIT_UPDATE_INFO();
			}
			this.UPDATED_COLUMN.add("remark");
			this.UPDATED_INFO.append("remark:\"" + this.remark+ "\"=>\""
                + remark + "\"\r\n");
			this.remark = remark;
		}
	}

	/**
	 * 设置创建时间。
	 */
	public void setCreateDate(java.util.Date createDate){
		if ((!String.valueOf(this.createDate).equals(String.valueOf(createDate)))) {
			if (this.UPDATED_COLUMN == null) {
				_INIT_UPDATE_INFO();
			}
			this.UPDATED_COLUMN.add("create_date");
			this.UPDATED_INFO.append("create_date:\"" + this.createDate+ "\"=>\""
                + createDate + "\"\r\n");
			this.createDate = createDate;
		}
	}

	/**
	 * 设置修改时间。
	 */
	public void setModifyDate(java.util.Date modifyDate){
		if ((!String.valueOf(this.modifyDate).equals(String.valueOf(modifyDate)))) {
			if (this.UPDATED_COLUMN == null) {
				_INIT_UPDATE_INFO();
			}
			this.UPDATED_COLUMN.add("modify_date");
			this.UPDATED_INFO.append("modify_date:\"" + this.modifyDate+ "\"=>\""
                + modifyDate + "\"\r\n");
			this.modifyDate = modifyDate;
		}
	}

	/**
	 * 设置最后更新时间。
	 */
	public void setLastUpdate(java.util.Date lastUpdate){
		if ((!String.valueOf(this.lastUpdate).equals(String.valueOf(lastUpdate)))) {
			if (this.UPDATED_COLUMN == null) {
				_INIT_UPDATE_INFO();
			}
			this.UPDATED_COLUMN.add("last_update");
			this.UPDATED_INFO.append("last_update:\"" + this.lastUpdate+ "\"=>\""
                + lastUpdate + "\"\r\n");
			this.lastUpdate = lastUpdate;
		}
	}

	/**
	 * 设置应用状态1: 上线; 0: 下线 -1:删除。
	 */
	public void setState(int state){
		if ((!String.valueOf(this.state).equals(String.valueOf(state)))) {
			if (this.UPDATED_COLUMN == null) {
				_INIT_UPDATE_INFO();
			}
			this.UPDATED_COLUMN.add("state");
			this.UPDATED_INFO.append("state:\"" + this.state+ "\"=>\""
                + state + "\"\r\n");
			this.state = state;
		}
	}

	/**
	 * 设置是否显示。
	 */
	public void setDisplayState(int displayState){
		if ((!String.valueOf(this.displayState).equals(String.valueOf(displayState)))) {
			if (this.UPDATED_COLUMN == null) {
				_INIT_UPDATE_INFO();
			}
			this.UPDATED_COLUMN.add("display_state");
			this.UPDATED_INFO.append("display_state:\"" + this.displayState+ "\"=>\""
                + displayState + "\"\r\n");
			this.displayState = displayState;
		}
	}

	/**
	 * 重载toString方法.
	 */
    @Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("id:\"" + this.id + "\"\r\n");
		sb.append("app_name:\"" + this.appName + "\"\r\n");
		sb.append("app_label:\"" + this.appLabel + "\"\r\n");
		sb.append("app_version:\"" + this.appVersion + "\"\r\n");
		sb.append("app_rank:\"" + this.appRank + "\"\r\n");
		sb.append("callback_url:\"" + this.callbackUrl + "\"\r\n");
		sb.append("redirect_url:\"" + this.redirectUrl + "\"\r\n");
		sb.append("remark:\"" + this.remark + "\"\r\n");
		sb.append("create_date:\"" + this.createDate + "\"\r\n");
		sb.append("modify_date:\"" + this.modifyDate + "\"\r\n");
		sb.append("last_update:\"" + this.lastUpdate + "\"\r\n");
		sb.append("state:\"" + this.state + "\"\r\n");
		sb.append("display_state:\"" + this.displayState + "\"\r\n");
		return sb.toString();
	}

}
