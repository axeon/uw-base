package uw.ai.rpc;

import uw.ai.vo.AiToolMeta;
import uw.common.dto.ResponseData;

import java.util.List;

public interface AiToolRpc {


    /**
     * 获取工具元数据。
     *
     * @return
     */
    ResponseData<List<AiToolMeta>> listToolMeta(String appName);

    /**
     * 更新工具元数据。
     *
     * @param aiToolMeta
     * @return
     */
    ResponseData updateToolMeta(AiToolMeta aiToolMeta);
}
