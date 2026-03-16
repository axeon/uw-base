package uw.webot.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.webot.WebotSession;
import uw.webot.conf.WebotProperties;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 浏览器机器人池（Browser Bot Pool）。
 * <p>
 * 基于 Hybrid 混合模式实现，支持 Browser 级别复用和 Tab 级别隔离。
 * 采用分层架构：BotPool -> BrowserGroup -> BrowserInstance -> BrowserTab
 * </p>
 * <p>
 * 主要特性：
 * <ul>
 *   <li>单例模式管理，确保全局唯一实例</li>
 *   <li>按浏览器类型分组管理（chromium、firefox、webkit）</li>
 *   <li>自动扩缩容，根据负载动态调整 Browser 数量</li>
 *   <li>健康监控，定期检查和恢复故障 Browser</li>
 *   <li>完整的统计信息收集和报告</li>
 * </ul>
 * </p>
 *
 * @author axeon
 * @see BrowserGroup
 * @see BrowserInstance
 * @see BrowserTab
 * @since 1.0.0
 */
public class BrowserBotPool {

    private static final Logger log = LoggerFactory.getLogger(BrowserBotPool.class);

    /**
     * 配置属性。
     */
    private final WebotProperties.BotPoolProperties botPoolProperties;

    /**
     * 浏览器组集合（按浏览器类型分组）。
     */
    private final ConcurrentHashMap<String, BrowserGroup> browserGroups = new ConcurrentHashMap<>();

    /**
     * 创建时间。
     */
    private final long createTime;

    /**
     * 健康监控线程池。
     */
    private final ScheduledExecutorService healthMonitorExecutor;

    /**
     * 是否活跃。
     */
    private final AtomicBoolean active = new AtomicBoolean(false);

    /**
     * 私有构造方法。
     * <p>
     * 初始化健康监控和统计收集线程池。
     * </p>
     *
     * @param botPoolProperties 配置属性
     */
    public BrowserBotPool(WebotProperties.BotPoolProperties botPoolProperties) {
        this.botPoolProperties = botPoolProperties;
        this.createTime = System.currentTimeMillis();

        // 启动健康监控线程
        this.healthMonitorExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "webot-pool-health-monitor");
            t.setDaemon(true);
            return t;
        });

        // 定期健康检查
        this.healthMonitorExecutor.scheduleWithFixedDelay(this::healthCheck, 30, 30, TimeUnit.SECONDS);

        this.active.set(true);
    }


    /**
     * 获取或创建 BrowserGroup。
     * <p>
     * 根据浏览器类型获取已有的 Group，或创建新的 Group。
     * 如果配置不匹配（如 headless 设置不同），会关闭旧的 Group 并创建新的。
     * </p>
     *
     * @param browserConfig 浏览器配置
     * @return BrowserGroup 实例，如果创建失败返回 null
     */
    private BrowserGroup getOrCreateGroup(BrowserConfig browserConfig) {
        // 检查是否已存在相同配置的 Group
        return browserGroups.computeIfAbsent(browserConfig.getBrowserGroupTag(), key -> {
            int maxBrowsersPerGroup = botPoolProperties.getMaxBrowsersPerGroup();
            int maxTabsPerBrowser = botPoolProperties.getMaxTabsPerBrowser();
            BrowserGroup group = new BrowserGroup(key, browserConfig, maxBrowsersPerGroup, maxTabsPerBrowser);
            group.init();
            return group;
        });
    }


    /**
     * 获取 BrowserTab（使用 SessionConfig）。
     * <p>
     * 根据 SessionConfig 中的浏览器配置获取或创建 BrowserGroup，
     * 然后在 Group 中获取或创建 BrowserTab。
     * </p>
     *
     * @param webotSession 会话配置
     * @return BrowserTab 实例
     * @throws IllegalArgumentException 配置无效时抛出
     * @throws IllegalStateException    创建 Group 失败时抛出
     * @throws TimeoutException         获取超时（默认 15 秒）时抛出
     */
    public BrowserTab openBrowserTab(WebotSession webotSession) throws TimeoutException {
        checkActive();
        BrowserConfig browserConfig = webotSession.getBrowserConfig();
        BrowserGroup browserGroup = getOrCreateGroup(browserConfig);
        if (browserGroup == null) {
            throw new IllegalStateException("Failed to create or get BrowserGroup for browser type: " + browserConfig.getBrowserType());
        }
        // 使用线程安全的 getBrowserInstance() 方法
        BrowserInstance browserInstance = browserGroup.selectBrowserInstance();
        return browserInstance.createBrowserTab(webotSession);
    }

    /**
     * 执行操作并自动管理 BrowserTab 生命周期。
     * <p>
     * 自动获取 BrowserTab，执行函数，然后归还 BrowserTab。
     * 即使发生异常也会确保 BrowserTab 被归还。
     * </p>
     *
     * @param <T>          返回类型
     * @param webotSession 会话配置
     * @param function     操作函数
     * @return 操作结果
     * @throws Exception 操作过程中发生的任何异常
     */
    public <T> T execute(WebotSession webotSession, BrowserBotFunction<T> function) throws Exception {
        try (BrowserTab browserTab = openBrowserTab(webotSession)) {
            return function.apply(browserTab);
        }
    }

    /**
     * 执行操作（无返回值）。
     * <p>
     * 自动获取 BrowserTab，执行消费者操作，然后归还 BrowserTab。
     * </p>
     *
     * @param webotSession 会话配置
     * @param consumer     操作消费者
     * @throws Exception 操作过程中发生的任何异常
     */
    public void execute(WebotSession webotSession, BrowserBotConsumer consumer) throws Exception {
        execute(webotSession, browserTab -> {
            consumer.accept(browserTab);
            return null;
        });
    }

    /**
     * 健康检查。
     * <p>
     * 定期检查所有 BrowserGroup 的健康状态，
     * 移除不活跃的 Group，下次请求时会自动创建新的。
     * </p>
     */
    private void healthCheck() {
        try {
            if (!active.get()) {
                return;
            }
            log.debug("Starting health check");
            log.debug("Health check completed, active groups: {}", browserGroups.size());
        } catch (Exception e) {
            log.error("Error during health check", e);
        }
    }

    /**
     * 获取实例池统计信息。
     *
     * @return PoolStatistics 统计信息
     */
    public PoolStats getStats() {
        long uptime = System.currentTimeMillis() - createTime;
        List<BrowserGroup.GroupStats> groupStats = browserGroups.values().stream().map(BrowserGroup::getStatistics).collect(Collectors.toList());
        int totalGroups = browserGroups.size();
        int totalBrowsers = 0, maxBrowsers = 0, activeTabs = 0,
                maxTabs = 0, totalBrowsersCreated = 0, totalTabsCreated = 0;
        for (BrowserGroup.GroupStats groupStat : groupStats) {
            totalBrowsers += groupStat.totalBrowsers();
            maxBrowsers += groupStat.maxBrowsers();
            activeTabs += groupStat.activeTabs();
            maxTabs += groupStat.maxTabs();
            totalBrowsersCreated += groupStat.totalBrowsersCreated();
            totalTabsCreated += groupStat.totalTabsCreated();
        }
        return new PoolStats(totalGroups, totalBrowsers, maxBrowsers, activeTabs, maxTabs, totalBrowsersCreated, totalTabsCreated, groupStats, uptime);
    }

    /**
     * 检查是否已关闭。
     *
     * @throws IllegalStateException 如果实例池已关闭
     */
    private void checkActive() {
        if (!active.get()) {
            throw new IllegalStateException("BrowserBotPool is shutdown");
        }
    }

    /**
     * 关闭实例池。
     * <p>
     * 关闭所有资源：
     * <ol>
     *   <li>停止健康监控线程</li>
     *   <li>停止统计收集线程</li>
     *   <li>关闭所有 BrowserGroup</li>
     *   <li>清除单例引用</li>
     * </ol>
     * 此方法线程安全，可重复调用（重复调用会忽略）。
     * </p>
     */
    public synchronized void shutdown() {

        if (active.compareAndSet(true, false)) {
            log.info("Shutting down BrowserBotPool");
            // 关闭健康监控线程
            if (healthMonitorExecutor != null) {
                healthMonitorExecutor.shutdown();
                try {
                    if (!healthMonitorExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                        healthMonitorExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    healthMonitorExecutor.shutdownNow();
                }
            }
            // 关闭所有 Group
            for (BrowserGroup group : browserGroups.values()) {
                try {
                    group.close();
                } catch (Exception e) {
                    log.warn("Error closing group [{}] during pool shutdown", group.getBrowserGroupTag(), e);
                }
            }
            browserGroups.clear();
            log.info("BrowserBotPool shutdown completed");
        }
    }

    /**
     * BrowserBot 功能函数接口。
     *
     * @param <T> 返回类型
     */
    @FunctionalInterface
    public interface BrowserBotFunction<T> {
        /**
         * 应用此函数到 BrowserTab。
         *
         * @param browserTab 浏览器页面上下文
         * @return 操作结果
         * @throws Exception 操作过程中可能抛出的异常
         */
        T apply(BrowserTab browserTab) throws Exception;
    }

    /**
     * BrowserBot 消费函数接口。
     */
    @FunctionalInterface
    public interface BrowserBotConsumer {
        /**
         * 接受 BrowserTab 执行操作。
         *
         * @param browserTab 浏览器页面上下文
         * @throws Exception 操作过程中可能抛出的异常
         */
        void accept(BrowserTab browserTab) throws Exception;
    }

    /**
     * 实例池统计信息记录类。
     * <p>
     * 包含实例池的整体统计信息，包括 Group 数量、Browser 数量、
     * Tab 数量、活跃数量、运行时间等。
     *
     */
    public record PoolStats(int totalGroups, int totalBrowsers, int maxBrowsers, int activeTabs,
                            int maxTabs, long totalBrowsersCreated, long totalTabsCreated,
                            List<BrowserGroup.GroupStats> groupStats, long uptime) {
        /**
         * 计算使用率。
         *
         * @return 使用率（0.0 - 1.0）
         */
        public double utilRate() {
            return (double) activeTabs / maxTabs;
        }

    }
}
