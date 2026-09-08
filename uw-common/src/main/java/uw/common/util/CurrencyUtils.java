package uw.common.util;

import java.util.Currency;
import java.util.Locale;
import java.util.Set;

/**
 * 货币工具类。
 * <p>
 * 基于JDK的{@link Currency}提供币种解析与<b>最小货币单位</b>（minor unit）换算能力。
 * 币种精度数据来自JDK内置的ISO 4217表，随JDK更新维护，无需自行维护币种表。
 * <p>
 * <b>最小货币单位</b>是金额存储的基本单位，其小数位数由币种决定（ISO 4217 exponent）：
 * <ul>
 *   <li>2位（scale=100）——CNY/USD/EUR等180种，最小单位为「分」</li>
 *   <li>0位（scale=1）——JPY/KRW/VND等30种，无小数概念，最小单位即主单位</li>
 *   <li>3位（scale=1000）——BHD/KWD/OMR/JOD/TND/LYD/IQD共7种中东北非第纳尔系</li>
 *   <li>4位（scale=10000）——CLF智利记账单位</li>
 * </ul>
 * 因此<b>「金额除以100得到元」的假设仅对2位币种成立</b>，跨币种场景必须使用
 * {@link #getScale(Currency)}换算，或直接使用{@link MoneyUtils}的币种感知方法。
 * <p>
 * 黄金(XAU)、特别提款权(XDR)等非流通代码的{@code getDefaultFractionDigits()}为-1，
 * 本类统一按{@link #getMinorUnit(Currency)}返回0处理（视为无小数），避免负数指数导致的换算异常。
 */
public class CurrencyUtils {

    /**
     * 默认币种为CNY.
     */
    public static final Currency CURRENCY_DEFAULT = Currency.getInstance("CNY");

    /**
     * 10的幂查表，索引为最小货币单位小数位数。
     * <p>
     * 覆盖ISO 4217全部取值（0~4），查表避免{@code Math.pow}的浮点误差。
     */
    private static final long[] SCALES = {1L, 10L, 100L, 1000L, 10000L};

    /**
     * 获得当前可用的货币集合。
     *
     * @return 当前JDK支持的全部ISO 4217货币
     */
    public static Set<Currency> getAvailableCurrencies() {
        return Currency.getAvailableCurrencies();
    }


    /**
     * 获得指定的currency。
     *
     * @param currencyCode 货币代码（ISO 4217），如CNY/USD/JPY
     * @return 对应货币；代码非法时返回null
     */
    public static Currency getCurrency(String currencyCode) {
        return getCurrency(currencyCode, null);
    }

    /**
     * 获得指定的currency。
     * <p>
     * 代码大小写不敏感：{@link Currency#getInstance(String)}仅接受大写ISO代码，
     * 本方法先统一转大写并去除首尾空白，避免库表/接口传入小写代码（如{@code "jpy"}）时
     * 静默回落到默认币种——那会导致按错误精度换算金额。
     *
     * @param currencyCode    货币代码（ISO 4217），大小写不敏感，如CNY/cny/JPY
     * @param defaultCurrency 代码非法或为null时的兜底币种
     * @return 对应货币；代码非法时返回defaultCurrency
     */
    public static Currency getCurrency(String currencyCode, Currency defaultCurrency) {
        if (currencyCode == null) {
            return defaultCurrency;
        }
        try {
            return Currency.getInstance(currencyCode.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return defaultCurrency;
        }
    }

    /**
     * 获得币种的最小货币单位小数位数（ISO 4217 exponent）。
     * <p>
     * 例：CNY/USD=2，JPY/KRW=0，KWD/BHD=3。
     * 黄金等非流通代码JDK返回-1，本方法归一化为0（视为无小数），保证换算比例恒为正。
     *
     * @param currency 币种，null时按{@link #CURRENCY_DEFAULT}处理
     * @return 最小货币单位小数位数，恒在0~4之间
     */
    public static int getMinorUnit(Currency currency) {
        int digits = (currency == null ? CURRENCY_DEFAULT : currency).getDefaultFractionDigits();
        // 黄金/特别提款权等非流通代码返回-1，归一化为0；理论上不会超过4，防御性截断
        if (digits < 0) {
            return 0;
        }
        return Math.min(digits, SCALES.length - 1);
    }

    /**
     * 获得币种的最小货币单位小数位数。
     *
     * @param currencyCode 货币代码（ISO 4217）；非法时按{@link #CURRENCY_DEFAULT}处理
     * @return 最小货币单位小数位数，恒在0~4之间
     */
    public static int getMinorUnit(String currencyCode) {
        return getMinorUnit(getCurrency(currencyCode, CURRENCY_DEFAULT));
    }

    /**
     * 获得币种的最小货币单位与主单位的换算比例，即{@code 10^minorUnit}。
     * <p>
     * 例：CNY/USD=100（1元=100分），JPY/KRW=1（无最小单位细分），KWD=1000。
     * <b>这是替代硬编码100的唯一正确方式。</b>
     *
     * @param currency 币种，null时按{@link #CURRENCY_DEFAULT}处理
     * @return 换算比例，恒为正数（1/10/100/1000/10000）
     */
    public static long getScale(Currency currency) {
        return SCALES[getMinorUnit(currency)];
    }

    /**
     * 获得币种的最小货币单位与主单位的换算比例，即{@code 10^minorUnit}。
     *
     * @param currencyCode 货币代码（ISO 4217）；非法时按{@link #CURRENCY_DEFAULT}处理
     * @return 换算比例，恒为正数（1/10/100/1000/10000）
     */
    public static long getScale(String currencyCode) {
        return SCALES[getMinorUnit(currencyCode)];
    }

    /**
     * 获得10的指定次幂（内部换算用）。
     *
     * @param exponent 指数，须在0~4之间
     * @return 10^exponent
     */
    static long pow10(int exponent) {
        return SCALES[exponent];
    }

}
