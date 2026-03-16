package uw.webot.example;

import com.microsoft.playwright.*;

import java.nio.file.Paths;

/**
 * 简单的 Playwright 测试，直接测试浏览器窗口是否能显示。
 */
public class PlaywrightSimpleTest {

    public static void main(String[] args) {
        System.out.println("=== Playwright 简单测试 ===");
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println();

        // 获取 Chrome 路径
        String chromePath = getChromePath();
        System.out.println("Chrome 路径: " + chromePath);

        Playwright playwright = null;
        Browser browser = null;
        BrowserContext context = null;
        Page page = null;

        try {
            System.out.println("\n1. 创建 Playwright...");
            playwright = Playwright.create();
            System.out.println("   ✓ Playwright 创建成功");

            System.out.println("\n2. 配置浏览器启动选项...");
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(false)  // 关键：设置为 false 以显示窗口
                    .setExecutablePath(Paths.get(chromePath));

            System.out.println("   - headless: " + launchOptions.headless);
            System.out.println("   - executablePath: " + launchOptions.executablePath);

            System.out.println("\n3. 启动浏览器（应该能看到窗口）...");
            System.out.println("   注意：此时应该弹出 Chrome 窗口！");

            browser = playwright.chromium().launch(launchOptions);
            System.out.println("   ✓ 浏览器启动成功");

            System.out.println("\n4. 创建浏览器上下文...");
            context = browser.newContext(new Browser.NewContextOptions()
                    .setViewportSize(1920, 1080));
            System.out.println("   ✓ 上下文创建成功");

            System.out.println("\n5. 创建页面...");
            page = context.newPage();
            System.out.println("   ✓ 页面创建成功");

            System.out.println("\n6. 访问百度...");
            page.navigate("https://www.baidu.com");
            System.out.println("   ✓ 页面加载完成");
            System.out.println("   - 标题: " + page.title());

            System.out.println("\n7. 截图...");
            byte[] screenshot = page.screenshot();
            System.out.println("   ✓ 截图完成，大小: " + screenshot.length + " bytes");

            // 保存截图
            java.nio.file.Files.write(Paths.get("playwright_test.png"), screenshot);
            System.out.println("   ✓ 截图已保存到 playwright_test.png");

            System.out.println("\n8. 等待 5 秒...");
            System.out.println("   此时浏览器窗口应该保持打开状态！");
            Thread.sleep(5000);

            System.out.println("\n=== 测试完成 ===");

        } catch (Exception e) {
            System.err.println("\n✗ 错误: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("\n9. 关闭资源...");
            if (page != null) page.close();
            if (context != null) context.close();
            if (browser != null) browser.close();
            if (playwright != null) playwright.close();
            System.out.println("   ✓ 资源已关闭");
        }
    }

    private static String getChromePath() {
        String home = System.getProperty("user.home");
        String[] possiblePaths = {
                home + "/Library/Caches/ms-playwright/chromium-1208/chrome-mac-arm64/Google Chrome for Testing.app/Contents/MacOS/Google Chrome for Testing",
                "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
                "/Applications/Chromium.app/Contents/MacOS/Chromium"
        };

        for (String path : possiblePaths) {
            if (Paths.get(path).toFile().exists()) {
                return path;
            }
        }

        return "chromium";
    }
}
