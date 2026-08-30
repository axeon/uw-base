package uw.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI积点 余额数据。
 */
@Schema(title = "AI积点余额数据", description = "AI积点余额数据")
public class AiCreditBalanceVo {

    /**
     * saasId
     */
    @Schema(title = "saasId", description = "saasId")
    private long saasId;

    /**
     * 累计充值(毫积点)
     */
    @Schema(title = "累计充值", description = "累计充值(毫积点,展示÷1000=积点)")
    private long creditsAll;

    /**
     * 累计消耗(毫积点)
     */
    @Schema(title = "累计消耗", description = "累计消耗(毫积点,展示÷1000=积点)")
    private long creditsUsed;

    /**
     * 剩余(=grant-used,毫积点,负数=欠费)
     */
    @Schema(title = "剩余", description = "剩余(=grant-used,毫积点,负数=欠费)")
    private long remaining;

    public long getSaasId() {
        return saasId;
    }

    public void setSaasId(long saasId) {
        this.saasId = saasId;
    }

    public long getCreditsAll() {
        return creditsAll;
    }

    public void setCreditsAll(long creditsAll) {
        this.creditsAll = creditsAll;
    }

    public long getCreditsUsed() {
        return creditsUsed;
    }

    public void setCreditsUsed(long creditsUsed) {
        this.creditsUsed = creditsUsed;
    }

    public long getRemaining() {
        return remaining;
    }

    public void setRemaining(long remaining) {
        this.remaining = remaining;
    }

}
