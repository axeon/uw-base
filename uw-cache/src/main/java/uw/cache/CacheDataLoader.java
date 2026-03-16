package uw.cache;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 缓存数据加载器。
 * 因为Redis序列化后会丢失类型，所以必须携带数据类型。
 * 开始使用的Function方案，当转成lambda之后丢失泛型类型。
 * 后使用interface方案，会被自动转为lambda丢失泛型类型。
 * 所以使用抽象类解决问题，同时也解决了泛型类型获取问题。
 *
 * @param <K>
 * @param <V>
 */
public abstract class CacheDataLoader<K, V> {

    /**
     * 自定义过期时间。
     *  0：永久有效。
     * >0：指定过期毫秒数。
     */
    private long expireMillis = 0L;

    /**
     * 加载数据。
     *
     * @param key
     * @return
     * @throws Exception
     */
    public abstract V load(K key) throws Exception;

    /**
     * 自定义设置过期时间。
     *
     * @param expireMillis
     */
    public void setExpireMillis(long expireMillis) {
        this.expireMillis = expireMillis;
    }

    /**
     * 自定义获取过期时间，默认0表示不定义。
     *
     * @return
     */
    public long getExpireMillis() {
        return expireMillis;
    }

    /**
     * 获取数值类型。
     *
     * @return
     */
    public Type getValueType() {
        if (getClass().getGenericSuperclass() instanceof ParameterizedType parameterizedType) {
            Type[] types = parameterizedType.getActualTypeArguments();
            return types[1];
        }
        return null;
    }
}
