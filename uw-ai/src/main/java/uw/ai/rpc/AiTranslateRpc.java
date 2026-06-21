package uw.ai.rpc;

import org.springframework.web.bind.annotation.RequestBody;
import uw.ai.vo.AiTranslateListParam;
import uw.ai.vo.AiTranslateMapParam;
import uw.ai.vo.AiTranslateResultData;
import uw.common.response.ResponseData;

/**
 * AI 翻译 RPC 接口。
 * <p>
 * 实现类（{@link uw.ai.rpc.impl.AiTranslateRpcImpl}）通过 RestClient 调用 AI 服务中心，
 * 支持按列表和 Map 两种模式批量翻译，每个目标语言返回一条 {@link AiTranslateResultData}。
 */
public interface AiTranslateRpc {
    /**
     * 按列表翻译：翻译 {@code param.textList} 中的文本到 {@code param.langList} 指定的各目标语言。
     *
     * @param param 列表翻译参数
     * @return 翻译结果数组，每个元素对应一个目标语言
     */
    ResponseData<AiTranslateResultData[]> translateList(@RequestBody AiTranslateListParam param);

    /**
     * 按 Map 翻译：翻译 {@code param.textMap} 中的 value（key 作为变量名保留）到各目标语言。
     *
     * @param param Map 翻译参数
     * @return 翻译结果数组，每个元素对应一个目标语言
     */
    ResponseData<AiTranslateResultData[]> translateMap(@RequestBody AiTranslateMapParam param);
}
