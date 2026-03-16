package uw.webot.stealth;

import uw.webot.core.BrowserTab;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * StealthManager.
 * 反检测服务管理器。
 *
 * @author axeon
 * @since 1.0.0
 */
public class StealthManager {

    /**
     * 服务映射表。
     */
    private final LinkedHashMap<String, StealthService> serviceMap = new LinkedHashMap<>();

    /**
     * 默认服务。
     */
    private static final StealthService DEFAULT_SERVICE = new StealthService(new StealthConfig());

    /**
     * 构造函数。
     *
     * @param serviceMap 配置映射表
     */
    public StealthManager(Map<String, StealthConfig> serviceMap) {
        if (serviceMap == null) {
            return;
        }
        for (Map.Entry<String, StealthConfig> entry : serviceMap.entrySet()) {
            this.serviceMap.put(entry.getKey(), new StealthService(entry.getValue()));
        }
    }

    /**
     * 获取默认的验证码服务。
     *
     * @return 验证码服务
     */
    public StealthService getDefaultStealthService() {
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
    public StealthService getStealthService(String configName) {
        if (configName == null) {
            return getDefaultStealthService();
        }
        return serviceMap.get(configName);
    }

    /**
     * 应用指定配置的验证码服务。
     *
     * @param browserTab 浏览器标签
     * @param stealthConfigKey 配置名称
     */
    public void apply(BrowserTab browserTab, String stealthConfigKey) {
        StealthService stealthService = getStealthService(stealthConfigKey);
        if (stealthService == null) {
            DEFAULT_SERVICE.applyStealth(browserTab);
        }else {
            stealthService.applyStealth(browserTab);
        }
    }
}
