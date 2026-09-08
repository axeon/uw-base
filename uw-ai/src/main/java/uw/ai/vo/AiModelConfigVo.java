package uw.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Date;

/**
 * AI 模型配置视图，供外部调用方查询可用模型及其计费参数。
 */
@Schema(title = "AI模型配置VO", description = "AI模型配置信息，供外部服务查询")
public class AiModelConfigVo {

    /**
     * 主键 ID。
     */
    @Schema(title = "主键 ID", description = "主键 ID", maxLength = 19, nullable = false)
    private long id;

    /**
     * 租户 ID。
     */
    @Schema(title = "租户 ID", description = "租户 ID", maxLength = 19, nullable = false)
    private long saasId;

    /**
     * 商户 ID。
     */
    @Schema(title = "商户 ID", description = "商户 ID", maxLength = 19, nullable = false)
    private long mchId;

    /**
     * API 连接配置 ID。
     */
    @Schema(title = "API 配置 ID", description = "API 连接配置 ID", maxLength = 19, nullable = false)
    private long apiId;

    /**
     * 供应商实现类名。
     */
    @Schema(title = "供应商实现类", description = "供应商实现类名", maxLength = 200, nullable = false)
    private String vendorClass;

    /**
     * 模型类型，如 CHAT、EMBEDDING、RERANK、TTS、OCR、IMAGE_GENERATION 或 AUDIO_TRANSCRIPTION。
     */
    @Schema(title = "模型类型", description = "模型类型，如 CHAT、EMBEDDING、RERANK、TTS、OCR、IMAGE_GENERATION 或 AUDIO_TRANSCRIPTION", maxLength = 50, nullable = false)
    private String modelType;

    /**
     * 模型能力标签。
     */
    @Schema(title = "模型能力标签", description = "模型能力标签", maxLength = 200, nullable = true)
    private String modelTag;

    /**
     * 模型配置代码。
     */
    @Schema(title = "配置代码", description = "模型配置代码", maxLength = 100, nullable = true)
    private String configCode;

    /**
     * 模型配置名称。
     */
    @Schema(title = "配置名称", description = "模型配置名称", maxLength = 200, nullable = true)
    private String configName;

    /**
     * 模型配置描述。
     */
    @Schema(title = "配置描述", description = "模型配置描述", maxLength = 65535, nullable = true)
    private String configDesc;

    /**
     * 供应商接口使用的模型名称。
     */
    @Schema(title = "模型名称", description = "供应商接口使用的模型名称", maxLength = 100, nullable = false)
    private String modelName;

    /**
     * 记录状态。
     */
    @Schema(title = "状态", description = "记录状态", maxLength = 10, nullable = true)
    private int state;

    /**
     * 输入计费参数；token 模型单位为毫积点/千 token，按量模型为 0。
     */
    @Schema(title = "输入计费参数", description = "输入计费参数；token 模型单位为毫积点/千 token，按量模型为 0")

    private int creditInput;

    /**
     * 输出计费参数；token 模型单位为毫积点/千 token，按量模型单位为毫积点/计费单位。
     */
    @Schema(title = "输出计费参数", description = "输出计费参数；token 模型单位为毫积点/千 token，按量模型单位为毫积点/计费单位")

    private int creditOutput;

    /**
     * 缓存读取计费参数，单位为毫积点/千 token；从基础输入费用中扣除的分量。
     */
    @Schema(title = "缓存读取计费参数", description = "缓存读取计费参数，单位为毫积点/千 token；从基础输入费用中扣除的分量")

    private int creditCacheRead;

    /**
     * 缓存写入计费参数，单位为毫积点/千 token；按计费公式计入写入分量，0 表示不计入。
     */
    @Schema(title = "缓存写入计费参数", description = "缓存写入计费参数，单位为毫积点/千 token；按计费公式计入写入分量，0 表示不计入")
    private int creditCacheWrite = 0;

    /**
     * 输入完整参考价，单位为分/百万 token；0 表示未配置，不参与积点扣减。
     */
    @Schema(title = "输入参考价", description = "输入完整参考价，单位为分/百万 token；0 表示未配置，不参与积点扣减")

    private int priceInput;

    /**
     * 输出完整参考价，单位为分/百万 token；0 表示未配置，不参与积点扣减。
     */
    @Schema(title = "输出参考价", description = "输出完整参考价，单位为分/百万 token；0 表示未配置，不参与积点扣减")

    private int priceOutput;

    /**
     * 缓存读取完整参考价，单位为分/百万 token；0 表示未配置，不参与积点扣减。
     */
    @Schema(title = "缓存读取参考价", description = "缓存读取完整参考价，单位为分/百万 token；0 表示未配置，不参与积点扣减")

    private int priceCacheRead;

    /**
     * 缓存写入完整参考价，单位为分/百万 token；0 表示未配置，不参与积点扣减。
     */
    @Schema(title = "缓存写入参考价", description = "缓存写入完整参考价，单位为分/百万 token；0 表示未配置，不参与积点扣减")
    private int priceCacheWrite;

    /**
     * 创建时间。
     */
    @Schema(title = "创建时间", description = "创建时间", maxLength = 23, nullable = true)
    private Date createDate;

    /**
     * 修改时间。
     */
    @Schema(title = "修改时间", description = "修改时间", maxLength = 23, nullable = true)
    private Date modifyDate;

    /**
     * 获取主键 ID。
     *
     * @return 主键 ID
     */
    public long getId() {
        return id;
    }

    /**
     * 设置主键 ID。
     *
     * @param id 主键 ID
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * 获取租户 ID。
     *
     * @return 租户 ID
     */
    public long getSaasId() {
        return saasId;
    }

    /**
     * 设置租户 ID。
     *
     * @param saasId 租户 ID
     */
    public void setSaasId(long saasId) {
        this.saasId = saasId;
    }

    /**
     * 获取商户 ID。
     *
     * @return 商户 ID
     */
    public long getMchId() {
        return mchId;
    }

    /**
     * 设置商户 ID。
     *
     * @param mchId 商户 ID
     */
    public void setMchId(long mchId) {
        this.mchId = mchId;
    }

    /**
     * 获取API 配置 ID。
     *
     * @return API 连接配置 ID
     */
    public long getApiId() {
        return apiId;
    }

    /**
     * 设置API 配置 ID。
     *
     * @param apiId API 连接配置 ID
     */
    public void setApiId(long apiId) {
        this.apiId = apiId;
    }

    /**
     * 获取供应商实现类。
     *
     * @return 供应商实现类名
     */
    public String getVendorClass() {
        return vendorClass;
    }

    /**
     * 设置供应商实现类。
     *
     * @param vendorClass 供应商实现类名
     */
    public void setVendorClass(String vendorClass) {
        this.vendorClass = vendorClass;
    }

    /**
     * 获取模型类型。
     *
     * @return 模型类型，如 CHAT、EMBEDDING、RERANK、TTS、OCR、IMAGE_GENERATION 或 AUDIO_TRANSCRIPTION
     */
    public String getModelType() {
        return modelType;
    }

    /**
     * 设置模型类型。
     *
     * @param modelType 模型类型，如 CHAT、EMBEDDING、RERANK、TTS、OCR、IMAGE_GENERATION 或 AUDIO_TRANSCRIPTION
     */
    public void setModelType(String modelType) {
        this.modelType = modelType;
    }

    /**
     * 获取模型能力标签。
     *
     * @return 模型能力标签
     */
    public String getModelTag() {
        return modelTag;
    }

    /**
     * 设置模型能力标签。
     *
     * @param modelTag 模型能力标签
     */
    public void setModelTag(String modelTag) {
        this.modelTag = modelTag;
    }

    /**
     * 获取配置代码。
     *
     * @return 模型配置代码
     */
    public String getConfigCode() {
        return configCode;
    }

    /**
     * 设置配置代码。
     *
     * @param configCode 模型配置代码
     */
    public void setConfigCode(String configCode) {
        this.configCode = configCode;
    }

    /**
     * 获取配置名称。
     *
     * @return 模型配置名称
     */
    public String getConfigName() {
        return configName;
    }

    /**
     * 设置配置名称。
     *
     * @param configName 模型配置名称
     */
    public void setConfigName(String configName) {
        this.configName = configName;
    }

    /**
     * 获取配置描述。
     *
     * @return 模型配置描述
     */
    public String getConfigDesc() {
        return configDesc;
    }

    /**
     * 设置配置描述。
     *
     * @param configDesc 模型配置描述
     */
    public void setConfigDesc(String configDesc) {
        this.configDesc = configDesc;
    }

    /**
     * 获取模型名称。
     *
     * @return 供应商接口使用的模型名称
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * 设置模型名称。
     *
     * @param modelName 供应商接口使用的模型名称
     */
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    /**
     * 获取状态。
     *
     * @return 记录状态
     */
    public int getState() {
        return state;
    }

    /**
     * 设置状态。
     *
     * @param state 记录状态
     */
    public void setState(int state) {
        this.state = state;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public Date getCreateDate() {
        return createDate;
    }

    /**
     * 设置创建时间。
     *
     * @param createDate 创建时间
     */
    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    /**
     * 获取修改时间。
     *
     * @return 修改时间
     */
    public Date getModifyDate() {
        return modifyDate;
    }

    /**
     * 设置修改时间。
     *
     * @param modifyDate 修改时间
     */
    public void setModifyDate(Date modifyDate) {
        this.modifyDate = modifyDate;
    }

    /**
     * 获取输入计费参数。
     *
     * @return 输入计费参数；token 模型单位为毫积点/千 token，按量模型为 0
     */
    public int getCreditInput() {
        return creditInput;
    }

    /**
     * 设置输入计费参数。
     *
     * @param creditInput 输入计费参数；token 模型单位为毫积点/千 token，按量模型为 0
     */
    public void setCreditInput(int creditInput) {
        this.creditInput = creditInput;
    }

    /**
     * 获取输出计费参数。
     *
     * @return 输出计费参数；token 模型单位为毫积点/千 token，按量模型单位为毫积点/计费单位
     */
    public int getCreditOutput() {
        return creditOutput;
    }

    /**
     * 设置输出计费参数。
     *
     * @param creditOutput 输出计费参数；token 模型单位为毫积点/千 token，按量模型单位为毫积点/计费单位
     */
    public void setCreditOutput(int creditOutput) {
        this.creditOutput = creditOutput;
    }

    /**
     * 获取缓存读取计费参数。
     *
     * @return 缓存读取计费参数，单位为毫积点/千 token；从基础输入费用中扣除的分量
     */
    public int getCreditCacheRead() {
        return creditCacheRead;
    }

    /**
     * 获取缓存写入计费参数。
     *
     * @return 缓存写入计费参数，单位为毫积点/千 token；按计费公式计入写入分量，0 表示不计入
     */
    public int getCreditCacheWrite() {
        return creditCacheWrite;
    }

    /**
     * 设置缓存读取计费参数。
     *
     * @param creditCacheRead 缓存读取计费参数，单位为毫积点/千 token；从基础输入费用中扣除的分量
     */
    public void setCreditCacheRead(int creditCacheRead) {
        this.creditCacheRead = creditCacheRead;
    }

    /**
     * 设置缓存写入计费参数。
     *
     * @param creditCacheWrite 缓存写入计费参数，单位为毫积点/千 token；按计费公式计入写入分量，0 表示不计入
     */
    public void setCreditCacheWrite(int creditCacheWrite) {
        this.creditCacheWrite = creditCacheWrite;
    }

    /**
     * 获取输入参考价。
     *
     * @return 输入完整参考价，单位为分/百万 token；0 表示未配置，不参与积点扣减
     */
    public int getPriceInput() {
        return priceInput;
    }

    /**
     * 设置输入参考价。
     *
     * @param priceInput 输入完整参考价，单位为分/百万 token；0 表示未配置，不参与积点扣减
     */
    public void setPriceInput(int priceInput) {
        this.priceInput = priceInput;
    }

    /**
     * 获取输出参考价。
     *
     * @return 输出完整参考价，单位为分/百万 token；0 表示未配置，不参与积点扣减
     */
    public int getPriceOutput() {
        return priceOutput;
    }

    /**
     * 设置输出参考价。
     *
     * @param priceOutput 输出完整参考价，单位为分/百万 token；0 表示未配置，不参与积点扣减
     */
    public void setPriceOutput(int priceOutput) {
        this.priceOutput = priceOutput;
    }

    /**
     * 获取缓存读取参考价。
     *
     * @return 缓存读取完整参考价，单位为分/百万 token；0 表示未配置，不参与积点扣减
     */
    public int getPriceCacheRead() {
        return priceCacheRead;
    }

    /**
     * 获取缓存写入参考价。
     *
     * @return 缓存写入完整参考价，单位为分/百万 token；0 表示未配置，不参与积点扣减
     */
    public int getPriceCacheWrite() {
        return priceCacheWrite;
    }

    /**
     * 设置缓存读取参考价。
     *
     * @param priceCacheRead 缓存读取完整参考价，单位为分/百万 token；0 表示未配置，不参与积点扣减
     */
    public void setPriceCacheRead(int priceCacheRead) {
        this.priceCacheRead = priceCacheRead;
    }

    /**
     * 设置缓存写入参考价。
     *
     * @param priceCacheWrite 缓存写入完整参考价，单位为分/百万 token；0 表示未配置，不参与积点扣减
     */
    public void setPriceCacheWrite(int priceCacheWrite) {
        this.priceCacheWrite = priceCacheWrite;
    }

}
