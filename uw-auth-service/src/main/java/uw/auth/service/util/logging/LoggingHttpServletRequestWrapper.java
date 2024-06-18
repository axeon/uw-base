package uw.auth.service.util.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.ContentCachingRequestWrapper;


/**
 * 记录日志的ServletRequest Wrapper
 */
public class LoggingHttpServletRequestWrapper extends ContentCachingRequestWrapper {

    /**
     * Create a new ContentCachingRequestWrapper for the given servlet request.
     *
     * @param request the original servlet request
     */
    public LoggingHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }
}
