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
public class MydbClientHelper {

    private static final Logger log = LoggerFactory.getLogger( MydbClientHelper.class );
    /**
     * 默认配置。
     */
    private static final String DEFAULT_CONFIG = "default";

    /**
     * Rest模板类
     */
    private static RestTemplate restTemplate;

    private static UwMydbClientProperties uwMydbClientProperties;

    public MydbClientHelper(UwMydbClientProperties uwMydbClientProperties, RestTemplate restTemplate) {
        MydbClientHelper.uwMydbClientProperties = uwMydbClientProperties;
        MydbClientHelper.restTemplate = restTemplate;
    }

    /**
     * 分配saas节点。
     *
     * @param saasId
     * @return
     */
    public static ResponseData assignSaasNode(Serializable saasId) {
        return assignSaasNode( DEFAULT_CONFIG, saasId, null );
    }


    /**
     * 分配saas节点。
     * 返回值含义。
     * success: 正常创建节点。
     * warn: 系统已存在节点。
     * error: 创建失败。
     *
     * @param saasId
     * @param preferNode 预设节点名。
     * @return
     */
    public static ResponseData assignSaasNode(Serializable saasId, String preferNode) {
        return assignSaasNode( DEFAULT_CONFIG, saasId, preferNode );
    }

    /**
     * 分配saas节点。
     * 返回值含义。
     * success: 正常创建节点。
     * warn: 系统已存在节点。
     * error: 创建失败。
     *
     * @param configKey
     * @param saasId
     * @param preferNode 预设节点名。
     * @return
     */
    public static ResponseData assignSaasNode(String configKey, Serializable saasId, String preferNode) {
        URI targetUrl =
                UriComponentsBuilder.fromHttpUrl( uwMydbClientProperties.getMydbCenterHost() ).path( "/rpc/app/assignSaasNode" ).queryParam( "configKey", configKey ).queryParam(
                        "saasId", saasId ).queryParam( "preferNode", preferNode ).build().encode().toUri();
        return restTemplate.exchange( targetUrl, HttpMethod.POST, HttpEntity.EMPTY, ResponseData.class ).getBody();
    }

}
