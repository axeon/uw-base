package uw.dao.impl;

import io.swagger.v3.oas.annotations.media.Schema;
import uw.dao.PageQueryParam;
import uw.dao.annotation.QueryMeta;

import java.util.Date;
import java.util.List;

/**
 * MSC应用查询参数。
 */
@Schema(title = "MSC应用查询参数", description = "MSC应用查询参数")
public class TestQueryParam extends PageQueryParam {

    /**
     * 主键
     */
    @QueryMeta(expr = "id=?")
    @Schema(title = "主键", description = "主键")
    private Long id;

    /**
     * 应用名称
     */
    @QueryMeta(expr = "app_name like ?")
    @Schema(title = "应用名称", description = "应用名称")
    private String appName;


    /**
     * 应用名称
     */
    @QueryMeta(expr = "(app_name like ? or app_desc like ?)")
    @Schema(title = "应用名称", description = "应用名称")
    private String appInfo;

    /**
     * 应用状态1: 上线; 0: 下线 -1:删除
     */
    @QueryMeta(expr = "state=?")
    @Schema(title = "应用状态1: 上线; 0: 下线 -1:删除", description = "应用状态1: 上线; 0: 下线 -1:删除")
    private Integer state;

    /**
     * 应用状态1: 上线; 0: 下线 -1:删除
     */
    @QueryMeta(expr = "state in (?)")
    @Schema(title = "应用状态1: 上线; 0: 下线 -1:删除", description = "应用状态1: 上线; 0: 下线 -1:删除")
    private Integer[] states;

    /**
     * 应用状态1: 上线; 0: 下线 -1:删除
     */
    @QueryMeta(expr = "stateList in (?)")
    @Schema(title = "应用状态1: 上线; 0: 下线 -1:删除", description = "应用状态1: 上线; 0: 下线 -1:删除")
    private List<Integer> stateList;

    /**
     * 应用状态1: 上线; 0: 下线 -1:删除
     */
    @QueryMeta(expr = "create_date between ? and ?")
    @Schema(title = "应用状态1: 上线; 0: 下线 -1:删除", description = "应用状态1: 上线; 0: 下线 -1:删除")
    private Date[] createDate;


    /**
     * 只要传值，将会启用expr。
     */
    @QueryMeta(expr = "state>=0")
    @Schema(title = "应用状态1: 上线; 0: 下线 -1:删除", description = "应用状态1: 上线; 0: 下线 -1:删除")
    private Boolean stateOn;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppInfo() {
        return appInfo;
    }

    public void setAppInfo(String appInfo) {
        this.appInfo = appInfo;
    }

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public Integer[] getStates() {
        return states;
    }

    public void setStates(Integer[] states) {
        this.states = states;
    }

    public List<Integer> getStateList() {
        return stateList;
    }

    public void setStateList(List<Integer> stateList) {
        this.stateList = stateList;
    }

    public Date[] getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date[] createDate) {
        this.createDate = createDate;
    }

    public Boolean getStateOn() {
        return stateOn;
    }

    public void setStateOn(Boolean stateOn) {
        this.stateOn = stateOn;
    }
}