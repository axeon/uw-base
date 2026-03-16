package uw.webot.stealth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.webot.core.BrowserTab;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 反检测服务。
 * 实现WebDriver属性隐藏、WebGL指纹伪装、浏览器指纹随机化等反检测功能。
 *
 * @author axeon
 * @since 1.0.0
 */
public class StealthService {

    private static final Logger log = LoggerFactory.getLogger(StealthService.class);

    /**
     * 配置属性。
     */
    private final StealthConfig config;

    /**
     * User-Agent列表。
     */
    private final List<String> userAgents;

    /**
     * 屏幕分辨率列表。
     */
    private final List<String> screenResolutions;

    /**
     * 时区列表。
     */
    private final List<String> timezones;

    /**
     * WebGL指纹列表。
     */
    private final List<WebGLFingerprint> webGLFingerprints;

    public StealthService(StealthConfig config) {
        this.config = config;
        this.userAgents = initUserAgents();
        this.screenResolutions = initScreenResolutions();
        this.timezones = initTimezones();
        this.webGLFingerprints = initWebGLFingerprints();
    }

    /**
     * 应用反检测措施。
     *
     * @param browserTab 浏览器实例
     */
    public void applyStealth(BrowserTab browserTab) {

        log.debug("Applying stealth measures to browserTab [{}]", browserTab.getBrowserTabId());

        try {
            // 隐藏WebDriver属性
            if (config.isHideWebDriver()) {
                hideWebDriver(browserTab);
            }

            // 伪装WebGL指纹
            if (config.isWebglSpoofing()) {
                spoofWebGL(browserTab);
            }

            // 随机化浏览器指纹
            if (config.isFingerprintRandomization()) {
                randomizeFingerprint(browserTab);
            }

            // 伪装插件
            if (config.isPluginSpoofing()) {
                spoofPlugins(browserTab);
            }

            // 伪装字体
            if (config.isFontSpoofing()) {
                spoofFonts(browserTab);
            }

            log.debug("Stealth measures applied to browserTab [{}]", browserTab.getBrowserTabId());

        } catch (Exception e) {
            log.error("Failed to apply stealth measures", e);
        }
    }

    /**
     * 隐藏WebDriver属性。
     */
    private void hideWebDriver(BrowserTab instance) {
        String script = """
                () => {
                    // 删除webdriver属性
                    delete Object.getPrototypeOf(navigator).webdriver;
                
                    // 覆盖navigator.languages
                    Object.defineProperty(navigator, 'languages', {
                        get: function() {
                            return ['zh-CN', 'zh', 'en'];
                        }
                    });
                
                    // 覆盖chrome对象
                    window.chrome = {
                        runtime: {},
                        loadTimes: function() {},
                        csi: function() {},
                        app: {}
                    };
                
                    // 覆盖permissions.query
                    const originalQuery = window.navigator.permissions.query;
                    window.navigator.permissions.query = (parameters) => (
                        parameters.name === 'notifications' ?
                            Promise.resolve({ state: Notification.permission }) :
                            originalQuery(parameters)
                    );
                
                    // 覆盖Notification.permission
                    Object.defineProperty(Notification, 'permission', {
                        get: function() {
                            return 'default';
                        }
                    });
                }
                """;

        instance.evaluate(script);
    }

    /**
     * 伪装WebGL指纹。
     */
    private void spoofWebGL(BrowserTab instance) {
        WebGLFingerprint fingerprint = getRandomWebGLFingerprint();

        String script = String.format("""
                        () => {
                            const getParameter = WebGLRenderingContext.prototype.getParameter;
                            WebGLRenderingContext.prototype.getParameter = function(parameter) {
                                if (parameter === 37445) {
                                    return '%s';
                                }
                                if (parameter === 37446) {
                                    return '%s';
                                }
                                if (parameter === 7937) {
                                    return '%s';
                                }
                                if (parameter === 7936) {
                                    return '%s';
                                }
                                return getParameter(parameter);
                            };
                        
                            // 覆盖getShaderPrecisionFormat
                            const getShaderPrecisionFormat = WebGLRenderingContext.prototype.getShaderPrecisionFormat;
                            WebGLRenderingContext.prototype.getShaderPrecisionFormat = function() {
                                return {
                                    precision: 23,
                                    rangeMin: 127,
                                    rangeMax: 127
                                };
                            };
                        }
                        """,
                fingerprint.vendor,
                fingerprint.renderer,
                fingerprint.unmaskedVendor,
                fingerprint.unmaskedRenderer
        );

        instance.evaluate(script);
    }

    /**
     * 随机化浏览器指纹。
     */
    private void randomizeFingerprint(BrowserTab instance) {
        String userAgent = getRandomUserAgent();
        String resolution = getRandomScreenResolution();
        String timezone = getRandomTimezone();

        String[] resParts = resolution.split("x");
        int width = Integer.parseInt(resParts[0]);
        int height = Integer.parseInt(resParts[1]);

        String script = String.format("""
                        () => {
                            // 设置User-Agent
                            Object.defineProperty(navigator, 'userAgent', {
                                get: function() {
                                    return '%s';
                                }
                            });
                        
                            // 设置屏幕分辨率
                            Object.defineProperty(screen, 'width', {
                                get: function() {
                                    return %d;
                                }
                            });
                            Object.defineProperty(screen, 'height', {
                                get: function() {
                                    return %d;
                                }
                            });
                            Object.defineProperty(screen, 'availWidth', {
                                get: function() {
                                    return %d;
                                }
                            });
                            Object.defineProperty(screen, 'availHeight', {
                                get: function() {
                                    return %d - 40;
                                }
                            });
                            Object.defineProperty(screen, 'colorDepth', {
                                get: function() {
                                    return 24;
                                }
                            });
                            Object.defineProperty(screen, 'pixelDepth', {
                                get: function() {
                                    return 24;
                                }
                            });
                        
                            // 设置时区
                            Intl.DateTimeFormat = function() {
                                return {
                                    resolvedOptions: function() {
                                        return {
                                            timeZone: '%s',
                                            locale: 'zh-CN'
                                        };
                                    }
                                };
                            };
                        
                            // 覆盖Date
                            const OriginalDate = Date;
                            Date = function() {
                                if (arguments.length === 0) {
                                    return new OriginalDate();
                                }
                                return new OriginalDate(...arguments);
                            };
                            Date.prototype = OriginalDate.prototype;
                            Date.now = OriginalDate.now;
                            Date.parse = OriginalDate.parse;
                            Date.UTC = OriginalDate.UTC;
                        }
                        """,
                userAgent, width, height, width, height, timezone
        );

        instance.evaluate(script);
    }

    /**
     * 伪装插件。
     */
    private void spoofPlugins(BrowserTab instance) {
        String script = """
                () => {
                    // 创建假的插件列表
                    const plugins = [
                        {
                            name: "Chrome PDF Plugin",
                            filename: "internal-pdf-viewer",
                            description: "Portable Document Format",
                            version: "undefined",
                            length: 1,
                            item: function() { return this[0]; },
                            namedItem: function() { return this[0]; }
                        },
                        {
                            name: "Chrome PDF Viewer",
                            filename: "mhjfbmdgcfjbbpaeojofohoefgiehjai",
                            description: "",
                            version: "undefined",
                            length: 1,
                            item: function() { return this[0]; },
                            namedItem: function() { return this[0]; }
                        },
                        {
                            name: "Native Client",
                            filename: "internal-nacl-plugin",
                            description: "",
                            version: "undefined",
                            length: 2,
                            item: function() { return this[0]; },
                            namedItem: function() { return this[0]; }
                        }
                    ];
                
                    Object.defineProperty(navigator, 'plugins', {
                        get: function() {
                            return plugins;
                        }
                    });
                
                    Object.defineProperty(navigator, 'mimeTypes', {
                        get: function() {
                            return [
                                {type: "application/pdf", suffixes: "pdf", description: "Portable Document Format", enabledPlugin: plugins[1]},
                                {type: "application/x-google-chrome-pdf", suffixes: "pdf", description: "Portable Document Format", enabledPlugin: plugins[0]},
                                {type: "application/x-nacl", suffixes: "", description: "", enabledPlugin: plugins[2]},
                                {type: "application/x-pnacl", suffixes: "", description: "", enabledPlugin: plugins[2]}
                            ];
                        }
                    });
                }
                """;

        instance.evaluate(script);
    }

    /**
     * 伪装字体。
     */
    private void spoofFonts(BrowserTab instance) {
        String script = """
                () => {
                    // 覆盖字体检测
                    const canvas = document.createElement('canvas');
                    const ctx = canvas.getContext('2d');
                
                    const originalMeasureText = ctx.measureText;
                    ctx.measureText = function(text) {
                        const result = originalMeasureText.call(this, text);
                        // 添加微小的随机变化
                        result.width = result.width + (Math.random() * 0.02 - 0.01);
                        return result;
                    };
                
                    // 覆盖getImageData
                    const originalGetImageData = ctx.getImageData;
                    ctx.getImageData = function(x, y, w, h) {
                        const imageData = originalGetImageData.call(this, x, y, w, h);
                        // 添加微小的噪声
                        for (let i = 0; i < imageData.data.length; i += 4) {
                            if (Math.random() < 0.01) {
                                imageData.data[i] = Math.min(255, Math.max(0, imageData.data[i] + (Math.random() * 2 - 1)));
                            }
                        }
                        return imageData;
                    };
                }
                """;

        instance.evaluate(script);
    }

    /**
     * 获取随机User-Agent。
     */
    private String getRandomUserAgent() {
        if (userAgents.isEmpty()) {
            return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
        }
        return userAgents.get(ThreadLocalRandom.current().nextInt(userAgents.size()));
    }

    /**
     * 获取随机屏幕分辨率。
     */
    private String getRandomScreenResolution() {
        if (screenResolutions.isEmpty()) {
            return "1920x1080";
        }
        return screenResolutions.get(ThreadLocalRandom.current().nextInt(screenResolutions.size()));
    }

    /**
     * 获取随机时区。
     */
    private String getRandomTimezone() {
        if (timezones.isEmpty()) {
            return null;
        }
        return timezones.get(ThreadLocalRandom.current().nextInt(timezones.size()));
    }

    /**
     * 获取随机WebGL指纹。
     */
    private WebGLFingerprint getRandomWebGLFingerprint() {
        if (webGLFingerprints.isEmpty()) {
            return new WebGLFingerprint(
                    "Google Inc. (NVIDIA)",
                    "ANGLE (NVIDIA, NVIDIA GeForce GTX 1050 Ti Direct3D11 vs_5_0 ps_5_0, D3D11)",
                    "Google Inc. (NVIDIA)",
                    "ANGLE (NVIDIA, NVIDIA GeForce GTX 1050 Ti Direct3D11 vs_5_0 ps_5_0, D3D11)"
            );
        }
        return webGLFingerprints.get(ThreadLocalRandom.current().nextInt(webGLFingerprints.size()));
    }

    /**
     * 初始化User-Agent列表。
     */
    private List<String> initUserAgents() {
        List<String> customUAs = config.getUserAgents();
        if (!customUAs.isEmpty()) {
            return new ArrayList<>(customUAs);
        }

        return Arrays.asList(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/121.0",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:109.0) Gecko/20100101 Firefox/121.0",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0"
        );
    }

    /**
     * 初始化屏幕分辨率列表。
     */
    private List<String> initScreenResolutions() {
        List<String> customResolutions = config.getScreenResolutions();
        if (!customResolutions.isEmpty()) {
            return new ArrayList<>(customResolutions);
        }

        return Arrays.asList(
                "1920x1080",
                "1366x768",
                "1440x900",
                "1536x864",
                "2560x1440",
                "1680x1050",
                "1280x720",
                "1600x900"
        );
    }

    /**
     * 初始化时区列表。
     */
    private List<String> initTimezones() {
        List<String> customTimezones = config.getTimezones();
        if (!customTimezones.isEmpty()) {
            return new ArrayList<>(customTimezones);
        }

        return Arrays.asList(
                "Asia/Shanghai",
                "Asia/Hong_Kong",
                "Asia/Tokyo",
                "Asia/Seoul",
                "Asia/Singapore",
                "America/New_York",
                "America/Los_Angeles",
                "Europe/London",
                "Europe/Paris",
                "Australia/Sydney"
        );
    }

    /**
     * 初始化WebGL指纹列表。
     */
    private List<WebGLFingerprint> initWebGLFingerprints() {
        return Arrays.asList(
                new WebGLFingerprint(
                        "Google Inc. (NVIDIA)",
                        "ANGLE (NVIDIA, NVIDIA GeForce GTX 1050 Ti Direct3D11 vs_5_0 ps_5_0, D3D11)",
                        "Google Inc. (NVIDIA)",
                        "ANGLE (NVIDIA, NVIDIA GeForce GTX 1050 Ti Direct3D11 vs_5_0 ps_5_0, D3D11)"
                ),
                new WebGLFingerprint(
                        "Google Inc. (NVIDIA)",
                        "ANGLE (NVIDIA, NVIDIA GeForce GTX 1060 Direct3D11 vs_5_0 ps_5_0, D3D11)",
                        "Google Inc. (NVIDIA)",
                        "ANGLE (NVIDIA, NVIDIA GeForce GTX 1060 Direct3D11 vs_5_0 ps_5_0, D3D11)"
                ),
                new WebGLFingerprint(
                        "Google Inc. (AMD)",
                        "ANGLE (AMD, AMD Radeon RX 580 Direct3D11 vs_5_0 ps_5_0, D3D11)",
                        "Google Inc. (AMD)",
                        "ANGLE (AMD, AMD Radeon RX 580 Direct3D11 vs_5_0 ps_5_0, D3D11)"
                ),
                new WebGLFingerprint(
                        "Google Inc. (Intel)",
                        "ANGLE (Intel, Intel(R) UHD Graphics 630 Direct3D11 vs_5_0 ps_5_0, D3D11)",
                        "Google Inc. (Intel)",
                        "ANGLE (Intel, Intel(R) UHD Graphics 630 Direct3D11 vs_5_0 ps_5_0, D3D11)"
                )
        );
    }

    /**
     * WebGL指纹。
     */
    private record WebGLFingerprint(
            String vendor,
            String renderer,
            String unmaskedVendor,
            String unmaskedRenderer
    ) {
    }

    /**
     * 测试反检测效果。
     *
     * @param instance 浏览器实例
     * @return 测试结果
     */
    public StealthTestResult testStealth(BrowserTab instance) {
        try {
            String script = """
                    () => {
                        const results = {
                            webdriver: typeof navigator.webdriver,
                            plugins: navigator.plugins ? navigator.plugins.length : 0,
                            languages: navigator.languages,
                            userAgent: navigator.userAgent,
                            screen: {
                                width: screen.width,
                                height: screen.height,
                                colorDepth: screen.colorDepth
                            },
                            webgl: null
                        };
                    
                        try {
                            const canvas = document.createElement('canvas');
                            const gl = canvas.getContext('webgl');
                            if (gl) {
                                results.webgl = {
                                    vendor: gl.getParameter(gl.VENDOR),
                                    renderer: gl.getParameter(gl.RENDERER),
                                    unmaskedVendor: gl.getParameter(37445),
                                    unmaskedRenderer: gl.getParameter(37446)
                                };
                            }
                        } catch (e) {
                            results.webgl = { error: e.message };
                        }
                    
                        return results;
                    }
                    """;

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) instance.evaluate(script);

            boolean webdriverHidden = "undefined".equals(result.get("webdriver"));
            boolean hasPlugins = (Integer) result.get("plugins") > 0;

            return new StealthTestResult(
                    webdriverHidden && hasPlugins,
                    result,
                    webdriverHidden ? null : "navigator.webdriver is still present"
            );

        } catch (Exception e) {
            log.error("Stealth test failed", e);
            return new StealthTestResult(false, null, e.getMessage());
        }
    }

    /**
     * 反检测测试结果。
     */
    public record StealthTestResult(
            boolean passed,
            Map<String, Object> details,
            String errorMessage
    ) {
    }
}
