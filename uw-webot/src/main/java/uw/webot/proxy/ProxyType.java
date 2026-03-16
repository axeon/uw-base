package uw.webot.proxy;

/**
 * 代理类型枚举。
 * <p>
 * 支持 HTTP、HTTPS、SOCKS4、SOCKS5 四种代理协议。
 * ANY 表示任意代理协议。
 * </p>
 */
public enum ProxyType {
    HTTP,
    HTTPS,
    SOCKS4,
    SOCKS5,
    ANY;
}
