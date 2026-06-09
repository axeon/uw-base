package uw.ai.rpc.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import uw.ai.conf.UwAiProperties;
import uw.ai.rpc.AiChatRpc;
import uw.ai.vo.AiChatGenerateParam;
import uw.common.response.ResponseData;

/**
 * AiChatRpcImpl.
 */
public class AiChatRpcImpl implements AiChatRpc {


    private static final Logger logger = LoggerFactory.getLogger(AiChatRpcImpl.class);

    /**
     * 带认证拦截器的RestClient。
     */
    private final RestClient authRestClient;

    /**
     * 带认证过滤器的WebClient（用于SSE流式响应）。
     */
    private final WebClient authWebClient;

    /**
     * 属性配置器
     */
    private final UwAiProperties uwAiProperties;

    public AiChatRpcImpl(UwAiProperties uwAiProperties, RestClient authRestClient, WebClient authWebClient) {
        this.authRestClient = authRestClient;
        this.uwAiProperties = uwAiProperties;
        this.authWebClient = authWebClient;
    }

    /**
     * AI对话生成。
     *
     * @param param 对话生成参数
     * @return 生成结果
     */
    @Override
    public ResponseData<String> generate(AiChatGenerateParam param) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("configId", param.getConfigId());
        if (param.getUserPrompt() != null) {
            body.add("userPrompt", param.getUserPrompt());
        }
        if (param.getSystemPrompt() != null) {
            body.add("systemPrompt", param.getSystemPrompt());
        }
        if (param.getToolList() != null) {
            for (int i = 0; i < param.getToolList().size(); i++) {
                if (param.getToolList().get(i) != null) {
                    body.add("toolList[" + i + "]", param.getToolList().get(i));
                }
            }
        }

        if (param.getToolContext() != null) {
            for (String key : param.getToolContext().keySet()) {
                if (param.getToolContext().get(key) != null) {
                    body.add("toolContext['" + key + "']", param.getToolContext().get(key));
                }
            }
        }

        if (param.getRagLibIds() != null) {
            for (int i = 0; i < param.getRagLibIds().length; i++) {
                body.add("ragLibIds[" + i + "]", param.getRagLibIds()[i]);
            }
        }

        if (param.getFileList() != null) {
            for (MultipartFile file : param.getFileList()) {
                if (file != null) {
                    body.add("fileList", file);
                }
            }
        }

        ResponseData<String> result = authRestClient.post()
                .uri(uwAiProperties.getAiCenterHost() + "/rpc/chat/generate")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<ResponseData<String>>() {});
        if (result == null) {
            return ResponseData.errorMsg("AiChatRpcImpl.generate() returned null body");
        }
        return result;
    }


    /**
     * AI对话流式生成。
     *
     * @param param 对话生成参数
     * @return SSE流式响应
     */
    @Override
    public Flux<String> chatGenerate(AiChatGenerateParam param) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("configId", param.getConfigId());
        if (StringUtils.isNotBlank(param.getUserPrompt())) {
            body.add("userPrompt", param.getUserPrompt());
        }
        if (StringUtils.isNotBlank(param.getSystemPrompt())) {
            body.add("systemPrompt", param.getSystemPrompt());
        }
        if (param.getToolList() != null) {
            for (int i = 0; i < param.getToolList().size(); i++) {
                if (param.getToolList().get(i) != null) {
                    body.add("toolList[" + i + "]", param.getToolList().get(i));
                }
            }
        }

        if (param.getToolContext() != null) {
            for (String key : param.getToolContext().keySet()) {
                if (param.getToolContext().get(key) != null) {
                    body.add("toolContext['" + key + "']", param.getToolContext().get(key));
                }
            }
        }

        if (param.getRagLibIds() != null) {
            for (int i = 0; i < param.getRagLibIds().length; i++) {
                body.add("ragLibIds[" + i + "]", param.getRagLibIds()[i]);
            }
        }

        if (param.getFileList() != null) {
            for (MultipartFile file : param.getFileList()) {
                if (file != null) {
                    body.add("fileList", file);
                }
            }
        }

        return authWebClient.post()
                .uri(uwAiProperties.getAiCenterHost() + "/rpc/chat/chatGenerate")
                .contentType(MediaType.MULTIPART_FORM_DATA).
                body(BodyInserters.fromMultipartData(body))
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class).doOnError(e -> {
                    logger.error("sse error", e);
                });
    }
}