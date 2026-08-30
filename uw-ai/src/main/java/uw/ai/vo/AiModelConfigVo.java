package uw.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Date;

/**
 * AI模型配置VO — 供外部调用方查询可用模型列表。
 */
@Schema(title = "AI模型配置VO", description = "AI模型配置信息，供外部服务查询")
public class AiModelConfigVo {


    /**
     * ID
     */
    @Schema(title = "ID", description = "ID", maxLength=19, nullable=false )
    private long id;

    /**
     * SAAS ID
     */
    @Schema(title = "SAAS ID", description = "SAAS ID", maxLength=19, nullable=false )
    private long saasId;

    /**
     * 商户ID
     */
    @Schema(title = "商户ID", description = "商户ID", maxLength=19, nullable=false )
    private long mchId;

    /**
     * API配置ID
     */
    @Schema(title = "API配置ID", description = "API配置ID", maxLength=19, nullable=false )
    private long apiId;

    /**
     * 供应商类
     */
    @Schema(title = "供应商类", description = "供应商类", maxLength=200, nullable=false )
    private String vendorClass;

    /**
     * 模型类型: CHAT/EMBEDDING/RERANK/TTS/OCR
     */
    @Schema(title = "模型类型: CHAT/EMBEDDING/RERANK/TTS/OCR", description = "模型类型: CHAT/EMBEDDING/RERANK/TTS/OCR", maxLength=50, nullable=false )
    private String modelType;

    /**
     * 模型能力标签
     */
    @Schema(title = "模型能力标签", description = "模型能力标签", maxLength=200, nullable=true )
    private String modelTag;

    /**
     * 配置代码
     */
    @Schema(title = "配置代码", description = "配置代码", maxLength=100, nullable=true )
    private String configCode;

    /**
     * 配置名称
     */
    @Schema(title = "配置名称", description = "配置名称", maxLength=200, nullable=true )
    private String configName;

    /**
     * 配置描述
     */
    @Schema(title = "配置描述", description = "配置描述", maxLength=65535, nullable=true )
    private String configDesc;

    /**
     * 模型名
     */
    @Schema(title = "模型名", description = "模型名", maxLength=100, nullable=false )
    private String modelName;

    /**
     * 状态
     */
    @Schema(title = "状态", description = "状态", maxLength=10, nullable=true )
    private int state;
    /**
     * 输入计费参数(毫积点/千token;按量模型=0)
     */
    private int creditInput;
    /**
     * 输出计费参数(毫积点/千token;按量模型=每单位毫积点)
     */
    private int creditOutput;
    /**
     * 缓存折抵参数(毫积点/千token)
     */
    private int creditCached;
    /**
     * 输入token厂商定价(分/百万token;0=未提供)
     */
    private int priceInput;
    /**
     * 输出token厂商定价(分/百万token;0=未提供)
     */
    private int priceOutput;
    /**
     * 缓存命中token厂商定价(分/百万token;0=未提供)
     */
    private int priceCached;

    /**
     * 创建时间
     */
    @Schema(title = "创建时间", description = "创建时间", maxLength=23, nullable=true )
    private Date createDate;

    /**
     * 修改时间
     */
    @Schema(title = "修改时间", description = "修改时间", maxLength=23, nullable=true )
    private Date modifyDate;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSaasId() {
        return saasId;
    }

    public void setSaasId(long saasId) {
        this.saasId = saasId;
    }

    public long getMchId() {
        return mchId;
    }

    public void setMchId(long mchId) {
        this.mchId = mchId;
    }

    public long getApiId() {
        return apiId;
    }

    public void setApiId(long apiId) {
        this.apiId = apiId;
    }

    public String getVendorClass() {
        return vendorClass;
    }

    public void setVendorClass(String vendorClass) {
        this.vendorClass = vendorClass;
    }

    public String getModelType() {
        return modelType;
    }

    public void setModelType(String modelType) {
        this.modelType = modelType;
    }

    public String getModelTag() {
        return modelTag;
    }

    public void setModelTag(String modelTag) {
        this.modelTag = modelTag;
    }

    public String getConfigCode() {
        return configCode;
    }

    public void setConfigCode(String configCode) {
        this.configCode = configCode;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public String getConfigDesc() {
        return configDesc;
    }

    public void setConfigDesc(String configDesc) {
        this.configDesc = configDesc;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getModifyDate() {
        return modifyDate;
    }

    public void setModifyDate(Date modifyDate) {
        this.modifyDate = modifyDate;
    }
    public int getCreditInput() {
        return creditInput;
    }

    public void setCreditInput(int creditInput) {
        this.creditInput = creditInput;
    }

    public int getCreditOutput() {
        return creditOutput;
    }

    public void setCreditOutput(int creditOutput) {
        this.creditOutput = creditOutput;
    }

    public int getCreditCached() {
        return creditCached;
    }

    public void setCreditCached(int creditCached) {
        this.creditCached = creditCached;
    }

    public int getPriceInput() {
        return priceInput;
    }

    public void setPriceInput(int priceInput) {
        this.priceInput = priceInput;
    }

    public int getPriceOutput() {
        return priceOutput;
    }

    public void setPriceOutput(int priceOutput) {
        this.priceOutput = priceOutput;
    }

    public int getPriceCached() {
        return priceCached;
    }

    public void setPriceCached(int priceCached) {
        this.priceCached = priceCached;
    }
}
