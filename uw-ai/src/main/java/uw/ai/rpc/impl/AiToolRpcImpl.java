package uw.ai.rpc.impl;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uw.ai.conf.UwAiProperties;
import uw.ai.rpc.AiToolRpc;
import uw.ai.vo.AiToolMeta;
import uw.common.dto.ResponseData;

import java.net.URI;
import java.util.List;

/**
 * AiToolRpcImpl.
 */
public class AiToolRpcImpl implements AiToolRpc {

    /**
     * Rest模板类
     */
    private final RestTemplate authRestTemplate;

    private final UwAiProperties uwAiProperties;

    public AiToolRpcImpl(UwAiProperties uwAiProperties, RestTemplate authRestTemplate) {
        this.authRestTemplate = authRestTemplate;
        this.uwAiProperties = uwAiProperties;
    }

    /**
     * 获取工具元数据。
     *
     * @param appName
     * @return
     */
    @Override
    public ResponseData<List<AiToolMeta>> listToolMeta(String appName) {
        URI targetUrl =
                UriComponentsBuilder.fromUriString( uwAiProperties.getAiCenterHost() ).path( "/rpc/tool/listToolMeta" ).queryParam( "appName", appName ).build().encode().toUri();
        return authRestTemplate.exchange( targetUrl, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<ResponseData<List<AiToolMeta>>>() {
        } ).getBody();
    }

    /**
     * 更新工具元数据。
     *
     * @param aiToolMeta
     * @return
     */
    @Override
    public ResponseData updateToolMeta(AiToolMeta aiToolMeta) {
        String url = uwAiProperties.getAiCenterHost() + "/rpc/tool/updateToolMeta";
        return authRestTemplate.postForObject( url, aiToolMeta, ResponseData.class );
    }
}
