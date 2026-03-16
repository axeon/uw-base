package uw.webot.captcha.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

/**
 * Capsolver服务实现。
 * 实现Capsolver第三方验证码服务集成。
 *
 * @author axeon
 * @since 1.0.0
 */
public class CapsolverServiceImpl implements CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(CapsolverServiceImpl.class);
    /**
     * Capsolver API URL.
     */
    private static final String API_BASE_URL = "https://api.capsolver.com";
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

    public CapsolverServiceImpl(CaptchaConfig captchaConfig) {
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
            return CaptchaResult.failure("Capsolver API key not configured");
        }

        try {
            String base64Image = Base64.getEncoder().encodeToString(imageData);

            ObjectNode task = OBJECT_MAPPER.createObjectNode();
            task.put("type", "ImageToTextTask");
            task.put("body", base64Image);

            if (options.getLanguage() != null) {
                task.put("language", options.getLanguage());
            }

            ObjectNode requestBody = OBJECT_MAPPER.createObjectNode();
            requestBody.put("clientKey", apiKey);
            requestBody.set("task", task);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + "/createTask"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode result = OBJECT_MAPPER.readTree(response.body());

            if (result.has("taskId")) {
                String taskId = result.get("taskId").asText();
                return pollForResult(taskId, startTime);
            } else if (result.has("errorId") && result.get("errorId").asInt() != 0) {
                String error = result.has("errorDescription") ?
                        result.get("errorDescription").asText() : "Unknown error";
                return CaptchaResult.failure("Capsolver submit failed: " + error);
            } else {
                return CaptchaResult.failure("Capsolver submit failed: No taskId returned");
            }

        } catch (Exception e) {
            log.error("Capsolver image recognition failed", e);
            return CaptchaResult.failure("Recognition failed: " + e.getMessage());
        }
    }

    @Override
    public CaptchaResult recognizeBase64Captcha(String base64Image) {
        return recognizeBase64Captcha(base64Image, new CaptchaOptions());
    }

    @Override
    public CaptchaResult recognizeBase64Captcha(String base64Image, CaptchaOptions options) {
        byte[] imageData = Base64.getDecoder().decode(base64Image);
        return recognizeImageCaptcha(imageData, options);
    }

    @Override
    public CaptchaResult solveReCaptchaV2(String siteKey, String pageUrl) {
        long startTime = System.currentTimeMillis();
        String apiKey = captchaConfig.getApiKey();

        if (apiKey == null || apiKey.isEmpty()) {
            return CaptchaResult.failure("Capsolver API key not configured");
        }

        try {
            ObjectNode task = OBJECT_MAPPER.createObjectNode();
            task.put("type", "ReCaptchaV2TaskProxyLess");
            task.put("websiteKey", siteKey);
            task.put("websiteURL", pageUrl);

            ObjectNode requestBody = OBJECT_MAPPER.createObjectNode();
            requestBody.put("clientKey", apiKey);
            requestBody.set("task", task);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + "/createTask"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode result = OBJECT_MAPPER.readTree(response.body());

            if (result.has("taskId")) {
                String taskId = result.get("taskId").asText();
                return pollForResult(taskId, startTime);
            } else {
                String error = result.has("errorDescription") ?
                        result.get("errorDescription").asText() : "Unknown error";
                return CaptchaResult.failure("Capsolver submit failed: " + error);
            }

        } catch (Exception e) {
            log.error("Capsolver ReCaptcha V2 solving failed", e);
            return CaptchaResult.failure("Solving failed: " + e.getMessage());
        }
    }

    @Override
    public CaptchaResult solveReCaptchaV3(String siteKey, String pageUrl, String action, float minScore) {
        long startTime = System.currentTimeMillis();
        String apiKey = captchaConfig.getApiKey();

        if (apiKey == null || apiKey.isEmpty()) {
            return CaptchaResult.failure("Capsolver API key not configured");
        }

        try {
            ObjectNode task = OBJECT_MAPPER.createObjectNode();
            task.put("type", "ReCaptchaV3TaskProxyLess");
            task.put("websiteKey", siteKey);
            task.put("websiteURL", pageUrl);
            task.put("pageAction", action);
            task.put("minScore", minScore);

            ObjectNode requestBody = OBJECT_MAPPER.createObjectNode();
            requestBody.put("clientKey", apiKey);
            requestBody.set("task", task);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + "/createTask"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode result = OBJECT_MAPPER.readTree(response.body());

            if (result.has("taskId")) {
                String taskId = result.get("taskId").asText();
                return pollForResult(taskId, startTime);
            } else {
                String error = result.has("errorDescription") ?
                        result.get("errorDescription").asText() : "Unknown error";
                return CaptchaResult.failure("Capsolver submit failed: " + error);
            }

        } catch (Exception e) {
            log.error("Capsolver ReCaptcha V3 solving failed", e);
            return CaptchaResult.failure("Solving failed: " + e.getMessage());
        }
    }

    @Override
    public CaptchaResult solveHCaptcha(String siteKey, String pageUrl) {
        long startTime = System.currentTimeMillis();
        String apiKey = captchaConfig.getApiKey();

        if (apiKey == null || apiKey.isEmpty()) {
            return CaptchaResult.failure("Capsolver API key not configured");
        }

        try {
            ObjectNode task = OBJECT_MAPPER.createObjectNode();
            task.put("type", "HCaptchaTaskProxyLess");
            task.put("websiteKey", siteKey);
            task.put("websiteURL", pageUrl);

            ObjectNode requestBody = OBJECT_MAPPER.createObjectNode();
            requestBody.put("clientKey", apiKey);
            requestBody.set("task", task);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + "/createTask"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode result = OBJECT_MAPPER.readTree(response.body());

            if (result.has("taskId")) {
                String taskId = result.get("taskId").asText();
                return pollForResult(taskId, startTime);
            } else {
                String error = result.has("errorDescription") ?
                        result.get("errorDescription").asText() : "Unknown error";
                return CaptchaResult.failure("Capsolver submit failed: " + error);
            }

        } catch (Exception e) {
            log.error("Capsolver hCaptcha solving failed", e);
            return CaptchaResult.failure("Solving failed: " + e.getMessage());
        }
    }

    @Override
    public CaptchaResult solveGeeTest(String gt, String challenge, String apiServer, String pageUrl) {
        long startTime = System.currentTimeMillis();
        String apiKey = captchaConfig.getApiKey();

        if (apiKey == null || apiKey.isEmpty()) {
            return CaptchaResult.failure("Capsolver API key not configured");
        }

        try {
            ObjectNode task = OBJECT_MAPPER.createObjectNode();
            task.put("type", "GeeTestTaskProxyLess");
            task.put("gt", gt);
            task.put("challenge", challenge);
            task.put("websiteURL", pageUrl);

            ObjectNode requestBody = OBJECT_MAPPER.createObjectNode();
            requestBody.put("clientKey", apiKey);
            requestBody.set("task", task);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + "/createTask"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode result = OBJECT_MAPPER.readTree(response.body());

            if (result.has("taskId")) {
                String taskId = result.get("taskId").asText();
                return pollForResult(taskId, startTime);
            } else {
                String error = result.has("errorDescription") ?
                        result.get("errorDescription").asText() : "Unknown error";
                return CaptchaResult.failure("Capsolver submit failed: " + error);
            }

        } catch (Exception e) {
            log.error("Capsolver GeeTest solving failed", e);
            return CaptchaResult.failure("Solving failed: " + e.getMessage());
        }
    }

    /**
     * 轮询获取结果。
     */
    private CaptchaResult pollForResult(String taskId, long startTime) {
        Duration timeout = captchaConfig.getMaxTimeout();
        Duration pollingInterval = Duration.ofSeconds(5);
        String apiKey = captchaConfig.getApiKey();

        long endTime = startTime + timeout.toMillis();

        while (System.currentTimeMillis() < endTime) {
            try {
                Thread.sleep(pollingInterval.toMillis());

                ObjectNode requestBody = OBJECT_MAPPER.createObjectNode();
                requestBody.put("clientKey", apiKey);
                requestBody.put("taskId", taskId);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE_URL + "/getTaskResult"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode result = OBJECT_MAPPER.readTree(response.body());

                if (result.has("status")) {
                    String status = result.get("status").asText();
                    if ("ready".equals(status)) {
                        long solveTime = System.currentTimeMillis() - startTime;
                        JsonNode solution = result.get("solution");
                        if (solution != null && solution.has("text")) {
                            String code = solution.get("text").asText();
                            return CaptchaResult.success(code, solveTime, 0);
                        } else if (solution != null && solution.has("gRecaptchaResponse")) {
                            String code = solution.get("gRecaptchaResponse").asText();
                            return CaptchaResult.success(code, solveTime, 0);
                        }
                    }
                }

                if (result.has("errorId") && result.get("errorId").asInt() != 0) {
                    String error = result.has("errorDescription") ?
                            result.get("errorDescription").asText() : "Unknown error";
                    return CaptchaResult.failure("Capsolver error: " + error);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return CaptchaResult.failure("Polling interrupted");
            } catch (Exception e) {
                log.error("Error polling for Capsolver result", e);
            }
        }

        return CaptchaResult.failure("Capsolver solving timeout");
    }

    @Override
    public boolean isAvailable() {
        String apiKey = captchaConfig.getApiKey();
        return apiKey != null && !apiKey.isEmpty();
    }

    @Override
    public double getBalance() {
        String apiKey = captchaConfig.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            return 0;
        }

        try {
            ObjectNode requestBody = OBJECT_MAPPER.createObjectNode();
            requestBody.put("clientKey", apiKey);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + "/getBalance"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode result = OBJECT_MAPPER.readTree(response.body());

            if (result.has("balance")) {
                return result.get("balance").asDouble();
            }
        } catch (Exception e) {
            log.error("Failed to get Capsolver balance", e);
        }

        return 0;
    }

    @Override
    public String getServiceType() {
        return "capsolver";
    }
}
