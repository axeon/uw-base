package uw.auth.service.util;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.util.IpMatchUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * ip web工具类。
 *
 * @author zhangjin
 */
public class IpWebUtils {

    private static final Logger logger = LoggerFactory.getLogger(IpWebUtils.class);

    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP = "X-Real-IP";

    /**
     * 可信代理IP范围列表。
     */
    private static volatile List<IpMatchUtils.IpRange> trustedProxyRanges;

    /**
     * 初始化可信代理列表。由 AuthServiceAutoConfiguration 在启动时调用。
     *
     * @param trustedProxies 逗号分隔的可信代理IP/CIDR列表
     */
    public static void initTrustedProxies(String trustedProxies) {
        trustedProxyRanges = IpMatchUtils.sortList(trustedProxies.split(","));
        logger.info("IpWebUtils trusted proxies initialized: {}", trustedProxies);
    }

    /**
     * 程序化设置可信代理列表。
     *
     * @param ranges 可信代理IP范围列表
     */
    public static void setTrustedProxies(List<IpMatchUtils.IpRange> ranges) {
        trustedProxyRanges = ranges;
    }

    /**
     * 获取客户端真实IP地址（返回 {@link String}）。
     * <p>
     * 仅当请求的直接来源IP在可信代理列表内时，才会从 X-Real-IP / X-Forwarded-For 头中提取IP。
     * 否则直接使用 TCP 连接的远程地址，防止伪造头绕过IP白名单。
     *
     * @param request HTTP请求对象
     * @return 客户端真实IP地址字符串
     */
    public static String getRealIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        // 仅当来源IP是可信代理时，才读取转发头
        if (isTrustedProxy(remoteAddr)) {
            String proxiedIp = getProxiedIp(request);
            if (proxiedIp != null) {
                return proxiedIp;
            }
        }

        return remoteAddr;
    }

    /**
     * 获取客户端真实IP地址（返回 {@link InetAddress} 对象）。
     *
     * @param request HTTP请求对象
     * @return 客户端真实IP地址，如果无法获取或解析失败返回 {@code null}
     */
    public static InetAddress getRealIpAddress(HttpServletRequest request) {
        try {
            return InetAddress.getByName(getRealIp(request));
        } catch (UnknownHostException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * 判断给定IP是否属于可信代理。
     */
    private static boolean isTrustedProxy(String ip) {
        if (trustedProxyRanges == null || trustedProxyRanges.isEmpty()) {
            // 未配置可信代理时，不信任任何转发头
            return false;
        }
        return IpMatchUtils.matches(trustedProxyRanges, ip);
    }

    /**
     * 从请求头中提取代理转发的真实IP。
     */
    private static String getProxiedIp(HttpServletRequest request) {
        // 1. 检查 X-Real-IP（优先级最高，由最接近应用的可信代理设置）
        String realIp = request.getHeader(HEADER_X_REAL_IP);
        if (isValidIp(realIp)) {
            return realIp.trim();
        }

        // 2. 检查 X-Forwarded-For，取最后一个有效IP（最可信的代理添加的）
        String forwarded = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (StringUtils.isNotBlank(forwarded)) {
            String[] ips = forwarded.split(",");
            for (int i = ips.length - 1; i >= 0; i--) {
                String candidate = ips[i].trim();
                if (isValidIp(candidate)) {
                    return candidate;
                }
            }
        }

        return null;
    }

    /**
     * 验证IP字符串是否有效。
     */
    private static boolean isValidIp(String ip) {
        return ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip);
    }

}
