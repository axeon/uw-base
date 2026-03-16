package uw.webot;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.util.SnowflakeIdGenerator;
import uw.webot.captcha.CaptchaManager;
import uw.webot.conf.WebotProperties;
import uw.webot.core.BrowserBotPool;
import uw.webot.core.BrowserTab;
import uw.webot.proxy.ProxyManager;
import uw.webot.proxy.ProxyService;
import uw.webot.proxy.ProxyType;
import uw.webot.session.SessionConfig;
import uw.webot.session.SessionService;
import uw.webot.stealth.StealthManager;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Webot管理器。
 * 提供统一的Web自动化操作入口，整合实例池、会话管理、验证码、反检测和代理服务。
 * <p>
 * 基于Hybrid混合模式，直接使用ManagedContext进行浏览器操作。
 * </p>
 *
 * @author axeon
 * @since 1.0.0
 */
public class WebotManager {

    private static final Logger log = LoggerFactory.getLogger(WebotManager.class);

    /**
     * 配置属性。
     */
    private final WebotProperties properties;

    /**
     * 浏览器实例池。
     */
    private final BrowserBotPool browserBotPool;

    /**
     * 会话管理器。
     */
    private final SessionService sessionService;

    /**
     * 验证码服务（可选）。
     */
    private final CaptchaManager captchaManager;

    /**
     * 反检测服务（可选）。
     */
    private final StealthManager stealthManager;

    /**
     * 代理服务（可选）。
     */
    private final ProxyManager proxyManager;

    /**
     * 单例实例。
     */
    private static WebotManager INSTANCE;


    /**
     * 返回全局唯一实例。
     *
     * @return
     */
    public static WebotManager getInstance() {
        return INSTANCE;
    }

    /**
     * 构造器注入依赖。
     *
     * @param properties     配置属性
     * @param browserBotPool 实例池
     * @param sessionService 会话管理器
     * @param captchaManager 验证码服务
     * @param stealthManager 反检测服务
     * @param proxyManager   代理服务
     */
    public WebotManager(WebotProperties properties,
                        BrowserBotPool browserBotPool,
                        SessionService sessionService,
                        CaptchaManager captchaManager,
                        StealthManager stealthManager,
                        ProxyManager proxyManager) {
        this.properties = properties;
        this.browserBotPool = browserBotPool;
        this.sessionService = sessionService;
        this.captchaManager = captchaManager;
        this.stealthManager = stealthManager;
        this.proxyManager = proxyManager;
        WebotManager.INSTANCE = this;
        log.info("WebotManager initialized with Hybrid mode");
    }

    /**
     * 获取浏览器页签（自动关联会话）。
     * <p>
     * <strong>重要：</strong>返回的 BrowserTab 实现了 {@link AutoCloseable} 接口，
     * 在获取时会自动关联所属的 BrowserBotPool。当调用 {@link BrowserTab#close()} 方法时，
     * BrowserTab 会自动归还到 BrowserBotPool 中以便复用。
     * </p>
     * <p>推荐使用 try-with-resources 语句：</p>
     * <pre>
     * try (BrowserTab page = webotManager.acquireBrowserTab(sessionId)) {
     *     page.navigate("https://example.com");
     *     // 执行操作...
     * } // 自动调用 close() 归还到 BrowserBotPool
     * </pre>
     * <p>或者使用 {@link #execute(WebotSession, WebotFunction)} 方法自动管理资源：</p>
     * <pre>
     * webotManager.execute(sessionId, page -> {
     *     page.navigate("https://example.com");
     *     // 自动释放资源
     * });
     * </pre>
     *
     * @param webotSession 会话信息（必须有效）
     * @return BrowserTab 实例，实现了 AutoCloseable 接口
     * @throws IllegalArgumentException sessionId 无效或已过期时抛出
     * @throws InterruptedException     中断异常
     * @throws TimeoutException         超时异常
     * @see BrowserTab#close()
     */
    public BrowserTab openBrowserTab(WebotSession webotSession) throws TimeoutException {
        log.debug("opening browserTab for session: {}", webotSession.getSessionId());
        BrowserTab browserTab = browserBotPool.openBrowserTab(webotSession);
        // 自动启用反检测服务。
        if (StringUtils.isNotBlank(webotSession.getStealthConfigKey())) {
            stealthManager.apply(browserTab, webotSession.getStealthConfigKey());
        }
        return browserTab;
    }

    /**
     * 执行操作并自动管理上下文生命周期。
     * <p>
     * 自动获取 BrowserTab，执行函数，然后使用 try-with-resources
     * 自动将 BrowserTab 归还到 BrowserBotPool 中。
     * 无需手动调用 acquireBrowserTab() 和 releaseContext()。
     * </p>
     *
     * @param <T>          返回类型
     * @param webotSession 会话
     * @param function     操作函数
     * @return 操作结果
     * @throws Exception 操作异常
     */
    public <T> T execute(WebotSession webotSession, WebotFunction<T> function) throws Exception {
        try (BrowserTab browserTab = openBrowserTab(webotSession)) {
            return function.apply(browserTab);
        }
    }

    /**
     * 执行操作（无返回值）。
     * <p>
     * 使用 try-with-resources 自动管理 BrowserTab 资源。
     * </p>
     *
     * @param webotSession 会话
     * @param consumer     操作消费者
     * @throws Exception 操作异常
     */
    public void execute(WebotSession webotSession, WebotConsumer consumer) throws Exception {
        execute(webotSession, browserTab -> {
            consumer.accept(browserTab);
            return null;
        });
    }

    /**
     * 创建新会话（使用 SessionConfig）。
     *
     * @param sessionConfig 会话配置
     * @return 会话ID
     * @throws IllegalArgumentException 配置无效时抛出
     */
    public WebotSession createSession(SessionConfig sessionConfig) {
        // 验证配置
        if (sessionConfig == null) {
            throw new IllegalArgumentException("SessionConfig cannot be null");
        }

        SessionConfig defaultConfig = properties.getSession().getDefaultSession();
        long expireTimeMillis = defaultConfig.getExpireTime().toMillis();
        String sessionId = generateSessionId();
        if (sessionConfig.getExpireTime() != null) {
            expireTimeMillis = sessionConfig.getExpireTime().toMillis();
        }
        // 创建 WebotSession
        WebotSession session = new WebotSession(sessionId, expireTimeMillis);
        // 设置浏览器配置
        if (sessionConfig.getBrowserConfig() != null) {
            session.setBrowserConfig(sessionConfig.getBrowserConfig());
        } else {
            session.setBrowserConfig(defaultConfig.getBrowserConfig());
        }
        // 设置代理服务
        if (sessionConfig.getProxyConfigKey() != null) {
            ProxyService proxyService = proxyManager.getProxyService(sessionConfig.getProxyConfigKey());
            if (proxyService != null) {
                ProxyService.ProxyInfo proxyInfo = proxyService.getProxy(ProxyType.HTTPS);
                if (proxyInfo != null) {
                    session.setProxyServer(proxyInfo.getProxyServer());
                }
            }
        }
        // 设置反检测服务
        if (sessionConfig.getStealthConfigKey() != null) {
            session.setStealthConfigKey(sessionConfig.getStealthConfigKey());
        }
        // 设置验证码服务
        if (sessionConfig.getCaptchaConfigKey() != null) {
            session.setCaptchaConfigKey(sessionConfig.getCaptchaConfigKey());
        }
        // 设置扩展参数
        if (sessionConfig.getExtParam() != null) {
            session.setExtParam(sessionConfig.getExtParam());
        } else {
            session.setExtParam(defaultConfig.getExtParam());
        }
        sessionService.setSession(sessionId, session, sessionConfig.getExpireTime());
        log.debug("Created session [{}]", sessionId);
        return session;
    }

    /**
     * 创建新会话（使用默认配置）。
     *
     * @return 会话ID
     */
    public WebotSession createSession() {
        SessionConfig defaultConfig = properties.getSession().getDefaultSession();
        return createSession(defaultConfig);
    }

    /**
     * 更新会话。
     *
     * @param webotSession 会话
     * @return 是否成功更新
     */
    public void updateSession(WebotSession webotSession, Duration ttl) {
        sessionService.setSession(webotSession.getSessionId(), webotSession, ttl);
    }

    /**
     * 销毁会话。
     *
     * @param sessionId 会话ID
     */
    public void destroySession(String sessionId) {
        sessionService.invalidateSession(sessionId);
        log.debug("Destroyed session: {}", sessionId);
    }

    /**
     * 获取会话。
     *
     * @param sessionId 会话ID
     * @return WebotSession实例
     */
    public WebotSession getSession(String sessionId) {
        return sessionService.getSession(sessionId);
    }


    /**
     * 生成会话ID。
     */
    private String generateSessionId() {
        return String.valueOf(SnowflakeIdGenerator.getInstance().generateId());
    }

    /**
     * 获取会话管理器。
     *
     * @return SessionManager实例
     */
    public SessionService getSessionManager() {
        return sessionService;
    }

    /**
     * 获取验证码服务。
     *
     * @return CaptchaService实例，如果未配置则返回null
     */
    public CaptchaManager getCaptchaManager() {
        return captchaManager;
    }

    /**
     * 获取反检测服务。
     *
     * @return StealthService实例，如果未配置则返回null
     */
    public StealthManager getStealthManager() {
        return stealthManager;
    }

    /**
     * 获取代理服务。
     *
     * @return ProxyService实例，如果未配置则返回null
     */
    public ProxyManager getProxyManager() {
        return proxyManager;
    }

    /**
     * 获取实例池统计信息。
     *
     * @return PoolStatistics实例
     */
    public BrowserBotPool.PoolStats getStats() {
        return browserBotPool.getStats();
    }

    /**
     * 获取配置属性。
     *
     * @return WebotProperties实例
     */
    public WebotProperties getProperties() {
        return properties;
    }

    /**
     * Context功能函数接口。
     */
    @FunctionalInterface
    public interface WebotFunction<T> {
        T apply(BrowserTab browserTab) throws Exception;
    }

    /**
     * Context消费函数接口。
     */
    @FunctionalInterface
    public interface WebotConsumer {
        void accept(BrowserTab browserTab) throws Exception;
    }
}
