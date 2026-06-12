package uw.common.util;

import java.util.Currency;
import java.util.Set;

/**
 * 货币工具类。
 */
public class CurrencyUtils {

    /**
     * 默认币种为CNY.
     */
    public static final Currency CURRENCY_DEFAULT = Currency.getInstance("CNY");

    /**
     * 获得当前可用的货币集合。
     *
     * @return
     */
    public static Set<Currency> getAvailableCurrencies() {
        return Currency.getAvailableCurrencies();
    }


    /**
     * 获得指定的currency。
     *
     * @param currencyCode
     * @return
     */
    public static Currency getCurrency(String currencyCode) {
        return getCurrency(currencyCode, null);
    }

    /**
     * 获得指定的currency。
     *
     * @param currencyCode
     * @return
     */
    public static Currency getCurrency(String currencyCode, Currency defaultCurrency) {
        try {
            return Currency.getInstance(currencyCode);
        } catch (Exception e) {
            return defaultCurrency;
        }
    }

}
