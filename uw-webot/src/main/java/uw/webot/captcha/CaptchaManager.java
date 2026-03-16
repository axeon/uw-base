package uw.webot.captcha;

import uw.webot.captcha.impl.CapsolverServiceImpl;
import uw.webot.captcha.impl.LocalOcrCaptchaServiceImpl;
import uw.webot.captcha.impl.TwoCaptchaServiceImpl;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 验证码服务管理器。
 *
 * @author axeon
 * @since 1.0.0
 */
public class CaptchaManager {

    /**
     * 服务映射表。
     */
    private final LinkedHashMap<String, CaptchaService> serviceMap = new LinkedHashMap<>();


    /**
     * 构造函数。
     *
     * @param configMap 配置映射表
     */
    public CaptchaManager(Map<String, CaptchaConfig> configMap) {
        /**
         * 配置映射表。
         */
        for (Map.Entry<String, CaptchaConfig> entry : configMap.entrySet()) {
            CaptchaService service = null;
            switch (entry.getValue().getServiceType()) {
                case OCR:
                    service = new LocalOcrCaptchaServiceImpl(entry.getValue());
                    break;
                case TWOCAPTCHA:
                    service = new TwoCaptchaServiceImpl(entry.getValue());
                    break;
                case CAPSOLVER:
                    service = new CapsolverServiceImpl(entry.getValue());
                    break;
                default:
                    break;
            }
            if (service != null) {
                serviceMap.put(entry.getKey(), service);
            }
        }

    }

    /**
     * 获取默认的验证码服务。
     *
     * @return 验证码服务
     */
    public CaptchaService getDefaultCaptchaService() {
        return serviceMap.firstEntry().getValue();
    }

    /**
     * 获取指定配置的验证码服务。
     *
     * @param configName 配置名称
     * @return 验证码服务
     */
    public CaptchaService getCaptchaService(String configName) {
        if (configName == null) {
            return getDefaultCaptchaService();
        }
        return serviceMap.get(configName);
    }


}
