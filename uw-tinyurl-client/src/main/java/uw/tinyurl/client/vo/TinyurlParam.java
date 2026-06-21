package uw.tinyurl.client.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Date;

/**
 * 短链接生成参数。
 * <p>
 * 描述一条短链接的归属、原始 URL、密语保护与过期时间等属性。推荐使用 Builder 模式构造：
 * <pre>{@code
 * TinyurlParam param = TinyurlParam.builder()
 *     .saasId(1001L)
 *     .objectType("LINK")
 *     .url("https://example.com/very/long/url")
 *     .build();
 * }</pre>
 *
 * @author axeon
 */
@Schema(title = "短链数据", description = "短链数据")
public class TinyurlParam implements Serializable {


    /**
     * 运营商 ID。
     */
    @Schema(title = "saasId", description = "saasId")
    private long saasId;

    /**
     * 对象类型，用于按业务分类统计短链接（如 {@code "LINK"}、{@code "SECRET_LINK"}）。
     */
    @Schema(title = "对象类型", description = "对象类型")
    private String objectType;

    /**
     * 对象 ID，用于与业务对象关联。
     */
    @Schema(title = "对象Id", description = "对象Id")
    private long objectId;

    /**
     * 原始长 URL（必填）。
     */
    @Schema(title = "url", description = "url")
    private String url;

    /**
     * 密语提示，访问短链时展示给用户的引导文案（如"请输入访问密码"）。
     */
    @Schema(title = "密语提示？", description = "密语提示？")
    private String secretTips;

    /**
     * 密语，访问短链时需用户输入以通过校验。
     */
    @Schema(title = "密语", description = "密语")
    private String secretData;

    /**
     * 过期时间，过期后短链不可访问；为 {@code null} 表示永不过期。
     */
    @Schema(title = "过期时间", description = "过期时间")
    private Date expireDate;

    /**
     * 默认构造方法（用于反序列化）。
     */
    public TinyurlParam() {
    }

    /**
     * 通过 Builder 构造参数对象。
     *
     * @param builder 构造器
     */
    private TinyurlParam(Builder builder) {
        setSaasId(builder.saasId);
        setObjectType(builder.objectType);
        setObjectId(builder.objectId);
        setUrl(builder.url);
        setSecretTips(builder.secretTips);
        setSecretData(builder.secretData);
        setExpireDate(builder.expireDate);
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
    public static Builder builder(TinyurlParam copy) {
        Builder builder = new Builder();
        builder.saasId = copy.getSaasId();
        builder.objectType = copy.getObjectType();
        builder.objectId = copy.getObjectId();
        builder.url = copy.getUrl();
        builder.secretTips = copy.getSecretTips();
        builder.secretData = copy.getSecretData();
        builder.expireDate = copy.getExpireDate();
        return builder;
    }

    public long getSaasId() {
        return saasId;
    }

    public void setSaasId(long saasId) {
        this.saasId = saasId;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public long getObjectId() {
        return objectId;
    }

    public void setObjectId(long objectId) {
        this.objectId = objectId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSecretTips() {
        return secretTips;
    }

    public void setSecretTips(String secretTips) {
        this.secretTips = secretTips;
    }

    public String getSecretData() {
        return secretData;
    }

    public void setSecretData(String secretData) {
        this.secretData = secretData;
    }

    public Date getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(Date expireDate) {
        this.expireDate = expireDate;
    }

    /**
     * {@link TinyurlParam} 的构造器，支持链式调用。
     */
    public static final class Builder {
        private long saasId;
        private String objectType;
        private long objectId;
        private String url;
        private String secretTips;
        private String secretData;
        private Date expireDate;

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
         * 设置对象类型。
         *
         * @param objectType 对象类型
         * @return 当前 Builder
         */
        public Builder objectType(String objectType) {
            this.objectType = objectType;
            return this;
        }

        /**
         * 设置对象 ID。
         *
         * @param objectId 对象 ID
         * @return 当前 Builder
         */
        public Builder objectId(long objectId) {
            this.objectId = objectId;
            return this;
        }

        /**
         * 设置原始长 URL（必填）。
         *
         * @param url 原始长 URL
         * @return 当前 Builder
         */
        public Builder url(String url) {
            this.url = url;
            return this;
        }

        /**
         * 设置密语提示。
         *
         * @param secretTips 密语提示
         * @return 当前 Builder
         */
        public Builder secretTips(String secretTips) {
            this.secretTips = secretTips;
            return this;
        }

        /**
         * 设置密语。
         *
         * @param secretData 密语
         * @return 当前 Builder
         */
        public Builder secretData(String secretData) {
            this.secretData = secretData;
            return this;
        }

        /**
         * 设置过期时间。
         *
         * @param expireDate 过期时间
         * @return 当前 Builder
         */
        public Builder expireDate(Date expireDate) {
            this.expireDate = expireDate;
            return this;
        }

        /**
         * 构建 {@link TinyurlParam} 实例。
         *
         * @return 新的参数对象
         */
        public TinyurlParam build() {
            return new TinyurlParam(this);
        }
    }
}
