package uw.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uw.ai.tool.AiTool;
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
    @PostMapping("/run")
    @MscPermDeclare(user = UserType.RPC)
    @Operation(summary = "运行任务", description = "运行任务")
    public ResponseData run(String toolClass, String toolInput) {
        AiTool aiTool = applicationContext.getBean( toolClass, AiTool.class );
        if (aiTool == null) {
            return ResponseData.errorMsg( "找不到任务类：" + toolClass );
        }
        return ResponseData.success( aiTool.apply( toolInput ) );
    }

}
