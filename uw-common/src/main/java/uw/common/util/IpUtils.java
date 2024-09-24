package uw.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ip工具类。用于获取本机内外网ip.
 *
 * @author zhangjin
 */
public class IpUtils {

    private static final Logger logger = LoggerFactory.getLogger( IpUtils.class );

    /**
     * 将字符串类型的ip转成long。
     *
     * @param strIp
     * @return
     */
    public static long ipToLong(String strIp) {
        String[] ips = strIp.split( "\\." );
        long[] ip = new long[4];
        if (ips.length != 4) {
            return -1;
        }
        try {
            ip[0] = Long.parseLong( ips[0] );
            ip[1] = Long.parseLong( ips[1] );
            ip[2] = Long.parseLong( ips[2] );
            ip[3] = Long.parseLong( ips[3] );
        } catch (Exception e) {
            ip = new long[4];
        }
        // 将每个.之间的字符串转换成整型
        return (ip[0] << 24) + (ip[1] << 16) + (ip[2] << 8) + ip[3];
    }

    /**
     * 将十进制整数形式转换成127.0.0.1形式的ip地址
     *
     * @param longIp
     * @return
     */
    public static String longToIp(long longIp) {
        return (longIp >>> 24) + "." +
                // 将高8位置0，然后右移16位
                ((longIp & 0x00FFFFFF) >>> 16) + "." +
                // 将高16位置0，然后右移8位
                ((longIp & 0x0000FFFF) >>> 8) + "." +
                // 将高24位置0
                (longIp & 0x000000FF);
    }

}
