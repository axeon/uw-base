package uw.webot.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 浏览器组。
 * <p>
 * 管理相同浏览器类型的多个 BrowserInstance 实例。
 * 实现负载均衡和故障转移。
 * </p>
 *
 * @author axeon
 * @see BrowserInstance
 * @see BrowserBotPool
 * @since 1.0.0
 */
public class BrowserGroup {

    private static final Logger log = LoggerFactory.getLogger(BrowserGroup.class);


    /**
     * Group ID。
     */
    private final String browserGroupTag;
    /**
     * 浏览器配型。
     */
    private final BrowserConfig browserConfig;

    /**
     * 可用浏览器队列（用于负载均衡）。
     */
    private final BlockingQueue<BrowserInstance> browserQueue;

    /**
     * Browser ID 生成器。
     */
    private final AtomicInteger browserIdGenerator = new AtomicInteger(0);

    /**
     * 最大 Browser 数量。
     */
    private final int maxBrowsersPerGroup;

    /**
     * 每个 Browser 的最大Tab数量。
     */
    private final int maxTabsPerBrowser;

    /**
     * 是否活跃。
     */
    private final AtomicBoolean active = new AtomicBoolean(false);

    /**
     * 创建时间。
     */
    private final long createTime;

    /**
     * 最后活跃时间。
     */
    private long lastActiveTime;

    /**
     * 构造方法。
     *
     * @param browserConfig       浏览器类型
     * @param maxBrowsersPerGroup 最大 Browser 数量
     * @param maxTabsPerBrowser   每个 Browser 的最大 Context 数量
     */
    protected BrowserGroup(String browserGroupTag, BrowserConfig browserConfig, int maxBrowsersPerGroup, int maxTabsPerBrowser) {
        this.browserGroupTag = browserGroupTag;
        this.browserConfig = browserConfig;
        this.maxBrowsersPerGroup = Math.max(1, maxBrowsersPerGroup);
        this.maxTabsPerBrowser = Math.max(1, maxTabsPerBrowser);
        this.createTime = System.currentTimeMillis();
        this.lastActiveTime = System.currentTimeMillis();
        this.browserQueue = new LinkedBlockingQueue<>(this.maxBrowsersPerGroup);
    }

    /**
     * 初始化方法。
     * <p>
     * 创建第一个 Browser。
     * </p>
     */
    protected void init() {
        log.debug("Initializing BrowserGroup [{}]", browserGroupTag);
        active.set(true);
    }

    /**
     * 获取 BrowserInstance（线程安全）。
     * <p>
     * 此方法是线程安全的，使用 synchronized 确保以下操作的原子性：
     * <ol>
     *   <li>选择可用的 BrowserInstance</li>
     *   <li>如果没有则创建新的</li>
     *   <li>验证浏览器实例仍然活跃</li>
     * </ol>
     *
     * @return BrowserInstance 实例
     * @throws TimeoutException 如果无法获取可用的浏览器实例
     */
    public synchronized BrowserInstance selectBrowserInstance() throws TimeoutException {
        checkActive();

        // 使用轮询策略选择 Browser
        BrowserInstance browserInstance = browserQueue.poll();
        if (browserInstance != null && browserInstance.isActive()) {
            browserQueue.add(browserInstance);
            return browserInstance;
        }
        if (browserInstance == null || !browserInstance.isActive()) {
            // 没有可用 Browser，尝试创建新的
            if (browserQueue.size() < maxBrowsersPerGroup) {
                browserInstance = createBrowserInstance();
            }
            if (browserInstance == null || !browserInstance.isActive()) {
                throw new TimeoutException("No available browserInstance in group [" + browserGroupTag + "]");
            }
        }
        return browserInstance;
    }

    /**
     * 创建新 Browser。
     *
     * @return BrowserInstance 实例
     */
    private synchronized BrowserInstance createBrowserInstance() {
        if (browserQueue.size() >= maxBrowsersPerGroup) {
            return null;
        }
        String browserId = browserGroupTag + ":browser-" + browserIdGenerator.incrementAndGet();
        BrowserInstance browserInstance = new BrowserInstance(browserId, this, browserConfig, maxTabsPerBrowser);
        try {
            browserInstance.init();
            browserQueue.add(browserInstance);
            return browserInstance;
        } catch (Exception e) {
            log.error("Failed to create BrowserInstance [{}] in group [{}]", browserId, browserGroupTag, e);
            browserInstance.close();
            return null;
        }
    }

    /**
     * 移除 BrowserInstance。
     *
     * @param browserInstance
     */
    protected synchronized void removeBrowserInstance(BrowserInstance browserInstance) {
        browserQueue.remove(browserInstance);
    }

    /**
     * 获取 Group ID。
     *
     * @return Group ID
     */
    public String getBrowserGroupTag() {
        return browserGroupTag;
    }

    /**
     * 检查 Group 是否活跃。
     *
     * @return true 如果 Group 处于活跃状态
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
     * 关闭 Group。
     */
    public synchronized void close() {
        if (active.compareAndSet(true, false)) {
            log.debug("Closing BrowserGroup [{}]", browserGroupTag);

            // 关闭所有 Browser
            for (BrowserInstance browserInstance : browserQueue) {
                try {
                    browserInstance.close();
                } catch (Exception e) {
                    log.warn("Error closing browserInstance [{}] during group shutdown", browserInstance.getBrowserId(), e);
                }
            }
            browserQueue.clear();
            log.debug("BrowserGroup [{}] closed", browserGroupTag);
        }
    }

    /**
     * 检查是否活跃。
     *
     * @throws IllegalStateException 如果 Group 未初始化
     */
    private void checkActive() {
        if (!active.get()) {
            throw new IllegalStateException("BrowserGroup [" + browserGroupTag + "] is not active");
        }
        lastActiveTime = System.currentTimeMillis();
    }

    /**
     * 获取统计信息。
     *
     * @return GroupStatistics 统计信息
     */
    public GroupStats getStatistics() {
        int activeTabs = 0, totalTabsCreated = 0;
        int maxTabs = maxBrowsersPerGroup * maxTabsPerBrowser;
        for (BrowserInstance browser : browserQueue) {
            activeTabs += browser.getActiveTabCount();
            totalTabsCreated += browser.getTotalTabsCreated();
        }
        return new GroupStats(browserGroupTag, browserQueue.size(), maxBrowsersPerGroup, activeTabs, maxTabs, browserIdGenerator.get(), totalTabsCreated, System.currentTimeMillis());
    }

    /**
     * 浏览器组统计信息。
     *
     */
    public record GroupStats(String browserGroupTag, int totalBrowsers, int maxBrowsers, int activeTabs, int maxTabs,
                             int totalBrowsersCreated, int totalTabsCreated, long uptime) {

        /**
         * 获取浏览器组使用率。
         *
         * @return 使用率
         */
        public double getUtilRate() {
            return (double) activeTabs / maxTabs;
        }

    }

}
