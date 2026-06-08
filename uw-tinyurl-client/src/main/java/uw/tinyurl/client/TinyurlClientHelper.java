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

    private static final Logger log = LoggerFactory.getLogger(TinyurlClientHelper.class);
    /**
     * Rest模板类
     */
    private static RestTemplate authRestTemplate;

    private static UwTinyurlProperties uwTinyurlProperties;

    public TinyurlClientHelper(UwTinyurlProperties uwTinyurlProperties, RestTemplate authRestTemplate) {
        TinyurlClientHelper.authRestTemplate = authRestTemplate;
        TinyurlClientHelper.uwTinyurlProperties = uwTinyurlProperties;
    }

    /**
     * 生成短链。
     *
     * @param tinyurlParam
     * @return
     */
    public static ResponseData<String> generate(TinyurlParam tinyurlParam) {
        String targetUrl = uwTinyurlProperties.getTinyurlCenterHost() + "/rpc/tinyurl/generate";
        try {
            ResponseData<String> result = authRestTemplate.exchange(targetUrl, HttpMethod.POST, new HttpEntity<>(tinyurlParam),
                    new ParameterizedTypeReference<ResponseData<String>>() {
                    }).getBody();
            if (result == null) {
                return ResponseData.errorMsg("TinyurlClientHelper.generate() returned null body");
            }
            return result;
        } catch (Exception e) {
            log.error("TinyurlClientHelper.generate()异常: {}", e.getMessage(), e);
            return ResponseData.errorMsg("TinyurlClientHelper.generate()异常: " + e.getMessage());
        }
    }

}
