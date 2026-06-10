package uw.common.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * 货币计算工具类。
 * <p>
 * 所有金额以分为单位存储在long中，加减乘除整数运算避免浮点误差。
 * 汇率等非整数倍率场景使用BigDecimal保证精度，结果四舍五入到分。
 * 溢出或除零时抛出ArithmeticException。
 * <p>
 * 提供两套API：
 * <ul>
 *   <li>静态方法 —— 单次运算，一行搞定</li>
 *   <li>{@link Chain} 链式调用 —— 多步连续运算</li>
 * </ul>
 * <p>
 * 链式调用示例：
 * <pre>
 * long fee = MoneyUtils.of(10000)       // 100.00元
 *         .multiply(3)                  // × 3件
 *         .multiplyRate("0.85")         // 85折
 *         .add(500)                     // 加5元手续费
 *         .cent();                      // 25550（255.50元）
 *
 * String display = MoneyUtils.of(19900)
 *         .multiplyBps(850)             // 85折
 *         .yuan();                      // "169.15"
 * </pre>
 */
public final class MoneyUtils {

    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private MoneyUtils() {
    }

    // ==================== 链式入口 ====================

    /**
     * 以分为单位创建链式计算。
     *
     * @param cents 金额（分）
     * @return Chain实例
     */
    public static Chain of(long cents) {
        return new Chain(cents);
    }

    /**
     * 以元为单位创建链式计算。
     *
     * @param yuan 金额字符串（元），如 "1.99"
     * @return Chain实例
     */
    public static Chain ofYuan(String yuan) {
        return new Chain(fromYuan(yuan));
    }

    // ==================== 加减 ====================

    /**
     * 安全相加，溢出时抛出ArithmeticException。
     *
     * @param a 加数（分）
     * @param b 加数（分）
     * @return a + b（分）
     */
    public static long add(long a, long b) {
        return Math.addExact(a, b);
    }

    /**
     * 安全相减，溢出时抛出ArithmeticException。
     *
     * @param a 被减数（分）
     * @param b 减数（分）
     * @return a - b（分）
     */
    public static long subtract(long a, long b) {
        return Math.subtractExact(a, b);
    }

    /**
     * 多值求和，溢出时抛出ArithmeticException。
     *
     * @param values 待求和的金额数组（分），不可为null
     * @return 所有值之和（分）
     */
    public static long sum(long... values) {
        long total = 0;
        for (long v : values) {
            total = Math.addExact(total, v);
        }
        return total;
    }

    // ==================== 乘法 ====================

    /**
     * 乘以整数倍数，溢出时抛出ArithmeticException。
     *
     * @param amount 金额（分）
     * @param factor 整数倍数，如数量
     * @return amount × factor（分）
     */
    public static long multiply(long amount, long factor) {
        return Math.multiplyExact(amount, factor);
    }

    /**
     * 乘以百分比（万分之），四舍五入到分。
     *
     * @param amount  金额（分）
     * @param rateBps 比率，单位为万分之（‱）。如850表示8.5%
     * @return amount × rateBps / 10000，四舍五入（分）
     */
    public static long multiplyBps(long amount, long rateBps) {
        long product = Math.multiplyExact(amount, rateBps);
        return divideHalfUp(product, 10000L);
    }

    /**
     * 乘以比率（分子/分母），四舍五入到分。
     *
     * @param amount      金额（分）
     * @param numerator   比率分子，如税率13
     * @param denominator 比率分母，如100。不可为0
     * @return amount × numerator / denominator，四舍五入（分）
     */
    public static long multiplyRatio(long amount, long numerator, long denominator) {
        if (denominator == 0) {
            throw new ArithmeticException("denominator must not be zero");
        }
        long product = Math.multiplyExact(amount, numerator);
        return divideHalfUp(product, denominator);
    }

    /**
     * 乘以double倍率，四舍五入到分。
     * <p>
     * 适用于汇率等非整数倍率。内部使用BigDecimal避免浮点误差。
     * 例：multiplyRate(1000000, 0.1477) = 147700（10000元 × 0.1477 = 1477.00元）
     *
     * @param amount 金额（分）
     * @param rate   倍率，如汇率0.1477。建议不超过6位小数
     * @return amount × rate，四舍五入（分）
     */
    public static long multiplyRate(long amount, double rate) {
        return BigDecimal.valueOf(amount)
                .multiply(BigDecimal.valueOf(rate), MathContext.DECIMAL64)
                .setScale(0, ROUNDING)
                .longValueExact();
    }

    /**
     * 乘以String倍率，四舍五入到分。
     * <p>
     * 适用于汇率等非整数倍率。String入参避免double构造时的精度丢失。
     * 例：multiplyRate(1000000, "0.1477") = 147700（10000元 × 0.1477 = 1477.00元）
     *
     * @param amount 金额（分）
     * @param rate   倍率字符串，如"0.1477"。不可为null
     * @return amount × rate，四舍五入（分）
     */
    public static long multiplyRate(long amount, String rate) {
        return BigDecimal.valueOf(amount)
                .multiply(new BigDecimal(rate), MathContext.DECIMAL64)
                .setScale(0, ROUNDING)
                .longValueExact();
    }

    // ==================== 除法 ====================

    /**
     * 四舍五入除法。
     *
     * @param dividend 被除数（分）
     * @param divisor  除数，不可为0
     * @return dividend / divisor，四舍五入
     */
    public static long divideHalfUp(long dividend, long divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("divisor must not be zero");
        }
        long half = Math.abs(divisor) / 2;
        if (dividend >= 0) {
            return (dividend + half) / divisor;
        } else {
            return (dividend - half) / divisor;
        }
    }

    /**
     * 向上取整除法（天花板除法）。
     * <p>
     * 适用于分笔拆单、向上取整手续费等场景。
     *
     * @param dividend 被除数（分）
     * @param divisor  除数，不可为0
     * @return dividend / divisor，向上取整
     */
    public static long ceilDiv(long dividend, long divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("divisor must not be zero");
        }
        // 正数向上取整：加 (divisor - 1) 再整除
        if (dividend >= 0 && divisor > 0) {
            return (dividend + divisor - 1) / divisor;
        }
        // 负/负向上取整：同正数逻辑，加 (|divisor| - 1)
        if (dividend < 0 && divisor < 0) {
            return (dividend + divisor - 1) / divisor;
        }
        // 异号：Java截断除法向零取整，恰好就是向上取整
        return dividend / divisor;
    }

    /**
     * 除以double倍率，四舍五入到分。
     * <p>
     * 适用于汇率逆向换算等场景。内部使用BigDecimal避免浮点误差。
     * 例：divideRate(147700, 0.1477) = 1000000（1477.00 / 0.1477 = 10000.00）
     *
     * @param amount 金额（分）
     * @param rate   除数倍率，如汇率0.1477。不可为0
     * @return amount / rate，四舍五入（分）
     */
    public static long divideRate(long amount, double rate) {
        return BigDecimal.valueOf(amount)
                .divide(BigDecimal.valueOf(rate), 0, ROUNDING)
                .longValueExact();
    }

    /**
     * 除以String倍率，四舍五入到分。
     * <p>
     * 适用于汇率逆向换算等场景。String入参避免double构造时的精度丢失。
     * 例：divideRate(147700, "0.1477") = 1000000（1477.00 / 0.1477 = 10000.00）
     *
     * @param amount 金额（分）
     * @param rate   除数字符串，如"0.1477"。不可为null或"0"
     * @return amount / rate，四舍五入（分）
     */
    public static long divideRate(long amount, String rate) {
        return BigDecimal.valueOf(amount)
                .divide(new BigDecimal(rate), 0, ROUNDING)
                .longValueExact();
    }

    // ==================== 分摊 ====================

    /**
     * 按比例分摊总金额到多份，最后一笔兜底，保证合计等于total。
     * <p>
     * 解决分笔舍入导致的"尾差"问题。
     * 例：allocate(100, {40, 30, 20, 10}) = {40, 30, 20, 10}
     *
     * @param total   待分摊总额（分）
     * @param weights 各份权重，不可为null或空数组，不可有负数
     * @return 各份金额数组（分），长度与weights相同，合计等于total
     */
    public static long[] allocate(long total, long[] weights) {
        if (weights == null || weights.length == 0) {
            throw new IllegalArgumentException("weights must not be empty");
        }
        long[] results = new long[weights.length];
        if (weights.length == 1) {
            results[0] = total;
            return results;
        }
        long weightSum = 0;
        for (long w : weights) {
            if (w < 0) {
                throw new IllegalArgumentException("weight must not be negative");
            }
            weightSum = Math.addExact(weightSum, w);
        }
        if (weightSum == 0) {
            throw new IllegalArgumentException("total weight must not be zero");
        }
        long allocated = 0;
        for (int i = 0; i < weights.length - 1; i++) {
            long product = Math.multiplyExact(total, weights[i]);
            results[i] = divideHalfUp(product, weightSum);
            allocated = Math.addExact(allocated, results[i]);
        }
        results[weights.length - 1] = Math.subtractExact(total, allocated);
        return results;
    }

    // ==================== 元/分转换 ====================

    /**
     * 分 → 元（字符串，保留2位小数）。
     *
     * @param cents 金额（分）
     * @return 元字符串，如 toYuan(199) = "1.99"
     */
    public static String toYuan(long cents) {
        boolean negative = cents < 0;
        long yuan = Math.abs(cents / 100);
        long fen = Math.abs(cents % 100);
        String str = yuan + "." + (fen < 10 ? "0" + fen : String.valueOf(fen));
        return negative ? "-" + str : str;
    }

    /**
     * 元 → 分。支持整数("5")、一位小数("1.9"→190)、两位小数("1.99"→199)。
     *
     * @param yuan 金额字符串（元），不可为null或空。不支持超过两位小数
     * @return 金额（分），如 fromYuan("1.99") = 199
     */
    public static long fromYuan(String yuan) {
        if (yuan == null || yuan.isEmpty()) {
            throw new IllegalArgumentException("yuan must not be empty");
        }
        boolean negative = yuan.startsWith("-");
        String num = negative ? yuan.substring(1) : yuan;
        int dot = num.indexOf('.');
        if (dot < 0) {
            long v = Long.parseLong(num);
            long result = Math.multiplyExact(v, 100);
            return negative ? -result : result;
        }
        String intPart = num.substring(0, dot);
        String decPart = num.substring(dot + 1);
        if (decPart.length() > 2) {
            throw new IllegalArgumentException("yuan scale must not exceed 2: " + yuan);
        }
        long intVal = intPart.isEmpty() ? 0 : Long.parseLong(intPart);
        long decVal = Long.parseLong(decPart);
        if (decPart.length() == 1) {
            decVal *= 10;
        }
        long result = Math.addExact(Math.multiplyExact(intVal, 100), decVal);
        return negative ? -result : result;
    }

    // ==================== 链式计算器 ====================

    /**
     * 链式货币计算器，支持连续运算后以 {@link #cent()} 或 {@link #yuan()} 结算。
     * <p>
     * 所有链式方法修改并返回自身，可连续调用。
     * 使用 {@link #cent()} 获取结果（分），或 {@link #yuan()} 获取元字符串。
     */
    public static final class Chain {

        private long value;

        private Chain(long cents) {
            this.value = cents;
        }

        // ---------- 加减 ----------

        /**
         * 加法。
         *
         * @param amount 加数（分）
         * @return this
         */
        public Chain add(long amount) {
            value = Math.addExact(value, amount);
            return this;
        }

        /**
         * 加法。
         *
         * @param other 另一个Chain实例
         * @return this
         */
        public Chain add(Chain other) {
            return add(other.value);
        }

        /**
         * 减法。
         *
         * @param amount 减数（分）
         * @return this
         */
        public Chain subtract(long amount) {
            value = Math.subtractExact(value, amount);
            return this;
        }

        /**
         * 减法。
         *
         * @param other 另一个Chain实例
         * @return this
         */
        public Chain subtract(Chain other) {
            return subtract(other.value);
        }

        // ---------- 乘法 ----------

        /**
         * 乘以整数倍数。
         *
         * @param factor 倍数，如数量
         * @return this
         */
        public Chain multiply(long factor) {
            value = Math.multiplyExact(value, factor);
            return this;
        }

        /**
         * 乘以百分比（万分之），四舍五入。
         *
         * @param rateBps 比率（‱），如850表示8.5%
         * @return this
         */
        public Chain multiplyBps(long rateBps) {
            value = MoneyUtils.multiplyBps(value, rateBps);
            return this;
        }

        /**
         * 乘以比率（分子/分母），四舍五入。
         *
         * @param numerator   分子
         * @param denominator 分母，不可为0
         * @return this
         */
        public Chain multiplyRatio(long numerator, long denominator) {
            value = MoneyUtils.multiplyRatio(value, numerator, denominator);
            return this;
        }

        /**
         * 乘以double倍率，四舍五入。适用于汇率等场景。
         *
         * @param rate 倍率，如0.1477
         * @return this
         */
        public Chain multiplyRate(double rate) {
            value = MoneyUtils.multiplyRate(value, rate);
            return this;
        }

        /**
         * 乘以String倍率，四舍五入。适用于汇率等场景。
         *
         * @param rate 倍率字符串，如"0.1477"
         * @return this
         */
        public Chain multiplyRate(String rate) {
            value = MoneyUtils.multiplyRate(value, rate);
            return this;
        }

        // ---------- 除法 ----------

        /**
         * 除以整数，四舍五入。
         *
         * @param divisor 除数，不可为0
         * @return this
         */
        public Chain divide(long divisor) {
            value = MoneyUtils.divideHalfUp(value, divisor);
            return this;
        }

        /**
         * 除以整数，向上取整。
         *
         * @param divisor 除数，不可为0
         * @return this
         */
        public Chain ceilDivide(long divisor) {
            value = MoneyUtils.ceilDiv(value, divisor);
            return this;
        }

        /**
         * 除以double倍率，四舍五入。适用于汇率逆向换算。
         *
         * @param rate 除数倍率，如0.1477。不可为0
         * @return this
         */
        public Chain divideRate(double rate) {
            value = MoneyUtils.divideRate(value, rate);
            return this;
        }

        /**
         * 除以String倍率，四舍五入。适用于汇率逆向换算。
         *
         * @param rate 除数字符串，如"0.1477"。不可为null或"0"
         * @return this
         */
        public Chain divideRate(String rate) {
            value = MoneyUtils.divideRate(value, rate);
            return this;
        }

        // ---------- 其他 ----------

        /**
         * 取反（正变负，负变正）。
         *
         * @return this
         */
        public Chain negate() {
            value = Math.negateExact(value);
            return this;
        }

        /**
         * 取绝对值。
         *
         * @return this
         */
        public Chain abs() {
            if (value < 0) {
                value = Math.negateExact(value);
            }
            return this;
        }

        // ---------- 结算 ----------

        /**
         * 结算为分。
         *
         * @return 金额（分）
         */
        public long cent() {
            return value;
        }

        /**
         * 结算为元字符串，保留2位小数。
         *
         * @return 如 "1.99"
         */
        public String yuan() {
            return MoneyUtils.toYuan(value);
        }

        // ---------- 判断 ----------

        /**
         * 是否为零。
         */
        public boolean isZero() {
            return value == 0;
        }

        /**
         * 是否为正。
         */
        public boolean isPositive() {
            return value > 0;
        }

        /**
         * 是否为负。
         */
        public boolean isNegative() {
            return value < 0;
        }

        /**
         * 是否大于。
         *
         * @param amount 比较值（分）
         */
        public boolean gt(long amount) {
            return value > amount;
        }

        /**
         * 是否大于。
         *
         * @param other 另一个Chain实例
         */
        public boolean gt(Chain other) {
            return value > other.value;
        }

        /**
         * 是否大于等于。
         *
         * @param amount 比较值（分）
         */
        public boolean gte(long amount) {
            return value >= amount;
        }

        /**
         * 是否大于等于。
         *
         * @param other 另一个Chain实例
         */
        public boolean gte(Chain other) {
            return value >= other.value;
        }

        /**
         * 是否小于。
         *
         * @param amount 比较值（分）
         */
        public boolean lt(long amount) {
            return value < amount;
        }

        /**
         * 是否小于。
         *
         * @param other 另一个Chain实例
         */
        public boolean lt(Chain other) {
            return value < other.value;
        }

        /**
         * 是否小于等于。
         *
         * @param amount 比较值（分）
         */
        public boolean lte(long amount) {
            return value <= amount;
        }

        /**
         * 是否小于等于。
         *
         * @param other 另一个Chain实例
         */
        public boolean lte(Chain other) {
            return value <= other.value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Chain)) return false;
            return value == ((Chain) o).value;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(value);
        }

        @Override
        public String toString() {
            return yuan();
        }
    }
}
