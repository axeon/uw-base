package uw.ai.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * uw-ai 配置属性。
 * <p>
 * 配置前缀 {@code uw.ai}。{@code appName} 取自 {@code project.name}，用于启动时按应用维度
 * 注册/拉取工具元数据；{@code aiCenterHost} 为 AI 服务中心地址。
 *
 * @author axeon
 */
@ConfigurationProperties(prefix = "uw.ai")
public class UwAiProperties {
    /**
     * 应用名称
     */
    @Value("${project.name}")
    private String appName;

    /**
     * Ai服务器
     */
    private String aiCenterHost = "http://uw-ai-center";

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAiCenterHost() {
        return aiCenterHost;
    }

    public void setAiCenterHost(String aiCenterHost) {
        this.aiCenterHost = aiCenterHost;
    }
}
