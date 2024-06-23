package uw.notify.client;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;
import uw.common.dto.ResponseData;
import uw.notify.client.conf.UwNotifyProperties;
import uw.notify.client.vo.WebNotifyMsg;

/**
 * Web通知Helper。
 */
public class WebNotifyClient {

    private static final Logger log = LoggerFactory.getLogger( WebNotifyClient.class );
    /**
     * Rest模板类
     */
    private RestTemplate restTemplate;

    private UwNotifyProperties uwNotifyProperties;

    public WebNotifyClient(UwNotifyProperties uwNotifyProperties, RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.uwNotifyProperties = uwNotifyProperties;
    }

    /**
     * 推送web通知。
     *
     * @param webNotifyMsg
     * @return
     */
    public ResponseData pushNotify(WebNotifyMsg webNotifyMsg) {
        ResponseData responseData = null;
        try {
            responseData = restTemplate.postForObject( uwNotifyProperties.getNotifyCenterHost() + "/rpc/webNotify/pushMsg", webNotifyMsg, ResponseData.class );
        } catch (Exception e) {
            log.error( "WebNotifyHelper推送消息异常: {}", e.getMessage(), e );
            return ResponseData.errorMsg( e.getMessage() );
        }
        return responseData;
    }

}
