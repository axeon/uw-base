package uw.cache.serializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import uw.common.data.KryoData;
import uw.common.util.KryoUtils;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Supplier;

/**
 * 基于 Kryo 的 Redis 序列化器，实现 Spring Data Redis 的 {@link RedisSerializer} 接口。
 * <p>
 * 使用 Kryo 可以大幅减少内存占用并提高序列化/反序列化速度，适用于需要缓存到 Redis 的
 * {@link KryoData} 对象。底层的 Kryo 实例管理与字节读写由 {@link KryoUtils} 统一负责，
 * 本类仅负责将 Spring 的 {@code byte[] <-> T} 语义适配到 {@code KryoUtils} 的静态方法上。
 * </p>
 *
 * <h3>类型约束</h3>
 * <ul>
 *     <li>反序列化需要预先知道目标类型，因此构造时必须传入具体的 {@link KryoData} 实现类，
 *     不可传入接口或抽象类型（这是 Kryo 反序列化的硬性要求）。</li>
 *     <li>目标类必须提供<b>公开无参构造</b>：构造序列化器时会用 {@link LambdaMetafactory} 把该构造
 *     织造成快速实例工厂，织造失败（无公开无参构造、抽象类等）会立即抛 {@link SerializationException}
 *     （fail-fast），避免运行期才发现。</li>
 *     <li>{@code serialize} 不依赖该类型，因此序列化任意同类型对象均不受影响。</li>
 * </ul>
 *
 * <h3>线程安全性</h3>
 * <p>
 * 本类所有字段均为不可变（{@code cls}、{@code defaultBufferSize}、{@code instanceFactory}），
 * {@link Supplier#get()} 每次返回新实例、无共享可变状态，故本类线程安全。Spring 通常以单例方式复用
 * {@link RedisSerializer}，底层的 {@link KryoUtils} 也是线程安全的。
 * </p>
 *
 * <h3>空值处理</h3>
 * <p>
 * {@code serialize(null)} 委派给 {@link KryoUtils}（返 null）；{@code deserialize} 对 null/空字节数组返回 null。
 * </p>
 *
 * @param <T> 可被 Kryo 序列化的对象类型，必须为 {@link KryoData} 的具体实现类
 * @see KryoUtils
 * @see RedisSerializer
 */
public class KryoDataRedisSerializer<T extends KryoData> implements RedisSerializer<T> {

    private static final Logger logger = LoggerFactory.getLogger(KryoDataRedisSerializer.class);

    /**
     * writeData 默认的 Output 初始缓冲区大小（字节）。
     * <p>
     * 多数业务对象序列化结果在 2KB 以内，超出会自动扩容（Output maxCapacity=-1 无上限）。
     * 调用方若能精确预估输出大小，应直接用传紧凑 bufferSize 以降低 GC。
     */
    private static final int DEFAULT_BUFFER_SIZE = 2560;
    /**
     * 默认的初始化缓冲区大小，用于创建 Kryo 输出流。
     */
    private final int defaultBufferSize;

    /**
     * 反序列化的目标类型，必须是 {@link KryoData} 的具体实现类（不可为接口/抽象类）。
     * 构造期用于织造 {@link #instanceFactory}，运行期通过 {@link #getTargetType()} 对外暴露（日志/诊断用）。
     */
    private final Class<? extends KryoData> cls;
    /**
     * 目标类型的无参实例工厂，由 {@link #cls} 的无参构造在构造器里一次性织造为 {@link Supplier}。
     * <p>
     * 反序列化在单例 RedisSerializer 上高频调用，每次 {@code deserialize} 都需要新实例。
     * 直接走 {@code cls.getDeclaredConstructor().newInstance()} 每次都有 Constructor 复制 + 反射调用开销；
     * 这里用 {@link LambdaMetafactory} 把无参构造编译成 {@code () -> new T()} 的 lambda（JVM invokedynamic 直连），
     * {@link Supplier#get()} 等价于直接 {@code new}，近乎零开销。
     * </p>
     * <p>
     * 织造在构造器中<b>只做一次</b>，失败立即 fail-fast（无公开无参构造则构造序列化器即抛异常），
     * 避免每次 deserialize 时才发现类型不可实例化。
     * </p>
     */
    private final Supplier<KryoData> instanceFactory;

    /**
     * 构造序列化器。
     *
     * @param cls 反序列化的目标类型，必须为 {@link KryoData} 的具体实现类且提供公开无参构造
     */
    public KryoDataRedisSerializer(Class<? extends KryoData> cls) {
        this(cls, DEFAULT_BUFFER_SIZE);
    }

    /**
     * 构造序列化器。
     *
     * @param cls             反序列化的目标类型，必须为 {@link KryoData} 的具体实现类且提供公开无参构造
     * @param defaultBufferSize 序列化时 Output 的初始缓冲区大小
     */
    public KryoDataRedisSerializer(Class<? extends KryoData> cls, int defaultBufferSize) {
        this.cls = cls;
        this.defaultBufferSize = defaultBufferSize;
        this.instanceFactory = buildInstanceFactory(cls);
    }

    /**
     * 返回本序列化器绑定的目标类型。
     * <p>
     * 工厂织造后类型信息已内化进 {@link #instanceFactory}，本字段运行期仅用于对外查询（日志/诊断），
     * 不参与序列化路径。
     * </p>
     *
     * @return 目标类型
     */
    public Class<? extends KryoData> getTargetType() {
        return cls;
    }

    /**
     * 把目标类型的无参构造织造成 {@link Supplier}，供 {@link #deserialize} 快速创建实例。
     *
     * @param type 目标类型
     * @return 每次 {@code get()} 等价于 {@code new type()} 的工厂
     * @throws SerializationException 目标类型无公开无参构造或织造失败时抛出（fail-fast）
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Supplier<KryoData> buildInstanceFactory(Class<? extends KryoData> type) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle ctor = lookup.findConstructor(type, MethodType.methodType(void.class));
            // metafactory 会为无参构造生成一个 () -> new type() 的 Supplier，invokeDynamic 直连，调用近乎零开销。
            return (Supplier) LambdaMetafactory.metafactory(
                    lookup,
                    "get",
                    MethodType.methodType(Supplier.class),
                    MethodType.methodType(Object.class),
                    ctor,
                    MethodType.methodType(type)
            ).getTarget().invoke();
        } catch (Throwable e) {
            throw new SerializationException(
                    "Cannot build instance factory for " + type.getName()
                            + ": 需提供公开无参构造。" + e.getMessage(), e);
        }
    }

    /**
     * 将对象序列化为字节数组，委托给 {@link KryoUtils#serializeData(int, KryoData)}，
     * 使用 {@link #defaultBufferSize} 作为初始缓冲区。
     *
     * @param msg 待序列化对象，可为 null（具体行为见 {@link KryoUtils}）
     * @return Kryo 序列化后的字节数组
     * @throws SerializationException 序列化失败时抛出
     */
    @Override
    public byte[] serialize(T msg) throws SerializationException {
        return KryoUtils.serializeData(defaultBufferSize, msg);
    }

    /**
     * 将字节数组反序列化为目标类型对象。
     * <p>
     * 实例创建走 {@link #instanceFactory}（lambda 工厂，近乎零开销）而非每次反射，
     * 字段填充委托给 {@link KryoUtils#deserializeData(byte[], KryoData)}（传入已创建实例的重载）。
     * </p>
     * <p>
     * data 为 null/空时返回 null（不创建实例），与 Redis 序列化器空值约定一致。
     * </p>
     *
     * @param data 字节数组，可为 null/空
     * @return 还原后的对象
     * @throws SerializationException 反序列化失败时抛出
     */
    @SuppressWarnings("unchecked")
    @Override
    public T deserialize(byte[] data) throws SerializationException {
        if (data == null || data.length == 0) {
            return null;
        }
        final KryoData instance = instanceFactory.get();
        return (T) KryoUtils.deserializeData(data, instance);
    }

}
