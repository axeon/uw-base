package uw.ai.rpc;

import uw.ai.vo.AiApiConfigVo;
import uw.ai.vo.AiModelConfigVo;
import uw.common.dto.ResponseData;

import java.util.List;

/**
 * AI配置查询RPC接口。
 */
public interface AiConfigRpc {

    /**
     * 获取所有可用的模型配置列表。
     *
     * @return ResponseData
     */
    ResponseData<List<AiModelConfigVo>> listModelConfig();

    /**
     * 获取所有可用的API连接配置列表。
     *
     * @return ResponseData
     */
    ResponseData<List<AiApiConfigVo>> listApiConfig();

}
