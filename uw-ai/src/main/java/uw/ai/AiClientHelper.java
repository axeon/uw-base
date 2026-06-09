package uw.ai;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import uw.ai.rpc.AiChatRpc;
import uw.ai.rpc.AiConfigRpc;
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

    public AiClientHelper(AiToolRpc toolRpc, AiChatRpc chatRpc, AiTranslateRpc translateRpc, AiConfigRpc configRpc) {
        AiClientHelper.toolRpc = toolRpc;
        AiClientHelper.chatRpc = chatRpc;
        AiClientHelper.translateRpc = translateRpc;
        AiClientHelper.configRpc = configRpc;
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
     * 获取所有可用的模型配置列表。
     */
    public static ResponseData<List<AiModelConfigVo>> listModelConfig() {
        return configRpc.listModelConfig();
    }
}
