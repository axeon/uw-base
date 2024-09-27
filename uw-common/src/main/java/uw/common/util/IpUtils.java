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
        int pos1 = strIp.indexOf( "." );
        int pos2 = strIp.indexOf( ".", pos1 + 1 );
        int pos3 = strIp.indexOf( ".", pos2 + 1 );
        if (pos1 == -1 || pos2 == -1 || pos3 == -1 || pos1 > 15 || pos2 > 15 || pos3 > 15) {
            return -1;
        }
        long ip = Long.parseLong( strIp.substring( 0, pos1 ) ) << 24;
        ip += Long.parseLong( strIp.substring( pos1 + 1, pos2 ) ) << 16;
        ip += Long.parseLong( strIp.substring( pos2 + 1, pos3 ) ) << 8;
        ip += Long.parseLong( strIp.substring( pos3 + 1 ) );
        return ip;
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
