package uw.webot.captcha.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.util.JsonUtils;
import uw.webot.captcha.CaptchaService;
import uw.webot.captcha.CaptchaConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 2Captcha服务实现。
 * 实现2Captcha第三方验证码服务集成。
 *
 * @author axeon
 * @since 1.0.0
 */
public class TwoCaptchaServiceImpl implements CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(TwoCaptchaServiceImpl.class);

    /**
     * 2Captcha API URL。
     */
    private static final String API_BASE_URL = "https://2captcha.com";

    /**
     * JSON对象映射器。
     */
    private static final ObjectMapper OBJECT_MAPPER = JsonUtils.getJsonMapper();

    /**
     * Captcha配置属性。
     */
    private final CaptchaConfig captchaConfig;

    /**
     * HTTP客户端。
     */
    private final HttpClient httpClient;

    public TwoCaptchaServiceImpl(CaptchaConfig captchaConfig) {
        this.captchaConfig = captchaConfig;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * 识别图片验证码。
     *
     * @param imageData 图片字节数据
     * @return 识别结果
     */
    @Override
    public CaptchaResult recognizeImageCaptcha(byte[] imageData) {
        return recognizeImageCaptcha(imageData, new CaptchaOptions());
    }

    /**
     * 识别图片验证码（带额外参数）。
     *
     * @param imageData 图片字节数据
     * @param options   识别选项
     * @return 识别结果
     */
    @Override
    public CaptchaResult recognizeImageCaptcha(byte[] imageData, CaptchaOptions options) {
        long startTime = System.currentTimeMillis();
        String apiKey = captchaConfig.getApiKey();

        if (apiKey == null || apiKey.isEmpty()) {
            return CaptchaResult.failure("2Captcha API key not configured");
        }

        try {
            // Base64编码图片
            String base64Image = Base64.getEncoder().encodeToString(imageData);

            // 提交验证码
            Map<String, String> params = new HashMap<>();
            params.put("key", apiKey);
            params.put("method", "base64");
            params.put("body", base64Image);
            params.put("json", "1");

            if (options.getMinLength() != null) {
                params.put("min_len", String.valueOf(options.getMinLength()));
            }
            if (options.getMaxLength() != null) {
                params.put("max_len", String.valueOf(options.getMaxLength()));
            }
            if (options.getLanguage() != null) {
                params.put("language", options.getLanguage());
            }

            String formData = buildFormData(params);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + "/in.php"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode result = OBJECT_MAPPER.readTree(response.body());

            if (result.has("status") && result.get("status").asInt() == 1) {
                String captchaId = result.get("request").asText();
                return pollForResult(captchaId, startTime);
            } else {
                String error = result.has("request") ? result.get("request").asText() : "Unknown error";
                return CaptchaResult.failure("2Captcha submit failed: " + error);
            }

        } catch (Exception e) {
            log.error("2Captcha image recognition failed", e);
            return CaptchaResult.failure("Recognition failed: " + e.getMessage());
        }
    }

    /**
     * 识别Base64编码的图片验证码。
     *
     * @param base64Image Base64编码的图片
     * @return 识别结果
     */
    @Override
    public CaptchaResult recognizeBase64Captcha(String base64Image) {
        return recognizeBase64Captcha(base64Image, new CaptchaOptions());
    }

    /**
     * 识别Base64编码的图片验证码（带额外参数）。
     *
     * @param base64Image Base64编码的图片
     * @param options     识别选项
     * @return 识别结果
     */
    @Override
    public CaptchaResult recognizeBase64Captcha(String base64Image, CaptchaOptions options) {
        byte[] imageData = Base64.getDecoder().decode(base64Image);
        return recognizeImageCaptcha(imageData, options);
    }

    /**
     * 解决ReCaptcha V2。
     *
     * @param siteKey 站点密钥
     * @param pageUrl 页面URL
     * @return 识别结果
     */
    @Override
    public CaptchaResult solveReCaptchaV2(String siteKey, String pageUrl) {
        long startTime = System.currentTimeMillis();
        String apiKey = captchaConfig.getApiKey();

        if (apiKey == null || apiKey.isEmpty()) {
            return CaptchaResult.failure("2Captcha API key not configured");
        }

        try {
            Map<String, String> params = new HashMap<>();
            params.put("key", apiKey);
            params.put("method", "userrecaptcha");
            params.put("googlekey", siteKey);
            params.put("pageurl", pageUrl);
            params.put("json", "1");

            String formData = buildFormData(params);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + "/in.php"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode result = OBJECT_MAPPER.readTree(response.body());

            if (result.has("status") && result.get("status").asInt() == 1) {
                String captchaId = result.get("request").asText();
                return pollForResult(captchaId, startTime);
            } else {
                String error = result.has("request") ? result.get("request").asText() : "Unknown error";
                return CaptchaResult.failure("2Captcha submit failed: " + error);
            }

        } catch (Exception e) {
            log.error("2Captcha ReCaptcha V2 solving failed", e);
            return CaptchaResult.failure("Solving failed: " + e.getMessage());
        }
    }

    /**
     * 解决ReCaptcha V3。
     *
     * @param siteKey  站点密钥
     * @param pageUrl  页面URL
     * @param action   动作
     * @param minScore 最低分数
     * @return 识别结果
     */
    @Override
    public CaptchaResult solveReCaptchaV3(String siteKey, String pageUrl, String action, float minScore) {
        long startTime = System.currentTimeMillis();
        String apiKey = captchaConfig.getApiKey();

        if (apiKey == null || apiKey.isEmpty()) {
            return CaptchaResult.failure("2Captcha API key not configured");
        }

        try {
            Map<String, String> params = new HashMap<>();
            params.put("key", apiKey);
            params.put("method", "userrecaptcha");
            params.put("version", "v3");
            params.put("googlekey", siteKey);
            params.put("pageurl", pageUrl);
            params.put("action", action);
            params.put("min_score", String.valueOf(minScore));
            params.put("json", "1");

            String formData = buildFormData(params);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + "/in.php"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode result = OBJECT_MAPPER.readTree(response.body());

            if (result.has("status") && result.get("status").asInt() == 1) {
                String captchaId = result.get("request").asText();
                return pollForResult(captchaId, startTime);
            } else {
                String error = result.has("request") ? result.get("request").asText() : "Unknown error";
                return CaptchaResult.failure("2Captcha submit failed: " + error);
            }

        } catch (Exception e) {
            log.error("2Captcha ReCaptcha V3 solving failed", e);
            return CaptchaResult.failure("Solving failed: " + e.getMessage());
        }
    }

    /**
     * 解决hCaptcha。
     *
     * @param siteKey 站点密钥
     * @param pageUrl 页面URL
     * @return 识别结果
     */
    @Override
    public CaptchaResult solveHCaptcha(String siteKey, String pageUrl) {
        long startTime = System.currentTimeMillis();
        String apiKey = captchaConfig.getApiKey();

        if (apiKey == null || apiKey.isEmpty()) {
            return CaptchaResult.failure("2Captcha API key not configured");
        }

        try {
            Map<String, String> params = new HashMap<>();
            params.put("key", apiKey);
            params.put("method", "hcaptcha");
            params.put("sitekey", siteKey);
            params.put("pageurl", pageUrl);
            params.put("json", "1");

            String formData = buildFormData(params);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + "/in.php"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode result = OBJECT_MAPPER.readTree(response.body());

            if (result.has("status") && result.get("status").asInt() == 1) {
                String captchaId = result.get("request").asText();
                return pollForResult(captchaId, startTime);
            } else {
                String error = result.has("request") ? result.get("request").asText() : "Unknown error";
                return CaptchaResult.failure("2Captcha submit failed: " + error);
            }

        } catch (Exception e) {
            log.error("2Captcha hCaptcha solving failed", e);
            return CaptchaResult.failure("Solving failed: " + e.getMessage());
        }
    }

    /**
     * 解决GeeTest。
     *
     * @param gt         GT参数
     * @param challenge  Challenge参数
     * @param apiServer  API服务器
     * @param pageUrl    页面URL
     * @return 识别结果
     */
    @Override
    public CaptchaResult solveGeeTest(String gt, String challenge, String apiServer, String pageUrl) {
        long startTime = System.currentTimeMillis();
        String apiKey = captchaConfig.getApiKey();

        if (apiKey == null || apiKey.isEmpty()) {
            return CaptchaResult.failure("2Captcha API key not configured");
        }

        try {
            Map<String, String> params = new HashMap<>();
            params.put("key", apiKey);
            params.put("method", "geetest");
            params.put("gt", gt);
            params.put("challenge", challenge);
            params.put("api_server", apiServer);
            params.put("pageurl", pageUrl);
            params.put("json", "1");

            String formData = buildFormData(params);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + "/in.php"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode result = OBJECT_MAPPER.readTree(response.body());

            if (result.has("status") && result.get("status").asInt() == 1) {
                String captchaId = result.get("request").asText();
                return pollForResult(captchaId, startTime);
            } else {
                String error = result.has("request") ? result.get("request").asText() : "Unknown error";
                return CaptchaResult.failure("2Captcha submit failed: " + error);
            }

        } catch (Exception e) {
            log.error("2Captcha GeeTest solving failed", e);
            return CaptchaResult.failure("Solving failed: " + e.getMessage());
        }
    }

    /**
     * 轮询获取结果。
     */
    private CaptchaResult pollForResult(String captchaId, long startTime) {
        Duration timeout = captchaConfig.getMaxTimeout();
        Duration pollingInterval = Duration.ofSeconds(5);
        String apiKey = captchaConfig.getApiKey();

        long endTime = startTime + timeout.toMillis();

        while (System.currentTimeMillis() < endTime) {
            try {
                Thread.sleep(pollingInterval.toMillis());

                String url = String.format("%s/res.php?key=%s&action=get&id=%s&json=1",
                        API_BASE_URL, apiKey, captchaId);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode result = OBJECT_MAPPER.readTree(response.body());

                if (result.has("status")) {
                    int status = result.get("status").asInt();
                    if (status == 1) {
                        long solveTime = System.currentTimeMillis() - startTime;
                        String code = result.get("request").asText();
                        return CaptchaResult.success(code, solveTime, 0);
                    }
                }

                if (result.has("request")) {
                    String requestStatus = result.get("request").asText();
                    if (!"CAPCHA_NOT_READY".equals(requestStatus)) {
                        return CaptchaResult.failure("2Captcha error: " + requestStatus);
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return CaptchaResult.failure("Polling interrupted");
            } catch (Exception e) {
                log.error("Error polling for 2Captcha result", e);
            }
        }

        return CaptchaResult.failure("2Captcha solving timeout");
    }

    /**
     * 检查服务是否可用。
     *
     * @return true 如果API密钥已配置
     */
    @Override
    public boolean isAvailable() {
        String apiKey = captchaConfig.getApiKey();
        return apiKey != null && !apiKey.isEmpty();
    }

    /**
     * 获取账户余额。
     *
     * @return 账户余额
     */
    @Override
    public double getBalance() {
        String apiKey = captchaConfig.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            return 0;
        }

        try {
            String url = String.format("%s/res.php?key=%s&action=getbalance&json=1", API_BASE_URL, apiKey);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode result = OBJECT_MAPPER.readTree(response.body());

            if (result.has("request")) {
                return result.get("request").asDouble();
            }
        } catch (Exception e) {
            log.error("Failed to get 2Captcha balance", e);
        }

        return 0;
    }

    /**
     * 获取服务类型。
     *
     * @return "2captcha"
     */
    @Override
    public String getServiceType() {
        return "2captcha";
    }

    /**
     * 构建表单数据。
     */
    private String buildFormData(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(java.net.URLEncoder.encode(entry.getValue(), java.nio.charset.StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
