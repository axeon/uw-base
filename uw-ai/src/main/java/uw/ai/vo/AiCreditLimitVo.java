package uw.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI积点 员工限额数据。
 */
@Schema(title = "AI积点员工限额数据", description = "AI积点员工限额数据")
public class AiCreditLimitVo {

    /**
     * saasId
     */
    @Schema(title = "saasId", description = "saasId")
    private long saasId;

    /**
     * 员工userId
     */
    @Schema(title = "员工userId", description = "员工userId")
    private long userId;

    /**
     * 周期限额积点
     */
    @Schema(title = "周期限额积点", description = "周期限额积点(计费口径)")
    private long creditsLimit;

    /**
     * 当前周期内已用积点
     */
    @Schema(title = "当前周期内已用积点", description = "当前周期内已用积点(计费口径,周期已过时为重置后口径)")
    private long creditsUsed;

    /**
     * 周期类型:0=不限 1=日 2=月
     */
    @Schema(title = "周期类型", description = "周期类型:0=不限 1=日 2=月")
    private int cycleType;

    /**
     * 状态:1=启用 0=停用(预检拦截,暂停该员工AI使用)
     */
    @Schema(title = "状态", description = "状态:1=启用 0=停用(预检拦截,暂停该员工AI使用)")
    private int state;

    public long getSaasId() {
        return saasId;
    }

    public void setSaasId(long saasId) {
        this.saasId = saasId;
    }

    /**
     * 用户类型
     */
    @Schema(title = "用户类型", description = "用户类型")
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

    public long getCreditsUsed() {
        return creditsUsed;
    }

    public void setCreditsUsed(long creditsUsed) {
        this.creditsUsed = creditsUsed;
    }

    public int getCycleType() {
        return cycleType;
    }

    public void setCycleType(int cycleType) {
        this.cycleType = cycleType;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

}
