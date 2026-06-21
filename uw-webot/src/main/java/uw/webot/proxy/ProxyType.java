package uw.webot.proxy;

/**
 * 代理类型枚举。
 * <p>
 * 支持 HTTP、HTTPS、SOCKS4、SOCKS5 四种代理协议。
 * ANY 表示任意代理协议。
 * </p>
 */
public enum ProxyType {
    /** HTTP 代理协议。 */
    HTTP,
    /** HTTPS（HTTP CONNECT over TLS）代理协议。 */
    HTTPS,
    /** SOCKS4 代理协议。 */
    SOCKS4,
    /** SOCKS5 代理协议。 */
    SOCKS5,
    /** 匹配任意代理协议（用于不限制类型的查询）。 */
    ANY;
}
