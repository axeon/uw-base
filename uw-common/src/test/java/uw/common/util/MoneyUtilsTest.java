package uw.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MoneyUtilsTest {

    // ==================== 静态方法：加减 ====================

    @Test
    void testAdd() {
        Assertions.assertEquals(300, MoneyUtils.add(100, 200));
        Assertions.assertEquals(0, MoneyUtils.add(100, -100));
        Assertions.assertEquals(-300, MoneyUtils.add(-100, -200));
        Assertions.assertEquals(0, MoneyUtils.add(0, 0));
    }

    @Test
    void testAddOverflow() {
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.add(Long.MAX_VALUE, 1));
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.add(Long.MAX_VALUE, Long.MAX_VALUE));
    }

    @Test
    void testAddBoundary() {
        // 最大值加0不溢出
        Assertions.assertEquals(Long.MAX_VALUE, MoneyUtils.add(Long.MAX_VALUE, 0));
        // 最小值加0不溢出
        Assertions.assertEquals(Long.MIN_VALUE, MoneyUtils.add(Long.MIN_VALUE, 0));
        // 最大值 + 最小值 = -1
        Assertions.assertEquals(-1, MoneyUtils.add(Long.MAX_VALUE, Long.MIN_VALUE));
    }

    @Test
    void testSubtract() {
        Assertions.assertEquals(100, MoneyUtils.subtract(300, 200));
        Assertions.assertEquals(-100, MoneyUtils.subtract(100, 200));
        Assertions.assertEquals(0, MoneyUtils.subtract(100, 100));
    }

    @Test
    void testSubtractOverflow() {
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.subtract(Long.MIN_VALUE, 1));
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.subtract(0, Long.MIN_VALUE));
    }

    @Test
    void testSubtractBoundary() {
        Assertions.assertEquals(0, MoneyUtils.subtract(Long.MAX_VALUE, Long.MAX_VALUE));
        Assertions.assertEquals(0, MoneyUtils.subtract(Long.MIN_VALUE, Long.MIN_VALUE));
    }

    @Test
    void testSum() {
        Assertions.assertEquals(600, MoneyUtils.sum(100, 200, 300));
        Assertions.assertEquals(0, MoneyUtils.sum());
        Assertions.assertEquals(-50, MoneyUtils.sum(100, -150));
    }

    @Test
    void testSumSingleValue() {
        Assertions.assertEquals(0, MoneyUtils.sum(0));
        Assertions.assertEquals(Long.MAX_VALUE, MoneyUtils.sum(Long.MAX_VALUE));
        Assertions.assertEquals(Long.MIN_VALUE, MoneyUtils.sum(Long.MIN_VALUE));
    }

    @Test
    void testSumOverflow() {
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.sum(Long.MAX_VALUE, 1));
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.sum(Long.MIN_VALUE, -1));
    }

    // ==================== 静态方法：乘法 ====================

    @Test
    void testMultiply() {
        Assertions.assertEquals(30000, MoneyUtils.multiply(10000, 3));
        Assertions.assertEquals(0, MoneyUtils.multiply(10000, 0));
        Assertions.assertEquals(-10000, MoneyUtils.multiply(10000, -1));
    }

    @Test
    void testMultiplyNegativeBoth() {
        Assertions.assertEquals(10000, MoneyUtils.multiply(-10000, -1));
        Assertions.assertEquals(100, MoneyUtils.multiply(-10, -10));
    }

    @Test
    void testMultiplyOverflow() {
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.multiply(Long.MAX_VALUE / 2 + 1, 2));
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.multiply(Long.MIN_VALUE, -1));
    }

    @Test
    void testMultiplyByZero() {
        Assertions.assertEquals(0, MoneyUtils.multiply(0, 99999));
        Assertions.assertEquals(0, MoneyUtils.multiply(Long.MAX_VALUE, 0));
        Assertions.assertEquals(0, MoneyUtils.multiply(Long.MIN_VALUE, 0));
    }

    @Test
    void testMultiplyByOne() {
        Assertions.assertEquals(Long.MAX_VALUE, MoneyUtils.multiply(Long.MAX_VALUE, 1));
        Assertions.assertEquals(Long.MIN_VALUE, MoneyUtils.multiply(Long.MIN_VALUE, 1));
    }

    @Test
    void testMultiplyBps() {
        Assertions.assertEquals(850, MoneyUtils.multiplyBps(10000, 850));
        Assertions.assertEquals(0, MoneyUtils.multiplyBps(10000, 0));
        Assertions.assertEquals(10000, MoneyUtils.multiplyBps(10000, 10000));
        // 199分 × 850‱ = 16.915 → 四舍五入到17
        Assertions.assertEquals(17, MoneyUtils.multiplyBps(199, 850));
        // 100%以上倍率
        Assertions.assertEquals(15000, MoneyUtils.multiplyBps(10000, 15000));
    }

    @Test
    void testMultiplyBpsRoundingBoundary() {
        // 1分 × 5000‱ = 0.5 → 四舍五入到1
        Assertions.assertEquals(1, MoneyUtils.multiplyBps(1, 5000));
        // 1分 × 4999‱ = 0.4999 → 四舍五入到0
        Assertions.assertEquals(0, MoneyUtils.multiplyBps(1, 4999));
        // 1分 × 5001‱ = 0.5001 → 四舍五入到1
        Assertions.assertEquals(1, MoneyUtils.multiplyBps(1, 5001));
    }

    @Test
    void testMultiplyBpsNegative() {
        Assertions.assertEquals(-850, MoneyUtils.multiplyBps(-10000, 850));
        Assertions.assertEquals(850, MoneyUtils.multiplyBps(-10000, -850));
        Assertions.assertEquals(-850, MoneyUtils.multiplyBps(10000, -850));
    }

    @Test
    void testMultiplyBpsZeroAmount() {
        Assertions.assertEquals(0, MoneyUtils.multiplyBps(0, 9999));
    }

    @Test
    void testMultiplyBpsOverflow() {
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.multiplyBps(Long.MAX_VALUE, 2));
    }

    @Test
    void testMultiplyRatio() {
        Assertions.assertEquals(1300, MoneyUtils.multiplyRatio(10000, 13, 100));
        Assertions.assertEquals(3, MoneyUtils.multiplyRatio(10, 1, 3));
        Assertions.assertEquals(7, MoneyUtils.multiplyRatio(20, 1, 3));
    }

    @Test
    void testMultiplyRatioNegative() {
        Assertions.assertEquals(-1300, MoneyUtils.multiplyRatio(-10000, 13, 100));
        Assertions.assertEquals(-1300, MoneyUtils.multiplyRatio(10000, -13, 100));
        Assertions.assertEquals(1300, MoneyUtils.multiplyRatio(-10000, -13, 100));
    }

    @Test
    void testMultiplyRatioDivZero() {
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.multiplyRatio(100, 1, 0));
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.multiplyRatio(0, 1, 0));
    }

    @Test
    void testMultiplyRatioOverflow() {
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.multiplyRatio(Long.MAX_VALUE, Long.MAX_VALUE, 1));
    }

    @Test
    void testMultiplyRateDouble() {
        Assertions.assertEquals(147700, MoneyUtils.multiplyRate(1000000, 0.1477));
        Assertions.assertEquals(0, MoneyUtils.multiplyRate(100, 0.0));
        Assertions.assertEquals(100, MoneyUtils.multiplyRate(100, 1.0));
        Assertions.assertEquals(50, MoneyUtils.multiplyRate(100, 0.5));
    }

    @Test
    void testMultiplyRateDoubleNegative() {
        Assertions.assertEquals(-147700, MoneyUtils.multiplyRate(-1000000, 0.1477));
        Assertions.assertEquals(-147700, MoneyUtils.multiplyRate(1000000, -0.1477));
        Assertions.assertEquals(147700, MoneyUtils.multiplyRate(-1000000, -0.1477));
    }

    @Test
    void testMultiplyRateDoubleSmallAmount() {
        // 1分 × 0.1477 = 0.1477 → 四舍五入到0
        Assertions.assertEquals(0, MoneyUtils.multiplyRate(1, 0.1477));
        // 10分 × 0.1477 = 1.477 → 四舍五入到1
        Assertions.assertEquals(1, MoneyUtils.multiplyRate(10, 0.1477));
    }

    @Test
    void testMultiplyRateDoubleLargeRate() {
        // 倍率大于1
        Assertions.assertEquals(20000, MoneyUtils.multiplyRate(10000, 2.0));
        Assertions.assertEquals(5000, MoneyUtils.multiplyRate(10000, 0.5));
    }

    @Test
    void testMultiplyRateString() {
        Assertions.assertEquals(147700, MoneyUtils.multiplyRate(1000000, "0.1477"));
        Assertions.assertEquals(100, MoneyUtils.multiplyRate(100, "1.0"));
        Assertions.assertEquals(-147700, MoneyUtils.multiplyRate(-1000000, "0.1477"));
    }

    @Test
    void testMultiplyRateStringZeroRate() {
        Assertions.assertEquals(0, MoneyUtils.multiplyRate(100, "0"));
        Assertions.assertEquals(0, MoneyUtils.multiplyRate(100, "0.00"));
    }

    @Test
    void testMultiplyRateStringHighPrecision() {
        // 6位小数精度
        Assertions.assertEquals(1, MoneyUtils.multiplyRate(1000, "0.001"));
        Assertions.assertEquals(100, MoneyUtils.multiplyRate(100000, "0.001"));
    }

    @Test
    void testMultiplyRateZeroAmount() {
        Assertions.assertEquals(0, MoneyUtils.multiplyRate(0, 0.1477));
        Assertions.assertEquals(0, MoneyUtils.multiplyRate(0, "0.1477"));
    }

    // ==================== 静态方法：除法 ====================

    @Test
    void testDivideHalfUp() {
        Assertions.assertEquals(3, MoneyUtils.divideHalfUp(10, 3));
        Assertions.assertEquals(5, MoneyUtils.divideHalfUp(14, 3));
        Assertions.assertEquals(5, MoneyUtils.divideHalfUp(15, 3));
        Assertions.assertEquals(25, MoneyUtils.divideHalfUp(100, 4));
        Assertions.assertEquals(10, MoneyUtils.divideHalfUp(30, 3));
    }

    @Test
    void testDivideHalfUpRoundingBoundary() {
        // 恰好0.5向上进位
        Assertions.assertEquals(1, MoneyUtils.divideHalfUp(5, 10));
        // 恰好0.4向下
        Assertions.assertEquals(0, MoneyUtils.divideHalfUp(4, 10));
        // 恰好0.6向上
        Assertions.assertEquals(1, MoneyUtils.divideHalfUp(6, 10));
        // 1 / 2 = 0.5 → 向上到1
        Assertions.assertEquals(1, MoneyUtils.divideHalfUp(1, 2));
    }

    @Test
    void testDivideHalfUpNegative() {
        Assertions.assertEquals(-3, MoneyUtils.divideHalfUp(-10, 3));
        Assertions.assertEquals(-5, MoneyUtils.divideHalfUp(-14, 3));
        Assertions.assertEquals(-3, MoneyUtils.divideHalfUp(10, -3));
        Assertions.assertEquals(3, MoneyUtils.divideHalfUp(-10, -3));
    }

    @Test
    void testDivideHalfUpDivZero() {
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.divideHalfUp(100, 0));
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.divideHalfUp(0, 0));
    }

    @Test
    void testDivideHalfUpZeroDividend() {
        Assertions.assertEquals(0, MoneyUtils.divideHalfUp(0, 3));
        Assertions.assertEquals(0, MoneyUtils.divideHalfUp(0, -3));
    }

    @Test
    void testDivideHalfUpOneByOne() {
        Assertions.assertEquals(1, MoneyUtils.divideHalfUp(1, 1));
        Assertions.assertEquals(-1, MoneyUtils.divideHalfUp(-1, 1));
    }

    @Test
    void testDivideHalfUpLargeNumbers() {
        Assertions.assertEquals(Long.MAX_VALUE, MoneyUtils.divideHalfUp(Long.MAX_VALUE, 1));
        Assertions.assertEquals(Long.MIN_VALUE, MoneyUtils.divideHalfUp(Long.MIN_VALUE, 1));
    }

    @Test
    void testCeilDiv() {
        Assertions.assertEquals(4, MoneyUtils.ceilDiv(10, 3));
        Assertions.assertEquals(3, MoneyUtils.ceilDiv(9, 3));
        Assertions.assertEquals(1, MoneyUtils.ceilDiv(1, 3));
        Assertions.assertEquals(1, MoneyUtils.ceilDiv(3, 3));
    }

    @Test
    void testCeilDivNegative() {
        // 负数向上取整：-10 / 3 = -3.33 → 向上（靠近零）到 -3
        Assertions.assertEquals(-3, MoneyUtils.ceilDiv(-10, 3));
        // 负数/负数
        Assertions.assertEquals(4, MoneyUtils.ceilDiv(-10, -3));
        // 正数/负数
        Assertions.assertEquals(-3, MoneyUtils.ceilDiv(10, -3));
    }

    @Test
    void testCeilDivZeroDividend() {
        Assertions.assertEquals(0, MoneyUtils.ceilDiv(0, 3));
        Assertions.assertEquals(0, MoneyUtils.ceilDiv(0, -3));
    }

    @Test
    void testCeilDivDivZero() {
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.ceilDiv(100, 0));
    }

    @Test
    void testDivideRateDouble() {
        Assertions.assertEquals(1000000, MoneyUtils.divideRate(147700, 0.1477));
        Assertions.assertEquals(100, MoneyUtils.divideRate(100, 1.0));
    }

    @Test
    void testDivideRateDoubleNegative() {
        Assertions.assertEquals(-1000000, MoneyUtils.divideRate(-147700, 0.1477));
        Assertions.assertEquals(-1000000, MoneyUtils.divideRate(147700, -0.1477));
        Assertions.assertEquals(1000000, MoneyUtils.divideRate(-147700, -0.1477));
    }

    @Test
    void testDivideRateString() {
        Assertions.assertEquals(1000000, MoneyUtils.divideRate(147700, "0.1477"));
        Assertions.assertEquals(100, MoneyUtils.divideRate(100, "1.0"));
    }

    @Test
    void testDivideRateByZero() {
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.divideRate(100, 0.0));
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.divideRate(100, "0"));
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.divideRate(0, 0.0));
    }

    @Test
    void testDivideRateZeroAmount() {
        Assertions.assertEquals(0, MoneyUtils.divideRate(0, 0.1477));
        Assertions.assertEquals(0, MoneyUtils.divideRate(0, "0.1477"));
    }

    // ==================== 静态方法：分摊 ====================

    @Test
    void testAllocate() {
        long[] result = MoneyUtils.allocate(100, new long[]{40, 30, 20, 10});
        Assertions.assertEquals(4, result.length);
        Assertions.assertEquals(100, result[0] + result[1] + result[2] + result[3]);
    }

    @Test
    void testAllocateEqualWeights() {
        long[] result = MoneyUtils.allocate(100, new long[]{1, 1, 1});
        Assertions.assertEquals(100, result[0] + result[1] + result[2]);
    }

    @Test
    void testAllocateSingleWeight() {
        long[] result = MoneyUtils.allocate(999, new long[]{1});
        Assertions.assertEquals(1, result.length);
        Assertions.assertEquals(999, result[0]);
    }

    @Test
    void testAllocateZeroTotal() {
        long[] result = MoneyUtils.allocate(0, new long[]{1, 2, 3});
        Assertions.assertEquals(0, result[0] + result[1] + result[2]);
    }

    @Test
    void testAllocateNegativeTotal() {
        long[] result = MoneyUtils.allocate(-100, new long[]{1, 1});
        Assertions.assertEquals(-100, result[0] + result[1]);
    }

    @Test
    void testAllocateTwoWeights() {
        long[] result = MoneyUtils.allocate(1, new long[]{1, 1});
        Assertions.assertEquals(1, result[0] + result[1]);
    }

    @Test
    void testAllocateLargeWeights() {
        long[] result = MoneyUtils.allocate(10000, new long[]{1000000, 2000000, 3000000});
        Assertions.assertEquals(10000, result[0] + result[1] + result[2]);
    }

    @Test
    void testAllocateUnevenSplit() {
        // 1分分3份，尾差由最后一份兜底
        long[] result = MoneyUtils.allocate(1, new long[]{1, 1, 1});
        Assertions.assertEquals(1, result[0] + result[1] + result[2]);
    }

    @Test
    void testAllocateManyParts() {
        long[] weights = new long[100];
        for (int i = 0; i < 100; i++) {
            weights[i] = 1;
        }
        long[] result = MoneyUtils.allocate(100, weights);
        long sum = 0;
        for (long r : result) {
            sum += r;
        }
        Assertions.assertEquals(100, sum);
    }

    @Test
    void testAllocatePrimeTotal() {
        long total = 997;
        long[] weights = {3, 7, 2, 5, 11};
        long[] result = MoneyUtils.allocate(total, weights);
        long sum = 0;
        for (long r : result) {
            sum += r;
        }
        Assertions.assertEquals(total, sum);
    }

    @Test
    void testAllocateOneWeightZero() {
        // 权重中包含0
        long[] result = MoneyUtils.allocate(100, new long[]{0, 1, 1});
        Assertions.assertEquals(100, result[0] + result[1] + result[2]);
    }

    @Test
    void testAllocateInvalidWeights() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> MoneyUtils.allocate(100, null));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> MoneyUtils.allocate(100, new long[]{}));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> MoneyUtils.allocate(100, new long[]{-1, 2}));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> MoneyUtils.allocate(100, new long[]{0, 0}));
    }

    @Test
    void testAllocateOverflow() {
        // 权重求和溢出
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.allocate(100, new long[]{Long.MAX_VALUE, Long.MAX_VALUE}));
    }

    // ==================== 静态方法：元/分转换 ====================

    @Test
    void testToYuan() {
        Assertions.assertEquals("1.99", MoneyUtils.toYuan(199));
        Assertions.assertEquals("0.99", MoneyUtils.toYuan(99));
        Assertions.assertEquals("1.00", MoneyUtils.toYuan(100));
        Assertions.assertEquals("0.01", MoneyUtils.toYuan(1));
        Assertions.assertEquals("0.00", MoneyUtils.toYuan(0));
        Assertions.assertEquals("-1.99", MoneyUtils.toYuan(-199));
        Assertions.assertEquals("1000000.00", MoneyUtils.toYuan(100000000L));
    }

    @Test
    void testToYuanEdgeCases() {
        Assertions.assertEquals("0.09", MoneyUtils.toYuan(9));
        Assertions.assertEquals("0.10", MoneyUtils.toYuan(10));
        Assertions.assertEquals("10.00", MoneyUtils.toYuan(1000));
        Assertions.assertEquals("-0.01", MoneyUtils.toYuan(-1));
        Assertions.assertEquals("-0.99", MoneyUtils.toYuan(-99));
        Assertions.assertEquals("-0.10", MoneyUtils.toYuan(-10));
        Assertions.assertEquals("0.00", MoneyUtils.toYuan(0));
    }

    @Test
    void testToYuanOneCent() {
        Assertions.assertEquals("0.01", MoneyUtils.toYuan(1));
        Assertions.assertEquals("-0.01", MoneyUtils.toYuan(-1));
    }

    @Test
    void testToYuanLargeNumber() {
        long large = 10_000_000_000_000L; // 1000亿分 = 100亿元
        Assertions.assertEquals("100000000000.00", MoneyUtils.toYuan(large));
        Assertions.assertEquals("-100000000000.00", MoneyUtils.toYuan(-large));
    }

    @Test
    void testToYuanLongMaxMin() {
        String maxStr = MoneyUtils.toYuan(Long.MAX_VALUE);
        Assertions.assertTrue(maxStr.contains("."));
        Assertions.assertEquals(Long.MAX_VALUE, MoneyUtils.fromYuan(maxStr));
        // Long.MIN_VALUE 的绝对值超出 Long.MAX_VALUE，fromYuan 往返会溢出
        // 仅验证 toYuan 输出格式正确
        String minStr = MoneyUtils.toYuan(Long.MIN_VALUE);
        Assertions.assertTrue(minStr.startsWith("-"));
        Assertions.assertTrue(minStr.contains("."));
    }

    @Test
    void testFromYuan() {
        Assertions.assertEquals(199, MoneyUtils.fromYuan("1.99"));
        Assertions.assertEquals(99, MoneyUtils.fromYuan("0.99"));
        Assertions.assertEquals(100, MoneyUtils.fromYuan("1.00"));
        Assertions.assertEquals(1, MoneyUtils.fromYuan("0.01"));
        Assertions.assertEquals(0, MoneyUtils.fromYuan("0"));
        Assertions.assertEquals(0, MoneyUtils.fromYuan("0.00"));
        Assertions.assertEquals(500, MoneyUtils.fromYuan("5"));
        Assertions.assertEquals(-199, MoneyUtils.fromYuan("-1.99"));
    }

    @Test
    void testFromYuanOneDecimal() {
        Assertions.assertEquals(190, MoneyUtils.fromYuan("1.9"));
        Assertions.assertEquals(50, MoneyUtils.fromYuan("0.5"));
        Assertions.assertEquals(-190, MoneyUtils.fromYuan("-1.9"));
    }

    @Test
    void testFromYuanZeroVariants() {
        Assertions.assertEquals(0, MoneyUtils.fromYuan("0"));
        Assertions.assertEquals(0, MoneyUtils.fromYuan("0.0"));
        Assertions.assertEquals(0, MoneyUtils.fromYuan("0.00"));
    }

    @Test
    void testFromYuanNegative() {
        Assertions.assertEquals(-199, MoneyUtils.fromYuan("-1.99"));
        Assertions.assertEquals(-100, MoneyUtils.fromYuan("-1.00"));
        Assertions.assertEquals(-500, MoneyUtils.fromYuan("-5"));
        Assertions.assertEquals(-190, MoneyUtils.fromYuan("-1.9"));
    }

    @Test
    void testFromYuanLargeNumber() {
        Assertions.assertEquals(10000000000L, MoneyUtils.fromYuan("100000000.00"));
        Assertions.assertEquals(-10000000000L, MoneyUtils.fromYuan("-100000000.00"));
    }

    @Test
    void testFromYuanInvalid() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> MoneyUtils.fromYuan(null));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> MoneyUtils.fromYuan(""));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> MoneyUtils.fromYuan("1.999"));
        // 超过2位小数
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> MoneyUtils.fromYuan("1.123"));
    }

    @Test
    void testFromYuanInvalidNumber() {
        Assertions.assertThrows(NumberFormatException.class,
                () -> MoneyUtils.fromYuan("abc"));
        // "1.2.3" 有多个小数点，decPart = "2.3" 长度3 > 2 → IllegalArgumentException
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> MoneyUtils.fromYuan("1.2.3"));
    }

    @Test
    void testYuanRoundTrip() {
        long[] values = {0, 1, 9, 10, 99, 100, 199, 10000, 999999, -199, -1, -10, -99};
        for (long v : values) {
            Assertions.assertEquals(v, MoneyUtils.fromYuan(MoneyUtils.toYuan(v)),
                    "round trip failed for " + v);
        }
    }

    // ==================== 汇率场景端到端 ====================

    @Test
    void testExchangeRateRoundTrip() {
        long originalCents = 1000000;
        String rate = "0.1477";
        long usdCents = MoneyUtils.multiplyRate(originalCents, rate);
        long backToCny = MoneyUtils.divideRate(usdCents, rate);
        Assertions.assertTrue(Math.abs(originalCents - backToCny) <= 2,
                "round trip error too large: " + Math.abs(originalCents - backToCny));
    }

    @Test
    void testExchangeRateRoundTripSmallAmount() {
        // 小金额汇率往返
        long originalCents = 1; // 1分
        String rate = "6.7698";
        long converted = MoneyUtils.multiplyRate(originalCents, rate);
        long back = MoneyUtils.divideRate(converted, rate);
        Assertions.assertTrue(Math.abs(originalCents - back) <= 2,
                "small amount round trip error: " + Math.abs(originalCents - back));
    }

    @Test
    void testExchangeRateInverseConsistency() {
        long cny = 1000000;
        String rateStr = "0.1477";
        double rateDouble = 0.1477;
        Assertions.assertEquals(
                MoneyUtils.multiplyRate(cny, rateStr),
                MoneyUtils.multiplyRate(cny, rateDouble));
    }

    @Test
    void testExchangeRateMultipleRates() {
        long amount = 1000000; // 10000元
        // 多种汇率
        String[] rates = {"0.1", "0.5", "1.0", "2.0", "6.7698", "0.1477", "0.001", "123.456"};
        for (String rate : rates) {
            long converted = MoneyUtils.multiplyRate(amount, rate);
            long back = MoneyUtils.divideRate(converted, rate);
            Assertions.assertTrue(Math.abs(amount - back) <= 2,
                    "rate " + rate + " round trip error: " + Math.abs(amount - back));
        }
    }

    // ==================== Chain：创建与结算 ====================

    @Test
    void testChainOf() {
        Assertions.assertEquals(10000, MoneyUtils.of(10000).cent());
        Assertions.assertEquals(0, MoneyUtils.of(0).cent());
        Assertions.assertEquals(-1, MoneyUtils.of(-1).cent());
        Assertions.assertEquals(Long.MAX_VALUE, MoneyUtils.of(Long.MAX_VALUE).cent());
        Assertions.assertEquals(Long.MIN_VALUE, MoneyUtils.of(Long.MIN_VALUE).cent());
    }

    @Test
    void testChainOfYuan() {
        Assertions.assertEquals(199, MoneyUtils.ofYuan("1.99").cent());
        Assertions.assertEquals(10000, MoneyUtils.ofYuan("100.00").cent());
        Assertions.assertEquals(0, MoneyUtils.ofYuan("0").cent());
    }

    @Test
    void testChainCent() {
        Assertions.assertEquals(500, MoneyUtils.of(500).cent());
        Assertions.assertEquals(0, MoneyUtils.of(0).cent());
    }

    @Test
    void testChainYuan() {
        Assertions.assertEquals("5.00", MoneyUtils.of(500).yuan());
        Assertions.assertEquals("1.99", MoneyUtils.of(199).yuan());
        Assertions.assertEquals("0.00", MoneyUtils.of(0).yuan());
        Assertions.assertEquals("-1.00", MoneyUtils.of(-100).yuan());
    }

    // ==================== Chain：加减 ====================

    @Test
    void testChainAdd() {
        Assertions.assertEquals(300, MoneyUtils.of(100).add(200).cent());
        Assertions.assertEquals(0, MoneyUtils.of(100).add(-100).cent());
    }

    @Test
    void testChainAddChain() {
        MoneyUtils.Chain a = MoneyUtils.of(100);
        MoneyUtils.Chain b = MoneyUtils.of(200);
        Assertions.assertEquals(300, a.add(b).cent());
    }

    @Test
    void testChainAddZero() {
        Assertions.assertEquals(100, MoneyUtils.of(100).add(0).cent());
    }

    @Test
    void testChainAddNegative() {
        Assertions.assertEquals(0, MoneyUtils.of(100).add(-100).cent());
        Assertions.assertEquals(-50, MoneyUtils.of(100).add(-150).cent());
    }

    @Test
    void testChainSubtract() {
        Assertions.assertEquals(100, MoneyUtils.of(300).subtract(200).cent());
        Assertions.assertEquals(-100, MoneyUtils.of(100).subtract(200).cent());
    }

    @Test
    void testChainSubtractChain() {
        Assertions.assertEquals(200, MoneyUtils.of(300).subtract(MoneyUtils.of(100)).cent());
    }

    @Test
    void testChainSubtractZero() {
        Assertions.assertEquals(100, MoneyUtils.of(100).subtract(0).cent());
    }

    @Test
    void testChainOverflow() {
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.of(Long.MAX_VALUE).add(1));
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.of(Long.MIN_VALUE).subtract(1));
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.of(0).add(Long.MAX_VALUE).add(1));
    }

    @Test
    void testChainMultiplyOverflow() {
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.of(Long.MAX_VALUE / 2 + 1).multiply(2));
    }

    // ==================== Chain：乘法 ====================

    @Test
    void testChainMultiply() {
        Assertions.assertEquals(30000, MoneyUtils.of(10000).multiply(3).cent());
        Assertions.assertEquals(0, MoneyUtils.of(10000).multiply(0).cent());
    }

    @Test
    void testChainMultiplyByOne() {
        Assertions.assertEquals(10000, MoneyUtils.of(10000).multiply(1).cent());
    }

    @Test
    void testChainMultiplyNegative() {
        Assertions.assertEquals(-10000, MoneyUtils.of(10000).multiply(-1).cent());
    }

    @Test
    void testChainMultiplyBps() {
        Assertions.assertEquals(850, MoneyUtils.of(10000).multiplyBps(850).cent());
    }

    @Test
    void testChainMultiplyBpsFull() {
        Assertions.assertEquals(10000, MoneyUtils.of(10000).multiplyBps(10000).cent());
    }

    @Test
    void testChainMultiplyRatio() {
        Assertions.assertEquals(1300, MoneyUtils.of(10000).multiplyRatio(13, 100).cent());
    }

    @Test
    void testChainMultiplyRatioDivZero() {
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.of(100).multiplyRatio(1, 0));
    }

    @Test
    void testChainMultiplyRateDouble() {
        Assertions.assertEquals(147700, MoneyUtils.of(1000000).multiplyRate(0.1477).cent());
    }

    @Test
    void testChainMultiplyRateString() {
        Assertions.assertEquals(147700, MoneyUtils.of(1000000).multiplyRate("0.1477").cent());
    }

    @Test
    void testChainMultiplyRateNegative() {
        Assertions.assertEquals(-147700, MoneyUtils.of(1000000).multiplyRate("-0.1477").cent());
    }

    // ==================== Chain：除法 ====================

    @Test
    void testChainDivide() {
        Assertions.assertEquals(3, MoneyUtils.of(10).divide(3).cent());
        Assertions.assertEquals(5, MoneyUtils.of(14).divide(3).cent());
    }

    @Test
    void testChainDivideByOne() {
        Assertions.assertEquals(100, MoneyUtils.of(100).divide(1).cent());
    }

    @Test
    void testChainDivideByZero() {
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.of(100).divide(0));
    }

    @Test
    void testChainDivideNegative() {
        Assertions.assertEquals(-3, MoneyUtils.of(-10).divide(3).cent());
        Assertions.assertEquals(-3, MoneyUtils.of(10).divide(-3).cent());
    }

    @Test
    void testChainCeilDivide() {
        Assertions.assertEquals(4, MoneyUtils.of(10).ceilDivide(3).cent());
        Assertions.assertEquals(3, MoneyUtils.of(9).ceilDivide(3).cent());
    }

    @Test
    void testChainCeilDivideByZero() {
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.of(100).ceilDivide(0));
    }

    @Test
    void testChainCeilDivideNegative() {
        Assertions.assertEquals(-3, MoneyUtils.of(-10).ceilDivide(3).cent());
    }

    @Test
    void testChainDivideRateDouble() {
        Assertions.assertEquals(1000000, MoneyUtils.of(147700).divideRate(0.1477).cent());
    }

    @Test
    void testChainDivideRateString() {
        Assertions.assertEquals(1000000, MoneyUtils.of(147700).divideRate("0.1477").cent());
    }

    @Test
    void testChainDivideRateByZero() {
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.of(100).divideRate(0.0));
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.of(100).divideRate("0"));
    }

    // ==================== Chain：其他 ====================

    @Test
    void testChainNegate() {
        Assertions.assertEquals(-100, MoneyUtils.of(100).negate().cent());
        Assertions.assertEquals(100, MoneyUtils.of(-100).negate().cent());
    }

    @Test
    void testChainNegateZero() {
        Assertions.assertEquals(0, MoneyUtils.of(0).negate().cent());
    }

    @Test
    void testChainNegateOverflow() {
        // Long.MIN_VALUE 取反溢出（因为绝对值比MAX_VALUE大1）
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.of(Long.MIN_VALUE).negate());
    }

    @Test
    void testChainAbs() {
        Assertions.assertEquals(100, MoneyUtils.of(-100).abs().cent());
        Assertions.assertEquals(100, MoneyUtils.of(100).abs().cent());
        Assertions.assertEquals(0, MoneyUtils.of(0).abs().cent());
    }

    @Test
    void testChainAbsOverflow() {
        // Long.MIN_VALUE 取绝对值会溢出
        Assertions.assertThrows(ArithmeticException.class,
                () -> MoneyUtils.of(Long.MIN_VALUE).abs());
    }

    // ==================== Chain：判断 ====================

    @Test
    void testChainIsZero() {
        Assertions.assertTrue(MoneyUtils.of(0).isZero());
        Assertions.assertFalse(MoneyUtils.of(1).isZero());
        Assertions.assertFalse(MoneyUtils.of(-1).isZero());
    }

    @Test
    void testChainIsPositive() {
        Assertions.assertTrue(MoneyUtils.of(1).isPositive());
        Assertions.assertTrue(MoneyUtils.of(Long.MAX_VALUE).isPositive());
        Assertions.assertFalse(MoneyUtils.of(0).isPositive());
        Assertions.assertFalse(MoneyUtils.of(-1).isPositive());
    }

    @Test
    void testChainIsNegative() {
        Assertions.assertTrue(MoneyUtils.of(-1).isNegative());
        Assertions.assertTrue(MoneyUtils.of(Long.MIN_VALUE).isNegative());
        Assertions.assertFalse(MoneyUtils.of(0).isNegative());
        Assertions.assertFalse(MoneyUtils.of(1).isNegative());
    }

    @Test
    void testChainGt() {
        Assertions.assertTrue(MoneyUtils.of(100).gt(50));
        Assertions.assertFalse(MoneyUtils.of(50).gt(100));
        Assertions.assertFalse(MoneyUtils.of(100).gt(100));
        // 负数比较
        Assertions.assertTrue(MoneyUtils.of(0).gt(-1));
        Assertions.assertTrue(MoneyUtils.of(-1).gt(-2));
    }

    @Test
    void testChainGtChain() {
        Assertions.assertTrue(MoneyUtils.of(100).gt(MoneyUtils.of(50)));
        Assertions.assertFalse(MoneyUtils.of(50).gt(MoneyUtils.of(100)));
        Assertions.assertFalse(MoneyUtils.of(100).gt(MoneyUtils.of(100)));
    }

    @Test
    void testChainGte() {
        Assertions.assertTrue(MoneyUtils.of(100).gte(100));
        Assertions.assertTrue(MoneyUtils.of(100).gte(50));
        Assertions.assertFalse(MoneyUtils.of(50).gte(100));
    }

    @Test
    void testChainGteChain() {
        Assertions.assertTrue(MoneyUtils.of(100).gte(MoneyUtils.of(100)));
        Assertions.assertFalse(MoneyUtils.of(99).gte(MoneyUtils.of(100)));
    }

    @Test
    void testChainLt() {
        Assertions.assertTrue(MoneyUtils.of(50).lt(100));
        Assertions.assertFalse(MoneyUtils.of(100).lt(50));
        Assertions.assertFalse(MoneyUtils.of(100).lt(100));
    }

    @Test
    void testChainLtChain() {
        Assertions.assertTrue(MoneyUtils.of(50).lt(MoneyUtils.of(100)));
        Assertions.assertFalse(MoneyUtils.of(100).lt(MoneyUtils.of(100)));
    }

    @Test
    void testChainLte() {
        Assertions.assertTrue(MoneyUtils.of(100).lte(100));
        Assertions.assertTrue(MoneyUtils.of(50).lte(100));
        Assertions.assertFalse(MoneyUtils.of(100).lte(50));
    }

    @Test
    void testChainLteChain() {
        Assertions.assertTrue(MoneyUtils.of(100).lte(MoneyUtils.of(100)));
        Assertions.assertFalse(MoneyUtils.of(101).lte(MoneyUtils.of(100)));
    }

    // ==================== Chain：equals/hashCode/toString ====================

    @Test
    void testChainEquals() {
        Assertions.assertEquals(MoneyUtils.of(100), MoneyUtils.of(100));
        Assertions.assertNotEquals(MoneyUtils.of(100), MoneyUtils.of(200));
        Assertions.assertNotEquals(MoneyUtils.of(100), null);
        Assertions.assertNotEquals(MoneyUtils.of(100), "100");
    }

    @Test
    void testChainEqualsAfterOperations() {
        // 100 + 100 == 50 * 4
        Assertions.assertEquals(
                MoneyUtils.of(100).add(100),
                MoneyUtils.of(50).multiply(4));
    }

    @Test
    void testChainHashCode() {
        Assertions.assertEquals(MoneyUtils.of(100).hashCode(), MoneyUtils.of(100).hashCode());
        Assertions.assertNotEquals(MoneyUtils.of(100).hashCode(), MoneyUtils.of(200).hashCode());
    }

    @Test
    void testChainToString() {
        Assertions.assertEquals("1.99", MoneyUtils.of(199).toString());
        Assertions.assertEquals("0.00", MoneyUtils.of(0).toString());
        Assertions.assertEquals("-1.00", MoneyUtils.of(-100).toString());
    }

    // ==================== Chain：复杂链式场景 ====================

    @Test
    void testChainComplexScenario() {
        long result = MoneyUtils.of(10000)
                .multiply(3)
                .multiplyBps(8500)
                .add(500)
                .cent();
        Assertions.assertEquals(26000, result);
    }

    @Test
    void testChainDiscountScenario() {
        Assertions.assertEquals(16915, MoneyUtils.of(19900).multiplyBps(8500).cent());
        Assertions.assertEquals("169.15", MoneyUtils.of(19900).multiplyBps(8500).yuan());
    }

    @Test
    void testChainExchangeScenario() {
        long usd = MoneyUtils.of(1000000).multiplyRate("0.1477").cent();
        Assertions.assertEquals(147700, usd);
        long cny = MoneyUtils.of(usd).divideRate("0.1477").cent();
        Assertions.assertEquals(1000000, cny);
    }

    @Test
    void testChainRefundScenario() {
        long refund = MoneyUtils.of(-19900)
                .abs()
                .multiplyRatio(80, 100)
                .negate()
                .cent();
        Assertions.assertEquals(-15920, refund);
    }

    @Test
    void testChainTaxCalculation() {
        // 100元 + 13% 税
        long total = MoneyUtils.of(10000)
                .multiplyRatio(113, 100)
                .cent();
        Assertions.assertEquals(11300, total);
    }

    @Test
    void testChainMultiStepDiscount() {
        // 100元先打9折，再打8折（折上折）
        long result = MoneyUtils.of(10000)
                .multiplyRatio(9, 10)
                .multiplyRatio(8, 10)
                .cent();
        Assertions.assertEquals(7200, result); // 100 * 0.9 * 0.8 = 72元
    }

    @Test
    void testChainSplitAndSum() {
        // 模拟AA制：999分3人均分
        long each = MoneyUtils.of(999).divide(3).cent();
        // 每人支付金额 * 3 应该 <= 总额（舍入损失）
        Assertions.assertTrue(each * 3 <= 999);
    }

    @Test
    void testChainZeroStart() {
        // 从0开始计算
        long result = MoneyUtils.of(0)
                .add(100)
                .subtract(50)
                .multiply(2)
                .cent();
        Assertions.assertEquals(100, result);
    }

    @Test
    void testChainNegativeStart() {
        // 从负数开始
        long result = MoneyUtils.of(-100)
                .abs()
                .add(50)
                .cent();
        Assertions.assertEquals(150, result);
    }

    // ==================== 综合边界 ====================

    @Test
    void testLongMaxValueOperations() {
        // MAX_VALUE可以安全乘1
        Assertions.assertEquals(Long.MAX_VALUE, MoneyUtils.multiply(Long.MAX_VALUE, 1));
        // MAX_VALUE除以1不变
        Assertions.assertEquals(Long.MAX_VALUE, MoneyUtils.divideHalfUp(Long.MAX_VALUE, 1));
        // MAX_VALUE加Long.MIN_VALUE等于-1
        Assertions.assertEquals(-1, MoneyUtils.add(Long.MAX_VALUE, Long.MIN_VALUE));
    }

    @Test
    void testLongMinValueOperations() {
        Assertions.assertEquals(Long.MIN_VALUE, MoneyUtils.multiply(Long.MIN_VALUE, 1));
        Assertions.assertEquals(Long.MIN_VALUE, MoneyUtils.divideHalfUp(Long.MIN_VALUE, 1));
    }

    @Test
    void testDivideHalfUpExhaustiveBoundary() {
        // 验证 0~9 除以 10 的四舍五入
        Assertions.assertEquals(0, MoneyUtils.divideHalfUp(0, 10));
        Assertions.assertEquals(0, MoneyUtils.divideHalfUp(1, 10));
        Assertions.assertEquals(0, MoneyUtils.divideHalfUp(4, 10));
        Assertions.assertEquals(1, MoneyUtils.divideHalfUp(5, 10)); // 0.5 → 1
        Assertions.assertEquals(1, MoneyUtils.divideHalfUp(6, 10));
        Assertions.assertEquals(1, MoneyUtils.divideHalfUp(9, 10));
        Assertions.assertEquals(1, MoneyUtils.divideHalfUp(10, 10));
    }

    @Test
    void testCeilDivExhaustive() {
        // 向上取整边界
        Assertions.assertEquals(1, MoneyUtils.ceilDiv(1, 10));
        Assertions.assertEquals(1, MoneyUtils.ceilDiv(10, 10));
        Assertions.assertEquals(2, MoneyUtils.ceilDiv(11, 10));
        Assertions.assertEquals(1, MoneyUtils.ceilDiv(1, 1));
    }

    @Test
    void testMultiplyRateRoundingBoundary() {
        // 1分 × 0.005 = 0.005 → 四舍五入到0
        Assertions.assertEquals(0, MoneyUtils.multiplyRate(1, 0.005));
        // 1分 × 0.015 = 0.015 → 四舍五入到0
        Assertions.assertEquals(0, MoneyUtils.multiplyRate(1, 0.015));
        // 100分 × 0.005 = 0.5 → 四舍五入到1
        Assertions.assertEquals(1, MoneyUtils.multiplyRate(100, 0.005));
    }

    @Test
    void testDivideRateRoundingBoundary() {
        // 5分 / 0.01 = 500
        Assertions.assertEquals(500, MoneyUtils.divideRate(5, 0.01));
        // 1分 / 3.0 = 0.33...
        Assertions.assertEquals(0, MoneyUtils.divideRate(1, 3.0));
        // 3分 / 3.0 = 1
        Assertions.assertEquals(1, MoneyUtils.divideRate(3, 3.0));
    }
}
