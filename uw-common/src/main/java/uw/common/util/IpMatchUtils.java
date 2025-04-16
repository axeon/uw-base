package uw.common.util;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * IP匹配器，支持IPv4/IPv6的IP范围匹配和排序。
 */
public class IpMatchUtils {

    /**
     * 对IP范围列表进行排序和合并。
     *
     * @param ipRangeList 待排序的IP范围列表
     * @return 排序合并后的IP范围列表
     */
    public static List<IpRange> sortList(List<IpRange> ipRangeList) {
        if (ipRangeList == null || ipRangeList.isEmpty()) {
            return ipRangeList;
        }

        // 1. 按起始地址排序
        ipRangeList.sort((o1, o2) -> IpRange.compareIp(o1.getStart(), o2.getStart()));

        List<IpRange> sortedList = new ArrayList<>(ipRangeList.size());

        // 2. 合并重叠或相邻的IP范围
        for (IpRange ipRange : ipRangeList) {
            if (sortedList.isEmpty()) {
                sortedList.add(ipRange);
            } else {
                IpRange lastRange = sortedList.getLast();
                if (IpRange.isOverlappingOrAdjacent(ipRange, lastRange)) {
                    // 合并范围
                    lastRange.setEnd(IpRange.mergeEnd(lastRange.getEnd(), ipRange.getEnd()));
                } else {
                    sortedList.add(ipRange);
                }
            }
        }

        return sortedList;
    }

    /**
     * 将字符串IP列表转换为排序后的IP范围列表。
     *
     * @param ipList 待处理的IP字符串数组
     * @return 排序后的IP范围列表
     */
    public static List<IpRange> sortList(String[] ipList) {
        if (ipList == null || ipList.length == 0) {
            return new ArrayList<>();
        }

        List<IpRange> ipRanges = new ArrayList<>();
        for (String ipStr : ipList) {
            if (ipStr == null || ipStr.trim().isEmpty()) {
                continue;
            }
            ipRanges.add(new IpRange(ipStr));
        }
        return sortList(ipRanges);
    }

    /**
     * 检查指定IP是否在已排序的IP范围内。
     *
     * @param sortedIpList 已排序的IP范围列表
     * @param ipStr        待检查的IP字符串
     * @return 是否匹配
     */
    public static boolean matches(List<IpRange> sortedIpList, String ipStr) {
        if (StringUtils.isBlank(ipStr)) {
            return false;
        }
        return matches(sortedIpList, parseInetAddress(ipStr));
    }

    /**
     * 检查指定IP是否在已排序的IP范围内。
     *
     * @param sortedIpList 已排序的IP范围列表
     * @param inetAddress  待检查的IP
     * @return 是否匹配
     */
    public static boolean matches(List<IpRange> sortedIpList, InetAddress inetAddress) {
        if (inetAddress == null) {
            return false;
        }
        if (sortedIpList == null || sortedIpList.isEmpty()) {
            return false;
        }
        byte[] ipBytes = inetAddressToBytes(inetAddress);
        int low = 0;
        int high = sortedIpList.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            IpRange range = sortedIpList.get(mid);
            int cmpStart = IpRange.compareIp(ipBytes, range.getStart());
            int cmpEnd = IpRange.compareIp(ipBytes, range.getEnd());

            if (cmpStart < 0) {
                high = mid - 1;
            } else if (cmpEnd > 0) {
                low = mid + 1;
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * 将Ip字符串转换为InetAddress对象。
     * 主要是把异常修改为
     *
     * @param ip
     * @return
     */
    public static InetAddress parseInetAddress(String ip) {
        try {
            return InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("非法的IP格式: " + ip, e);
        }
    }

    /**
     * 将IP字符串转换为IPv6格式的字节数组。
     *
     * @param inetAddress InetAddress（支持IPv4/IPv6）
     * @return 16字节的IPv6格式字节数组
     */
    public static byte[] inetAddressToBytes(InetAddress inetAddress) {
        byte[] ipBytes = inetAddress.getAddress();
        if (inetAddress instanceof Inet4Address) {
            byte[] ipv6Bytes = new byte[16];
            // 前10字节为0，第11和12字节设为0xFF，IPv4地址在最后4字节
            ipv6Bytes[10] = (byte) 0xFF;
            ipv6Bytes[11] = (byte) 0xFF;
            System.arraycopy(ipBytes, 0, ipv6Bytes, 12, 4); // IPv4地址从第12字节开始
            return ipv6Bytes;
        }
        return ipBytes;
    }

    /**
     * IP范围对象，存储起始和结束地址的16字节IPv6格式。
     */
    @Schema(title = "IP范围对象", description = "存储IPv4/IPv6的IP地址范围")
    public static class IpRange {

        private static final Logger log = LoggerFactory.getLogger(IpRange.class);
        /**
         * IPv6字节数组的长度。
         */
        private static final int IPV6_BYTE_LENGTH = 16;

        /**
         * 起始地址的16字节IPv6格式。
         */
        @Schema(title = "起始地址（IPv6字节数组）", description = "16字节的IPv6地址起始值")
        private byte[] start;

        /**
         * 结束地址的16字节IPv6格式。
         */
        @Schema(title = "结束地址（IPv6字节数组）", description = "16字节的IPv6地址结束值")
        private byte[] end;

        /**
         * 构造一个空的IP范围对象。
         */
        public IpRange() {
            this.start = new byte[IPV6_BYTE_LENGTH];
            this.end = new byte[IPV6_BYTE_LENGTH];
        }

        /**
         * 通过IP模式字符串构造IP范围。
         *
         * @param ipPattern IP模式字符串（支持CIDR和通配符）
         */
        public IpRange(String ipPattern) {
            this.start = new byte[IPV6_BYTE_LENGTH];
            this.end = new byte[IPV6_BYTE_LENGTH];
            convert(ipPattern);
        }

        /**
         * 创建指定掩码位数的IPv6掩码字节数组。
         */
        private static byte[] createMask(int maskBits) {
            byte[] mask = new byte[IPV6_BYTE_LENGTH];
            int byteIndex = maskBits / 8;
            int bitIndex = maskBits % 8;
            for (int i = 0; i < byteIndex; i++) {
                mask[i] = (byte) 0xFF;
            }
            if (bitIndex > 0) {
                mask[byteIndex] = (byte) (0xFF << (8 - bitIndex));
            }
            return mask;
        }

        /**
         * 对IP字节数组应用掩码。
         */
        private static void applyMask(byte[] ip, byte[] mask) {
            for (int i = 0; i < IPV6_BYTE_LENGTH; i++) {
                ip[i] &= mask[i];
            }
        }

        /**
         * 反转掩码（用于计算广播地址）。
         */
        private static void invertMask(byte[] mask) {
            for (int i = 0; i < IPV6_BYTE_LENGTH; i++) {
                mask[i] = (byte) (~mask[i]);
            }
        }

        /**
         * 合并两个IP范围的结束地址。
         */
        private static byte[] mergeEnd(byte[] currentEnd, byte[] newEnd) {
            if (compareIp(currentEnd, newEnd) >= 0) {
                return currentEnd;
            } else {
                return newEnd;
            }
        }

        /**
         * 比较两个IPv6字节数组的大小。
         */
        private static int compareIp(byte[] ip1, byte[] ip2) {
            for (int i = 0; i < IPV6_BYTE_LENGTH; i++) {
                int diff = Byte.toUnsignedInt(ip1[i]) - Byte.toUnsignedInt(ip2[i]);
                if (diff != 0) return diff;
            }
            return 0;
        }

        /**
         * 判断两个IP范围是否重叠或相邻。
         */
        private static boolean isOverlappingOrAdjacent(IpRange ipRange, IpRange lastRange) {
            return compareIp(ipRange.getStart(), lastRange.getEnd()) <= 0;
        }

        /**
         * 获取起始地址的InetAddress对象。
         *
         * @return
         */
        public InetAddress getStartAddress() {
            try {
                return InetAddress.getByAddress(start);
            } catch (Exception e) {
                throw new IllegalArgumentException("非法的IP数据！");
            }
        }

        // ------------------------------ 私有工具方法 ------------------------------

        /**
         * 获取结束地址的InetAddress对象。
         *
         * @return
         */
        public InetAddress getEndAddress() {
            try {
                return InetAddress.getByAddress(end);
            } catch (Exception e) {
                throw new IllegalArgumentException("非法的IP数据！");
            }
        }

        /**
         * 获取起始地址的IPv6字节数组。
         *
         * @return
         */
        public byte[] getStart() {
            return start;
        }

        /**
         * 设置起始地址的IPv6字节数组。
         *
         * @param start
         */
        public void setStart(byte[] start) {
            this.start = start;
        }

        public byte[] getEnd() {
            return end;
        }


        // ------------------------------ 辅助方法 ------------------------------

        public void setEnd(byte[] end) {
            this.end = end;
        }

        /**
         * 将IP模式字符串转换为起始和结束地址。
         *
         * @param ipPattern IP模式字符串（如"192.168.1.0/24"或"2001:db8::/32"）
         */
        private void convert(String ipPattern) {
            try {
                String ip = ipPattern.trim();
                int maskBitCount = 128; // 默认为/128（单个地址）

                // 解析CIDR
                if (ip.contains("/")) {
                    String[] parts = ip.split("/");
                    ip = parts[0];
                    maskBitCount = Integer.parseInt(parts[1]);
                    // 验证掩码位数
                    if (maskBitCount < 0 || maskBitCount > 128) {
                        throw new IllegalArgumentException("无效的掩码位数: " + maskBitCount);
                    }
                }
                // 解析范围表达式
                else if (ip.contains("-")) {
                    String[] parts = ip.split("-");
                    if (parts.length != 2) {
                        throw new IllegalArgumentException("非法的IP范围格式: " + ip);
                    }
                    byte[] startBytes = inetAddressToBytes(parseInetAddress(parts[0]));
                    byte[] endBytes = inetAddressToBytes(parseInetAddress(parts[1]));

                    // 验证起始地址 <= 结束地址
                    if (compareIp(startBytes, endBytes) > 0) {
                        throw new IllegalArgumentException("起始地址必须小于等于结束地址: " + ip);
                    }

                    // 直接设置start和end
                    this.start = Arrays.copyOf(startBytes, IPV6_BYTE_LENGTH);
                    this.end = Arrays.copyOf(endBytes, IPV6_BYTE_LENGTH);
                    return;
                }
                // 解析通配符
                else if (ip.contains("*")) {
                    maskBitCount = 32 - (StringUtils.countMatches(ip, "*") * 8);
                    ip = ip.replace("*", "0");
                }

                // 转换为IPv6字节数组
                InetAddress inetAddress = parseInetAddress(ip);
                byte[] ipBytes = inetAddressToBytes(inetAddress);

                // 初始化起始地址
                System.arraycopy(ipBytes, 0, this.start, 0, IPV6_BYTE_LENGTH);

                // 处理IPv4地址的掩码转换
                if (inetAddress instanceof Inet4Address) {
                    // IPv4的默认掩码位数为32（当未指定CIDR时）
                    if (maskBitCount == 128) {
                        maskBitCount = 32;
                    }
                    maskBitCount += 96; // 转换为IPv6的掩码位数
                }

                // 验证最终掩码位数
                if (maskBitCount < 0 || maskBitCount > 128) {
                    throw new IllegalArgumentException("无效的掩码位数: " + maskBitCount);
                }

                // 计算网络地址和广播地址
                if (maskBitCount < 128) {
                    byte[] mask = createMask(maskBitCount);
                    // 计算网络地址
                    applyMask(this.start, mask);
                    // 反转掩码
                    invertMask(mask);
                    // 计算广播地址：网络地址 | 反转后的掩码
                    this.end = Arrays.copyOf(this.start, IPV6_BYTE_LENGTH);
                    for (int i = 0; i < IPV6_BYTE_LENGTH; i++) {
                        this.end[i] |= mask[i];
                    }
                } else {
                    this.end = Arrays.copyOf(this.start, IPV6_BYTE_LENGTH);
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("非法的IP格式: " + ipPattern, e);
            }
        }
    }
}
