package uw.tinyurl.client;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import uw.common.response.ResponseData;
import uw.tinyurl.client.conf.UwTinyurlProperties;
import uw.tinyurl.client.vo.TinyurlParam;


/**
 * 短链接生成客户端 Helper。
 * <p>
 * 通过 HTTP RPC 调用 tinyurl-center 服务生成长链接对应的短链接，支持密语保护与过期时间。
 * 所有方法均为静态方法，由 {@link uw.tinyurl.client.conf.UwTinyurlAutoConfiguration}
 * 在启动时通过依赖注入完成对内部静态 {@code RestClient} 与配置的初始化，
 * 业务方无需持有 Bean 实例即可直接调用。
 * <p>
 * 共享的 {@link RestClient}（命名为 {@code authRestClient}）由 {@code uw-auth-client}
 * 提供，内置鉴权拦截器，调用 tinyurl-center 时自动携带服务间鉴权信息。
 *
 * @author axeon
 * @see TinyurlParam
 */
public class TinyurlClientHelper {

    private static final Logger log = LoggerFactory.getLogger(TinyurlClientHelper.class);

    /**
     * 用于发起 RPC 调用的共享 RestClient（由 uw-auth-client 提供，带鉴权拦截器）。
     */
    private static RestClient authRestClient;

    /**
     * tinyurl-center 连接配置。
     */
    private static UwTinyurlProperties uwTinyurlProperties;

    /**
     * 构造方法，由自动装配调用以完成静态字段的依赖注入。
     * <p>
     * 该设计为有意为之：通过 Spring 实例化完成对静态字段的注入，配合静态工具方法对外暴露能力，
     * 使业务方无需持有 Helper Bean 即可调用。单 ApplicationContext 下由 {@code @ConditionalOnMissingBean}
     * 保证唯一初始化。
     *
     * @param uwTinyurlProperties tinyurl-center 连接配置
     * @param authRestClient      共享的带鉴权 RestClient
     */
    public TinyurlClientHelper(UwTinyurlProperties uwTinyurlProperties, RestClient authRestClient) {
        TinyurlClientHelper.authRestClient = authRestClient;
        TinyurlClientHelper.uwTinyurlProperties = uwTinyurlProperties;
    }

    /**
     * 生成短链接。
     * <p>
     * 通过 POST JSON 调用 tinyurl-center 的 {@code /rpc/tinyurl/generate} 接口，
     * 返回 {@code ResponseData<String>}，其 {@code data} 为生成的短链接码。
     * 参数在调用前做前置校验，任一必填项不合法直接返回 ERROR，不会发起 RPC：
     * {@code tinyurlParam} 不可为 null，且其 {@code url} 不可为空。
     * RPC 调用阶段的任何异常（网络、序列化、HTTP 4xx/5xx）均被捕获并以 {@link ResponseData#errorMsg} 返回，
     * 不会向外抛出。
     *
     * @param tinyurlParam 短链接生成参数（不可为 null，且 {@code url} 必填）
     * @return 生成结果：SUCCESS=生成成功，{@code data} 为短链码 / ERROR=参数非法或生成失败（含异常信息）
     */
    public static ResponseData<String> generate(TinyurlParam tinyurlParam) {
        if (tinyurlParam == null) {
            return ResponseData.errorMsg("tinyurlParam不能为空！");
        }
        if (tinyurlParam.getUrl() == null || tinyurlParam.getUrl().isEmpty()) {
            return ResponseData.errorMsg("url不能为空！");
        }
        String targetUrl = uwTinyurlProperties.getTinyurlCenterHost() + "/rpc/tinyurl/generate";
        try {
            ResponseData<String> result = authRestClient.post()
                    .uri(targetUrl)
                    .body(tinyurlParam)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ResponseData<String>>() {});
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
