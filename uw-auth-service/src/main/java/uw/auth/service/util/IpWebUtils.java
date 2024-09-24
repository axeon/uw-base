package uw.auth.service.util;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ip web工具类。
 *
 * @author zhangjin
 */
public class IpWebUtils {

    private static final Logger logger = LoggerFactory.getLogger( IpWebUtils.class );

    /**
     * 根据http请求头，请求ip地址。
     *
     * @param request
     * @return
     */
    public static String getTrueIp(HttpServletRequest request) {
        String ip = request.getHeader( "x-forwarded-for" );
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase( ip )) {
            ip = request.getHeader( "Proxy-Client-IP" );
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase( ip )) {
            ip = request.getRemoteAddr();
        }
        if (ip.contains( "," )) {
            ip = ip.substring( ip.lastIndexOf( "," ) + 1, ip.length() );
        }
        return ip;
    }
}
