package uw.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uw.ai.tool.AiTool;
import uw.ai.tool.AiToolParam;
import uw.ai.vo.AiToolExecuteParam;
import uw.auth.service.annotation.MscPermDeclare;
import uw.auth.service.constant.UserType;
import uw.common.response.ResponseData;

/**
 * AI工具执行控制器。
 * <p>
 * 供 AI 服务中心（uw-ai-center）回调，以 RPC 用户身份（{@link UserType#RPC}）在本地执行
 * 已注册的 {@link AiTool} 工具。
 **/
@RestController
@RequestMapping("/rpc/ai/tool")
public class AiToolExecuteController {

    private static final Logger logger = LoggerFactory.getLogger(AiToolExecuteController.class);

    /**
     * spring上下文对象
     */
    private final ApplicationContext applicationContext;

    public AiToolExecuteController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 运行任务。
     * <p>
     * 服务中心决策调用某工具时，回调本接口执行：根据 {@code toolClass} 从 Spring 容器获取
     * {@link AiTool} Bean，将 {@code toolInput} 反序列化为参数后执行 {@code apply}。
     *
     * @param executeParam 工具执行参数（工具类全限定名 + JSON 输入）
     * @return 工具执行结果
     */
    @PostMapping("/execute")
    @MscPermDeclare(user = UserType.RPC)
    @Operation(summary = "运行任务", description = "运行任务")
    public ResponseData execute(@RequestBody AiToolExecuteParam executeParam) {
        if (StringUtils.isBlank(executeParam.getToolClass()) || StringUtils.isBlank(executeParam.getToolInput())) {
            return ResponseData.errorMsg("请给出任务类和任务参数！");
        }
        AiTool<AiToolParam, ResponseData> aiTool;
        try {
            aiTool = applicationContext.getBean(executeParam.getToolClass(), AiTool.class);
        } catch (Exception e) {
            logger.error("获取AiTool实例失败: {}", executeParam.getToolClass(), e);
            return ResponseData.errorMsg("任务类不存在或不可用");
        }
        AiToolParam toolParam = aiTool.convertParam(executeParam.getToolInput());
        return aiTool.apply(toolParam);
    }

}
