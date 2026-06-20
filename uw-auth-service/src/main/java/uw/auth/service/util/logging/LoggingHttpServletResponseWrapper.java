package uw.auth.service.util.logging;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.ContentCachingResponseWrapper;


/**
 * 用于记录响应日志的 {@link ContentCachingResponseWrapper}。
 * <p>
 * 在响应输出后通过 {@link #getContentAsByteArray()} 获取已缓存的响应体写入操作日志，
 * 并由 {@link #copyBodyToResponse()} 将内容真正写回客户端。
 */
public class LoggingHttpServletResponseWrapper extends ContentCachingResponseWrapper {

    /**
     * Create a new ContentCachingResponseWrapper for the given servlet response.
     *
     * @param response the original servlet response
     */
    public LoggingHttpServletResponseWrapper(HttpServletResponse response) {
        super(response);
    }
}
