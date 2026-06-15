package uw.ai.rpc;

import uw.ai.vo.AiImageGenerateParam;
import uw.ai.vo.AiImageResultData;
import uw.common.response.ResponseData;

/**
 * AI图片生成RPC接口。
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
