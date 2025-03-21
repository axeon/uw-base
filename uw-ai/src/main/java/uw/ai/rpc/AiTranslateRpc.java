package uw.ai.rpc;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import uw.ai.vo.AiTranslateListParam;
import uw.ai.vo.AiTranslateMapParam;
import uw.ai.vo.AiTranslateResultData;
import uw.auth.service.annotation.MscPermDeclare;
import uw.auth.service.constant.UserType;
import uw.common.dto.ResponseData;

public interface AiTranslateRpc {
    /**
     * 翻译列表。
     */
    @PostMapping("/translateList")
    @Operation(summary = "翻译列表", description = "翻译列表")
    @MscPermDeclare(user = UserType.RPC)
    ResponseData<AiTranslateResultData[]> translateList(@RequestBody AiTranslateListParam param);

    /**
     * 翻译Map。
     */
    @PostMapping("/translateMap")
    @Operation(summary = "翻译Map", description = "翻译Map")
    @MscPermDeclare(user = UserType.RPC)
    ResponseData<AiTranslateResultData[]> translateMap(@RequestBody AiTranslateMapParam param);
}
