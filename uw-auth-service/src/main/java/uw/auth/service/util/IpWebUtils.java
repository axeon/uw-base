package uw.auth.service.util;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

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
     * 获取客户端真实IP地址（返回 {@link String}）。
     * <p>
     * 优先级顺序：X-Real-IP → X-Forwarded-For → 远程地址。
     * <p>
     * X-Real-IP 通常由最接近应用的可信代理（如 Nginx）设置，比 X-Forwarded-For 更难伪造。
     * 如果 X-Forwarded-For 包含多个 IP（如 `client, proxy1, proxy2`），将取最后一个有效IP（最可信的代理添加的）。
     *
     * @param request HTTP请求对象
     * @return 客户端真实IP地址字符串，如果无法获取返回 {@code null}
     */
    public static String getRealIp(HttpServletRequest request) {
        // 1. 获取代理前的IP
        String proxiedIp = getProxiedIp(request);

        // 2. 获取代理前的IP
        if (proxiedIp != null) {
            return proxiedIp;
        }

        // 3. 获取直接连接的IP
        return request.getRemoteAddr();
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
     * 获取代理后的真实IP地址（返回 {@link String}）。
     *
     * @param request
     * @return
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
     *
     * @param ip IP字符串
     * @return 是否有效（非空、非"unknown"）
     */
    private static boolean isValidIp(String ip) {
        return ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip);
    }

}
