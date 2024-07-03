package uw.cache.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import uw.cache.FusionCache;
import uw.cache.constant.CacheNotifyType;
import uw.cache.util.KryoUtils;
import uw.cache.vo.FusionCacheNotifyMessage;

/**
 * 融合缓存通知的监听器。
 * 主要用于多实例之间同步清除缓存。
 */
public class FusionCacheNotifyListener implements MessageListener {

    private final Logger logger = LoggerFactory.getLogger( FusionCacheNotifyListener.class );


    @Override
    public void onMessage(Message message, byte[] pattern) {
        FusionCacheNotifyMessage notifyMessage = KryoUtils.deserialize( message.getBody(), FusionCacheNotifyMessage.class );
        if (FusionCache.existsCache( notifyMessage.getCacheName() )) {
            if (logger.isDebugEnabled()) {
                logger.debug( "receive fusion cache notify message! type: {}, Name: {}, key: {}", notifyMessage.getNotifyType(), notifyMessage.getCacheName(),
                        notifyMessage.getCacheKey() );
            }
            if (notifyMessage.getNotifyType() == CacheNotifyType.INVALIDATE.getValue()) {
                FusionCache.invalidate( notifyMessage.getCacheName(), notifyMessage.getCacheKey(), false );
            } else if (notifyMessage.getNotifyType() == CacheNotifyType.REFRESH.getValue()) {
                FusionCache.refresh( notifyMessage.getCacheName(), notifyMessage.getCacheKey(), false );
            } else {
                logger.warn( "receive unknown fusion cache notify message! type: {}, Name: {}, key: {}", notifyMessage.getNotifyType(), notifyMessage.getCacheName(),
                        notifyMessage.getCacheKey() );
            }
        }
    }
}
