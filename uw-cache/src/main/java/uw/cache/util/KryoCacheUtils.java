package uw.cache.util;

import uw.cache.vo.CacheValueWrapper;
import uw.common.util.KryoUtils;

/**
 * 缓存专用的kryo序列化工具类。
 * <p>
 * 通用序列化能力（serialize/deserialize 等）已统一收敛到 {@link KryoUtils}，
 * 本类仅保留与 {@link CacheValueWrapper} 强相关的封装序列化逻辑：
 * 写入时先写过期时间，再写入业务值；读取时反向还原。
 *
 * @author axeon
 */
public class KryoCacheUtils {

    private KryoCacheUtils() {
    }

    /**
     * 序列化缓存值封装对象。
     * <p>
     * 协议：先写入8字节过期时间戳，若业务值非空则继续用kryo写入对象；为空则仅保留过期时间。
     *
     * @param valueWrapper 缓存值封装对象，为null时返回null
     * @return 序列化后的字节数组
     */
    public static byte[] serializeValueWrapper(CacheValueWrapper<?> valueWrapper) {
        if (valueWrapper == null) {
            return null;
        }
        return KryoUtils.serialize((kryo, output) -> {
            output.writeLong(valueWrapper.getExpiredAt());
            if (valueWrapper.getValue() != null) {
                kryo.writeObject(output, valueWrapper.getValue());
            }
        });
    }

    /**
     * 反序列化缓存值封装对象。
     * <p>
     * 与 {@link #serializeValueWrapper} 对应：先读取过期时间戳，若仍有剩余数据则读取业务值。
     *
     * @param data 字节数组，为null或空时返回null
     * @param cls  业务值的类型
     * @param <T>  业务值类型
     * @return 缓存值封装对象
     */
    public static <T> CacheValueWrapper<T> deserializeValueWrapper(byte[] data, Class<T> cls) {
        if (data == null || data.length == 0) {
            return null;
        }
        return KryoUtils.deserialize(data, (kryo, input) -> {
            CacheValueWrapper<T> valueWrapper = new CacheValueWrapper<>();
            valueWrapper.setExpiredAt(input.readLong());
            if (!input.end()) {
                valueWrapper.setValue(kryo.readObject(input, cls));
            }
            return valueWrapper;
        });
    }

}
