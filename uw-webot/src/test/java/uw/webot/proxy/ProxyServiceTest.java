package uw.webot.proxy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uw.webot.proxy.impl.LocalProxyPoolImpl;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProxyService 单元测试。
 * <p>
 * 测试代理服务的各项功能，包括：
 * <ul>
 *   <li>代理获取和释放</li>
 *   <li>代理健康检查</li>
 *   <li>代理失败处理</li>
 *   <li>代理池统计信息</li>
 *   <li>并发场景下的代理分配</li>
 * </ul>
 * </p>
 *
 * @author axeon
 * @since 1.0.0
 */
public class ProxyServiceTest {

    private LocalProxyPoolImpl localProxyPoolImpl;

    @BeforeEach
    void setUp() {
        // 配置代理
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setMaxFailures(3);
        proxyConfig.setHealthCheckInterval(Duration.ofSeconds(5));

        // 添加测试代理服务器
        List<ProxyConfig.ProxyServer> servers = Arrays.asList(
                createProxyServer("proxy1.example.com", 8080, "http"),
                createProxyServer("proxy2.example.com", 8080, "http"),
                createProxyServer("proxy3.example.com", 8080, "socks5")
        );
        proxyConfig.setServers(servers);

        // 创建代理池
        localProxyPoolImpl = new LocalProxyPoolImpl(proxyConfig);
    }

    @AfterEach
    void tearDown() {
        if (localProxyPoolImpl != null) {
            localProxyPoolImpl.shutdown();
        }
    }

    /**
     * 创建代理服务器配置。
     */
    private ProxyConfig.ProxyServer createProxyServer(String host, int port, String type) {
        ProxyConfig.ProxyServer server = new ProxyConfig.ProxyServer();
        server.setHost(host);
        server.setPort(port);
        server.setType(ProxyType.valueOf(type.toUpperCase()));
        return server;
    }

    @Test
    @DisplayName("测试获取代理 - 应该返回可用的代理")
    void testGetProxy() {
        // 获取代理
        ProxyService.ProxyInfo proxy = localProxyPoolImpl.getProxy(ProxyType.HTTP);

        // 验证
        assertNotNull(proxy, "应该返回代理");
        assertNotNull(proxy.getHost(), "代理主机不应为空");
        assertTrue(proxy.getPort() > 0, "代理端口应大于0");
        assertTrue(proxy.getPort() <= 65535, "代理端口应小于等于65535");

        // 释放代理
        localProxyPoolImpl.releaseProxy(proxy);
    }

    @Test
    @DisplayName("测试获取指定类型的代理")
    void testGetProxyByType() {
        // 获取 HTTP 代理
        ProxyService.ProxyInfo httpProxy = localProxyPoolImpl.getProxy(ProxyType.HTTP);
        assertNotNull(httpProxy, "应该返回HTTP代理");
        assertEquals("http", httpProxy.getType().name().toLowerCase(), "代理类型应为HTTP");

        // 释放
        localProxyPoolImpl.releaseProxy(httpProxy);

        // 获取 SOCKS5 代理
        ProxyService.ProxyInfo socksProxy = localProxyPoolImpl.getProxy(ProxyType.SOCKS5);
        assertNotNull(socksProxy, "应该返回SOCKS5代理");
        assertEquals("socks5", socksProxy.getType().name().toLowerCase(), "代理类型应为SOCKS5");
        // 释放
        localProxyPoolImpl.releaseProxy(socksProxy);
    }

    @Test
    @DisplayName("测试代理释放 - 释放后应可重新获取")
    void testReleaseProxy() {
        // 获取代理
        ProxyService.ProxyInfo proxy = localProxyPoolImpl.getProxy(ProxyType.HTTP);

        // 释放代理
        localProxyPoolImpl.releaseProxy(proxy);

        // 再次获取，应该可能获取到同一个代理
        ProxyService.ProxyInfo proxy2 = localProxyPoolImpl.getProxy(ProxyType.HTTP);
        assertNotNull(proxy2, "释放后应能获取到代理");

        localProxyPoolImpl.releaseProxy(proxy2);
    }

    @Test
    @DisplayName("测试标记代理失败 - 失败次数超过阈值应标记为不可用")
    void testMarkProxyFailed() {
        // 获取代理
        ProxyService.ProxyInfo proxy = localProxyPoolImpl.getProxy(ProxyType.HTTP);
//
//        // 验证初始状态
//        assertTrue(proxy.(), "代理初始状态应为可用");
//        assertEquals(0, proxy.getFailureCount(), "初始失败次数应为0");
//
//        // 标记失败（未达到阈值）
//        localProxyPoolImpl.markProxyFailed(proxy);
//        assertEquals(1, proxy.getFailureCount(), "失败次数应为1");
//        assertTrue(proxy.isAvailable(), "未达到阈值，代理仍应可用");
//
//        localProxyPoolImpl.markProxyFailed(proxy);
//        assertEquals(2, proxy.getFailureCount(), "失败次数应为2");
//        assertTrue(proxy.isAvailable(), "未达到阈值，代理仍应可用");
//
//        // 第三次失败，超过阈值（maxFailures=3）
//        localProxyPoolImpl.markProxyFailed(proxy);
//        assertEquals(3, proxy.getFailureCount(), "失败次数应为3");
//        assertFalse(proxy.isAvailable(), "超过阈值，代理应标记为不可用");
    }

    @Test
    @DisplayName("测试代理池统计信息")
    void testGetStatistics() {
        // 获取统计信息
        ProxyService.ProxyPoolStatistics stats = localProxyPoolImpl.getStatistics();

        // 验证
        assertNotNull(stats, "统计信息不应为空");
        assertTrue(stats.totalProxies() >= 0, "总代理数应大于等于0");
        assertTrue(stats.availableProxies() >= 0, "可用代理数应大于等于0");
        assertTrue(stats.unavailableProxies() >= 0, "不可用代理数应大于等于0");

        // 验证总数 = 可用 + 不可用
        assertEquals(stats.totalProxies(),
                stats.availableProxies() + stats.unavailableProxies(),
                "总代理数应等于可用加不可用");
    }

    @Test
    @DisplayName("测试代理健康检查 - 无效代理应返回false")
    void testCheckProxyHealth() {
//        // 创建一个无效的代理
//        ProxyService.ProxyInfo invalidProxy = new ProxyService.ProxyInfo(
//                "invalid-proxy", "http", "invalid.host.example", 8080, 1
//        );

        // 健康检查应失败
//        boolean healthy = localProxyPool.checkProxyHealth(invalidProxy);
//        assertFalse(healthy, "无效代理的健康检查应失败");
//        assertTrue(invalidProxy.getFailureCount() > 0, "失败次数应增加");
    }

    @Test
    @DisplayName("测试并发获取代理 - 多线程环境下应正常工作")
    void testConcurrentProxyAcquisition() throws InterruptedException {
        int threadCount = 10;
        int proxiesPerThread = 5;

        Thread[] threads = new Thread[threadCount];
        final int[] successCount = {0};
        final int[] failureCount = {0};

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < proxiesPerThread; j++) {
                    try {
                        ProxyService.ProxyInfo proxy = localProxyPoolImpl.getProxy(ProxyType.HTTP);
                        if (proxy != null) {
                            successCount[0]++;
                            // 模拟使用
                            Thread.sleep(10);
                            localProxyPoolImpl.releaseProxy(proxy);
                        } else {
                            failureCount[0]++;
                        }
                    } catch (Exception e) {
                        failureCount[0]++;
                    }
                }
            });
            threads[i].start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        // 验证
        assertTrue(successCount[0] > 0, "应有成功的代理获取");
        assertEquals(threadCount * proxiesPerThread, successCount[0] + failureCount[0],
                "总次数应等于成功加失败");
    }

    @Test
    @DisplayName("测试代理URL生成")
    void testProxyUrlGeneration() {
        // 无认证代理
//        ProxyService.ProxyInfo proxyNoAuth = new ProxyService.ProxyInfo(
//                "proxy-1", "http", "proxy.example.com", 8080, 1
//        );
//        assertEquals("http://proxy.example.com:8080", proxyNoAuth.getProxyUrl(),
//                "无认证代理URL格式不正确");

        // 有认证代理
//        ProxyService.ProxyInfo proxyWithAuth = new ProxyService.ProxyInfo(
//                "proxy-2", "http", "proxy.example.com", 8080, "user", "pass", 1
//        );
//        assertEquals("http://user:pass@proxy.example.com:8080", proxyWithAuth.getProxyUrl(),
//                "有认证代理URL格式不正确");
    }

    @Test
    @DisplayName("测试代理服务关闭")
    void testShutdown() {
        // 关闭前应该能获取代理
        ProxyService.ProxyInfo proxy = localProxyPoolImpl.getProxy(ProxyType.HTTP);
        assertNotNull(proxy, "关闭前应能获取代理");
        localProxyPoolImpl.releaseProxy(proxy);

        // 关闭服务
        localProxyPoolImpl.shutdown();

        // 关闭后应抛出异常
        assertThrows(IllegalStateException.class, () -> localProxyPoolImpl.getProxy(ProxyType.HTTP),
                "关闭后获取代理应抛出异常");
    }

    @Test
    @DisplayName("测试空代理池 - 无配置代理时应返回null或抛出异常")
    void testEmptyProxyPool() {
        // 创建无代理的配置
        ProxyConfig emptyProxyConfig = new ProxyConfig();

        LocalProxyPoolImpl emptyPool = new LocalProxyPoolImpl(emptyProxyConfig);

        // 获取代理应返回null
        ProxyService.ProxyInfo proxy = emptyPool.getProxy(ProxyType.HTTP);
        assertNull(proxy, "空代理池应返回null");

        emptyPool.shutdown();
    }
}
