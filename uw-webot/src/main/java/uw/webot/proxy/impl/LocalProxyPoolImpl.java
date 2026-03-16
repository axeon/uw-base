package uw.webot.proxy.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.webot.proxy.ProxyConfig;
import uw.webot.proxy.ProxyService;
import uw.webot.proxy.ProxyType;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 代理池实现。
 * 实现代理池管理机制，支持代理健康检查和自动切换。
 */
public class LocalProxyPoolImpl implements ProxyService {

    private static final Logger log = LoggerFactory.getLogger(LocalProxyPoolImpl.class);

    /**
     * 配置属性。
     */
    private final ProxyConfig proxyConfig;

    /**
     * 所有代理。
     * 使用 BlockingQueue 保证线程安全，同时支持阻塞获取。
     */
    private final BlockingQueue<ProxyInfo> allProxies;

    /**
     * 健康检查间隔（毫秒）。
     */
    private static final long HEALTH_CHECK_INTERVAL_MS = 60_000; // 60 秒

    /**
     * 是否已关闭。
     */
    private volatile boolean shutdown = false;

    public LocalProxyPoolImpl(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
        // 初始化代理
        List<ProxyConfig.ProxyServer> servers = proxyConfig.getServers();
        this.allProxies = new LinkedBlockingQueue<>(servers.size());
        for (ProxyConfig.ProxyServer server : servers) {
            ProxyInfo proxyInfo = new ProxyInfo(
                    server,
                    1 // 默认权重
            );
            allProxies.add(proxyInfo);
        }
        // 如果没有配置代理，记录警告
        if (allProxies.isEmpty()) {
            log.warn("No proxies configured in ProxyPool");
        } else {
            log.debug("ProxyPool initialized with {} proxies", allProxies.size());
        }
    }

    /**
     * 查找可用代理。
     */
    @Override
    public ProxyInfo getProxy(ProxyType proxyType) {
        // 临时存储不匹配的代理，稍后放回
        List<ProxyInfo> nonMatchingProxies = new ArrayList<>();

        ProxyInfo foundProxy = null;

        // 从队列中取出代理查找匹配的类型
        while (!allProxies.isEmpty()) {
            ProxyInfo proxy = allProxies.poll();
            if (proxy != null) {
                // 匹配类型
                if (checkProxy(proxyType, proxy)) {
                    foundProxy = proxy;
                    break;
                } else {
                    nonMatchingProxies.add(proxy);
                }
            }
        }

        // 将不匹配的代理放回队列
        if (!nonMatchingProxies.isEmpty()) {
            allProxies.addAll(nonMatchingProxies);
        }

        return foundProxy;
    }

    /**
     * 检查代理状态。
     */
    private boolean checkProxy(ProxyType proxyType, ProxyInfo proxy) {
        boolean result = false;
        if (proxy.getType() == proxyType || proxyType == ProxyType.ANY) {
            // 如果在指定时间内健康，则直接返回。
            if (proxy.getLastHealthCheckResult() && proxy.getLastHealthCheckTime() + proxyConfig.getHealthCheckInterval().toMillis() < System.currentTimeMillis()) {
                result = true;
            }
            // 如果超出检测时间，则需要重新检测。
            if (proxy.getLastHealthCheckTime() + proxyConfig.getHealthCheckInterval().toMillis() > System.currentTimeMillis()) {
                if (proxy.getFailureCount() < proxyConfig.getMaxFailures()) {
                    if (checkProxyHealth(proxy)) {
                        result = true;
                    }
                }
            }
        }
        if (result) {
            proxy.setLastUsedTime(System.currentTimeMillis());
        }
        return result;
    }


    /**
     * 释放代理。
     */
    @Override
    public void releaseProxy(ProxyInfo proxyInfo) {
        // 代理不需要释放，直接使用循环队列机制
        // 此方法保留以兼容接口，但不再执行任何操作
    }


    /**
     * 标记代理失败。
     */
    @Override
    public void markProxyFailed(ProxyInfo proxyInfo) {
        // 代理不需要释放，直接使用循环队列机制
        // 此方法保留以兼容接口，但不再执行任何操作
    }

    @Override
    public boolean checkProxyHealth(ProxyInfo proxyInfo) {
        if (proxyInfo == null) {
            return false;
        }

        try {
            // 创建 HttpClient
            HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .proxy(ProxySelector.of(new InetSocketAddress(proxyInfo.getProxyServer().getHost(), proxyInfo.getProxyServer().getPort())));

            try (HttpClient client = clientBuilder.build()) {

                // 发送健康检查请求
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://httpbin.org/ip"))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                boolean healthy = response.statusCode() == 200;

                // 更新健康检查状态
                proxyInfo.setHealthCheckResult(healthy);

                if (healthy) {
                    proxyInfo.setHealthCheckResult(true);
                    log.debug("Proxy [{}] health check passed", proxyInfo.toString());
                } else {
                    proxyInfo.setHealthCheckResult(false);
                    log.warn("Proxy [{}] health check failed with status: {}",
                            proxyInfo.toString(), response.statusCode());
                }
                return healthy;
            }
        } catch (Exception e) {
            proxyInfo.setHealthCheckResult(false);
            log.warn("Proxy [{}] health check failed: {}", proxyInfo.toString(), e.getMessage());
            return false;
        }
    }

    @Override
    public ProxyPoolStatistics getStatistics() {
        int healthyCount = 0;
        int unHealthyCount = 0;
        int httpCount = 0;
        int httpsCount = 0;
        int socksCount = 0;

        for (ProxyInfo proxy : allProxies) {
            if (proxy.getLastHealthCheckResult()) {
                healthyCount++;
            } else if (proxy.getLastHealthCheckTime() > 0) {
                unHealthyCount++;
            }
            switch (proxy.getType()) {
                case HTTP:
                    httpCount++;
                    break;
                case HTTPS:
                    httpsCount++;
                    break;
                case SOCKS5:
                    socksCount++;
                    break;
            }
        }

        return new ProxyPoolStatistics(
                allProxies.size(),
                healthyCount,
                unHealthyCount,
                httpCount,
                httpsCount,
                socksCount
        );
    }

    @Override
    public void shutdown() {
        if (shutdown) {
            return;
        }
        shutdown = true;
        // 清空代理队列
        allProxies.clear();
    }

    /**
     * 检查是否已关闭。
     */
    private void checkShutdown() {
        if (shutdown) {
            throw new IllegalStateException("ProxyPool is shutdown");
        }
    }
}
