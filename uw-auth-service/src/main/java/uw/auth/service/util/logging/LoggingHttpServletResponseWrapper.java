package uw.auth.service.util.logging;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.ContentCachingResponseWrapper;


/**
 * 记录日志的ServletRequest Wrapper
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
