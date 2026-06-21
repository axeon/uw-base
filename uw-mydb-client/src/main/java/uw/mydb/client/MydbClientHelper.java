package uw.mydb.client;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import uw.common.response.ResponseData;
import uw.mydb.client.conf.UwMydbClientProperties;
import uw.mydb.client.vo.DataNode;

import java.io.Serializable;

/**
 * MyDB 数据库运维中心客户端 Helper。
 * <p>
 * 通过 HTTP RPC 调用 mydb-center 服务，为指定运营商（SAAS）动态分配数据库节点，
 * 实现分库分表路由。所有方法均为静态方法，由 {@link uw.mydb.client.conf.UwMydbAutoConfiguration}
 * 在启动时通过依赖注入完成对内部静态 {@code RestClient} 与配置的初始化，
 * 业务方无需持有 Bean 实例即可直接调用。
 * <p>
 * 共享的 {@link RestClient}（命名为 {@code authRestClient}）由 {@code uw-auth-client}
 * 提供，内置鉴权拦截器，调用 mydb-center 时自动携带服务间鉴权信息。
 *
 * @author axeon
 * @see DataNode
 */
public class MydbClientHelper {

    private static final Logger log = LoggerFactory.getLogger(MydbClientHelper.class);

    /**
     * 默认配置组 key，当调用方未显式指定 {@code configKey} 时使用。
     */
    private static final String DEFAULT_CONFIG = "default";

    /**
     * 用于发起 RPC 调用的共享 RestClient（由 uw-auth-client 提供，带鉴权拦截器）。
     */
    private static RestClient authRestClient;

    /**
     * mydb-center 连接配置。
     */
    private static UwMydbClientProperties uwMydbClientProperties;

    /**
     * 构造方法，由自动装配调用以完成静态字段的依赖注入。
     * <p>
     * 该设计为有意为之：通过 Spring 实例化完成对静态字段的注入，配合静态工具方法对外暴露能力，
     * 使业务方无需持有 Helper Bean 即可调用。单 ApplicationContext 下由 {@code @ConditionalOnMissingBean}
     * 保证唯一初始化。
     *
     * @param uwMydbClientProperties mydb-center 连接配置
     * @param authRestClient         共享的带鉴权 RestClient
     */
    public MydbClientHelper(UwMydbClientProperties uwMydbClientProperties, RestClient authRestClient) {
        MydbClientHelper.uwMydbClientProperties = uwMydbClientProperties;
        MydbClientHelper.authRestClient = authRestClient;
    }

    /**
     * 使用默认配置组为指定运营商自动分配数据节点。
     *
     * @param saasId 运营商 ID
     * @return 分配结果：SUCCESS=新建节点 / WARN=节点已存在 / ERROR=分配失败
     */
    public static ResponseData<DataNode> assignSaasNode(Serializable saasId) {
        return assignSaasNode(DEFAULT_CONFIG, saasId, null);
    }

    /**
     * 使用默认配置组为指定运营商分配数据节点，可指定偏好节点名。
     *
     * @param saasId     运营商 ID
     * @param preferNode 偏好节点名（可为 {@code null}，表示由服务端自动选择）
     * @return 分配结果：SUCCESS=新建节点 / WARN=节点已存在 / ERROR=分配失败
     */
    public static ResponseData<DataNode> assignSaasNode(Serializable saasId, String preferNode) {
        return assignSaasNode(DEFAULT_CONFIG, saasId, preferNode);
    }

    /**
     * 指定配置组为指定运营商分配数据节点，可指定偏好节点名。
     * <p>
     * 通过 POST 表单调用 mydb-center 的 {@code /rpc/app/assignSaasNode} 接口。
     * 参数在调用前做前置校验，任一必填项不合法直接返回 ERROR，不会发起 RPC：
     * {@code configKey} 与 {@code saasId} 不可为空。
     * RPC 调用阶段的任何异常（网络、序列化、HTTP 4xx/5xx）均被捕获并以 {@link ResponseData#errorMsg} 返回，
     * 不会向外抛出。
     *
     * @param configKey  配置组 key（对应 mydb-center 的分库分表配置分组，不可为空）
     * @param saasId     运营商 ID（不可为 null）
     * @param preferNode 偏好节点名（可为 {@code null}，表示由服务端自动选择）
     * @return 分配结果：SUCCESS=新建节点 / WARN=节点已存在 / ERROR=参数非法或分配失败（含异常信息）
     */
    public static ResponseData<DataNode> assignSaasNode(String configKey, Serializable saasId, String preferNode) {
        if (configKey == null || configKey.isEmpty()) {
            return ResponseData.errorMsg("configKey不能为空！");
        }
        if (saasId == null) {
            return ResponseData.errorMsg("saasId不能为空！");
        }
        String targetUrl = uwMydbClientProperties.getMydbCenterHost() + "/rpc/app/assignSaasNode";
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("configKey", configKey);
            formData.add("saasId", String.valueOf(saasId));
            if (preferNode != null) {
                formData.add("preferNode", preferNode);
            }
            ResponseData<DataNode> result = authRestClient.post()
                    .uri(targetUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ResponseData<DataNode>>() {});
            if (result == null) {
                return ResponseData.errorMsg("MydbClientHelper.assignSaasNode() returned null body");
            }
            return result;
        } catch (Exception e) {
            log.error("MydbClientHelper.assignSaasNode()异常: {}", e.getMessage(), e);
            return ResponseData.errorMsg("MydbClientHelper.assignSaasNode()异常: " + e.getMessage());
        }
    }

}
