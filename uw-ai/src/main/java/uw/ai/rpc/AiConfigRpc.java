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
     * 获取所有可用的模型配置列表。
     *
     * @return 模型配置列表
     */
    ResponseData<List<AiModelInfoVo>> listModelConfig();

    /**
     * 获取所有可用的API连接配置列表。
     *
     * @return ResponseData
     */
    ResponseData<List<AiModelApiVo>> listApiConfig();

}
