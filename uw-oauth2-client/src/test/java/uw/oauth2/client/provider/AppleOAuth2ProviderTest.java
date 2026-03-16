package uw.oauth2.client.provider;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import uw.oauth2.client.conf.OAuth2ClientProperties;

import java.util.Date;

/**
 * StandardOAuth2Provider单元测试
 */
class AppleOAuth2ProviderTest {

    private static OAuth2ClientProperties.ProviderConfig config;
    private static AppleOAuth2Provider provider;

    static {
        // 初始化配置
        config = new OAuth2ClientProperties.ProviderConfig();
        config.setClientId("");
        config.setClientSecret(null);
        config.setAuthUri("https://appleid.apple.com/auth/authorize");
        config.setTokenUri("https://appleid.apple.com/auth/token");
        config.setAuthScope("openid email name");
        config.getExtParam().put("teamId", "");
        config.getExtParam().put("keyId", "");
        config.getExtParam().put("p8Key", """
                """);
        provider = new AppleOAuth2Provider("apple", config, "", "");
    }

    public static void main(String[] args) throws Exception {
        // 生成授权URL
        System.out.println(provider.buildAuthUrl(null));

//        String code = "c307118b95b2e42b091dd2f29169957d1.0.prtus.mXn3Q_jKJIquUOryIVofhg";
//        System.out.println(JsonUtils.toString(provider.getToken(code, null)));

//        String user="{\"name\":{\"firstName\":\"king\",\"lastName\":\"zhang\"},\"email\":\"23231269@qq.com\"}";
//
        String idToken = "eyJraWQiOiJhVmVIRmFXeEFaIiwiYWxnIjoiUlMyNTYifQ.eyJpc3MiOiJodHRwczovL2FwcGxlaWQuYXBwbGUuY29tIiwiYXVkIjoiY29tLndlc3QtcXIuZ3Vlc3QtdWktdGVzdC5zaWduaW53aXRoYXBwbGUiLCJleHAiOjE3Njc3MTE5ODAsImlhdCI6MTc2NzYyNTU4MCwic3ViIjoiMDAxMzQyLjc3MmI4NjFkOWFmNzQ0MmE5MDU0NmYyMzk3NTA3ZjdlLjE0MDYiLCJhdF9oYXNoIjoiV0JUWmZaemJrOWlLVjZzU0NnWlBBZyIsImVtYWlsIjoiMjMyMzEyNjlAcXEuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImF1dGhfdGltZSI6MTc2NzYyNTM4OCwibm9uY2Vfc3VwcG9ydGVkIjp0cnVlfQ.iF1RH8QTjlHrA5XpyxrmdKztig3abj-B4V5OFgY56KR1JdNqe8MqOLDEQEXbPhKfe7NzBc7t3C9YZfSP8T7C6klxxysKXDYrB94w4NpT970hj1E_L9YmEKyVMS6Z-9mTMvYRwUMSvMFWK-TAOXF0KR6pvVx5sP8YubfaECfwU3M63iY-Q8hkJWid69Due4falKK_esIpYJZIvFvMQ5xcrKT8AAHNMaHpgxhAszeG_86vco_W02dKwbN5O23CE-Wu8d6s1jCCYuKcdYIVHnceI9wqBBOn44rEARFeOvHFgWMSz2ra_61s49dQLTRsG7_63bi9X8FrtbtCMPz6VFvXYA";
        DecodedJWT jwt = JWT.decode(idToken);
//        // 2. 获取标准声明
        String userId = jwt.getSubject();           // sub
        String issuer = jwt.getIssuer();            // iss
        String audience = jwt.getAudience().get(0); // aud
        Date expiresAt = jwt.getExpiresAt();        // exp
        Date issuedAt = jwt.getIssuedAt();          // iat
//
        System.out.println("用户ID: " + userId);
        System.out.println("颁发者: " + issuer);
        System.out.println("过期时间: " + expiresAt);
//
//        // 3. 获取自定义声明
        jwt.getClaims().forEach((key, claim) -> {
            System.out.println(key + ": " + claim.asString());
        });

//        OAuth2Token token = OAuth2Token.builder()
//                .accessToken("ya29.a0Aa7pCA8PHqTnMI4nwnx31i7LnJpblUpWkZhSQd-iqsRbkcSxTt-3Tx6X9uIabacFVIZGRNq2UxOcNo7iooLf6OXvfPdnuztjNbwJS8SLpIA5j9alp14zUb6scgwViXSsXrbymf7iDkiaZqCLjueAgUElzz5SAe46zaPe0PLGYSMPyFZKrJennUkxFwLT66lyE0bdiukaCgYKAQASARMSFQHGX2MizvzbEWJeQUW8Tf8N11Uu6A0206")
//                .tokenType("Bearer")
//                .expiresIn(3600)
//                .scope("openid email profile")
//                .build();
//        OAuth2UserInfo userInfo = provider.getUserInfo(token);
//        System.out.println(JsonUtils.toString(userInfo));
    }


}