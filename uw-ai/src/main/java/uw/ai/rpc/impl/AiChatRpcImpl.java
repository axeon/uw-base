package uw.ai.rpc.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import uw.ai.conf.UwAiProperties;
import uw.ai.rpc.AiChatRpc;
import uw.ai.vo.AiChatGenerateParam;
import uw.common.dto.ResponseData;

/**
 * AiChatRpcImpl.
 */
public class AiChatRpcImpl implements AiChatRpc {


    private static final Logger logger = LoggerFactory.getLogger(AiChatRpcImpl.class);
    /**
     * Rest模板类
     */
    private final RestTemplate authRestTemplate;

    /**
     * WebClient实例，用于处理流式响应
     */
    private final WebClient authWebClient;

    /**
     * UwAi配置类
     */
    private final UwAiProperties uwAiProperties;

    public AiChatRpcImpl(UwAiProperties uwAiProperties, RestTemplate authRestTemplate, WebClient authWebClient) {
        this.authRestTemplate = authRestTemplate;
        this.uwAiProperties = uwAiProperties;
        this.authWebClient = authWebClient;
    }

    /**
     * 生成。
     *
     * @param param
     * @return
     */
    @Override
    public ResponseData<String> generate(AiChatGenerateParam param) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        // 普通参数直接添加（注意键名需与后端接口参数匹配）
        body.add("configId", param.getConfigId());
        if (param.getUserPrompt() != null) {
            body.add("userPrompt", param.getUserPrompt());
        }
        if (param.getSystemPrompt() != null) {
            body.add("systemPrompt", param.getSystemPrompt());
        }
        // 修复: 添加toolList参数
        if (param.getToolList() != null) {
            for (int i = 0; i < param.getToolList().size(); i++) {
                if (param.getToolList().get(i) != null) {
                    body.add("toolList[" + i + "]", param.getToolList().get(i));
                }
            }
        }

        // 添加toolContext参数
        if (param.getToolContext() != null) {
            for (String key : param.getToolContext().keySet()) {
                if (param.getToolContext().get(key) != null) {
                    body.add("toolContext['" + key + "']", param.getToolContext().get(key));
                }
            }
        }

        // 修复: 添加ragLibIds参数
        if (param.getRagLibIds() != null) {
            for (int i = 0; i < param.getRagLibIds().length; i++) {
                body.add("ragLibIds[" + i + "]", param.getRagLibIds()[i]);
            }
        }

        // 文件参数处理（关键修正点）
        if (param.getFileList() != null) {
            for (MultipartFile file : param.getFileList()) {
                // 直接添加MultipartFile对象，无需转换（Spring会自动处理）
                if (file != null) {
                    body.add("fileList", file); // 假设后端参数名是"fileList"
                }
            }
        }

        // 设置正确的Content-Type（Spring会自动添加multipart/form-data）
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA); // 显式声明类型

        // 构建请求实体
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        return authRestTemplate.exchange(uwAiProperties.getAiCenterHost() + "/rpc/chat/generate", HttpMethod.POST, requestEntity, new ParameterizedTypeReference<ResponseData<String>>() {
        }).getBody();
    }


    /**
     * 生成。
     *
     * @param param
     * @return
     */
    @Override
    public Flux<String> chatGenerate(AiChatGenerateParam param) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        // 普通参数直接添加（注意键名需与后端接口参数匹配）
        body.add("configId", param.getConfigId());
        if (StringUtils.isNotBlank(param.getUserPrompt())) {
            body.add("userPrompt", param.getUserPrompt());
        }
        if (StringUtils.isNotBlank(param.getSystemPrompt())) {
            body.add("systemPrompt", param.getSystemPrompt());
        }
        // 添加toolList参数
        if (param.getToolList() != null) {
            for (int i = 0; i < param.getToolList().size(); i++) {
                if (param.getToolList().get(i) != null) {
                    body.add("toolList[" + i + "]", param.getToolList().get(i));
                }
            }
        }

        // 添加toolContext参数
        if (param.getToolContext() != null) {
            for (String key : param.getToolContext().keySet()) {
                if (param.getToolContext().get(key) != null) {
                    body.add("toolContext['" + key + "']", param.getToolContext().get(key));
                }
            }
        }

        // 添加ragLibIds参数
        if (param.getRagLibIds() != null) {
            for (int i = 0; i < param.getRagLibIds().length; i++) {
                body.add("ragLibIds[" + i + "]", param.getRagLibIds()[i]);
            }
        }

        // 文件参数处理（关键修正点）
        if (param.getFileList() != null) {
            for (MultipartFile file : param.getFileList()) {
                // 直接添加MultipartFile对象，无需转换（Spring会自动处理）
                if (file != null) {
                    body.add("fileList", file); // 假设后端参数名是"fileList"
                }
            }
        }

        // 使用WebClient处理SSE流式响应
        return authWebClient.post()
                .uri(uwAiProperties.getAiCenterHost() + "/rpc/chat/chatGenerate")
//                .uri("http://localhost:10081/rpc/chat/chatGenerate")
                .contentType(MediaType.MULTIPART_FORM_DATA).
                body(BodyInserters.fromMultipartData(body))
                .accept(MediaType.TEXT_EVENT_STREAM)          // 1. 声明接受 SSE
                .retrieve()
                .bodyToFlux(String.class).doOnError(e -> {
                    logger.error("sse error", e);
                });
    }
}