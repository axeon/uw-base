package uw.ai.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 配置类。
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
