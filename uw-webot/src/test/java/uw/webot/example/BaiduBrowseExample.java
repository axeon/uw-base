package uw.webot.example;

import com.microsoft.playwright.Page;
import uw.webot.WebotManager;
import uw.webot.WebotSession;
import uw.webot.captcha.CaptchaManager;
import uw.webot.conf.WebotProperties;
import uw.webot.core.BrowserBotPool;
import uw.webot.core.BrowserConfig;
import uw.webot.core.BrowserTab;
import uw.webot.core.BrowserType;
import uw.webot.proxy.ProxyManager;
import uw.webot.session.SessionConfig;
import uw.webot.session.SessionService;
import uw.webot.session.impl.GlobalSessionServiceImpl;
import uw.webot.stealth.StealthManager;

import java.nio.file.Paths;
import java.util.Arrays;

/**
 * 百度浏览示例程序。
 * <p>
 * 演示如何使用 WebotManager 进行有头模式浏览器操作。
 * 使用 try-with-resources 自动管理 BrowserTab 资源生命周期。
 * </p>
 *
 * <p>使用示例（推荐方式）：</p>
 * <pre>
 * // 创建 WebotManager
 * WebotManager webotManager = createWebotManager();
 *
 * // 创建会话
 * String sessionId = webotManager.createSession(sessionConfig);
 *
 * // 使用 try-with-resources 自动释放资源
 * try (BrowserTab page = webotManager.acquireContext(sessionId)) {
 *     page.navigate("https://www.baidu.com");
 *     String title = page.title();
 *     // 资源会自动归还到 BrowserBotPool，无需手动调用 releaseContext()
 * }
 *
 * // 销毁会话
 * webotManager.destroySession(sessionId);
 * </pre>
 *
 * <p>运行前请确保：</p>
 * <ol>
 *   <li>已安装 Chrome 浏览器</li>
 *   <li>如果是 macOS，Chrome 安装在 /Applications/Google Chrome.app</li>
 *   <li>如果是 Linux，Chrome 安装在 /usr/bin/google-chrome 或 /usr/bin/chromium-browser</li>
 * </ol>
 *
 * @author axeon
 * @see WebotManager
 * @see BrowserTab
 * @see BrowserBotPool
 * @since 1.0.0
 */
public class BaiduBrowseExample {

    /**
     * 程序入口。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        System.out.println("=== Webot 百度浏览示例 ===");
        System.out.println("说明：此示例使用 try-with-resources 自动管理资源");
        System.out.println();

        // 设置 Clash Verge 代理
        System.setProperty("HTTPS_PROXY", "127.0.0.1:7890");
        System.out.println("✓ 已设置代理: 127.0.0.1:7890");
        System.out.println();

        // 1. 创建 WebotManager
        WebotManager webotManager = createWebotManager();
        System.out.println("✓ WebotManager 创建成功");

        // 2. 创建会话配置（有头模式）
        String chromePath = getChromePath();
        System.out.println("Chrome 路径: " + chromePath);

        BrowserConfig browserConfig = BrowserConfig.builder().browserType(BrowserType.CHROMIUM).headless(false)  // 有头模式，可以看到浏览器窗口
                .viewportSize(1920, 1080).javaScriptEnabled(true)
                // 只保留必要的参数
                .args(Arrays.asList("--disable-blink-features=AutomationControlled")).build();

        System.out.println("浏览器配置:");
        System.out.println("  - headless: " + browserConfig.isHeadless());
        System.out.println("  - executablePath: " + browserConfig.getExecutablePath());
        System.out.println();

        SessionConfig sessionConfig = SessionConfig.builder().browserConfig(browserConfig).build();

        // 3. 创建会话
        WebotSession session = webotManager.createSession(sessionConfig);
        System.out.println("✓ 会话创建成功: " + session.getSessionId());
        System.out.println();

        try {
            // 4. 使用 try-with-resources 自动管理 BrowserTab 资源
            // BrowserTab 实现了 Closeable 接口，退出 try 块时会自动调用 close()
            // close() 方法会将 BrowserTab 归还到 BrowserBotPool 中以便复用
            try (BrowserTab browserTab = webotManager.openBrowserTab(session)) {
                System.out.println("✓ 浏览器启动成功，上下文ID: " + browserTab.getBrowserTabId());
                System.out.println();

                // 访问百度
                System.out.println("正在访问百度 (https://www.baidu.com)...");
                browserTab.navigate("https://www.baidu.com");

                System.out.println("等待页面加载...");
                Thread.sleep(5000);

                // 获取页面信息
                String title = browserTab.title();
                String url = browserTab.url();

                System.out.println("✓ 页面加载完成");
                System.out.println("  标题: " + title);
                System.out.println("  URL: " + url);
                System.out.println();

                // 截图
                System.out.println("正在截图...");
                byte[] screenshot = browserTab.screenshot(new Page.ScreenshotOptions().setFullPage(true));
                System.out.println("✓ 截图已获取，大小: " + screenshot.length + " bytes");

                // 保存截图到文件
                try {
                    String screenshotPath = "baidu_result.png";
                    java.nio.file.Files.write(java.nio.file.Paths.get(screenshotPath), screenshot);
                    System.out.println("✓ 截图已保存到: " + screenshotPath);
                } catch (Exception e) {
                    System.err.println("✗ 保存截图失败: " + e.getMessage());
                }

                System.out.println();
                System.out.println("=== 浏览完成！===");
                System.out.println("浏览器窗口将在 3 秒后关闭...");
                Thread.sleep(3000);

            }
            // try-with-resources 会自动调用 page.close() 将 BrowserTab 归还到池中

        } catch (Exception e) {
            System.err.println();
            System.err.println("✗ 浏览过程中出错: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 5. 销毁会话
            webotManager.destroySession(session.getSessionId());
            System.out.println("✓ 会话已销毁");
        }

        System.out.println();
        System.out.println("=== 示例程序结束 ===");
    }

    /**
     * 创建 WebotManager 实例。
     * <p>
     * 在非 Spring 环境下手动创建 WebotManager 所需的依赖。
     * </p>
     *
     * @return WebotManager 实例
     */
    private static WebotManager createWebotManager() {
        // 创建配置
        WebotProperties properties = createProperties();

        // 初始化 BrowserBotPool
        BrowserBotPool browserBotPool = new BrowserBotPool(properties.getBotPool());

        // 创建 SessionManager（使用本地实现）
        SessionService sessionService = new GlobalSessionServiceImpl(properties.getSession());

        CaptchaManager captchaManager = new CaptchaManager(properties.getCaptcha());
        StealthManager stealthManager = new StealthManager(properties.getStealth());
        ProxyManager proxyManager = new ProxyManager(properties.getProxy());

        // 创建 WebotManager
        // 在非 Spring 环境下，可选服务传入 null
        return new WebotManager(properties, browserBotPool, sessionService, captchaManager, stealthManager, proxyManager);
    }

    /**
     * 获取 Chrome 路径。
     * <p>
     * 依次检查以下路径：
     * <ol>
     *   <li>Playwright 安装的 Chrome for Testing</li>
     *   <li>系统安装的 Google Chrome</li>
     *   <li>系统安装的 Chromium</li>
     * </ol>
     * </p>
     *
     * @return Chrome 可执行文件路径
     */
    private static String getChromePath() {
        String home = System.getProperty("user.home");
        String[] possiblePaths = {home + "/Library/Caches/ms-playwright/chromium-1208/chrome-mac-arm64/Google Chrome for Testing.app/Contents/MacOS/Google Chrome for Testing", home + "/Library/Caches/ms-playwright/chromium-1208/chrome-mac/Google Chrome for Testing.app/Contents/MacOS/Google Chrome for Testing", "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome", "/Applications/Chromium.app/Contents/MacOS/Chromium"};

        for (String path : possiblePaths) {
            if (Paths.get(path).toFile().exists()) {
                return path;
            }
        }

        return "chromium";
    }

    /**
     * 创建 WebotProperties 配置。
     *
     * @return WebotProperties 配置对象
     */
    private static WebotProperties createProperties() {
        WebotProperties properties = new WebotProperties();
        properties.setEnabled(true);

        // 配置 BotPool
        WebotProperties.BotPoolProperties botPool = properties.getBotPool();
        botPool.setMaxBrowsersPerGroup(2);
        botPool.setMaxTabsPerBrowser(5);

        // 配置 Session
        WebotProperties.SessionProperties session = properties.getSession();
        session.setDistributed(false);  // 本地模式

        return properties;
    }
}
