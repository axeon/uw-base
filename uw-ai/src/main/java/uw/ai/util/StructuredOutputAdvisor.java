package uw.ai.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.ai.vo.AiChatGenerateParam;
import uw.common.response.ResponseData;

import java.util.function.Function;

/**
 * 结构化输出顾问
 * <p>
 * 职责：①请求增强——把目标类型的 JSON Schema 格式说明追加到系统提示（仅使用 getFormat，
 * 避免 Schema 嵌套重复导致弱模型混淆）；②输出自校正——JSON 解析失败时把上一次的输出与
 * 解析错误信息追加到用户提示反馈给模型重新生成，最多 maxAttempts 次；生成失败
 * （配置/网络等）不重试，直接透传。
 * <p>
 * 模型调用通过 modelCall 函数注入，与 AiClientHelper 解耦，可独立单测。
 *
 * @param <T> 目标实体类型
 */
public class StructuredOutputAdvisor<T> {

    private static final Logger log = LoggerFactory.getLogger(StructuredOutputAdvisor.class);

    /**
     * 默认自校正重试次数。
     */
    private static final int DEFAULT_RETRY_TIMES = 3;

    /**
     * 自校正重试时追加到用户提示的模板：原提示 + 错误信息 + 上次输出 + 修正要求。
     */
    private static final String RETRY_PROMPT = """
            %s
            你上一次的输出JSON解析失败，错误信息：%s
            你上一次的输出：
            %s
            请修正后重新输出，必须严格遵守JSON Schema格式，仅输出JSON本身。""";

    /**
     * 类型转换器（JSON Schema 生成 + 输出清洗 + 反序列化）。
     */
    private final BeanOutputConverter<T> converter;

    /**
     * 最大尝试次数（含首次）。
     */
    private final int maxAttempts;

    /**
     * 以默认重试次数（3次）构造顾问。
     *
     * @param clazz 目标类型
     */
    public StructuredOutputAdvisor(Class<T> clazz) {
        this(clazz, DEFAULT_RETRY_TIMES);
    }

    /**
     * 构造顾问。
     *
     * @param clazz       目标类型
     * @param maxAttempts 最大尝试次数（含首次）
     */
    public StructuredOutputAdvisor(Class<T> clazz, int maxAttempts) {
        this.converter = new BeanOutputConverter<>(clazz);
        this.maxAttempts = maxAttempts;
    }

    /**
     * 增强请求并执行自校正生成。
     * <p>
     * 在系统提示后追加 JSON Schema 格式说明，调用模型生成并清洗（去除 markdown 标记、
     * think 标签等）后反序列化为目标类型；解析失败时携带错误信息与上次输出重试。
     *
     * @param request   对话生成参数（方法会修改其 systemPrompt/userPrompt）
     * @param modelCall 模型调用函数（通常传 AiClientHelper::generate）
     * @return 转换后的实体；生成失败或重试后仍 JSON 转换失败时返回错误 ResponseData
     */
    public ResponseData<T> adviseCall(AiChatGenerateParam request, Function<AiChatGenerateParam, ResponseData<String>> modelCall) {
        String baseSystemPrompt = request.getSystemPrompt() == null ? "" : request.getSystemPrompt() + "\n";
        request.setSystemPrompt(baseSystemPrompt + converter.getFormat());

        String originalUserPrompt = request.getUserPrompt();
        Exception lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            ResponseData<String> response = modelCall.apply(request);
            if (response.isNotSuccess()) {
                return response.raw();
            }
            String cleaned = converter.cleanJson(response.getData());
            try {
                return ResponseData.success(converter.convert(cleaned), response.getCode(), response.getData());
            } catch (Exception e) {
                lastError = e;
                log.warn("adviseCall() 第{}/{}次JSON转换失败: {}", attempt, maxAttempts, e.getMessage());
                if (attempt < maxAttempts) {
                    request.setUserPrompt(RETRY_PROMPT.formatted(originalUserPrompt, e.getMessage(), cleaned));
                }
            }
        }
        log.error("adviseCall() JSON转换失败(已重试{}次): {}", maxAttempts, lastError.getMessage(), lastError);
        return ResponseData.errorMsg("JSON转换失败: " + lastError.getMessage());
    }
}
