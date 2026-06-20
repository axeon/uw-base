package uw.common.app.vo;

import com.esotericsoftware.kryo.KryoSerializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import uw.common.util.KryoUtils;

/**
 * 基于 Kryo 的 Redis 序列化器。
 * <p>
 * 使用 Kryo 可以大幅减少内存占用并提高序列化/反序列化速度，适用于需要缓存到 Redis 的
 * {@link KryoSerializable} 对象。反序列化时按构造时传入的目标类型还原对象。
 * </p>
 *
 * @param <T> 可被 Kryo 序列化的对象类型
 */
public class KryoRedisSerializer<T extends KryoSerializable> implements RedisSerializer<T> {

    private static final Logger logger = LoggerFactory.getLogger(KryoRedisSerializer.class);


    /**
     * 反序列化的目标类型。
     */
    private Class<? extends KryoSerializable> cls;

    /**
     * 构造序列化器。
     *
     * @param type 反序列化的目标类型
     */
    public KryoRedisSerializer(Class<? extends KryoSerializable> type) {
        this.cls = type;
    }

    /**
     * 将对象序列化为字节数组。
     *
     * @param msg 待序列化对象，可为 null
     * @return Kryo 序列化后的字节数组
     * @throws SerializationException 序列化失败时抛出
     */
    @Override
    public byte[] serialize(T msg) throws SerializationException {
        return KryoUtils.serialize(msg);
    }

    /**
     * 将字节数组反序列化为目标类型对象。
     *
     * @param data 字节数组，可为 null/空
     * @return 还原后的对象
     * @throws SerializationException 反序列化失败时抛出
     */
    @SuppressWarnings("unchecked")
    @Override
    public T deserialize(byte[] data) throws SerializationException {
        return (T) KryoUtils.deserialize(data, cls);
    }


}
