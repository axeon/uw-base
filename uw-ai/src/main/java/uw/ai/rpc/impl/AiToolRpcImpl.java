package uw.ai.rpc.impl;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import uw.ai.conf.UwAiProperties;
import uw.ai.rpc.AiToolRpc;
import uw.ai.vo.AiToolMeta;
import uw.common.response.ResponseData;

import java.util.List;

/**
 * AiToolRpcImpl.
 */
public class AiToolRpcImpl implements AiToolRpc {

    private final RestClient authRestClient;

    private final UwAiProperties uwAiProperties;

    public AiToolRpcImpl(UwAiProperties uwAiProperties, RestClient authRestClient) {
        this.authRestClient = authRestClient;
        this.uwAiProperties = uwAiProperties;
    }

    /**
     * 列出指定appName下的工具列表。
     *
     * @param appName 应用名称
     * @return 工具元数据列表
     */
    @Override
    public ResponseData<List<AiToolMeta>> listToolMeta(String appName) {
        ResponseData<List<AiToolMeta>> result = authRestClient.get()
                .uri(uwAiProperties.getAiCenterHost() + "/rpc/tool/listToolMeta?appName={appName}", appName)
                .retrieve()
                .body(new ParameterizedTypeReference<ResponseData<List<AiToolMeta>>>() {});
        if (result == null) {
            return ResponseData.errorMsg("AiToolRpcImpl.listToolMeta() returned null body");
        }
        return result;
    }

    /**
     * 更新工具配置信息。
     *
     * @param aiToolMeta 工具元数据
     * @return 更新结果
     */
    @Override
    public ResponseData updateToolMeta(AiToolMeta aiToolMeta) {
        ResponseData result = authRestClient.post()
                .uri(uwAiProperties.getAiCenterHost() + "/rpc/tool/updateToolMeta")
                .body(aiToolMeta)
                .retrieve()
                .body(ResponseData.class);
        if (result == null) {
            return ResponseData.errorMsg("AiToolRpcImpl.updateToolMeta() returned null body");
        }
        return result;
    }
}
