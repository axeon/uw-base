package uw.gateway.client.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Date;

/**
 * 运营商限速策略设置参数。
 * <p>
 * 描述一条针对运营商（SAAS）的网关限速规则。限速维度由 {@link #limitType} 决定
 * （对应服务端 {@code uw.gateway.center.acl.rate.constant.MscAclRateLimitType} 的 value：
 * -1 不限速 / 0 IP / 1 SAAS_LEVEL / 2 USER_TYPE / 3 USER_ID / 11 SAAS_LEVEL_URI / 12 USER_TYPE_URI / 13 USER_ID_URI），
 * {@link #userType} 与 {@link #userId} 仅在按用户维度限速时使用。推荐使用 Builder 模式构造：
 * <pre>{@code
 * SaasRateLimitParam param = SaasRateLimitParam.builder()
 *     .saasId(1001L)
 *     .limitType(0)   // 0=IP限速，裸值与服务端 MscAclRateLimitType.value 对应
 *     .limitSeconds(10)
 *     .limitRequests(1000)
 *     .expireDate(expireDate)
 *     .remark("RPC更新配置。")
 *     .build();
 * }</pre>
 *
 * @author axeon
 */
@Schema(title = "运营商限速策略参数", description = "运营商限速策略参数")
public class SaasRateLimitParam implements Serializable {

    /**
     * 运营商 ID（必填，须大于 0）。
     */
    @Schema(title = "saasId", description = "运营商ID，须大于0")
    private long saasId;

    /**
     * 限速类型（对应 MscAclRateLimitType）。
     */
    @Schema(title = "限速类型", description = "限速类型(MscAclRateLimitType: -1不限速/0 IP/1 SAAS_LEVEL/2 USER_TYPE/3 USER_ID/11 SAAS_LEVEL_URI/12 USER_TYPE_URI/13 USER_ID_URI)")
    private int limitType;

    /**
     * 用户类型，仅按用户类型/用户ID维度限速时使用，其余场景传 -1。
     */
    @Schema(title = "用户类型", description = "用户类型，按用户维度限速时使用，否则-1")
    private int userType = -1;

    /**
     * 用户 ID，仅按用户ID维度限速时使用，其余场景传 -1。
     */
    @Schema(title = "用户ID", description = "用户ID，按用户ID维度限速时使用，否则-1")
    private long userId = -1L;

    /**
     * 限速统计窗口（秒）。
     */
    @Schema(title = "限速秒数", description = "限速统计窗口（秒）")
    private int limitSeconds;

    /**
     * 窗口内最大请求数。
     */
    @Schema(title = "限速请求数", description = "窗口内最大请求数")
    private int limitRequests;

    /**
     * 窗口内最大字节数。
     */
    @Schema(title = "限速字节数", description = "窗口内最大字节数")
    private int limitBytes;

    /**
     * 限速策略过期时间（必填）。
     */
    @Schema(title = "过期时间", description = "限速策略过期时间，必填")
    private Date expireDate;

    /**
     * 备注信息（必填）。
     */
    @Schema(title = "备注", description = "备注信息，必填")
    private String remark;

    /**
     * 默认构造方法（用于反序列化）。
     */
    public SaasRateLimitParam() {
    }

    /**
     * 通过 Builder 构造参数对象。
     *
     * @param builder 构造器
     */
    private SaasRateLimitParam(Builder builder) {
        this.saasId = builder.saasId;
        this.limitType = builder.limitType;
        this.userType = builder.userType;
        this.userId = builder.userId;
        this.limitSeconds = builder.limitSeconds;
        this.limitRequests = builder.limitRequests;
        this.limitBytes = builder.limitBytes;
        this.expireDate = builder.expireDate;
        this.remark = builder.remark;
    }

    /**
     * 创建一个新的 Builder。
     *
     * @return 空白 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 基于已有参数对象创建 Builder，用于拷贝修改。
     *
     * @param copy 源参数对象
     * @return 已载入源对象字段的 Builder
     */
    public static Builder builder(SaasRateLimitParam copy) {
        Builder builder = new Builder();
        builder.saasId = copy.saasId;
        builder.limitType = copy.limitType;
        builder.userType = copy.userType;
        builder.userId = copy.userId;
        builder.limitSeconds = copy.limitSeconds;
        builder.limitRequests = copy.limitRequests;
        builder.limitBytes = copy.limitBytes;
        builder.expireDate = copy.expireDate;
        builder.remark = copy.remark;
        return builder;
    }

    public long getSaasId() {
        return saasId;
    }

    public void setSaasId(long saasId) {
        this.saasId = saasId;
    }

    public int getLimitType() {
        return limitType;
    }

    public void setLimitType(int limitType) {
        this.limitType = limitType;
    }

    public int getUserType() {
        return userType;
    }

    public void setUserType(int userType) {
        this.userType = userType;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public int getLimitSeconds() {
        return limitSeconds;
    }

    public void setLimitSeconds(int limitSeconds) {
        this.limitSeconds = limitSeconds;
    }

    public int getLimitRequests() {
        return limitRequests;
    }

    public void setLimitRequests(int limitRequests) {
        this.limitRequests = limitRequests;
    }

    public int getLimitBytes() {
        return limitBytes;
    }

    public void setLimitBytes(int limitBytes) {
        this.limitBytes = limitBytes;
    }

    public Date getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(Date expireDate) {
        this.expireDate = expireDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * {@link SaasRateLimitParam} 的构造器，支持链式调用。
     */
    public static final class Builder {
        private long saasId;
        private int limitType;
        private int userType = -1;
        private long userId = -1L;
        private int limitSeconds;
        private int limitRequests;
        private int limitBytes;
        private Date expireDate;
        private String remark;

        private Builder() {
        }

        /**
         * 设置运营商 ID。
         *
         * @param saasId 运营商 ID
         * @return 当前 Builder
         */
        public Builder saasId(long saasId) {
            this.saasId = saasId;
            return this;
        }

        /**
         * 设置限速类型。
         *
         * @param limitType 限速类型（对应 MscAclRateLimitType）
         * @return 当前 Builder
         */
        public Builder limitType(int limitType) {
            this.limitType = limitType;
            return this;
        }

        /**
         * 设置用户类型（按用户维度限速时使用）。
         *
         * @param userType 用户类型，否则 -1
         * @return 当前 Builder
         */
        public Builder userType(int userType) {
            this.userType = userType;
            return this;
        }

        /**
         * 设置用户 ID（按用户ID维度限速时使用）。
         *
         * @param userId 用户 ID，否则 -1
         * @return 当前 Builder
         */
        public Builder userId(long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * 设置限速统计窗口（秒）。
         *
         * @param limitSeconds 限速统计窗口（秒）
         * @return 当前 Builder
         */
        public Builder limitSeconds(int limitSeconds) {
            this.limitSeconds = limitSeconds;
            return this;
        }

        /**
         * 设置窗口内最大请求数。
         *
         * @param limitRequests 窗口内最大请求数
         * @return 当前 Builder
         */
        public Builder limitRequests(int limitRequests) {
            this.limitRequests = limitRequests;
            return this;
        }

        /**
         * 设置窗口内最大字节数。
         *
         * @param limitBytes 窗口内最大字节数
         * @return 当前 Builder
         */
        public Builder limitBytes(int limitBytes) {
            this.limitBytes = limitBytes;
            return this;
        }

        /**
         * 设置限速策略过期时间。
         *
         * @param expireDate 过期时间
         * @return 当前 Builder
         */
        public Builder expireDate(Date expireDate) {
            this.expireDate = expireDate;
            return this;
        }

        /**
         * 设置备注信息。
         *
         * @param remark 备注信息
         * @return 当前 Builder
         */
        public Builder remark(String remark) {
            this.remark = remark;
            return this;
        }

        /**
         * 构造参数对象。
         *
         * @return 参数对象
         */
        public SaasRateLimitParam build() {
            return new SaasRateLimitParam(this);
        }
    }
}
