package uw.ai.rpc;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Flux;
import uw.ai.vo.AiChatGenerateParam;
import uw.ai.vo.AiChatMsgParam;
import uw.ai.vo.AiChatSessionParam;
import uw.auth.service.annotation.MscPermDeclare;
import uw.auth.service.constant.UserType;
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
