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
     * @param ipStr
     * @return
     */
    public static long ipToLong(String ipStr) {
        if (ipStr == null) {
            throw new IllegalArgumentException( "ip地址信息为null!" );
        }
        int pos1 = ipStr.indexOf( "." );
        int pos2 = ipStr.indexOf( ".", pos1 + 1 );
        int pos3 = ipStr.indexOf( ".", pos2 + 1 );
        if (pos1 == -1 || pos2 == -1 || pos3 == -1 || pos1 > 15 || pos2 > 15 || pos3 > 15) {
            throw new IllegalArgumentException( "非法的ip格式: " + ipStr );
        }
        long ipLong = Long.parseLong( ipStr.substring( 0, pos1 ) ) << 24;
        ipLong += Long.parseLong( ipStr.substring( pos1 + 1, pos2 ) ) << 16;
        ipLong += Long.parseLong( ipStr.substring( pos2 + 1, pos3 ) ) << 8;
        ipLong += Long.parseLong( ipStr.substring( pos3 + 1 ) );
        return ipLong;
    }

    /**
     * 将十进制整数形式转换成127.0.0.1形式的ip地址
     *
     * @param ipLong
     * @return
     */
    public static String longToIp(long ipLong) {
        return (ipLong >>> 24) + "." +
                // 将高8位置0，然后右移16位
                ((ipLong & 0x00FFFFFF) >>> 16) + "." +
                // 将高16位置0，然后右移8位
                ((ipLong & 0x0000FFFF) >>> 8) + "." +
                // 将高24位置0
                (ipLong & 0x000000FF);
    }

}
