package uw.common.vo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import uw.common.constant.ValidateResponseCode;

import java.util.Locale;

/**
 * {@link uw.common.response.ResponseCode} 的 i18n 解析与 basename 自动加载验证。
 * <p>
 * 以 {@link ValidateResponseCode}（codePrefix=uw.validate，资源包 i18n/messages/uw_validate）为样本，
 * 验证：双方法模式、basename 按 codePrefix 自动推导、不同 Locale 命中对应资源、资源缺失回退兜底文案。
 */
public class ResponseCodeI18nTest {

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @BeforeEach
    void setUp() {
        // 默认中文，单测覆盖
    }

    @Test
    void testCodePrefixAndFullCode() {
        ValidateResponseCode code = ValidateResponseCode.NOT_NULL;
        Assertions.assertEquals("uw.validate", code.codePrefix(), "codePrefix");
        Assertions.assertEquals("not.null", code.code(), "短码由枚举名派生");
        Assertions.assertEquals("uw.validate.not.null", code.getCode(), "完整码 = prefix + 短码");
    }

    @Test
    void testGetMessageIsRawFallback() {
        // getMessage() 是原始兜底文案，不经 i18n
        Assertions.assertEquals("不能为NULL", ValidateResponseCode.NOT_NULL.getMessage());
    }

    @Test
    void testI18nZhCn() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        // 命中 uw_validate_zh_CN.properties: not.null=不能为NULL
        Assertions.assertEquals("不能为NULL", ValidateResponseCode.NOT_NULL.getLocalizedMessage());
    }

    @Test
    void testI18nEn() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        // 命中 uw_validate_en.properties: not.null=Cannot be NULL
        Assertions.assertEquals("Cannot be NULL", ValidateResponseCode.NOT_NULL.getLocalizedMessage(),
                "英文 Locale 应命中 uw_validate_en 资源");
    }

    @Test
    void testI18nUnknownLocaleFallbackToDefault() {
        // 没有对应资源文件的 Locale（如日语以外的冷门语言），应回退到默认 properties 或兜底文案
        LocaleContextHolder.setLocale(Locale.KOREAN);
        String msg = ValidateResponseCode.NOT_NULL.getLocalizedMessage();
        // 至少应是非空字符串（命中默认 uw_validate.properties 或回退 getMessage 兜底）
        Assertions.assertNotNull(msg);
        Assertions.assertFalse(msg.isEmpty());
    }
}
