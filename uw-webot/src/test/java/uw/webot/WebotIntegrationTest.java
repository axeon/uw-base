package uw.webot;

import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import uw.webot.conf.WebotAutoConfiguration;
import uw.webot.core.BrowserBotPool;
import uw.webot.core.BrowserConfig;
import uw.webot.core.BrowserTab;
import uw.webot.core.BrowserType;
import uw.webot.session.SessionConfig;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Hybrid 模式集成测试。
 * 验证 WebotManager 与 BrowserInstancePool 的集成。
 */
@SpringBootTest(classes = {WebotAutoConfiguration.class})
@TestPropertySource(properties = {"logging.level.uw.webot=DEBUG","uw.webot.enabled=true", "uw.webot.botPool.max-browsers-per-group=1", "uw.webot.botPool.max-tabs-per-browser=5",})
class WebotIntegrationTest {

    @Autowired
    private WebotManager webotManager;

    @Test
    void testCreateSessionAndAcquireContext() throws Exception {
        // 创建会话
        WebotSession session = webotManager.createSession(SessionConfig.builder().browserConfig(BrowserConfig.builder().browserType(BrowserType.CHROMIUM).headless(false).viewportHeight(1920).viewportWidth(1080).build()).build());
        assertNotNull(session);

        // 获取Context
        try(BrowserTab browserTab = webotManager.openBrowserTab(session)) {
            browserTab.navigate("https://www.baidu.com");
            browserTab.waitForLoadState(LoadState.NETWORKIDLE);
            System.out.println(browserTab.title());
            assertNotNull(browserTab);
            Thread.sleep(5_000);
        }
        // 销毁会话
        webotManager.destroySession(session.getSessionId());
    }

    @Test
    void testExecuteWithSession() throws Exception {
        WebotSession session = webotManager.createSession();

        String result = webotManager.execute(session, browserTab -> {
            return "browserTab: " + browserTab.getBrowserTabId();
        });

        System.out.println(result);
        assertNotNull(result);
        assertTrue(result.contains("browserTab:"));

        webotManager.destroySession(session.getSessionId());
    }

    @Test
    void testPoolStatistics() {
        BrowserBotPool.PoolStats stats = webotManager.getStats();
        assertNotNull(stats);
    }

    @Test
    void testConcurrentSessions() throws Exception {
        int sessionCount = 10;
        CountDownLatch latch = new CountDownLatch(sessionCount);
        AtomicInteger successCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(sessionCount);

        for (int i = 0; i < sessionCount; i++) {
            executor.submit(() -> {
                WebotSession session = webotManager.createSession(SessionConfig.builder().browserConfig(BrowserConfig.builder().browserType(BrowserType.CHROMIUM).headless(false).viewportHeight(1920).viewportWidth(1080).build()).build());
                try (BrowserTab browserTab = webotManager.openBrowserTab(session)) {
                    browserTab.navigate("https://www.baidu.com");
                    browserTab.waitForLoadState(LoadState.NETWORKIDLE);
                    System.out.println(browserTab.title());
                    Thread.sleep(5_000);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
                webotManager.destroySession(session.getSessionId());
            });
        }

//        assertTrue(latch.await(120, TimeUnit.SECONDS));
//        assertEquals(sessionCount, successCount.get());
//        executor.shutdown();
        Thread.sleep(1200_000);
    }

    @Test
    void testCustomSessionConfig() throws Exception {
        WebotSession webotSession = WebotSession.builder().browserConfig(BrowserConfig.builder().browserType(BrowserType.CHROMIUM).headless(true).viewportHeight(1920).viewportWidth(1080).build()).build();

        WebotSession session = webotManager.createSession();
        assertNotNull(session);

        BrowserTab browserTab = webotManager.openBrowserTab(session);
        assertNotNull(browserTab);
        browserTab.close();
        webotManager.destroySession(session.getSessionId());
    }
}
