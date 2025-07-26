package uw.ai;


import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import uw.ai.rpc.AiChatRpc;
import uw.ai.rpc.AiToolRpc;
import uw.ai.rpc.AiTranslateRpc;
import uw.ai.util.BeanOutputConverter;
import uw.ai.vo.*;
import uw.auth.service.annotation.MscPermDeclare;
import uw.auth.service.constant.UserType;
import uw.common.dto.ResponseData;

import java.util.List;

/**
 * AiClientHelper。
 */
public class AiClientHelper {

    private static final Logger log = LoggerFactory.getLogger(AiClientHelper.class);

    /**
     * 生成系统提示.
     */
    private static final String ENTITY_SYSTEM_PROMPT = """
            Your response should be in JSON format.
            Do not include any explanations, only provide a RFC8259 compliant JSON response following this format without deviation.
            Do not include markdown code blocks in your response.
            Remove the ```json markdown from the output.
            Here is the JSON Schema instance your output must adhere to:
            ```%s```
            """;

    /**
     * toolRpc
     */
    private static AiToolRpc toolRpc;

    /**
     * chatRpc
     */
    private static AiChatRpc chatRpc;

    /**
     * translateRpc
     */
    private static AiTranslateRpc translateRpc;

    public AiClientHelper(AiToolRpc toolRpc, AiChatRpc chatRpc, AiTranslateRpc translateRpc) {
        AiClientHelper.toolRpc = toolRpc;
        AiClientHelper.chatRpc = chatRpc;
        AiClientHelper.translateRpc = translateRpc;
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
    public static ResponseData<String> chatGenerate(AiChatGenerateParam param) {
        return chatRpc.generate(param);
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
    public <T> ResponseData<T> chatGenerateEntity(AiChatGenerateParam param, Class<T> clazz) {
        // 设置类型转换器
        BeanOutputConverter<T> beanOutputConverter = new BeanOutputConverter<>(clazz);
        // 设置系统提示
        StringBuilder systemPrompt = new StringBuilder();
        if (param.getSystemPrompt() != null) {
            systemPrompt.append(param.getSystemPrompt());
            systemPrompt.append("\n");
        }
        systemPrompt.append(String.format(ENTITY_SYSTEM_PROMPT, beanOutputConverter.getFormat()));
        param.setSystemPrompt(systemPrompt.toString());
        // 调用生成
        ResponseData<String> responseData = chatGenerate(param);
        if (responseData.isNotSuccess()) {
            return responseData.raw();
        }
        // 转换成实体
        return ResponseData.success(beanOutputConverter.convert(responseData.getData()));
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
}
