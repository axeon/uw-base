package uw.oauth2.client.provider;

import org.springframework.web.util.UriUtils;
import uw.common.dto.ResponseData;
import uw.common.util.JsonUtils;
import uw.oauth2.client.conf.OAuth2ClientProperties;
import uw.oauth2.client.vo.OAuth2Token;
import uw.oauth2.client.vo.OAuth2UserInfo;

import java.nio.charset.StandardCharsets;

/**
 * WechatOAuth2ProviderTest
 */
class WechatOAuth2ProviderTest {

    private static OAuth2ClientProperties.ProviderConfig config;
    private static WechatOAuth2Provider provider;

    static {
        // 初始化配置
        config = new OAuth2ClientProperties.ProviderConfig();
        config.setClientId("");
        config.setClientSecret("");
        config.setAuthUri("https://open.weixin.qq.com/connect/qrconnect");
        config.setTokenUri("https://api.weixin.qq.com/sns/oauth2/access_token");
        config.setUserInfoUri("https://api.weixin.qq.com/sns/userinfo");
        config.setAuthScope("snsapi_login");
        provider = new WechatOAuth2Provider("wechat", config, "https://xili.pub/oauth2/redirect","https://xili.pub/oauth2/qrcode/");
    }

    public static void main(String[] args) {
        // 生成授权URL
        System.out.println(provider.buildAuthUrl(null));
        // 返回信息: https://guest-ui-test.west-qr.com/oauth2/redirect?state=google_134071110540996608&code=4%2F0ATX87lPhO4WDwvc6eerd80Alcns4GSy0FMKslEVksm_5zf6YbTNab4mx-tZDbWCN9c1y_A&scope=email+profile+https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fuserinfo.profile+openid+https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fuserinfo.email&authuser=0&prompt=none

        String code = "071QnC0w3ugCi63dvW2w3a9LLM2QnC00";
        code = UriUtils.decode(code, StandardCharsets.UTF_8);
        OAuth2Token oAuth2Token = provider.getToken(code, null, null).getData();
        System.out.println(JsonUtils.toString(oAuth2Token));

//        String idToken="eyJhbGciOiJSUzI1NiIsImtpZCI6IjQ5NmQwMDhlOGM3YmUxY2FlNDIwOWUwZDVjMjFiMDUwYTYxZTk2MGYiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJhenAiOiIxMDQ3MDM4ODYwMjQ4LW5xcjdnZTIzc2VvYjNrZ2FmbWVja2FpMjNjNWpybTJuLmFwcHMuZ29vZ2xldXNlcmNvbnRlbnQuY29tIiwiYXVkIjoiMTA0NzAzODg2MDI0OC1ucXI3Z2UyM3Nlb2Iza2dhZm1lY2thaTIzYzVqcm0ybi5hcHBzLmdvb2dsZXVzZXJjb250ZW50LmNvbSIsInN1YiI6IjEwMzE4MjQ5MjE1NjEzOTI4Mzg5NyIsImVtYWlsIjoic2lub2xpb25ldEBnbWFpbC5jb20iLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiYXRfaGFzaCI6Ik9KVXhacE5xWElsU204dzhoX0NXbnciLCJuYW1lIjoiemhhbmcgamluIiwicGljdHVyZSI6Imh0dHBzOi8vbGgzLmdvb2dsZXVzZXJjb250ZW50LmNvbS9hL0FDZzhvY0pBTnFMVlJCUW11ODRyMHFTbXZYOXNfb1hwV3d6bVdWUEtReS1qQTB6RnBNRjJwUT1zOTYtYyIsImdpdmVuX25hbWUiOiJ6aGFuZyIsImZhbWlseV9uYW1lIjoiamluIiwiaWF0IjoxNzY3MDg5NDg0LCJleHAiOjE3NjcwOTMwODR9.hMvTtBmjiEgvYZKNSw9eBr8h9k6ZHsgCJED0_7UkJr7FbmV1bLSVy2fxtkaiVLI9PLfubZNVbxgHbjVu8KuR26_QO2B3InY7Z9jsC8cH9G4SK7lL0QIJ76bvTCBz8cMzlkEBYrxc0dpDLDmNqiNrArcPaxpZ-JTNndCdzwQcWJ6xIS_HPrzfqwdGfDf5cebHD0zWHNi1Rlyl0j5TUckS2SxA6oxgEKUs_Yvra0kGBZpe3CBr30gIN8wXLRzYxdM9yp-rGh4gAI_Dk8eaD2aqrfZ7jzjmHhUdX_8mKsRN55C-h0qTVBxvfOVaF8BdoPbgF2926d7hDFdUdKYhtxCagA";
//        DecodedJWT jwt = JWT.decode(idToken);
//        // 2. 获取标准声明
//        String userId = jwt.getSubject();           // sub
//        String issuer = jwt.getIssuer();            // iss
//        String audience = jwt.getAudience().get(0); // aud
//        Date expiresAt = jwt.getExpiresAt();        // exp
//        Date issuedAt = jwt.getIssuedAt();          // iat
//
//        System.out.println("用户ID: " + userId);
//        System.out.println("颁发者: " + issuer);
//        System.out.println("过期时间: " + expiresAt);
//
//        // 3. 获取自定义声明
//        String email = jwt.getClaim("email").asString();
//        String name = jwt.getClaim("name").asString();
//        Boolean emailVerified = jwt.getClaim("email_verified").asBoolean();
//        System.out.println("邮箱: " + email);
//        System.out.println("姓名: " + name);

//        jwt.getClaims().forEach((key, claim) -> {
//            System.out.println(key + ": " + claim.asString());
//        });


        ResponseData<OAuth2UserInfo> userInfoResponse = provider.getUserInfo(oAuth2Token);
        System.out.println(JsonUtils.toString(userInfoResponse));
    }


}