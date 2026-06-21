package uw.cache.serializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import uw.common.util.KryoUtils;

/**
 * 基于 Kryo 的 Redis 序列化器，实现 Spring Data Redis 的 {@link RedisSerializer} 接口。
 * <p>
 * 使用 Kryo 可以大幅减少内存占用并提高序列化/反序列化速度，适用于需要缓存到 Redis 的<b>任意</b>对象
 * （不再要求实现 {@code KryoSerializable}）。底层的 Kryo 实例管理与字节读写由 {@link KryoUtils} 统一负责，
 * 本类仅负责将 Spring 的 {@code byte[] <-> T} 语义适配到 {@link KryoUtils} 的静态方法上。
 * </p>
 *
 * <p>
 * <b>与 {@code KryoDataRedisSerializer} 的取舍</b>：本类走 KryoUtils 的<b>整对象反射路径</b>
 * （{@link KryoUtils#serialize(Object)} / {@link KryoUtils#deserialize(byte[], Class)}，走池），
 * 无需业务类自实现读写逻辑，最省心；{@code KryoDataRedisSerializer} 走<b>接口式无池路径</b>
 * （{@code serializeData/deserializeData}，每次 new Output/Input），吞吐更高、零池竞争，
 * 但要求业务类实现 {@code KryoData} 接口并手写字段读写。对延迟/吞吐敏感且能接受手写读写的场景，
 * 优先用 {@code KryoDataRedisSerializer}；追求省心用本类。
 * </p>
 *
 * <h3>两种模式（由构造器是否传入 type 决定，二选一不可切换）</h3>
 * <ul>
 *     <li><b>类型已知模式</b>（构造时传入 type）：序列化用 {@link KryoUtils#serialize(Object)}，
 *         反序列化用 {@link KryoUtils#deserialize(byte[], Class)}。<b>字节流不带类信息</b>，体积更小；
 *         序列化端与反序列化端必须传入<b>同一个</b> type，否则字节流不兼容。</li>
 *     <li><b>类型未知模式</b>（构造时不传 type，走 withClass 系列）：序列化用 {@link KryoUtils#serializeWithClass(Object)}，
 *         反序列化用 {@link KryoUtils#deserializeWithClass(byte[])}。<b>字节流自带类信息</b>，
 *         反序列化端无需知道目标类型即可还原；代价是字节流多一份类标识（未 {@code register} 的类写全限定类名，体积偏大）。</li>
 * </ul>
 *
 * <h3>类型约束</h3>
 * <ul>
 *     <li>Kryo 反序列化的硬性要求：类型必须是<b>具体实现类</b>，不可用接口或抽象类型
 *         （{@code List/Map/Set} 要用 {@code ArrayList/LinkedHashMap/HashSet} 等具体实现）。</li>
 *     <li>类型已知模式下，{@code serialize} 不依赖该 type（任意同类型对象均可序列化），
 *         {@code deserialize} 用 type 作为还原目标。</li>
 *     <li>类型未知模式下 type 不参与读写，完全由字节流中的类信息决定。</li>
 * </ul>
 *
 * <h3>线程安全性</h3>
 * <p>
 * 本类所有字段均为不可变（{@code type}、{@code withClass}），实际线程安全性取决于 {@link KryoUtils}
 * 内部的 Kryo 实例管理策略（JCTools 无锁 CAS 池）。Spring 通常以单例方式复用 {@link RedisSerializer}，
 * 底层 {@link KryoUtils} 是线程安全的。
 * </p>
 *
 * <h3>空值处理</h3>
 * <p>
 * {@code null} 入参与空字节数组的处理完全委派给 {@link KryoUtils}（均返回 null），调用方应参考其约定。
 * </p>
 *
 *
 * <h3>泛型参数 {@code <T>}</h3>
 * <p>
 * 不强制上界，表示序列化目标对象类型。类型已知模式下 {@code T} 须为具体实现类（不可为接口/抽象类）；
 * 类型未知模式下 {@code T} 由字节流中的类信息决定，声明时按预期目标类型给出即可。
 * </p>
 *
 * @see KryoUtils
 * @see RedisSerializer
 */
public class KryoRedisSerializer<T> implements RedisSerializer<T> {

    private static final Logger logger = LoggerFactory.getLogger(KryoRedisSerializer.class);

    /**
     * 类型已知模式下的反序列化目标类型；类型未知模式（withClass）下为 {@code null}。
     * <p>
     * 必须是具体实现类（不可为接口/抽象类），否则 Kryo 反序列化失败。
     * </p>
     */
    private final Class<T> type;
    /**
     * 是否为类型未知模式（构造时未传入 type 时为 true，走 {@code serializeWithClass/deserializeWithClass}）。
     * <p>
     * 固化序列化/反序列化路径，避免同一实例上两种模式交叉调用导致字节流不兼容。
     * </p>
     */
    private final boolean withClass;

    /**
     * 构造类型未知模式的序列化器（走 withClass 系列，字节流自带类信息）。
     * <p>
     * 适用于反序列化端不预先知道目标类型、或需在同一序列化器上存取多种类型的场景。
     * 序列化用 {@link KryoUtils#serializeWithClass(Object)}，反序列化用 {@link KryoUtils#deserializeWithClass(byte[])}。
     * </p>
     */
    public KryoRedisSerializer() {
        this.type = null;
        this.withClass = true;
    }

    /**
     * 构造类型已知模式的序列化器（字节流不带类信息，体积更小）。
     * <p>
     * 序列化端与反序列化端必须传入<b>同一个</b> type，否则字节流不兼容。
     * 序列化用 {@link KryoUtils#serialize(Object)}，反序列化用 {@link KryoUtils#deserialize(byte[], Class)}。
     * </p>
     *
     * @param type 反序列化的目标类型，必须是具体实现类（不可为接口/抽象类）
     */
    public KryoRedisSerializer(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null; for type-unknown mode use the no-arg constructor.");
        }
        this.type = type;
        this.withClass = false;
    }

    /**
     * 将对象序列化为字节数组。
     * <p>
     * 类型已知模式委托给 {@link KryoUtils#serialize(Object)}（不带类信息，更小）；
     * 类型未知模式委托给 {@link KryoUtils#serializeWithClass(Object)}（带类信息）。
     * </p>
     *
     * @param msg 待序列化对象，可为 null（具体行为见 {@link KryoUtils}）
     * @return Kryo 序列化后的字节数组
     * @throws SerializationException 序列化失败时抛出
     */
    @Override
    public byte[] serialize(T msg) throws SerializationException {
        if (withClass) {
            return KryoUtils.serializeWithClass(msg);
        }
        return KryoUtils.serialize(msg);
    }

    /**
     * 将字节数组反序列化为目标类型对象。
     * <p>
     * 类型已知模式委托给 {@link KryoUtils#deserialize(byte[], Class)}（用 {@code type} 作为还原类型）；
     * 类型未知模式委托给 {@link KryoUtils#deserializeWithClass(byte[])}（类型由字节流中的类信息决定）。
     * 通过 {@link SuppressWarnings} 抑制 {@code Object}/{@code T} 之间的未检查转换告警——
     * 转换安全性由调用方保证（类型已知模式下序列化端写入的对象类型与 type 一致；类型未知模式下由字节流类信息保证）。
     * </p>
     *
     * @param data 字节数组，可为 null/空（具体行为见 {@link KryoUtils}）
     * @return 还原后的对象
     * @throws SerializationException 反序列化失败时抛出
     */
    @SuppressWarnings("unchecked")
    @Override
    public T deserialize(byte[] data) throws SerializationException {
        if (withClass) {
            return (T) KryoUtils.deserializeWithClass(data);
        }
        return KryoUtils.deserialize(data, type);
    }

}
