package uw.webot.captcha;

import org.junit.jupiter.api.Test;
import uw.webot.captcha.CaptchaService.CaptchaOptions;
import uw.webot.captcha.CaptchaService.CaptchaResult;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CaptchaService测试类。
 */
public class CaptchaServiceTest {

    @Test
    public void testCaptchaResultSuccess() {
        CaptchaResult result = CaptchaResult.success("ABC123", 1500, 0.002);
        
        assertTrue(result.success());
        assertEquals("ABC123", result.code());
        assertEquals(1500, result.solveTimeMillis());
        assertEquals(0.002, result.cost());
        assertEquals("", result.errorMessage());
    }

    @Test
    public void testCaptchaResultFailure() {
        CaptchaResult result = CaptchaResult.failure("Recognition failed");
        
        assertFalse(result.success());
        assertEquals("", result.code());
        assertEquals(0, result.solveTimeMillis());
        assertEquals(0, result.cost());
        assertEquals("Recognition failed", result.errorMessage());
    }

    @Test
    public void testCaptchaOptionsBuilder() {
        CaptchaOptions options = new CaptchaOptions()
                .withCaptchaType("image")
                .withMinLength(4)
                .withMaxLength(6)
                .withCharset("0123456789")
                .withCaseSensitive(false)
                .withLanguage("zh")
                .withExtraParam("custom", "value");
        
        assertEquals("image", options.getCaptchaType());
        assertEquals(4, options.getMinLength());
        assertEquals(6, options.getMaxLength());
        assertEquals("0123456789", options.getCharset());
        assertFalse(options.isCaseSensitive());
        assertEquals("zh", options.getLanguage());
        assertEquals("value", options.getExtraParams().get("custom"));
    }

    @Test
    public void testCaptchaOptionsDefaults() {
        CaptchaOptions options = new CaptchaOptions();
        
        assertEquals("image", options.getCaptchaType());
        assertNull(options.getMinLength());
        assertNull(options.getMaxLength());
        assertNull(options.getCharset());
        assertFalse(options.isCaseSensitive());
        assertEquals("en", options.getLanguage());
        assertTrue(options.getExtraParams().isEmpty());
    }
}
