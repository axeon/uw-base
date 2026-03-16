package uw.oauth2.client.provider;

import uw.common.dto.ResponseData;
import uw.common.util.JsonUtils;
import uw.httpclient.http.HttpData;
import uw.oauth2.client.conf.OAuth2ClientProperties;
import uw.oauth2.client.constant.OAuth2ClientResponseCode;
import uw.oauth2.client.vo.OAuth2Token;
import uw.oauth2.client.vo.OAuth2UserInfo;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

/**
 * 支付宝OAuth2 Provider，处理支付宝特有的OAuth2流程
 */
public class AlipayOAuth2Provider extends AbstractOAuth2Provider {

    /**
     * 构造函数
     *
     * @param providerCode   Provider名称
     * @param providerConfig Provider配置
     */
    public AlipayOAuth2Provider(String providerCode, OAuth2ClientProperties.ProviderConfig providerConfig, String redirectUri, String qrcodeUri) {
        super(providerCode, providerConfig, redirectUri, qrcodeUri);
    }

    /**
     * 获取token
     *
     * @param authCode 授权码
     * @param authStateId  state
     * @param extParam
     * @return token
     */
    @Override
    public ResponseData<OAuth2Token> getToken(String authCode, String authStateId, Map<String, String> extParam) {
        try {
            // 支付宝获取token需要使用签名
            Map<String, String> params = new TreeMap<>();
            params.put("grant_type", "authorization_code");
            params.put("code", authCode);
            params.put("app_id", providerConfig.getClientId());
            params.put("method", "alipay.system.oauth.token");
            params.put("charset", "UTF-8");
            params.put("sign_type", "RSA2");
            params.put("version", "1.0");
            params.put("timestamp", generateTimestamp());
            params.put("format", "JSON");

            // 添加签名
            params.put("sign", generateSign(params));

            // 修改httpClient调用方式，使用正确的方法
            HttpData httpData = JSON_INTERFACE_HELPER.postBodyForData(providerConfig.getTokenUri(), params);
            if (httpData.getStatusCode() != 200) {
                return ResponseData.errorCode(OAuth2ClientResponseCode.INVALID_HTTP_CODE, httpData.getStatusCode(), httpData.getResponseData());
            }
            return parseTokenResponse(httpData.getResponseData());
        } catch (Exception e) {
            logger.error("Failed to exchange token for provider {}", providerCode, e);
            return ResponseData.errorCode(OAuth2ClientResponseCode.HTTP_REQUEST_FAILED, e.toString());
        }
    }

    /**
     * 获取用户信息
     *
     * @param oAuth2Token accessToken
     * @return 用户信息
     */
    @Override
    public ResponseData<OAuth2UserInfo> getUserInfo(OAuth2Token oAuth2Token) {
        try {
            // 支付宝获取用户信息需要使用签名
            Map<String, String> params = new TreeMap<>();
            params.put("app_id", providerConfig.getClientId());
            params.put("method", "alipay.user.info.share");
            params.put("charset", "UTF-8");
            params.put("sign_type", "RSA2");
            params.put("version", "1.0");
            params.put("timestamp", generateTimestamp());
            params.put("format", "JSON");
            params.put("auth_token", oAuth2Token.getAccessToken());

            // 添加签名
            params.put("sign", generateSign(params));

            // 修改httpClient调用方式，使用正确的方法
            HttpData httpData = JSON_INTERFACE_HELPER.postBodyForData(providerConfig.getUserInfoUri(), params);
            if (httpData.getStatusCode() != 200) {
                return ResponseData.errorCode(OAuth2ClientResponseCode.INVALID_HTTP_CODE, httpData.getStatusCode(), httpData.getResponseData());
            }

            String responseBody = httpData.getResponseData();

            // 解析用户信息
            return parseUserInfoResponse(responseBody);
        } catch (Exception e) {
            logger.error("Failed to get user info for provider {}", providerCode, e);
            return ResponseData.errorCode(OAuth2ClientResponseCode.HTTP_REQUEST_FAILED, e.toString());
        }
    }

    /**
     * 生成签名
     *
     * @param params 参数
     */
    @Override
    protected void addAuthParam(Map<String, String> params) {
        // 支付宝授权URL不需要特殊参数，使用标准OAuth2参数
        // 但需要确保client_id参数名称正确
        // 支付宝使用app_id而不是client_id，需要转换
        if (params.containsKey("client_id")) {
            String clientId = params.remove("client_id");
            params.put("app_id", clientId);
        }
    }

    /**
     * 解析token响应
     *
     * @param responseBody 响应体
     * @return token
     */
    @Override
    protected ResponseData<OAuth2Token> parseTokenResponse(String responseBody) {
        try {
            Map<String, Object> tokenMap = JsonUtils.parse(responseBody, Map.class);

            // 支付宝返回的错误处理
            if (tokenMap.containsKey("error")) {
                return ResponseData.errorCode(OAuth2ClientResponseCode.HTTP_REQUEST_FAILED,
                        "Alipay token error: " + tokenMap.get("error") + " - " + tokenMap.get("error_description"));
            }

            // 支付宝返回的token信息在"alipay_system_oauth_token_response"字段中
            Map<String, Object> response = (Map<String, Object>) tokenMap.get("alipay_system_oauth_token_response");

            // 检查返回结果
            if (response != null && "10000".equals(response.get("code"))) {
                OAuth2Token token = new OAuth2Token();
                token.setAccessToken((String) response.get("access_token"));
                token.setRefreshToken((String) response.get("refresh_token"));
                token.setTokenType("Bearer");
                token.setExpiresIn(((Number) response.getOrDefault("expires_in", 0)).longValue());
                token.setScope((String) response.get("scope"));

                // 支付宝返回的用户ID设置到token中
                token.setOpenId((String) response.get("user_id"));

                // 保存原始响应数据
                token.setRawParams(response);

                return ResponseData.success(token);
            } else {
                String errorCode = response != null ? (String) response.get("code") : "unknown";
                String errorMsg = response != null ? (String) response.get("msg") : "unknown error";
                return ResponseData.errorCode(OAuth2ClientResponseCode.HTTP_REQUEST_FAILED,
                        "Alipay token error: " + errorCode + " - " + errorMsg);
            }
        } catch (Exception e) {
            logger.error("Failed to parse token response for provider {}", providerCode, e);
            return ResponseData.errorCode(OAuth2ClientResponseCode.HTTP_REQUEST_FAILED, e.toString());
        }
    }

    /**
     * 解析用户信息响应
     *
     * @param responseBody 响应体
     * @return 用户信息
     */
    @Override
    protected ResponseData<OAuth2UserInfo> parseUserInfoResponse(String responseBody) {
        try {
            Map<String, Object> userMap = JsonUtils.parse(responseBody, Map.class);

            // 支付宝返回的错误处理
            if (userMap.containsKey("error")) {
                return ResponseData.errorCode(OAuth2ClientResponseCode.HTTP_REQUEST_FAILED,
                        "Alipay user info error: " + userMap.get("error") + " - " + userMap.get("error_description"));
            }

            // 支付宝返回的用户信息在"alipay_user_info_share_response"字段中
            Map<String, Object> response = (Map<String, Object>) userMap.get("alipay_user_info_share_response");

            // 检查返回结果
            if (response != null && "10000".equals(response.get("code"))) {
                OAuth2UserInfo userInfo = new OAuth2UserInfo();
                userInfo.setProviderCode(providerCode);
                userInfo.setOpenId((String) response.get("user_id"));
                userInfo.setUsername((String) response.get("nick_name"));
                userInfo.setEmail((String) response.get("email"));
                userInfo.setAvatar((String) response.get("avatar"));

                // 支付宝性别：F-女，M-男，U-未知
                String gender = "unknown";
                if (response.get("gender") != null && "M".equals(response.get("gender"))) {
                    gender = "male";
                } else if (response.get("gender") != null && "F".equals(response.get("gender"))) {
                    gender = "female";
                }
                userInfo.setGender(gender);

                // 设置其他可能的字段
                userInfo.setPhone((String) response.get("mobile"));
                userInfo.setAddress((String) response.get("address"));
                userInfo.setArea((String) response.get("province"));

                // 保存原始响应数据
                userInfo.setRawParams(response);

                return ResponseData.success(userInfo);
            } else {
                String errorCode = response != null ? (String) response.get("code") : "unknown";
                String errorMsg = response != null ? (String) response.get("msg") : "unknown error";
                return ResponseData.errorCode(OAuth2ClientResponseCode.HTTP_REQUEST_FAILED,
                        "Alipay user info error: " + errorCode + " - " + errorMsg);
            }
        } catch (Exception e) {
            logger.error("Failed to parse user info response for provider {}", providerCode, e);
            return ResponseData.errorCode(OAuth2ClientResponseCode.HTTP_REQUEST_FAILED, e.toString());
        }
    }

    /**
     * 生成支付宝签名
     *
     * @param params 请求参数
     * @return 签名结果
     */
    private String generateSign(Map<String, String> params) {
        try {
            // 构建签名内容
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty() && !"sign".equals(entry.getKey())) {
                    sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
                }
            }
            String signContent = sb.substring(0, sb.length() - 1);

            logger.info("Alipay sign content: {}", signContent);

            // 从extParam中获取私钥
            String privateKey = providerConfig.getExtParam().get("privateKey");
            if (privateKey == null || privateKey.isEmpty()) {
                throw new RuntimeException("Private key is not configured for Alipay provider. Please set it in extParam with key 'privateKey'");
            }

            // 去除私钥中的换行符和前后缀
            privateKey = privateKey.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");

            // 解码私钥
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKey);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey rsaPrivateKey = keyFactory.generatePrivate(keySpec);

            // 使用SHA256withRSA算法进行签名
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(rsaPrivateKey);
            signature.update(signContent.getBytes("UTF-8"));
            byte[] signBytes = signature.sign();

            // 返回Base64编码的签名结果
            return Base64.getEncoder().encodeToString(signBytes);
        } catch (Exception e) {
            logger.error("Failed to generate Alipay sign", e);
            throw new RuntimeException("Failed to generate sign", e);
        }
    }

    /**
     * 生成当前时间戳
     *
     * @return 时间戳（格式：yyyy-MM-dd HH:mm:ss）
     */
    private String generateTimestamp() {
        return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}