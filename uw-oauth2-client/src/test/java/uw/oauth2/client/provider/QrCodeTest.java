package uw.oauth2.client.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uw.oauth2.client.conf.OAuth2ClientProperties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 二维码登录功能测试
 */
class QrCodeTest {

    private OAuth2ClientProperties.ProviderConfig wechatConfig;
    private OAuth2ClientProperties.ProviderConfig alipayConfig;
    private String redirectUri = "https://example.com/oauth2/redirect";
    private String qrCodeUrl = "https://guest-ui-test.west-qr.com/oauth2/qrcode/";

    @BeforeEach
    void setUp() {
        // 初始化微信配置
        wechatConfig = new OAuth2ClientProperties.ProviderConfig();
        wechatConfig.setClientId("wx1234567890");
        wechatConfig.setClientSecret("secret123456");
        wechatConfig.setAuthUri("https://open.weixin.qq.com/connect/oauth2/authorize");
        wechatConfig.setAuthScope("snsapi_userinfo");
        wechatConfig.setTokenUri("https://api.weixin.qq.com/sns/oauth2/access_token");
        wechatConfig.setUserInfoUri("https://api.weixin.qq.com/sns/userinfo");

        // 初始化支付宝配置
        alipayConfig = new OAuth2ClientProperties.ProviderConfig();
        alipayConfig.setClientId("2021000000000000");
        alipayConfig.setClientSecret("alipay-secret-123456");
        alipayConfig.setAuthUri("https://openauth.alipay.com/oauth2/authorize.htm");
        alipayConfig.setAuthScope("auth_base");
        alipayConfig.setTokenUri("https://openapi.alipay.com/gateway.do?method=alipay.system.oauth.token");
        alipayConfig.setUserInfoUri("https://openapi.alipay.com/gateway.do?method=alipay.user.info.share");
    }

    @Test
    void testWechatQrCodeGeneration() {
        WechatOAuth2Provider provider = new WechatOAuth2Provider("wechat", wechatConfig, redirectUri,qrCodeUrl);
        String qrCodeUrl = provider.buildQrCode();
        
        System.out.println("Generated WeChat QR Code URL: " + qrCodeUrl);
        
        // 验证URL是否使用了二维码特定的URI
        assertTrue(qrCodeUrl.contains("qrconnect"), "URL should use QR code specific URI");
        assertTrue(qrCodeUrl.contains("appid=wx1234567890"), "URL should contain appid");
        assertTrue(qrCodeUrl.contains("redirect_uri="), "URL should contain redirect_uri");
        assertTrue(qrCodeUrl.contains("response_type=code"), "URL should contain response_type=code");
        assertTrue(qrCodeUrl.contains("scope=snsapi_login"), "URL should use QR code specific scope");
        assertTrue(qrCodeUrl.endsWith("#wechat_redirect"), "URL should end with #wechat_redirect");
    }

    @Test
    void testAlipayQrCodeGeneration() {
        AlipayOAuth2Provider provider = new AlipayOAuth2Provider("alipay", alipayConfig, redirectUri,qrCodeUrl);
        String qrCodeUrl = provider.buildQrCode();
        
        System.out.println("Generated Alipay QR Code URL: " + qrCodeUrl);
        
        // 验证URL是否使用了二维码特定的URI
        assertTrue(qrCodeUrl.contains("publicAppAuthorize.htm"), "URL should use QR code specific URI");
        assertTrue(qrCodeUrl.contains("app_id=2021000000000000"), "URL should contain app_id");
        assertTrue(qrCodeUrl.contains("redirect_uri="), "URL should contain redirect_uri");
        assertTrue(qrCodeUrl.contains("scope=auth_user"), "URL should use QR code specific scope");
    }
}