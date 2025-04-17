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

    public abstract V load(K key) throws Exception;

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
