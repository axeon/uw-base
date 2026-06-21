package uw.ai.rpc;

import reactor.core.publisher.Flux;
import uw.ai.vo.AiChatGenerateParam;
import uw.common.response.ResponseData;

/**
 * AI 对话生成 RPC 接口。
 * <p>
 * 实现类（{@link uw.ai.rpc.impl.AiChatRpcImpl}）通过 RestClient/WebClient 调用 AI 服务中心。
 */
public interface AiChatRpc {


    /**
     * 同步生成对话响应数据。
     *
     * @param param 对话生成参数
     * @return 完整响应文本
     */
    ResponseData<String> generate(AiChatGenerateParam param);


    /**
     * 流式生成对话响应数据（SSE）。
     *
     * @param param 对话生成参数
     * @return 文本片段流
     */
    Flux<String> chatGenerate(AiChatGenerateParam param);

}
