package uw.webot.session.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.cache.FusionCache;
import uw.webot.WebotSession;
import uw.webot.conf.WebotProperties;
import uw.webot.session.SessionService;

import java.time.Duration;

/**
 * 分布式会话管理器实现。
 * 基于FusionCache组件实现分布式session存储，确保会话状态持久化。
 * <p>
 * 注意：userId由业务方管理，业务方需要自行维护userId与sessionId的关联关系。
 * </p>
 */
public class GlobalSessionServiceImpl implements SessionService {

    private static final Logger log = LoggerFactory.getLogger(GlobalSessionServiceImpl.class);

    private static final String CACHE_NAME = "uw-webot-session";

    public GlobalSessionServiceImpl(WebotProperties.SessionProperties sessionProperties) {
        FusionCache.config(FusionCache.Config.builder(CACHE_NAME).localCacheMaxNum(1000).globalCache(sessionProperties.isDistributed()).build());
        log.info("GlobalSessionServiceImpl initialized with cacheName: {}", CACHE_NAME);
    }


    @Override
    public void setSession(String sessionId, WebotSession session, Duration ttl) {
        FusionCache.put(CACHE_NAME, sessionId, session, ttl.toMillis());
    }

    @Override
    public WebotSession getSession(String sessionId) {
        return FusionCache.get(CACHE_NAME, sessionId);
    }

    @Override
    public boolean invalidateSession(String sessionId) {
        return FusionCache.invalidate(CACHE_NAME, sessionId);
    }

    @Override
    public long getActiveSessionCount() {
        return FusionCache.localCacheSize(CACHE_NAME);
    }
}
