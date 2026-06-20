package uw.auth.service.util.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.ContentCachingRequestWrapper;

/**
 * 用于记录请求日志的 {@link ContentCachingRequestWrapper}。
 * <p>
 * 在请求处理完成后通过 {@link #getContentAsByteArray()} 获取已缓存的请求体，
 * 供 {@code AuthServiceFilter} 写入操作日志。body 缓存上限可配，避免大文件上传导致 OOM。
 */
public class LoggingHttpServletRequestWrapper extends ContentCachingRequestWrapper {

    /**
     * 默认body缓存上限（8MB）。
     */
    private static final int DEFAULT_CONTENT_CACHE_LIMIT = 8 * 1024 * 1024;

    /**
     * Create a new ContentCachingRequestWrapper for the given servlet request.
     *
     * @param request the original servlet request
     */
    public LoggingHttpServletRequestWrapper(HttpServletRequest request) {
        super(request, DEFAULT_CONTENT_CACHE_LIMIT);
    }

    /**
     * Create a new ContentCachingRequestWrapper for the given servlet request and cache limit.
     *
     * @param request       the original servlet request
     * @param contentCacheLimit 缓存body的最大字节数
     */
    public LoggingHttpServletRequestWrapper(HttpServletRequest request, int contentCacheLimit) {
        super(request, contentCacheLimit);
    }
}
