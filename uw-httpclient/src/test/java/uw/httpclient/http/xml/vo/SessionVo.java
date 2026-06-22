package uw.httpclient.http.xml.vo;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * 解析带命名空间的 Xml 测试 VO。
 * <p>
 * 用于 {@link uw.httpclient.http.xml.XmlInterfaceTest} 验证 {@link uw.httpclient.xml.XmlObjectMapperImpl}
 * 对带命名空间 XML 的序列化/反序列化能力。根节点为命名空间 {@code i} 下的 {@code Response}。
 *
 * @since 2018/1/9
 */
@JacksonXmlRootElement(namespace = "i", localName = "Response")
public class SessionVo {

    /**
     * 会话 ID。
     */
    private String sessionId;

    /**
     * 状态码。
     */
    private int code;

    /**
     * 结果节点。
     */
    private Result result;

    /**
     * 错误信息。
     */
    private String error;

    /**
     * 获取会话 ID。
     *
     * @return 会话 ID。
     */
    @JacksonXmlProperty(localName = "SessionId")
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 设置会话 ID。
     *
     * @param sessionId 会话 ID。
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 获取状态码。
     *
     * @return 状态码。
     */
    @JacksonXmlProperty(localName = "Code")
    public int getCode() {
        return code;
    }

    /**
     * 设置状态码。
     *
     * @param code 状态码。
     */
    public void setCode(int code) {
        this.code = code;
    }

    /**
     * 获取结果节点。
     *
     * @return 结果节点。
     */
    @JacksonXmlProperty(localName = "Result", namespace = "i")
    public Result getResult() {
        return result;
    }

    /**
     * 设置结果节点。
     *
     * @param result 结果节点。
     */
    public void setResult(Result result) {
        this.result = result;
    }

    /**
     * 获取错误信息。
     *
     * @return 错误信息。
     */
    @JacksonXmlProperty(localName = "Error")
    public String getError() {
        return error;
    }

    /**
     * 设置错误信息。
     *
     * @param error 错误信息。
     */
    public void setError(String error) {
        this.error = error;
    }

    /**
     * 结果子节点，带命名空间 {@code i}。
     */
    @JacksonXmlRootElement(namespace = "i", localName = "Result")
    public static class Result {

        /**
         * 是否为空（属性形式）。
         */
        private boolean nil;

        /**
         * 获取是否为空。
         *
         * @return 是否为空。
         */
        @JacksonXmlProperty(localName = "nil", isAttribute = true)
        public boolean getNil() {
            return nil;
        }

        /**
         * 设置是否为空。
         *
         * @param nil 是否为空。
         */
        public void setNil(boolean nil) {
            this.nil = nil;
        }
    }
}
