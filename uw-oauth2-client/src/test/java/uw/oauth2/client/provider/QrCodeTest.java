package uw.oauth2.client.provider;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import uw.kv.GlobalCache;
import uw.oauth2.client.conf.UwOAuth2ClientProperties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 二维码登录功能测试
 */
class QrCodeTest {

    private UwOAuth2ClientProperties.ProviderConfig wechatConfig;
    private UwOAuth2ClientProperties.ProviderConfig alipayConfig;
    private String redirectUri = "https://example.com/oauth2/redirect";
    private String qrCodeUrl = "https://guest-ui-test.west-qr.com/oauth2/qrcode/";

    /**
     * GlobalCache 为静态门面（构造注入 static template）：纯 JUnit 环境下以 mock 模板手工初始化，
     * 修复既有失败（此前 opsForValue 为 null NPE，见 261119 提交记录）。
     */
    @BeforeAll
    static void initGlobalCache() {
        RedisTemplate<String, byte[]> mockTemplate = Mockito.mock(RedisTemplate.class);
        Mockito.when(mockTemplate.opsForValue()).thenReturn(Mockito.mock(ValueOperations.class));
        new GlobalCache(mockTemplate);
    }

    @BeforeEach
    void setUp() {
        // 初始化微信配置
        wechatConfig = new UwOAuth2ClientProperties.ProviderConfig();
        wechatConfig.setClientId("wx1234567890");
        wechatConfig.setClientSecret("secret123456");
        wechatConfig.setAuthUri("https://open.weixin.qq.com/connect/oauth2/authorize");
        wechatConfig.setAuthScope("snsapi_userinfo");
        wechatConfig.setTokenUri("https://api.weixin.qq.com/sns/oauth2/access_token");
        wechatConfig.setUserInfoUri("https://api.weixin.qq.com/sns/userinfo");

        // 初始化支付宝配置
        alipayConfig = new UwOAuth2ClientProperties.ProviderConfig();
        alipayConfig.setClientId("2021000000000000");
        alipayConfig.setClientSecret("alipay-secret-123456");
        alipayConfig.setAuthUri("https://openauth.alipay.com/oauth2/authorize.htm");
        alipayConfig.setAuthScope("auth_base");
        alipayConfig.setTokenUri("https://openapi.alipay.com/gateway.do?method=alipay.system.oauth.token");
        alipayConfig.setUserInfoUri("https://openapi.alipay.com/gateway.do?method=alipay.user.info.share");
    }

    @Test
    void testWechatQrCodeGeneration() {
        WechatOAuth2Provider provider = new WechatOAuth2Provider("wechat", wechatConfig, redirectUri, qrCodeUrl);
        String qrCodeUrl = provider.buildQrCode();

        System.out.println("Generated WeChat QR Code URL: " + qrCodeUrl);

        // 断言对齐现实现（qrcodeUri + authStateId 前端渲染模式，平台原生扫码 URL 语义已随重构退役）
        assertTrue(qrCodeUrl.startsWith("https://guest-ui-test.west-qr.com/oauth2/qrcode/"), "URL 应以 qrcodeUri 前缀开头");
        String stateId = qrCodeUrl.substring("https://guest-ui-test.west-qr.com/oauth2/qrcode/".length());
        assertFalse(stateId.isBlank(), "URL 应携带非空 authStateId");
    }

    @Test
    void testAlipayQrCodeGeneration() {
        AlipayOAuth2Provider provider = new AlipayOAuth2Provider("alipay", alipayConfig, redirectUri, qrCodeUrl);
        String qrCodeUrl = provider.buildQrCode();

        System.out.println("Generated Alipay QR Code URL: " + qrCodeUrl);

        // 断言对齐现实现（qrcodeUri + authStateId 前端渲染模式）
        assertTrue(qrCodeUrl.startsWith("https://guest-ui-test.west-qr.com/oauth2/qrcode/"), "URL 应以 qrcodeUri 前缀开头");
        assertFalse(qrCodeUrl.substring("https://guest-ui-test.west-qr.com/oauth2/qrcode/".length()).isBlank(), "URL 应携带非空 authStateId");
    }
}