package uw.ai.rpc;

import uw.ai.vo.AiToolMeta;
import uw.common.response.ResponseData;

import java.util.List;

/**
 * AI 工具元数据管理 RPC 接口。
 * <p>
 * 实现类（{@link uw.ai.rpc.impl.AiToolRpcImpl}）通过 RestClient 调用 AI 服务中心，
 * 启动时用于拉取与同步工具元数据。
 */
public interface AiToolRpc {


    /**
     * 获取指定应用下的工具元数据列表。
     *
     * @param appName 应用名称
     * @return 工具元数据列表
     */
    ResponseData<List<AiToolMeta>> listToolMeta(String appName);

    /**
     * 新增或更新工具元数据。
     *
     * @param aiToolMeta 工具元数据（含 id 时为更新，否则为新增）
     * @return 操作结果
     */
    ResponseData updateToolMeta(AiToolMeta aiToolMeta);
}
