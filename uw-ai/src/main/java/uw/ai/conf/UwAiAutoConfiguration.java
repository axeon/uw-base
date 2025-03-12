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
import uw.ai.AiClientHelper;
import uw.ai.controller.AiToolExecuteController;
import uw.ai.rpc.AiChatRpc;
import uw.ai.rpc.AiToolRpc;
import uw.ai.tool.AiTool;
import uw.ai.util.AiToolSchemaGenerator;
import uw.ai.vo.AiToolMeta;
import uw.common.dto.ResponseData;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 启动自动配置。
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties({UwAiProperties.class})
public class UwAiAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger( UwAiAutoConfiguration.class );

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
     * AiClientHelper初始化。
     *
     * @param toolRpc
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    public AiClientHelper aiClientHelper(AiToolRpc toolRpc, AiChatRpc chatRpc) {
        return new AiClientHelper( toolRpc, chatRpc );
    }

    /**
     * ApplicationContext初始化完成或刷新后执行init方法。
     * 首先获取服务器端的TOOL配置，然后初始化本地TOOL配置。
     * 如果发现TOOL有升级，则更新到服务器端。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        Map<String, AiTool> aiToolMap = applicationContext.getBeansOfType( AiTool.class );
        logger.info( "扫描到当前实例AiTool数量为[{}]！", aiToolMap.size() );
        if (!aiToolMap.isEmpty()) {
            logger.info( "开始注册AiTool。。。" );
            //处理apiType列表。
            ResponseData<List<AiToolMeta>> sysAiToolMetaListData = AiClientHelper.listToolMeta( uwAiProperties.getAppName() );
            if (sysAiToolMetaListData.isNotSuccess()) {
                logger.error( "拉取系统端AiToolMeta列表失败！ {}", sysAiToolMetaListData.getMsg() );
                return;
            }
            List<AiToolMeta> sysAiToolMetaList = sysAiToolMetaListData.getData();
            Map<String, AiToolMeta> sysAiToolMetaMap = sysAiToolMetaList.stream().collect( Collectors.toMap( x -> x.getToolClass(), x -> x,
                    (existingValue, newValue) -> newValue ) );
            logger.info( "系统端拉取到有效AiTool共{}条！", sysAiToolMetaMap.size() );
            for (AiTool aiTool : aiToolMap.values()) {
                String toolClass = aiTool.getClass().getName();
                Method applyMethod;
                try {
                    applyMethod = Arrays.stream( aiTool.getClass().getDeclaredMethods() ).filter( method -> method.getName().equals( "apply" ) ).findFirst().orElse( null );
                } catch (Exception e) {
                    logger.error( "AiTool[{}]找不到正确定义的apply方法！{}", toolClass, e.getMessage(), e );
                    continue;
                }
                if (applyMethod == null) {
                    logger.error( "AiTool[{}]找不到正确定义的apply方法！", toolClass );
                    continue;
                }
                AiToolMeta sysToolMeta = sysAiToolMetaMap.get( toolClass );
                if (sysToolMeta == null || !sysToolMeta.getToolVersion().equals( aiTool.toolVersion() )) {
                    AiToolMeta aiToolMeta = new AiToolMeta();
                    if (sysToolMeta != null) {
                        aiToolMeta.setId( sysToolMeta.getId() );
                    }
                    aiToolMeta.setAppName( uwAiProperties.getAppName() );
                    aiToolMeta.setToolClass( toolClass );
                    aiToolMeta.setToolVersion( aiTool.toolVersion() );
                    aiToolMeta.setToolName( aiTool.toolName() );
                    aiToolMeta.setToolDesc( aiTool.toolDesc() );
                    aiToolMeta.setToolInput( AiToolSchemaGenerator.generateForMethodInput( applyMethod ) );
                    aiToolMeta.setToolOutput( AiToolSchemaGenerator.generateForMethodOutput( applyMethod ) );
                    ResponseData responseData = AiClientHelper.updateToolMeta( aiToolMeta );
                    if (responseData.isSuccess()) {
                        logger.info( "注册AiToolMeta[{}]成功！", toolClass );
                    } else {
                        logger.error( "注册AiToolMeta[{}]失败！{}", toolClass, responseData.getMsg() );
                    }
                }
            }
            logger.info( "完成注册AiToolMeta!" );
        }
    }

    /**
     * AiToolExecuteController初始化。
     *
     * @param applicationContext
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    AiToolExecuteController aiToolExecuteController(ApplicationContext applicationContext) {
        return new AiToolExecuteController( applicationContext );
    }

}
