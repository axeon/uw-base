package uw.webot.conf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import uw.webot.WebotManager;
import uw.webot.captcha.CaptchaManager;
import uw.webot.core.BrowserBotPool;
import uw.webot.proxy.ProxyManager;
import uw.webot.session.SessionService;
import uw.webot.session.impl.GlobalSessionServiceImpl;
import uw.webot.stealth.StealthManager;

/**
 * Webot自动配置类。
 * 支持Spring环境无缝集成。
 * <p>
 * WebotManager通过构造器注入依赖，由Spring容器自动管理。
 * </p>
 *
 * @author axeon
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(WebotProperties.class)
@ConditionalOnProperty(prefix = "uw.webot", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "uw.webot")
public class WebotAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(WebotAutoConfiguration.class);

    /**
     * 配置浏览器实例池。
     *
     * @param properties 配置属性
     * @return BrowserInstancePool实例
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public BrowserBotPool browserBotPool(WebotProperties properties) throws Exception {
        log.info("Initializing BrowserBotPool, maxBrowsersPerGroup: {}, maxTabsPerBrowser: {} ",
                properties.getBotPool().getMaxBrowsersPerGroup(), properties.getBotPool().getMaxTabsPerBrowser());
        return new BrowserBotPool(properties.getBotPool());
    }

    /**
     * 配置会话管理器。
     *
     * @param properties 配置属性
     * @return SessionManager实例
     */
    @Bean
    @ConditionalOnMissingBean
    public SessionService sessionManager(WebotProperties properties) {
        return new GlobalSessionServiceImpl(properties.getSession());
    }

    /**
     * 配置验证码服务。
     *
     * @param properties 配置属性
     * @return CaptchaService实例
     */
    @Bean
    @ConditionalOnMissingBean
    public CaptchaManager captchaManager(WebotProperties properties) {
        return new CaptchaManager(properties.getCaptcha());
    }

    /**
     * 配置反检测服务。
     *
     * @param properties 配置属性
     * @return StealthService实例
     */
    @Bean
    @ConditionalOnMissingBean
    public StealthManager stealthManager(WebotProperties properties) {
        return new StealthManager(properties.getStealth());
    }

    /**
     * 配置代理服务。
     *
     * @param properties 配置属性
     * @return ProxyService实例
     */
    @Bean
    @ConditionalOnMissingBean
    public ProxyManager proxyManager(WebotProperties properties) {
        return new ProxyManager(properties.getProxy());
    }

    /**
     * 配置Webot管理器。
     *
     * @param properties     配置属性
     * @param browserBotPool 实例池
     * @param sessionService 会话管理器
     * @param captchaManager 验证码服务
     * @param stealthManager 反检测服务
     * @param proxyManager   代理服务
     * @return WebotManager实例
     */
    @Bean
    @ConditionalOnMissingBean
    public WebotManager webotManager(WebotProperties properties, BrowserBotPool browserBotPool, SessionService sessionService, CaptchaManager captchaManager, StealthManager stealthManager, ProxyManager proxyManager) {
        log.info("Initializing WebotManager");
        return new WebotManager(properties, browserBotPool, sessionService, captchaManager, stealthManager, proxyManager);
    }

}
