package uw.webot.captcha;

/**
 * 验证码服务接口。
 * 定义标准化的验证码解码服务接口，包含识别、验证、重试等核心方法。
 *
 * @author axeon
 * @since 1.0.0
 */
public interface CaptchaService {

    /**
     * 识别图片验证码。
     *
     * @param imageData 图片字节数据
     * @return 识别结果
     */
    CaptchaResult recognizeImageCaptcha(byte[] imageData);

    /**
     * 识别图片验证码（带额外参数）。
     *
     * @param imageData 图片字节数据
     * @param options   识别选项
     * @return 识别结果
     */
    CaptchaResult recognizeImageCaptcha(byte[] imageData, CaptchaOptions options);

    /**
     * 识别Base64编码的图片验证码。
     *
     * @param base64Image Base64编码的图片
     * @return 识别结果
     */
    CaptchaResult recognizeBase64Captcha(String base64Image);

    /**
     * 识别Base64编码的图片验证码（带额外参数）。
     *
     * @param base64Image Base64编码的图片
     * @param options     识别选项
     * @return 识别结果
     */
    CaptchaResult recognizeBase64Captcha(String base64Image, CaptchaOptions options);

    /**
     * 解决ReCaptcha V2。
     *
     * @param siteKey   站点密钥
     * @param pageUrl   页面URL
     * @return 识别结果
     */
    CaptchaResult solveReCaptchaV2(String siteKey, String pageUrl);

    /**
     * 解决ReCaptcha V3。
     *
     * @param siteKey   站点密钥
     * @param pageUrl   页面URL
     * @param action    动作
     * @param minScore  最小分数
     * @return 识别结果
     */
    CaptchaResult solveReCaptchaV3(String siteKey, String pageUrl, String action, float minScore);

    /**
     * 解决hCaptcha。
     *
     * @param siteKey   站点密钥
     * @param pageUrl   页面URL
     * @return 识别结果
     */
    CaptchaResult solveHCaptcha(String siteKey, String pageUrl);

    /**
     * 解决GeeTest验证码。
     *
     * @param gt        GT参数
     * @param challenge Challenge参数
     * @param apiServer API服务器
     * @param pageUrl   页面URL
     * @return 识别结果
     */
    CaptchaResult solveGeeTest(String gt, String challenge, String apiServer, String pageUrl);

    /**
     * 检查服务是否可用。
     *
     * @return 是否可用
     */
    boolean isAvailable();

    /**
     * 获取服务余额。
     *
     * @return 余额（如果支持）
     */
    double getBalance();

    /**
     * 获取服务类型。
     *
     * @return 服务类型
     */
    String getServiceType();

    /**
     * 验证码识别结果。
     */
    record CaptchaResult(
            boolean success,
            String code,
            String errorMessage,
            long solveTimeMillis,
            double cost
    ) {
        public CaptchaResult {
            if (code == null) {
                code = "";
            }
            if (errorMessage == null) {
                errorMessage = "";
            }
        }

        public static CaptchaResult success(String code, long solveTimeMillis, double cost) {
            return new CaptchaResult(true, code, null, solveTimeMillis, cost);
        }

        public static CaptchaResult failure(String errorMessage) {
            return new CaptchaResult(false, null, errorMessage, 0, 0);
        }
    }

    /**
     * 验证码识别选项。
     */
    class CaptchaOptions {
        /**
         * 验证码类型。
         */
        private String captchaType = "image";

        /**
         * 最小长度。
         */
        private Integer minLength;

        /**
         * 最大长度。
         */
        private Integer maxLength;

        /**
         * 字符集。
         */
        private String charset;

        /**
         * 区分大小写。
         */
        private boolean caseSensitive = false;

        /**
         * 语言。
         */
        private String language = "en";

        /**
         * 额外参数。
         */
        private java.util.Map<String, String> extraParams = new java.util.HashMap<>();

        public String getCaptchaType() {
            return captchaType;
        }

        public void setCaptchaType(String captchaType) {
            this.captchaType = captchaType;
        }

        public Integer getMinLength() {
            return minLength;
        }

        public void setMinLength(Integer minLength) {
            this.minLength = minLength;
        }

        public Integer getMaxLength() {
            return maxLength;
        }

        public void setMaxLength(Integer maxLength) {
            this.maxLength = maxLength;
        }

        public String getCharset() {
            return charset;
        }

        public void setCharset(String charset) {
            this.charset = charset;
        }

        public boolean isCaseSensitive() {
            return caseSensitive;
        }

        public void setCaseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public java.util.Map<String, String> getExtraParams() {
            return extraParams;
        }

        public void setExtraParams(java.util.Map<String, String> extraParams) {
            this.extraParams = extraParams;
        }

        public CaptchaOptions withCaptchaType(String captchaType) {
            this.captchaType = captchaType;
            return this;
        }

        public CaptchaOptions withMinLength(Integer minLength) {
            this.minLength = minLength;
            return this;
        }

        public CaptchaOptions withMaxLength(Integer maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        public CaptchaOptions withCharset(String charset) {
            this.charset = charset;
            return this;
        }

        public CaptchaOptions withCaseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
            return this;
        }

        public CaptchaOptions withLanguage(String language) {
            this.language = language;
            return this;
        }

        public CaptchaOptions withExtraParam(String key, String value) {
            this.extraParams.put(key, value);
            return this;
        }
    }
}
