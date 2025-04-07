package uw.auth.service.util;

import jakarta.servlet.http.HttpServletRequest;
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

    private static final Logger logger = LoggerFactory.getLogger( IpWebUtils.class );

    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP = "X-Real-IP";

    /**
     * 获取客户端真实IP地址（返回 {@link String}）。
     * <p>
     * 优先级顺序：X-Forwarded-For → X-Real-IP → 远程地址。
     * <p>
     * 如果X-Forwarded-For包含多个IP（如 `client, proxy1, proxy2`），将取第一个有效IP。
     *
     * @param request HTTP请求对象
     * @return 客户端真实IP地址字符串，如果无法获取返回 {@code null}
     */
    public static String getRealIpString(HttpServletRequest request) {
        // 1. 获取代理前的IP
        String proxiedIp = getProxiedIpString( request );

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
            return InetAddress.getByName( getRealIpString( request ) );
        } catch (UnknownHostException e) {
            logger.error( e.getMessage(), e );
            return null;
        }
    }

    /**
     * 获取代理后的真实IP地址（返回 {@link String}）。
     *
     * @param request
     * @return
     */
    private static String getProxiedIpString(HttpServletRequest request) {
        // 1. 检查 X-Forwarded-For（优先级最高）
        String ip = request.getHeader( HEADER_X_FORWARDED_FOR );
        if (isValidIp( ip )) {
            // 直接取逗号前的IP（第一个有效IP）
            int commaIndex = ip.indexOf( ',' );
            String candidate = commaIndex != -1 ? ip.substring( 0, commaIndex ).trim() : ip.trim();
            if (isValidIp( candidate )) {
                return candidate;
            }
        }

        // 2. 检查 X-Real-IP
        ip = request.getHeader( HEADER_X_REAL_IP );
        if (isValidIp( ip )) {
            return ip.trim();
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
        return ip != null && !ip.trim().isEmpty() && !"unknown".equalsIgnoreCase( ip.trim() );
    }

}
