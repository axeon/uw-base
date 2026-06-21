package uw.gateway.client;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import uw.common.response.ResponseData;
import uw.gateway.client.conf.UwGatewayProperties;
import uw.gateway.client.vo.SaasRateLimitParam;


/**
 * 网关管理客户端 Helper。
 * <p>
 * 通过 HTTP RPC 调用 gateway-center 服务，管理运营商（SAAS）的网关限速策略，
 * 包括设置限速参数与清除限速。所有方法均为静态方法，
 * 由 {@link uw.gateway.client.conf.UwGatewayAutoConfiguration} 在启动时通过依赖注入
 * 完成对内部静态 {@code RestClient} 与配置的初始化，业务方无需持有 Bean 实例即可直接调用。
 * <p>
 * 共享的 {@link RestClient}（命名为 {@code authRestClient}）由 {@code uw-auth-client}
 * 提供，内置鉴权拦截器，调用 gateway-center 时自动携带服务间鉴权信息。
 *
 * @author axeon
 */
public class GatewayClientHelper {

    private static final Logger log = LoggerFactory.getLogger(GatewayClientHelper.class);

    /**
     * 用于发起 RPC 调用的共享 RestClient（由 uw-auth-client 提供，带鉴权拦截器）。
     */
    private static RestClient authRestClient;

    /**
     * gateway-center 连接配置。
     */
    private static UwGatewayProperties uwGatewayProperties;

    /**
     * 构造方法，由自动装配调用以完成静态字段的依赖注入。
     * <p>
     * 该设计为有意为之：通过 Spring 实例化完成对静态字段的注入，配合静态工具方法对外暴露能力，
     * 使业务方无需持有 Helper Bean 即可调用。单 ApplicationContext 下由 {@code @ConditionalOnMissingBean}
     * 保证唯一初始化。
     *
     * @param uwGatewayProperties gateway-center 连接配置
     * @param authRestClient      共享的带鉴权 RestClient
     */
    public GatewayClientHelper(UwGatewayProperties uwGatewayProperties, RestClient authRestClient) {
        GatewayClientHelper.authRestClient = authRestClient;
        GatewayClientHelper.uwGatewayProperties = uwGatewayProperties;
    }

    /**
     * 设置（更新）指定运营商的网关限速策略。
     * <p>
     * 通过 PUT 调用 gateway-center 的 {@code /rpc/service/updateSaasRateLimit} 接口，请求体为 JSON。
     * 限速维度由 {@link SaasRateLimitParam#getLimitType()} 决定（裸值，对应服务端 MscAclRateLimitType 的 value），
     * {@code userType/userId} 仅在按用户维度限速时使用。参数在调用前做前置校验，任一必填项不合法
     * 直接返回 ERROR，不会发起 RPC：{@code param} 不可为 null，其 {@code saasId} 须大于 0、
     * {@code expireDate} 与 {@code remark} 不可为 null（服务端要求必填）。
     * RPC 调用阶段的任何异常（网络、序列化、HTTP 4xx/5xx）均被捕获并以 {@link ResponseData#errorMsg} 返回，
     * 不会向外抛出。
     *
     * @param param 限速策略参数（推荐通过 {@link SaasRateLimitParam#builder()} 构造）
     * @return 操作结果：SUCCESS=设置成功 / ERROR=参数非法或设置失败（含异常信息）
     */
    public static ResponseData<Void> updateSaasRateLimit(SaasRateLimitParam param) {
        if (param == null) {
            return ResponseData.errorMsg("param不能为空！");
        }
        if (param.getSaasId() <= 0) {
            return ResponseData.errorMsg("saasId不能为空！");
        }
        if (param.getExpireDate() == null) {
            return ResponseData.errorMsg("expireDate不能为空！");
        }
        if (param.getRemark() == null) {
            return ResponseData.errorMsg("remark不能为空！");
        }
        String targetUrl = uwGatewayProperties.getGatewayCenterHost() + "/rpc/service/updateSaasRateLimit";
        try {
            ResponseData<Void> result = authRestClient.put()
                    .uri(targetUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(param)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ResponseData<Void>>() {});
            if (result == null) {
                return ResponseData.errorMsg("GatewayClientHelper.updateSaasRateLimit() returned null body");
            }
            return result;
        } catch (Exception e) {
            log.error("GatewayClientHelper.updateSaasRateLimit()异常: {}", e.getMessage(), e);
            return ResponseData.errorMsg("GatewayClientHelper.updateSaasRateLimit()异常: " + e.getMessage());
        }
    }

    /**
     * 清除指定运营商的网关限速策略，恢复默认访问。
     * <p>
     * 通过 PUT 表单调用 gateway-center 的 {@code /rpc/service/clearSaasRateLimit} 接口。
     * 参数在调用前做前置校验，任一必填项不合法直接返回 ERROR，不会发起 RPC：
     * {@code saasId} 须大于 0、{@code remark} 不可为 null（服务端要求必填）。
     * RPC 调用阶段的任何异常（网络、序列化、HTTP 4xx/5xx）均被捕获并以 {@link ResponseData#errorMsg} 返回，
     * 不会向外抛出。
     *
     * @param saasId 运营商 ID（须大于 0）
     * @param remark 备注信息（必填，不可为 null）
     * @return 操作结果：SUCCESS=清除成功 / ERROR=参数非法或清除失败（含异常信息）
     */
    public static ResponseData<Void> clearSaasRateLimit(long saasId, String remark) {
        if (saasId <= 0) {
            return ResponseData.errorMsg("saasId不能为空！");
        }
        if (remark == null) {
            return ResponseData.errorMsg("remark不能为空！");
        }
        String targetUrl = uwGatewayProperties.getGatewayCenterHost() + "/rpc/service/clearSaasRateLimit";
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("saasId", String.valueOf(saasId));
            formData.add("remark", remark);
            ResponseData<Void> result = authRestClient.put()
                    .uri(targetUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ResponseData<Void>>() {});
            if (result == null) {
                return ResponseData.errorMsg("GatewayClientHelper.clearSaasRateLimit() returned null body");
            }
            return result;
        } catch (Exception e) {
            log.error("GatewayClientHelper.clearSaasRateLimit()异常: {}", e.getMessage(), e);
            return ResponseData.errorMsg("GatewayClientHelper.clearSaasRateLimit()异常: " + e.getMessage());
        }
    }

}
