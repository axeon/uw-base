package uw.ai.rpc;

import uw.ai.vo.AiChatGenerateParam;
import uw.common.dto.ResponseData;

/**
 * AiChatRpc.
 */
public interface AiChatRpc {


    /**
     * 更新工具元数据。
     *
     * @param param
     * @return
     */
    ResponseData<String> generate(AiChatGenerateParam param);
}
