package uw.tinyurl.client;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.loadbalancer.ResponseData;
import org.springframework.web.client.RestTemplate;
import uw.tinyurl.client.conf.UwTinyurlProperties;
import uw.tinyurl.client.vo.TinyurlParam;


/**
 * 短链帮助类。
 */
public class TinyurlClient {

    private static final Logger log = LoggerFactory.getLogger( TinyurlClient.class );
    /**
     * Rest模板类
     */
    private RestTemplate restTemplate;

    private UwTinyurlProperties uwTinyurlProperties;

    public TinyurlClient(UwTinyurlProperties uwTinyurlProperties, RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.uwTinyurlProperties = uwTinyurlProperties;
    }

    /**
     * 推送web通知。
     *
     * @param tinyurlParam
     * @return
     */
    public ResponseData generate(TinyurlParam tinyurlParam) {
        ResponseData responseData = restTemplate.postForObject( uwTinyurlProperties.getTinyurlCenterHost() + "/rpc/tinyurl/generate", tinyurlParam, ResponseData.class );
        return responseData;
    }

}
