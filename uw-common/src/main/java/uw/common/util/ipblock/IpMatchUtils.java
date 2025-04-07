package uw.common.util.ipblock;


import java.util.ArrayList;
import java.util.List;

/**
 * IP匹配器。
 */
public class IpMatchUtils {


    /**
     * 对ip列表进行排序。
     *
     * @return
     */
    public static List<IpRange> sortList(List<IpRange> ipRangeList) {
        if (ipRangeList == null || ipRangeList.isEmpty()) {
            return ipRangeList;
        }
        List<IpRange> sortedIpList = new ArrayList<>( ipRangeList.size() );
        //先排序
        ipRangeList.sort( (o1, o2) -> {
            if (o1.getStart() != o2.getStart()) {
                return o1.getStart() > o2.getStart() ? 1 : -1;
            }
            if (o1.getEnd() != o2.getEnd()) {
                return o1.getEnd() > o2.getEnd() ? 1 : -1;
            }
            return 0;
        } );

        //再合并
        int insertIndex = 0;
        for (IpRange ipRange : ipRangeList) {
            if (sortedIpList.isEmpty()) {
                sortedIpList.add( ipRange );
                continue;
            }

            IpRange beforeIpRange = sortedIpList.get( insertIndex );

            if (ipRange.getStart() <= beforeIpRange.getEnd() + 1) {
                beforeIpRange.setEnd( ipRange.getEnd() );
            } else {
                sortedIpList.add( ipRange );
                ++insertIndex;
            }
        }
        return sortedIpList;
    }

    /**
     * 对ip列表进行排序。
     *
     * @param ipList
     * @return
     */
    public static List<IpRange> sortList(String[] ipList) {
        if (ipList == null || ipList.length == 0) {
            return new ArrayList<>( 0 );
        }
        List<IpRange> ipRangeList = new ArrayList<>( ipList.length );
        for (String requiredIp : ipList) {
            //要过滤排重。
            if (requiredIp != null) {
                requiredIp = requiredIp.trim();
                if (!requiredIp.isEmpty()) {
                    ipRangeList.add( new IpRange( requiredIp ) );
                }
            }
        }
        return sortList( ipRangeList );
    }

    /**
     * 匹配IP
     *
     * @param ipLong
     * @return
     */
    public static boolean matches(List<IpRange> sortedIpList, long ipLong) {
        int start = 0;
        int end = sortedIpList.size() - 1;
        while (start <= end) {
            int mid = (start + end) / 2;
            IpRange ipRange = sortedIpList.get( mid );
            if (ipLong < ipRange.getStart()) {
                end = mid - 1;
            } else if (ipLong > ipRange.getEnd()) {
                start = mid + 1;
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * 匹配IP
     *
     * @param ipStr
     * @return
     */
    public static boolean matches(List<IpRange> sortedIpList, String ipStr) {
        return matches( sortedIpList, ipToLong( ipStr ) );
    }

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
}
