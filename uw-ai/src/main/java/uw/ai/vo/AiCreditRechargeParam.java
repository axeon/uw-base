package uw.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI积点 充值参数（admin/RPC 给 saas 充值）。
 */
@Schema(title = "AI积点充值参数", description = "AI积点充值参数")
public class AiCreditRechargeParam {

    /**
     * 目标租户saasId
     */
    @Schema(title = "目标租户saasId", description = "目标租户saasId")
    private long saasId;

    /**
     * 充值毫积点(负数=回收)。HTTP 充值申请入口接收毫积点；
     */
    @Schema(title = "充值毫积点", description = "RPC入口单位为毫积点；负数表示回收")
    private long credits;

    /**
     * 备注
     */
    @Schema(title = "备注", description = "备注")
    private String remark;

    public long getSaasId() {
        return saasId;
    }

    public void setSaasId(long saasId) {
        this.saasId = saasId;
    }

    public long getCredits() {
        return credits;
    }

    public void setCredits(long credits) {
        this.credits = credits;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

}
