package uw.ai.rpc;

import uw.ai.vo.AiModelApiVo;
import uw.ai.vo.AiModelInfoVo;
import uw.common.response.ResponseData;

import java.util.List;

/**
 * AI配置查询RPC接口。
 */
public interface AiConfigRpc {

    /**
     * 根据saas信息获取模型配置列表。
     *
     * @param saasId 租户ID
     * @param mchId  商户ID（可为null或0，表示不限商户）
     * @return 模型配置列表
     */
    ResponseData<List<AiModelInfoVo>> listModelConfigBySaas(Long saasId, Long mchId);

    /**
     * 根据API配置ID获取模型配置列表。
     *
     * @param apiId API配置ID
     * @return 模型配置列表
     */
    ResponseData<List<AiModelInfoVo>> listModelConfigByApi(Long apiId);

    /**
     * 根据ID获取模型配置。
     *
     * @param id 模型配置ID
     * @return 模型配置
     */
    ResponseData<AiModelInfoVo> getModelConfigById(Long id);

    /**
     * 根据配置代码获取模型配置。
     *
     * @param configCode 配置代码
     * @return 模型配置
     */
    ResponseData<AiModelInfoVo> getModelConfigByCode(String configCode);

    /**
     * 根据模型类型和标签获取模型配置列表。
     *
     * @param modelType 模型类型
     * @param modelTag  模型能力标签（可为null或空，表示不限标签）
     * @return 模型配置列表
     */
    ResponseData<List<AiModelInfoVo>> listModelConfigByType(String modelType, String modelTag);

    /**
     * 根据saas信息获取API连接配置列表。
     *
     * @param saasId 租户ID
     * @param mchId  商户ID（可为null或0，表示不限商户）
     * @return API连接配置列表
     */
    ResponseData<List<AiModelApiVo>> listApiConfigBySaas(Long saasId, Long mchId);

    /**
     * 根据ID获取API连接配置。
     *
     * @param id API配置ID
     * @return API连接配置
     */
    ResponseData<AiModelApiVo> getApiConfigById(Long id);

    /**
     * 根据配置代码获取API连接配置。
     *
     * @param apiCode 配置代码
     * @return API连接配置
     */
    ResponseData<AiModelApiVo> getApiConfigByCode(String apiCode);
}
