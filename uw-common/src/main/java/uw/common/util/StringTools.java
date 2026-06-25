package uw.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * 字符串与数组/集合互转工具类。
 * <p>
 * 聚焦三类高频场景：
 * <ol>
 *   <li>数组/集合与字符串的 join / split（支持 long/int/String/Collection）；</li>
 *   <li>SQL 占位符与 IN 子句生成（仅服务端已清洗的白名单 ID，不接受外部未清洗输入）；</li>
 *   <li>常见但原生/StringUtils 用着啰嗦的零散补位：驼峰下划线互转、null 安全判等、随机串。</li>
 * </ol>
 * 所有方法空安全：null 输入不抛 NPE，按各方法约定返回空值。校验类方法请使用 {@link ValidateUtils}，
 * 脱敏请使用 {@link MaskUtils}，本类不重复实现。
 *
 * @author zhangjin
 */
public class StringTools {

    private static final Logger logger = LoggerFactory.getLogger(StringTools.class);

    /**
     * 默认分隔符：逗号。
     */
    private static final String DEFAULT_SEP = ",";

    /**
     * 空集合对应的 IN 子句兜底值，避免生成 "in ()" 导致 SQL 语法错误。
     */
    private static final String EMPTY_IN_CLAUSE = "(0)";

    /**
     * 随机字符集：大小写字母 + 数字。
     */
    private static final char[] ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    /**
     * 纯数字字符集：0~9。
     */
    private static final char[] NUMERIC = "0123456789".toCharArray();

    /**
     * 正则元字符集合，用于escapeRegex。
     */
    private static final String REGEX_META = ".\\*+?()[]{}|$^";

    /**
     * 文件名非法字符集合，用于safeFileName。
     */
    private static final String ILLEGAL_FILENAME = "<>:\"/\\|?*";

    /**
     * 安全随机数生成器，用于随机字符串场景。
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // ==================== Join：数组/集合 -> 字符串 ====================

    /**
     * 将long数组用逗号拼接为字符串。null或空数组返回空串。
     *
     * @param arr
     * @return
     */
    public static String join(long[] arr) {
        return join(arr, DEFAULT_SEP);
    }

    /**
     * 将long数组用指定分隔符拼接为字符串。null或空数组返回空串。
     *
     * @param arr
     * @param sep
     * @return
     */
    public static String join(long[] arr, String sep) {
        if (arr == null || arr.length == 0) {
            return "";
        }
        String s = sep == null ? DEFAULT_SEP : sep;
        StringBuilder sb = new StringBuilder(arr.length * 4);
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) {
                sb.append(s);
            }
            sb.append(arr[i]);
        }
        return sb.toString();
    }


    /**
     * 将int数组用逗号拼接为字符串。null或空数组返回空串。
     *
     * @param arr
     * @return
     */
    public static String join(int[] arr) {
        return join(arr, DEFAULT_SEP);
    }

    /**
     * 将int数组用指定分隔符拼接为字符串。null或空数组返回空串。
     *
     * @param arr
     * @param sep
     * @return
     */
    public static String join(int[] arr, String sep) {
        if (arr == null || arr.length == 0) {
            return "";
        }
        String s = sep == null ? DEFAULT_SEP : sep;
        StringBuilder sb = new StringBuilder(arr.length * 4);
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) {
                sb.append(s);
            }
            sb.append(arr[i]);
        }
        return sb.toString();
    }


    /**
     * 将String数组用逗号拼接为字符串。null或空数组返回空串，元素为null时跳过。
     *
     * @param arr
     * @return
     */
    public static String join(String[] arr) {
        return join(arr, DEFAULT_SEP);
    }

    /**
     * 将String数组用指定分隔符拼接为字符串。null或空数组返回空串，元素为null时跳过。
     *
     * @param arr
     * @param sep
     * @return
     */
    public static String join(String[] arr, String sep) {
        if (arr == null || arr.length == 0) {
            return "";
        }
        String s = sep == null ? DEFAULT_SEP : sep;
        StringBuilder sb = new StringBuilder(arr.length * 8);
        boolean first = true;
        for (String e : arr) {
            if (e == null) {
                continue;
            }
            if (!first) {
                sb.append(s);
            }
            sb.append(e);
            first = false;
        }
        return sb.toString();
    }

    /**
     * 将集合用逗号拼接为字符串。null或空集合返回空串，元素为null时跳过。
     *
     * @param coll
     * @return
     */
    public static String join(Collection<?> coll) {
        return join(coll, DEFAULT_SEP);
    }

    /**
     * 将集合用指定分隔符拼接为字符串。null或空集合返回空串，元素为null时跳过。
     *
     * @param coll
     * @param sep
     * @return
     */
    public static String join(Collection<?> coll, String sep) {
        if (coll == null || coll.isEmpty()) {
            return "";
        }
        String s = sep == null ? DEFAULT_SEP : sep;
        StringBuilder sb = new StringBuilder(coll.size() * 8);
        boolean first = true;
        for (Object e : coll) {
            if (e == null) {
                continue;
            }
            if (!first) {
                sb.append(s);
            }
            sb.append(e);
            first = false;
        }
        return sb.toString();
    }

    /**
     * 拼接Map为字符串，如 k1=v1&k2=v2。map为null/空返回空串，自动跳过null的key/value。
     * 使用LinkedHashMap顺序时按插入序输出。
     * <p>
     * 注意：方法名独立为joinMap而非join重载，避免与 {@link #join(Collection, String)}
     * 在多参String签名上产生重载歧义。
     *
     * @param map
     * @param entrySep
     * @param kvSep
     * @return
     */
    public static String joinMap(Map<?, ?> map, String entrySep, String kvSep) {
        if (map == null || map.isEmpty()) {
            return "";
        }
        String es = entrySep == null ? DEFAULT_SEP : entrySep;
        String ks = kvSep == null ? "=" : kvSep;
        StringBuilder sb = new StringBuilder(map.size() * 8);
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object k = entry.getKey();
            Object v = entry.getValue();
            if (k == null || v == null) {
                continue;
            }
            if (!first) {
                sb.append(es);
            }
            sb.append(k).append(ks).append(v);
            first = false;
        }
        return sb.toString();
    }

    // ==================== Split：字符串 -> 数组/集合 ====================

    /**
     * 将字符串按逗号/空白切分为long数组。null或空串返回空数组，自动trim并跳过空白段，
     * 无法解析为数字的脏数据逐条跳过并记WARN。
     *
     * @param s
     * @return
     */
    public static long[] splitToLongArray(String s) {
        return splitToLongArray(s, DEFAULT_SEP);
    }

    /**
     * 将字符串按指定分隔符切分为long数组。null或空串返回空数组，自动trim并跳过空白段，
     * 无法解析为数字的脏数据逐条跳过并记WARN。
     *
     * @param s
     * @param sep
     * @return
     */
    public static long[] splitToLongArray(String s, String sep) {
        if (s == null || s.isEmpty()) {
            return new long[0];
        }
        List<String> tokens = splitTokens(s, sep);
        long[] arr = new long[tokens.size()];
        int n = 0;
        for (String t : tokens) {
            try {
                // 先解析成功再写入并递增，避免parseLong抛异常时n被提前自增
                long val = Long.parseLong(t);
                arr[n++] = val;
            } catch (NumberFormatException e) {
                logger.warn("splitToLongArray skip invalid token, value={}", t);
            }
        }
        // 脏数据跳过后实际元素可能少于tokens数，裁剪
        if (n < arr.length) {
            long[] trimmed = new long[n];
            System.arraycopy(arr, 0, trimmed, 0, n);
            return trimmed;
        }
        return arr;
    }

    /**
     * 将字符串按逗号/空白切分为int数组。null或空串返回空数组，自动trim并跳过空白段，
     * 无法解析为数字的脏数据逐条跳过并记WARN。
     *
     * @param s
     * @return
     */
    public static int[] splitToIntArray(String s) {
        return splitToIntArray(s, DEFAULT_SEP);
    }

    /**
     * 将字符串按指定分隔符切分为int数组。null或空串返回空数组，自动trim并跳过空白段，
     * 无法解析为数字的脏数据逐条跳过并记WARN。
     *
     * @param s
     * @param sep
     * @return
     */
    public static int[] splitToIntArray(String s, String sep) {
        if (s == null || s.isEmpty()) {
            return new int[0];
        }
        List<String> tokens = splitTokens(s, sep);
        int[] arr = new int[tokens.size()];
        int n = 0;
        for (String t : tokens) {
            try {
                int val = Integer.parseInt(t);
                arr[n++] = val;
            } catch (NumberFormatException e) {
                logger.warn("splitToIntArray skip invalid token, value={}", t);
            }
        }
        if (n < arr.length) {
            int[] trimmed = new int[n];
            System.arraycopy(arr, 0, trimmed, 0, n);
            return trimmed;
        }
        return arr;
    }

    /**
     * 将字符串按逗号/空白切分为String数组。null或空串返回空数组，自动trim并跳过空白段。
     *
     * @param s
     * @return
     */
    public static String[] splitToStringArray(String s) {
        return splitToStringArray(s, DEFAULT_SEP);
    }

    /**
     * 将字符串按指定分隔符切分为String数组。null或空串返回空数组，自动trim并跳过空白段。
     *
     * @param s
     * @param sep
     * @return
     */
    public static String[] splitToStringArray(String s, String sep) {
        List<String> list = splitToStringList(s, sep);
        return list.toArray(new String[0]);
    }

    /**
     * 将字符串按逗号/空白切分为Long列表。null或空串返回空列表，自动trim并跳过空白段，
     * 无法解析为数字的脏数据逐条跳过并记WARN。
     *
     * @param s
     * @return
     */
    public static List<Long> splitToLongList(String s) {
        return splitToLongList(s, DEFAULT_SEP);
    }

    /**
     * 将字符串按指定分隔符切分为Long列表。null或空串返回空列表，自动trim并跳过空白段，
     * 无法解析为数字的脏数据逐条跳过并记WARN。
     *
     * @param s
     * @param sep
     * @return
     */
    public static List<Long> splitToLongList(String s, String sep) {
        if (s == null || s.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> tokens = splitTokens(s, sep);
        List<Long> list = new ArrayList<>(tokens.size());
        for (String t : tokens) {
            try {
                list.add(Long.parseLong(t));
            } catch (NumberFormatException e) {
                logger.warn("splitToLongList skip invalid token, value={}", t);
            }
        }
        return list;
    }

    /**
     * 将字符串按逗号/空白切分为Integer列表。null或空串返回空列表，自动trim并跳过空白段，
     * 无法解析为数字的脏数据逐条跳过并记WARN。
     *
     * @param s
     * @return
     */
    public static List<Integer> splitToIntList(String s) {
        return splitToIntList(s, DEFAULT_SEP);
    }

    /**
     * 将字符串按指定分隔符切分为Integer列表。null或空串返回空列表，自动trim并跳过空白段，
     * 无法解析为数字的脏数据逐条跳过并记WARN。
     *
     * @param s
     * @param sep
     * @return
     */
    public static List<Integer> splitToIntList(String s, String sep) {
        if (s == null || s.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> tokens = splitTokens(s, sep);
        List<Integer> list = new ArrayList<>(tokens.size());
        for (String t : tokens) {
            try {
                list.add(Integer.parseInt(t));
            } catch (NumberFormatException e) {
                logger.warn("splitToIntList skip invalid token, value={}", t);
            }
        }
        return list;
    }

    /**
     * 将字符串按逗号/空白切分为String列表。null或空串返回空列表，自动trim并跳过空白段。
     *
     * @param s
     * @return
     */
    public static List<String> splitToStringList(String s) {
        return splitToStringList(s, DEFAULT_SEP);
    }

    /**
     * 将字符串按指定分隔符切分为String列表。null或空串返回空列表，自动trim并跳过空白段。
     *
     * @param s
     * @param sep
     * @return
     */
    public static List<String> splitToStringList(String s, String sep) {
        if (s == null || s.isEmpty()) {
            return new ArrayList<>();
        }
        return splitTokens(s, sep);
    }

    /**
     * 按单字符分隔符切分（避免正则意外，分隔符为普通char）。omitEmpty为true时跳过空段。
     * str为null返回空列表。
     *
     * @param str
     * @param separator
     * @param omitEmpty
     * @return
     */
    public static List<String> split(String str, char separator, boolean omitEmpty) {
        if (str == null) {
            return new ArrayList<>();
        }
        List<String> list = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == separator) {
                String seg = str.substring(start, i);
                if (!omitEmpty || !seg.isEmpty()) {
                    list.add(seg);
                }
                start = i + 1;
            }
        }
        String last = str.substring(start);
        if (!omitEmpty || !last.isEmpty()) {
            list.add(last);
        }
        return list;
    }

    /**
     * 按单字符分隔符切分，最多分割limit段（剩余不再切分），如CSV首列限制。
     * str为null返回空列表，limit<=0表示不限制。
     *
     * @param str
     * @param separator
     * @param limit
     * @return
     */
    public static List<String> split(String str, char separator, int limit) {
        if (str == null) {
            return new ArrayList<>();
        }
        if (limit <= 0) {
            return split(str, separator, false);
        }
        List<String> list = new ArrayList<>(limit);
        int start = 0;
        for (int i = 0; i < str.length() && list.size() < limit - 1; i++) {
            if (str.charAt(i) == separator) {
                list.add(str.substring(start, i));
                start = i + 1;
            }
        }
        list.add(str.substring(start));
        return list;
    }

    /**
     * 按行切分，统一处理 \n / \r\n / \r。空行保留。str为null返回空列表。
     *
     * @param str
     * @return
     */
    public static List<String> splitLines(String str) {
        if (str == null || str.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> list = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\n') {
                int end = i;
                if (end > start && str.charAt(end - 1) == '\r') {
                    end--;
                }
                list.add(str.substring(start, end));
                start = i + 1;
            } else if (c == '\r' && (i + 1 >= str.length() || str.charAt(i + 1) != '\n')) {
                list.add(str.substring(start, i));
                start = i + 1;
            }
        }
        // 末尾有内容才作为最后一行，避免换行结尾时多出空尾行
        if (start < str.length()) {
            list.add(str.substring(start));
        }
        return list;
    }

    /**
     * 将字符串解析为Map，是 {@link #joinMap} 的逆操作。如 "k1=v1&k2=v2" -> {k1=v1, k2=v2}。
     * str为null/空返回空Map（LinkedHashMap，保持解析顺序）。
     * <p>
     * 解析规则：先按entrySep切分条目，每个条目按kvSep切分为key/value；
     * 条目中无kvSep的视为key、value为空串；kvSep以第一次出现为准（value可含kvSep字符）；
     * 重复key后者覆盖前者。
     *
     * @param str
     * @param entrySep 条目分隔符，null用默认逗号
     * @param kvSep    key/value分隔符，null用"="
     * @return
     */
    public static Map<String, String> splitMap(String str, String entrySep, String kvSep) {
        Map<String, String> map = new LinkedHashMap<>();
        if (str == null || str.isEmpty()) {
            return map;
        }
        String es = entrySep == null ? DEFAULT_SEP : entrySep;
        String ks = kvSep == null ? "=" : kvSep;
        List<String> tokens = splitTokens(str, es);
        for (String token : tokens) {
            int idx = token.indexOf(ks);
            if (idx < 0) {
                // 无kvSep：key为整个token，value为空串
                map.put(token, "");
            } else {
                map.put(token.substring(0, idx), token.substring(idx + ks.length()));
            }
        }
        return map;
    }

    // ==================== 占位符与 IN 子句 ====================

    /**
     * 生成count个SQL占位符，逗号分隔，如 "?,?,?"。count<=0返回空串。
     *
     * @param count
     * @return
     */
    public static String buildPlaceholders(int count) {
        return buildPlaceholders(count, "?", DEFAULT_SEP);
    }

    /**
     * 生成count个SQL占位符，可自定义占位符与分隔符。count<=0返回空串。
     *
     * @param count
     * @param placeholder
     * @param sep
     * @return
     */
    public static String buildPlaceholders(int count, String placeholder, String sep) {
        if (count <= 0) {
            return "";
        }
        String p = placeholder == null ? "?" : placeholder;
        String s = sep == null ? DEFAULT_SEP : sep;
        char[] ph = p.toCharArray();
        char[] sc = s.toCharArray();
        // 用long计算容量避免count*unit溢出成负数导致NegativeArraySizeException
        long total = (long) count * (ph.length + sc.length) - sc.length;
        if (total > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("buildPlaceholders count too large: " + count);
        }
        char[] buf = new char[(int) total];
        int pos = 0;
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                System.arraycopy(sc, 0, buf, pos, sc.length);
                pos += sc.length;
            }
            System.arraycopy(ph, 0, buf, pos, ph.length);
            pos += ph.length;
        }
        return new String(buf);
    }

    /**
     * 根据long数组生成 IN 子句，如 "(1,2,3)"。null或空数组返回 "(0)" 避免SQL语法错误。
     * <p>
     * 注意：仅用于服务端已清洗的白名单ID，不接受外部未清洗输入。
     *
     * @param ids
     * @return
     */
    public static String buildInClause(long[] ids) {
        if (ids == null || ids.length == 0) {
            return EMPTY_IN_CLAUSE;
        }
        return "(" + join(ids, DEFAULT_SEP) + ")";
    }

    /**
     * 根据集合生成 IN 子句，如 "(1,2,3)"。null或空集合返回 "(0)" 避免SQL语法错误。
     * <p>
     * 注意：仅用于服务端已清洗的白名单ID，不接受外部未清洗输入。
     *
     * @param coll
     * @return
     */
    public static String buildInClause(Collection<?> coll) {
        if (coll == null || coll.isEmpty()) {
            return EMPTY_IN_CLAUSE;
        }
        return "(" + join(coll, DEFAULT_SEP) + ")";
    }

    /**
     * 在joined首尾各补一个分隔符，如 surround("1,2,3", ",") -> ",1,2,3,"。
     * joined为null或空串时原样返回，避免产生孤立分隔符；sep为null时用默认逗号。
     * 常用于把join结果加工成SQL/MQ匹配所需的两侧带分隔符形态。
     *
     * @param joined
     * @param sep
     * @return
     */
    public static String surround(String joined, String sep) {
        if (joined == null || joined.isEmpty()) {
            return joined == null ? "" : joined;
        }
        String s = sep == null ? DEFAULT_SEP : sep;
        return s + joined + s;
    }

    // ==================== 驼峰 / 下划线 / 大小写 ====================

    /**
     * 首字母大写。null返回空串。
     *
     * @param s
     * @return
     */
    public static String capFirst(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        char c = s.charAt(0);
        if (Character.isUpperCase(c)) {
            return s;
        }
        return Character.toUpperCase(c) + s.substring(1);
    }

    /**
     * 首字母小写。null返回空串。
     *
     * @param s
     * @return
     */
    public static String uncapFirst(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        char c = s.charAt(0);
        if (Character.isLowerCase(c)) {
            return s;
        }
        return Character.toLowerCase(c) + s.substring(1);
    }

    /**
     * 按自定义分隔符转驼峰，如 user-name -> userName（separator='-'）。
     * 同时做大小写规范化（首段全小写，其余段首字母大写），如 USER-NAME -> userName。null返回空串。
     *
     * @param str
     * @param separator
     * @return
     */
    public static String toCamelCase(String str, char separator) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        // 按指定分隔符切分，再做大小写规范化
        String sep = String.valueOf(separator);
        List<String> tokens = new ArrayList<>();
        for (String part : str.split(Pattern.quote(sep), -1)) {
            if (!part.isEmpty()) {
                tokens.add(part.toLowerCase(Locale.ROOT));
            }
        }
        return formatCamel(tokens, false);
    }

    /**
     * 转蛇形（下划线），如 userName -> user_name, HTTPResponse -> http_response。
     * 自动识别驼峰大小写边界与常见分隔符(- . / 空格)，源格式无需指定。null返回空串。
     *
     * @param str
     * @return
     */
    public static String toSnakeCase(String str) {
        return convertCase(str, CaseStyle.SNAKE);
    }

    /**
     * 转短横线命名，如 userName -> user-name。源格式自动识别。null返回空串。
     *
     * @param str
     * @return
     */
    public static String toKebabCase(String str) {
        return convertCase(str, CaseStyle.KEBAB);
    }

    /**
     * 转Pascal命名（首字母大写驼峰），如 user_name -> UserName。源格式自动识别。null返回空串。
     *
     * @param str
     * @return
     */
    public static String toPascalCase(String str) {
        return convertCase(str, CaseStyle.PASCAL);
    }

    /**
     * 转大写蛇形（C常量风格），如 userName -> USER_NAME。源格式自动识别。null返回空串。
     *
     * @param str
     * @return
     */
    public static String toMacroCase(String str) {
        return convertCase(str, CaseStyle.MACRO);
    }

    /**
     * 转Train命名（HTTP Header风格），如 userName -> User-Name。源格式自动识别。null返回空串。
     *
     * @param str
     * @return
     */
    public static String toTrainCase(String str) {
        return convertCase(str, CaseStyle.TRAIN);
    }

    /**
     * 转点号命名（配置文件风格），如 userName -> user.name。源格式自动识别。null返回空串。
     *
     * @param str
     * @return
     */
    public static String toDotCase(String str) {
        return convertCase(str, CaseStyle.DOT);
    }

    /**
     * 转路径命名（路由风格），如 userName -> user/name。源格式自动识别。null返回空串。
     *
     * @param str
     * @return
     */
    public static String toPathCase(String str) {
        return convertCase(str, CaseStyle.PATH);
    }

    /**
     * 转扁平命名（无分隔符小写），如 userName -> username。源格式自动识别。null返回空串。
     *
     * @param str
     * @return
     */
    public static String toFlatCase(String str) {
        return convertCase(str, CaseStyle.FLAT);
    }

    /**
     * 命名风格互转入口，自动探测源格式后按to风格输出。
     * 探测规则：所有分隔符(-_. /)视为边界，并按大小写边界切分驼峰/Pascal，
     * 能正确处理HTTPResponse等连续大写缩写。
     * str为null返回空串，to为null原样返回。
     *
     * @param str
     * @param to    目标风格
     * @return
     */
    public static String convertCase(String str, CaseStyle to) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        if (to == null) {
            return str;
        }
        List<String> tokens = tokenize(str);
        return format(tokens, to);
    }

    /**
     * 自动探测分词：所有分隔符(-_. /)视为边界，并按大小写边界切分驼峰/Pascal，
     * 正确处理连续大写缩写（如HTTPResponse -> [http, response]，userID -> [user, id]）。
     *
     * @param str
     * @return
     */
    private static List<String> tokenize(String str) {
        List<String> tokens = new ArrayList<>();
        // 先按显式分隔符切：已知分隔符都视为边界
        // FLAT 无分隔符，只能靠大小写边界；其它风格的分隔符统一用正则切
        String[] parts = str.split("[-_./ ]+", -1);
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            // 再按驼峰/Pascal的大小写边界细切（处理 camelCase 与 HTTPResponse）
            splitCamel(part, tokens);
        }
        return tokens;
    }

    /**
     * 按大小写边界把单个token进一步切分，并入tokens。处理连续大写缩写：
     * HTTPResponse -> [HTTP, Response]，userID -> [user, ID]，XMLParser -> [XML, Parser]。
     *
     * @param token
     * @param tokens
     */
    private static void splitCamel(String token, List<String> tokens) {
        int start = 0;
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            boolean upper = Character.isUpperCase(c);
            // 边界1：小写/数字 -> 大写（userN 的 N 前切分）
            if (i > start && upper && !Character.isUpperCase(token.charAt(i - 1))) {
                tokens.add(token.substring(start, i).toLowerCase(Locale.ROOT));
                start = i;
            }
            // 边界2：大写序列后跟小写（HTTPR 的 R 前切分，保留 HTTP）
            else if (i > start && i + 1 < token.length() && upper
                    && Character.isUpperCase(token.charAt(i - 1))
                    && Character.isLowerCase(token.charAt(i + 1))) {
                tokens.add(token.substring(start, i).toLowerCase(Locale.ROOT));
                start = i;
            }
        }
        if (start < token.length()) {
            tokens.add(token.substring(start).toLowerCase(Locale.ROOT));
        }
    }

    /**
     * 按to风格把token列表格式化为字符串。
     *
     * @param tokens
     * @param to
     * @return
     */
    private static String format(List<String> tokens, CaseStyle to) {
        if (tokens.isEmpty()) {
            return "";
        }
        switch (to) {
            case CAMEL:
                return formatCamel(tokens, false);
            case PASCAL:
                return formatCamel(tokens, true);
            case SNAKE:
                return String.join("_", tokens);
            case MACRO:
                return String.join("_", tokens).toUpperCase(Locale.ROOT);
            case KEBAB:
                return String.join("-", tokens);
            case TRAIN:
                return formatTrain(tokens);
            case DOT:
                return String.join(".", tokens);
            case PATH:
                return String.join("/", tokens);
            case FLAT:
                return String.join("", tokens);
            default:
                return String.join("_", tokens);
        }
    }

    /**
     * 格式化为驼峰/Pascal：首单词原样小写(或Pascal首字母大写)，后续单词首字母大写。
     *
     * @param tokens
     * @param pascal
     * @return
     */
    private static String formatCamel(List<String> tokens, boolean pascal) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            String w = tokens.get(i);
            if (i == 0 && !pascal) {
                sb.append(w.toLowerCase(Locale.ROOT));
            } else {
                sb.append(capFirst(w));
            }
        }
        return sb.toString();
    }

    /**
     * 格式化为Train：每单词首字母大写，用-连接。
     *
     * @param tokens
     * @return
     */
    private static String formatTrain(List<String> tokens) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            if (i > 0) {
                sb.append('-');
            }
            sb.append(capFirst(tokens.get(i)));
        }
        return sb.toString();
    }

    // ==================== null 安全 trim / 判等 ====================

    /**
     * trim字符串，null返回空串。
     *
     * @param s
     * @return
     */
    public static String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * trim字符串，空白返回null。
     *
     * @param s
     * @return
     */
    public static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * null安全的等值判断，两个null视为相等。
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean equals(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    /**
     * null安全的忽略大小写等值判断，两个null视为相等。
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean equalsIgnoreCase(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equalsIgnoreCase(b);
    }

    // ==================== 默认值 ====================

    /**
     * 空白则返回默认值，比defaultString更严格（空白字符也算空）。str非blank则原样返回。
     *
     * @param str
     * @param defaultStr
     * @return
     */
    public static String defaultIfBlank(String str, String defaultStr) {
        return (str == null || str.isBlank()) ? defaultStr : str;
    }

    /**
     * 空白则惰性求值默认值，避免默认值本身有性能开销。
     *
     * @param str
     * @param supplier
     * @return
     */
    public static String defaultIfBlank(String str, Supplier<String> supplier) {
        return (str == null || str.isBlank()) ? (supplier == null ? null : supplier.get()) : str;
    }

    /**
     * 空串则惰性求值默认值。注意：仅判空串，空白字符串（如"  "）不算空。
     *
     * @param str
     * @param supplier
     * @return
     */
    public static String defaultIfEmpty(String str, Supplier<String> supplier) {
        return (str == null || str.isEmpty()) ? (supplier == null ? null : supplier.get()) : str;
    }

    // ==================== 截断 / 填充 ====================

    /**
     * 超长截断并追加后缀（如"..."）。str为null返回空串，maxLength<=suffix长度时直接返回后缀或空串。
     *
     * @param str
     * @param maxLength
     * @param suffix
     * @return
     */
    public static String truncate(String str, int maxLength, String suffix) {
        if (str == null) {
            return "";
        }
        String suf = suffix == null ? "" : suffix;
        if (maxLength <= 0) {
            return "";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        int keep = maxLength - suf.length();
        if (keep <= 0) {
            return suf.length() <= maxLength ? suf : "";
        }
        return str.substring(0, keep) + suf;
    }

    /**
     * 按显示宽度截断（中文/全角算2宽度，其余算1宽度）。maxWidth<=0返回空串。
     *
     * @param str
     * @param maxWidth
     * @return
     */
    public static String truncateByWidth(String str, int maxWidth) {
        if (str == null || maxWidth <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int width = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            int w = charWidth(c);
            if (width + w > maxWidth) {
                break;
            }
            sb.append(c);
            width += w;
        }
        return sb.toString();
    }

    /**
     * 向前填充padChar直到达到minLength。str为null返回空串（不填充）。
     *
     * @param str
     * @param minLength
     * @param padChar
     * @return
     */
    public static String padStart(String str, int minLength, char padChar) {
        if (str == null) {
            return "";
        }
        if (str.length() >= minLength || minLength <= 0) {
            return str;
        }
        StringBuilder sb = new StringBuilder(minLength);
        int pad = minLength - str.length();
        for (int i = 0; i < pad; i++) {
            sb.append(padChar);
        }
        return sb.append(str).toString();
    }

    /**
     * 向后填充padChar直到达到minLength。str为null返回空串（不填充）。
     *
     * @param str
     * @param minLength
     * @param padChar
     * @return
     */
    public static String padEnd(String str, int minLength, char padChar) {
        if (str == null) {
            return "";
        }
        if (str.length() >= minLength || minLength <= 0) {
            return str;
        }
        StringBuilder sb = new StringBuilder(minLength).append(str);
        int pad = minLength - str.length();
        for (int i = 0; i < pad; i++) {
            sb.append(padChar);
        }
        return sb.toString();
    }

    // ==================== 格式化 ====================

    /**
     * 命名参数模板，占位符形如${name}。未提供的key原样保留，null值替换为空串。
     *
     * @param template
     * @param params
     * @return
     */
    public static String format(String template, Map<String, Object> params) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        if (params == null || params.isEmpty() || template.indexOf("${") < 0) {
            return template;
        }
        StringBuilder sb = new StringBuilder(template.length());
        int i = 0;
        while (i < template.length()) {
            int start = template.indexOf("${", i);
            if (start < 0) {
                sb.append(template, i, template.length());
                break;
            }
            int end = template.indexOf('}', start + 2);
            if (end < 0) {
                sb.append(template, i, template.length());
                break;
            }
            sb.append(template, i, start);
            String key = template.substring(start + 2, end);
            if (params.containsKey(key)) {
                Object v = params.get(key);
                sb.append(v == null ? "" : v.toString());
            } else {
                // 未提供的key原样保留
                sb.append(template, start, end + 1);
            }
            i = end + 1;
        }
        return sb.toString();
    }

    /**
     * 简化版String.format，null安全：模板为null返回空串，参数含null时按"null"渲染。
     * 占位符规则与String.format一致（%s/%d等）。
     *
     * @param template
     * @param args
     * @return
     */
    public static String format(String template, Object... args) {
        if (template == null) {
            return "";
        }
        if (args == null || args.length == 0) {
            return template;
        }
        return String.format(template, args);
    }

    // ==================== 清理 / 规范化 ====================

    /**
     * 清理不可见字符：零宽字符、BOM、全角空格及各类控制字符。str为null返回空串。
     * 标准空白（普通空格/制表/换行）保留，如需合并请用{@link #normalizeSpace}。
     *
     * @param str
     * @return
     */
    public static String clean(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            // 全角空格(U+3000)替换为普通空格
            if (c == '\u3000') {
                sb.append(' ');
                continue;
            }
            // BOM (U+FEFF)
            if (c == '\uFEFF') {
                continue;
            }
            // 零宽字符：ZWSP(U+200B)/ZWNJ(U+200C)/ZWJ(U+200D)/Word Joiner(U+2060)
            if (c == '\u200B' || c == '\u200C' || c == '\u200D' || c == '\u2060') {
                continue;
            }
            // 除标准空白外的控制字符：C0控制(0x00-0x1F)、DEL(0x7F)、C1控制(0x80-0x9F)
            if ((c <= '\u001F' || c == '\u007F' || (c >= '\u0080' && c <= '\u009F'))
                    && c != '\t' && c != '\n' && c != '\r') {
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * 合并连续空白为单个空格并trim首尾。str为null返回空串。
     *
     * @param str
     * @return
     */
    public static String normalizeSpace(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(str.length());
        boolean inSpace = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c <= ' ') {
                if (!inSpace && sb.length() > 0) {
                    sb.append(' ');
                    inSpace = true;
                }
            } else {
                sb.append(c);
                inSpace = false;
            }
        }
        // 去尾
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ' ') {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * 全角字符转半角。ＡＢＣ１２３ -> ABC123，用于表单输入统一。str为null返回空串。
     * 处理：全角ASCII(0xFF01-0xFF5E)偏移0xFEE0、全角空格(U+3000)->普通空格。
     *
     * @param str
     * @return
     */
    public static String toHalfWidth(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        char[] cs = str.toCharArray();
        for (int i = 0; i < cs.length; i++) {
            char c = cs[i];
            if (c == '　') {
                cs[i] = ' ';
            } else if (c >= '！' && c <= '～') {
                cs[i] = (char) (c - 0xFEE0);
            }
        }
        return new String(cs);
    }

    /**
     * 半角ASCII转全角。ABC123 -> ＡＢＣ１２３，用于中日文排版对齐。str为null返回空串。
     *
     * @param str
     * @return
     */
    public static String toFullWidth(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        char[] cs = str.toCharArray();
        for (int i = 0; i < cs.length; i++) {
            char c = cs[i];
            if (c == ' ') {
                cs[i] = '　';
            } else if (c >= '!' && c <= '~') {
                cs[i] = (char) (c + 0xFEE0);
            }
        }
        return new String(cs);
    }

    /**
     * 提取字符串中所有CJK中日韩文字。str为null返回空串。
     *
     * @param str
     * @return
     */
    public static String extractChinese(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (isCJKChar(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 判断字符串是否包含中日韩文字。str为null返回false。
     *
     * @param str
     * @return
     */
    public static boolean hasCJK(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (isCJKChar(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    // ==================== 随机字符串 ====================

    /**
     * 生成指定长度的纯数字随机串（基于SecureRandom），用于验证码等。
     * len<=0返回空串。
     *
     * @param len
     * @return
     */
    public static String randomNumeric(int len) {
        return random(len, NUMERIC);
    }

    /**
     * 生成指定长度的大小写字母+数字随机串（基于SecureRandom），用于token/盐等。
     * len<=0返回空串。
     *
     * @param len
     * @return
     */
    public static String randomAlphanumeric(int len) {
        return random(len, ALPHANUMERIC);
    }

    /**
     * 从指定字符集中生成指定长度的随机串（基于SecureRandom）。len<=0或字符集为空返回空串。
     *
     * @param len
     * @param chars
     * @return
     */
    public static String random(int len, char[] chars) {
        if (len <= 0 || chars == null || chars.length == 0) {
            return "";
        }
        char[] buf = new char[len];
        for (int i = 0; i < len; i++) {
            buf[i] = chars[SECURE_RANDOM.nextInt(chars.length)];
        }
        return new String(buf);
    }

    /**
     * 生成随机中文字符串（CJK常用汉字区U+4E00-U+9FA5），用于测试数据生成。len<=0返回空串。
     *
     * @param len
     * @return
     */
    public static String randomChinese(int len) {
        if (len <= 0) {
            return "";
        }
        char[] buf = new char[len];
        for (int i = 0; i < len; i++) {
            // CJK统一表意文字常用区
            buf[i] = (char) ('一' + SECURE_RANDOM.nextInt('鿿' - '一' + 1));
        }
        return new String(buf);
    }

    /**
     * 生成无横线的简单UUID（32位hex）。基于UUID.randomUUID()。
     *
     * @return
     */
    public static String randomUuidSimple() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成URL安全的随机字符串。先取随机字节再用Base64(URL safe)编码并去填充符。
     * length为输出的近似字节数（Base64会膨胀约4/3），最终长度可能略大于length。
     * length<=0返回空串。
     *
     * @param length
     * @return
     */
    public static String randomToken(int length) {
        if (length <= 0) {
            return "";
        }
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // ==================== 包含 / 计数 / 前后缀 ====================

    /**
     * 是否包含任意一个子串。str为null或searchStrs为空返回false。
     *
     * @param str
     * @param searchStrs
     * @return
     */
    public static boolean containsAny(String str, String... searchStrs) {
        if (str == null || searchStrs == null) {
            return false;
        }
        for (String s : searchStrs) {
            if (s != null && str.contains(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否包含全部子串。str为null或searchStrs为空返回false。
     *
     * @param str
     * @param searchStrs
     * @return
     */
    public static boolean containsAll(String str, String... searchStrs) {
        if (str == null || searchStrs == null || searchStrs.length == 0) {
            return false;
        }
        for (String s : searchStrs) {
            if (s == null || !str.contains(s)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 忽略大小写判断是否包含子串。使用Locale.ROOT做大小写折叠，规避土耳其语i问题。任一为null返回false。
     *
     * @param str
     * @param searchStr
     * @return
     */
    public static boolean containsIgnoreCase(String str, String searchStr) {
        if (str == null || searchStr == null) {
            return false;
        }
        return str.toLowerCase(Locale.ROOT).contains(searchStr.toLowerCase(Locale.ROOT));
    }

    /**
     * 统计子串出现次数（不重叠）。str或sub为null、sub为空返回0。
     *
     * @param str
     * @param sub
     * @return
     */
    public static int countMatches(String str, String sub) {
        if (str == null || sub == null || sub.isEmpty()) {
            return 0;
        }
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) >= 0) {
            count++;
            idx += sub.length();
        }
        return count;
    }

    /**
     * 是否以任意前缀开头。str为null或prefixes为空返回false。
     *
     * @param str
     * @param prefixes
     * @return
     */
    public static boolean startsWithAny(String str, String... prefixes) {
        if (str == null || prefixes == null) {
            return false;
        }
        for (String p : prefixes) {
            if (p != null && str.startsWith(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否以任意后缀结尾。str为null或suffixes为空返回false。
     *
     * @param str
     * @param suffixes
     * @return
     */
    public static boolean endsWithAny(String str, String... suffixes) {
        if (str == null || suffixes == null) {
            return false;
        }
        for (String s : suffixes) {
            if (s != null && str.endsWith(s)) {
                return true;
            }
        }
        return false;
    }

    // ==================== 转义 / 编码 ====================

    /**
     * HTML转义：& < > " '。str为null返回空串。
     *
     * @param str
     * @return
     */
    public static String escapeHtml(String str) {
        if (str == null) {
            return "";
        }
        if (str.indexOf('&') < 0 && str.indexOf('<') < 0 && str.indexOf('>') < 0
                && str.indexOf('"') < 0 && str.indexOf('\'') < 0) {
            return str;
        }
        StringBuilder sb = new StringBuilder(str.length() + 16);
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#39;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * JSON字符串转义：把 " \ 与控制字符转义为合法JSON字符串内部表示。str为null返回"null"。
     * 注意返回值不含外层引号，由调用方包裹。
     *
     * @param str
     * @return
     */
    public static String escapeJson(String str) {
        if (str == null) {
            return "null";
        }
        if (needsJsonEscape(str)) {
            StringBuilder sb = new StringBuilder(str.length() + 16);
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                switch (c) {
                    case '"':
                        sb.append("\\\"");
                        break;
                    case '\\':
                        sb.append("\\\\");
                        break;
                    case '\n':
                        sb.append("\\n");
                        break;
                    case '\r':
                        sb.append("\\r");
                        break;
                    case '\t':
                        sb.append("\\t");
                        break;
                    case '\b':
                        sb.append("\\b");
                        break;
                    case '\f':
                        sb.append("\\f");
                        break;
                    default:
                        if (c < ' ') {
                            sb.append(String.format("\\u%04x", (int) c));
                        } else {
                            sb.append(c);
                        }
                }
            }
            return sb.toString();
        }
        return str;
    }

    /**
     * XML转义：& < > " '。与HTML转义引号实体不同，统一用XML命名实体。str为null返回空串。
     *
     * @param str
     * @return
     */
    public static String escapeXml(String str) {
        if (str == null) {
            return "";
        }
        if (str.indexOf('&') < 0 && str.indexOf('<') < 0 && str.indexOf('>') < 0
                && str.indexOf('"') < 0 && str.indexOf('\'') < 0) {
            return str;
        }
        StringBuilder sb = new StringBuilder(str.length() + 16);
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&apos;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 正则特殊字符转义，便于把任意字符串作为正则字面量匹配。str为null返回空串。
     *
     * @param str
     * @return
     */
    public static String escapeRegex(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(str.length() * 2);
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (REGEX_META.indexOf(c) >= 0) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * 将字符串中的非ASCII字符编码为\\uXXXX形式，ASCII字符原样保留。str为null返回空串。
     *
     * @param str
     * @return
     */
    public static String unicodeEncode(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(str.length() * 2);
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c < 128) {
                sb.append(c);
            } else {
                sb.append(String.format("\\u%04x", (int) c));
            }
        }
        return sb.toString();
    }

    /**
     * 将\\uXXXX形式的转义解码回原字符，非法转义原样保留。str为null返回空串。
     *
     * @param str
     * @return
     */
    public static String unicodeDecode(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        int idx = str.indexOf("\\u");
        if (idx < 0) {
            return str;
        }
        StringBuilder sb = new StringBuilder(str.length());
        int i = 0;
        while (i < str.length()) {
            char c = str.charAt(i);
            if (c == '\\' && i + 5 < str.length() && str.charAt(i + 1) == 'u') {
                String hex = str.substring(i + 2, i + 6);
                if (isHex(hex)) {
                    sb.append((char) Integer.parseInt(hex, 16));
                    i += 6;
                    continue;
                }
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    // ==================== 路径 / 文件名 ====================

    /**
     * 跨平台提取文件名，同时处理 / 与 \。path为null返回空串。
     *
     * @param path
     * @return
     */
    public static String getFileName(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    /**
     * 取扩展名（不含点，小写）。无扩展名返回空串。filename为null返回空串。
     *
     * @param filename
     * @return
     */
    public static String getFileExtension(String filename) {
        String name = getFileName(filename);
        if (name.isEmpty()) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 去除扩展名，保留路径与主名。filename为null返回空串。
     *
     * @param filename
     * @return
     */
    public static String removeFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        int slash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        if (dot > slash && dot > 0) {
            return filename.substring(0, dot);
        }
        return filename;
    }

    /**
     * 规范化路径：合并连续分隔符、解析 . 与 ..。path为null返回空串。
     *
     * @param path
     * @return
     */
    public static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        // 统一用/处理
        String p = path.replace('\\', '/');
        String[] segs = p.split("/");
        List<String> out = new ArrayList<>(segs.length);
        for (String seg : segs) {
            if (seg.isEmpty() || seg.equals(".")) {
                continue;
            }
            if (seg.equals("..")) {
                // out为空或末尾已是..时保留..（相对路径无法继续向上，标准Path.normalize行为）；
                // 否则回退一级（弹出末尾普通段）
                if (out.isEmpty() || "..".equals(out.get(out.size() - 1))) {
                    out.add(seg);
                } else {
                    out.remove(out.size() - 1);
                }
            } else {
                out.add(seg);
            }
        }
        StringBuilder sb = new StringBuilder(p.length());
        boolean absolute = p.startsWith("/");
        if (absolute) {
            sb.append('/');
            // 绝对路径下，根目录之上的..无意义（无法回退根），剔除开头连续的..
            while (!out.isEmpty() && "..".equals(out.get(0))) {
                out.remove(0);
            }
        }
        for (int i = 0; i < out.size(); i++) {
            if (i > 0) {
                sb.append('/');
            }
            sb.append(out.get(i));
        }
        if (!absolute && sb.length() == 0) {
            return ".";
        }
        return sb.toString();
    }

    /**
     * 去除文件名中的非法字符（&lt; &gt; : " / \ | ? * 与控制字符），替换为指定字符（默认_）。filename为null返回空串。
     *
     * @param filename
     * @param replacement
     * @return
     */
    public static String safeFileName(String filename, char replacement) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        char[] cs = filename.toCharArray();
        for (int i = 0; i < cs.length; i++) {
            char c = cs[i];
            if (ILLEGAL_FILENAME.indexOf(c) >= 0 || c < ' ') {
                cs[i] = replacement;
            }
        }
        return new String(cs);
    }

    /**
     * 去除文件名非法字符，默认替换为下划线。
     *
     * @param filename
     * @return
     */
    public static String safeFileName(String filename) {
        return safeFileName(filename, '_');
    }

    // ==================== 文本差异 / 相似度 ====================
    //
    // 提供三种主流字符串相似度算法，分别适用于不同场景：
    //   - levenshteinSimilarity / levenshteinDistance：编辑距离。衡量"把A改成B的最少操作数"，
    //     对字符级增删改、顺序错位敏感，适合拼写纠错/OCR纠错/打字错误识别。
    //   - lcsSimilarity：最长公共子序列。衡量"A是B的子串程度"，适合简称匹配全称，
    //     但对"附加后缀"会误判高分（如"全聚德" vs "全聚德东四分店"）。
    //   - ngramSimilarity：二元组(bigram)余弦相似度。衡量整体片段重合度，对个别字符替换/
    //     增删区分度最高，适合区分"名称高度相似但不同"（如"万达嘉华" vs "万达瑞华"）。
    // 三者维度不同，不能互相替代：编辑距离看操作量，LCS/N-gram看集合重合度。

    /**
     * 基于Levenshtein编辑距离计算相似度，返回[0,1]，1表示完全相同。
     * 两个null返回1.0，单边null返回0.0。
     *
     * @param s1
     * @param s2
     * @return
     */
    public static double levenshteinSimilarity(String s1, String s2) {
        if (s1 == null && s2 == null) {
            return 1.0;
        }
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        if (s1.equals(s2)) {
            return 1.0;
        }
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) {
            return 1.0;
        }
        int dist = levenshteinDistance(s1, s2);
        return 1.0 - (double) dist / maxLen;
    }

    /**
     * 计算Levenshtein编辑距离（最少单字符插入/删除/替换次数）。两个null返回0，
     * 单边null返回另一串长度。常用于判断"两串是否仅差几次打字错误"。
     *
     * @param s1
     * @param s2
     * @return
     */
    public static int levenshteinDistance(String s1, String s2) {
        if (s1 == null && s2 == null) {
            return 0;
        }
        if (s1 == null) {
            return s2.length();
        }
        if (s2 == null) {
            return s1.length();
        }
        if (s1.equals(s2)) {
            return 0;
        }
        int len1 = s1.length();
        int len2 = s2.length();
        int[] prev = new int[len2 + 1];
        int[] curr = new int[len2 + 1];
        for (int j = 0; j <= len2; j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= len1; i++) {
            curr[0] = i;
            for (int j = 1; j <= len2; j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[len2];
    }

    /**
     * 基于最长公共子序列(LCS)计算相似度，返回[0,1]，1表示完全相同。
     * 适合简称匹配全称（如"故宫" vs "北京故宫博物院"）。注意：对附加后缀会误判偏高。
     * 两个null返回1.0，单边null返回0.0。用一维数组优化空间至O(n)。
     *
     * @param s1
     * @param s2
     * @return
     */
    public static double lcsSimilarity(String s1, String s2) {
        if (s1 == null && s2 == null) {
            return 1.0;
        }
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        if (s1.equals(s2)) {
            return 1.0;
        }
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) {
            return 1.0;
        }
        // 内层循环用较短的串，减少空间
        String a = s1, b = s2;
        if (a.length() < b.length()) {
            String t = a;
            a = b;
            b = t;
        }
        int m = a.length();
        int n = b.length();
        int[] prev = new int[n + 1];
        int[] curr = new int[n + 1];
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    curr[j] = prev[j - 1] + 1;
                } else {
                    curr[j] = Math.max(curr[j - 1], prev[j]);
                }
            }
            int[] t = prev;
            prev = curr;
            curr = t;
        }
        return (double) prev[n] / maxLen;
    }

    /**
     * 基于二元组(bigram)余弦相似度计算，返回[0,1]，1表示完全相同。
     * 对个别字符替换/增删区分度最高，适合区分"名称高度相似但不同"（如"万达嘉华" vs "万达瑞华"、
     * 东西站分店）。长度<=1时无法构成bigram，仅当完全相同返回1.0否则0.0。
     * 两个null返回1.0，单边null返回0.0。
     *
     * @param s1
     * @param s2
     * @return
     */
    public static double ngramSimilarity(String s1, String s2) {
        if (s1 == null && s2 == null) {
            return 1.0;
        }
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        if (s1.equals(s2)) {
            return 1.0;
        }
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen <= 1) {
            return 0.0;
        }
        Map<String, int[]> bigrams = new HashMap<>();
        for (int i = 0; i < s1.length() - 1; i++) {
            String gram = s1.substring(i, i + 2);
            bigrams.computeIfAbsent(gram, k -> new int[2])[0]++;
        }
        for (int i = 0; i < s2.length() - 1; i++) {
            String gram = s2.substring(i, i + 2);
            bigrams.computeIfAbsent(gram, k -> new int[2])[1]++;
        }
        long dotProduct = 0, normA = 0, normB = 0;
        for (int[] freq : bigrams.values()) {
            dotProduct += (long) freq[0] * freq[1];
            normA += (long) freq[0] * freq[0];
            normB += (long) freq[1] * freq[1];
        }
        if (normA == 0 || normB == 0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }


    /**
     * 简单行级差异标记：oldStr与newStr按行对比，旧行前缀"- "、新行前缀"+ "、相同行前缀"  "。
     * 任意为null按空串处理。注意：仅做行级新增/删除对比，不做行内细粒度对齐。
     *
     * @param oldStr
     * @param newStr
     * @return
     */
    public static String diff(String oldStr, String newStr) {
        String[] oldLines = (oldStr == null ? "" : oldStr).split("\n", -1);
        String[] newLines = (newStr == null ? "" : newStr).split("\n", -1);
        // LCS求公共行序列
        int[][] dp = new int[oldLines.length + 1][newLines.length + 1];
        for (int i = oldLines.length - 1; i >= 0; i--) {
            for (int j = newLines.length - 1; j >= 0; j--) {
                if (oldLines[i].equals(newLines[j])) {
                    dp[i][j] = dp[i + 1][j + 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i + 1][j], dp[i][j + 1]);
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        int i = 0, j = 0;
        while (i < oldLines.length && j < newLines.length) {
            if (oldLines[i].equals(newLines[j])) {
                sb.append("  ").append(oldLines[i]).append('\n');
                i++;
                j++;
            } else if (dp[i + 1][j] >= dp[i][j + 1]) {
                sb.append("- ").append(oldLines[i]).append('\n');
                i++;
            } else {
                sb.append("+ ").append(newLines[j]).append('\n');
                j++;
            }
        }
        while (i < oldLines.length) {
            sb.append("- ").append(oldLines[i++]).append('\n');
        }
        while (j < newLines.length) {
            sb.append("+ ").append(newLines[j++]).append('\n');
        }
        // 去掉末尾多余换行
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * 字符串相似度的便捷别名，默认使用Levenshtein编辑距离算法。详见{@link #levenshteinSimilarity}。
     * 中文场景（景区/酒店名匹配）建议按需选用{@link #lcsSimilarity}或{@link #ngramSimilarity}。
     *
     * @param s1
     * @param s2
     * @return
     */
    public static double similarity(String s1, String s2) {
        return levenshteinSimilarity(s1, s2);
    }

    // ==================== 内部方法 ====================

    /**
     * 估算字符显示宽度：中文/全角字符算2，其余算1。仅用于控制台/固定宽表格近似计算。
     *
     * @param c
     * @return
     */
    private static int charWidth(char c) {
        // CJK统一表意、全角标点等常见区间算2宽度
        if (c >= '　' && c <= '￯') {
            return 2;
        }
        if (c >= '一' && c <= '鿿') {
            return 2;
        }
        return 1;
    }

    /**
     * 判断字符是否为CJK中日韩文字。覆盖常见区间：CJK统一表意、扩展A、日文假名、谚文音节。
     *
     * @param c
     * @return
     */
    private static boolean isCJKChar(char c) {
        // CJK统一表意文字（含中日韩常用汉字）
        if (c >= '一' && c <= '鿿') {
            return true;
        }
        // CJK扩展A
        if (c >= '㐀' && c <= '䶿') {
            return true;
        }
        // 日文平假名、片假名
        if (c >= '぀' && c <= 'ヿ') {
            return true;
        }
        // 韩文谚文音节
        if (c >= '가' && c <= '힣') {
            return true;
        }
        return false;
    }

    /**
     * 用indexOf+substring切分字符串，返回已trim、已剔除空段的token列表。
     * 比String.split(正则)更高效：不经过正则引擎，且切分时直接跳过首尾空白与空段，
     * 不产生需二次trim的带空白小串。
     * <p>
     * 默认分隔符(null或",")时把逗号与任意空白(空格/制表/换行)统一当作分隔符，
     * 连续分隔符合并；自定义分隔符按字面量匹配，分隔符本身不会作为正则元字符。
     *
     * @param s   非null非空
     * @param sep 分隔符，null或","表示默认(逗号+空白)
     * @return
     */
    private static List<String> splitTokens(String s, String sep) {
        List<String> tokens = new ArrayList<>(s.length() / 4 + 1);
        // 空分隔符防御：indexOf("",i)恒返回i会导致死循环，退化为按默认分隔
        boolean defaultSep = sep == null || sep.isEmpty() || sep.equals(DEFAULT_SEP);
        int len = s.length();
        int start = 0;
        int i = 0;
        while (i <= len) {
            // 找下一个分隔符位置（默认模式：逗号或空白都算；自定义模式：字面量匹配）
            int end = defaultSep ? indexOfDefaultSep(s, i) : s.indexOf(sep, i);
            if (end < 0) {
                end = len;
            }
            // 取[start,end)并trim，空段跳过
            while (start < end && s.charAt(start) <= ' ') {
                start++;
            }
            int e = end;
            while (e > start && s.charAt(e - 1) <= ' ') {
                e--;
            }
            if (e > start) {
                tokens.add(s.substring(start, e));
            }
            if (end >= len) {
                break;
            }
            // 默认模式下连续分隔符(逗号+空白)需整体跳过
            if (defaultSep) {
                // 跳过当前位置的分隔字符及后续连续空白/逗号
                int j = end;
                while (j < len && (s.charAt(j) == ',' || s.charAt(j) <= ' ')) {
                    j++;
                }
                start = j;
                i = j;
            } else {
                start = end + sep.length();
                i = start;
            }
        }
        return tokens;
    }

    /**
     * 默认模式下从from起找第一个分隔符(逗号或空白)位置，找不到返回-1。
     *
     * @param s
     * @param from
     * @return
     */
    private static int indexOfDefaultSep(String s, int from) {
        for (int k = from; k < s.length(); k++) {
            char c = s.charAt(k);
            if (c == ',' || c <= ' ') {
                return k;
            }
        }
        return -1;
    }

    /**
     * 判断字符串是否含需JSON转义的字符。
     *
     * @param str
     * @return
     */
    private static boolean needsJsonEscape(String str) {
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '"' || c == '\\' || c < ' ') {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断字符串是否全部为十六进制字符（长度4）。
     *
     * @param s
     * @return
     */
    private static boolean isHex(String s) {
        if (s == null || s.length() != 4) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return true;
    }


    /**
     * 命名风格枚举，配合 {@link StringTools#convertCase} 使用。
     *
     * <ul>
     *   <li>{@link #CAMEL}   userName      小驼峰</li>
     *   <li>{@link #PASCAL}  UserName      大驼峰</li>
     *   <li>{@link #SNAKE}   user_name     蛇形</li>
     *   <li>{@link #MACRO}   USER_NAME     大写蛇形（C常量风格）</li>
     *   <li>{@link #KEBAB}   user-name     短横线</li>
     *   <li>{@link #TRAIN}   User-Name     Train（HTTP Header风格）</li>
     *   <li>{@link #DOT}     user.name     点号（配置文件风格）</li>
     *   <li>{@link #PATH}    user/name     路径（路由风格）</li>
     *   <li>{@link #FLAT}    username      扁平（无分隔符）</li>
     * </ul>
     */
    public enum CaseStyle {
        CAMEL,
        PASCAL,
        SNAKE,
        MACRO,
        KEBAB,
        TRAIN,
        DOT,
        PATH,
        FLAT
    }

    private StringTools() {
    }
}
