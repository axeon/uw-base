package uw.webot.core;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.webot.WebotSession;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 管理的浏览器实例。
 * <p>
 * 封装 Playwright 的 Browser 实例，管理多个 BrowserTab。
 * 实现 Browser 级别的资源复用和 Page 级别的隔离。
 * </p>
 * <p>
 * 线程安全说明：
 * 每个 BrowserInstance 包含一个单线程执行器（ExecutorService），
 * 所有 Playwright 相关操作都在该线程中串行执行，确保线程安全。
 * </p>
 *
 * @author axeon
 * @see BrowserTab
 * @see BrowserGroup
 * @since 1.0.0
 */
public class BrowserInstance {

    private static final Logger log = LoggerFactory.getLogger(BrowserInstance.class);

    /**
     * 单线程执行器 - 所有 Playwright 操作在此线程中串行执行。
     */
    private final ExecutorService executor;

    /**
     * 浏览器 ID。
     */
    private final String browserId;

    /**
     * 浏览器 配置。
     */
    private final BrowserConfig browserConfig;

    /**
     * 所属的 BrowserGroup
     */
    private final BrowserGroup browserGroup;

    /**
     * Playwright 实例。
     */
    private Playwright playwright;

    /**
     * 浏览器实例。
     */
    private Browser browser;

    /**
     * 管理的 Page 集合。
     */
    private final ConcurrentHashMap<String, BrowserTab> browserTabMap = new ConcurrentHashMap<>();

    /**
     * Page ID 生成器。
     */
    private final AtomicInteger tabIdGenerator = new AtomicInteger(0);

    /**
     * 创建时间。
     */
    private final long createTime;

    /**
     * 最后活跃时间。
     */
    private long lastActiveTime;

    /**
     * 最大 Page 数量。
     */
    private final int maxTabsPerBrowser;

    /**
     * 是否活跃。
     */
    private final AtomicBoolean active = new AtomicBoolean(false);

    /**
     * 构造方法。
     *
     * @param browserId         浏览器 ID
     * @param browserGroup      所属的 BrowserBotPool
     * @param browserConfig     浏览器配置
     * @param maxTabsPerBrowser 最大 Page 数量
     */
    protected BrowserInstance(String browserId, BrowserGroup browserGroup, BrowserConfig browserConfig, int maxTabsPerBrowser) {
        if (browserId == null) {
            throw new IllegalArgumentException("browserId cannot be null");
        }
        if (browserGroup == null) {
            throw new IllegalArgumentException("ownerPool cannot be null");
        }
        this.browserId = browserId;
        this.browserGroup = browserGroup;
        this.browserConfig = browserConfig;
        this.maxTabsPerBrowser = Math.max(1, maxTabsPerBrowser);
        this.createTime = System.currentTimeMillis();
        this.lastActiveTime = System.currentTimeMillis();

        // 创建单线程执行器 - 确保所有 Playwright 操作串行执行
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, browserId);
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 初始化浏览器。
     * <p>
     * 在专属线程中创建 Playwright 和 Browser 实例。
     * </p>
     */
    protected void init() {
        submitAndWait(() -> {
            try {
                log.debug("Initializing BrowserInstance [{}] in executor thread", browserId);
                // 创建 Playwright
                this.playwright = Playwright.create();
                // 创建浏览器启动选项
                BrowserType.LaunchOptions launchOpts = new BrowserType.LaunchOptions()
                        .setHeadless(browserConfig.isHeadless());
                // 添加额外参数
                List<String> args = browserConfig.getArgs();
                if (args != null && !args.isEmpty()) {
                    launchOpts.setArgs(args);
                }

                // 如果指定了可执行文件路径，使用它
                String executablePath = browserConfig.getExecutablePath();
                if (executablePath != null && !executablePath.isEmpty()) {
                    launchOpts.setExecutablePath(java.nio.file.Paths.get(executablePath));
                }

                // 创建浏览器
                uw.webot.core.BrowserType browserType = browserConfig.getBrowserType();
                switch (browserType) {
                    case FIREFOX:
                        this.browser = playwright.firefox().launch(launchOpts);
                        break;
                    case WEBKIT:
                        this.browser = playwright.webkit().launch(launchOpts);
                        break;
                    case CHROMIUM:
                    default:
                        this.browser = playwright.chromium().launch(launchOpts);
                        break;
                }

                log.info("BrowserInstance [{}] initialized successfully", browserId);
                active.set(true);
            } catch (Exception e) {
                log.error("Failed to initialize BrowserInstance [{}]", browserId, e);
                close();
                throw e;
            }
            return null;
        });
    }

    /**
     * 创建 BrowserTab。
     * <p>
     * 在专属线程中创建 BrowserTab，确保线程安全。
     * </p>
     *
     * @param webotSession session 配置
     * @return BrowserTab 实例
     */
    protected synchronized BrowserTab createBrowserTab(WebotSession webotSession) {
            checkActive();
            if (browserTabMap.size() >= maxTabsPerBrowser) {
                throw new IllegalStateException("Maximum number of tabs reached: " + maxTabsPerBrowser);
            }
            String browserTabId = browserId + ":tab-" + tabIdGenerator.incrementAndGet();
            BrowserTab browserTab = new BrowserTab(browserTabId, this, webotSession);
            browserTab.init(); // 在专属线程中初始化
            browserTabMap.put(browserTabId, browserTab);
            log.debug("BrowserTab [{}] opened from browser [{}], active count: {}", browserTab.getBrowserTabId(), browserId, browserTabMap.size());
            return browserTab;
    }

    /**
     * 获取 BrowserTab。
     *
     * @param browserTabId Tab ID
     * @return BrowserTab 实例
     */
    protected BrowserTab getBrowserTab(String browserTabId) {
        return browserTabMap.get(browserTabId);
    }

    /**
     * 移除 BrowserTab。
     *
     * @param browserTab BrowserTab 实例
     */
    protected void removeBrowserTab(BrowserTab browserTab) {
        if (browserTab == null) {
            return;
        }
        browserTabMap.remove(browserTab.getBrowserTabId());
    }

    /**
     * 获取浏览器实例（只能在 executor 线程中调用）。
     *
     * @return 浏览器实例
     * @throws IllegalStateException 如果不是在 executor 线程中调用
     */
    protected Browser getRawBrowser() {
        if (!isExecutorThread()) {
            throw new IllegalStateException("getRawBrowser() can only be called within the executor thread. Use submitAndWait() instead.");
        }
        return browser;
    }

    /**
     * 获取 Playwright 实例（只能在 executor 线程中调用）。
     *
     * @return Playwright 实例
     * @throws IllegalStateException 如果不是在 executor 线程中调用
     */
    protected Playwright getRawPlaywright() {
        if (!isExecutorThread()) {
            throw new IllegalStateException("getRawPlaywright() can only be called within the executor thread. Use submitAndWait() instead.");
        }
        return playwright;
    }

    /**
     * 检查当前线程是否是 executor 线程。
     *
     * @return true 如果当前线程是 executor 线程
     */
    private boolean isExecutorThread() {
        return Thread.currentThread().getName().equals(browserId);
    }

    /**
     * 提交任务到专属线程执行，并等待结果。
     *
     * @param task 任务
     * @param <T>  返回类型
     * @return 任务执行结果
     */
    public <T> T submitAndWait(Callable<T> task) {
        try {
            Future<T> future = executor.submit(task);
            return future.get(60, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Task execution timeout", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Task execution interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException("Task execution failed", cause);
        }
    }

    /**
     * 提交任务到专属线程执行，并等待结果（带超时）。
     *
     * @param task    任务
     * @param timeout 超时时间
     * @param unit    时间单位
     * @param <T>     返回类型
     * @return 任务执行结果
     */
    public <T> T submitAndWait(Callable<T> task, long timeout, TimeUnit unit) {
        try {
            Future<T> future = executor.submit(task);
            return future.get(timeout, unit);
        } catch (TimeoutException e) {
            throw new RuntimeException("Task execution timeout", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Task execution interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException("Task execution failed", cause);
        }
    }

    /**
     * 提交任务到专属线程执行，返回 Future（异步）。
     * <p>
     * 此方法允许调用方自行决定何时等待结果，或注册回调处理结果。
     * </p>
     *
     * @param task 任务
     * @param <T>  返回类型
     * @return Future 对象，可用于获取异步执行结果
     */
    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }

    /**
     * 检查是否已关闭。
     */
    private void checkActive() {
        if (!active.get()) {
            throw new IllegalStateException("BrowserInstance [" + browserId + "] is shutdown");
        }
        lastActiveTime = System.currentTimeMillis();
    }

    /**
     * 获取浏览器 ID。
     *
     * @return 浏览器 ID
     */
    public String getBrowserId() {
        return browserId;
    }

    /**
     * 检查浏览器是否活跃。
     *
     * @return true 如果浏览器处于活跃状态
     */
    public boolean isActive() {
        return active.get();
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public long getCreateTime() {
        return createTime;
    }

    /**
     * 获取最后活跃时间。
     *
     * @return 最后活跃时间
     */
    public long getLastActiveTime() {
        return lastActiveTime;
    }

    /**
     * 获取活跃 Page 数量。
     *
     * @return 活跃 Page 数量
     */
    public int getActiveTabCount() {
        return browserTabMap.size();
    }

    /**
     * 获取总容量。
     *
     * @return 总容量
     */
    public int getTotalCapacity() {
        return maxTabsPerBrowser;
    }

    /**
     * 获取总创建的 Tab 数量。
     *
     * @return 总创建的 Tab 数量
     */
    public int getTotalTabsCreated() {
        return tabIdGenerator.get();
    }

    /**
     * 关闭浏览器实例。
     */
    public void close() {
        if (active.compareAndSet(true, false)) {
            log.debug("Closing BrowserInstance [{}]", browserId);

            // 从 BrowserGroup 中移除
            this.browserGroup.removeBrowserInstance(this);

            // 关闭所有 Page
            for (BrowserTab browserTab : browserTabMap.values()) {
                try {
                    browserTab.close();
                } catch (Exception e) {
                    log.warn("Error closing browserTab [{}] during browser shutdown", browserTab.getBrowserTabId(), e);
                }
            }
            browserTabMap.clear();

            // 在 executor 线程中执行清理playwright
            submitAndWait(() -> {
                // 关闭浏览器
                try {
                    if (browser != null) {
                        browser.close();
                        browser = null;
                    }
                } catch (Exception e) {
                    log.warn("Error closing browser during cleanup", e);
                }

                // 关闭 Playwright
                try {
                    if (playwright != null) {
                        playwright.close();
                        playwright = null;
                    }
                } catch (Exception e) {
                    log.warn("Error closing playwright during cleanup", e);
                }
                return null;
            });

            // 关闭 executor
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.debug("BrowserInstance [{}] closed", browserId);
        }
    }

    /**
     * 获取浏览器统计信息。
     */
    public BrowserStats getStats() {
        return new BrowserStats(browserId, browserConfig.getBrowserGroupTag(), browserTabMap.size(), maxTabsPerBrowser, tabIdGenerator.get(), System.currentTimeMillis() - createTime);
    }

    /**
     * 浏览器统计信息记录类。
     */
    public record BrowserStats(String browserId, String browserGroupTag, int activeTabs, int maxTabs,
                               long totalTabsCreated, long uptime) {
    }
}
