package uw.tinyurl.client;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import uw.common.dto.ResponseData;
import uw.tinyurl.client.conf.UwTinyurlProperties;
import uw.tinyurl.client.vo.TinyurlParam;


/**
 * 短链帮助类。
 */
public class TinyurlClientHelper {

    private static final Logger log = LoggerFactory.getLogger( TinyurlClientHelper.class );
    /**
     * Rest模板类
     */
    private static RestTemplate tokenRestTemplate;

    private static UwTinyurlProperties uwTinyurlProperties;

    public TinyurlClientHelper(UwTinyurlProperties uwTinyurlProperties, RestTemplate tokenRestTemplate) {
        TinyurlClientHelper.tokenRestTemplate = tokenRestTemplate;
        TinyurlClientHelper.uwTinyurlProperties = uwTinyurlProperties;
    }

    /**
     * 推送web通知。
     *
     * @param tinyurlParam
     * @return
     */
    public static ResponseData<String> generate(TinyurlParam tinyurlParam) {
        return tokenRestTemplate.exchange( uwTinyurlProperties.getTinyurlCenterHost() + "/rpc/tinyurl/generate", HttpMethod.POST, new HttpEntity<>( tinyurlParam ),
                new ParameterizedTypeReference<ResponseData<String>>() {
        } ).getBody();
    }

}
