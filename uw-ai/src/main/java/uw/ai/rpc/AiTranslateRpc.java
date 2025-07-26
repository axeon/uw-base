package uw.ai.rpc;

import org.springframework.web.bind.annotation.RequestBody;
import uw.ai.vo.AiTranslateListParam;
import uw.ai.vo.AiTranslateMapParam;
import uw.ai.vo.AiTranslateResultData;
import uw.common.dto.ResponseData;

public interface AiTranslateRpc {
    /**
     * 翻译列表。
     */
    ResponseData<AiTranslateResultData[]> translateList(@RequestBody AiTranslateListParam param);

    /**
     * 翻译Map。
     */
    ResponseData<AiTranslateResultData[]> translateMap(@RequestBody AiTranslateMapParam param);
}
