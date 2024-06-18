package uw.auth.service.ipblock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * IP匹配器。
 */
public class IpMatchUtils {

    /**
     * 对ip列表进行排序。
     *
     * @param requiredIpList
     * @return
     */
    public static List<IpRange> sortList(String[] requiredIpList) {
        List<IpRange> sortedIpList = new ArrayList<>( requiredIpList.length );
        if (requiredIpList == null || requiredIpList.length == 0) {
            return sortedIpList;
        }

        List<IpRange> tempIpList = new ArrayList<>( requiredIpList.length );
        for (String requiredIp : requiredIpList) {
            //要过滤排重。
            if (requiredIp != null) {
                requiredIp = requiredIp.trim();
            }
            if (requiredIp.length() > 0) {
                tempIpList.add( new IpRange( requiredIp ) );
            }
        }
        //先排序
        Collections.sort( tempIpList, (o1, o2) -> {
            if (o1.getFrom() != o2.getFrom()) {
                return o1.getFrom() > o2.getFrom() ? 1 : -1;
            }
            if (o1.getTo() != o2.getTo()) {
                return o1.getTo() > o2.getTo() ? 1 : -1;
            }
            return 0;
        } );

        //再整理
        int insertIndex = 0;
        for (IpRange ipRange : tempIpList) {
            if (sortedIpList.isEmpty()) {
                sortedIpList.add( ipRange );
                continue;
            }

            IpRange beforeIpRange = sortedIpList.get( insertIndex );

            if (ipRange.getFrom() <= beforeIpRange.getTo() + 1) {
                beforeIpRange.setTo( ipRange.getTo() );
            } else {
                sortedIpList.add( ipRange );
                ++insertIndex;
            }
        }
        return sortedIpList;
    }

    /**
     * 匹配IP
     *
     * @param ip
     * @return
     */
    public static boolean matches(List<IpRange> sortedIpList, String ip) {
        IpRange targetIpRange = new IpRange( ip );
        long targetIpLong = targetIpRange.getFrom();

        int start = 0;
        int end = sortedIpList.size() - 1;
        while (start <= end) {
            int mid = (start + end) / 2;
            IpRange ipRange = sortedIpList.get( mid );
            if (targetIpLong < ipRange.getFrom()) {
                end = mid - 1;
            } else if (targetIpLong > ipRange.getTo()) {
                start = mid + 1;
            } else {
                return true;
            }
        }
        return false;
    }
}
