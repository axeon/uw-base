package uw.webot.captcha.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.webot.captcha.CaptchaService;
import uw.webot.captcha.CaptchaConfig;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * OCR验证码服务实现。
 * 本地OCR识别备选方案，支持常见验证码类型识别。
 * 注意：此实现为简化版本，实际生产环境建议使用Tesseract或EasyOCR等专业OCR引擎。
 */
public class LocalOcrCaptchaServiceImpl implements CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(LocalOcrCaptchaServiceImpl.class);

    /**
     * Captcha配置属性。
     */
    private final CaptchaConfig captchaConfig;


    public LocalOcrCaptchaServiceImpl(CaptchaConfig captchaConfig) {
        this.captchaConfig = captchaConfig;
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

        try {
            // 预处理图片
            BufferedImage processedImage = preprocessImage(imageData, options);

            // 执行OCR识别（简化实现，实际应调用Tesseract或EasyOCR）
            String recognizedText = performOcr(processedImage, options);

            long solveTime = System.currentTimeMillis() - startTime;

            if (recognizedText != null && !recognizedText.isEmpty()) {
                return CaptchaResult.success(recognizedText, solveTime, 0);
            } else {
                return CaptchaResult.failure("OCR recognition returned empty result");
            }

        } catch (Exception e) {
            log.error("OCR recognition failed", e);
            return CaptchaResult.failure("OCR recognition failed: " + e.getMessage());
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
        try {
            byte[] imageData = Base64.getDecoder().decode(base64Image);
            return recognizeImageCaptcha(imageData, options);
        } catch (Exception e) {
            log.error("Failed to decode base64 image", e);
            return CaptchaResult.failure("Failed to decode base64 image: " + e.getMessage());
        }
    }

    /**
     * 解决ReCaptcha V2。
     * 本地OCR服务不支持此功能。
     *
     * @param siteKey 站点密钥
     * @param pageUrl 页面URL
     * @return 识别结果（失败）
     */
    @Override
    public CaptchaResult solveReCaptchaV2(String siteKey, String pageUrl) {
        return CaptchaResult.failure("OCR service does not support ReCaptcha V2");
    }

    /**
     * 解决ReCaptcha V3。
     * 本地OCR服务不支持此功能。
     *
     * @param siteKey  站点密钥
     * @param pageUrl  页面URL
     * @param action   动作
     * @param minScore 最低分数
     * @return 识别结果（失败）
     */
    @Override
    public CaptchaResult solveReCaptchaV3(String siteKey, String pageUrl, String action, float minScore) {
        return CaptchaResult.failure("OCR service does not support ReCaptcha V3");
    }

    /**
     * 解决hCaptcha。
     * 本地OCR服务不支持此功能。
     *
     * @param siteKey 站点密钥
     * @param pageUrl 页面URL
     * @return 识别结果（失败）
     */
    @Override
    public CaptchaResult solveHCaptcha(String siteKey, String pageUrl) {
        return CaptchaResult.failure("OCR service does not support hCaptcha");
    }

    /**
     * 解决GeeTest。
     * 本地OCR服务不支持此功能。
     *
     * @param gt         GT参数
     * @param challenge  Challenge参数
     * @param apiServer  API服务器
     * @param pageUrl    页面URL
     * @return 识别结果（失败）
     */
    @Override
    public CaptchaResult solveGeeTest(String gt, String challenge, String apiServer, String pageUrl) {
        return CaptchaResult.failure("OCR service does not support GeeTest");
    }

    /**
     * 检查服务是否可用。
     * 本地OCR服务始终可用。
     *
     * @return true
     */
    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * 获取账户余额。
     * 本地OCR服务无余额概念，返回-1。
     *
     * @return -1
     */
    @Override
    public double getBalance() {
        return -1;
    }

    /**
     * 获取服务类型。
     *
     * @return "ocr"
     */
    @Override
    public String getServiceType() {
        return "ocr";
    }

    /**
     * 预处理图片。
     *
     * @param imageData 图片数据
     * @param options   识别选项
     * @return 预处理后的图片
     */
    private BufferedImage preprocessImage(byte[] imageData, CaptchaOptions options) throws Exception {
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));

        if (originalImage == null) {
            throw new IllegalArgumentException("Failed to read image data");
        }

        // 转换为灰度图
        BufferedImage grayImage = new BufferedImage(
                originalImage.getWidth(),
                originalImage.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY
        );
        Graphics2D g = grayImage.createGraphics();
        g.drawImage(originalImage, 0, 0, null);
        g.dispose();

        // 二值化处理
        int threshold = 80;
        BufferedImage binaryImage = new BufferedImage(
                grayImage.getWidth(),
                grayImage.getHeight(),
                BufferedImage.TYPE_BYTE_BINARY
        );

        for (int y = 0; y < grayImage.getHeight(); y++) {
            for (int x = 0; x < grayImage.getWidth(); x++) {
                int rgb = grayImage.getRGB(x, y);
                int gray = rgb & 0xFF;
                if (gray < threshold) {
                    binaryImage.setRGB(x, y, Color.BLACK.getRGB());
                } else {
                    binaryImage.setRGB(x, y, Color.WHITE.getRGB());
                }
            }
        }

        return binaryImage;
    }

    /**
     * 执行OCR识别。
     * 注意：此为简化实现，实际应集成Tesseract或EasyOCR。
     *
     * @param image   预处理后的图片
     * @param options 识别选项
     * @return 识别结果
     */
    private String performOcr(BufferedImage image, CaptchaOptions options) {
        // 简化实现：返回占位符
        // 实际生产环境应使用以下方式之一：
        // 1. Tesseract: Tesseract.doOCR(image)
        // 2. EasyOCR: reader.readtext(image)

        log.warn("OCR recognition is using simplified implementation. " +
                "For production use, please integrate Tesseract or EasyOCR.");

        // 返回一个占位符，提示需要集成真正的OCR引擎
        return "OCR_PLACEHOLDER";
    }

    /**
     * 获取图片的Base64编码。
     *
     * @param image 图片
     * @return Base64编码
     */
    private String imageToBase64(BufferedImage image) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}
