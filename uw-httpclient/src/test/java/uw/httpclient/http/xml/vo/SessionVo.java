package uw.httpclient.http.xml.vo;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * 解析带命名空间的Xml
 *
 * 
 * @since 2018/1/9
 */
@JacksonXmlRootElement(namespace = "i", localName = "Response")
public class SessionVo {

    private String sessionId;

    private int code;

    private Result result;

    private String error;

    @JacksonXmlProperty(localName = "SessionId")
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @JacksonXmlProperty(localName = "Code")
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    @JacksonXmlProperty(localName = "Result", namespace = "i")
    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    @JacksonXmlProperty(localName = "Error")
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @JacksonXmlRootElement(namespace = "i", localName = "Result")
    public static class Result {

        private boolean nil;

        @JacksonXmlProperty(localName = "nil", isAttribute = true)
        public boolean getNil() {
            return nil;
        }

        public void setNil(boolean nil) {
            this.nil = nil;
        }
    }
}
