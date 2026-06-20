package uw.cache;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 缓存数据加载器。
 * <p>
 * 由于 Kryo 反序列化时会丢失泛型类型信息，所以加载器必须携带数据类型。
 * 早期使用 Function / 函数式接口方案，被自动转为 lambda 后会丢失泛型类型，
 * 因此这里使用抽象类方案，并通过反射 {@code getClass().getGenericSuperclass()} 获取精确的 V 类型。
 * <p>
 * 使用约束：
 * <ol>
 *   <li>必须以匿名内部类形式实现：{@code new CacheDataLoader<K,V>(){...}}，不能用 lambda。</li>
 *   <li>泛型 V 必须是具体实现类（如 ArrayList/LinkedHashMap/HashSet），不能是接口类型。</li>
 * </ol>
 *
 * @param <K> 缓存主键类型
 * @param <V> 缓存值类型（必须是 Kryo 可序列化的具体类）
 */
public abstract class CacheDataLoader<K, V> {

    /**
     * 自定义过期时间。
     * <p>
     * 0：永久有效（默认）。
     * >0：指定过期毫秒数，将覆盖 FusionCache.Config / get 调用时传入的过期时间。
     */
    private long expireMillis = 0L;

    /**
     * 加载数据。
     * <p>
     * 当本地缓存未命中、且全局（Redis）缓存未命中或已过期时触发。
     * 实现方可在内部访问数据库或远程服务获取数据；返回 null 将触发空值保护（防穿透）。
     *
     * @param key 缓存主键
     * @return 加载到的数据，null 表示无数据（将按 nullProtectMillis 短时缓存空值）
     * @throws Exception 加载过程中的异常，框架会捕获并按 failProtectMillis 短时缓存空值
     */
    public abstract V load(K key) throws Exception;

    /**
     * 自定义设置过期时间。
     *
     * @param expireMillis 过期毫秒数，0 表示永久有效
     */
    public void setExpireMillis(long expireMillis) {
        this.expireMillis = expireMillis;
    }

    /**
     * 获取自定义过期时间。
     * <p>
     * 0 表示未自定义（采用调用方传入的过期时间），>0 表示使用此自定义值。
     *
     * @return 过期毫秒数，默认 0
     */
    public long getExpireMillis() {
        return expireMillis;
    }

    /**
     * 获取缓存值 V 的精确类型。
     * <p>
     * 通过反射匿名子类的 {@code ParameterizedType} 获取 V 的实际类型，供 Kryo 反序列化使用。
     * 若子类未保留泛型信息则返回 null。
     *
     * @return V 的 Type，无法解析时返回 null
     */
    public Type getValueType() {
        if (getClass().getGenericSuperclass() instanceof ParameterizedType parameterizedType) {
            Type[] types = parameterizedType.getActualTypeArguments();
            return types[1];
        }
        return null;
    }
}
