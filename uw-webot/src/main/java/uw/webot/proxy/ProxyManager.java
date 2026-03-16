package uw.webot.proxy;

import uw.webot.proxy.impl.LocalProxyPoolImpl;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Proxy服务Manager.
 * 代理管理器。
 *
 * @author axeon
 * @since 1.0.0
 */
public class ProxyManager {

    /**
     * 服务映射表。
     */
    private final LinkedHashMap<String, ProxyService> serviceMap = new LinkedHashMap<>();


    /**
     * 构造函数。
     *
     * @param configMap 配置映射表
     */
    public ProxyManager(Map<String, ProxyConfig> configMap) {
        /**
         * 配置映射表。
         */
        for (Map.Entry<String, ProxyConfig> entry : configMap.entrySet()) {
            ProxyService service = new LocalProxyPoolImpl(entry.getValue());
            serviceMap.put(entry.getKey(), service);
        }
    }

    /**
     * 获取默认的验证码服务。
     *
     * @return 验证码服务
     */
    public ProxyService getDefaultProxyService() {
        if (serviceMap.isEmpty()) {
            return null;
        }
        return serviceMap.firstEntry().getValue();
    }

    /**
     * 获取指定配置的验证码服务。
     *
     * @param configName 配置名称
     * @return 验证码服务
     */
    public ProxyService getProxyService(String configName) {
        if (configName == null) {
            return getDefaultProxyService();
        }
        return serviceMap.get(configName);
    }


}
