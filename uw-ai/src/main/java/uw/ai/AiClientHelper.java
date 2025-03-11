package uw.ai;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.ai.rpc.AiToolRpc;
import uw.ai.vo.AiToolMeta;
import uw.common.dto.ResponseData;

import java.util.List;

/**
 * AiClientHelper。
 */
public class AiClientHelper {

    private static final Logger log = LoggerFactory.getLogger( AiClientHelper.class );
    /**
     * Rest模板类
     */
    private static AiToolRpc toolRpc;

    public AiClientHelper(AiToolRpc toolRpc) {
        AiClientHelper.toolRpc = toolRpc;
    }

    /**
     * 获取工具元数据。
     *
     * @return
     */
    public static ResponseData<List<AiToolMeta>> listToolMeta(String appName) {
        return toolRpc.listToolMeta( appName );
    }

    /**
     * 更新工具元数据。
     *
     * @param aiToolMeta
     * @return
     */
    public static ResponseData updateToolMeta(AiToolMeta aiToolMeta) {
        return toolRpc.updateToolMeta( aiToolMeta );
    }



}
