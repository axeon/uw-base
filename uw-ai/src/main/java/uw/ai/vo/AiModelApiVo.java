package uw.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Date;

/**
 * AI API连接配置VO。
 */
@Schema(title = "AI API连接配置VO", description = "AI API连接配置信息，供外部服务查询")
public class AiModelApiVo {

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
     * 配置代码
     */
    @Schema(title = "配置代码", description = "配置代码", maxLength=100, nullable=true )
    private String apiCode;

    /**
     * 配置名称
     */
    @Schema(title = "配置名称", description = "配置名称", maxLength=200, nullable=true )
    private String apiName;

    /**
     * 配置描述
     */
    @Schema(title = "配置描述", description = "配置描述", maxLength=65535, nullable=true )
    private String apiDesc;

    /**
     * API地址
     */
    @Schema(title = "API地址", description = "API地址", maxLength=200, nullable=true )
    private String apiUrl;

    /**
     * API密钥
     */
    @Schema(title = "API密钥", description = "API密钥", maxLength=200, nullable=true )
    private String apiKey;

    /**
     * 状态
     */
    @Schema(title = "状态", description = "状态", maxLength=10, nullable=true )
    private int state;


    /**
     * 创建时间
     */
    @Schema(title = "创建时间", description = "创建时间", maxLength=23, nullable=true )
    private java.util.Date createDate;

    /**
     * 修改时间
     */
    @Schema(title = "修改时间", description = "修改时间", maxLength=23, nullable=true )
    private java.util.Date modifyDate;

    public AiModelApiVo() {
    }

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

    public String getApiDesc() {
        return apiDesc;
    }

    public void setApiDesc(String apiDesc) {
        this.apiDesc = apiDesc;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
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
}
