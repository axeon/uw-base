package uw.ai.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 配置类。
 *
 * @author axeon
 */
@ConfigurationProperties(prefix = "uw.ai")
public class UwAiProperties {

    /**
     * Ai服务器
     */
    private String aiCenterHost = "http://uw-ai-center";


    public String getAiCenterHost() {
        return aiCenterHost;
    }

    public void setAiCenterHost(String aiCenterHost) {
        this.aiCenterHost = aiCenterHost;
    }
}
