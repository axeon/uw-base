package uw.ai;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import uw.ai.rpc.AiChatRpc;
import uw.ai.rpc.AiConfigRpc;
import uw.ai.rpc.AiImageRpc;
import uw.ai.rpc.AiToolRpc;
import uw.ai.rpc.AiTranslateRpc;
import uw.ai.util.BeanOutputConverter;
import uw.ai.vo.*;
import uw.common.response.ResponseData;

import java.util.List;

/**
 * AiClientHelper。
 */
public class AiClientHelper {

    private static final Logger log = LoggerFactory.getLogger(AiClientHelper.class);

    /**
     * toolRpc
     */
    private static volatile AiToolRpc toolRpc;

    /**
     * chatRpc
     */
    private static volatile AiChatRpc chatRpc;

    /**
     * translateRpc
     */
    private static volatile AiTranslateRpc translateRpc;

    /**
     * configRpc
     */
    private static volatile AiConfigRpc configRpc;

    /**
     * imageRpc
     */
    private static volatile AiImageRpc imageRpc;

    public AiClientHelper(AiToolRpc toolRpc, AiChatRpc chatRpc, AiTranslateRpc translateRpc, AiConfigRpc configRpc, AiImageRpc imageRpc) {
        AiClientHelper.toolRpc = toolRpc;
        AiClientHelper.chatRpc = chatRpc;
        AiClientHelper.translateRpc = translateRpc;
        AiClientHelper.configRpc = configRpc;
        AiClientHelper.imageRpc = imageRpc;
    }

    /**
     * 获取工具元数据。
     *
     * @return
     */
    public static ResponseData<List<AiToolMeta>> listToolMeta(String appName) {
        return toolRpc.listToolMeta(appName);
    }

    /**
     * 更新工具元数据。
     *
     * @param aiToolMeta
     * @return
     */
    public static ResponseData updateToolMeta(AiToolMeta aiToolMeta) {
        return toolRpc.updateToolMeta(aiToolMeta);
    }

    /**
     * 聊天生成。
     *
     * @param param
     * @return
     */
    public static ResponseData<String> generate(AiChatGenerateParam param) {
        return chatRpc.generate(param);
    }

    /**
     * 聊天生成。
     *
     * @param param
     * @return
     */
    public static Flux<String> chatGenerate(AiChatGenerateParam param) {
        return chatRpc.chatGenerate(param);
    }

    /**
     * 聊天生成实体。
     *
     * @param param
     * @param clazz
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> ResponseData<T> generateEntity(AiChatGenerateParam param, Class<T> clazz) {
        // 设置类型转换器
        BeanOutputConverter<T> beanOutputConverter = new BeanOutputConverter<>(clazz);
        // 设置系统提示（仅使用 getFormat，避免 Schema 嵌套重复导致弱模型混淆）
        StringBuilder systemPrompt = new StringBuilder();
        if (param.getSystemPrompt() != null) {
            systemPrompt.append(param.getSystemPrompt());
            systemPrompt.append("\n");
        }
        systemPrompt.append(beanOutputConverter.getFormat());
        param.setSystemPrompt(systemPrompt.toString());
        // 调用生成
        ResponseData<String> responseData = generate(param);
        if (responseData.isNotSuccess()) {
            return responseData.raw();
        }
        // 转换成实体
        try {
            return ResponseData.success(beanOutputConverter.convert(beanOutputConverter.cleanJson(responseData.getData())), responseData.getCode(), responseData.getData());
        } catch (Exception e) {
            log.error("generateEntity() JSON转换失败: {}", e.getMessage(), e);
            return ResponseData.errorMsg("JSON转换失败: " + e.getMessage());
        }
    }

    /**
     * 翻译列表。
     *
     * @param param
     */
    public static ResponseData<AiTranslateResultData[]> translateList(AiTranslateListParam param) {
        return translateRpc.translateList(param);
    }

    /**
     * 翻译Map。
     *
     * @param param
     */
    public static ResponseData<AiTranslateResultData[]> translateMap(AiTranslateMapParam param) {
        return translateRpc.translateMap(param);
    }


    /**
     * 根据saas信息获取模型配置列表。
     *
     * @param saasId 租户ID
     * @param mchId  商户ID（可为null或0，表示不限商户）
     * @return 模型配置列表
     */
    public static ResponseData<List<AiModelInfoVo>> listModelInfoBySaas(Long saasId, Long mchId) {
        return configRpc.listModelConfigBySaas(saasId, mchId);
    }

    /**
     * 根据API配置ID获取模型配置列表。
     *
     * @param apiId API配置ID
     * @return 模型配置列表
     */
    public static ResponseData<List<AiModelInfoVo>> listModelInfoByApi(Long apiId) {
        return configRpc.listModelConfigByApi(apiId);
    }

    /**
     * 根据ID获取模型配置。
     *
     * @param id 模型配置ID
     * @return 模型配置
     */
    public static ResponseData<AiModelInfoVo> listModelInfoById(Long id) {
        return configRpc.getModelConfigById(id);
    }

    /**
     * 根据配置代码获取模型配置。
     *
     * @param configCode 配置代码
     * @return 模型配置
     */
    public static ResponseData<AiModelInfoVo> listModelInfoByCode(String configCode) {
        return configRpc.getModelConfigByCode(configCode);
    }

    /**
     * 根据模型类型和标签获取模型配置列表。
     *
     * @param modelType 模型类型
     * @param modelTag  模型能力标签（可为null或空，表示不限标签）
     * @return 模型配置列表
     */
    public static ResponseData<List<AiModelInfoVo>> listModelInfoByType(String modelType, String modelTag) {
        return configRpc.listModelConfigByType(modelType, modelTag);
    }

    /**
     * 根据saas信息获取API连接配置列表。
     *
     * @param saasId 租户ID
     * @param mchId  商户ID（可为null或0，表示不限商户）
     * @return API连接配置列表
     */
    public static ResponseData<List<AiModelApiVo>> listModelApiBySaas(Long saasId, Long mchId) {
        return configRpc.listApiConfigBySaas(saasId, mchId);
    }

    /**
     * 根据ID获取API连接配置。
     *
     * @param id API配置ID
     * @return API连接配置
     */
    public static ResponseData<AiModelApiVo> getModelApiById(Long id) {
        return configRpc.getApiConfigById(id);
    }

    /**
     * 根据配置代码获取API连接配置。
     *
     * @param apiCode 配置代码
     * @return API连接配置
     */
    public static ResponseData<AiModelApiVo> getModelApiByCode(String apiCode) {
        return configRpc.getApiConfigByCode(apiCode);
    }

    /**
     * 生成图片。
     *
     * @param param 图片生成参数
     * @return 图片生成结果
     */
    public static ResponseData<AiImageResultData> generateImage(AiImageGenerateParam param) {
        return imageRpc.generate(param);
    }
}
