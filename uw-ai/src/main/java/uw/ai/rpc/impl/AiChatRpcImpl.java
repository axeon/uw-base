package uw.ai.rpc.impl;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uw.ai.conf.UwAiProperties;
import uw.ai.rpc.AiChatRpc;
import uw.ai.util.BeanOutputConverter;
import uw.ai.vo.AiChatGenerateParam;
import uw.common.dto.ResponseData;

@Component
public class AiChatRpcImpl implements AiChatRpc {

    /**
     * 生成系统提示.
     */
    private static final String ENTITY_SYSTEM_PROMPT = """
            Your response should be in JSON format.
            Do not include any explanations, only provide a RFC8259 compliant JSON response following this format without deviation.
            Do not include markdown code blocks in your response.
            Remove the ```json markdown from the output.
            Here is the JSON Schema instance your output must adhere to:
            ```%s```
            """;

    /**
     * Rest模板类
     */
    private final RestTemplate tokenRestTemplate;

    private final UwAiProperties uwAiProperties;

    public AiChatRpcImpl(RestTemplate tokenRestTemplate, UwAiProperties uwAiProperties) {
        this.tokenRestTemplate = tokenRestTemplate;
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
        String url = uwAiProperties.getAiCenterHost() + "/rpc/chat/generate";
        return tokenRestTemplate.exchange( url, HttpMethod.POST, new HttpEntity<>( param ), new ParameterizedTypeReference<ResponseData<String>>() {
        } ).getBody();
    }

    public <T> ResponseData<T> generateEntity(AiChatGenerateParam param, Class<T> clazz) {
        // 设置类型转换器
        BeanOutputConverter<T> beanOutputConverter = new BeanOutputConverter<>( clazz );
        // 设置系统提示
        StringBuilder systemPrompt = new StringBuilder();
        if (param.getSystemPrompt() != null) {
            systemPrompt.append( param.getSystemPrompt() );
            systemPrompt.append( "\n" );
        }
        systemPrompt.append( String.format( ENTITY_SYSTEM_PROMPT, beanOutputConverter.getFormat() ) );
        param.setSystemPrompt( systemPrompt.toString() );
        // 调用生成
        ResponseData<String> responseData = generate( param );
        if (responseData.isNotSuccess()) {
            return responseData.prototype();
        }
        // 转换成实体
        return ResponseData.success( beanOutputConverter.convert( responseData.getData() ) );
    }

}
