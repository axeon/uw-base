package uw.cache.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import uw.cache.FusionCache;
import uw.cache.constant.CacheNotifyType;
import uw.common.util.KryoUtils;
import uw.cache.vo.FusionCacheNotifyMessage;

/**
 * 融合缓存通知的监听器。
 * <p>
 * 订阅 Redis Pub/Sub 通道 {@code UW_CACHE_NOTIFY_CHANNEL}，处理集群内其他实例广播的缓存失效/刷新通知，
 * 从而保证多实例本地 Caffeine 缓存的一致性。
 * <p>
 * 处理逻辑：忽略自身发出的消息（按 senderId 判断），其余按 notifyType 分发到
 * {@link FusionCache#invalidate} / {@link FusionCache#refresh}（notify=false，避免再次广播形成环路）。
 */
public class FusionCacheNotifyListener implements MessageListener {

    private final Logger logger = LoggerFactory.getLogger(FusionCacheNotifyListener.class);


    /**
     * 接收并处理集群缓存通知消息。
     *
     * @param message Redis 消息体（Kryo 序列化的 {@link FusionCacheNotifyMessage}）
     * @param pattern 订阅模式（未使用）
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            FusionCacheNotifyMessage notifyMessage = KryoUtils.deserialize(message.getBody(), FusionCacheNotifyMessage.class);
            if (FusionCache.existsCache(notifyMessage.getCacheName())) {
                if (logger.isDebugEnabled()) {
                    logger.debug("receive fusion cache notify message! type: {}, Name: {}, key: {}", notifyMessage.getNotifyType(), notifyMessage.getCacheName(),
                            notifyMessage.getCacheKey());
                }
                // 忽略自己发送的消息。
                if (notifyMessage.getSenderId() == FusionCache.INSTANCE_ID) {
                    return;
                }
                if (notifyMessage.getNotifyType() == CacheNotifyType.INVALIDATE.getValue()) {
                    FusionCache.invalidate(notifyMessage.getCacheName(), notifyMessage.getCacheKey(), false);
                } else if (notifyMessage.getNotifyType() == CacheNotifyType.REFRESH.getValue()) {
                    FusionCache.refresh(notifyMessage.getCacheName(), notifyMessage.getCacheKey(), false);
                } else {
                    logger.warn("receive unknown fusion cache notify message! type: {}, Name: {}, key: {}", notifyMessage.getNotifyType(), notifyMessage.getCacheName(),
                            notifyMessage.getCacheKey());
                }
            }
        } catch (Exception e) {
            logger.error("receive fusion cache notify message error: {}", e.getMessage());
        }

    }
}
