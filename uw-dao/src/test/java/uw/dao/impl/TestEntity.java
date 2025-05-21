package uw.dao.impl;

import io.swagger.v3.oas.annotations.media.Schema;
import uw.common.util.JsonUtils;
import uw.dao.DataEntity;
import uw.dao.DataUpdateInfo;
import uw.dao.annotation.ColumnMeta;
import uw.dao.annotation.TableMeta;

import java.io.Serializable;


/**
 * MscApp实体类
 * MSC应用
 *
 * @author axeon
 */
@TableMeta(tableName = "msc_app", tableType = "table")
@Schema(title = "MSC应用", description = "MSC应用")
public class TestEntity implements DataEntity, Serializable {


    /**
     * 主键
     */
    @ColumnMeta(columnName = "id", dataType = "long", dataSize = 19, nullable = false, primaryKey = true)
    @Schema(title = "主键", description = "主键", maxLength = 19, nullable = false)
    private long id;

    /**
     * 应用名称
     */
    @ColumnMeta(columnName = "app_name", dataType = "String", dataSize = 255, nullable = false)
    @Schema(title = "应用名称", description = "应用名称", maxLength = 255, nullable = false)
    private String appName;

    /**
     * 应用显示名称
     */
    @ColumnMeta(columnName = "app_label", dataType = "String", dataSize = 255, nullable = true)
    @Schema(title = "应用显示名称", description = "应用显示名称", maxLength = 255, nullable = true)
    private String appLabel;

    /**
     * 应用版本
     */
    @ColumnMeta(columnName = "app_version", dataType = "String", dataSize = 255, nullable = true)
    @Schema(title = "应用版本", description = "应用版本", maxLength = 255, nullable = true)
    private String appVersion;

    /**
     * 应用显示顺序
     */
    @ColumnMeta(columnName = "app_rank", dataType = "int", dataSize = 10, nullable = false)
    @Schema(title = "应用显示顺序", description = "应用显示顺序", maxLength = 10, nullable = false)
    private int appRank;

    /**
     * 备注
     */
    @ColumnMeta(columnName = "remark", dataType = "String", dataSize = 255, nullable = true)
    @Schema(title = "备注", description = "备注", maxLength = 255, nullable = true)
    private String remark;

    /**
     * 权限数量
     */
    @ColumnMeta(columnName = "perm_num", dataType = "int", dataSize = 10, nullable = false)
    @Schema(title = "权限数量", description = "权限数量", maxLength = 10, nullable = false)
    private int permNum;

    /**
     * 运行主机数量
     */
    @ColumnMeta(columnName = "run_host", dataType = "int", dataSize = 10, nullable = true)
    @Schema(title = "运行主机数量", description = "运行主机数量", maxLength = 10, nullable = true)
    private int runHost;

    /**
     * 运行线程数
     */
    @ColumnMeta(columnName = "run_thread", dataType = "int", dataSize = 10, nullable = true)
    @Schema(title = "运行线程数", description = "运行线程数", maxLength = 10, nullable = true)
    private int runThread;

    /**
     * 运行内存数
     */
    @ColumnMeta(columnName = "run_mem", dataType = "long", dataSize = 19, nullable = true)
    @Schema(title = "运行内存数", description = "运行内存数", maxLength = 19, nullable = true)
    private long runMem;

    /**
     * 运行用户数
     */
    @ColumnMeta(columnName = "run_user", dataType = "int", dataSize = 10, nullable = true)
    @Schema(title = "运行用户数", description = "运行用户数", maxLength = 10, nullable = true)
    private int runUser;

    /**
     * 运行访问数
     */
    @ColumnMeta(columnName = "run_access", dataType = "long", dataSize = 19, nullable = true)
    @Schema(title = "运行访问数", description = "运行访问数", maxLength = 19, nullable = true)
    private long runAccess;

    /**
     * 创建时间
     */
    @ColumnMeta(columnName = "create_date", dataType = "java.util.Date", dataSize = 23, nullable = true)
    @Schema(title = "创建时间", description = "创建时间", maxLength = 23, nullable = true)
    private java.util.Date createDate;

    /**
     * 修改时间
     */
    @ColumnMeta(columnName = "modify_date", dataType = "java.util.Date", dataSize = 19, nullable = true)
    @Schema(title = "修改时间", description = "修改时间", maxLength = 19, nullable = true)
    private java.util.Date modifyDate;

    /**
     * 最后更新时间
     */
    @ColumnMeta(columnName = "last_update", dataType = "java.util.Date", dataSize = 23, nullable = false)
    @Schema(title = "最后更新时间", description = "最后更新时间", maxLength = 23, nullable = false)
    private java.util.Date lastUpdate;

    /**
     * 是否显示0不显示 1显示 2 VIP
     */
    @ColumnMeta(columnName = "display_state", dataType = "int", dataSize = 10, nullable = false)
    @Schema(title = "是否显示0不显示 1显示 2 VIP", description = "是否显示0不显示 1显示 2 VIP", maxLength = 10, nullable = false)
    private int displayState;

    /**
     * 应用状态1: 上线; 0: 下线 -1:删除
     */
    @ColumnMeta(columnName = "state", dataType = "int", dataSize = 10, nullable = false)
    @Schema(title = "应用状态1: 上线; 0: 下线 -1:删除", description = "应用状态1: 上线; 0: 下线 -1:删除", maxLength = 10, nullable = false)
    private int state;

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
        return "msc_app";
    }

    /**
     * 获得实体的表注释。
     */
    @Override
    public String ENTITY_NAME() {
        return "MSC应用";
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
     * 获取主键。
     */
    public long getId() {
        return this.id;
    }

    /**
     * 设置主键。
     */
    public void setId(long id) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "id", this.id, id, !_IS_LOADED);
        this.id = id;
    }

    /**
     * 获取应用名称。
     */
    public String getAppName() {
        return this.appName;
    }

    /**
     * 设置应用名称。
     */
    public void setAppName(String appName) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "appName", this.appName, appName, !_IS_LOADED);
        this.appName = appName;
    }

    /**
     * 获取应用显示名称。
     */
    public String getAppLabel() {
        return this.appLabel;
    }

    /**
     * 设置应用显示名称。
     */
    public void setAppLabel(String appLabel) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "appLabel", this.appLabel, appLabel, !_IS_LOADED);
        this.appLabel = appLabel;
    }

    /**
     * 获取应用版本。
     */
    public String getAppVersion() {
        return this.appVersion;
    }

    /**
     * 设置应用版本。
     */
    public void setAppVersion(String appVersion) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "appVersion", this.appVersion, appVersion, !_IS_LOADED);
        this.appVersion = appVersion;
    }

    /**
     * 获取应用显示顺序。
     */
    public int getAppRank() {
        return this.appRank;
    }

    /**
     * 设置应用显示顺序。
     */
    public void setAppRank(int appRank) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "appRank", this.appRank, appRank, !_IS_LOADED);
        this.appRank = appRank;
    }

    /**
     * 获取备注。
     */
    public String getRemark() {
        return this.remark;
    }

    /**
     * 设置备注。
     */
    public void setRemark(String remark) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "remark", this.remark, remark, !_IS_LOADED);
        this.remark = remark;
    }

    /**
     * 获取权限数量。
     */
    public int getPermNum() {
        return this.permNum;
    }

    /**
     * 设置权限数量。
     */
    public void setPermNum(int permNum) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "permNum", this.permNum, permNum, !_IS_LOADED);
        this.permNum = permNum;
    }

    /**
     * 获取运行主机数量。
     */
    public int getRunHost() {
        return this.runHost;
    }

    /**
     * 设置运行主机数量。
     */
    public void setRunHost(int runHost) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "runHost", this.runHost, runHost, !_IS_LOADED);
        this.runHost = runHost;
    }

    /**
     * 获取运行线程数。
     */
    public int getRunThread() {
        return this.runThread;
    }

    /**
     * 设置运行线程数。
     */
    public void setRunThread(int runThread) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "runThread", this.runThread, runThread, !_IS_LOADED);
        this.runThread = runThread;
    }

    /**
     * 获取运行内存数。
     */
    public long getRunMem() {
        return this.runMem;
    }

    /**
     * 设置运行内存数。
     */
    public void setRunMem(long runMem) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "runMem", this.runMem, runMem, !_IS_LOADED);
        this.runMem = runMem;
    }

    /**
     * 获取运行用户数。
     */
    public int getRunUser() {
        return this.runUser;
    }

    /**
     * 设置运行用户数。
     */
    public void setRunUser(int runUser) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "runUser", this.runUser, runUser, !_IS_LOADED);
        this.runUser = runUser;
    }

    /**
     * 获取运行访问数。
     */
    public long getRunAccess() {
        return this.runAccess;
    }

    /**
     * 设置运行访问数。
     */
    public void setRunAccess(long runAccess) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "runAccess", this.runAccess, runAccess, !_IS_LOADED);
        this.runAccess = runAccess;
    }

    /**
     * 获取创建时间。
     */
    public java.util.Date getCreateDate() {
        return this.createDate;
    }

    /**
     * 设置创建时间。
     */
    public void setCreateDate(java.util.Date createDate) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "createDate", this.createDate, createDate, !_IS_LOADED);
        this.createDate = createDate;
    }

    /**
     * 获取修改时间。
     */
    public java.util.Date getModifyDate() {
        return this.modifyDate;
    }

    /**
     * 设置修改时间。
     */
    public void setModifyDate(java.util.Date modifyDate) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "modifyDate", this.modifyDate, modifyDate, !_IS_LOADED);
        this.modifyDate = modifyDate;
    }

    /**
     * 获取最后更新时间。
     */
    public java.util.Date getLastUpdate() {
        return this.lastUpdate;
    }

    /**
     * 设置最后更新时间。
     */
    public void setLastUpdate(java.util.Date lastUpdate) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "lastUpdate", this.lastUpdate, lastUpdate, !_IS_LOADED);
        this.lastUpdate = lastUpdate;
    }

    /**
     * 获取是否显示0不显示 1显示 2 VIP。
     */
    public int getDisplayState() {
        return this.displayState;
    }

    /**
     * 设置是否显示0不显示 1显示 2 VIP。
     */
    public void setDisplayState(int displayState) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "displayState", this.displayState, displayState, !_IS_LOADED);
        this.displayState = displayState;
    }

    /**
     * 获取应用状态1: 上线; 0: 下线 -1:删除。
     */
    public int getState() {
        return this.state;
    }

    /**
     * 设置应用状态1: 上线; 0: 下线 -1:删除。
     */
    public void setState(int state) {
        DataUpdateInfo.addUpdateInfo(_UPDATED_INFO, "state", this.state, state, !_IS_LOADED);
        this.state = state;
    }

    /**
     * 设置主键链式调用。
     */
    public TestEntity id(long id) {
        setId(id);
        return this;
    }

    /**
     * 设置应用名称链式调用。
     */
    public TestEntity appName(String appName) {
        setAppName(appName);
        return this;
    }

    /**
     * 设置应用显示名称链式调用。
     */
    public TestEntity appLabel(String appLabel) {
        setAppLabel(appLabel);
        return this;
    }

    /**
     * 设置应用版本链式调用。
     */
    public TestEntity appVersion(String appVersion) {
        setAppVersion(appVersion);
        return this;
    }

    /**
     * 设置应用显示顺序链式调用。
     */
    public TestEntity appRank(int appRank) {
        setAppRank(appRank);
        return this;
    }

    /**
     * 设置备注链式调用。
     */
    public TestEntity remark(String remark) {
        setRemark(remark);
        return this;
    }

    /**
     * 设置权限数量链式调用。
     */
    public TestEntity permNum(int permNum) {
        setPermNum(permNum);
        return this;
    }

    /**
     * 设置运行主机数量链式调用。
     */
    public TestEntity runHost(int runHost) {
        setRunHost(runHost);
        return this;
    }

    /**
     * 设置运行线程数链式调用。
     */
    public TestEntity runThread(int runThread) {
        setRunThread(runThread);
        return this;
    }

    /**
     * 设置运行内存数链式调用。
     */
    public TestEntity runMem(long runMem) {
        setRunMem(runMem);
        return this;
    }

    /**
     * 设置运行用户数链式调用。
     */
    public TestEntity runUser(int runUser) {
        setRunUser(runUser);
        return this;
    }

    /**
     * 设置运行访问数链式调用。
     */
    public TestEntity runAccess(long runAccess) {
        setRunAccess(runAccess);
        return this;
    }

    /**
     * 设置创建时间链式调用。
     */
    public TestEntity createDate(java.util.Date createDate) {
        setCreateDate(createDate);
        return this;
    }

    /**
     * 设置修改时间链式调用。
     */
    public TestEntity modifyDate(java.util.Date modifyDate) {
        setModifyDate(modifyDate);
        return this;
    }

    /**
     * 设置最后更新时间链式调用。
     */
    public TestEntity lastUpdate(java.util.Date lastUpdate) {
        setLastUpdate(lastUpdate);
        return this;
    }

    /**
     * 设置是否显示0不显示 1显示 2 VIP链式调用。
     */
    public TestEntity displayState(int displayState) {
        setDisplayState(displayState);
        return this;
    }

    /**
     * 设置应用状态1: 上线; 0: 下线 -1:删除链式调用。
     */
    public TestEntity state(int state) {
        setState(state);
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