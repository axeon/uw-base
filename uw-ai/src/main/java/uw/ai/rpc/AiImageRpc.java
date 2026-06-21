package uw.ai.rpc;

import uw.ai.vo.AiImageGenerateParam;
import uw.ai.vo.AiImageResultData;
import uw.common.response.ResponseData;

/**
 * AI 图片生成 RPC 接口。
 * <p>
 * 实现类（{@link uw.ai.rpc.impl.AiImageRpcImpl}）通过 RestClient 调用 AI 服务中心生成图片。
 */
public interface AiImageRpc {

    /**
     * 生成图片。
     *
     * @param param 图片生成参数
     * @return 图片生成结果
     */
    ResponseData<AiImageResultData> generate(AiImageGenerateParam param);

}
