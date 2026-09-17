package uw.common.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HmacUtils 单测：多段签名与拼接单段的逐字节等价性（分段签名不改变签名值是
 * OpsAgentApi 大 payload 分段签名正确性的前提）。
 */
class HmacUtilsTest {

    private static final String SECRET = "test-secret";

    @Test
    void multiPartSignEqualsConcatenated() {
        //任意切分方式结果一致。
        String message = "host-hash-1:" + "x".repeat(100_000) + ":" + 1700000000000L;
        String single = HmacUtils.sign(message, SECRET);
        String splitMiddle = HmacUtils.sign(new byte[][]{
                "host-hash-1:".getBytes(StandardCharsets.UTF_8),
                "x".repeat(100_000).getBytes(StandardCharsets.UTF_8),
                (":" + 1700000000000L).getBytes(StandardCharsets.UTF_8)}, SECRET);
        String splitTiny = HmacUtils.sign(new byte[][]{
                "h".getBytes(StandardCharsets.UTF_8),
                "ost-hash-1:".getBytes(StandardCharsets.UTF_8),
                ("x".repeat(37_001)).getBytes(StandardCharsets.UTF_8),
                ("x".repeat(62_999) + ":" + 1700000000000L).getBytes(StandardCharsets.UTF_8)}, SECRET);
        assertEquals(single, splitMiddle);
        assertEquals(single, splitTiny);
    }

    @Test
    void nullOrEmptyPartSkipped() {
        String message = "abc:123";
        String withEmpty = HmacUtils.sign(new byte[][]{
                "abc".getBytes(StandardCharsets.UTF_8),
                new byte[0],
                null,
                ":123".getBytes(StandardCharsets.UTF_8)}, SECRET);
        assertEquals(HmacUtils.sign(message, SECRET), withEmpty);
    }

    @Test
    void verifyMultiPartSymmetric() {
        byte[][] parts = new byte[][]{
                "hash-1:".getBytes(StandardCharsets.UTF_8),
                "{\"a\":1}".getBytes(StandardCharsets.UTF_8),
                ":1700000000000".getBytes(StandardCharsets.UTF_8)};
        String signature = HmacUtils.sign(parts, SECRET);
        assertTrue(HmacUtils.verify(parts, SECRET, signature));
        //与单段拼接互验：分段签名可被单段验签方（Center）接受。
        String concatenated = "hash-1:" + "{\"a\":1}" + ":" + 1700000000000L;
        assertTrue(HmacUtils.verify(concatenated, SECRET, signature));
        assertFalse(HmacUtils.verify(concatenated, SECRET, "deadbeef"));
        assertFalse(HmacUtils.verify(parts, SECRET, null));
        assertFalse(HmacUtils.verify(parts, SECRET, ""));
    }
}
