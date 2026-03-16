# UW-Webot 自动化框架

UW-Webot 是一个基于 Microsoft Playwright 的高性能 Web 自动化框架，采用 Hybrid 混合模式设计，支持浏览器实例复用和页签级隔离，提供验证码识别、反检测、代理池等企业级功能。

## 目录

- [项目概述](#项目概述)
- [核心功能](#核心功能)
- [技术架构](#技术架构)
- [环境要求](#环境要求)
- [安装部署](#安装部署)
- [使用指南](#使用指南)
- [配置说明](#配置说明)
- [API 参考](#api-参考)
- [常见问题](#常见问题)

## 项目概述

UW-Webot 旨在解决传统 Web 自动化框架在资源利用率和并发性能方面的瓶颈。通过创新的 Hybrid 混合模式，实现了：

- **浏览器实例复用**：同一浏览器实例可被多个页签共享，大幅降低内存占用
- **页签级隔离**：每个页签拥有独立的 Cookie、Storage 和代理配置
- **智能扩缩容**：根据负载自动调整浏览器实例数量
- **企业级功能**：内置验证码识别、反检测、代理池等高级特性

### 适用场景

- 大规模网页数据采集
- 自动化测试与监控
- 电商价格监控与比价
- 社交媒体自动化运营
- SEO 排名监控

## 核心功能

### 1. Hybrid 混合模式

采用分层架构设计，实现资源高效利用：

```
BrowserBotPool (单例)
    ├── BrowserGroup (chromium) - 最多 5 个 Browser（可配置最大20个）
    │       ├── BrowserInstance 1 - 最多 20 个 Tab （可配置最大50个）
    │       ├── BrowserInstance 2 - 最多 20 个 Tab
    │       └── ...
    ├── BrowserGroup (firefox) - 最多 5 个 Browser
    └── BrowserGroup (webkit) - 最多 5 个 Browser
```

### 2. 验证码识别

支持多种验证码服务：
- **本地 OCR**：基于 Tesseract 的本地识别
- **2Captcha**：第三方验证码解决服务
- **Capsolver**：AI 驱动的验证码解决服务

### 3. 反检测功能

- WebDriver 属性隐藏
- WebGL 指纹伪装
- Canvas 指纹随机化
- 浏览器指纹混淆

### 4. 代理池管理

- 支持 HTTP/HTTPS/SOCKS4/SOCKS5 协议
- 代理健康检查与自动故障转移
- 按类型筛选代理
- 失败次数统计与自动剔除

### 5. 会话管理

- 本地会话（基于 Caffeine 缓存）
- 分布式会话（基于 Redis/FusionCache）
- 会话状态持久化
- 自动过期清理

## 技术架构

### 核心组件关系

```
┌─────────────────────────────────────────────────────────────┐
│                        WebotManager                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ CaptchaManager│  │ StealthManager│  │ ProxyManager │       │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘       │
└─────────┼────────────────┼────────────────┼─────────────────┘
          │                │                │
          ▼                ▼                ▼
┌─────────────────────────────────────────────────────────────┐
│                      BrowserBotPool                          │
│                    (单例模式管理)                             │
└─────────┬───────────────────────────────────────────────────┘
          │
    ┌─────┴─────┬─────────────┬─────────────┐
    ▼           ▼             ▼             ▼
┌────────┐ ┌────────┐   ┌────────┐   ┌────────┐
│Chromium│ │Firefox │   │WebKit  │   │ ...    │  BrowserGroup
│ Group  │ │ Group  │   │ Group  │   │        │
└───┬────┘ └───┬────┘   └───┬────┘   └────────┘
    │          │            │
    ▼          ▼            ▼
┌────────┐ ┌────────┐   ┌────────┐
│Browser │ │Browser │   │Browser │  BrowserInstance
│Instance│ │Instance│   │Instance│
└───┬────┘ └───┬────┘   └───┬────┘
    │          │            │
    ▼          ▼            ▼
┌────────┐ ┌────────┐   ┌────────┐
│Browser │ │Browser │   │Browser │  BrowserTab (页签级隔离)
│  Tab   │ │  Tab   │   │  Tab   │
└────────┘ └────────┘   └────────┘
```

### 资源管理策略

1. **Browser 级别复用**：同一 Browser 实例可承载多个 Tab，共享浏览器进程
2. **Tab 级别隔离**：每个 Tab 拥有独立的 Context（Cookie、Storage、代理）
3. **信号量控制**：使用 Semaphore 控制并发访问，防止资源耗尽
4. **超时机制**：获取资源时支持超时设置，避免无限等待
5. **健康检查**：定期检查 Browser 健康状态，自动剔除异常实例

## 环境要求

### 系统要求

| 组件 | 最低版本  | 推荐版本  |
|------|-------|-------|
| Java | 17    | 25    |
| Maven | 3.8   | 3.9+  |
| Spring Boot | 3.2.x | 3.3.x |

### 浏览器要求

| 浏览器 | 支持版本 |
|--------|---------|
| Chromium/Chrome | 120+ |
| Firefox | 120+ |
| WebKit/Safari | 17+ |

### 依赖组件

- Microsoft Playwright 1.58.0
- Spring Boot Starter Validation
- Caffeine Cache（本地缓存）
- FusionCache（分布式缓存，可选）

## 安装部署

### Maven 依赖

```xml
<dependency>
    <groupId>com.umtone</groupId>
    <artifactId>uw-webot</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 快速开始

1. **添加配置到 `application.yml`**

```yaml
uw:
  webot:
    enabled: true
    bot-pool:
      max-browsers-per-group: 5      # 每种浏览器类型最大实例数
      max-tabs-per-browser: 20       # 每个浏览器实例最大页签数
    session:
      distributed: false             # 是否启用分布式会话
      default-session:
        expire-time: P30D            # 会话默认过期时间
```

2. **使用 WebotManager**

```java
@Service
public class CrawlerService {
    
    @Autowired
    private WebotManager webotManager;
    
    public void crawl(String url) throws Exception {
        // 创建会话
        WebotSession session = webotManager.createSession(
            SessionConfig.builder()
                .browserConfig(BrowserConfig.builder()
                    .browserType(BrowserType.CHROMIUM)
                    .headless(true)
                    .build())
                .build()
        );
        
        // 获取浏览器页签
        try (BrowserTab tab = webotManager.openBrowserTab(session)) {
            tab.navigate(url);
            String content = tab.content();
            // 处理页面内容...
        }
        
        // 销毁会话
        webotManager.destroySession(session.getSessionId());
    }
}
```

## 使用指南

### 基础操作

#### 1. 创建会话

```java
// 基础配置
SessionConfig config = SessionConfig.builder()
    .browserConfig(BrowserConfig.builder()
        .browserType(BrowserType.CHROMIUM)
        .headless(false)
        .viewportSize(1920, 1080)
        .userAgent("Mozilla/5.0...")
        .build())
    .expireTime(Duration.ofHours(1))
    .build();

WebotSession session = webotManager.createSession(config);
```

#### 2. 获取浏览器页签

```java
// 方式 1：使用 try-with-resources（推荐）
try (BrowserTab tab = webotManager.openBrowserTab(session)) {
    tab.navigate("https://example.com");
    // 执行操作...
}

// 方式 2：使用 execute 方法自动管理资源
webotManager.execute(session, tab -> {
    tab.navigate("https://example.com");
    // 执行操作...
});
```

#### 3. 页面操作

```java
// 导航
tab.navigate("https://example.com");

// 获取页面内容
String content = tab.content();

// 重新加载页面
tab.reload();

// 执行 JavaScript
Object result = tab.evaluate("document.title");

// 截图
byte[] screenshot = tab.screenshot(new Page.ScreenshotOptions()
    .setPath(Paths.get("screenshot.png")));

// 等待页面加载
tab.waitForLoadState(LoadState.NETWORKIDLE);
```

### 高级功能

#### 使用代理

```java
// 配置代理
SessionConfig config = SessionConfig.builder()
    .browserConfig(BrowserConfig.builder()
        .browserType(BrowserType.CHROMIUM)
        .build())
    .proxyConfigKey("default")  // 引用 uw.webot.proxy 中的配置
    .build();
```

#### 启用反检测

```java
SessionConfig config = SessionConfig.builder()
    .browserConfig(BrowserConfig.builder()
        .browserType(BrowserType.CHROMIUM)
        .build())
    .stealthConfigKey("default")  // 引用 uw.webot.stealth 中的配置
    .build();
```

#### 验证码识别

```java
// 获取验证码服务
CaptchaService captchaService = captchaManager.getService("2captcha");

// 识别图片验证码
CaptchaResult result = captchaService.recognizeImageCaptcha(imageBytes);
if (result.isSuccess()) {
    String code = result.getCode();
    tab.evaluate("document.querySelector('#captcha').value = '" + code + "'");
}
```

#### 存储状态管理

```java
// 获取存储状态JSON
String storageState = tab.getStorageStateJson();

// 保存到文件
Files.writeString(Path.of("storage-state.json"), storageState);

// 从文件恢复状态
String loadedState = Files.readString(Path.of("storage-state.json"));
// 注意：需要创建新的会话来应用存储状态
```

## 配置说明

### 完整配置示例

```yaml
uw:
  webot:
    enabled: true
    
    # 浏览器池配置
    bot-pool:
      max-browsers-per-group: 5      # 每种浏览器类型最大实例数 (1-20)
      max-tabs-per-browser: 20       # 每个浏览器实例最大页签数 (1-50)
    
    # 验证码配置
    captcha:
      default:
        enabled: true
        service-type: 2captcha       # 可选: ocr, 2captcha, capsolver
        api-key: "your-api-key"
        timeout: PT2M
        max-retries: 3
    
    # 代理配置
    proxy:
      default:
        enabled: true
        type: http                    # http, https, socks4, socks5
        host: proxy.example.com
        port: 8080
        username: user
        password: pass
        pool-enabled: true
        pool-max-size: 10
        max-failures: 3
      
      # 多个代理配置
      backup:
        enabled: true
        type: socks5
        host: backup-proxy.example.com
        port: 1080
    
    # 反检测配置
    stealth:
      default:
        enabled: true
        hide-web-driver: true
        webgl-spoofing: true
        canvas-spoofing: true
        timezone-spoofing: true
        locale-spoofing: true
    
    # 会话配置
    session:
      distributed: false              # 是否启用分布式会话
      default-session:
        expire-time: P30D             # ISO-8601 持续时间格式
        browser-config:
          browser-type: chromium
          headless: true
          viewport-width: 1920
          viewport-height: 1080
```

## API 参考

### WebotManager

| 方法 | 描述 | 参数 | 返回值 |
|------|------|------|--------|
| `createSession()` | 创建默认会话 | 无 | WebotSession |
| `createSession(SessionConfig)` | 创建会话 | 会话配置 | WebotSession |
| `openBrowserTab(WebotSession)` | 获取浏览器页签 | 会话 | BrowserTab |
| `execute(WebotSession, WebotFunction)` | 执行操作并自动管理资源 | 会话, 函数 | T |
| `execute(WebotSession, WebotConsumer)` | 执行操作（无返回值） | 会话, 消费者 | void |
| `getSession(String)` | 获取会话 | 会话ID | WebotSession |
| `updateSession(WebotSession)` | 更新会话 | 会话 | void |
| `destroySession(String)` | 销毁会话 | 会话ID | void |
| `getCaptchaManager()` | 获取验证码管理器 | 无 | CaptchaManager |
| `getProxyManager()` | 获取代理管理器 | 无 | ProxyManager |
| `getStealthManager()` | 获取反检测管理器 | 无 | StealthManager |
| `getStats()` | 获取池统计信息 | 无 | PoolStats |

### BrowserTab

| 方法 | 描述 | 参数 | 返回值 |
|------|------|------|--------|
| `navigate(String)` | 导航到URL | url | Response |
| `navigate(String, Page.NavigateOptions)` | 导航到URL（带选项） | url, 选项 | Response |
| `waitForLoadState(LoadState)` | 等待页面加载状态 | 加载状态 | void |
| `url()` | 获取当前URL | 无 | String |
| `title()` | 获取页面标题 | 无 | String |
| `content()` | 获取页面HTML | 无 | String |
| `reload()` | 重新加载页面 | 无 | Response |
| `reload(Page.ReloadOptions)` | 重新加载页面（带选项） | 选项 | Response |
| `screenshot(Page.ScreenshotOptions)` | 截图 | 选项 | byte[] |
| `evaluate(String)` | 执行JavaScript | 表达式 | Object |
| `evaluate(String, Object)` | 执行JavaScript（带参数） | 表达式, 参数 | Object |
| `getStorageStateJson()` | 获取存储状态JSON | 无 | String |
| `consume(BiConsumer<BrowserContext, Page>)` | 执行自定义操作 | 消费者 | void |
| `execute(BiFunction<BrowserContext, Page, T>)` | 执行自定义操作（带返回值） | 函数 | T |
| `executeAsync(BiFunction<BrowserContext, Page, T>)` | 异步执行自定义操作 | 函数 | Future<T> |
| `close()` | 关闭并归还页签 | 无 | void |

## 常见问题

### Q1: 如何调整并发能力？

**A**: 通过修改 `application.yml` 中的配置：

```yaml
uw:
  webot:
    bot-pool:
      max-browsers-per-group: 10    # 增加浏览器实例数
      max-tabs-per-browser: 30      # 增加每个实例的页签数
```

总并发能力 = 浏览器类型数 × max-browsers-per-group × max-tabs-per-browser

### Q2: 页签获取超时怎么办？

**A**: 可能原因及解决方案：

1. **资源不足**：增加 `max-browsers-per-group` 或 `max-tabs-per-browser`
2. **页签未释放**：确保使用 `try-with-resources` 或手动调用 `tab.close()`
3. **浏览器崩溃**：检查日志，查看是否有浏览器实例异常退出

### Q3: 如何配置分布式会话？

**A**: 

```yaml
uw:
  webot:
    session:
      distributed: true
```

并确保项目中引入了 FusionCache 依赖。

### Q4: 验证码服务如何选择？

**A**: 

| 服务类型 | 适用场景 | 成本 | 准确率 |
|---------|---------|------|--------|
| OCR | 简单数字/字母验证码 | 低 | 中 |
| 2Captcha | 复杂验证码、ReCaptcha | 中 | 高 |
| Capsolver | AI 验证码、ReCaptcha V3 | 中高 | 很高 |

### Q5: 如何排查代理问题？

**A**: 

1. 检查代理配置是否正确
2. 使用 `proxyService.checkProxyHealth(proxy)` 测试代理可用性
3. 查看日志中的代理健康检查记录
4. 确认代理类型（HTTP/HTTPS/SOCKS）与目标网站兼容

### Q6: 内存占用过高怎么办？

**A**: 

1. **减少浏览器实例数**：降低 `max-browsers-per-group`
2. **及时释放资源**：确保页签使用完毕后调用 `close()`
3. **缩短会话过期时间**：减少 `expire-time`
4. **启用无头模式**：`headless: true`