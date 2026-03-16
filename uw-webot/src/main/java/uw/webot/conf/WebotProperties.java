package uw.webot.conf;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import uw.webot.captcha.CaptchaConfig;
import uw.webot.core.BrowserConfig;
import uw.webot.proxy.ProxyConfig;
import uw.webot.session.SessionConfig;
import uw.webot.stealth.StealthConfig;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Webot自动化配置属性类。
 * 包含所有可配置参数，使用JSR-380注解进行参数校验。
 * <p>
 * 验证码、反检测、代理配置已移至 SessionProperties 中，
 * 作为创建默认 WebotSession 的参数。
 * </p>
 *
 * @author axeon
 * @since 1.0.0
 */
@Validated
@ConfigurationProperties(prefix = "uw.webot")
public class WebotProperties {

    /**
     * 是否启用Webot自动化功能。
     */
    private boolean enabled = true;

    /**
     * Hybrid混合模式配置。
     */
    private BotPoolProperties botPool = new BotPoolProperties();

    /**
     * 验证码配置属性。
     */
    private Map<String, CaptchaConfig> captcha = new LinkedHashMap<>();

    /**
     * 代理配置。
     */
    private Map<String, ProxyConfig> proxy = new LinkedHashMap<>();

    /**
     * 浏览器无痕模式配置。
     */
    private Map<String, StealthConfig> stealth = new LinkedHashMap<>();

    /**
     * 默认会话管理配置（包含验证码、反检测、代理配置）。
     */
    private SessionProperties session = new SessionProperties();


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public BotPoolProperties getBotPool() {
        return botPool;
    }

    public void setBotPool(BotPoolProperties botPool) {
        this.botPool = botPool;
    }

    public Map<String, CaptchaConfig> getCaptcha() {
        return captcha;
    }

    public void setCaptcha(Map<String, CaptchaConfig> captcha) {
        this.captcha = captcha;
    }

    public Map<String, ProxyConfig> getProxy() {
        return proxy;
    }

    public void setProxy(Map<String, ProxyConfig> proxy) {
        this.proxy = proxy;
    }

    public Map<String, StealthConfig> getStealth() {
        return stealth;
    }

    public void setStealth(Map<String, StealthConfig> stealth) {
        this.stealth = stealth;
    }

    public SessionProperties getSession() {
        return session;
    }

    public void setSession(SessionProperties session) {
        this.session = session;
    }

    /**
     * Hybrid混合模式配置属性。
     */
    @Validated
    public static class BotPoolProperties {

        /**
         * 每个浏览器组的最大Browser数量。
         */
        @Min(1)
        @Max(20)
        private int maxBrowsersPerGroup = 5;

        /**
         * 每个Browser的最大BrowserTab数量。
         */
        @Min(1)
        @Max(50)
        private int maxTabsPerBrowser = 20;

        public int getMaxBrowsersPerGroup() {
            return maxBrowsersPerGroup;
        }

        public void setMaxBrowsersPerGroup(int maxBrowsersPerGroup) {
            this.maxBrowsersPerGroup = maxBrowsersPerGroup;
        }

        public int getMaxTabsPerBrowser() {
            return maxTabsPerBrowser;
        }

        public void setMaxTabsPerBrowser(int maxTabsPerBrowser) {
            this.maxTabsPerBrowser = maxTabsPerBrowser;
        }
    }

    /**
     * 会话管理配置属性。
     * 包含验证码、反检测、代理配置，用于创建默认 WebotSession。
     */
    @Validated
    public static class SessionProperties {

        /**
         * 是否启用分布式会话。
         */
        private boolean distributed = false;


        private SessionConfig defaultSession = new SessionConfig();

        public boolean isDistributed() {
            return distributed;
        }

        public void setDistributed(boolean distributed) {
            this.distributed = distributed;
        }

        public SessionConfig getDefaultSession() {
            return defaultSession;
        }

        public void setDefaultSession(SessionConfig defaultSession) {
            this.defaultSession = defaultSession;
        }
    }
}
