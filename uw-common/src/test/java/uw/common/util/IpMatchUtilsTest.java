package uw.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

public class IpMatchUtilsTest {

    // ============= IPv4 测试用例 =============

    @Test
    void testIpv4Cidr() {
        // 测试IPv4 CIDR
        List<IpMatchUtils.IpRange> ranges = IpMatchUtils.sortList(new String[]{"192.168.1.0/24"});
        IpMatchUtils.IpRange range = ranges.get(0);
        assertIpRange(range, "192.168.1.0", "192.168.1.255");
    }

    @Test
    void testIpv4Wildcard() {
        // 测试IPv4通配符
        List<IpMatchUtils.IpRange> ranges = IpMatchUtils.sortList(new String[]{"192.168.*.*"});
        IpMatchUtils.IpRange range = ranges.get(0);
        assertIpRange(range, "192.168.0.0", "192.168.255.255");
    }

    @Test
    void testIpv4Range() {
        // 测试IPv4范围表达式
        List<IpMatchUtils.IpRange> ranges = IpMatchUtils.sortList(new String[]{"192.168.1.5-192.168.1.10"});
        IpMatchUtils.IpRange range = ranges.get(0);
        assertIpRange(range, "192.168.1.5", "192.168.1.10");
    }

    @Test
    void testIpv4Single() {
        // 测试单个IPv4地址
        List<IpMatchUtils.IpRange> ranges = IpMatchUtils.sortList(new String[]{"192.168.1.1"});
        IpMatchUtils.IpRange range = ranges.get(0);
        assertIpRange(range, "192.168.1.1", "192.168.1.1");
    }

    // ============= IPv6 测试用例 =============

    @Test
    void testIpv6Cidr() {
        // 测试IPv6 CIDR
        List<IpMatchUtils.IpRange> ranges = IpMatchUtils.sortList(new String[]{"2001:db8::/32"});
        IpMatchUtils.IpRange range = ranges.get(0);
        assertIpRange(range, "2001:db8::", "2001:db8:ffff:ffff:ffff:ffff:ffff:ffff");
    }

    @Test
    void testIpv6Range() {
        // 测试IPv6范围表达式
        List<IpMatchUtils.IpRange> ranges = IpMatchUtils.sortList(new String[]{"2001:db8::1-2001:db8::10"});
        IpMatchUtils.IpRange range = ranges.get(0);
        assertIpRange(range, "2001:db8::1", "2001:db8::10");
    }

    @Test
    void testIpv6Single() {
        // 测试单个IPv6地址
        List<IpMatchUtils.IpRange> ranges = IpMatchUtils.sortList(new String[]{"2001:0db8:85a3::8a2e:0370:7334"});
        IpMatchUtils.IpRange range = ranges.get(0);
        assertIpRange(range, "2001:0db8:85a3::8a2e:0370:7334", "2001:0db8:85a3::8a2e:0370:7334");
    }

    // ============= 合并测试 =============

    @Test
    void testMergeIpv4Ranges() {
        // 测试IPv4范围合并
        List<IpMatchUtils.IpRange> ranges = IpMatchUtils.sortList(Arrays.asList(
                new IpMatchUtils.IpRange("192.168.1.0/24"),
                new IpMatchUtils.IpRange("192.168.1.200-192.168.1.250")
        ));
        IpMatchUtils.IpRange merged = ranges.get(0);
        assertIpRange(merged, "192.168.1.0", "192.168.1.255");
    }

    @Test
    void testMergeIpv6Ranges() {
        List<IpMatchUtils.IpRange> ranges = IpMatchUtils.sortList(Arrays.asList(
                new IpMatchUtils.IpRange("2001:db8::/48"),
                new IpMatchUtils.IpRange("2001:db8::1000-2001:db8::2000")
        ));
        IpMatchUtils.IpRange merged = ranges.get(0);
        assertIpRange(merged, "2001:db8::", "2001:db8::ffff:ffff:ffff:ffff:ffff");
    }

    // ============= 异常测试 =============

    @Test
    void testInvalidIpv4Format() {
        // 测试无效IPv4格式
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            IpMatchUtils.sortList(new String[]{"invalid.ip.address"});
        });
    }

    @Test
    void testInvalidIpv6Format() {
        // 测试无效IPv6格式
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            IpMatchUtils.sortList(new String[]{"2001:db8::/129"});
        });
    }

    @Test
    void testInvalidRange() {
        // 测试无效的范围表达式（起始>结束）
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            IpMatchUtils.sortList(new String[]{"192.168.1.10-192.168.1.5"});
        });
    }

    // ============= 匹配测试 =============

    @Test
    void testIpv4Match() {
        List<IpMatchUtils.IpRange> ranges = IpMatchUtils.sortList(new String[]{"192.168.1.0/24"});
        Assertions.assertTrue(IpMatchUtils.matches(ranges, "192.168.1.123"));
        Assertions.assertFalse(IpMatchUtils.matches(ranges, "192.168.2.1"));
    }

    @Test
    void testIpv6Match() {
        List<IpMatchUtils.IpRange> ranges = IpMatchUtils.sortList(new String[]{"2001:db8::/32"});
        Assertions.assertTrue(IpMatchUtils.matches(ranges, "2001:db8::1"));
        Assertions.assertFalse(IpMatchUtils.matches(ranges, "2002::1"));
    }

    @Test
    void testIpv6Local(){
        String ipWhiteList = "127.0.0.1,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16,::1/128,fe80::/10,FC00::/7";
        List<IpMatchUtils.IpRange> ranges = IpMatchUtils.sortList(ipWhiteList.split(","));
        Assertions.assertTrue(IpMatchUtils.matches(ranges, "0:0:0:0:0:0:0:1"));
    }

    // ============= 辅助方法 =============

    private void assertIpRange(IpMatchUtils.IpRange range, String startStr, String endStr) {
        try {
            byte[] startBytes = IpMatchUtils.inetAddressToBytes(InetAddress.getByName(startStr));
            byte[] endBytes = IpMatchUtils.inetAddressToBytes(InetAddress.getByName(endStr));
            Assertions.assertArrayEquals(startBytes, range.getStart());
            Assertions.assertArrayEquals(endBytes, range.getEnd());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
