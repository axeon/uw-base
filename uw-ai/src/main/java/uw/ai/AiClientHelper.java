package uw.ai;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import uw.ai.rpc.*;
import uw.ai.util.StructuredOutputAdvisor;
import uw.ai.vo.*;
import uw.common.response.ResponseData;

import java.util.List;

/**
 * AI 客户端静态门面。
 * <p>
 * 封装对话生成（同步/流式）、结构化输出、翻译、图片生成、模型/API 配置查询与工具元数据管理。
 * 底层 RPC Bean 由 {@link uw.ai.conf.UwAiAutoConfiguration} 自动装配，并在构造本类时注入静态字段，
 * 因此应用启动后即可通过静态方法调用。
 * <p>
 * 所有调用统一返回 {@link ResponseData}，可使用 {@code onSuccess/onError} 处理结果。
 */
public class AiClientHelper {

    private static final Logger log = LoggerFactory.getLogger(AiClientHelper.class);

    /**
     * toolRpc
     */
    private static volatile AiToolRpc aiToolRpc;

    /**
     * chatRpc
     */
    private static volatile AiChatRpc aiChatRpc;

    /**
     * translateRpc
     */
    private static volatile AiTranslateRpc aiTranslateRpc;

    /**
     * configRpc
     */
    private static volatile AiConfigRpc aiConfigRpc;

    /**
     * imageRpc
     */
    private static volatile AiImageRpc aiImageRpc;

    /**
     * tokenRpc
     */
    private static volatile AiCreditRpc aiCreditRpc;

    public AiClientHelper(AiToolRpc aiToolRpc, AiChatRpc aiChatRpc, AiTranslateRpc aiTranslateRpc, AiConfigRpc aiConfigRpc, AiImageRpc aiImageRpc, AiCreditRpc aiCreditRpc) {
        AiClientHelper.aiToolRpc = aiToolRpc;
        AiClientHelper.aiChatRpc = aiChatRpc;
        AiClientHelper.aiTranslateRpc = aiTranslateRpc;
        AiClientHelper.aiConfigRpc = aiConfigRpc;
        AiClientHelper.aiImageRpc = aiImageRpc;
        AiClientHelper.aiCreditRpc = aiCreditRpc;
    }

    /**
     * 获取工具元数据。
     *
     * @return
     */
    public static ResponseData<List<AiToolMeta>> listToolMeta(String appName) {
        return aiToolRpc.listToolMeta(appName);
    }

    /**
     * 更新工具元数据。
     *
     * @param aiToolMeta
     * @return
     */
    public static ResponseData updateToolMeta(AiToolMeta aiToolMeta) {
        return aiToolRpc.updateToolMeta(aiToolMeta);
    }

    /**
     * 根据saas信息获取模型配置列表。
     *
     * @param saasId 租户ID
     * @param mchId  商户ID（可为null或0，表示不限商户）
     * @return 模型配置列表
     */
    public static ResponseData<List<AiModelConfigVo>> listModelInfoBySaas(Long saasId, Long mchId) {
        return aiConfigRpc.listModelConfigBySaas(saasId, mchId);
    }

    /**
     * 根据API配置ID获取模型配置列表。
     *
     * @param apiId API配置ID
     * @return 模型配置列表
     */
    public static ResponseData<List<AiModelConfigVo>> listModelInfoByApi(Long apiId) {
        return aiConfigRpc.listModelConfigByApi(0L, apiId, null);
    }

    /**
     * 根据ID获取模型配置。
     *
     * @param id 模型配置ID
     * @return 模型配置
     */
    public static ResponseData<AiModelConfigVo> listModelInfoById(Long id) {
        return aiConfigRpc.getModelConfig(0L, id, null);
    }

    /**
     * 根据ID获取模型配置（限定租户域）。
     *
     * @param saasId 租户ID（0=平台域）
     * @param id     模型配置ID
     * @return 模型配置
     */
    public static ResponseData<AiModelConfigVo> listModelInfoById(Long saasId, Long id) {
        return aiConfigRpc.getModelConfig(saasId, id, null);
    }

    /**
     * 根据配置代码获取模型配置（平台域 saas_id=0）。
     *
     * @param configCode 配置代码
     * @return 模型配置
     */
    public static ResponseData<AiModelConfigVo> listModelInfoByCode(String configCode) {
        return aiConfigRpc.getModelConfig(0L, null, configCode);
    }

    /**
     * 根据配置代码获取模型配置（限定租户域，唯一域 (saas_id, config_code)）。
     *
     * @param saasId     租户ID（0=平台域）
     * @param configCode 配置代码
     * @return 模型配置
     */
    public static ResponseData<AiModelConfigVo> listModelInfoByCode(Long saasId, String configCode) {
        return aiConfigRpc.getModelConfig(saasId, null, configCode);
    }

    /**
     * 根据模型类型和标签获取模型配置列表。
     *
     * @param modelType 模型类型
     * @param modelTag  模型能力标签（可为null或空，表示不限标签）
     * @return 模型配置列表
     */
    public static ResponseData<List<AiModelConfigVo>> listModelInfoByType(String modelType, String modelTag) {
        return aiConfigRpc.listModelConfigByType(modelType, modelTag);
    }

    /**
     * 根据saas信息获取API连接配置列表。
     *
     * @param saasId 租户ID
     * @param mchId  商户ID（可为null或0，表示不限商户）
     * @return API连接配置列表
     */
    public static ResponseData<List<AiModelApiVo>> listModelApiBySaas(Long saasId, Long mchId) {
        return aiConfigRpc.listApiConfigBySaas(saasId, mchId);
    }

    /**
     * 根据ID获取API连接配置。
     *
     * @param id API配置ID
     * @return API连接配置
     */
    public static ResponseData<AiModelApiVo> getModelApiById(Long id) {
        return aiConfigRpc.getApiConfig(0L, id, null);
    }

    /**
     * 根据配置代码获取API连接配置（平台域 saas_id=0）。
     *
     * @param apiCode 配置代码
     * @return API连接配置
     */
    public static ResponseData<AiModelApiVo> getModelApiByCode(String apiCode) {
        return aiConfigRpc.getApiConfig(0L, null, apiCode);
    }

    /**
     * 根据配置代码获取API连接配置（限定租户域，唯一域 (saas_id, api_code)）。
     *
     * @param saasId 租户ID（0=平台域）
     * @param apiCode 配置代码
     * @return API连接配置
     */
    public static ResponseData<AiModelApiVo> getModelApiByCode(Long saasId, String apiCode) {
        return aiConfigRpc.getApiConfig(saasId, null, apiCode);
    }

    /**
     * 聊天生成。
     *
     * @param param
     * @return
     */
    public static ResponseData<String> generate(AiChatGenerateParam param) {
        return aiChatRpc.generate(param);
    }

    /**
     * 聊天生成。
     *
     * @param param
     * @return
     */
    public static Flux<String> generateStream(AiChatGenerateParam param) {
        return aiChatRpc.generateStream(param);
    }

    /**
     * 聊天生成实体（结构化输出）。
     * <p>
     * 委托 {@link StructuredOutputAdvisor} 完成：在系统提示后追加目标类型的 JSON Schema 格式说明，
     * 对大模型返回的文本进行清洗（去除 markdown 标记、think 标签等）后反序列化为目标类型；
     * JSON 解析失败时自动重试（默认最多 3 次），把上一次的输出与解析错误信息追加到用户提示中
     * 反馈给模型针对性修正；生成失败（配置/网络等）不重试。
     *
     * @param param 对话生成参数（系统提示可为空，会追加格式说明）
     * @param clazz 目标类型
     * @param <T>   目标类型泛型
     * @return 转换后的实体；生成失败或重试后仍 JSON 转换失败时返回错误 ResponseData
     */
    public static <T> ResponseData<T> generateEntity(AiChatGenerateParam param, Class<T> clazz) {
        return new StructuredOutputAdvisor<>(clazz).adviseCall(param, AiClientHelper::generate);
    }

    /**
     * 翻译列表。
     *
     * @param param
     */
    public static ResponseData<AiTranslateResultData[]> translateList(AiTranslateListParam param) {
        return aiTranslateRpc.translateList(param);
    }

    /**
     * 翻译Map。
     *
     * @param param
     */
    public static ResponseData<AiTranslateResultData[]> translateMap(AiTranslateMapParam param) {
        return aiTranslateRpc.translateMap(param);
    }

    /**
     * 生成图片。
     *
     * @param param 图片生成参数
     * @return 图片生成结果
     */
    public static ResponseData<AiImageResultData> generateImage(AiImageGenerateParam param) {
        return aiImageRpc.generate(param);
    }

    /**
     * 给 saas 充值 AI 积点（ 可为负=回收）。
     *
     * @param param 充值参数
     * @return 充值后的余额
     */
    public static ResponseData<AiCreditBalanceVo> rechargeSaasCredits(AiCreditRechargeParam param) {
        return aiCreditRpc.recharge(param);
    }

    /**
     * 查询 saas AI 积点 余额。
     *
     * @param saasId 租户
     * @return 余额
     */
    public static ResponseData<AiCreditBalanceVo> getSaasCreditsBalance(long saasId) {
        return aiCreditRpc.getBalance(saasId);
    }

    /**
     * 查询员工 AI 积点限额（周期已过时按重置后口径返回）。
     *
     * @param saasId 租户
     * @param userId 员工
     * @return 员工限额（无行=未开通 AI 授权，data 为空）
     */
    public static ResponseData<AiCreditLimitVo> getUserCreditsLimit(long saasId, long userId) {
        return aiCreditRpc.getUserLimit(saasId, userId);
    }

    /**
     * 清零员工当前周期已用 AI 积点（立即重置）。
     *
     * @param saasId 租户
     * @param userId 员工
     * @return 重置后的限额
     */
    public static ResponseData<AiCreditLimitVo> resetUserCreditsLimit(long saasId, long userId) {
        return aiCreditRpc.resetUserLimit(saasId, userId);
    }

}
