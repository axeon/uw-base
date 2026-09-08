package uw.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

/**
 * 货币计算工具类。
 * <p>
 * 所有金额以<b>最小货币单位</b>（minor unit）为单位存储在long中，加减乘除整数运算避免浮点误差。
 * 汇率等非整数倍率场景使用BigDecimal保证精度，结果四舍五入到最小单位。
 * 溢出或除零时抛出ArithmeticException。
 * <p>
 * <b>最小货币单位不等于「分」</b>——其小数位数由币种决定（ISO 4217 exponent，见{@link CurrencyUtils}）：
 * CNY/USD为2位（1元=100分），JPY/KRW为0位（无小数细分），KWD/BHD等为3位（1第纳尔=1000费尔）。
 * 因此<b>硬编码除以100仅对2位币种成立</b>；跨币种场景请使用本类的币种感知方法
 * （{@link #toMajor}/{@link #fromMajor}/{@link #exchange}）而非{@link #toYuan}/{@link #fromYuan}。
 * <p>
 * 币种缺省一律按{@link CurrencyUtils#CURRENCY_DEFAULT}（CNY）处理，故存量「分」语义的调用完全等价，
 * 无需改动即可继续使用。
 * <p>
 * 提供两套API：
 * <ul>
 *   <li>静态方法 —— 单次运算，一行搞定</li>
 *   <li>{@link Chain} 链式调用 —— 多步连续运算，可携带币种</li>
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
 *
 * String cn = MoneyUtils.of(214748364700L)
 *         .chinese();                   // "贰拾壹亿肆仟柒佰肆拾捌万叁仟陆佰肆拾柒元整"
 * </pre>
 * <p>
 * 多币种示例：
 * <pre>
 * String jpy = MoneyUtils.of(1000, "JPY").major();     // "1000"（日元无小数）
 * String kwd = MoneyUtils.of(1999, "KWD").major();     // "1.999"（第纳尔千分位）
 *
 * // 跨币种换算自动处理精度差：100.00 USD × 150 = 15000 日元
 * long yen = MoneyUtils.exchange(10000, "150", "USD", "JPY");   // 15000
 *
 * // 币种不一致的运算直接抛异常，防止串币
 * MoneyUtils.of(100, "CNY").add(MoneyUtils.of(100, "USD"));     // IllegalArgumentException
 * </pre>
 */
public final class MoneyUtils {

    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private MoneyUtils() {
    }

    // ==================== 链式入口 ====================

    /**
     * 以最小货币单位创建链式计算，币种为默认的CNY。
     *
     * @param minorAmount 金额（最小货币单位，CNY下即分）
     * @return Chain实例
     */
    public static Chain of(long minorAmount) {
        return new Chain(minorAmount, CurrencyUtils.CURRENCY_DEFAULT);
    }

    /**
     * 以最小货币单位创建链式计算，并指定币种。
     * <p>
     * 币种决定{@link Chain#major()}的小数位数，并参与运算时的一致性校验。
     *
     * @param minorAmount 金额（最小货币单位，如JPY下即日元、KWD下即费尔）
     * @param currency    币种，null时按CNY处理
     * @return Chain实例
     */
    public static Chain of(long minorAmount, Currency currency) {
        return new Chain(minorAmount, currency);
    }

    /**
     * 以最小货币单位创建链式计算，并指定币种。
     *
     * @param minorAmount  金额（最小货币单位）
     * @param currencyCode 货币代码（ISO 4217），非法时按CNY处理
     * @return Chain实例
     */
    public static Chain of(long minorAmount, String currencyCode) {
        return new Chain(minorAmount, CurrencyUtils.getCurrency(currencyCode, CurrencyUtils.CURRENCY_DEFAULT));
    }

    /**
     * 以元为单位创建链式计算（CNY专用）。
     *
     * @param yuan 金额字符串（元），如 "1.99"
     * @return Chain实例
     */
    public static Chain ofYuan(String yuan) {
        return new Chain(fromYuan(yuan), CurrencyUtils.CURRENCY_DEFAULT);
    }

    /**
     * 以主货币单位创建链式计算，小数位数按币种校验。
     *
     * @param majorAmount 金额字符串（主单位），如CNY的"1.99"、JPY的"1000"、KWD的"1.999"
     * @param currency    币种，null时按CNY处理
     * @return Chain实例
     */
    public static Chain ofMajor(String majorAmount, Currency currency) {
        return new Chain(fromMajor(majorAmount, currency), currency);
    }

    /**
     * 以主货币单位创建链式计算，小数位数按币种校验。
     *
     * @param majorAmount  金额字符串（主单位）
     * @param currencyCode 货币代码（ISO 4217），非法时按CNY处理
     * @return Chain实例
     */
    public static Chain ofMajor(String majorAmount, String currencyCode) {
        return ofMajor(majorAmount, CurrencyUtils.getCurrency(currencyCode, CurrencyUtils.CURRENCY_DEFAULT));
    }

    // ==================== 加减 ====================

    /**
     * 安全相加，溢出时抛出ArithmeticException。
     *
     * @param a 加数（最小货币单位）
     * @param b 加数（最小货币单位）
     * @return a + b（最小货币单位）
     */
    public static long add(long a, long b) {
        return Math.addExact(a, b);
    }

    /**
     * 安全相减，溢出时抛出ArithmeticException。
     *
     * @param a 被减数（最小货币单位）
     * @param b 减数（最小货币单位）
     * @return a - b（最小货币单位）
     */
    public static long subtract(long a, long b) {
        return Math.subtractExact(a, b);
    }

    /**
     * 多值求和，溢出时抛出ArithmeticException。
     *
     * @param values 待求和的金额数组（最小货币单位），不可为null
     * @return 所有值之和（最小货币单位）
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
     * @param amount 金额（最小货币单位）
     * @param factor 整数倍数，如数量
     * @return amount × factor（最小货币单位）
     */
    public static long multiply(long amount, long factor) {
        return Math.multiplyExact(amount, factor);
    }

    /**
     * 乘以百分比（万分之），四舍五入到最小货币单位。
     *
     * @param amount  金额（最小货币单位）
     * @param rateBps 比率，单位为万分之（‱）。如850表示8.5%
     * @return amount × rateBps / 10000，四舍五入（最小货币单位）
     */
    public static long multiplyBps(long amount, long rateBps) {
        long product = Math.multiplyExact(amount, rateBps);
        return divideHalfUp(product, 10000L);
    }

    /**
     * 乘以比率（分子/分母），四舍五入到最小货币单位。
     *
     * @param amount      金额（最小货币单位）
     * @param numerator   比率分子，如税率13
     * @param denominator 比率分母，如100。不可为0
     * @return amount × numerator / denominator，四舍五入（最小货币单位）
     */
    public static long multiplyRatio(long amount, long numerator, long denominator) {
        if (denominator == 0) {
            throw new ArithmeticException("denominator must not be zero");
        }
        long product = Math.multiplyExact(amount, numerator);
        return divideHalfUp(product, denominator);
    }

    /**
     * 乘以double倍率，四舍五入到最小货币单位。
     * <p>
     * 适用于汇率等非整数倍率。内部使用BigDecimal避免浮点误差。
     * 例：multiplyRate(1000000, 0.1477) = 147700（10000元 × 0.1477 = 1477.00元）
     *
     * @param amount 金额（最小货币单位）
     * @param rate   倍率，如汇率0.1477。建议不超过6位小数
     * @return amount × rate，四舍五入（最小货币单位）
     */
    public static long multiplyRate(long amount, double rate) {
        // BigDecimal.valueOf(double)取double的规范字符串表示（最短往返表示），
        // 再精确相乘——不指定MathContext，避免大额金额被截断到16位有效数字
        return BigDecimal.valueOf(amount)
                .multiply(BigDecimal.valueOf(rate))
                .setScale(0, ROUNDING)
                .longValueExact();
    }

    /**
     * 乘以String倍率，四舍五入到最小货币单位。
     * <p>
     * 适用于汇率等非整数倍率。String入参避免double构造时的精度丢失。
     * 例：multiplyRate(1000000, "0.1477") = 147700（10000元 × 0.1477 = 1477.00元）
     *
     * @param amount 金额（最小货币单位）
     * @param rate   倍率字符串，如"0.1477"。不可为null
     * @return amount × rate，四舍五入（最小货币单位）
     */
    public static long multiplyRate(long amount, String rate) {
        // 不指定MathContext以保持完全精确：long最大19位有效数字，DECIMAL64仅16位会静默截断大额
        return BigDecimal.valueOf(amount)
                .multiply(new BigDecimal(rate))
                .setScale(0, ROUNDING)
                .longValueExact();
    }

    // ==================== 除法 ====================

    /**
     * 四舍五入除法。
     *
     * @param dividend 被除数（最小货币单位）
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
     * @param dividend 被除数（最小货币单位）
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
     * 除以double倍率，四舍五入到最小货币单位。
     * <p>
     * 适用于汇率逆向换算等场景。内部使用BigDecimal避免浮点误差。
     * 例：divideRate(147700, 0.1477) = 1000000（1477.00 / 0.1477 = 10000.00）
     *
     * @param amount 金额（最小货币单位）
     * @param rate   除数倍率，如汇率0.1477。不可为0
     * @return amount / rate，四舍五入（最小货币单位）
     */
    public static long divideRate(long amount, double rate) {
        return BigDecimal.valueOf(amount)
                .divide(BigDecimal.valueOf(rate), 0, ROUNDING)
                .longValueExact();
    }

    /**
     * 除以String倍率，四舍五入到最小货币单位。
     * <p>
     * 适用于汇率逆向换算等场景。String入参避免double构造时的精度丢失。
     * 例：divideRate(147700, "0.1477") = 1000000（1477.00 / 0.1477 = 10000.00）
     *
     * @param amount 金额（最小货币单位）
     * @param rate   除数字符串，如"0.1477"。不可为null或"0"
     * @return amount / rate，四舍五入（最小货币单位）
     */
    public static long divideRate(long amount, String rate) {
        return BigDecimal.valueOf(amount)
                .divide(new BigDecimal(rate), 0, ROUNDING)
                .longValueExact();
    }

    // ==================== 汇率换算 ====================

    /**
     * 按汇率跨币种换算金额，自动处理两币种的最小单位精度差。
     * <p>
     * 汇率语义为「1 主单位from = rate 主单位to」（与银行报价一致），而金额以最小单位存储，
     * 故换算公式为：
     * <pre>
     * minorTo = minorFrom × rate × 10^(minorUnitTo − minorUnitFrom)
     * </pre>
     * 两币种精度相同时修正因子为1，退化为普通的{@link #multiplyRate}。
     * <p>
     * <b>精度差是易被忽略的资损点</b>：USD(2位)→JPY(0位)按汇率150换算，
     * 10000（=100.00美元）若直接相乘得1500000（=150万日元），实际应为15000日元，相差100倍。
     * <pre>
     * exchange(10000, "150", USD, JPY)   // 15000 ✓
     * multiplyRate(10000, "150")         // 1500000 ✗ 未修正精度差
     * </pre>
     *
     * @param minorAmount  源金额（源币种的最小货币单位）
     * @param rate         汇率字符串，如"150"、"0.1477"。不可为null
     * @param fromCurrency 源币种，null时按CNY处理
     * @param toCurrency   目标币种，null时按CNY处理
     * @return 换算后金额（目标币种的最小货币单位），四舍五入
     */
    public static long exchange(long minorAmount, String rate, Currency fromCurrency, Currency toCurrency) {
        int fromUnit = CurrencyUtils.getMinorUnit(fromCurrency);
        int toUnit = CurrencyUtils.getMinorUnit(toCurrency);
        // 乘法不指定MathContext以保持完全精确：long最大19位有效数字，DECIMAL64仅16位会静默截断
        BigDecimal result = BigDecimal.valueOf(minorAmount).multiply(new BigDecimal(rate));
        int delta = toUnit - fromUnit;
        if (delta > 0) {
            result = result.multiply(BigDecimal.valueOf(CurrencyUtils.pow10(delta)));
        } else if (delta < 0) {
            // 一次定标到整数：显式scale+舍入模式，避免无终止小数抛ArithmeticException
            return result.divide(BigDecimal.valueOf(CurrencyUtils.pow10(-delta)), 0, ROUNDING).longValueExact();
        }
        return result.setScale(0, ROUNDING).longValueExact();
    }

    /**
     * 按汇率跨币种换算金额，自动处理两币种的最小单位精度差。
     *
     * @param minorAmount      源金额（源币种的最小货币单位）
     * @param rate             汇率字符串，如"150"、"0.1477"。不可为null
     * @param fromCurrencyCode 源币种代码（ISO 4217），非法时按CNY处理
     * @param toCurrencyCode   目标币种代码（ISO 4217），非法时按CNY处理
     * @return 换算后金额（目标币种的最小货币单位），四舍五入
     */
    public static long exchange(long minorAmount, String rate, String fromCurrencyCode, String toCurrencyCode) {
        return exchange(minorAmount, rate,
                CurrencyUtils.getCurrency(fromCurrencyCode, CurrencyUtils.CURRENCY_DEFAULT),
                CurrencyUtils.getCurrency(toCurrencyCode, CurrencyUtils.CURRENCY_DEFAULT));
    }

    // ==================== 分摊 ====================

    /**
     * 按比例分摊总金额到多份，最后一笔兜底，保证合计等于total。
     * <p>
     * 解决分笔舍入导致的"尾差"问题。
     * 例：allocate(100, {40, 30, 20, 10}) = {40, 30, 20, 10}
     *
     * @param total   待分摊总额（最小货币单位）
     * @param weights 各份权重，不可为null或空数组，不可有负数
     * @return 各份金额数组（最小货币单位），长度与weights相同，合计等于total
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

    // ==================== 主单位/最小单位转换 ====================

    /**
     * 最小货币单位 → 主货币单位字符串，小数位数由币种决定。
     * <p>
     * 例：{@code toMajor(1999, CNY)="19.99"}、{@code toMajor(1000, JPY)="1000"}、
     * {@code toMajor(1999, KWD)="1.999"}。0位币种不输出小数点。
     *
     * @param minorAmount 金额（最小货币单位）
     * @param currency    币种，null时按CNY处理
     * @return 主单位字符串，负数以"-"开头
     */
    public static String toMajor(long minorAmount, Currency currency) {
        int digits = CurrencyUtils.getMinorUnit(currency);
        if (digits == 0) {
            return Long.toString(minorAmount);
        }
        long scale = CurrencyUtils.pow10(digits);
        boolean negative = minorAmount < 0;
        // 先取绝对值再拆分，避免Long.MIN_VALUE取反溢出：用求余的绝对值而非对被除数取反
        long major = Math.abs(minorAmount / scale);
        long minor = Math.abs(minorAmount % scale);
        StringBuilder sb = new StringBuilder();
        if (negative) {
            sb.append('-');
        }
        sb.append(major).append('.');
        // 左补零到币种精度，如CNY的9分输出"09"、KWD的9费尔输出"009"
        String minorStr = Long.toString(minor);
        for (int i = minorStr.length(); i < digits; i++) {
            sb.append('0');
        }
        sb.append(minorStr);
        return sb.toString();
    }

    /**
     * 最小货币单位 → 主货币单位字符串，小数位数由币种决定。
     *
     * @param minorAmount  金额（最小货币单位）
     * @param currencyCode 货币代码（ISO 4217），非法时按CNY处理
     * @return 主单位字符串
     */
    public static String toMajor(long minorAmount, String currencyCode) {
        return toMajor(minorAmount, CurrencyUtils.getCurrency(currencyCode, CurrencyUtils.CURRENCY_DEFAULT));
    }

    /**
     * 主货币单位 → 最小货币单位，小数位数超出币种精度时抛出异常。
     * <p>
     * 例：{@code fromMajor("19.99", CNY)=1999}、{@code fromMajor("1000", JPY)=1000}、
     * {@code fromMajor("1.999", KWD)=1999}。
     * <b>不做静默截断</b>——{@code fromMajor("1.999", CNY)}会抛出IllegalArgumentException，
     * 避免精度丢失导致的资金差错。
     *
     * @param majorAmount 金额字符串（主单位），不可为null或空
     * @param currency    币种，null时按CNY处理
     * @return 金额（最小货币单位）
     */
    public static long fromMajor(String majorAmount, Currency currency) {
        if (majorAmount == null || majorAmount.isEmpty()) {
            throw new IllegalArgumentException("amount must not be empty");
        }
        int digits = CurrencyUtils.getMinorUnit(currency);
        long scale = CurrencyUtils.pow10(digits);
        boolean negative = majorAmount.startsWith("-");
        String num = negative ? majorAmount.substring(1) : majorAmount;
        int dot = num.indexOf('.');
        if (dot < 0) {
            long v = Long.parseLong(num);
            long result = Math.multiplyExact(v, scale);
            return negative ? -result : result;
        }
        String intPart = num.substring(0, dot);
        String decPart = num.substring(dot + 1);
        if (decPart.length() > digits) {
            throw new IllegalArgumentException("amount scale must not exceed " + digits + ": " + majorAmount);
        }
        long intVal = intPart.isEmpty() ? 0 : Long.parseLong(intPart);
        long decVal = decPart.isEmpty() ? 0 : Long.parseLong(decPart);
        // 右补零到币种精度，如CNY的"1.9"补为190、KWD的"1.9"补为1900
        for (int i = decPart.length(); i < digits; i++) {
            decVal = Math.multiplyExact(decVal, 10);
        }
        long result = Math.addExact(Math.multiplyExact(intVal, scale), decVal);
        return negative ? -result : result;
    }

    /**
     * 主货币单位 → 最小货币单位，小数位数超出币种精度时抛出异常。
     *
     * @param majorAmount  金额字符串（主单位），不可为null或空
     * @param currencyCode 货币代码（ISO 4217），非法时按CNY处理
     * @return 金额（最小货币单位）
     */
    public static long fromMajor(String majorAmount, String currencyCode) {
        return fromMajor(majorAmount, CurrencyUtils.getCurrency(currencyCode, CurrencyUtils.CURRENCY_DEFAULT));
    }

    // ==================== 元/分转换（CNY专用） ====================

    /**
     * 分 → 元（字符串，保留2位小数）。
     * <p>
     * CNY专用快捷方法，等价于{@code toMajor(cents, CNY)}。
     * <b>多币种场景请用{@link #toMajor}</b>——日元/第纳尔等币种的换算比例并非100。
     *
     * @param cents 金额（分）
     * @return 元字符串，如 toYuan(199) = "1.99"
     */
    public static String toYuan(long cents) {
        return toMajor(cents, CurrencyUtils.CURRENCY_DEFAULT);
    }

    /**
     * 元 → 分。支持整数("5")、一位小数("1.9"→190)、两位小数("1.99"→199)。
     * <p>
     * CNY专用快捷方法，等价于{@code fromMajor(yuan, CNY)}。
     * <b>多币种场景请用{@link #fromMajor}</b>——日元/第纳尔等币种的换算比例并非100。
     *
     * @param yuan 金额字符串（元），不可为null或空。不支持超过两位小数
     * @return 金额（分），如 fromYuan("1.99") = 199
     */
    public static long fromYuan(String yuan) {
        if (yuan == null || yuan.isEmpty()) {
            throw new IllegalArgumentException("yuan must not be empty");
        }
        return fromMajor(yuan, CurrencyUtils.CURRENCY_DEFAULT);
    }

    // ==================== 中文大写 ====================

    private static final char[] CN_DIGITS = {'零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖'};
    private static final String[] CN_INT_UNITS = {"", "万", "亿", "万亿"};
    private static final String[] CN_SEC_UNITS = {"", "拾", "佰", "仟"};

    /**
     * 分 → 中文大写金额（CNY专用）。
     * <p>
     * 标准格式：整数部分按"亿/万/元"分组，角分位分别为"角/分"，无角分时以"整"结尾。
     * 例：toChinese(2147483647) = "贰仟壹佰肆拾柒万肆仟捌佰叁拾陆元肆角柒分"
     * <p>
     * 「元/角/分」是人民币专有单位，<b>不可用于其他币种</b>——传入日元/第纳尔金额会得到错误文案。
     *
     * @param cents 金额（分），支持负数
     * @return 中文大写字符串，负数以"负"开头
     */
    public static String toChinese(long cents) {
        if (cents == 0) {
            return "零元整";
        }
        StringBuilder sb = new StringBuilder();
        if (cents < 0) {
            sb.append("负");
            cents = -cents;
        }
        long yuan = cents / 100;
        int fen = (int) (cents % 100);
        int jiao = fen / 10;
        int fenBit = fen % 10;

        if (yuan > 0) {
            sb.append(integerToChinese(yuan)).append("元");
        }
        boolean hasJiao = jiao > 0;
        boolean hasFen = fenBit > 0;
        if (hasJiao) {
            sb.append(CN_DIGITS[jiao]).append("角");
        }
        if (hasFen) {
            sb.append(CN_DIGITS[fenBit]).append("分");
        }
        if (!hasJiao && !hasFen) {
            sb.append("整");
        } else if (!hasFen) {
            // 仅角无分：标准写法仍以"整"收尾
            sb.append("整");
        }
        return sb.toString();
    }

    private static String integerToChinese(long n) {
        String s = Long.toString(n);
        int len = s.length();
        StringBuilder result = new StringBuilder();
        int sectionCount = (len + 3) / 4;
        boolean lastSectionZero = true;
        boolean needZeroBetweenSections = false;

        for (int si = 0; si < sectionCount; si++) {
            int end = len - si * 4;
            int start = Math.max(0, end - 4);
            String section = s.substring(start, end);
            int sectionVal = Integer.parseInt(section);
            if (sectionVal == 0) {
                needZeroBetweenSections = true;
                continue;
            }
            StringBuilder secSb = new StringBuilder();
            boolean prevZero = false;
            for (int i = 0; i < section.length(); i++) {
                int d = section.charAt(i) - '0';
                int pos = section.length() - 1 - i;
                if (d == 0) {
                    prevZero = true;
                } else {
                    if (prevZero) {
                        secSb.append('零');
                    }
                    secSb.append(CN_DIGITS[d]).append(CN_SEC_UNITS[pos]);
                    prevZero = false;
                }
            }
            if (needZeroBetweenSections && !lastSectionZero) {
                result.insert(0, '零');
            }
            secSb.append(CN_INT_UNITS[si]);
            result.insert(0, secSb);
            lastSectionZero = false;
            needZeroBetweenSections = false;
        }
        return result.toString();
    }

    // ==================== 链式计算器 ====================

    /**
     * 链式货币计算器，支持连续运算后以 {@link #cent()} 或 {@link #major()} 结算。
     * <p>
     * 所有链式方法修改并返回自身，可连续调用。
     * 使用 {@link #cent()} 获取结果（最小货币单位），{@link #major()} 获取主单位字符串（按币种精度），
     * 或 {@link #yuan()} 获取元字符串（CNY专用）。
     * <p>
     * Chain携带币种信息：{@link #major()}据此决定小数位数，
     * 与另一Chain的运算/比较会校验币种一致性，<b>币种不一致直接抛出IllegalArgumentException</b>，
     * 防止不同币种金额被误当作同币种相加（串币）。
     */
    public static final class Chain {

        private long value;

        private final Currency currency;

        private Chain(long minorAmount, Currency currency) {
            this.value = minorAmount;
            this.currency = currency == null ? CurrencyUtils.CURRENCY_DEFAULT : currency;
        }

        /**
         * 校验另一Chain的币种与本Chain一致，不一致抛出异常。
         *
         * @param other 另一个Chain实例
         */
        private void checkCurrency(Chain other) {
            if (!currency.equals(other.currency)) {
                throw new IllegalArgumentException(
                        "currency mismatch: " + currency.getCurrencyCode() + " vs " + other.currency.getCurrencyCode());
            }
        }

        /**
         * 获取本Chain的币种。
         *
         * @return 币种，恒非null
         */
        public Currency currency() {
            return currency;
        }

        // ---------- 加减 ----------

        /**
         * 加法。
         *
         * @param amount 加数（最小货币单位）
         * @return this
         */
        public Chain add(long amount) {
            value = Math.addExact(value, amount);
            return this;
        }

        /**
         * 加法，币种须一致。
         *
         * @param other 另一个Chain实例，币种不一致时抛出IllegalArgumentException
         * @return this
         */
        public Chain add(Chain other) {
            checkCurrency(other);
            return add(other.value);
        }

        /**
         * 减法。
         *
         * @param amount 减数（最小货币单位）
         * @return this
         */
        public Chain subtract(long amount) {
            value = Math.subtractExact(value, amount);
            return this;
        }

        /**
         * 减法，币种须一致。
         *
         * @param other 另一个Chain实例，币种不一致时抛出IllegalArgumentException
         * @return this
         */
        public Chain subtract(Chain other) {
            checkCurrency(other);
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
         * 按汇率换算为目标币种，自动处理最小单位精度差。
         * <p>
         * 币种发生变化，故<b>返回新的Chain实例</b>（其余链式方法均返回自身）。
         *
         * @param rate       汇率字符串（1 主单位当前币种 = rate 主单位目标币种）
         * @param toCurrency 目标币种，null时按CNY处理
         * @return 目标币种的新Chain实例
         */
        public Chain exchangeTo(String rate, Currency toCurrency) {
            return new Chain(MoneyUtils.exchange(value, rate, currency, toCurrency), toCurrency);
        }

        /**
         * 按汇率换算为目标币种，自动处理最小单位精度差。
         *
         * @param rate           汇率字符串
         * @param toCurrencyCode 目标币种代码（ISO 4217），非法时按CNY处理
         * @return 目标币种的新Chain实例
         */
        public Chain exchangeTo(String rate, String toCurrencyCode) {
            return exchangeTo(rate, CurrencyUtils.getCurrency(toCurrencyCode, CurrencyUtils.CURRENCY_DEFAULT));
        }

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
         * 结算为最小货币单位。
         *
         * @return 金额（最小货币单位，CNY下即分）
         */
        public long cent() {
            return value;
        }

        /**
         * 结算为最小货币单位（{@link #cent()}的多币种语义别名）。
         *
         * @return 金额（最小货币单位）
         */
        public long minor() {
            return value;
        }

        /**
         * 结算为主货币单位字符串，小数位数由币种决定。
         * <p>
         * 例：CNY下1999→"19.99"，JPY下1000→"1000"，KWD下1999→"1.999"。
         *
         * @return 主单位字符串
         */
        public String major() {
            return MoneyUtils.toMajor(value, currency);
        }

        /**
         * 结算为元字符串，保留2位小数（CNY专用）。
         * <p>
         * 多币种场景请用{@link #major()}——非2位币种下"元"的概念并不成立。
         *
         * @return 如 "1.99"
         */
        public String yuan() {
            return MoneyUtils.toYuan(value);
        }

        /**
         * 结算为中文大写金额，如 "贰拾壹亿肆仟柒佰肆拾捌万叁仟陆佰肆拾柒元整"（CNY专用）。
         * <p>
         * 「元/角/分」是人民币专有单位，本方法不校验币种，<b>非CNY的Chain调用会得到错误文案</b>。
         *
         * @return 中文大写字符串
         */
        public String chinese() {
            return MoneyUtils.toChinese(value);
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
         * @param amount 比较值（最小货币单位）
         */
        public boolean gt(long amount) {
            return value > amount;
        }

        /**
         * 是否大于，币种须一致。
         *
         * @param other 另一个Chain实例，币种不一致时抛出IllegalArgumentException
         */
        public boolean gt(Chain other) {
            checkCurrency(other);
            return value > other.value;
        }

        /**
         * 是否大于等于。
         *
         * @param amount 比较值（最小货币单位）
         */
        public boolean gte(long amount) {
            return value >= amount;
        }

        /**
         * 是否大于等于，币种须一致。
         *
         * @param other 另一个Chain实例，币种不一致时抛出IllegalArgumentException
         */
        public boolean gte(Chain other) {
            checkCurrency(other);
            return value >= other.value;
        }

        /**
         * 是否小于。
         *
         * @param amount 比较值（最小货币单位）
         */
        public boolean lt(long amount) {
            return value < amount;
        }

        /**
         * 是否小于，币种须一致。
         *
         * @param other 另一个Chain实例，币种不一致时抛出IllegalArgumentException
         */
        public boolean lt(Chain other) {
            checkCurrency(other);
            return value < other.value;
        }

        /**
         * 是否小于等于。
         *
         * @param amount 比较值（最小货币单位）
         */
        public boolean lte(long amount) {
            return value <= amount;
        }

        /**
         * 是否小于等于，币种须一致。
         *
         * @param other 另一个Chain实例，币种不一致时抛出IllegalArgumentException
         */
        public boolean lte(Chain other) {
            checkCurrency(other);
            return value <= other.value;
        }

        /**
         * 判等：金额与币种均相同才相等。
         * <p>
         * 与{@link #gt}等比较方法不同，本方法对币种不一致<b>返回false而非抛异常</b>，
         * 以符合{@link Object#equals}的通用约定（可安全用于集合）。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Chain other)) return false;
            return value == other.value && currency.equals(other.currency);
        }

        @Override
        public int hashCode() {
            return Long.hashCode(value) * 31 + currency.hashCode();
        }

        /**
         * 输出主单位金额加币种代码，如"19.99 CNY"、"1000 JPY"。
         */
        @Override
        public String toString() {
            return major() + " " + currency.getCurrencyCode();
        }
    }
}
