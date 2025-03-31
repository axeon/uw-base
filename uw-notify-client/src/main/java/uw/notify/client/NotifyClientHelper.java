package uw.notify.client;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import uw.common.dto.ResponseData;
import uw.notify.client.conf.UwNotifyProperties;
import uw.notify.client.vo.WebNotifyMsg;

/**
 * Web通知Helper。
 */
public class NotifyClientHelper {

    private static final Logger log = LoggerFactory.getLogger( NotifyClientHelper.class );
    /**
     * Rest模板类
     */
    private static RestTemplate authRestTemplate;

    private static UwNotifyProperties uwNotifyProperties;

    public NotifyClientHelper(UwNotifyProperties uwNotifyProperties, RestTemplate authRestTemplate) {
        NotifyClientHelper.authRestTemplate = authRestTemplate;
        NotifyClientHelper.uwNotifyProperties = uwNotifyProperties;
    }

    /**
     * 推送web通知。
     *
     * @param webNotifyMsg
     * @return
     */
    public static ResponseData pushNotify(WebNotifyMsg webNotifyMsg) {
        ResponseData responseData = null;
        try {
            responseData = authRestTemplate.exchange( uwNotifyProperties.getNotifyCenterHost() + "/rpc/notify/pushNotify", HttpMethod.POST, new HttpEntity<>( webNotifyMsg ),
                    ResponseData.class ).getBody();
        } catch (Exception e) {
            log.error( "WebNotifyHelper推送消息异常: {}", e.getMessage(), e );
            return ResponseData.errorMsg( "WebNotifyHelper推送消息异常: " + e.getMessage() );
        }
        return responseData;

    }

}
