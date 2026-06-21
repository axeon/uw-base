package uw.ai.conf;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import uw.ai.AiClientHelper;
import uw.ai.controller.AiToolExecuteController;
import uw.ai.rpc.AiChatRpc;
import uw.ai.rpc.AiConfigRpc;
import uw.ai.rpc.AiImageRpc;
import uw.ai.rpc.AiToolRpc;
import uw.ai.rpc.AiTranslateRpc;
import uw.ai.rpc.impl.AiChatRpcImpl;
import uw.ai.rpc.impl.AiConfigRpcImpl;
import uw.ai.rpc.impl.AiImageRpcImpl;
import uw.ai.rpc.impl.AiToolRpcImpl;
import uw.ai.rpc.impl.AiTranslateRpcImpl;
import uw.ai.tool.AiTool;
import uw.ai.util.AiToolSchemaGenerator;
import uw.ai.vo.AiToolMeta;
import uw.common.response.ResponseData;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * uw-ai 自动配置。
 * <p>
 * 装配各 RPC Bean 与 {@link AiClientHelper} 静态门面；应用就绪后扫描并按版本同步工具元数据到
 * AI 服务中心。通过 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} 注册。
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties({UwAiProperties.class})
public class UwAiAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(UwAiAutoConfiguration.class);

    /**
     * applicationContext
     */
    private final ApplicationContext applicationContext;

    /**
     * uwNotifyProperties.
     */
    private final UwAiProperties uwAiProperties;

    public UwAiAutoConfiguration(ApplicationContext applicationContext, UwAiProperties uwAiProperties) {
        this.applicationContext = applicationContext;
        this.uwAiProperties = uwAiProperties;
    }

    /**
     * 初始化 AiToolRpc Bean（工具元数据拉取与同步）。
     */
    @Bean
    @ConditionalOnMissingBean
    public AiToolRpc aiToolRpc(UwAiProperties uwAiProperties, RestClient authRestClient) {
        return new AiToolRpcImpl(uwAiProperties, authRestClient);
    }

    /**
     * 初始化 AiChatRpc Bean（同步经 RestClient，流式经 WebClient）。
     */
    @Bean
    @ConditionalOnMissingBean
    public AiChatRpc aiChatRpc(UwAiProperties uwAiProperties, RestClient authRestClient, WebClient authWebClient) {
        return new AiChatRpcImpl(uwAiProperties, authRestClient, authWebClient);
    }

    /**
     * 初始化 AiConfigRpc Bean（模型/API 配置查询）。
     */
    @Bean
    @ConditionalOnMissingBean
    public AiConfigRpc aiConfigRpc(UwAiProperties uwAiProperties, RestClient authRestClient) {
        return new AiConfigRpcImpl(uwAiProperties, authRestClient);
    }

    /**
     * 初始化 AiTranslateRpc Bean（列表/Map 批量翻译）。
     */
    @Bean
    @ConditionalOnMissingBean
    public AiTranslateRpc aiTranslateRpc(UwAiProperties uwAiProperties, RestClient authRestClient) {
        return new AiTranslateRpcImpl(uwAiProperties, authRestClient);
    }

    /**
     * 初始化 AiImageRpc Bean（图片生成）。
     */
    @Bean
    @ConditionalOnMissingBean
    public AiImageRpc aiImageRpc(UwAiProperties uwAiProperties, RestClient authRestClient) {
        return new AiImageRpcImpl(uwAiProperties, authRestClient);
    }


    /**
     * 初始化 AiClientHelper 静态门面，注入全部底层 RPC Bean。
     */
    @Bean
    @ConditionalOnMissingBean
    public AiClientHelper aiClientHelper(AiToolRpc toolRpc, AiChatRpc chatRpc, AiTranslateRpc translateRpc, AiConfigRpc configRpc, AiImageRpc imageRpc) {
        return new AiClientHelper(toolRpc, chatRpc, translateRpc, configRpc, imageRpc);
    }

    /**
     * 应用就绪后注册工具元数据。
     * <p>
     * 先从服务中心拉取本应用已有工具元数据，再扫描容器内所有 {@link AiTool} Bean：
     * 若工具不存在或版本不一致，则基于 {@link AiToolSchemaGenerator} 重新生成输入/输出
     * JSON Schema 并同步到服务中心。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        Map<String, AiTool> aiToolMap = applicationContext.getBeansOfType(AiTool.class);
        logger.info("扫描到当前实例AiTool数量为[{}]！", aiToolMap.size());
        if (!aiToolMap.isEmpty()) {
            logger.info("开始注册AiTool。。。");
            //处理apiType列表。
            ResponseData<List<AiToolMeta>> sysAiToolMetaListData = AiClientHelper.listToolMeta(uwAiProperties.getAppName());
            if (sysAiToolMetaListData.isNotSuccess()) {
                logger.error("拉取系统端AiToolMeta列表失败！ {}", sysAiToolMetaListData.getMsg());
                return;
            }
            List<AiToolMeta> sysAiToolMetaList = sysAiToolMetaListData.getData();
            // 防御服务中心返回 data 为 null 的场景。
            if (sysAiToolMetaList == null) {
                sysAiToolMetaList = Collections.emptyList();
            }
            Map<String, AiToolMeta> sysAiToolMetaMap = sysAiToolMetaList.stream()
                    .filter(x -> x != null && x.getToolClass() != null)
                    .collect(Collectors.toMap(x -> x.getToolClass(), x -> x,
                    (existingValue, newValue) -> newValue));
            logger.info("系统端拉取到有效AiTool共{}条！", sysAiToolMetaMap.size());
            for (AiTool aiTool : aiToolMap.values()) {
                String toolClass = aiTool.getClass().getName();
                Method applyMethod;
                try {
                    // 排除泛型擦除产生的 synthetic/bridge 方法，避免拿到擦除后的参数类型导致 Schema 生成错误。
                    applyMethod = Arrays.stream(aiTool.getClass().getDeclaredMethods())
                            .filter(method -> method.getName().equals("apply") && !method.isBridge() && !method.isSynthetic())
                            .findFirst()
                            .orElse(null);
                } catch (Exception e) {
                    logger.error("AiTool[{}]找不到正确定义的apply方法！{}", toolClass, e.getMessage(), e);
                    continue;
                }
                if (applyMethod == null) {
                    logger.error("AiTool[{}]找不到正确定义的apply方法！", toolClass);
                    continue;
                }
                AiToolMeta sysToolMeta = sysAiToolMetaMap.get(toolClass);
                if (sysToolMeta == null || !sysToolMeta.getToolVersion().equals(aiTool.toolVersion())) {
                    AiToolMeta aiToolMeta = new AiToolMeta();
                    if (sysToolMeta != null) {
                        aiToolMeta.setId(sysToolMeta.getId());
                    }
                    aiToolMeta.setAppName(uwAiProperties.getAppName());
                    aiToolMeta.setToolClass(toolClass);
                    aiToolMeta.setToolVersion(aiTool.toolVersion());
                    aiToolMeta.setToolName(aiTool.toolName());
                    aiToolMeta.setToolDesc(aiTool.toolDesc());
                    aiToolMeta.setToolInput(AiToolSchemaGenerator.generateForMethodInput(applyMethod));
                    aiToolMeta.setToolOutput(AiToolSchemaGenerator.generateForMethodOutput(applyMethod));
                    ResponseData responseData = AiClientHelper.updateToolMeta(aiToolMeta);
                    if (responseData.isSuccess()) {
                        logger.info("注册AiToolMeta[{}]成功！", toolClass);
                    } else {
                        logger.error("注册AiToolMeta[{}]失败！{}", toolClass, responseData.getMsg());
                    }
                }
            }
            logger.info("完成注册AiToolMeta!");
        }
    }

    /**
     * 初始化工具执行控制器 Bean（供服务中心回调执行工具）。
     */
    @Bean
    @ConditionalOnMissingBean
    AiToolExecuteController aiToolExecuteController(ApplicationContext applicationContext) {
        return new AiToolExecuteController(applicationContext);
    }

}
