package uw.mfa.ipblock;


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
     * @param ip
     * @return
     */
    public static boolean matches(List<IpRange> sortedIpList, String ip) {
        IpRange targetIpRange = new IpRange( ip );
        long targetIpLong = targetIpRange.getStart();

        int start = 0;
        int end = sortedIpList.size() - 1;
        while (start <= end) {
            int mid = (start + end) / 2;
            IpRange ipRange = sortedIpList.get( mid );
            if (targetIpLong < ipRange.getStart()) {
                end = mid - 1;
            } else if (targetIpLong > ipRange.getEnd()) {
                start = mid + 1;
            } else {
                return true;
            }
        }
        return false;
    }
}
