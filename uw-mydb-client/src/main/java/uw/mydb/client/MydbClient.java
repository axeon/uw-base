package uw.mydb.client;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.loadbalancer.ResponseData;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uw.mydb.client.conf.UwMydbClientProperties;

import java.io.Serializable;
import java.net.URI;

/**
 * MydbHelper。
 */
public class MydbClient {

    private static final Logger log = LoggerFactory.getLogger( MydbClient.class );

    /**
     * 默认配置。
     */
    public static final String DEFAULT_CONFIG = "default";

    /**
     * Rest模板类
     */
    private RestTemplate restTemplate;

    private UwMydbClientProperties uwNotifyProperties;

    public MydbClient(UwMydbClientProperties uwNotifyProperties, RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.uwNotifyProperties = uwNotifyProperties;
    }

    /**
     * 分配saas节点。
     * @param saasId
     * @return
     */
    public ResponseData assignSaasNode(Serializable saasId) {
        return assignSaasNode( DEFAULT_CONFIG,saasId );
    }

    /**
     * 分配saas节点。
     * @param configKey
     * @param saasId
     * @return
     */
    public ResponseData assignSaasNode(String configKey, Serializable saasId) {
        URI targetUrl = UriComponentsBuilder.fromHttpUrl( uwNotifyProperties.getMydbCenterHost() ).path( "/rpc/app/assignSaasNode" )
                .queryParam( "configKey", configKey ).queryParam( "saasId", saasId ).build().encode().toUri();
        return restTemplate.exchange( targetUrl, HttpMethod.POST, HttpEntity.EMPTY, ResponseData.class ).getBody();
    }

}
