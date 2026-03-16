package uw.webot.core;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.webot.WebotSession;
import uw.webot.proxy.ProxyConfig;
import uw.webot.stealth.StealthConfig;
import uw.webot.stealth.StealthManager;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 管理的浏览器页面。
 * <p>
 * 封装Playwright的BrowserContext和Page，是混合模式中的基础资源单元。
 * 多个BrowserTab可以共享同一个Browser实例，实现资源复用。
 * </p>
 * <p>
 * 该类实现了 {@link Closeable} 接口，支持 try-with-resources 语句进行自动资源管理。
 * 当调用 {@link #close()} 方法时，BrowserTab 会自动归还到所属的 {@link BrowserBotPool} 中，
 * 类似于 JDBC 连接池的连接归还机制。
 * </p>
 * <p>
 * <strong>线程安全说明：</strong>
 * 所有 Playwright 相关操作都通过 {@link BrowserInstance#submitAndWait(java.util.concurrent.Callable)}
 * 提交到 BrowserInstance 的专属线程中执行，确保线程安全。
 * 如果需要直接操作 Page 对象，请使用 {@link #execute(Consumer)} 或 {@link #execute(Function)} 方法。
 * </p>
 * <p>使用示例：</p>
 * <pre>
 * try (BrowserTab page = browserBotPool.openBrowserTab(sessionConfig)) {
 *     page.navigate("https://example.com");
 *     // 执行其他操作...
 * } // 自动调用 close() 归还到 BrowserBotPool
 * </pre>
 *
 * @author axeon
 * @see Closeable
 * @see BrowserBotPool
 * @since 1.0.0
 */
public class BrowserTab implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(BrowserTab.class);

    /**
     * 上下文ID。
     */
    private final String browserTabId;

    /**
     * 所属的浏览器实例。
     */
    private final BrowserInstance browserInstance;

    /**
     * 当前会话。
     */
    private final WebotSession webotSession;

    /**
     * 当前页面。
     */
    private Page page;

    /**
     * 浏览器上下文。
     */
    private BrowserContext context;

    /**
     * 创建时间。
     */
    private final long createTime;

    /**
     * 最后使用时间。
     */
    private long lastActiveTime;

    /**
     * 持有者线程。
     */
    private volatile Thread holderThread;

    /**
     * 上下文级别的元数据存储。
     */
    private final Map<String, Object> metadata = new ConcurrentHashMap<>();

    /**
     * 是否活跃。
     */
    private final AtomicBoolean active = new AtomicBoolean(false);

    /**
     * 构造方法。
     * <p>
     * 通过构造器注入 ownerPool，确保对象创建时依赖就存在，
     * 避免使用 setter 方法可能导致的空指针异常和状态不一致问题。
     * </p>
     *
     * @param browserTabId    上下文ID，不能为空
     * @param browserInstance 所属的浏览器实例，不能为空
     * @param webotSession    浏览器配置，如果为 null 则使用默认配置
     * @throws IllegalArgumentException 如果 browserTabId、browser 或 ownerPool 为 null
     */
    protected BrowserTab(String browserTabId, BrowserInstance browserInstance, WebotSession webotSession) {
        if (browserTabId == null) {
            throw new IllegalArgumentException("browserTabId cannot be null");
        }
        if (browserInstance == null) {
            throw new IllegalArgumentException("browserInstance cannot be null");
        }

        this.browserTabId = browserTabId;
        this.browserInstance = browserInstance;
        this.webotSession = webotSession;
        this.createTime = System.currentTimeMillis();
        this.lastActiveTime = createTime;
    }

    /**
     * 内部初始化方法（在 BrowserInstance 的 executor 线程中调用）。
     * <p>
     * 该方法会创建浏览器上下文和页面，并应用反检测脚本。
     * </p>
     */
    protected void init() {
        log.debug("Initializing BrowserTab [{}] in executor thread", browserTabId);
        BrowserConfig browserConfig = webotSession.getBrowserConfig();


        // 创建浏览器上下文选项
        Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                .setViewportSize(browserConfig.getViewportWidth(), browserConfig.getViewportHeight())
                .setJavaScriptEnabled(browserConfig.isJavaScriptEnabled());

        // 设置 User-Agent
        String userAgent = browserConfig.getUserAgent();
        if (userAgent != null && !userAgent.isEmpty()) {
            contextOptions.setUserAgent(userAgent);
        }

        // 设置 Locale
        String locale = browserConfig.getLocale();
        if (locale != null && !locale.isEmpty()) {
            contextOptions.setLocale(locale);
        }

        // 设置 Timezone
        String timezone = browserConfig.getTimezone();
        if (timezone != null && !timezone.isEmpty()) {
            contextOptions.setTimezoneId(timezone);
        }

        // 存储状态
        String storageStateJson = webotSession.getStorageStateJson();
        if (storageStateJson != null && !storageStateJson.isEmpty()) {
            contextOptions.setStorageState(storageStateJson);
        }

        ProxyConfig.ProxyServer proxyServer = webotSession.getProxyServer();
        // 设置代理
        if (proxyServer != null) {
            contextOptions.setProxy(new com.microsoft.playwright.options.Proxy(
                    proxyServer.getHost() + ":" + proxyServer.getPort()
            ));
        }

        this.browserInstance.submitAndWait(() -> {
            try {
                // 检查浏览器实例是否可用（在 executor 线程中，可以直接调用）
                Browser rawBrowser = browserInstance.getRawBrowser();
                if (rawBrowser == null) {
                    throw new IllegalStateException("Browser instance is not available for BrowserTab [" + browserTabId + "]. The browser may have failed to initialize or has been closed.");
                }
                // 创建浏览器上下文
                log.debug("Creating BrowserContext for BrowserTab [{}]", browserTabId);
                this.context = rawBrowser.newContext(contextOptions);
                // 创建页面
                log.debug("Creating Page for BrowserTab [{}]", browserTabId);
                this.page = context.newPage();
                holderThread = Thread.currentThread();
                active.set(true);
                log.debug("BrowserTab [{}] initialized successfully", browserTabId);
            } catch (Exception e) {
                log.error("Failed to initialize BrowserTab [{}]: {}", browserTabId, e.getMessage(), e);
                close();
                throw e;
            }
            return null;
        });
    }

    // ==================== 页面操作方法 ====================

    /**
     * 在 BrowserInstance 的专属线程中执行自定义操作（无返回值）。
     * <p>
     * 此方法允许直接访问 Page 和 Context 对象，同时确保线程安全。
     * </p>
     * <p>使用示例：</p>
     * <pre>
     * // 使用Page进行操作
     * browserTab.execute((context, page) -> {
     *     page.navigate("https://example.com");
     * });
     *
     * // 使用Context获取存储状态
     * browserTab.execute((context, page) -> {
     *     String storageState = context.storageState();
     *     // 保存storageState...
     * });
     * </pre>
     *
     * @param consumer 操作函数，第一个参数是Context，第二个参数是Page
     */
    public void consume(BiConsumer<BrowserContext, Page> consumer) {
        browserInstance.submitAndWait(() -> {
            checkActive();
            consumer.accept(context, page);
            return null;
        });
    }

    /**
     * 在 BrowserInstance 的专属线程中执行自定义操作（带返回值）。
     * <p>
     * 此方法允许直接访问 Page 和 Context 对象，同时确保线程安全并返回结果。
     * </p>
     * <p>使用示例：</p>
     * <pre>
     * // 获取页面标题
     * String title = browserTab.execute((context, page) -> {
     *     page.navigate("https://example.com");
     *     return page.title();
     * });
     *
     * // 获取存储状态
     * String storageState = browserTab.execute((context, page) -> {
     *     return context.storageState();
     * });
     * </pre>
     *
     * @param function 操作函数，第一个参数是Context，第二个参数是Page
     * @param <T>      返回类型
     * @return 操作结果
     */
    public <T> T execute(BiFunction<BrowserContext, Page, T> function) {
        return browserInstance.submitAndWait(() -> {
            checkActive();
            return function.apply(context, page);
        });
    }

    /**
     * 在 BrowserInstance 的专属线程中异步执行自定义操作，返回 Future。
     * <p>
     * 此方法允许调用方自行决定何时等待结果，适合需要并行执行多个操作的场景。
     * </p>
     *
     * @param function 操作函数，第一个参数是Context，第二个参数是Page
     * @param <T>      返回类型
     * @return Future 对象，可用于获取异步执行结果
     */
    public <T> Future<T> executeAsync(BiFunction<BrowserContext, Page, T> function) {
        return browserInstance.submit(() -> {
            checkActive();
            return function.apply(context, page);
        });
    }

    /**
     * 导航到指定URL。
     *
     * @param url URL地址
     * @return 响应对象
     */
    public Response navigate(String url) {
        return this.execute((context, page)-> page.navigate(url));
    }

    /**
     * 导航到指定URL（带选项）。
     *
     * @param url     URL地址
     * @param options 导航选项
     * @return 响应对象
     */
    public Response navigate(String url, Page.NavigateOptions options) {
        return this.execute((context, page) -> page.navigate(url, options));
    }

    /**
     * 等待页面加载状态。
     *
     * @param state 加载状态
     */
    public void waitForLoadState(LoadState state) {
        this.<Void>execute((context, page) -> {
            page.waitForLoadState(state);
            return null;
        });
    }

    /**
     * 获取当前页面URL。
     *
     * @return 当前URL
     */
    public String url() {
        return this.<String>execute((context, page) -> page.url());
    }

    /**
     * 获取页面标题。
     *
     * @return 页面标题
     */
    public String title() {
        return this.<String>execute((context, page) -> page.title());
    }

    /**
     * 获取页面的完整HTML内容。
     *
     * @return 页面的完整HTML内容
     */
    public String content() {
        return this.<String>execute((context, page) -> page.content());
    }

    /**
     * 重新加载当前页面。
     *
     * @return 主资源响应
     */
    public Response reload() {
        return this.execute((context, page) -> page.reload());
    }

    /**
     * 重新加载当前页面（带选项）。
     *
     * @param options 重新加载选项
     * @return 主资源响应
     */
    public Response reload(Page.ReloadOptions options) {
        return this.execute((context, page) -> page.reload(options));
    }

    /**
     * 捕获页面截图。
     *
     * @param options 截图选项
     * @return 截图字节数组
     */
    public byte[] screenshot(Page.ScreenshotOptions options) {
        return this.execute((context, page) -> page.screenshot(options));
    }

    /**
     * 在浏览器上下文中执行JavaScript。
     *
     * @param expression JavaScript表达式
     * @return 执行结果
     */
    public Object evaluate(String expression) {
        return this.execute((context, page) -> page.evaluate(expression));
    }

    /**
     * 在浏览器上下文中执行JavaScript（带参数）。
     *
     * @param expression JavaScript表达式
     * @param arg 传递给表达式的参数
     * @return 执行结果
     */
    public Object evaluate(String expression, Object arg) {
        return this.execute((context, page) -> page.evaluate(expression, arg));
    }

    // ==================== Storage State 方法 ====================

    /**
     * 获取当前浏览器上下文的存储状态（JSON格式）。
     * <p>
     * 使用Playwright原生的 {@link BrowserContext#storageState()} 方法，
     * 返回包含cookies、localStorage和IndexedDB的完整存储状态JSON字符串。
     * </p>
     * <p>
     * <strong>性能说明：</strong>
     * 这是原生API调用，性能最优，可以获取整个上下文的完整存储状态。
     * </p>
     * <p>使用示例：</p>
     * <pre>
     * String storageState = browserTab.getStorageStateJson();
     * // 保存到文件
     * Files.writeString(Path.of("state.json"), storageState);
     * </pre>
     *
     * @return 存储状态JSON字符串
     * @throws IllegalStateException 如果BrowserTab已关闭
     */
    public String getStorageStateJson() {
        return browserInstance.submitAndWait(() -> {
            checkActive();
            return context.storageState();
        });
    }

    // ==================== 上下文管理方法 ====================

    /**
     * 更新最后使用时间。
     */
    private void checkActive() {
        if (!active.get()) {
            throw new IllegalStateException("BrowserTab [" + browserTabId + "] is shutdown");
        }
        lastActiveTime = System.currentTimeMillis();
    }

    /**
     * 获取上下文ID。
     *
     * @return 上下文ID
     */
    public String getBrowserTabId() {
        return browserTabId;
    }

    /**
     * 获取 WebotSession 对象。
     *
     * @return WebotSession 对象
     */
    public WebotSession getWebotSession() {
        return webotSession;
    }

    /**
     * 判断当前标签页是否处于活动状态。
     *
     * @return true 表示当前标签页处于活动状态，false 表示当前标签页处于非活动状态
     */
    public boolean isActive() {
        return active.get();
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间（毫秒时间戳）
     */
    public long getCreateTime() {
        return createTime;
    }

    /**
     * 获取最后使用时间。
     *
     * @return 最后使用时间（毫秒时间戳）
     */
    public long getLastActiveTime() {
        return lastActiveTime;
    }

    /**
     * 获取持有者线程。
     *
     * @return 持有者线程
     */
    public Thread getHolderThread() {
        return holderThread;
    }

    /**
     * 获取元数据。
     *
     * @param key 键
     * @return 值
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * 设置元数据。
     *
     * @param key   键
     * @param value 值
     */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    /**
     * 移除元数据。
     *
     * @param key 键
     */
    public void removeMetadata(String key) {
        metadata.remove(key);
    }

    // ==================== 资源管理方法 ====================

    /**
     * 关闭浏览器页面上下文。
     * <p>
     * 此方法线程安全，可重复调用（重复调用会忽略）。
     * 实现自 {@link Closeable} 接口，支持 try-with-resources 语句。
     * </p>
     */
    @Override
    public void close() {
        if (active.compareAndSet(true, false)) {
            log.debug("Closing BrowserTab [{}]", browserTabId);

            // 提交关闭任务到 BrowserInstance 的 executor
            try {
                browserInstance.submitAndWait(() -> {
                    // 关闭 Page
                    if (page != null) {
                        try {
                            page.close();
                        } catch (Exception e) {
                            log.warn("Error closing page for BrowserTab [{}]", browserTabId, e);
                        }
                        page = null;
                    }

                    // 关闭 Context
                    if (context != null) {
                        try {
                            context.close();
                        } catch (Exception e) {
                            log.warn("Error closing context for BrowserTab [{}]", browserTabId, e);
                        }
                        context = null;
                    }
                    // 从 BrowserInstance 中移除
                    browserInstance.removeBrowserTab(this);
                    holderThread = null;
                    metadata.clear();
                    return null;
                }, 10, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Error closing BrowserTab [{}]", browserTabId, e);
            }

            log.debug("BrowserTab [{}] closed", browserTabId);
        }
    }

}
