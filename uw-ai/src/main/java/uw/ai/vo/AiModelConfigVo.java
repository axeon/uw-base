package uw.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Date;

/**
 * AI模型配置VO。
 */
@Schema(title = "AI模型配置VO", description = "AI模型配置信息，供外部服务查询")
public class AiModelConfigVo {

    /**
     * 主键ID
     */
    @Schema(title = "主键ID", description = "模型配置ID")
    private long id;

    /**
     * 模型类型
     */
    @Schema(title = "模型类型", description = "CHAT/EMBEDDING/RERANK/TTS/OCR")
    private String modelType;

    /**
     * 配置代码
     */
    @Schema(title = "配置代码", description = "配置代码")
    private String configCode;

    /**
     * 配置名称
     */
    @Schema(title = "配置名称", description = "配置名称")
    private String configName;

    /**
     * 配置描述
     */
    @Schema(title = "配置描述", description = "配置描述")
    private String configDesc;

    /**
     * 模型名
     */
    @Schema(title = "模型名", description = "模型名，如 qwen-flash / text-embedding-v3")
    private String modelName;

    /**
     * Vendor类
     */
    @Schema(title = "Vendor类", description = "供应商实现类，如 OpenAiVendor / OllamaVendor")
    private String vendorClass;

    /**
     * API配置ID
     */
    @Schema(title = "API配置ID", description = "关联的API连接配置ID")
    private long apiId;

    /**
     * API配置代码
     */
    @Schema(title = "API配置代码", description = "关联的API连接配置代码")
    private String apiCode;

    /**
     * API配置名称
     */
    @Schema(title = "API配置名称", description = "关联的API连接配置名称，如 阿里DashScope生产账号")
    private String apiName;

    /**
     * API地址
     */
    @Schema(title = "API地址", description = "API连接地址")
    private String apiUrl;

    /**
     * 状态
     */
    @Schema(title = "状态", description = "状态")
    private int state;

    /**
     * 创建时间
     */
    @Schema(title = "创建时间", description = "创建时间")
    private Date createDate;

    public AiModelConfigVo() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getModelType() {
        return modelType;
    }

    public void setModelType(String modelType) {
        this.modelType = modelType;
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

    public String getVendorClass() {
        return vendorClass;
    }

    public void setVendorClass(String vendorClass) {
        this.vendorClass = vendorClass;
    }

    public long getApiId() {
        return apiId;
    }

    public void setApiId(long apiId) {
        this.apiId = apiId;
    }

    public String getApiCode() {
        return apiCode;
    }

    public void setApiCode(String apiCode) {
        this.apiCode = apiCode;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
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
}
