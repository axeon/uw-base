package uw.ai.rpc;

import uw.ai.vo.AiChatGenerateParam;
import uw.common.dto.ResponseData;

/**
 * AiChatRpc.
 */
public interface AiChatRpc {


    /**
     * 生成响应数据。
     *
     * @param param param
     * @return ResponseData
     */
    ResponseData<String> generate(AiChatGenerateParam param);

}
