package uw.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI积点 员工限额设置参数（saas 设置员工用量）。
 */
@Schema(title = "AI积点员工限额设置参数", description = "AI积点员工限额设置参数")
public class AiCreditUserLimitParam {

    /**
     * 员工userId
     */
    @Schema(title = "员工userId", description = "员工userId")
    private long userId;

    /**
     * 周期限额(毫积点,前端已×1000;0=只统计不拦截)
     */
    @Schema(title = "周期限额", description = "周期限额(毫积点,0=只统计不拦截)")
    private long creditsLimit;

    /**
     * 周期类型:0=不限 1=日 2=月
     */
    @Schema(title = "周期类型", description = "周期类型:0=不限 1=日 2=月")
    private int cycleType;

    /**
     * 是否启用限额(false=停用该员工限额,预检拦截暂停其AI使用)
     */
    @Schema(title = "是否启用限额", description = "是否启用限额(false=停用该员工限额,预检拦截暂停其AI使用)")
    private boolean enable;

    /**
     * 备注
     */
    @Schema(title = "备注", description = "备注")
    private String remark;

    /**
     * 员工用户类型
     */
    @Schema(title = "员工用户类型", description = "员工用户类型")
    private int userType;

    public long getUserId() {
        return userId;
    }

    public int getUserType() {
        return userType;
    }

    public void setUserType(int userType) {
        this.userType = userType;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getCreditsLimit() {
        return creditsLimit;
    }

    public void setCreditsLimit(long creditsLimit) {
        this.creditsLimit = creditsLimit;
    }

    public int getCycleType() {
        return cycleType;
    }

    public void setCycleType(int cycleType) {
        this.cycleType = cycleType;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

}
