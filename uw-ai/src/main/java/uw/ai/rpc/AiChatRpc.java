package uw.ai.rpc;

import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
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


    /**
     * 生成流式响应数据。
     *
     * @param param param
     * @return ResponseData
     */
    Flux<String> chatGenerate(AiChatGenerateParam param);

}
