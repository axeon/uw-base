package uw.webot.core;

/**
 * 浏览器类型枚举。
 * <p>
 * 支持 Chromium、Firefox、WebKit 三种浏览器类型。
 * </p>
 */
public enum BrowserType {
    /** Google Chromium / Chrome 内核浏览器。 */
    CHROMIUM,
    /** Mozilla Firefox 浏览器。 */
    FIREFOX,
    /** WebKit 内核浏览器（macOS 上为 Safari 内核）。 */
    WEBKIT
}
