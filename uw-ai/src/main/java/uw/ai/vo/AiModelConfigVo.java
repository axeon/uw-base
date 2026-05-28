package uw.ai.vo;

import java.util.Date;

/**
 * AI模型配置VO — 供外部调用方查询可用模型列表。
 */
public class AiModelConfigVo {

    /**
     * 模型配置ID
     */
    private long id;

    /**
     * 模型类型: CHAT/EMBEDDING/RERANK/TTS/OCR
     */
    private String modelType;

    /**
     * 配置代码
     */
    private String configCode;

    /**
     * 配置名称
     */
    private String configName;

    /**
     * 配置描述
     */
    private String configDesc;

    /**
     * 模型名
     */
    private String modelName;

    /**
     * 供应商类
     */
    private String vendorClass;

    /**
     * 状态
     */
    private int state;

    /**
     * 创建时间
     */
    private Date createDate;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getModelType() { return modelType; }
    public void setModelType(String modelType) { this.modelType = modelType; }

    public String getConfigCode() { return configCode; }
    public void setConfigCode(String configCode) { this.configCode = configCode; }

    public String getConfigName() { return configName; }
    public void setConfigName(String configName) { this.configName = configName; }

    public String getConfigDesc() { return configDesc; }
    public void setConfigDesc(String configDesc) { this.configDesc = configDesc; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getVendorClass() { return vendorClass; }
    public void setVendorClass(String vendorClass) { this.vendorClass = vendorClass; }

    public int getState() { return state; }
    public void setState(int state) { this.state = state; }

    public Date getCreateDate() { return createDate; }
    public void setCreateDate(Date createDate) { this.createDate = createDate; }
}