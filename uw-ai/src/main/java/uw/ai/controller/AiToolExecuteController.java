package uw.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;
import uw.ai.tool.AiTool;
import uw.ai.tool.AiToolParam;
import uw.ai.vo.AiToolExecuteParam;
import uw.auth.service.annotation.MscPermDeclare;
import uw.auth.service.constant.UserType;
import uw.common.dto.ResponseData;

/**
 * 运行AipVendor的容器。
 * 用于执行本地任务。
 **/
@RestController
@RequestMapping("/rpc/ai/tool")
public class AiToolExecuteController {

    private static final Logger logger = LoggerFactory.getLogger( AiToolExecuteController.class );

    /**
     * spring上下文对象
     */
    private final ApplicationContext applicationContext;

    public AiToolExecuteController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 运行任务。
     *
     * @return 是否成功
     */
    @PostMapping("/execute")
    @MscPermDeclare(user = UserType.RPC)
    @Operation(summary = "运行任务", description = "运行任务")
    public ResponseData execute(@RequestBody AiToolExecuteParam executeParam) {
        if (StringUtils.isBlank( executeParam.getToolClass())|| StringUtils.isBlank( executeParam.getToolInput() )){
            return ResponseData.errorMsg( "请给出任务类和任务参数！" );
        }
        AiTool aiTool = applicationContext.getBean( executeParam.getToolClass(), AiTool.class );
        if (aiTool == null) {
            return ResponseData.errorMsg( "找不到任务类：" + executeParam.getToolClass() );
        }
        AiToolParam toolParam = aiTool.convertParam( executeParam.getToolInput() );
        return (ResponseData)aiTool.apply( toolParam ) ;
    }

}
