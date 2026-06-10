package uw.common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Pattern;

/**
 * 数据校验工具类，提供业务系统常见的校验方法。
 * <p>
 * 所有方法返回 boolean，不抛异常。null 输入统一返回 false。
 * <p>
 * 中国特有校验方法以 {@code China} 命名，如：
 * {@link #isChinaIdCard(String)}、{@link #isChinaMobile(String)}、
 * {@link #isChinaPlateNo(String)}、{@link #isChinaUscc(String)}、{@link #isChinaName(String)}。
 */
public final class ValidateUtils {

    // ==================== 正则常量 ====================

    private static final Pattern DIGITS_PATTERN = Pattern.compile("^\\d+$");
    private static final Pattern LETTERS_PATTERN = Pattern.compile("^[a-zA-Z]+$");
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");
    private static final Pattern INTEGER_PATTERN = Pattern.compile("^[+-]?\\d+$");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("^[+-]?\\d+(\\.\\d+)?$");
    private static final Pattern CHINESE_NAME_PATTERN = Pattern.compile("^[一-龥·]{2,20}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"
    );
    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$"
    );
    private static final Pattern IPV6_PATTERN = Pattern.compile(
            "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$"
    );

    // ==================== 格式化常量 ====================

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter
            .ofPattern("uuuu-MM-dd HH:mm:ss")
            .withResolverStyle(ResolverStyle.STRICT);

    private ValidateUtils() {
    }

    // ==================== 字符串校验 ====================

    /**
     * 字符串是否不为空（非null且非空字符串）。
     *
     * @param value 待校验字符串
     * @return 是否不为空
     */
    public static boolean isNotEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    /**
     * 字符串是否不为空白（非null且 trim 后非空）。
     *
     * @param value 待校验字符串
     * @return 是否不为空白
     */
    public static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 字符串长度是否在指定闭区间内。
     * <p>
     * null 视为长度0。
     *
     * @param value  待校验字符串
     * @param minLen 最小长度（包含）
     * @param maxLen 最大长度（包含）
     * @return 长度是否在 [minLen, maxLen] 范围内
     */
    public static boolean isLengthInRange(String value, int minLen, int maxLen) {
        int len = value == null ? 0 : value.length();
        return len >= minLen && len <= maxLen;
    }

    /**
     * 是否为纯数字字符串（0~9）。
     *
     * @param value 待校验字符串，不可为null
     * @return 是否全部为数字
     */
    public static boolean isDigits(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return DIGITS_PATTERN.matcher(value).matches();
    }

    /**
     * 是否为纯英文字母字符串（a~z, A~Z）。
     *
     * @param value 待校验字符串，不可为null
     * @return 是否全部为英文字母
     */
    public static boolean isLetters(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return LETTERS_PATTERN.matcher(value).matches();
    }

    /**
     * 是否为字母数字组合字符串（a~z, A~Z, 0~9）。
     *
     * @param value 待校验字符串，不可为null
     * @return 是否全部为字母或数字
     */
    public static boolean isAlphanumeric(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return ALPHANUMERIC_PATTERN.matcher(value).matches();
    }

    /**
     * 密码强度校验：至少包含字母和数字，长度在指定闭区间内。
     *
     * @param password 密码，不可为null
     * @param minLen   最小长度（包含）
     * @param maxLen   最大长度（包含）
     * @return 是否满足密码强度要求
     */
    public static boolean isStrongPassword(String password, int minLen, int maxLen) {
        if (password == null || password.length() < minLen || password.length() > maxLen) {
            return false;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                hasLetter = true;
            } else if (c >= '0' && c <= '9') {
                hasDigit = true;
            }
        }
        return hasLetter && hasDigit;
    }

    // ==================== 数值校验：整数 ====================

    /**
     * 是否为合法的整数（支持正负号），且在 long 范围内。
     *
     * @param value 待校验字符串，不可为null
     * @return 是否为合法的整数格式
     */
    public static boolean isInteger(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        if (!INTEGER_PATTERN.matcher(value).matches()) {
            return false;
        }
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 是否为正整数（大于0，无符号前缀，无前导零）。
     *
     * @param value 待校验字符串，不可为null
     * @return 是否为正整数
     */
    public static boolean isPositiveInteger(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        if (value.charAt(0) == '0' && value.length() > 1) {
            return false;
        }
        return isDigits(value) && !"0".equals(value);
    }

    /**
     * 是否为非负整数（大于等于0，无符号前缀，无前导零）。
     *
     * @param value 待校验字符串，不可为null
     * @return 是否为非负整数
     */
    public static boolean isNonNegativeInteger(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        if (value.charAt(0) == '0' && value.length() > 1) {
            return false;
        }
        return isDigits(value);
    }

    /**
     * 是否为负整数（小于0，带负号前缀，无前导零）。
     *
     * @param value 待校验字符串，不可为null
     * @return 是否为负整数
     */
    public static boolean isNegativeInteger(String value) {
        if (value == null || !value.startsWith("-") || value.length() < 2) {
            return false;
        }
        String digits = value.substring(1);
        if (digits.charAt(0) == '0' && digits.length() > 1) {
            return false;
        }
        return isDigits(digits) && !"0".equals(digits);
    }

    // ==================== 数值校验：浮点数 ====================

    /**
     * 是否为合法的浮点数（支持正负号，支持整数和小数形式）。
     * <p>
     * 例：isDecimal("123") → true，isDecimal("-12.34") → true，isDecimal("+0.5") → true
     *
     * @param value 待校验字符串，不可为null
     * @return 是否为合法的浮点数格式
     */
    public static boolean isDecimal(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        if (!DECIMAL_PATTERN.matcher(value).matches()) {
            return false;
        }
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 是否为正浮点数（大于0）。
     *
     * @param value 待校验字符串，不可为null
     * @return 是否为正浮点数
     */
    public static boolean isPositiveDecimal(String value) {
        if (!isDecimal(value)) {
            return false;
        }
        return Double.parseDouble(value) > 0;
    }

    /**
     * 是否为非负浮点数（大于等于0）。
     *
     * @param value 待校验字符串，不可为null
     * @return 是否为非负浮点数
     */
    public static boolean isNonNegativeDecimal(String value) {
        if (!isDecimal(value)) {
            return false;
        }
        return Double.parseDouble(value) >= 0;
    }

    /**
     * 是否为负浮点数（小于0）。
     *
     * @param value 待校验字符串，不可为null
     * @return 是否为负浮点数
     */
    public static boolean isNegativeDecimal(String value) {
        if (!isDecimal(value)) {
            return false;
        }
        return Double.parseDouble(value) < 0;
    }

    // ==================== 数值校验：精度与范围 ====================

    /**
     * 浮点数精度校验：是否为合法浮点数且小数位数不超过指定位数。
     *
     * @param value    待校验字符串，不可为null
     * @param maxScale 最大允许小数位数（0表示不允许小数）
     * @return 是否为合法浮点数且精度不超过 maxScale
     */
    public static boolean isDecimalWithScale(String value, int maxScale) {
        if (!isDecimal(value)) {
            return false;
        }
        int dotIndex = value.indexOf('.');
        if (dotIndex < 0) {
            return true;
        }
        int scale = value.length() - dotIndex - 1;
        return scale <= maxScale;
    }

    /**
     * 数值范围校验：是否为合法浮点数且在指定闭区间 [min, max] 内。
     *
     * @param value 待校验字符串，不可为null
     * @param min   最小值（包含）
     * @param max   最大值（包含）
     * @return 是否在 [min, max] 范围内
     */
    public static boolean isInRange(String value, double min, double max) {
        if (!isDecimal(value)) {
            return false;
        }
        double v = Double.parseDouble(value);
        return v >= min && v <= max;
    }

    // ==================== 日期时间校验 ====================

    /**
     * 是否为合法的日期字符串（yyyy-MM-dd）。
     * <p>
     * 使用 ISO 标准解析，自动校验月份、日期合法性（含闰年）。
     *
     * @param value 日期字符串，不可为null
     * @return 是否为合法日期
     */
    public static boolean isDate(String value) {
        if (value == null || value.length() != 10) {
            return false;
        }
        try {
            LocalDate.parse(value, DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * 是否为合法的日期字符串，严格匹配指定格式。
     * <p>
     * 例：isDate("2023/01/05", "yyyy/MM/dd") → true，isDate("20230105", "yyyyMMdd") → true
     *
     * @param value   日期字符串，不可为null
     * @param pattern 日期格式，不可为null。如 "yyyy-MM-dd"、"yyyy/MM/dd"、"yyyyMMdd"
     * @return 是否为合法日期
     */
    public static boolean isDate(String value, String pattern) {
        if (value == null || pattern == null) {
            return false;
        }
        try {
            LocalDate.parse(value, DateTimeFormatter.ofPattern(pattern));
            return true;
        } catch (DateTimeParseException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 日期范围校验：是否为合法日期且在指定闭区间 [start, end] 内。
     *
     * @param value 日期字符串（yyyy-MM-dd），不可为null
     * @param start 起始日期（包含），不可为null
     * @param end   结束日期（包含），不可为null
     * @return 是否为合法日期且在范围内
     */
    public static boolean isDateInRange(String value, LocalDate start, LocalDate end) {
        if (!isDate(value)) {
            return false;
        }
        LocalDate date = LocalDate.parse(value, DATE_FORMATTER);
        return !date.isBefore(start) && !date.isAfter(end);
    }

    /**
     * 是否为合法的时间字符串（HH:mm:ss）。
     * <p>
     * 时：00~23，分：00~59，秒：00~59。
     *
     * @param value 时间字符串，不可为null
     * @return 是否为合法时间
     */
    public static boolean isTime(String value) {
        if (value == null || value.length() != 8) {
            return false;
        }
        try {
            LocalTime.parse(value, TIME_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * 是否为合法的时间字符串，严格匹配指定格式。
     * <p>
     * 例：isTime("14:30", "HH:mm") → true
     *
     * @param value   时间字符串，不可为null
     * @param pattern 时间格式，不可为null。如 "HH:mm:ss"、"HH:mm"
     * @return 是否为合法时间
     */
    public static boolean isTime(String value, String pattern) {
        if (value == null || pattern == null) {
            return false;
        }
        try {
            LocalTime.parse(value, DateTimeFormatter.ofPattern(pattern));
            return true;
        } catch (DateTimeParseException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 是否为合法的日期时间字符串（yyyy-MM-dd HH:mm:ss）。
     * <p>
     * 使用严格解析模式，拒绝超出范围的值（如24:00:00、13月）。
     *
     * @param value 日期时间字符串，不可为null
     * @return 是否为合法的日期时间
     */
    public static boolean isDateTime(String value) {
        if (value == null || value.length() != 19) {
            return false;
        }
        try {
            LocalDateTime.parse(value, DATETIME_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * 是否为合法的日期时间字符串，严格匹配指定格式。
     * <p>
     * 例：isDateTime("2023-01-05T14:30:00", "yyyy-MM-dd'T'HH:mm:ss") → true
     *
     * @param value   日期时间字符串，不可为null
     * @param pattern 日期时间格式，不可为null
     * @return 是否为合法的日期时间
     */
    public static boolean isDateTime(String value, String pattern) {
        if (value == null || pattern == null) {
            return false;
        }
        try {
            LocalDateTime.parse(value, DateTimeFormatter.ofPattern(pattern));
            return true;
        } catch (DateTimeParseException | IllegalArgumentException e) {
            return false;
        }
    }

    // ==================== 网络校验 ====================

    /**
     * 电子邮箱地址格式校验。
     * <p>
     * 长度不超过254字符（RFC 5321）。
     *
     * @param email 邮箱地址，不可为null
     * @return 是否为合法的邮箱格式
     */
    public static boolean isEmail(String email) {
        if (email == null || email.isEmpty() || email.length() > 254) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * 是否为合法的URL（支持 http、https、ftp 协议）。
     * <p>
     * 长度不超过2048字符。
     *
     * @param value URL字符串，不可为null
     * @return 是否为合法的URL格式
     */
    public static boolean isUrl(String value) {
        if (value == null || value.isEmpty() || value.length() > 2048) {
            return false;
        }
        return URL_PATTERN.matcher(value).matches();
    }

    /**
     * 是否为合法的IPv4地址。
     *
     * @param value IP地址字符串，不可为null
     * @return 是否为合法的IPv4地址
     */
    public static boolean isIpv4(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return IPV4_PATTERN.matcher(value).matches();
    }

    /**
     * 是否为合法的IPv6地址（标准8段全称格式）。
     * <p>
     * 不支持 :: 缩写格式。
     *
     * @param value IP地址字符串，不可为null
     * @return 是否为合法的IPv6地址
     */
    public static boolean isIpv6(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return IPV6_PATTERN.matcher(value).matches();
    }

    /**
     * 是否为合法的IP地址（自动识别IPv4或IPv6）。
     *
     * @param value IP地址字符串，不可为null
     * @return 是否为合法的IP地址
     */
    public static boolean isIp(String value) {
        return isIpv4(value) || isIpv6(value);
    }

    // ==================== 中国特有校验 ====================

    /**
     * 中国大陆居民身份证号码（18位）校验。
     * <p>
     * 校验规则（GB 11643-1999）：
     * <ol>
     *   <li>长度18位，前17位为数字，第18位为数字或X/x</li>
     *   <li>出生日期合法（1900~2100年）</li>
     *   <li>校验码正确（加权求和取模11）</li>
     * </ol>
     *
     * @param idCard 身份证号码，不可为null
     * @return 是否为合法的18位身份证号码
     */
    public static boolean isChinaIdCard(String idCard) {
        if (idCard == null || idCard.length() != 18) {
            return false;
        }
        for (int i = 0; i < 17; i++) {
            char c = idCard.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        char last = idCard.charAt(17);
        if (!((last >= '0' && last <= '9') || last == 'X' || last == 'x')) {
            return false;
        }
        int year = Integer.parseInt(idCard.substring(6, 10));
        int month = Integer.parseInt(idCard.substring(10, 12));
        int day = Integer.parseInt(idCard.substring(12, 14));
        if (!isValidDate(year, month, day)) {
            return false;
        }
        int[] weights = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        char[] checkCodes = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (idCard.charAt(i) - '0') * weights[i];
        }
        char expected = checkCodes[sum % 11];
        return Character.toUpperCase(last) == expected;
    }

    /**
     * 中国大陆手机号码校验。
     * <p>
     * 规则：1开头，第二位为3~9，共11位纯数字。
     * 覆盖当前已分配号段：13x, 14x, 15x, 16x, 17x, 18x, 19x。
     *
     * @param mobile 手机号码，不可为null
     * @return 是否为合法的大陆手机号
     */
    public static boolean isChinaMobile(String mobile) {
        if (mobile == null || mobile.length() != 11) {
            return false;
        }
        if (mobile.charAt(0) != '1') {
            return false;
        }
        char second = mobile.charAt(1);
        if (second < '3' || second > '9') {
            return false;
        }
        for (int i = 2; i < 11; i++) {
            char c = mobile.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    /**
     * 中文姓名校验。
     * <p>
     * 规则：2~20个中文字符（Unicode CJK统一表意文字），允许包含间隔号"·"（少数民族姓名）。
     * 不接受纯间隔号、连续间隔号或首尾间隔号。
     *
     * @param name 姓名，不可为null
     * @return 是否为合法的中文姓名
     */
    public static boolean isChinaName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (!CHINESE_NAME_PATTERN.matcher(name).matches()) {
            return false;
        }
        if (name.charAt(0) == '·' || name.charAt(name.length() - 1) == '·') {
            return false;
        }
        if (name.contains("··")) {
            return false;
        }
        return true;
    }

    /**
     * 统一社会信用代码校验（18位）。
     * <p>
     * 规则（GB 32100-2015）：长度18位，字符集为 0-9 和 A-Z（不含I、O、S、V），
     * 前17位加权求和取模31，第18位为校验码。
     *
     * @param uscc 统一社会信用代码，不可为null
     * @return 是否为合法的统一社会信用代码
     */
    public static boolean isChinaUscc(String uscc) {
        if (uscc == null || uscc.length() != 18) {
            return false;
        }
        String chars = "0123456789ABCDEFGHJKLMNPQRTUWXY";
        int[] weights = {1, 3, 9, 27, 19, 26, 16, 17, 20, 29, 25, 13, 8, 24, 10, 30, 28};
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            int idx = chars.indexOf(uscc.charAt(i));
            if (idx < 0) {
                return false;
            }
            sum += idx * weights[i];
        }
        int check = 31 - (sum % 31);
        char expected = chars.charAt(check == 31 ? 0 : check);
        return uscc.charAt(17) == expected;
    }

    /**
     * 中国车牌号校验（含新能源）。
     * <p>
     * 支持格式：
     * <ul>
     *   <li>普通蓝牌（7位）：省份简称 + 字母 + 5位字母数字（不含I、O）</li>
     *   <li>新能源车牌（8位）：省份简称 + 字母 + 6位字母数字</li>
     * </ul>
     *
     * @param plateNo 车牌号，不可为null
     * @return 是否为合法的车牌号
     */
    public static boolean isChinaPlateNo(String plateNo) {
        if (plateNo == null || plateNo.isEmpty()) {
            return false;
        }
        String provinces = "[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤川青藏琼宁]";
        String normalRegex = "^" + provinces + "[A-HJ-NP-Z][A-HJ-NP-Z0-9]{5}$";
        String newEnergyRegex = "^" + provinces + "[A-HJ-NP-Z][A-HJ-NP-Z0-9]{6}$";
        return plateNo.matches(normalRegex) || plateNo.matches(newEnergyRegex);
    }

    // ==================== 内部方法 ====================

    private static boolean isValidDate(int year, int month, int day) {
        if (year < 1900 || year > 2100) {
            return false;
        }
        if (month < 1 || month > 12) {
            return false;
        }
        int[] daysInMonth = {0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        if (month == 2 && isLeapYear(year)) {
            daysInMonth[2] = 29;
        }
        return day >= 1 && day <= daysInMonth[month];
    }

    private static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }
}
