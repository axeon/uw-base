package uw.auth.service.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 限速目标。
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@Schema(title = "限速目标", description = "限速目标")
public enum RateLimitTarget {

    /**
     * 不限速
     */
    NONE(-1, "不限速"),

    /**
     * 以IP限速。
     */
    IP(0, "IP限速"),

    /**
     * 用户ID限速
     */
    USER(1, "用户限速"),

    /**
     * MCH ID限速。
     */
    MCH(2, "商户限速"),

    /**
     * SAAS限速。
     */
    SAAS(5, "SAAS限速"),

    /**
     * 用户和资源限速。
     */
    USER_URI(11, "用户资源限速"),

    /**
     * MCH 和资源限速。
     */
    MCH_URI(12, "商户资源限速"),

    /**
     * SAAS 和资源限速。
     */
    SAAS_URI(15, "SAAS资源限速");

    /**
     * 数值。
     */
    private int value;

    /**
     * 标签。
     */
    private String label;

    RateLimitTarget(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }


    /**
     * 根据value获得enum。
     *
     * @param value
     * @return
     */
    public static RateLimitTarget findByValue(int value) {
        for (RateLimitTarget target : RateLimitTarget.values()) {
            if (value == target.value) {
                return target;
            }
        }
        return NONE;
    }
}
