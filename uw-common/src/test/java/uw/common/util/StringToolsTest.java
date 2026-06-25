package uw.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class StringToolsTest {

    // ==================== Join ====================

    @Test
    void testJoinLongArray() {
        Assertions.assertEquals("", StringTools.join((long[]) null));
        Assertions.assertEquals("", StringTools.join(new long[0]));
        Assertions.assertEquals("1,2,3", StringTools.join(new long[]{1, 2, 3}));
        Assertions.assertEquals("1|2|3", StringTools.join(new long[]{1, 2, 3}, "|"));
        // null sep 退化为默认逗号
        Assertions.assertEquals("1,2", StringTools.join(new long[]{1, 2}, null));
    }

    @Test
    void testJoinIntArray() {
        Assertions.assertEquals("", StringTools.join((int[]) null));
        Assertions.assertEquals("1,2,3", StringTools.join(new int[]{1, 2, 3}));
        Assertions.assertEquals("1-2", StringTools.join(new int[]{1, 2}, "-"));
    }

    @Test
    void testJoinStringArray() {
        Assertions.assertEquals("", StringTools.join((String[]) null));
        // null 元素跳过
        Assertions.assertEquals("a,c", StringTools.join(new String[]{"a", null, "c"}));
        Assertions.assertEquals("a/b/c", StringTools.join(new String[]{"a", "b", "c"}, "/"));
    }

    @Test
    void testJoinCollection() {
        Assertions.assertEquals("", StringTools.join((java.util.Collection<?>) null));
        Assertions.assertEquals("", StringTools.join(Collections.emptyList()));
        Assertions.assertEquals("1,2,3", StringTools.join(Arrays.asList(1, 2, 3)));
        Assertions.assertEquals("1,2,3", StringTools.join(Arrays.asList(1L, 2L, 3L)));
        // null 元素跳过
        Assertions.assertEquals("a,c", StringTools.join(Arrays.asList("a", null, "c")));
        Assertions.assertEquals("a|c", StringTools.join(Arrays.asList("a", null, "c"), "|"));
    }

    @Test
    void testSurround() {
        // 在 join 结果首尾补分隔符：用户自行组合 join + surround
        Assertions.assertEquals(",1,2,3,", StringTools.surround(StringTools.join(new long[]{1, 2, 3}, ","), ","));
        Assertions.assertEquals(",a,b,c,", StringTools.surround(StringTools.join(new String[]{"a", "b", "c"}, ","), ","));
        // 自定义分隔符
        Assertions.assertEquals("|1|2|3|", StringTools.surround(StringTools.join(new long[]{1, 2, 3}, "|"), "|"));
        // 空串不补分隔符，避免孤立分隔符
        Assertions.assertEquals("", StringTools.surround("", ","));
        Assertions.assertEquals("", StringTools.surround(null, ","));
        // sep为null用默认逗号
        Assertions.assertEquals(",1,2,3,", StringTools.surround("1,2,3", null));
    }

    // ==================== Split ====================

    @Test
    void testSplitToLongArray() {
        Assertions.assertArrayEquals(new long[0], StringTools.splitToLongArray(null));
        Assertions.assertArrayEquals(new long[0], StringTools.splitToLongArray(""));
        Assertions.assertArrayEquals(new long[]{1, 2, 3}, StringTools.splitToLongArray("1,2,3"));
        // 自动 trim + 跳过空白段
        Assertions.assertArrayEquals(new long[]{1, 2, 3}, StringTools.splitToLongArray(" 1 , , 2 ,3 "));
        // 默认正则兼容空白
        Assertions.assertArrayEquals(new long[]{1, 2, 3}, StringTools.splitToLongArray("1 2 3"));
        // 自定义分隔符
        Assertions.assertArrayEquals(new long[]{1, 2}, StringTools.splitToLongArray("1|2", "|"));
        // 脏数据逐条跳过（不抛异常）
        Assertions.assertArrayEquals(new long[]{1, 3}, StringTools.splitToLongArray("1,abc,3"));
    }

    @Test
    void testSplitToIntArray() {
        Assertions.assertArrayEquals(new int[0], StringTools.splitToIntArray(null));
        Assertions.assertArrayEquals(new int[]{1, 2, 3}, StringTools.splitToIntArray("1,2,3"));
        Assertions.assertArrayEquals(new int[]{1, 3}, StringTools.splitToIntArray("1,xx,3"));
    }

    @Test
    void testSplitToStringArray() {
        Assertions.assertArrayEquals(new String[0], StringTools.splitToStringArray(null));
        Assertions.assertArrayEquals(new String[]{"a", "b"}, StringTools.splitToStringArray("a,b"));
        // trim + 跳过空白段
        Assertions.assertArrayEquals(new String[]{"a", "b"}, StringTools.splitToStringArray(" a , , b "));
        Assertions.assertArrayEquals(new String[]{"a", "b"}, StringTools.splitToStringArray("a|b|", "|"));
    }

    @Test
    void testSplitToLongList() {
        Assertions.assertTrue(StringTools.splitToLongList(null).isEmpty());
        Assertions.assertEquals(Arrays.asList(1L, 2L, 3L), StringTools.splitToLongList("1,2,3"));
        Assertions.assertEquals(Arrays.asList(1L, 2L), StringTools.splitToLongList("1,2", ","));
    }

    @Test
    void testSplitToIntList() {
        Assertions.assertTrue(StringTools.splitToIntList(null).isEmpty());
        Assertions.assertTrue(StringTools.splitToIntList("").isEmpty());
        Assertions.assertEquals(Arrays.asList(1, 2, 3), StringTools.splitToIntList("1,2,3"));
        // 脏数据跳过
        Assertions.assertEquals(Arrays.asList(1, 3), StringTools.splitToIntList("1,xx,3"));
        // 自定义分隔符
        Assertions.assertEquals(Arrays.asList(1, 2), StringTools.splitToIntList("1|2", "|"));
    }

    @Test
    void testSplitToStringList() {
        Assertions.assertTrue(StringTools.splitToStringList("").isEmpty());
        List<String> list = StringTools.splitToStringList(" a , b ");
        Assertions.assertEquals(Arrays.asList("a", "b"), list);
    }

    @Test
    void testSplitMixedSeparators() {
        // 默认模式：逗号与各种空白混用，连续分隔符合并
        Assertions.assertArrayEquals(new long[]{1, 2, 3}, StringTools.splitToLongArray("1,\t2\n 3"));
        // 只有空白无逗号也按默认分隔
        Assertions.assertArrayEquals(new long[]{1, 2, 3}, StringTools.splitToLongArray(" 1   2   3 "));
        // 首尾分隔符不产生空段
        Assertions.assertArrayEquals(new long[]{1, 2}, StringTools.splitToLongArray(",1,2,"));
        // 空分隔符防御：不应死循环，退化为默认分隔
        Assertions.assertArrayEquals(new long[]{1, 2, 3}, StringTools.splitToLongArray("1,2,3", ""));
    }

    @Test
    void testSplitSpecialSeparatorLiteral() {
        // 点号是正则元字符，显式传 sep 必须按字面量切，不能当作"任意字符"
        Assertions.assertArrayEquals(new String[]{"a", "b"}, StringTools.splitToStringArray("a.b", "."));
    }

    // ==================== 占位符与 IN 子句 ====================

    @Test
    void testBuildPlaceholders() {
        Assertions.assertEquals("", StringTools.buildPlaceholders(0));
        Assertions.assertEquals("", StringTools.buildPlaceholders(-1));
        Assertions.assertEquals("?", StringTools.buildPlaceholders(1));
        Assertions.assertEquals("?,?,?", StringTools.buildPlaceholders(3));
        // 自定义占位符与分隔符（占位符:p、分隔符|）
        Assertions.assertEquals(":p|:p|:p", StringTools.buildPlaceholders(3, ":p", "|"));
        // 溢出防御：超大count应抛IllegalArgumentException而非NegativeArraySizeException
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> StringTools.buildPlaceholders(Integer.MAX_VALUE, "??", ","));
    }

    @Test
    void testBuildInClause() {
        Assertions.assertEquals("(0)", StringTools.buildInClause((long[]) null));
        Assertions.assertEquals("(0)", StringTools.buildInClause(new long[0]));
        Assertions.assertEquals("(1,2,3)", StringTools.buildInClause(new long[]{1, 2, 3}));
        Assertions.assertEquals("(0)", StringTools.buildInClause((java.util.Collection<?>) null));
        Assertions.assertEquals("(1,2,3)", StringTools.buildInClause(Arrays.asList(1, 2, 3)));
    }

    // ==================== 驼峰 / 下划线 / 大小写 ====================

    @Test
    void testCapFirst() {
        Assertions.assertEquals("", StringTools.capFirst(null));
        Assertions.assertEquals("User", StringTools.capFirst("user"));
        Assertions.assertEquals("User", StringTools.capFirst("User"));
        Assertions.assertEquals("", StringTools.capFirst(""));
    }

    @Test
    void testUncapFirst() {
        Assertions.assertEquals("", StringTools.uncapFirst(null));
        Assertions.assertEquals("user", StringTools.uncapFirst("User"));
        Assertions.assertEquals("user", StringTools.uncapFirst("user"));
    }

    // ==================== trim / 判等 ====================

    @Test
    void testTrimToEmpty() {
        Assertions.assertEquals("", StringTools.trimToEmpty(null));
        Assertions.assertEquals("", StringTools.trimToEmpty("   "));
        Assertions.assertEquals("a", StringTools.trimToEmpty("  a "));
    }

    @Test
    void testTrimToNull() {
        Assertions.assertNull(StringTools.trimToNull(null));
        Assertions.assertNull(StringTools.trimToNull("   "));
        Assertions.assertEquals("a", StringTools.trimToNull("  a "));
    }

    @Test
    void testEquals() {
        Assertions.assertTrue(StringTools.equals(null, null));
        Assertions.assertFalse(StringTools.equals(null, "a"));
        Assertions.assertFalse(StringTools.equals("a", null));
        Assertions.assertTrue(StringTools.equals("a", "a"));
        Assertions.assertFalse(StringTools.equals("a", "b"));
    }

    @Test
    void testEqualsIgnoreCase() {
        Assertions.assertTrue(StringTools.equalsIgnoreCase(null, null));
        Assertions.assertTrue(StringTools.equalsIgnoreCase("AbC", "abc"));
        Assertions.assertFalse(StringTools.equalsIgnoreCase("a", null));
    }

    // ==================== 随机串 ====================

    @Test
    void testRandom() {
        Assertions.assertEquals("", StringTools.randomNumeric(0));
        Assertions.assertEquals("", StringTools.randomNumeric(-1));
        String num = StringTools.randomNumeric(8);
        Assertions.assertEquals(8, num.length());
        Assertions.assertTrue(num.matches("\\d{8}"));

        String alnum = StringTools.randomAlphanumeric(16);
        Assertions.assertEquals(16, alnum.length());
        Assertions.assertTrue(alnum.matches("[A-Za-z0-9]{16}"));

        // 自定义字符集
        String s = StringTools.random(5, new char[]{'a', 'b'});
        Assertions.assertEquals(5, s.length());
        Assertions.assertTrue(s.matches("[ab]{5}"));

        // 空字符集返回空串
        Assertions.assertEquals("", StringTools.random(5, new char[0]));
    }

    @Test
    void testRandomUniqueness() {
        // 连续两次生成大概率不同（SecureRandom）
        boolean diff = false;
        for (int i = 0; i < 10; i++) {
            if (!StringTools.randomAlphanumeric(16).equals(StringTools.randomAlphanumeric(16))) {
                diff = true;
                break;
            }
        }
        Assertions.assertTrue(diff, "随机串不应连续相同");
    }

    // ==================== 默认值 ====================

    @Test
    void testDefaultIfBlank() {
        Assertions.assertEquals("x", StringTools.defaultIfBlank(null, "x"));
        Assertions.assertEquals("x", StringTools.defaultIfBlank("   ", "x"));
        Assertions.assertEquals("a", StringTools.defaultIfBlank("a", "x"));
    }

    @Test
    void testDefaultIfBlankSupplier() {
        // 惰性求值：非blank时不调用supplier
        int[] counter = {0};
        Supplier<String> sup = () -> { counter[0]++; return "lazy"; };
        Assertions.assertEquals("a", StringTools.defaultIfBlank("a", sup));
        Assertions.assertEquals(0, counter[0], "非blank时不应求值supplier");
        Assertions.assertEquals("lazy", StringTools.defaultIfBlank("  ", sup));
        Assertions.assertEquals(1, counter[0]);
    }

    @Test
    void testDefaultIfEmptySupplier() {
        Assertions.assertEquals("y", StringTools.defaultIfEmpty("", () -> "y"));
        // 空白字符串不算empty
        Assertions.assertEquals("  ", StringTools.defaultIfEmpty("  ", () -> "y"));
    }

    // ==================== 截断 / 填充 ====================

    @Test
    void testTruncate() {
        Assertions.assertEquals("", StringTools.truncate(null, 5, "..."));
        Assertions.assertEquals("abc", StringTools.truncate("abc", 5, "..."));
        Assertions.assertEquals("ab...", StringTools.truncate("abcdef", 5, "..."));
        Assertions.assertEquals("", StringTools.truncate("abcdef", 0, "..."));
    }

    @Test
    void testTruncateByWidth() {
        Assertions.assertEquals("中", StringTools.truncateByWidth("中文测试", 3));
        // 中(2) + a(1) = 3
        Assertions.assertEquals("中a", StringTools.truncateByWidth("中a文", 3));
        Assertions.assertEquals("", StringTools.truncateByWidth("abc", 0));
        Assertions.assertEquals("abc", StringTools.truncateByWidth("abc", 5));
    }

    @Test
    void testPadStart() {
        Assertions.assertEquals("007", StringTools.padStart("7", 3, '0'));
        Assertions.assertEquals("123", StringTools.padStart("123", 2, '0'));
        Assertions.assertEquals("", StringTools.padStart(null, 3, '0'));
        Assertions.assertEquals("xxx", StringTools.padStart("", 3, 'x'));
    }

    @Test
    void testPadEnd() {
        Assertions.assertEquals("7xx", StringTools.padEnd("7", 3, 'x'));
        Assertions.assertEquals("7", StringTools.padEnd("7", 1, 'x'));
    }

    // ==================== 格式化 ====================

    @Test
    void testFormatNamed() {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "Tom");
        params.put("age", 18);
        Assertions.assertEquals("Hello Tom, age=18", StringTools.format("Hello ${name}, age=${age}", params));
        // 未提供的key原样保留
        Assertions.assertEquals("Hello ${x}", StringTools.format("Hello ${x}", params));
        // null值替换为空串
        params.put("name", null);
        Assertions.assertEquals("Hi ", StringTools.format("Hi ${name}", params));
    }

    @Test
    void testFormatSimple() {
        Assertions.assertEquals("", StringTools.format(null, "a"));
        Assertions.assertEquals("no args", StringTools.format("no args"));
        Assertions.assertEquals("a-b", StringTools.format("%s-%s", "a", "b"));
    }

    // ==================== 命名风格转换 ====================

    @Test
    void testCaseConvert() {
        Assertions.assertEquals("userName", StringTools.toCamelCase("user-name", '-'));
        Assertions.assertEquals("user_name", StringTools.toSnakeCase("userName"));
        Assertions.assertEquals("user_name", StringTools.toSnakeCase("UserName"));
        Assertions.assertEquals("user-name", StringTools.toKebabCase("userName"));
        Assertions.assertEquals("UserName", StringTools.toPascalCase("user_name"));
        Assertions.assertEquals("USER_NAME", StringTools.toMacroCase("userName"));
        Assertions.assertEquals("User-Name", StringTools.toTrainCase("userName"));
        Assertions.assertEquals("user.name", StringTools.toDotCase("userName"));
        Assertions.assertEquals("user/name", StringTools.toPathCase("userName"));
        Assertions.assertEquals("username", StringTools.toFlatCase("userName"));
    }

    @Test
    void testCaseConvertMultiSource() {
        // 关键：toXxxCase 现在能正确识别任意源格式，不再只假设驼峰输入
        // 源为 kebab
        Assertions.assertEquals("user_name", StringTools.toSnakeCase("user-name"));
        Assertions.assertEquals("UserName", StringTools.toPascalCase("user-name"));
        // 源为 dot
        Assertions.assertEquals("user_name", StringTools.toSnakeCase("user.name"));
        // 源为 path
        Assertions.assertEquals("user_name", StringTools.toSnakeCase("user/name"));
        // 源为 macro（大写蛇形），不能再把每个字母拆开
        Assertions.assertEquals("user_name", StringTools.toSnakeCase("USER_NAME"));
        // toCamelCase 指定分隔符也能正确规范化大小写
        Assertions.assertEquals("userName", StringTools.toCamelCase("USER_NAME", '_'));
        // 源为 train
        Assertions.assertEquals("user_name", StringTools.toSnakeCase("User-Name"));
        // 源为 flat
        Assertions.assertEquals("username", StringTools.toSnakeCase("username"));
    }

    @Test
    void testCaseConvertAcronym() {
        // 连续大写缩写不应被逐字拆开
        Assertions.assertEquals("http_response", StringTools.toSnakeCase("HTTPResponse"));
        Assertions.assertEquals("user_id", StringTools.toSnakeCase("userID"));
        Assertions.assertEquals("xml_parser", StringTools.toSnakeCase("XMLParser"));
        // 多分隔符混合
        Assertions.assertEquals("http_response_id", StringTools.toSnakeCase("HTTPResponseID"));
        // 转 Pascal 时缩写也保留
        Assertions.assertEquals("HttpResponse", StringTools.toPascalCase("HTTPResponse"));
    }

    @Test
    void testConvertCaseEnum() {
        // 自动探测源格式，按 to 输出
        Assertions.assertEquals("USER_NAME", StringTools.convertCase("userName", StringTools.CaseStyle.MACRO));
        Assertions.assertEquals("User-Name", StringTools.convertCase("userName", StringTools.CaseStyle.TRAIN));
        // kebab 源
        Assertions.assertEquals("userName", StringTools.convertCase("user-name", StringTools.CaseStyle.CAMEL));
        // macro 源
        Assertions.assertEquals("user-name", StringTools.convertCase("USER_NAME", StringTools.CaseStyle.KEBAB));
        // to=null 原样返回
        Assertions.assertEquals("abc", StringTools.convertCase("abc", null));
        // 往返：CAMEL -> SNAKE -> CAMEL
        Assertions.assertEquals("httpResponse", StringTools.convertCase(
                StringTools.convertCase("httpResponse", StringTools.CaseStyle.SNAKE),
                StringTools.CaseStyle.CAMEL));
    }

    // ==================== 清理 / 规范化 ====================

    @Test
    void testClean() {
        Assertions.assertEquals("", StringTools.clean(null));
        // 全角空格 -> 普通空格
        Assertions.assertEquals("a b", StringTools.clean("a　b"));
        // BOM 去除
        Assertions.assertEquals("abc", StringTools.clean("﻿abc"));
        // 零宽字符去除
        Assertions.assertEquals("ab", StringTools.clean("a​b"));
        // 标准空白保留
        Assertions.assertEquals("a\tb", StringTools.clean("a\tb"));
    }

    @Test
    void testNormalizeSpace() {
        Assertions.assertEquals("", StringTools.normalizeSpace(null));
        Assertions.assertEquals("a b c", StringTools.normalizeSpace("  a   b\tc\n"));
        Assertions.assertEquals("a", StringTools.normalizeSpace("  a  "));
    }

    // ==================== 全角/半角/CJK ====================

    @Test
    void testHalfFullWidth() {
        Assertions.assertEquals("ABC123", StringTools.toHalfWidth("ＡＢＣ１２３"));
        Assertions.assertEquals("ＡＢＣ", StringTools.toFullWidth("ABC"));
        Assertions.assertEquals("a b", StringTools.toHalfWidth("a　b"));
    }

    @Test
    void testExtractChinese() {
        Assertions.assertEquals("中文", StringTools.extractChinese("a中b文c"));
        Assertions.assertEquals("", StringTools.extractChinese("abc123"));
    }

    @Test
    void testHasCJK() {
        Assertions.assertTrue(StringTools.hasCJK("hello世界"));
        Assertions.assertFalse(StringTools.hasCJK("hello"));
        Assertions.assertFalse(StringTools.hasCJK(null));
    }

    // ==================== split 增强 / splitLines ====================

    @Test
    void testSplitCharOmitEmpty() {
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), StringTools.split("a,,b,c", ',', true));
        Assertions.assertEquals(Arrays.asList("a", "", "b", "c"), StringTools.split("a,,b,c", ',', false));
        Assertions.assertTrue(StringTools.split(null, ',', true).isEmpty());
    }

    @Test
    void testSplitCharLimit() {
        // 限制2段，剩余不再切
        Assertions.assertEquals(Arrays.asList("a", "b,c,d"), StringTools.split("a,b,c,d", ',', 2));
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), StringTools.split("a,b,c", ',', 0));
    }

    @Test
    void testSplitLines() {
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), StringTools.splitLines("a\nb\r\nc"));
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), StringTools.splitLines("a\rb\rc"));
        Assertions.assertEquals(Arrays.asList("a", "b"), StringTools.splitLines("a\nb\n"));
        Assertions.assertTrue(StringTools.splitLines(null).isEmpty());
    }

    // ==================== join 增强 ====================

    @Test
    void testJoinMap() {
        Map<Object, Object> m = new LinkedHashMap<>();
        m.put("k1", "v1");
        m.put("k2", "v2");
        Assertions.assertEquals("k1=v1&k2=v2", StringTools.joinMap(m, "&", "="));
        Assertions.assertEquals("", StringTools.joinMap((Map<?, ?>) null, "&", "="));
    }

    @Test
    void testSplitMap() {
        // joinMap 的逆操作
        Map<String, String> m = StringTools.splitMap("k1=v1&k2=v2", "&", "=");
        Assertions.assertEquals(2, m.size());
        Assertions.assertEquals("v1", m.get("k1"));
        Assertions.assertEquals("v2", m.get("k2"));
        // 保持解析顺序
        Assertions.assertEquals("[k1, k2]", new java.util.ArrayList<>(m.keySet()).toString());
        // null/空返回空Map
        Assertions.assertTrue(StringTools.splitMap(null, "&", "=").isEmpty());
        Assertions.assertTrue(StringTools.splitMap("", "&", "=").isEmpty());
        // 默认分隔符（逗号）
        Map<String, String> m2 = StringTools.splitMap("a=1,b=2", null, null);
        Assertions.assertEquals("1", m2.get("a"));
        Assertions.assertEquals("2", m2.get("b"));
        // 无kvSep的条目：value为空串
        Map<String, String> m3 = StringTools.splitMap("flag&a=1", "&", "=");
        Assertions.assertEquals("", m3.get("flag"));
        Assertions.assertEquals("1", m3.get("a"));
        // value含kvSep字符：以第一次出现为准
        Map<String, String> m4 = StringTools.splitMap("expr=a=b&c=1", "&", "=");
        Assertions.assertEquals("a=b", m4.get("expr"));
        // value含entrySep：不会误切（因为按entrySep先切条目，条目内kvSep才解析value）
        Map<String, String> m5 = StringTools.splitMap("url=http://x?a=1&b=2", "&", "=");
        Assertions.assertEquals("http://x?a=1", m5.get("url"));
        Assertions.assertEquals("2", m5.get("b"));
    }

    // ==================== 包含 / 计数 ====================

    @Test
    void testContains() {
        Assertions.assertTrue(StringTools.containsAny("hello", "ell", "xyz"));
        Assertions.assertFalse(StringTools.containsAny("hello", "abc", "xyz"));
        Assertions.assertTrue(StringTools.containsAll("hello world", "hello", "world"));
        Assertions.assertFalse(StringTools.containsAll("hello", "hello", "world"));
        Assertions.assertTrue(StringTools.containsIgnoreCase("Hello", "hELL"));
        Assertions.assertFalse(StringTools.containsIgnoreCase(null, "a"));
    }

    @Test
    void testCountMatches() {
        // 不重叠计数：abab 在 ababab 中匹配一次（0-4），剩余 ab 不够
        Assertions.assertEquals(1, StringTools.countMatches("ababab", "abab"));
        Assertions.assertEquals(0, StringTools.countMatches("abc", ""));
        Assertions.assertEquals(3, StringTools.countMatches("aaa", "a"));
    }

    @Test
    void testStartsEndsWithAny() {
        Assertions.assertTrue(StringTools.startsWithAny("hello", "he", "wo"));
        Assertions.assertFalse(StringTools.startsWithAny("hello", "wo"));
        Assertions.assertTrue(StringTools.endsWithAny("file.txt", ".txt", ".csv"));
        Assertions.assertFalse(StringTools.endsWithAny("file.txt", ".csv"));
    }

    // ==================== 转义 ====================

    @Test
    void testEscapeHtml() {
        Assertions.assertEquals("&lt;a&gt;&amp;&#39;b&#39;&quot;c&quot;", StringTools.escapeHtml("<a>&'b'\"c\""));
        Assertions.assertEquals("plain", StringTools.escapeHtml("plain"));
    }

    @Test
    void testEscapeJson() {
        Assertions.assertEquals("null", StringTools.escapeJson(null));
        Assertions.assertEquals("a\\\"b\\\\c", StringTools.escapeJson("a\"b\\c"));
        Assertions.assertEquals("line1\\nline2", StringTools.escapeJson("line1\nline2"));
        Assertions.assertEquals("plain", StringTools.escapeJson("plain"));
    }

    @Test
    void testEscapeXml() {
        Assertions.assertEquals("&lt;a&gt;&amp;&apos;b&apos;&quot;c&quot;", StringTools.escapeXml("<a>&'b'\"c\""));
    }

    @Test
    void testEscapeRegex() {
        // . 是正则元字符，转义后应能字面量匹配
        Assertions.assertEquals("a\\.b", StringTools.escapeRegex("a.b"));
        Assertions.assertEquals("\\(\\)", StringTools.escapeRegex("()"));
    }

    @Test
    void testUnicodeEncodeDecode() {
        Assertions.assertEquals("a\\u4e2d\\u6587", StringTools.unicodeEncode("a中文"));
        Assertions.assertEquals("a中文", StringTools.unicodeDecode("a\\u4e2d\\u6587"));
        // 非法转义原样保留
        Assertions.assertEquals("\\u123", StringTools.unicodeDecode("\\u123"));
        // 往返
        Assertions.assertEquals("你好World", StringTools.unicodeDecode(StringTools.unicodeEncode("你好World")));
    }

    // ==================== 路径 / 文件名 ====================

    @Test
    void testFileName() {
        Assertions.assertEquals("a.txt", StringTools.getFileName("/path/to/a.txt"));
        Assertions.assertEquals("a.txt", StringTools.getFileName("C:\\dir\\a.txt"));
        Assertions.assertEquals("txt", StringTools.getFileExtension("a.TXT"));
        Assertions.assertEquals("", StringTools.getFileExtension("noext"));
        Assertions.assertEquals("", StringTools.getFileExtension(".hidden"));
    }

    @Test
    void testRemoveFileExtension() {
        Assertions.assertEquals("/path/to/a", StringTools.removeFileExtension("/path/to/a.txt"));
        Assertions.assertEquals("noext", StringTools.removeFileExtension("noext"));
    }

    @Test
    void testNormalizePath() {
        Assertions.assertEquals("a/c", StringTools.normalizePath("a//b/../c"));
        Assertions.assertEquals("/a/c", StringTools.normalizePath("/a/b/../c"));
        Assertions.assertEquals("", StringTools.normalizePath(""));
        Assertions.assertEquals("a/b", StringTools.normalizePath("a/./b"));
        // 开头的..应保留（相对路径无法继续向上，不能静默丢弃）
        Assertions.assertEquals("../../a", StringTools.normalizePath("../../a"));
        Assertions.assertEquals("..", StringTools.normalizePath(".."));
        // b 回退到空，a 再回退到空，结果为当前目录
        Assertions.assertEquals(".", StringTools.normalizePath("b/../a/.."));
        // 绝对路径下根之上的..无意义，应剔除
        Assertions.assertEquals("/a", StringTools.normalizePath("/../a"));
        Assertions.assertEquals("/a/c", StringTools.normalizePath("/../../a/b/../c"));
    }

    @Test
    void testSafeFileName() {
        Assertions.assertEquals("a_b_c", StringTools.safeFileName("a<b>c"));
        Assertions.assertEquals("a_b", StringTools.safeFileName("a/b"));
        Assertions.assertEquals("a_b_d", StringTools.safeFileName("a<b>d", '_'));
    }

    // ==================== 随机串增强 ====================

    @Test
    void testRandomChinese() {
        String s = StringTools.randomChinese(5);
        Assertions.assertEquals(5, s.length());
        Assertions.assertTrue(StringTools.hasCJK(s));
    }

    @Test
    void testRandomUuidSimple() {
        String u = StringTools.randomUuidSimple();
        Assertions.assertEquals(32, u.length());
        Assertions.assertEquals(-1, u.indexOf('-'));
    }

    @Test
    void testRandomToken() {
        String t = StringTools.randomToken(16);
        Assertions.assertFalse(t.isEmpty());
        // URL safe: 不含 + /
        Assertions.assertEquals(-1, t.indexOf('+'));
        Assertions.assertEquals(-1, t.indexOf('/'));
    }

    // ==================== diff / similarity ====================

    @Test
    void testDiff() {
        String d = StringTools.diff("a\nb\nc", "a\nx\nc");
        Assertions.assertTrue(d.contains("- b"));
        Assertions.assertTrue(d.contains("+ x"));
        Assertions.assertTrue(d.contains("  a"));
    }

    @Test
    void testSimilarity() {
        // similarity 是 levenshteinSimilarity 的别名
        Assertions.assertEquals(1.0, StringTools.similarity("abc", "abc"), 0.001);
        Assertions.assertEquals(1.0, StringTools.similarity(null, null), 0.001);
        Assertions.assertEquals(0.0, StringTools.similarity(null, "a"), 0.001);
        // "abc" vs "adc" 编辑距离1，maxLen3，相似度2/3
        Assertions.assertEquals(2.0 / 3, StringTools.similarity("abc", "adc"), 0.001);
        Assertions.assertTrue(StringTools.similarity("kitten", "sitting") > 0.5);
    }

    @Test
    void testLevenshteinDistance() {
        Assertions.assertEquals(0, StringTools.levenshteinDistance("abc", "abc"));
        Assertions.assertEquals(0, StringTools.levenshteinDistance(null, null));
        Assertions.assertEquals(3, StringTools.levenshteinDistance(null, "abc"));
        Assertions.assertEquals(3, StringTools.levenshteinDistance("kitten", "sitting"));
        Assertions.assertEquals(1, StringTools.levenshteinDistance("abc", "adc"));
    }

    @Test
    void testLcsSimilarity() {
        Assertions.assertEquals(1.0, StringTools.lcsSimilarity("abc", "abc"), 0.001);
        Assertions.assertEquals(1.0, StringTools.lcsSimilarity(null, null), 0.001);
        Assertions.assertEquals(0.0, StringTools.lcsSimilarity(null, "a"), 0.001);
        // 简称匹配全称："故宫"(2字) 是 "北京故宫博物院"(7字) 的子序列，LCS=2/maxLen(7)
        Assertions.assertEquals(2.0 / 7, StringTools.lcsSimilarity("故宫", "北京故宫博物院"), 0.001);
    }

    @Test
    void testNgramSimilarity() {
        Assertions.assertEquals(1.0, StringTools.ngramSimilarity("abc", "abc"), 0.001);
        Assertions.assertEquals(1.0, StringTools.ngramSimilarity(null, null), 0.001);
        Assertions.assertEquals(0.0, StringTools.ngramSimilarity(null, "a"), 0.001);
        // 长度<=1无法构成bigram
        Assertions.assertEquals(0.0, StringTools.ngramSimilarity("a", "b"), 0.001);
        // 个别字替换：万达嘉华 vs 万达瑞华，bigram共享仅"万达"一项，余弦=1/3
        // 正好体现ngram对差异的敏感（区分度高，不像LCS会判很高）
        double d = StringTools.ngramSimilarity("万达嘉华", "万达瑞华");
        Assertions.assertEquals(1.0 / 3, d, 0.001);
        // 同串
        Assertions.assertEquals(1.0, StringTools.ngramSimilarity("hello", "hello"), 0.001);
    }

    @Test
    void testChineseUtilsDelegate() {
        // ChineseUtils 仍返回万分制，委托 StringTools
        Assertions.assertEquals(10000, ChineseUtils.lcsSimilarDegree("故宫", "故宫"));
        Assertions.assertEquals(10000, ChineseUtils.ngramSimilarDegree("故宫", "故宫"));
        int ngram = ChineseUtils.ngramSimilarDegree("万达嘉华", "万达瑞华");
        Assertions.assertEquals(3333, ngram, 5, "ngram万分制约1/3: " + ngram);
        // similarDegree 是 ngramSimilarDegree 的别名
        Assertions.assertEquals(ChineseUtils.ngramSimilarDegree("万达嘉华", "万达瑞华"),
                ChineseUtils.similarDegree("万达嘉华", "万达瑞华"));
    }
}
