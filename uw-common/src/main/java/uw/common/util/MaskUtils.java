package uw.common.util;

import java.util.Arrays;

/**
 * 数据脱敏工具类，对敏感信息（手机号、身份证、银行卡、邮箱、姓名、车牌、Token 等）做掩码处理。
 * <p>
 * 约定：
 * <ul>
 *   <li>所有方法对 {@code null} 输入返回 {@code null}，不抛异常。</li>
 *   <li>过短无法脱敏的输入（长度不足以保留明文片段时），整体替换为 {@link #FULL_MASK}。</li>
 *   <li>脱敏仅用于回显、日志、展示等非安全存储场景；不可作为加密手段。</li>
 * </ul>
 * <p>
 * 两类 API：
 * <ul>
 *   <li>通用 {@link #mask(String, int, int, String)} —— 中间填充<strong>固定掩码串</strong>（默认 {@link #FULL_MASK}），
 *       不随原文长度变化，不泄漏长度结构，适合凭证/密钥。</li>
 *   <li>业务语义化方法（{@link #maskChinaMobile(String)} 等）—— 中间<strong>按原文长度逐位填星</strong>，
 *       符合「138****5678」的直觉展示，适用于固定格式字段。</li>
 * </ul>
 * <p>
 * 中国特有脱敏方法以 {@code China} 前缀命名，与 {@link ValidateUtils} 保持一致，
 * 如 {@link #maskChinaMobile(String)}、{@link #maskChinaIdCard(String)}、{@link #maskChinaName(String)}。
 *
 * @see ValidateUtils
 */
public final class MaskUtils {

    /**
     * 整体脱敏占位符，用于输入过短无法保留明文片段时。
     */
    public static final String FULL_MASK = "****";

    private MaskUtils() {
    }

    // ==================== 通用脱敏（固定掩码串） ====================

    /**
     * 通用字符串脱敏：保留前 {@code keepPrefix} 位与后 {@code keepSuffix} 位，中间以 {@code mask} 填充。
     * <p>
     * 规则：
     * <ul>
     *   <li>{@code input} 为 {@code null} 返回 {@code null}。</li>
     *   <li>{@code keepPrefix}/{@code keepSuffix} 小于 0 按 0 处理。</li>
     *   <li>当 {@code keepPrefix + keepSuffix >= input.length()} 时（无法保留中间段），整体返回 {@link #FULL_MASK}。</li>
     *   <li>{@code mask} 为 {@code null} 时使用默认 {@link #FULL_MASK}。</li>
     * </ul>
     * <p>
     * 注意：中间填充的是<strong>固定掩码串</strong>，不随原文长度变化（不泄漏长度结构）。
     * 若需要「按原文长度逐位填星」（如手机号展示），请使用 {@link #maskByLength(String, int, int)}
     * 或对应的业务语义化方法。
     *
     * @param input      原始字符串
     * @param keepPrefix 保留前缀长度
     * @param keepSuffix 保留后缀长度
     * @param mask       中间填充的掩码字符串
     * @return 脱敏后的字符串
     */
    public static String mask(String input, int keepPrefix, int keepSuffix, String mask) {
        if (input == null) {
            return null;
        }
        int prefix = Math.max(keepPrefix, 0);
        int suffix = Math.max(keepSuffix, 0);
        int len = input.length();
        if (prefix + suffix >= len) {
            return FULL_MASK;
        }
        String middle = (mask == null) ? FULL_MASK : mask;
        StringBuilder sb = new StringBuilder(prefix + middle.length() + suffix);
        if (prefix > 0) {
            sb.append(input, 0, prefix);
        }
        sb.append(middle);
        if (suffix > 0) {
            sb.append(input, len - suffix, len);
        }
        return sb.toString();
    }

    /**
     * 通用字符串脱敏：保留前后指定位数，中间填充默认 {@link #FULL_MASK}。
     *
     * @param input      原始字符串
     * @param keepPrefix 保留前缀长度
     * @param keepSuffix 保留后缀长度
     * @return 脱敏后的字符串
     */
    public static String mask(String input, int keepPrefix, int keepSuffix) {
        return mask(input, keepPrefix, keepSuffix, FULL_MASK);
    }

    /**
     * 通用字符串脱敏：保留前 {@code keepPrefix} 位与后 {@code keepSuffix} 位，中间按原文长度逐位填星。
     * <p>
     * 示例：{@code maskByLength("13812345678", 3, 4) -> "138****5678"}，
     * 星号数量等于被掩码的字符数。
     *
     * @param input      原始字符串
     * @param keepPrefix 保留前缀长度
     * @param keepSuffix 保留后缀长度
     * @return 脱敏后的字符串
     */
    public static String maskByLength(String input, int keepPrefix, int keepSuffix) {
        if (input == null) {
            return null;
        }
        int prefix = Math.max(keepPrefix, 0);
        int suffix = Math.max(keepSuffix, 0);
        int len = input.length();
        if (prefix + suffix >= len) {
            return FULL_MASK;
        }
        int masked = len - prefix - suffix;
        StringBuilder sb = new StringBuilder(len);
        if (prefix > 0) {
            sb.append(input, 0, prefix);
        }
        for (int i = 0; i < masked; i++) {
            sb.append('*');
        }
        if (suffix > 0) {
            sb.append(input, len - suffix, len);
        }
        return sb.toString();
    }

    /**
     * 凭证类脱敏（Token / 密钥 / 密文等）：保留前4后4，中间固定掩码（不泄漏长度）。
     * 输入长度 ≤ 8 时整体返回 {@link #FULL_MASK}。
     *
     * @param secret 凭证字符串
     * @return 脱敏后的字符串
     */
    public static String maskSecret(String secret) {
        return mask(secret, 4, 4);
    }

    // ==================== 业务语义化脱敏（按原长填星） ====================

    /**
     * 中国大陆手机号脱敏：保留前3后4，中间4位逐位填星。
     * 示例：{@code 13812345678 -> 138****5678}。
     *
     * @param mobile 手机号
     * @return 脱敏后的手机号
     */
    public static String maskChinaMobile(String mobile) {
        return maskByLength(mobile, 3, 4);
    }

    /**
     * 固定电话脱敏：保留区号（前4位）与末尾2位，中间逐位填星。
     * 示例：{@code 01012345678 -> 0101****78}。
     *
     * @param telephone 固定电话
     * @return 脱敏后的固定电话
     */
    public static String maskTelephone(String telephone) {
        return maskByLength(telephone, 4, 2);
    }

    /**
     * 中国大陆身份证号脱敏（18位）：保留前6后4，中间8位逐位填星。
     * 示例：{@code 110101199001011234 -> 110101********1234}。
     *
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    public static String maskChinaIdCard(String idCard) {
        return maskByLength(idCard, 6, 4);
    }

    /**
     * 护照号脱敏：保留前2后2，中间逐位填星。
     * 示例：{@code E12345678 -> E1******78}。
     *
     * @param passport 护照号
     * @return 脱敏后的护照号
     */
    public static String maskPassport(String passport) {
        return maskByLength(passport, 2, 2);
    }

    /**
     * 中文姓名脱敏：保留首字，其余逐位填星。
     * 示例：{@code 张三 -> 张*}，{@code 欧阳修 -> 欧**}。
     *
     * @param name 姓名
     * @return 脱敏后的姓名
     */
    public static String maskChinaName(String name) {
        if (name == null) {
            return null;
        }
        if (name.length() <= 1) {
            return FULL_MASK;
        }
        return maskByLength(name, 1, 0);
    }

    /**
     * 银行卡号脱敏：保留前4后4，中间逐位填星。
     * 示例：{@code 6222021234567890 -> 6222********7890}。
     *
     * @param bankCard 银行卡号
     * @return 脱敏后的银行卡号
     */
    public static String maskBankCard(String bankCard) {
        return maskByLength(bankCard, 4, 4);
    }

    /**
     * 邮箱脱敏：{@code @} 前的本地部分保留首字符，其余逐位填星；域名完整保留。
     * 示例：{@code alice@example.com -> a****@example.com}，{@code ab@x.com -> a*@x.com}。
     * 无 {@code @} 或本地部分为空时整体返回 {@link #FULL_MASK}。
     *
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    public static String maskEmail(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            // 无 @ 或本地部分为空
            return FULL_MASK;
        }
        String local = email.substring(0, at);
        String domain = email.substring(at);
        // 本地部分保留首字符，其余填星；本地部分长度 1 时保留该字符。
        String maskedLocal = local.charAt(0) + repeat('*', local.length() - 1);
        return maskedLocal + domain;
    }

    /**
     * 中国车牌号脱敏：保留前2位（省份简称+字母），其余逐位填星。
     * 示例：{@code 京A12345 -> 京A*****}，{@code 京AD12345（新能源8位）-> 京A******}。
     *
     * @param plateNo 车牌号
     * @return 脱敏后的车牌号
     */
    public static String maskChinaPlateNo(String plateNo) {
        if (plateNo == null) {
            return null;
        }
        if (plateNo.length() <= 2) {
            return FULL_MASK;
        }
        return maskByLength(plateNo, 2, 0);
    }

    /**
     * 统一社会信用代码脱敏（18位）：保留前4后4，中间逐位填星。
     * 示例：{@code 911101081234561234 -> 9111**********1234}。
     *
     * @param uscc 统一社会信用代码
     * @return 脱敏后的统一社会信用代码
     */
    public static String maskChinaUscc(String uscc) {
        return maskByLength(uscc, 4, 4);
    }

    /**
     * 纳税人识别号（税号）脱敏：保留前4后4，中间逐位填星。
     * <p>
     * 中国纳税人识别号长度不固定（常见 15、18、20 位，企业常与统一社会信用代码相同），
     * 本方法按原长逐位填星，兼容各类长度。若确知为 18 位 USCC，可优先使用 {@link #maskChinaUscc(String)}。
     * 示例：{@code 911101081234561234 -> 9111**********1234}。
     *
     * @param taxNo 纳税人识别号
     * @return 脱敏后的纳税人识别号
     * @see #maskChinaUscc(String)
     */
    public static String maskChinaTaxNo(String taxNo) {
        return maskByLength(taxNo, 4, 4);
    }

    /**
     * 详细地址脱敏：保留前6位（通常含省市区关键信息），其余逐位填星。
     * <p>
     * 注意：地址脱敏粒度较粗，若需精确到「保留省市区」需结合具体地址格式。
     *
     * @param address 详细地址
     * @return 脱敏后的地址
     */
    public static String maskAddress(String address) {
        return maskByLength(address, 6, 0);
    }

    /**
     * IMEI（国际移动设备识别码）脱敏：保留前6（TAC 型号核准号）后2（备查尾号），中间逐位填星。
     * <p>
     * 兼容 15 位 IMEI 与 16 位 IMEISV。
     * 示例：{@code 490154203237518 -> 490154*******18}。
     *
     * @param imei IMEI
     * @return 脱敏后的 IMEI
     */
    public static String maskImei(String imei) {
        return maskByLength(imei, 6, 2);
    }

    /**
     * 微信号脱敏：保留首字符与末1位，中间逐位填星。
     * <p>
     * 微信号长度 6~20 位、字母开头，保留首字符符合社交展示惯例。
     * 示例：{@code alice_wx -> a******x}，{@code tom -> t*m}。
     * 长度 ≤ 2 时整体返回 {@link #FULL_MASK}。
     *
     * @param wechatId 微信号
     * @return 脱敏后的微信号
     */
    public static String maskWechatId(String wechatId) {
        if (wechatId == null) {
            return null;
        }
        if (wechatId.length() <= 2) {
            return FULL_MASK;
        }
        return maskByLength(wechatId, 1, 1);
    }

    /**
     * IPv4 地址脱敏：保留前两段，后两段替换为 {@code *.*}。
     * 示例：{@code 192.168.1.100 -> 192.168.*.*}。
     * 非标准 IPv4 格式（非 4 段数字）时整体返回 {@link #FULL_MASK}。
     *
     * @param ip IPv4 地址
     * @return 脱敏后的 IP
     */
    public static String maskIpv4(String ip) {
        if (ip == null) {
            return null;
        }
        String[] parts = ip.split("\\.", -1);
        if (parts.length != 4 || Arrays.stream(parts).anyMatch(p -> p.isEmpty() || !isDigits(p))) {
            return FULL_MASK;
        }
        return parts[0] + "." + parts[1] + ".*.*";
    }

    // ==================== 内部方法 ====================

    /**
     * 判断字符串是否全为数字。
     *
     * @param s 字符串
     * @return 全为数字返回 true
     */
    private static boolean isDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    /**
     * 重复字符 {@code n} 次。
     *
     * @param c 字符
     * @param n 重复次数，小于 0 按 0 处理
     * @return 由 {@code n} 个 {@code c} 组成的字符串
     */
    private static String repeat(char c, int n) {
        if (n <= 0) {
            return "";
        }
        char[] arr = new char[n];
        Arrays.fill(arr, c);
        return new String(arr);
    }
}
