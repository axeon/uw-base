package uw.notify.client;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import uw.common.response.ResponseData;
import uw.notify.client.conf.UwNotifyProperties;
import uw.notify.client.vo.WebNotifyMsg;

/**
 * Web通知Helper。
 */
public class NotifyClientHelper {

    private static final Logger log = LoggerFactory.getLogger(NotifyClientHelper.class);

    private static RestClient authRestClient;

    private static UwNotifyProperties uwNotifyProperties;

    public NotifyClientHelper(UwNotifyProperties uwNotifyProperties, RestClient authRestClient) {
        NotifyClientHelper.authRestClient = authRestClient;
        NotifyClientHelper.uwNotifyProperties = uwNotifyProperties;
    }

    /**
     * 推送Web通知消息。
     *
     * @param webNotifyMsg 通知消息
     * @return 推送结果
     */
    public static ResponseData pushNotify(WebNotifyMsg webNotifyMsg) {
        String targetUrl = uwNotifyProperties.getNotifyCenterHost() + "/rpc/notify/pushNotify";
        try {
            ResponseData result = authRestClient.post()
                    .uri(targetUrl)
                    .body(webNotifyMsg)
                    .retrieve()
                    .body(ResponseData.class);
            if (result == null) {
                return ResponseData.errorMsg("NotifyClientHelper.pushNotify() returned null body");
            }
            return result;
        } catch (Exception e) {
            log.error("NotifyClientHelper.pushNotify()异常: {}", e.getMessage(), e);
            return ResponseData.errorMsg("NotifyClientHelper.pushNotify()异常: " + e.getMessage());
        }
    }

}
