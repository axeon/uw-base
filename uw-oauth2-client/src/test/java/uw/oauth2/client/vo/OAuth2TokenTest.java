package uw.oauth2.client.vo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OAuth2Token类的单元测试
 */
class OAuth2TokenTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testOAuth2TokenBuilder() {
        // 测试构建器模式
        Map<String, Object> rawParams = new HashMap<>();
        rawParams.put("custom_field", "custom_value");
        rawParams.put("expires_at", 1234567890L);

        OAuth2Token token = OAuth2Token.builder()
                .accessToken("test_access_token")
                .refreshToken("test_refresh_token")
                .tokenType("Bearer")
                .expiresIn(3600)
                .scope("openid profile email")
                .idToken("test_id_token")
                .openId("test_open_id")
                .username("test_user")
                .email("test@example.com")
                .phone("12345678901")
                .avatar("https://example.com/avatar.jpg")
                .error("invalid_grant")
                .errorDescription("Invalid grant provided")
                .errorUri("https://example.com/error")
                .createTime(System.currentTimeMillis())
                .refreshTokenExpiresIn(7200)
                .issuedAt(System.currentTimeMillis())
                .rawParams(rawParams)
                .build();

        // 验证基本字段
        assertEquals("test_access_token", token.getAccessToken());
        assertEquals("test_refresh_token", token.getRefreshToken());
        assertEquals("Bearer", token.getTokenType());
        assertEquals(3600, token.getExpiresIn());
        assertEquals("openid profile email", token.getScope());
        assertEquals("test_id_token", token.getIdToken());
        assertEquals("test_open_id", token.getOpenId());
        assertEquals("test_user", token.getUsername());
        assertEquals("test@example.com", token.getEmail());
        assertEquals("12345678901", token.getPhone());
        assertEquals("https://example.com/avatar.jpg", token.getAvatar());

        // 验证新增字段
        assertEquals("invalid_grant", token.getError());
        assertEquals("Invalid grant provided", token.getErrorDescription());
        assertEquals("https://example.com/error", token.getErrorUri());
        assertTrue(token.getCreateTime() > 0);
        assertEquals(7200, token.getRefreshTokenExpiresIn());
        assertTrue(token.getIssuedAt() > 0);

        // 验证原始参数
        assertNotNull(token.getRawParams());
        assertEquals("custom_value", token.getRawParams().get("custom_field"));
        assertEquals(1234567890L, token.getRawParams().get("expires_at"));
    }

    @Test
    void testOAuth2TokenCopyConstructor() {
        // 创建原始token
        Map<String, Object> rawParams = new HashMap<>();
        rawParams.put("original_field", "original_value");

        OAuth2Token originalToken = OAuth2Token.builder()
                .accessToken("original_access_token")
                .refreshToken("original_refresh_token")
                .tokenType("Bearer")
                .expiresIn(3600)
                .scope("openid profile email")
                .idToken("original_id_token")
                .openId("original_open_id")
                .username("original_user")
                .email("original@example.com")
                .phone("12345678901")
                .avatar("https://example.com/original_avatar.jpg")
                .error("invalid_request")
                .errorDescription("Request is invalid")
                .errorUri("https://example.com/original_error")
                .createTime(1000000L)
                .refreshTokenExpiresIn(7200)
                .issuedAt(2000000L)
                .rawParams(rawParams)
                .build();

        // 使用copy构造器
        OAuth2Token copiedToken = OAuth2Token.builder(originalToken).build();

        // 验证所有字段都被正确复制
        assertEquals(originalToken.getAccessToken(), copiedToken.getAccessToken());
        assertEquals(originalToken.getRefreshToken(), copiedToken.getRefreshToken());
        assertEquals(originalToken.getTokenType(), copiedToken.getTokenType());
        assertEquals(originalToken.getExpiresIn(), copiedToken.getExpiresIn());
        assertEquals(originalToken.getScope(), copiedToken.getScope());
        assertEquals(originalToken.getIdToken(), copiedToken.getIdToken());
        assertEquals(originalToken.getOpenId(), copiedToken.getOpenId());
        assertEquals(originalToken.getUsername(), copiedToken.getUsername());
        assertEquals(originalToken.getEmail(), copiedToken.getEmail());
        assertEquals(originalToken.getPhone(), copiedToken.getPhone());
        assertEquals(originalToken.getAvatar(), copiedToken.getAvatar());
        assertEquals(originalToken.getError(), copiedToken.getError());
        assertEquals(originalToken.getErrorDescription(), copiedToken.getErrorDescription());
        assertEquals(originalToken.getErrorUri(), copiedToken.getErrorUri());
        assertEquals(originalToken.getCreateTime(), copiedToken.getCreateTime());
        assertEquals(originalToken.getRefreshTokenExpiresIn(), copiedToken.getRefreshTokenExpiresIn());
        assertEquals(originalToken.getIssuedAt(), copiedToken.getIssuedAt());
        assertEquals(originalToken.getRawParams(), copiedToken.getRawParams());
    }

    @Test
    void testOAuth2TokenJsonSerialization() throws JsonProcessingException {
        long currentTime = System.currentTimeMillis();
        OAuth2Token token = OAuth2Token.builder()
                .accessToken("access_token_123")
                .refreshToken("refresh_token_456")
                .tokenType("Bearer")
                .expiresIn(3600)
                .scope("openid profile email")
                .idToken("id_token_789")
                .openId("user_123")
                .username("test_user")
                .email("test@example.com")
                .error("invalid_client")
                .errorDescription("Client authentication failed")
                .createTime(currentTime)
                .refreshTokenExpiresIn(7200)
                .issuedAt(currentTime + 1000)
                .build();

        // 序列化为JSON
        String json = objectMapper.writeValueAsString(token);
        assertNotNull(json);
        assertTrue(json.contains("access_token"));
        assertTrue(json.contains("refresh_token"));
        assertTrue(json.contains("error"));
        assertTrue(json.contains("error_description"));
        assertTrue(json.contains("create_time"));
        assertTrue(json.contains("refresh_token_expires_in"));
        assertTrue(json.contains("issued_at"));

        // 反序列化回对象（注意：标准Jackson反序列化可能不包含所有字段，这里主要验证序列化功能）
        OAuth2Token deserialized = objectMapper.readValue(json, OAuth2Token.class);
        assertEquals(token.getAccessToken(), deserialized.getAccessToken());
        assertEquals(token.getError(), deserialized.getError());
        assertEquals(token.getCreateTime(), deserialized.getCreateTime());
        assertEquals(token.getRefreshTokenExpiresIn(), deserialized.getRefreshTokenExpiresIn());
        assertEquals(token.getIssuedAt(), deserialized.getIssuedAt());
    }

    @Test
    void testOAuth2TokenErrorHandling() {
        // 测试错误情况的处理
        OAuth2Token errorToken = OAuth2Token.builder()
                .error("access_denied")
                .errorDescription("The user denied access")
                .errorUri("https://example.com/oauth/error")
                .build();

        assertNull(errorToken.getAccessToken()); // 错误情况下可能没有访问令牌
        assertEquals("access_denied", errorToken.getError());
        assertEquals("The user denied access", errorToken.getErrorDescription());
        assertEquals("https://example.com/oauth/error", errorToken.getErrorUri());
    }

    @Test
    void testOAuth2TokenToUserInfo() {
        OAuth2Token token = OAuth2Token.builder()
                .openId("user_123")
                .username("test_user")
                .email("test@example.com")
                .phone("12345678901")
                .avatar("https://example.com/avatar.jpg")
                .build();

        OAuth2UserInfo userInfo = token.toUserInfo();

        assertEquals("user_123", userInfo.getOpenId());
        assertEquals("test_user", userInfo.getUsername());
        assertEquals("test@example.com", userInfo.getEmail());
        assertEquals("12345678901", userInfo.getPhone());
        assertEquals("https://example.com/avatar.jpg", userInfo.getAvatar());
    }
}