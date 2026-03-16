package uw.webot.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import uw.webot.WebotSession;
import uw.webot.conf.WebotProperties;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BrowserInstancePool单元测试。
 */
class BrowserBotPoolTest {

    private WebotProperties properties;
    private BrowserBotPool instancePool;

    @BeforeEach
    void setUp() throws Exception {
        properties = new WebotProperties();
        properties.getBotPool().setMaxBrowsersPerGroup(2);
        properties.getBotPool().setMaxTabsPerBrowser(5);

        instancePool = new BrowserBotPool(properties.getBotPool());
    }

    @AfterEach
    void tearDown() {
        if (instancePool != null) {
            instancePool.shutdown();
        }
    }

    @Test
    @Timeout(30)
    void testAcquireAndCloseBrowserTab() throws Exception {
        WebotSession webotSession = createDefaultConfig();

        BrowserTab browserTab = instancePool.openBrowserTab(webotSession);
        assertNotNull(browserTab);
        assertNotNull(browserTab.getBrowserTabId());

        // 归还Context
        browserTab.close();

        // 验证统计信息
        BrowserBotPool.PoolStats stats = instancePool.getStats();
        assertEquals(1, stats.totalTabsCreated());
        assertEquals(0, stats.activeTabs());
    }


    @Test
    @Timeout(60)
    void testConcurrentAcquire() throws Exception {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    WebotSession webotSession = createDefaultConfig();
                    BrowserTab browserTab = instancePool.openBrowserTab(webotSession);

                    // 模拟一些操作
                    Thread.sleep(100);

                    browserTab.close();
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        assertEquals(threadCount, successCount.get());

        // 验证所有Context都已归还
        BrowserBotPool.PoolStats stats = instancePool.getStats();
        assertEquals(0, stats.activeTabs());

        executor.shutdown();
    }

    @Test
    @Timeout(30)
    void testDifferentBrowserTypes() throws Exception {
        // Chromium
        WebotSession chromiumConfig = WebotSession.builder()
                .browserConfig(BrowserConfig.builder().browserType(BrowserType.CHROMIUM).build())
                .build();

        BrowserTab chromiumContext = instancePool.openBrowserTab(chromiumConfig);
        assertNotNull(chromiumContext);
        chromiumContext.close();

        // Firefox
        WebotSession firefoxConfig = WebotSession.builder()
                .browserConfig(BrowserConfig.builder().browserType(BrowserType.FIREFOX).build())
                .build();

        BrowserTab firefoxContext = instancePool.openBrowserTab(firefoxConfig);
        assertNotNull(firefoxContext);
        firefoxContext.close();

        // 验证有两个Group
        BrowserBotPool.PoolStats stats = instancePool.getStats();
        assertEquals(2, stats.totalGroups());
    }

    @Test
    @Timeout(30)
    void testExecuteFunction() throws Exception {
        WebotSession webotSession = createDefaultConfig();

        String result = instancePool.execute(webotSession, context -> {
            return "Context ID: " + context.getBrowserTabId();
        });

        assertNotNull(result);
        assertTrue(result.contains("Context ID:"));

        // 验证Context已自动归还
        BrowserBotPool.PoolStats stats = instancePool.getStats();
        assertEquals(0, stats.activeTabs());
    }

    @Test
    @Timeout(30)
    void testStatistics() throws Exception {
        WebotSession webotSession = createDefaultConfig();

        // 获取多个Context
        BrowserTab tab1 = instancePool.openBrowserTab(webotSession);
        BrowserTab tab2 = instancePool.openBrowserTab(webotSession);

        BrowserBotPool.PoolStats stats = instancePool.getStats();
        assertTrue(stats.totalTabsCreated() >= 2);
        assertEquals(2, stats.activeTabs());
        assertTrue(stats.maxTabs() > 0);
        assertTrue(stats.uptime() > 0);

        tab1.close();
        tab2.close();
    }

    @Test
    @Timeout(30)
    void testInvalidConfig() {
        WebotSession webotSession = createDefaultConfig();

        assertThrows(IllegalArgumentException.class, () -> {
            instancePool.openBrowserTab(webotSession);
        });
    }

    private WebotSession createDefaultConfig() {
        return WebotSession.builder()
                .browserConfig(new BrowserConfig())
                .build();
    }
}
