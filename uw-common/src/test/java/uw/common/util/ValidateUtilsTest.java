package uw.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

public class ValidateUtilsTest {

    // ==================== 身份证 ====================

    @Test
    void testIdCardValid() {
        // 110101199003077758 —— 北京，校验码正确
        Assertions.assertTrue(ValidateUtils.isChinaIdCard("110101199003077758"));
        // 440308199901010012 —— 深圳
        Assertions.assertTrue(ValidateUtils.isChinaIdCard("440308199901010012"));
        // 校验码为X
        Assertions.assertTrue(ValidateUtils.isChinaIdCard("11010120000229123X"));
        // 校验码为小写x
        Assertions.assertTrue(ValidateUtils.isChinaIdCard("11010120000229123x"));
    }

    @Test
    void testIdCardInvalid() {
        // null
        Assertions.assertFalse(ValidateUtils.isChinaIdCard(null));
        // 长度不足
        Assertions.assertFalse(ValidateUtils.isChinaIdCard("1101011990030777"));
        // 长度超出
        Assertions.assertFalse(ValidateUtils.isChinaIdCard("1101011990030777581"));
        // 空字符串
        Assertions.assertFalse(ValidateUtils.isChinaIdCard(""));
        // 前17位有字母
        Assertions.assertFalse(ValidateUtils.isChinaIdCard("11010119900307A758"));
        // 第18位非法字符
        Assertions.assertFalse(ValidateUtils.isChinaIdCard("11010119900307775A"));
        // 校验码错误（最后一位改错）
        Assertions.assertFalse(ValidateUtils.isChinaIdCard("110101199003077759"));
        // 非法月份
        Assertions.assertFalse(ValidateUtils.isChinaIdCard("110101199013077758"));
        // 非法日期
        Assertions.assertFalse(ValidateUtils.isChinaIdCard("110101199002307758"));
        // 日期为00
        Assertions.assertFalse(ValidateUtils.isChinaIdCard("110101199003007758"));
        // 月份为00
        Assertions.assertFalse(ValidateUtils.isChinaIdCard("110101199000077758"));
    }

    @Test
    void testIdCardLeapYear() {
        // 闰年2月29日合法（2000是闰年）
        Assertions.assertTrue(ValidateUtils.isChinaIdCard("11010120000229123X"));
        // 平年2月29日非法（1900不是闰年）
        Assertions.assertFalse(ValidateUtils.isChinaIdCard("110101190002291234"));
        // 非闰年2月29日非法（2100不是闰年）
        Assertions.assertFalse(ValidateUtils.isChinaIdCard("110101210002291234"));
    }

    @Test
    void testIdCardBoundaryDate() {
        // 年份1900
        Assertions.assertTrue(ValidateUtils.isChinaIdCard("110101190001010022"));
        // 年份超出范围（1800 < 1900）
        Assertions.assertFalse(ValidateUtils.isChinaIdCard("110101180001010024"));
        // 月份13
        Assertions.assertFalse(ValidateUtils.isChinaIdCard("110101199013071234"));
        // 日期32
        Assertions.assertFalse(ValidateUtils.isChinaIdCard("110101199001321234"));
    }

    @Test
    void testIdCardAllZerosDate() {
        Assertions.assertFalse(ValidateUtils.isChinaIdCard("110101190000001234"));
    }

    // ==================== 手机号 ====================

    @Test
    void testMobileValid() {
        Assertions.assertTrue(ValidateUtils.isChinaMobile("13800138000"));
        Assertions.assertTrue(ValidateUtils.isChinaMobile("15912345678"));
        Assertions.assertTrue(ValidateUtils.isChinaMobile("18600001111"));
        Assertions.assertTrue(ValidateUtils.isChinaMobile("19900001111"));
        Assertions.assertTrue(ValidateUtils.isChinaMobile("17000001111"));
        Assertions.assertTrue(ValidateUtils.isChinaMobile("16000001111"));
        Assertions.assertTrue(ValidateUtils.isChinaMobile("14000001111"));
        Assertions.assertTrue(ValidateUtils.isChinaMobile("13000000000"));
        Assertions.assertTrue(ValidateUtils.isChinaMobile("19999999999"));
    }

    @Test
    void testMobileInvalid() {
        Assertions.assertFalse(ValidateUtils.isChinaMobile(null));
        Assertions.assertFalse(ValidateUtils.isChinaMobile(""));
        Assertions.assertFalse(ValidateUtils.isChinaMobile("12345678901"));
        Assertions.assertFalse(ValidateUtils.isChinaMobile("10123456789"));
        Assertions.assertFalse(ValidateUtils.isChinaMobile("12000001111"));
        Assertions.assertFalse(ValidateUtils.isChinaMobile("1380013800"));
        Assertions.assertFalse(ValidateUtils.isChinaMobile("138001380001"));
        Assertions.assertFalse(ValidateUtils.isChinaMobile("1380013800a"));
        Assertions.assertFalse(ValidateUtils.isChinaMobile(" 13800138000"));
        Assertions.assertFalse(ValidateUtils.isChinaMobile("13800138000 "));
        Assertions.assertFalse(ValidateUtils.isChinaMobile("+8613800138000"));
    }

    // ==================== 邮箱 ====================

    @Test
    void testEmailValid() {
        Assertions.assertTrue(ValidateUtils.isEmail("test@example.com"));
        Assertions.assertTrue(ValidateUtils.isEmail("user.name@domain.com"));
        Assertions.assertTrue(ValidateUtils.isEmail("user+tag@gmail.com"));
        Assertions.assertTrue(ValidateUtils.isEmail("user-name@sub.domain.com"));
        Assertions.assertTrue(ValidateUtils.isEmail("a@b.cn"));
        Assertions.assertTrue(ValidateUtils.isEmail("test123@test.co.jp"));
        Assertions.assertTrue(ValidateUtils.isEmail("user%name@domain.org"));
        Assertions.assertTrue(ValidateUtils.isEmail("a_b@domain.com"));
    }

    @Test
    void testEmailInvalid() {
        Assertions.assertFalse(ValidateUtils.isEmail(null));
        Assertions.assertFalse(ValidateUtils.isEmail(""));
        Assertions.assertFalse(ValidateUtils.isEmail("plaintext"));
        Assertions.assertFalse(ValidateUtils.isEmail("@domain.com"));
        Assertions.assertFalse(ValidateUtils.isEmail("user@"));
        Assertions.assertFalse(ValidateUtils.isEmail("user@.com"));
        Assertions.assertFalse(ValidateUtils.isEmail("user@domain"));
        Assertions.assertFalse(ValidateUtils.isEmail("user@domain."));
        Assertions.assertFalse(ValidateUtils.isEmail("user name@domain.com"));
        // 超长
        Assertions.assertFalse(ValidateUtils.isEmail("a@" + "b".repeat(250) + ".com"));
    }

    @Test
    void testEmailSubdomain() {
        Assertions.assertTrue(ValidateUtils.isEmail("user@mail.example.com"));
        Assertions.assertTrue(ValidateUtils.isEmail("user@a.b.c.d.com"));
    }

    @Test
    void testEmailSpecialChars() {
        // 允许 . _ % + -
        Assertions.assertTrue(ValidateUtils.isEmail("a.b@example.com"));
        Assertions.assertTrue(ValidateUtils.isEmail("a_b@example.com"));
        Assertions.assertTrue(ValidateUtils.isEmail("a%b@example.com"));
        Assertions.assertTrue(ValidateUtils.isEmail("a+b@example.com"));
        Assertions.assertTrue(ValidateUtils.isEmail("a-b@example.com"));
    }

    // ==================== 中文姓名 ====================

    @Test
    void testChineseNameValid() {
        Assertions.assertTrue(ValidateUtils.isChinaName("张三"));
        Assertions.assertTrue(ValidateUtils.isChinaName("欧阳修"));
        Assertions.assertTrue(ValidateUtils.isChinaName("买买提·艾力"));
        Assertions.assertTrue(ValidateUtils.isChinaName("古丽·阿依"));
        Assertions.assertTrue(ValidateUtils.isChinaName("司马相如"));
        Assertions.assertTrue(ValidateUtils.isChinaName("爱新觉罗·溥仪"));
        Assertions.assertTrue(ValidateUtils.isChinaName("诸葛亮"));
    }

    @Test
    void testChineseNameInvalid() {
        Assertions.assertFalse(ValidateUtils.isChinaName(null));
        Assertions.assertFalse(ValidateUtils.isChinaName(""));
        // 单字
        Assertions.assertFalse(ValidateUtils.isChinaName("张"));
        // 超长（21个字）
        Assertions.assertFalse(ValidateUtils.isChinaName("一一一一一一一一一一一一一一一一一一一一一一"));
        // 包含英文
        Assertions.assertFalse(ValidateUtils.isChinaName("Tom"));
        Assertions.assertFalse(ValidateUtils.isChinaName("张三Tom"));
        // 包含数字
        Assertions.assertFalse(ValidateUtils.isChinaName("张三1"));
        // 包含空格
        Assertions.assertFalse(ValidateUtils.isChinaName("张 三"));
        // 首位间隔号
        Assertions.assertFalse(ValidateUtils.isChinaName("·艾力"));
        // 末位间隔号
        Assertions.assertFalse(ValidateUtils.isChinaName("艾力·"));
        // 连续间隔号
        Assertions.assertFalse(ValidateUtils.isChinaName("买买提··艾力"));
    }

    // ==================== 纯数字 ====================

    @Test
    void testDigitsValid() {
        Assertions.assertTrue(ValidateUtils.isDigits("0"));
        Assertions.assertTrue(ValidateUtils.isDigits("123"));
        Assertions.assertTrue(ValidateUtils.isDigits("0123456789"));
    }

    @Test
    void testDigitsInvalid() {
        Assertions.assertFalse(ValidateUtils.isDigits(null));
        Assertions.assertFalse(ValidateUtils.isDigits(""));
        Assertions.assertFalse(ValidateUtils.isDigits("12a3"));
        Assertions.assertFalse(ValidateUtils.isDigits("12.3"));
        Assertions.assertFalse(ValidateUtils.isDigits("-123"));
        Assertions.assertFalse(ValidateUtils.isDigits(" 123"));
    }

    // ==================== 纯字母 ====================

    @Test
    void testLettersValid() {
        Assertions.assertTrue(ValidateUtils.isLetters("abc"));
        Assertions.assertTrue(ValidateUtils.isLetters("ABC"));
        Assertions.assertTrue(ValidateUtils.isLetters("HelloWorld"));
    }

    @Test
    void testLettersInvalid() {
        Assertions.assertFalse(ValidateUtils.isLetters(null));
        Assertions.assertFalse(ValidateUtils.isLetters(""));
        Assertions.assertFalse(ValidateUtils.isLetters("abc123"));
        Assertions.assertFalse(ValidateUtils.isLetters("abc def"));
        Assertions.assertFalse(ValidateUtils.isLetters("abc-def"));
    }

    // ==================== 字母数字 ====================

    @Test
    void testAlphanumericValid() {
        Assertions.assertTrue(ValidateUtils.isAlphanumeric("abc123"));
        Assertions.assertTrue(ValidateUtils.isAlphanumeric("ABC"));
        Assertions.assertTrue(ValidateUtils.isAlphanumeric("123"));
        Assertions.assertTrue(ValidateUtils.isAlphanumeric("a1B2c3"));
    }

    @Test
    void testAlphanumericInvalid() {
        Assertions.assertFalse(ValidateUtils.isAlphanumeric(null));
        Assertions.assertFalse(ValidateUtils.isAlphanumeric(""));
        Assertions.assertFalse(ValidateUtils.isAlphanumeric("abc 123"));
        Assertions.assertFalse(ValidateUtils.isAlphanumeric("abc-123"));
        Assertions.assertFalse(ValidateUtils.isAlphanumeric("abc_123"));
    }

    // ==================== 密码强度 ====================

    @Test
    void testStrongPasswordValid() {
        Assertions.assertTrue(ValidateUtils.isStrongPassword("abc123", 6, 20));
        Assertions.assertTrue(ValidateUtils.isStrongPassword("Password1", 8, 20));
        Assertions.assertTrue(ValidateUtils.isStrongPassword("a1", 2, 10));
        Assertions.assertTrue(ValidateUtils.isStrongPassword("Abc123!@#", 6, 20));
    }

    @Test
    void testStrongPasswordInvalid() {
        // null
        Assertions.assertFalse(ValidateUtils.isStrongPassword(null, 6, 20));
        // 太短
        Assertions.assertFalse(ValidateUtils.isStrongPassword("ab1", 6, 20));
        // 太长
        Assertions.assertFalse(ValidateUtils.isStrongPassword("abc12345678901234567890", 6, 20));
        // 纯数字
        Assertions.assertFalse(ValidateUtils.isStrongPassword("123456", 6, 20));
        // 纯字母
        Assertions.assertFalse(ValidateUtils.isStrongPassword("abcdef", 6, 20));
    }

    // ==================== 统一社会信用代码 ====================

    @Test
    void testUsccValid() {
        Assertions.assertTrue(ValidateUtils.isChinaUscc("91110108592330437D"));
        Assertions.assertTrue(ValidateUtils.isChinaUscc("91350100M000100Y43"));
    }

    @Test
    void testUsccInvalid() {
        Assertions.assertFalse(ValidateUtils.isChinaUscc(null));
        Assertions.assertFalse(ValidateUtils.isChinaUscc(""));
        Assertions.assertFalse(ValidateUtils.isChinaUscc("91110108592330437"));   // 长度不足
        Assertions.assertFalse(ValidateUtils.isChinaUscc("91110108592330437CC")); // 长度超出
        Assertions.assertFalse(ValidateUtils.isChinaUscc("91110108592330437E")); // 校验码错误
        Assertions.assertFalse(ValidateUtils.isChinaUscc("11110108592330437C")); // 校验码错误
        // 含非法字符（I、O不在编码字符集中）
        Assertions.assertFalse(ValidateUtils.isChinaUscc("9I110108592330437C"));
    }

    // ==================== 车牌号 ====================

    @Test
    void testPlateNoValid() {
        // 普通车牌（7位）
        Assertions.assertTrue(ValidateUtils.isChinaPlateNo("京A12345"));
        Assertions.assertTrue(ValidateUtils.isChinaPlateNo("沪B67890"));
        Assertions.assertTrue(ValidateUtils.isChinaPlateNo("粤Z123AB"));
        Assertions.assertTrue(ValidateUtils.isChinaPlateNo("川A12345"));
        Assertions.assertTrue(ValidateUtils.isChinaPlateNo("琼A12345"));
        // 新能源车牌（8位）
        Assertions.assertTrue(ValidateUtils.isChinaPlateNo("京AD12345"));
        Assertions.assertTrue(ValidateUtils.isChinaPlateNo("沪BF12345"));
    }

    @Test
    void testPlateNoInvalid() {
        Assertions.assertFalse(ValidateUtils.isChinaPlateNo(null));
        Assertions.assertFalse(ValidateUtils.isChinaPlateNo(""));
        // 少于7位
        Assertions.assertFalse(ValidateUtils.isChinaPlateNo("京A1234"));
        // 省份错误
        Assertions.assertFalse(ValidateUtils.isChinaPlateNo("AA12345"));
        // 包含I和O（不允许）
        Assertions.assertFalse(ValidateUtils.isChinaPlateNo("京I12345"));
        Assertions.assertFalse(ValidateUtils.isChinaPlateNo("京A1234O"));
    }

    // ==================== 整数校验 ====================

    @Test
    void testIntegerValid() {
        Assertions.assertTrue(ValidateUtils.isInteger("0"));
        Assertions.assertTrue(ValidateUtils.isInteger("123"));
        Assertions.assertTrue(ValidateUtils.isInteger("-123"));
        Assertions.assertTrue(ValidateUtils.isInteger("+456"));
        Assertions.assertTrue(ValidateUtils.isInteger(String.valueOf(Long.MAX_VALUE)));
        Assertions.assertTrue(ValidateUtils.isInteger(String.valueOf(Long.MIN_VALUE)));
    }

    @Test
    void testIntegerInvalid() {
        Assertions.assertFalse(ValidateUtils.isInteger(null));
        Assertions.assertFalse(ValidateUtils.isInteger(""));
        Assertions.assertFalse(ValidateUtils.isInteger("12.3"));
        Assertions.assertFalse(ValidateUtils.isInteger("12a3"));
        Assertions.assertFalse(ValidateUtils.isInteger("+-1"));
        Assertions.assertFalse(ValidateUtils.isInteger("--1"));
        Assertions.assertFalse(ValidateUtils.isInteger("999999999999999999999999999999"));
    }

    // ==================== 正整数 ====================

    @Test
    void testPositiveIntegerValid() {
        Assertions.assertTrue(ValidateUtils.isPositiveInteger("1"));
        Assertions.assertTrue(ValidateUtils.isPositiveInteger("123"));
        Assertions.assertTrue(ValidateUtils.isPositiveInteger("999999"));
    }

    @Test
    void testPositiveIntegerInvalid() {
        Assertions.assertFalse(ValidateUtils.isPositiveInteger(null));
        Assertions.assertFalse(ValidateUtils.isPositiveInteger("0"));
        Assertions.assertFalse(ValidateUtils.isPositiveInteger("-1"));
        Assertions.assertFalse(ValidateUtils.isPositiveInteger("+1"));
        Assertions.assertFalse(ValidateUtils.isPositiveInteger("01"));
        Assertions.assertFalse(ValidateUtils.isPositiveInteger("1.5"));
    }

    // ==================== 非负整数 ====================

    @Test
    void testNonNegativeIntegerValid() {
        Assertions.assertTrue(ValidateUtils.isNonNegativeInteger("0"));
        Assertions.assertTrue(ValidateUtils.isNonNegativeInteger("123"));
    }

    @Test
    void testNonNegativeIntegerInvalid() {
        Assertions.assertFalse(ValidateUtils.isNonNegativeInteger(null));
        Assertions.assertFalse(ValidateUtils.isNonNegativeInteger("-1"));
        Assertions.assertFalse(ValidateUtils.isNonNegativeInteger("01"));
        Assertions.assertFalse(ValidateUtils.isNonNegativeInteger("1.0"));
    }

    // ==================== 负整数 ====================

    @Test
    void testNegativeIntegerValid() {
        Assertions.assertTrue(ValidateUtils.isNegativeInteger("-1"));
        Assertions.assertTrue(ValidateUtils.isNegativeInteger("-999"));
    }

    @Test
    void testNegativeIntegerInvalid() {
        Assertions.assertFalse(ValidateUtils.isNegativeInteger(null));
        Assertions.assertFalse(ValidateUtils.isNegativeInteger("0"));
        Assertions.assertFalse(ValidateUtils.isNegativeInteger("1"));
        Assertions.assertFalse(ValidateUtils.isNegativeInteger("-0"));
        Assertions.assertFalse(ValidateUtils.isNegativeInteger("-01"));
        Assertions.assertFalse(ValidateUtils.isNegativeInteger("--1"));
        Assertions.assertFalse(ValidateUtils.isNegativeInteger("-1.5"));
    }

    // ==================== 浮点数 ====================

    @Test
    void testDecimalValid() {
        Assertions.assertTrue(ValidateUtils.isDecimal("0"));
        Assertions.assertTrue(ValidateUtils.isDecimal("123"));
        Assertions.assertTrue(ValidateUtils.isDecimal("-123"));
        Assertions.assertTrue(ValidateUtils.isDecimal("12.34"));
        Assertions.assertTrue(ValidateUtils.isDecimal("-12.34"));
        Assertions.assertTrue(ValidateUtils.isDecimal("+12.34"));
        Assertions.assertTrue(ValidateUtils.isDecimal("0.0"));
        Assertions.assertTrue(ValidateUtils.isDecimal("0.5"));
        Assertions.assertTrue(ValidateUtils.isDecimal("100.00"));
    }

    @Test
    void testDecimalInvalid() {
        Assertions.assertFalse(ValidateUtils.isDecimal(null));
        Assertions.assertFalse(ValidateUtils.isDecimal(""));
        Assertions.assertFalse(ValidateUtils.isDecimal("12.34.56"));
        Assertions.assertFalse(ValidateUtils.isDecimal("abc"));
        Assertions.assertFalse(ValidateUtils.isDecimal("12a.34"));
        Assertions.assertFalse(ValidateUtils.isDecimal("."));
        Assertions.assertFalse(ValidateUtils.isDecimal("-."));
    }

    @Test
    void testPositiveDecimalValid() {
        Assertions.assertTrue(ValidateUtils.isPositiveDecimal("1"));
        Assertions.assertTrue(ValidateUtils.isPositiveDecimal("0.01"));
        Assertions.assertTrue(ValidateUtils.isPositiveDecimal("999.99"));
    }

    @Test
    void testPositiveDecimalInvalid() {
        Assertions.assertFalse(ValidateUtils.isPositiveDecimal("0"));
        Assertions.assertFalse(ValidateUtils.isPositiveDecimal("-1"));
        Assertions.assertFalse(ValidateUtils.isPositiveDecimal("-0.01"));
        Assertions.assertFalse(ValidateUtils.isPositiveDecimal(null));
    }

    @Test
    void testNonNegativeDecimalValid() {
        Assertions.assertTrue(ValidateUtils.isNonNegativeDecimal("0"));
        Assertions.assertTrue(ValidateUtils.isNonNegativeDecimal("0.0"));
        Assertions.assertTrue(ValidateUtils.isNonNegativeDecimal("123.45"));
    }

    @Test
    void testNonNegativeDecimalInvalid() {
        Assertions.assertFalse(ValidateUtils.isNonNegativeDecimal("-1"));
        Assertions.assertFalse(ValidateUtils.isNonNegativeDecimal("-0.01"));
        Assertions.assertFalse(ValidateUtils.isNonNegativeDecimal(null));
    }

    @Test
    void testNegativeDecimalValid() {
        Assertions.assertTrue(ValidateUtils.isNegativeDecimal("-1"));
        Assertions.assertTrue(ValidateUtils.isNegativeDecimal("-0.01"));
    }

    @Test
    void testNegativeDecimalInvalid() {
        Assertions.assertFalse(ValidateUtils.isNegativeDecimal("0"));
        Assertions.assertFalse(ValidateUtils.isNegativeDecimal("1"));
        Assertions.assertFalse(ValidateUtils.isNegativeDecimal(null));
    }

    @Test
    void testDecimalWithScaleValid() {
        Assertions.assertTrue(ValidateUtils.isDecimalWithScale("12.34", 2));
        Assertions.assertTrue(ValidateUtils.isDecimalWithScale("12.3", 2));
        Assertions.assertTrue(ValidateUtils.isDecimalWithScale("12", 2));
        Assertions.assertTrue(ValidateUtils.isDecimalWithScale("12.345", 3));
        Assertions.assertTrue(ValidateUtils.isDecimalWithScale("0.1", 1));
    }

    @Test
    void testDecimalWithScaleInvalid() {
        Assertions.assertFalse(ValidateUtils.isDecimalWithScale("12.345", 2));
        Assertions.assertFalse(ValidateUtils.isDecimalWithScale("12.34", 1));
        Assertions.assertFalse(ValidateUtils.isDecimalWithScale(null, 2));
        Assertions.assertFalse(ValidateUtils.isDecimalWithScale("abc", 2));
    }

    @Test
    void testInRangeValid() {
        Assertions.assertTrue(ValidateUtils.isInRange("5", 0, 10));
        Assertions.assertTrue(ValidateUtils.isInRange("0", 0, 10));
        Assertions.assertTrue(ValidateUtils.isInRange("10", 0, 10));
        Assertions.assertTrue(ValidateUtils.isInRange("5.5", 0, 10));
        Assertions.assertTrue(ValidateUtils.isInRange("-1", -5, 5));
    }

    @Test
    void testInRangeInvalid() {
        Assertions.assertFalse(ValidateUtils.isInRange("-1", 0, 10));
        Assertions.assertFalse(ValidateUtils.isInRange("11", 0, 10));
        Assertions.assertFalse(ValidateUtils.isInRange("10.01", 0, 10));
        Assertions.assertFalse(ValidateUtils.isInRange(null, 0, 10));
        Assertions.assertFalse(ValidateUtils.isInRange("abc", 0, 10));
    }

    // ==================== 日期校验 ====================

    @Test
    void testDateValid() {
        Assertions.assertTrue(ValidateUtils.isDate("2023-01-01"));
        Assertions.assertTrue(ValidateUtils.isDate("2000-02-29")); // 闰年
        Assertions.assertTrue(ValidateUtils.isDate("1900-01-01"));
        Assertions.assertTrue(ValidateUtils.isDate("2099-12-31"));
    }

    @Test
    void testDateInvalid() {
        Assertions.assertFalse(ValidateUtils.isDate(null));
        Assertions.assertFalse(ValidateUtils.isDate(""));
        Assertions.assertFalse(ValidateUtils.isDate("2023-13-01")); // 非法月份
        Assertions.assertFalse(ValidateUtils.isDate("2023-02-29")); // 平年
        Assertions.assertFalse(ValidateUtils.isDate("2023-00-01")); // 零月
        Assertions.assertFalse(ValidateUtils.isDate("2023-01-00")); // 零日
        Assertions.assertFalse(ValidateUtils.isDate("2023-01-32")); // 超出日期
        Assertions.assertFalse(ValidateUtils.isDate("2023/01/01")); // 格式不对
        Assertions.assertFalse(ValidateUtils.isDate("20230101"));   // 格式不对
        Assertions.assertFalse(ValidateUtils.isDate("2023-1-1"));   // 不补零
        Assertions.assertFalse(ValidateUtils.isDate("abc"));
    }

    @Test
    void testDateWithPatternValid() {
        Assertions.assertTrue(ValidateUtils.isDate("2023/01/01", "yyyy/MM/dd"));
        Assertions.assertTrue(ValidateUtils.isDate("20230101", "yyyyMMdd"));
    }

    @Test
    void testDateWithPatternInvalid() {
        Assertions.assertFalse(ValidateUtils.isDate("2023-01-01", "yyyyMMdd"));
        Assertions.assertFalse(ValidateUtils.isDate(null, "yyyy-MM-dd"));
        Assertions.assertFalse(ValidateUtils.isDate("2023-01-01", null));
        Assertions.assertFalse(ValidateUtils.isDate("2023-13-01", "yyyy-MM-dd"));
    }

    @Test
    void testDateInRangeValid() {
        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end = LocalDate.of(2023, 12, 31);
        Assertions.assertTrue(ValidateUtils.isDateInRange("2023-06-15", start, end));
        Assertions.assertTrue(ValidateUtils.isDateInRange("2023-01-01", start, end));
        Assertions.assertTrue(ValidateUtils.isDateInRange("2023-12-31", start, end));
    }

    @Test
    void testDateInRangeInvalid() {
        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end = LocalDate.of(2023, 12, 31);
        Assertions.assertFalse(ValidateUtils.isDateInRange("2022-12-31", start, end));
        Assertions.assertFalse(ValidateUtils.isDateInRange("2024-01-01", start, end));
        Assertions.assertFalse(ValidateUtils.isDateInRange("not-a-date", start, end));
        Assertions.assertFalse(ValidateUtils.isDateInRange(null, start, end));
    }

    // ==================== 时间校验 ====================

    @Test
    void testTimeValid() {
        Assertions.assertTrue(ValidateUtils.isTime("00:00:00"));
        Assertions.assertTrue(ValidateUtils.isTime("23:59:59"));
        Assertions.assertTrue(ValidateUtils.isTime("12:30:45"));
    }

    @Test
    void testTimeInvalid() {
        Assertions.assertFalse(ValidateUtils.isTime(null));
        Assertions.assertFalse(ValidateUtils.isTime(""));
        Assertions.assertFalse(ValidateUtils.isTime("24:00:00")); // 超出小时
        Assertions.assertFalse(ValidateUtils.isTime("23:60:00")); // 超出分钟
        Assertions.assertFalse(ValidateUtils.isTime("23:59:60")); // 超出秒
        Assertions.assertFalse(ValidateUtils.isTime("12:30"));    // 格式不对
        Assertions.assertFalse(ValidateUtils.isTime("12:30:45.123")); // 格式不对
        Assertions.assertFalse(ValidateUtils.isTime("1:30:00"));  // 不补零
        Assertions.assertFalse(ValidateUtils.isTime("ab:cd:ef"));
    }

    @Test
    void testTimeWithPatternValid() {
        Assertions.assertTrue(ValidateUtils.isTime("14:30", "HH:mm"));
        Assertions.assertTrue(ValidateUtils.isTime("00:00", "HH:mm"));
        Assertions.assertTrue(ValidateUtils.isTime("23:59", "HH:mm"));
    }

    @Test
    void testTimeWithPatternInvalid() {
        Assertions.assertFalse(ValidateUtils.isTime(null, "HH:mm"));
        Assertions.assertFalse(ValidateUtils.isTime("14:30", null));
        Assertions.assertFalse(ValidateUtils.isTime("25:00", "HH:mm"));
        Assertions.assertFalse(ValidateUtils.isTime("abc", "HH:mm"));
    }

    // ==================== 日期时间校验 ====================

    @Test
    void testDateTimeValid() {
        Assertions.assertTrue(ValidateUtils.isDateTime("2023-01-01 00:00:00"));
        Assertions.assertTrue(ValidateUtils.isDateTime("2023-06-15 12:30:45"));
        Assertions.assertTrue(ValidateUtils.isDateTime("2023-12-31 23:59:59"));
    }

    @Test
    void testDateTimeInvalid() {
        Assertions.assertFalse(ValidateUtils.isDateTime(null));
        Assertions.assertFalse(ValidateUtils.isDateTime(""));
        Assertions.assertFalse(ValidateUtils.isDateTime("2023-01-01 24:00:00"));
        Assertions.assertFalse(ValidateUtils.isDateTime("2023-13-01 00:00:00"));
        Assertions.assertFalse(ValidateUtils.isDateTime("2023-01-01T00:00:00")); // T分隔
        Assertions.assertFalse(ValidateUtils.isDateTime("2023-01-01 12:30"));    // 缺秒
        Assertions.assertFalse(ValidateUtils.isDateTime("abc"));
    }

    @Test
    void testDateTimeWithPatternValid() {
        Assertions.assertTrue(ValidateUtils.isDateTime("2023-01-01T12:30:45", "yyyy-MM-dd'T'HH:mm:ss"));
        Assertions.assertTrue(ValidateUtils.isDateTime("2023-01-01 12:30", "yyyy-MM-dd HH:mm"));
        Assertions.assertTrue(ValidateUtils.isDateTime("20230101123045", "yyyyMMddHHmmss"));
    }

    @Test
    void testDateTimeWithPatternInvalid() {
        Assertions.assertFalse(ValidateUtils.isDateTime("2023-01-01 12:30:45", "yyyy-MM-dd'T'HH:mm:ss"));
        Assertions.assertFalse(ValidateUtils.isDateTime(null, "yyyy-MM-dd HH:mm:ss"));
        Assertions.assertFalse(ValidateUtils.isDateTime("2023-01-01 12:30:45", null));
    }

    // ==================== URL校验 ====================

    @Test
    void testUrlValid() {
        Assertions.assertTrue(ValidateUtils.isUrl("http://example.com"));
        Assertions.assertTrue(ValidateUtils.isUrl("https://example.com"));
        Assertions.assertTrue(ValidateUtils.isUrl("https://example.com/path?q=1#anchor"));
        Assertions.assertTrue(ValidateUtils.isUrl("ftp://files.example.com/dir/file.txt"));
        Assertions.assertTrue(ValidateUtils.isUrl("http://192.168.1.1:8080/api"));
        Assertions.assertTrue(ValidateUtils.isUrl("https://example.com:443/path"));
    }

    @Test
    void testUrlInvalid() {
        Assertions.assertFalse(ValidateUtils.isUrl(null));
        Assertions.assertFalse(ValidateUtils.isUrl(""));
        Assertions.assertFalse(ValidateUtils.isUrl("example.com"));
        Assertions.assertFalse(ValidateUtils.isUrl("www.example.com"));
        Assertions.assertFalse(ValidateUtils.isUrl("http://"));
        Assertions.assertFalse(ValidateUtils.isUrl("file:///tmp/file.txt")); // 不支持file协议
    }

    // ==================== IPv4校验 ====================

    @Test
    void testIpv4Valid() {
        Assertions.assertTrue(ValidateUtils.isIpv4("192.168.1.1"));
        Assertions.assertTrue(ValidateUtils.isIpv4("0.0.0.0"));
        Assertions.assertTrue(ValidateUtils.isIpv4("255.255.255.255"));
        Assertions.assertTrue(ValidateUtils.isIpv4("10.0.0.1"));
        Assertions.assertTrue(ValidateUtils.isIpv4("127.0.0.1"));
    }

    @Test
    void testIpv4Invalid() {
        Assertions.assertFalse(ValidateUtils.isIpv4(null));
        Assertions.assertFalse(ValidateUtils.isIpv4(""));
        Assertions.assertFalse(ValidateUtils.isIpv4("256.0.0.1"));
        Assertions.assertFalse(ValidateUtils.isIpv4("192.168.1"));
        Assertions.assertFalse(ValidateUtils.isIpv4("192.168.1.1.1"));
        Assertions.assertFalse(ValidateUtils.isIpv4("192.168.1.-1"));
        Assertions.assertFalse(ValidateUtils.isIpv4("192.168.1.999"));
        Assertions.assertFalse(ValidateUtils.isIpv4("a.b.c.d"));
    }

    // ==================== IPv6校验 ====================

    @Test
    void testIpv6Valid() {
        Assertions.assertTrue(ValidateUtils.isIpv6("2001:0db8:85a3:0000:0000:8a2e:0370:7334"));
        Assertions.assertTrue(ValidateUtils.isIpv6("0000:0000:0000:0000:0000:0000:0000:0001"));
        Assertions.assertTrue(ValidateUtils.isIpv6("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"));
    }

    @Test
    void testIpv6Invalid() {
        Assertions.assertFalse(ValidateUtils.isIpv6(null));
        Assertions.assertFalse(ValidateUtils.isIpv6(""));
        Assertions.assertFalse(ValidateUtils.isIpv6("::1")); // 缩写格式不支持
        Assertions.assertFalse(ValidateUtils.isIpv6("2001:db8::1")); // 缩写格式不支持
        Assertions.assertFalse(ValidateUtils.isIpv6("192.168.1.1")); // IPv4
    }

    // ==================== IP（自动识别） ====================

    @Test
    void testIpAuto() {
        Assertions.assertTrue(ValidateUtils.isIp("192.168.1.1"));
        Assertions.assertTrue(ValidateUtils.isIp("0000:0000:0000:0000:0000:0000:0000:0001"));
        Assertions.assertFalse(ValidateUtils.isIp(null));
        Assertions.assertFalse(ValidateUtils.isIp("abc"));
    }

    // ==================== 长度校验 ====================

    @Test
    void testLengthInRangeValid() {
        Assertions.assertTrue(ValidateUtils.isLengthInRange("abc", 1, 5));
        Assertions.assertTrue(ValidateUtils.isLengthInRange("abc", 3, 3));
        Assertions.assertTrue(ValidateUtils.isLengthInRange("", 0, 5));
    }

    @Test
    void testLengthInRangeNullAsEmpty() {
        Assertions.assertTrue(ValidateUtils.isLengthInRange(null, 0, 5));
        Assertions.assertFalse(ValidateUtils.isLengthInRange(null, 1, 5));
    }

    @Test
    void testLengthInRangeInvalid() {
        Assertions.assertFalse(ValidateUtils.isLengthInRange("abcdef", 1, 5));
        Assertions.assertFalse(ValidateUtils.isLengthInRange("", 1, 5));
        Assertions.assertFalse(ValidateUtils.isLengthInRange("a", 2, 5));
    }

    // ==================== 非空校验 ====================

    @Test
    void testIsNotEmpty() {
        Assertions.assertTrue(ValidateUtils.isNotEmpty("abc"));
        Assertions.assertTrue(ValidateUtils.isNotEmpty(" "));
        Assertions.assertFalse(ValidateUtils.isNotEmpty(null));
        Assertions.assertFalse(ValidateUtils.isNotEmpty(""));
    }

    @Test
    void testIsNotBlank() {
        Assertions.assertTrue(ValidateUtils.isNotBlank("abc"));
        Assertions.assertTrue(ValidateUtils.isNotBlank(" abc "));
        Assertions.assertFalse(ValidateUtils.isNotBlank(null));
        Assertions.assertFalse(ValidateUtils.isNotBlank(""));
        Assertions.assertFalse(ValidateUtils.isNotBlank(" "));
        Assertions.assertFalse(ValidateUtils.isNotBlank("   "));
        Assertions.assertFalse(ValidateUtils.isNotBlank("\t\n"));
    }
}
