package uw.webot.stealth;

import java.util.ArrayList;
import java.util.List;

/**
 * 反检测配置类。
 * <p>
 * 配置浏览器反检测功能，包括 WebDriver 隐藏、WebGL 指纹伪装、浏览器指纹随机化等。
 * 用于绕过网站的自动化检测机制。
 * </p>
 *
 * @author axeon
 * @see StealthService
 * @since 1.0.0
 */
public class StealthConfig {

    /**
     * 是否隐藏WebDriver属性。
     */
    private boolean hideWebDriver = true;

    /**
     * 是否启用WebGL指纹伪装。
     */
    private boolean webglSpoofing = true;

    /**
     * 是否启用插件伪装。
     */
    private boolean pluginSpoofing = true;

    /**
     * 是否启用字体伪装。
     */
    private boolean fontSpoofing = true;

    /**
     * 是否启用指纹随机化。
     */
    private boolean fingerprintRandomization = false;

    /**
     * 自定义User-Agent列表。
     */
    private List<String> userAgents = new ArrayList<>();

    /**
     * 自定义屏幕分辨率列表。
     */
    private List<String> screenResolutions = new ArrayList<>();

    /**
     * 自定义时区列表。
     */
    private List<String> timezones = new ArrayList<>();

    public StealthConfig() {
    }

    private StealthConfig(Builder builder) {
        setHideWebDriver(builder.hideWebDriver);
        setWebglSpoofing(builder.webglSpoofing);
        setPluginSpoofing(builder.pluginSpoofing);
        setFontSpoofing(builder.fontSpoofing);
        setFingerprintRandomization(builder.fingerprintRandomization);
        setUserAgents(builder.userAgents);
        setScreenResolutions(builder.screenResolutions);
        setTimezones(builder.timezones);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(StealthConfig copy) {
        Builder builder = new Builder();
        builder.hideWebDriver = copy.isHideWebDriver();
        builder.webglSpoofing = copy.isWebglSpoofing();
        builder.pluginSpoofing = copy.isPluginSpoofing();
        builder.fontSpoofing = copy.isFontSpoofing();
        builder.fingerprintRandomization = copy.isFingerprintRandomization();
        builder.userAgents = copy.getUserAgents();
        builder.screenResolutions = copy.getScreenResolutions();
        builder.timezones = copy.getTimezones();
        return builder;
    }

    public boolean isHideWebDriver() {
        return hideWebDriver;
    }

    public void setHideWebDriver(boolean hideWebDriver) {
        this.hideWebDriver = hideWebDriver;
    }

    public boolean isWebglSpoofing() {
        return webglSpoofing;
    }

    public void setWebglSpoofing(boolean webglSpoofing) {
        this.webglSpoofing = webglSpoofing;
    }

    public boolean isFingerprintRandomization() {
        return fingerprintRandomization;
    }

    public void setFingerprintRandomization(boolean fingerprintRandomization) {
        this.fingerprintRandomization = fingerprintRandomization;
    }

    public boolean isPluginSpoofing() {
        return pluginSpoofing;
    }

    public void setPluginSpoofing(boolean pluginSpoofing) {
        this.pluginSpoofing = pluginSpoofing;
    }

    public boolean isFontSpoofing() {
        return fontSpoofing;
    }

    public void setFontSpoofing(boolean fontSpoofing) {
        this.fontSpoofing = fontSpoofing;
    }

    public List<String> getUserAgents() {
        return userAgents;
    }

    public void setUserAgents(List<String> userAgents) {
        this.userAgents = userAgents;
    }

    public List<String> getScreenResolutions() {
        return screenResolutions;
    }

    public void setScreenResolutions(List<String> screenResolutions) {
        this.screenResolutions = screenResolutions;
    }

    public List<String> getTimezones() {
        return timezones;
    }

    public void setTimezones(List<String> timezones) {
        this.timezones = timezones;
    }

    public static final class Builder {

        /**
         * 是否隐藏WebDriver属性。
         */
        private boolean hideWebDriver = true;

        /**
         * 是否启用WebGL指纹伪装。
         */
        private boolean webglSpoofing = true;

        /**
         * 是否启用插件伪装。
         */
        private boolean pluginSpoofing = true;

        /**
         * 是否启用字体伪装。
         */
        private boolean fontSpoofing = true;

        /**
         * 是否启用指纹随机化。
         */
        private boolean fingerprintRandomization = false;

        /**
         * 自定义User-Agent列表。
         */
        private List<String> userAgents = new ArrayList<>();

        /**
         * 自定义屏幕分辨率列表。
         */
        private List<String> screenResolutions = new ArrayList<>();

        /**
         * 自定义时区列表。
         */
        private List<String> timezones = new ArrayList<>();

        private Builder() {
        }


        public Builder hideWebDriver(boolean hideWebDriver) {
            this.hideWebDriver = hideWebDriver;
            return this;
        }

        public Builder webglSpoofing(boolean webglSpoofing) {
            this.webglSpoofing = webglSpoofing;
            return this;
        }

        public Builder pluginSpoofing(boolean pluginSpoofing) {
            this.pluginSpoofing = pluginSpoofing;
            return this;
        }

        public Builder fontSpoofing(boolean fontSpoofing) {
            this.fontSpoofing = fontSpoofing;
            return this;
        }

        public Builder fingerprintRandomization(boolean fingerprintRandomization) {
            this.fingerprintRandomization = fingerprintRandomization;
            return this;
        }

        public Builder userAgents(List<String> userAgents) {
            this.userAgents = userAgents;
            return this;
        }

        public Builder screenResolutions(List<String> screenResolutions) {
            this.screenResolutions = screenResolutions;
            return this;
        }

        public Builder timezones(List<String> timezones) {
            this.timezones = timezones;
            return this;
        }

        public StealthConfig build() {
            return new StealthConfig(this);
        }
    }
}
