package uw.ai.rpc.impl;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import uw.ai.conf.UwAiProperties;
import uw.ai.rpc.AiChatRpc;
import uw.ai.vo.AiChatGenerateParam;
import uw.common.dto.ResponseData;

/**
 * AiChatRpcImpl.
 */
public class AiChatRpcImpl implements AiChatRpc {


    /**
     * Rest模板类
     */
    private final RestTemplate authRestTemplate;

    private final UwAiProperties uwAiProperties;

    public AiChatRpcImpl(UwAiProperties uwAiProperties, RestTemplate authRestTemplate) {
        this.authRestTemplate = authRestTemplate;
        this.uwAiProperties = uwAiProperties;
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
        body.add( "configId", param.getConfigId() );
        body.add( "userPrompt", param.getUserPrompt() );
        body.add( "systemPrompt", param.getSystemPrompt() );
        body.add( "toolList", param.getToolList() );

        // 文件参数处理（关键修正点）
        if (param.getFileList() != null) {
            for (MultipartFile file : param.getFileList()) {
                // 直接添加MultipartFile对象，无需转换（Spring会自动处理）
                body.add( "fileList", file ); // 假设后端参数名是"fileList"
            }
        }

        // 设置正确的Content-Type（Spring会自动添加multipart/form-data）
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType( MediaType.MULTIPART_FORM_DATA ); // 显式声明类型

        // 构建请求实体
        HttpEntity<MultiValueMap<String, Object>> requestEntity =
                new HttpEntity<>( body, headers );

        return authRestTemplate.exchange(
                uwAiProperties.getAiCenterHost() + "/rpc/chat/generate",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<ResponseData<String>>() {
                }
        ).getBody();
    }
}
