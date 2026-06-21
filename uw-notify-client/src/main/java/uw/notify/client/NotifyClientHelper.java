package uw.notify.client;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import uw.common.response.ResponseData;
import uw.notify.client.conf.UwNotifyProperties;
import uw.notify.client.vo.WebNotifyMsg;

/**
 * Web 通知推送客户端 Helper。
 * <p>
 * 通过 HTTP RPC 调用 notify-center 服务，向指定的单个在线用户推送 Web 通知消息
 * （当前不支持广播，{@code WebNotifyMsg.userId} 必须 &gt; 0）。
 * 推送的消息经 notify-center 转发后，最终通过 SSE（Server-Sent Events）等实时通道下发给前端。
 * 所有方法均为静态方法，由 {@link uw.notify.client.conf.UwNotifyAutoConfiguration}
 * 在启动时通过依赖注入完成对内部静态 {@code RestClient} 与配置的初始化，
 * 业务方无需持有 Bean 实例即可直接调用。
 * <p>
 * 共享的 {@link RestClient}（命名为 {@code authRestClient}）由 {@code uw-auth-client}
 * 提供，内置鉴权拦截器，调用 notify-center 时自动携带服务间鉴权信息。
 *
 * @author axeon
 * @see WebNotifyMsg
 */
public class NotifyClientHelper {

    private static final Logger log = LoggerFactory.getLogger(NotifyClientHelper.class);

    /**
     * 用于发起 RPC 调用的共享 RestClient（由 uw-auth-client 提供，带鉴权拦截器）。
     */
    private static RestClient authRestClient;

    /**
     * notify-center 连接配置。
     */
    private static UwNotifyProperties uwNotifyProperties;

    /**
     * 构造方法，由自动装配调用以完成静态字段的依赖注入。
     * <p>
     * 该设计为有意为之：通过 Spring 实例化完成对静态字段的注入，配合静态工具方法对外暴露能力，
     * 使业务方无需持有 Helper Bean 即可调用。单 ApplicationContext 下由 {@code @ConditionalOnMissingBean}
     * 保证唯一初始化。
     *
     * @param uwNotifyProperties notify-center 连接配置
     * @param authRestClient     共享的带鉴权 RestClient
     */
    public NotifyClientHelper(UwNotifyProperties uwNotifyProperties, RestClient authRestClient) {
        NotifyClientHelper.authRestClient = authRestClient;
        NotifyClientHelper.uwNotifyProperties = uwNotifyProperties;
    }

    /**
     * 推送 Web 通知消息。
     * <p>
     * 通过 POST JSON 调用 notify-center 的 {@code /rpc/notify/pushNotify} 接口。
     * 参数在调用前做前置校验，{@code webNotifyMsg} 为 null 时直接返回 ERROR，不会发起 RPC。
     * RPC 调用阶段的任何异常（网络、序列化、HTTP 4xx/5xx）均被捕获并以 {@link ResponseData#errorMsg} 返回，
     * 不会向外抛出。
     *
     * @param webNotifyMsg 通知消息体（不可为 null，{@code userId} 必须 &gt; 0）
     * @return 推送结果：SUCCESS=推送成功 / ERROR=参数非法或推送失败（使用固定文案，异常细节仅入日志）
     */
    public static ResponseData<Void> pushNotify(WebNotifyMsg webNotifyMsg) {
        if (webNotifyMsg == null) {
            return ResponseData.errorMsg("webNotifyMsg不能为空！");
        }
        String targetUrl = uwNotifyProperties.getNotifyCenterHost() + "/rpc/notify/pushNotify";
        try {
            ResponseData<Void> result = authRestClient.post()
                    .uri(targetUrl)
                    .body(webNotifyMsg)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ResponseData<Void>>() {});
            if (result == null) {
                return ResponseData.errorMsg("NotifyClientHelper.pushNotify() returned null body");
            }
            return result;
        } catch (Exception e) {
            // 详细原因只进日志（可能含序列化/网络内部关键字），对外用固定文案，避免信息泄露。
            log.error("NotifyClientHelper.pushNotify()异常: {}", e.getMessage(), e);
            return ResponseData.errorMsg("推送通知失败，请稍后重试。");
        }
    }

}
