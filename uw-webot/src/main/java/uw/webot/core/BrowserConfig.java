package uw.webot.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 浏览器配置类。
 * <p>
 * 配置浏览器类型、视口大小、User-Agent 等参数。
 * </p>
 *
 * @author axeon
 * @since 1.0.0
 */
public class BrowserConfig {

    /**
     * 浏览器可执行文件路径（可选，用于指定自定义浏览器）。
     */
    private String executablePath;

    /**
     * 默认浏览器类型。
     */
    public static final BrowserType DEFAULT_BROWSER_TYPE = BrowserType.CHROMIUM;

    /**
     * 默认无头模式。
     */
    public static final boolean DEFAULT_HEADLESS = true;

    /**
     * 默认视口宽度。
     */
    public static final int DEFAULT_VIEWPORT_WIDTH = 1920;

    /**
     * 默认视口高度。
     */
    public static final int DEFAULT_VIEWPORT_HEIGHT = 1080;

    /**
     * 默认JavaScript启用状态。
     */
    public static final boolean DEFAULT_JAVASCRIPT_ENABLED = true;

    /**
     * 浏览器类型。
     */
    private BrowserType browserType = DEFAULT_BROWSER_TYPE;

    /**
     * 是否无头模式。
     */
    private boolean headless = DEFAULT_HEADLESS;

    /**
     * 视口宽度。
     */
    private int viewportWidth = DEFAULT_VIEWPORT_WIDTH;

    /**
     * 视口高度。
     */
    private int viewportHeight = DEFAULT_VIEWPORT_HEIGHT;

    /**
     * User-Agent字符串。
     */
    private String userAgent;

    /**
     * 语言设置。
     */
    private String locale;

    /**
     * 时区。
     */
    private String timezone;

    /**
     * 额外启动参数。
     */
    private List<String> args = new ArrayList<>();

    /**
     * 是否启用JavaScript。
     */
    private boolean javaScriptEnabled = DEFAULT_JAVASCRIPT_ENABLED;


    public BrowserConfig() {
    }


    private BrowserConfig(Builder builder) {
        setExecutablePath(builder.executablePath);
        setBrowserType(builder.browserType);
        setHeadless(builder.headless);
        setViewportWidth(builder.viewportWidth);
        setViewportHeight(builder.viewportHeight);
        setUserAgent(builder.userAgent);
        setLocale(builder.locale);
        setTimezone(builder.timezone);
        setArgs(builder.args);
        setJavaScriptEnabled(builder.javaScriptEnabled);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(BrowserConfig copy) {
        Builder builder = new Builder();
        builder.executablePath = copy.getExecutablePath();
        builder.browserType = copy.getBrowserType();
        builder.headless = copy.isHeadless();
        builder.viewportWidth = copy.getViewportWidth();
        builder.viewportHeight = copy.getViewportHeight();
        builder.userAgent = copy.getUserAgent();
        builder.locale = copy.getLocale();
        builder.timezone = copy.getTimezone();
        builder.args = copy.getArgs();
        builder.javaScriptEnabled = copy.isJavaScriptEnabled();
        return builder;
    }


    /**
     * 获取浏览器标签。
     *
     * @return 浏览器标签
     */
    public String getBrowserGroupTag() {
        StringBuilder sb = new StringBuilder();
        sb.append(browserType.name().toLowerCase());
        if (headless) {
            sb.append(":headless");
        }
        if (executablePath != null && !executablePath.isEmpty()) {
            sb.append(":").append(executablePath);
        }
        return sb.toString();
    }

    public String getExecutablePath() {
        return executablePath;
    }

    public void setExecutablePath(String executablePath) {
        this.executablePath = executablePath;
    }

    public BrowserType getBrowserType() {
        return browserType;
    }

    public void setBrowserType(BrowserType browserType) {
        this.browserType = browserType;
    }

    public boolean isHeadless() {
        return headless;
    }

    public void setHeadless(boolean headless) {
        this.headless = headless;
    }

    public int getViewportWidth() {
        return viewportWidth;
    }

    public void setViewportWidth(int viewportWidth) {
        this.viewportWidth = viewportWidth;
    }

    public int getViewportHeight() {
        return viewportHeight;
    }

    public void setViewportHeight(int viewportHeight) {
        this.viewportHeight = viewportHeight;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args;
    }

    public boolean isJavaScriptEnabled() {
        return javaScriptEnabled;
    }

    public void setJavaScriptEnabled(boolean javaScriptEnabled) {
        this.javaScriptEnabled = javaScriptEnabled;
    }

    public static final class Builder {
        private String executablePath;
        private BrowserType browserType = DEFAULT_BROWSER_TYPE;
        private boolean headless = DEFAULT_HEADLESS;
        private int viewportWidth = DEFAULT_VIEWPORT_WIDTH;
        private int viewportHeight = DEFAULT_VIEWPORT_HEIGHT;
        private String userAgent;
        private String locale;
        private String timezone;
        private List<String> args;
        private boolean javaScriptEnabled;

        private Builder() {
        }

        public Builder executablePath(String executablePath) {
            this.executablePath = executablePath;
            return this;
        }

        public Builder browserType(BrowserType browserType) {
            this.browserType = browserType;
            return this;
        }

        public Builder headless(boolean headless) {
            this.headless = headless;
            return this;
        }

        public Builder viewportWidth(int viewportWidth) {
            this.viewportWidth = viewportWidth;
            return this;
        }

        public Builder viewportHeight(int viewportHeight) {
            this.viewportHeight = viewportHeight;
            return this;
        }

        public Builder viewportSize(int width, int height) {
            this.viewportWidth = width;
            this.viewportHeight = height;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder locale(String locale) {
            this.locale = locale;
            return this;
        }

        public Builder timezone(String timezone) {
            this.timezone = timezone;
            return this;
        }

        public Builder args(List<String> args) {
            this.args = args;
            return this;
        }

        public Builder javaScriptEnabled(boolean javaScriptEnabled) {
            this.javaScriptEnabled = javaScriptEnabled;
            return this;
        }

        public BrowserConfig build() {
            return new BrowserConfig(this);
        }

    }
}
