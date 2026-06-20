package uw.common.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.jctools.queues.MpmcArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.data.KryoData;
import uw.common.data.PageList;
import uw.common.data.PageRowSet;
import uw.common.dto.PageQueryParam;
import uw.common.dto.QueryParam;
import uw.common.response.ResponseData;

import java.io.OutputStream;
import java.lang.reflect.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * kryo基础工具类。
 * <p>
 * 通过池化的 Kryo 与 Output 对象，提供低开销的对象序列化能力。底层池基于 JCTools {@code MpmcArrayQueue}（无锁 CAS），
 * 相比 kryo 官方 {@code Pool}（{@code LinkedBlockingQueue}，有锁）在多线程下吞吐更高。
 *
 * <h2>API 与选型</h2>
 * lambda/对象式方法统一命名为 {@code serialize}/{@code deserialize}，靠参数重载区分；
 * KryoData 接口式因泛型签名与 deserialize(Function/BiFunction) 冲突，成对使用独立名 {@code serializeData}/{@code deserializeData}。
 * 提供四类序列化/反序列化路径，按"是否需要 kryo.writeObject/readObject"和"是否容忍 GC"选择：
 *
 * <h3>1. 整对象读写（最省心，吞吐中等）</h3>
 * <ul>
 *   <li>{@link #serialize(Object)} / {@link #deserialize(byte[], Class)}：kryo 反射读写整个对象（FieldSerializer）。</li>
 *   <li>路径走 {@link #serializePool}（成对 Kryo+Output）/ {@link #unserializePool}（Kryo），池化复用。</li>
 *   <li><b>适用</b>：对象字段固定、不想手写读写逻辑、对象类型在编译期已知。</li>
 *   <li><b>代价</b>：反射有开销（比手工读写慢约 1.5~2 倍）；未 {@code register} 的类走全限定类名路径，体积偏大。</li>
 * </ul>
 *
 * <h3>2. 自定义读写-完整版（需 kryo 对象）</h3>
 * <ul>
 *   <li>{@link #serialize(BiConsumer)} / {@link #deserialize(byte[], BiFunction)}：回调同时拿到 Kryo 与 Output/Input。</li>
 *   <li>可在同一会话内混用原语（writeLong/writeString）与 {@code kryo.writeObject/readObject}。</li>
 *   <li>路径走 {@link #serializePool}/{@link #unserializePool}（同整对象读写），Output 池化、无 buffer 垃圾。</li>
 *   <li><b>适用</b>：需要对象级序列化（kryo.writeObject）或自定义协议混合 kryo 的场景；GC 敏感时优于轻量版。</li>
 *   <li><b>代价</b>：每次走池的 obtain/free（虽有池竞争，但 Output 复用无垃圾）。</li>
 * </ul>
 *
 * <h3>3. 自定义读写-轻量版（纯原语，无池）</h3>
 * <ul>
 *   <li>{@link #serialize(Consumer)} / {@link #serialize(int, Consumer)} / {@link #deserialize(byte[], Function)}：回调只拿到 Output/Input，不获取 Kryo。</li>
 *   <li>serialize 可选指定 bufferSize（{@link #serialize(int, Consumer)}）或用默认（{@link #serialize(Consumer)}，{@link #DEFAULT_BUFFER_SIZE}=2048）。</li>
 *   <li>每次分配新 Output/Input，多线程吞吐最高（零池竞争）。</li>
 *   <li><b>代价（重要）</b>：每次产生垃圾（Output 的 byte[bufferSize] + toBytes 结果数组），
 *       见 {@link #serialize(int, Consumer)} 的 GC 成本说明。高 QPS 下需权衡 GC 压力。</li>
 * </ul>
 *
 * <h3>4. 接口式手工序列化（KryoData，纯原语，无池）</h3>
 * <ul>
 *   <li>{@link #serializeData(KryoData)} / {@link #serializeData(int, KryoData)} / {@link #deserializeData(byte[], KryoData)}：
 *       数据类实现 {@link KryoData} 接口（自实现 serialize/deserialize 原语），本方法负责借 Output/Input 并 toBytes。</li>
 *   <li>与方式3同属轻量无池路径，区别是把读写逻辑内聚到数据类自身（而非 lambda）；serializeData 同样支持可选 bufferSize。</li>
 *   <li><b>适用</b>：一个类固定一套读写逻辑、多处复用；不想每次写 lambda。</li>
 *   <li><b>代价</b>：同方式3（GC 成本）。</li>
 *   <li><b>命名</b>：KryoData 的序列化/反序列化统一用 serializeData/deserializeData 独立方法名（成对、对称），
 *       不沿用 serialize/deserialize 重载——因 {@code <T extends KryoData>} 泛型签名与 deserialize(Function/BiFunction) 重载解析冲突。</li>
 * </ul>
 *
 * <h3>选型决策树</h3>
 * <pre>
 * 需要序列化任意对象（不想手写字段读写）？
 *   ├─ 是 → serialize(Object)/deserialize(byte[],Class)（整对象读写）
 *   └─ 否（可手写读写逻辑）
 *       ├─ 读写逻辑要内聚到数据类自身？
 *       │   ├─ 是（实现 KryoData 接口）→ serializeData/deserializeData（方式4）
 *       │   └─ 否（用 lambda 回调）
 *       │       ├─ 回调里要用 kryo.writeObject/readObject？
 *       │       │   ├─ 是 → serialize(BiConsumer)/deserialize(BiFunction)（完整版，走池）
 *       │       │   └─ 否（纯原语，无池）
 *       │       │       ├─ 能预估大小 → serialize(bufferSize, Consumer)（紧凑buffer省GC）
 *       │       │       └─ 不能预估 → serialize(Consumer)（默认buffer）
 * </pre>
 *
 * <h2>性能实测（JDK 25、JMH、200条随机数据，ops/ms）</h2>
 * <pre>
 * 方式                   单线程    20线程     50线程
 * 整对象读写-序列化       1486     3042       2183      // 走池
 * 整对象读写-反序列化     1439     3642       3009      // 走池
 * 手工读写-序列化         2609     28066      26221     // 无池(new Output)，吞吐最高但有GC
 * 手工读写-反序列化       2131     17424      17443     // 无池(new Input)
 * KryoSerializable-序列化 2742     2162       1222      // 走池
 * KryoSerializable-反序列化 2147   3044       2346      // 走池
 * </pre>
 * <ul>
 *   <li>单线程：手工 ≈ KryoSerializable，比整对象反射快约 75%~85%（反射慢在 FieldSerializer）。</li>
 *   <li>多线程：手工读写一骑绝尘（零池竞争，多核线性扩展），是走池方案的 8~12 倍。
 *       代价是每次 new 产生垃圾（见上）。</li>
 *   <li>走池方案（整对象/KryoSerializable）多线程受池竞争限制，吞吐上限 ~3000。</li>
 * </ul>
 *
 * <h2>为何用 JCTools 池而非 ThreadLocal</h2>
 * 曾实测对比三种池实现（JCTools MpmcArrayQueue / kryo官方Pool / ThreadLocal），50线程吞吐：
 * <pre>
 * kryo官方Pool（有锁LinkedBlockingQueue）   ~1400   最差，锁竞争串行化
 * JCTools MpmcArrayQueue（无锁CAS）          ~2600   中等，无锁但仍共享
 * ThreadLocal（零共享）                       ~13700  最优，消灭共享
 * </pre>
 * ThreadLocal 在平台线程下吞吐是 JCTools 的 5 倍，看似最优——<b>但本类最终选择 JCTools 池</b>，原因：
 * <ul>
 *   <li><b>虚拟线程兼容性</b>：ThreadLocal 绑定在 Thread 实例上，虚拟线程也是一个 Thread。
 *       百万级虚拟线程下，ThreadLocal 池会随线程数线性膨胀（每虚拟线程一份 Kryo+Output），内存爆炸。
 *       而 JCTools 池容量固定（{@link #MAX_CAPACITY}=128），不随线程数增长。</li>
 *   <li>JDK 官方明确建议虚拟线程下避免用 ThreadLocal 缓存昂贵资源（参见 JEP 444 及 jackson-core #919）。</li>
 *   <li>JCTools 池在平台线程下虽不及 ThreadLocal，但已显著优于 kryo 官方 Pool（无锁 vs 有锁）；
 *       在虚拟线程下则是安全且正确的高并发选择。</li>
 *   <li>结论：JCTools 池在两种线程模型下都"不差"（平台线程次优、虚拟线程最优），是更稳妥的通用方案。</li>
 * </ul>
 *
 * <h2>其他</h2>
 * <ul>
 *   <li>{@link #type2Class(Type)}：将泛型 Type 解析为可实例化的具体 Class（集合接口映射为默认实现）。</li>
 *   <li>Kryo 实例配置见 {@link #newKryo()}：{@code registrationRequired=false}（通用，允许未注册类走类名路径）、
 *       {@code references=false}（性能优先，不支持循环引用——调用方确保对象图无环）。</li>
 *   <li>已预注册常用 JDK 集合/时间类与 uw-common 数据类，走 int ID 路径（快、小）；
 *       调用方传入的<b>其他业务类未注册</b>，走全限定类名路径（慢、大）。高频业务类建议在调用方 register。</li>
 * </ul>
 *
 * <h2>异常策略（所有方法统一）</h2>
 * <ul>
 *   <li>所有 serialize/deserialize/serializeData/deserializeData 方法<b>都不声明 checked exception</b>。</li>
 *   <li>序列化/反序列化失败（字段错位、类型不符、数据损坏）时，kryo 抛 {@code KryoException}（RuntimeException），
 *       本方法<b>直接上浮</b>，由调用方 catch 处理。</li>
 *   <li><b>绝不吞异常、绝不用日志替代异常</b>：序列化失败是致命错误（数据损坏），若被日志吞掉后返回 null/空，
 *       会导致脏数据静默扩散。日志仅用于非致命场景（如池 shrink 失败），不用于序列化异常。</li>
 *   <li>调用方如需记录，应在自己的 catch 块里 log，而非依赖工具方法内部。</li>
 * </ul>
 *
 * @author axeon
 */
public class KryoUtils {

    private static final Logger log = LoggerFactory.getLogger(KryoUtils.class);
    /**
     * 池子的最小容量（稳态保留这么多对象，shrink不会低于此值）。
     */
    private static final int MIN_CAPACITY = 16;
    /**
     * 池子的最大容量（峰值允许临时累积到这么多，超过则free时丢弃）。
     */
    private static final int MAX_CAPACITY = 128;
    /**
     * writeData 默认的 Output 初始缓冲区大小（字节）。
     * <p>
     * 多数业务对象序列化结果在 2KB 以内，超出会自动扩容（Output maxCapacity=-1 无上限）。
     * 调用方若能精确预估输出大小，应直接用 {@link #write(int, Consumer)} 传紧凑 bufferSize 以降低 GC。
     */
    private static final int DEFAULT_BUFFER_SIZE = 2048;
    /**
     * 后台shrink线程的执行间隔（毫秒）。
     */
    private static final long SHRINK_INTERVAL_MILLIS = 60_000L;

    /**
     * 序列化池：成对持有 Kryo 与 Output。
     * <p>
     * 序列化路径需要 Kryo + Output 协同，将二者绑定为一个 entry 池化，
     * 一次 obtain/free 即可同时获取/归还两者，相比独立池减半池操作与竞争面。
     * Output 在归还前由调用方 reset，避免复用脏数据。
     */
    private static final JcPool<SerializeEntry> serializePool = new JcPool<>(MIN_CAPACITY, MAX_CAPACITY) {
        protected SerializeEntry create() {
            return new SerializeEntry(newKryo(), new Output(2560, -1));
        }
    };

    /**
     * 反序列化池：仅持有 Kryo。
     * <p>
     * 反序列化路径不需要 Output（Input 不池化，每次 new），故用独立池，不与序列化路径争抢资源。
     */
    private static final JcPool<Kryo> unserializePool = new JcPool<>(MIN_CAPACITY, MAX_CAPACITY) {
        protected Kryo create() {
            return newKryo();
        }
    };

    /**
     * 构建预注册常用 JDK 类的 Kryo 实例。
     *
     * @return 配置好的 Kryo
     */
    private static Kryo newKryo() {
        Kryo kryo = new Kryo();
        kryo.setClassLoader(Thread.currentThread().getContextClassLoader());
        kryo.setRegistrationRequired(false);
        kryo.setReferences(false);
        kryo.setOptimizedGenerics(true);
        // 基本类型
        kryo.register(ArrayList.class);
        kryo.register(LinkedList.class);
        kryo.register(HashMap.class);
        kryo.register(LinkedHashMap.class);
        kryo.register(TreeMap.class);
        kryo.register(ConcurrentHashMap.class);
        kryo.register(HashSet.class);
        kryo.register(LinkedHashSet.class);
        // 时间类型
        kryo.register(Date.class);
        kryo.register(LocalDate.class);
        kryo.register(LocalTime.class);
        kryo.register(LocalDateTime.class);
        // 自定义类型
        kryo.register(PageList.class);
        kryo.register(PageRowSet.class);
        kryo.register(ResponseData.class);
        kryo.register(QueryParam.class);
        kryo.register(PageQueryParam.class);
        return kryo;
    }

    /**
     * 序列化池条目：成对持有 Kryo 与 Output，二者同生命周期、同线程使用。
     *
     * @param kryo  Kryo 实例
     * @param output Output 实例
     */
    private record SerializeEntry(Kryo kryo, Output output) {
    }

    /**
     * 序列化对象为字节数组。
     *
     * @param value 待序列化对象，为null时返回null
     * @return 序列化后的字节数组
     */
    public static byte[] serialize(Object value) {
        if (value == null) {
            return null;
        }
        byte[] data = null;
        final SerializeEntry entry = serializePool.obtain();
        try {
            entry.kryo().writeObject(entry.output(), value);
            //此时复制出数据
            data = entry.output().toBytes();
        } finally {
            entry.output().reset();
            serializePool.free(entry);
        }
        return data;
    }

    /**
     * 序列化对象并写入输出流。
     * <p>
     * 绑定 OutputStream 后写入并 flush，归还 Output 前解绑流，避免池化对象持有外部流引用。
     *
     * @param value 待序列化对象，为null时直接返回不写入
     * @param out   目标输出流
     */
    public static void serialize(Object value, OutputStream out) {
        if (value == null) {
            return;
        }
        final SerializeEntry entry = serializePool.obtain();
        try {
            entry.output().setOutputStream(out);// 绑定流
            entry.kryo().writeObject(entry.output(), value);
            entry.output().flush();
        } finally {
            entry.output().setOutputStream(null);// ← 解绑！
            entry.output().reset();
            serializePool.free(entry);
        }
    }

    /**
     * 序列化对象为字节数组（含类信息，多态序列化）。
     * <p>
     * 与 {@link #serialize(Object)} 的区别：本方法用 {@code kryo.writeClassAndObject} 写入对象，
     * <b>字节流自带对象的类信息</b>。适用于<b>反序列化端不知道具体类型</b>的场景
     * （如消息队列、RPC：生产端写入任意对象，消费端从字节还原出原始类型，无需传入 Class）。
     * <p>
     * 代价：相比 {@link #serialize(Object)} 多写一份类标识（未 {@code register} 的类写全限定类名，体积偏大）。
     * 若两端类型已知，应优先用 {@link #serialize(Object)}（不带类信息，更小）。
     * <p>
     * 路径走 {@link #serializePool}（成对 Kryo+Output），Output 池化复用。
     *
     * @param value 待序列化对象，为null时返回null
     * @return 序列化后的字节数组（含类信息）
     * @see #deserializeWithClass(byte[])
     * @see #serialize(Object)
     */
    public static byte[] serializeWithClass(Object value) {
        if (value == null) {
            return null;
        }
        byte[] data;
        final SerializeEntry entry = serializePool.obtain();
        try {
            entry.kryo().writeClassAndObject(entry.output(), value);
            data = entry.output().toBytes();
        } finally {
            entry.output().reset();
            serializePool.free(entry);
        }
        return data;
    }

    /**
     * 从字节数组反序列化为对象（含类信息，多态反序列化）。
     * <p>
     * 用 {@code kryo.readClassAndObject} 从字节中读取对象的类信息并还原，<b>返回 Object</b>，
     * 调用方按需强转。与 {@link #deserialize(byte[], Class)} 的区别：无需调用方提供目标 Class，
     * 类型由字节流自带的类信息决定。适用于消息队列、RPC 等反序列化端不知类型的场景。
     * <p>
     * 路径走 {@link #unserializePool}（Kryo），Input 每次 new。
     *
     * @param data 字节数组（由 {@link #serializeWithClass(Object)} 产出），为null或空时返回null
     * @return 反序列化得到的对象，类型由字节中的类信息决定
     * @see #serializeWithClass(Object)
     * @see #deserialize(byte[], Class)
     */
    public static Object deserializeWithClass(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        Object value;
        final Kryo kryo = unserializePool.obtain();
        final Input input = new Input(data);
        try {
            value = kryo.readClassAndObject(input);
        } finally {
            unserializePool.free(kryo);
        }
        return value;
    }

    /**
     * 从字节数组反序列化为指定类型的对象。
     *
     * @param data 字节数组，为null或空时返回null
     * @param cls  目标类型
     * @param <T>  目标类型
     * @return 反序列化得到的对象
     */
    public static <T> T deserialize(byte[] data, Class<T> cls) {
        if (data == null || data.length == 0) {
            return null;
        }
        T value;
        final Kryo kryo = unserializePool.obtain();
        final Input input = new Input(data);
        try {
            value = kryo.readObject(input, cls);
        } finally {
            unserializePool.free(kryo);
        }
        return value;
    }

    /**
     * 自定义写入并返回字节数组（完整版）。
     * <p>
     * 由调用方在 serializer 回调中自行决定写入哪些原语及顺序，实现完全自定义的序列化协议。
     * 回调同时接收 Kryo 与 Output，调用方既可写原语（writeLong/writeString等），
     * 也可调用 {@code kryo.writeObject(output, obj)} 做对象级序列化，二者可在同一会话内混用。
     * Kryo与Output均从池中获取，使用后自动归还。
     * <p>
     * <b>性能提示：</b>若回调内只写原语、不需要 {@code kryo.writeObject}，可使用轻量版
     * {@link #serialize(int, Consumer)}，省去 Kryo 池操作；但轻量版每次分配 buffer，需权衡 GC 成本。
     *
     * @param serializer 写入回调，接收 Kryo 与 Output
     * @return 序列化后的字节数组
     * @see #serialize(int, Consumer)
     */
    public static byte[] serialize(BiConsumer<Kryo, Output> serializer) {
        if (serializer == null) {
            return null;
        }
        final SerializeEntry entry = serializePool.obtain();
        try {
            serializer.accept(entry.kryo(), entry.output());
            return entry.output().toBytes();
        } finally {
            entry.output().reset();
            serializePool.free(entry);
        }
    }

    /**
     * 自定义写入并返回字节数组（轻量版，无池化，默认缓冲区）。
     * <p>
     * 仅向调用方暴露 Output，适用于只写原语（writeLong/writeString等）、不需要 kryo.writeObject 的场景。
     * 使用 {@link #DEFAULT_BUFFER_SIZE} 作为初始缓冲区。需精确控制大小时用 {@link #serialize(int, Consumer)}。
     *
     * @param serializer 写入回调，接收一个Output
     * @return 序列化后的字节数组
     * @see #serialize(int, Consumer)
     */
    public static byte[] serialize(Consumer<Output> serializer) {
        return serialize(DEFAULT_BUFFER_SIZE, serializer);
    }

    /**
     * 自定义写入并返回字节数组（轻量版，无池化，指定缓冲区大小）。
     * <p>
     * 仅向调用方暴露 Output，适用于只写原语（writeLong/writeString等）、不需要 kryo.writeObject 的场景。
     * 本方法不使用任何池，每次调用分配一个新的 Output。
     * <p>
     * <b>成本说明（重要）：</b>每次调用会产生两个短生命周期垃圾对象，由 young GC 回收：
     * <ul>
     *   <li>一个 {@code byte[bufferSize]} —— Output 的内部缓冲区，即本方法的 bufferSize 参数；</li>
     *   <li>一个 {@code byte[实际写入长度]} —— {@link Output#toBytes()} 返回的精确长度结果数组（这是 byte[] 契约下不可避免的）。</li>
     * </ul>
     * 以 QPS=3000、平均输出 1KB、bufferSize=1024 估算：约 6 MB/秒 垃圾（buffer + 结果数组各 3 MB/秒）。
     * 若 bufferSize 远大于实际输出（如开 2560 却只写 200 字节），buffer 垃圾会显著放大——
     * 故调用方应按预估输出上限紧凑设置 bufferSize，避免无谓放大 GC 压力。
     * <p>
     * 与 {@link #serialize(BiConsumer)} 的取舍：本方法省去 Kryo 池操作、吞吐更高且无池竞争，
     * 但代价是上述每次的 buffer 垃圾；适合纯原语写入、且能合理预估 bufferSize 的场景。
     * 若无法预估大小或对 GC 敏感，应改用 {@link #serialize(BiConsumer)}（Output 池化复用，无 buffer 垃圾）。
     *
     * @param bufferSize Output 初始缓冲区大小（字节），应按预估输出上限紧凑设置，过大会放大 GC 压力
     * @param serializer 写入回调，接收一个Output
     * @return 序列化后的字节数组
     * @see #serialize(BiConsumer)
     */
    public static byte[] serialize(int bufferSize, Consumer<Output> serializer) {
        if (serializer == null) {
            return null;
        }
        final Output output = new Output(bufferSize, -1);
        serializer.accept(output);
        return output.toBytes();
    }

    /**
     * 序列化实现了 {@link KryoData} 接口的对象（接口式手工序列化，默认缓冲区）。
     * <p>
     * 调用 {@code data.serialize(output)}，由对象自身负责写原语，本方法负责借出 Output 并 toBytes。
     * 使用 {@link #DEFAULT_BUFFER_SIZE} 作为初始缓冲区。需精确控制大小时用 {@link #serializeData(int, KryoData)}。
     *
     * @param data 实现 KryoData 的对象，为null时返回null
     * @return 序列化后的字节数组
     * @see #serializeData(int, KryoData)
     */
    public static byte[] serializeData(KryoData data) {
        return serializeData(DEFAULT_BUFFER_SIZE, data);
    }

    /**
     * 序列化实现了 {@link KryoData} 接口的对象（接口式手工序列化，指定缓冲区大小）。
     * <p>
     * 属于轻量无池路径（同 {@link #serialize(int, Consumer)}），每次分配一个新的 Output，
     * 多线程吞吐高但有 GC 成本（见 {@link #serialize(int, Consumer)} 的 GC 说明）。
     * <p>
     * 与 {@link #serialize(int, Consumer)} 的区别：后者用 lambda 回调，本方法用对象实现 KryoData 接口。
     * 适合"一个类固定一套读写逻辑、多处复用"的场景，把序列化逻辑内聚到数据类自身。
     *
     * @param bufferSize Output 初始缓冲区大小（字节）
     * @param data       实现 KryoData 的对象，为null时返回null
     * @return 序列化后的字节数组
     * @see #serializeData(KryoData)
     */
    public static byte[] serializeData(int bufferSize, KryoData data) {
        if (data == null) {
            return null;
        }
        final Output output = new Output(bufferSize, -1);
        data.serialize(output);
        return output.toBytes();
    }

    /**
     * 反序列化：将字节数组读入调用方提供的 {@link KryoData} 实例（接口式手工序列化）。
     * <p>
     * 调用方负责创建实例（{@code new MyData()}），本方法调用 {@code instance.deserialize(input)} 填充字段后返回该实例。
     * 属于轻量无池路径（同 {@link #deserialize(byte[], Function)}），每次 new Input。
     * <p>
     * <b>命名说明</b>：本方法不沿用 deserialize 重载，因为 {@code <T extends KryoData> T} 的泛型签名
     * 会与 {@link #deserialize(byte[], Function)} / {@link #deserialize(byte[], BiFunction)} 在重载解析时产生歧义。
     *
     * @param data     字节数组，为null或空时返回传入的 instance（未填充）
     * @param instance 调用方创建的空实例，由本方法填充
     * @param <T>      目标类型
     * @return 填充后的 instance（即传入的对象）
     * @see #serializeData(KryoData)
     */
    public static <T extends KryoData> T deserializeData(byte[] data, T instance) {
        if (data == null || data.length == 0 || instance == null) {
            return instance;
        }
        final Input input = new Input(data);
        instance.deserialize(input);
        return instance;
    }

    /**
     * 自定义读取（完整版）。
     * <p>
     * 由调用方在 deserializer 回调中按与写入时一致的顺序读取原语，实现完全自定义的反序列化协议。
     * 回调同时接收 Kryo 与 Input，调用方既可读原语（readLong/readString等），
     * 也可调用 {@code kryo.readObject(input, cls)} 做对象级反序列化，二者可在同一会话内混用。
     * Kryo从池中获取，使用后自动归还。
     * <p>
     * <b>性能提示：</b>本方法每次都会从池中获取并归还一个 Kryo 实例。若回调内只读原语、
     * 不需要 {@code kryo.readObject}，应优先使用轻量版 {@link #deserialize(byte[], Function)}，省去一次池操作。
     *
     * @param data        字节数组
     * @param deserializer 读取回调，接收 Kryo 与 Input
     * @param <T>         返回类型
     * @return 回调构造出的对象
     * @see #deserialize(byte[], Function)
     */
    public static <T> T deserialize(byte[] data, BiFunction<Kryo, Input, T> deserializer) {
        if (data == null || data.length == 0 || deserializer == null) {
            return null;
        }
        final Kryo kryo = unserializePool.obtain();
        final Input input = new Input(data);
        try {
            return deserializer.apply(kryo, input);
        } finally {
            unserializePool.free(kryo);
        }
    }

    /**
     * 自定义读取（轻量版，推荐优先使用）。
     * <p>
     * 仅向调用方暴露 Input，适用于只读原语（readLong/readString等）、不需要 kryo.readObject 的场景。
     * 相比完整版 {@link #deserialize(byte[], BiFunction)}，本方法不获取/归还 Kryo，省去一次池操作，开销更低。
     *
     * @param data        字节数组
     * @param deserializer 读取回调，接收一个Input
     * @param <T>         返回类型
     * @return 回调构造出的对象
     * @see #deserialize(byte[], BiFunction)
     */
    public static <T> T deserialize(byte[] data, Function<Input, T> deserializer) {
        if (data == null || data.length == 0 || deserializer == null) {
            return null;
        }
        final Input input = new Input(data);
        return deserializer.apply(input);
    }

    /**
     * 把泛型 Type 解析为可实例化的具体 Class。
     * <p>
     * 处理 Class、GenericArrayType、ParameterizedType、TypeVariable、WildcardType 等场景，
     * 对 List/Set/Map 等接口类型映射为对应的默认实现类（见 {@link #resolveConcreteClass(Class)}）。
     * 无法解析时回退为 Object.class。
     *
     * @param type 泛型类型
     * @return 对应的具体 Class
     */
    public static Class<?> type2Class(Type type) {
        if (type instanceof Class<?> cls) {
            return resolveConcreteClass(cls);
        } else if (type instanceof GenericArrayType) {
            // having to create an array instance to get the class is kinda nasty
            // but apparently this is a current limitation of java-reflection concerning array classes.
            return Array.newInstance(type2Class(((GenericArrayType) type).getGenericComponentType()), 0).getClass(); // E.g. T[] -> T -> Object.class if <T> or Number.class
        } else if (type instanceof ParameterizedType parameterizedType) {
            return resolveConcreteClass(type2Class(parameterizedType.getRawType()));
        } else if (type instanceof TypeVariable) {
            Type[] bounds = ((TypeVariable<?>) type).getBounds();
            return bounds.length == 0 ? Object.class : type2Class(bounds[0]); // erasure is to the left-most bound.
        } else if (type instanceof WildcardType) {
            Type[] bounds = ((WildcardType) type).getUpperBounds();
            return bounds.length == 0 ? Object.class : type2Class(bounds[0]); // erasure is to the left-most upper bound.
        } else {
            // throw new UnsupportedOperationException( "cannot handle type class: " + type.getClass() );
            return Object.class;
        }
    }

    /**
     * 将集合/映射的抽象类型映射为可实例化的具体实现类。
     * <p>
     * 例如 List → ArrayList、Map → HashMap、Set → HashSet 等，确保反射实例化时能成功。
     *
     * @param cls 输入类型
     * @return 可实例化的具体实现类
     */
    private static Class<?> resolveConcreteClass(Class<?> cls) {
        if (cls == List.class || cls == AbstractList.class) {
            return ArrayList.class;
        }
        if (cls == Set.class || cls == AbstractSet.class) {
            return HashSet.class;
        }
        if (cls == SortedSet.class || cls == NavigableSet.class) {
            return TreeSet.class;
        }
        if (cls == Map.class || cls == AbstractMap.class) {
            return HashMap.class;
        }
        if (cls == SortedMap.class || cls == NavigableMap.class) {
            return TreeMap.class;
        }
        if (cls == Deque.class || cls == Queue.class) {
            return LinkedList.class;
        }
        return cls;
    }

    /**
     * 基于无锁 {@link MpmcArrayQueue} 的对象池。
     * <p>
     * 与 kryo 官方 {@code Pool(threadSafe=true)}（内部为 {@code LinkedBlockingQueue}，有锁）相比，
     * 本类底层为无锁 CAS 队列，高并发下 obtain/free 吞吐显著更高（实测 20 线程下 2~3 倍）。
     * <p>
     * 容量策略：
     * <ul>
     *     <li>{@code minCapacity}：稳态保留的最小对象数，shrink 不会低于此值；</li>
     *     <li>{@code maxCapacity}：峰值允许累积的最大对象数（即队列容量），free 时若池已满则直接丢弃（由 GC 回收）。</li>
     * </ul>
     * {@link #shrinkToMin()} 直接从队列 poll 丢弃多余对象，把空闲数压向 minCapacity——无需替换队列
     * （队列容量只是逻辑上限，真正占内存的是池内的对象）。
     * <p>
     * 后台缩容：所有 JcPool 实例注册到一个共享列表，由单个 daemon 线程按固定间隔（{@code SHRINK_INTERVAL_MILLIS}）遍历执行 shrink。
     * 用裸 {@code Thread + Thread.sleep} 循环而非 {@code ScheduledExecutorService}，因为这是低频（分钟级）任务，
     * 调度器的 DelayedTaskQueue 堆操作与锁对低频任务是纯开销。
     * <p>
     * <b>对象状态清理</b>：本池不在 free 时自动 reset 对象，调用方需自行处理（如归还 Output 前调用 {@code output.reset()} 清零 position）。
     * 线程安全：obtain/free/shrink 均可并发。注意池化的对象本身（如 {@link Kryo}）非线程安全，
     * 由调用方保证同一对象同一时刻只被一个线程使用。
     *
     * @param <T> 池化对象类型
     */
    private abstract static class JcPool<T> {

        /**
         * 池内对象最小数量。
         */
        private final int minCapacity;
        /**
         * 池内对象最大数量。
         */
        private final int maxCapacity;
        /**
         * 无锁队列。final字段，obtain/free 直接访问，无 volatile/间接寻址开销。
         * shrink 不替换它，只 poll 丢弃多余对象。
         */
        private final MpmcArrayQueue<T> queue;
        /**
         * 观测窗口内 obtain 是否扑空过（池空需要create）。
         * <p>
         * 用于shrink门控：扑空过说明负载接近或超过池容量，峰值遗留对象有复用价值，本轮不回收。
         * 只需"是否扑空过"这一个事实，无需扑空次数，故用boolean而非计数。
         * 写在多线程obtain的扑空分支（相对低频），读在单线程shrink；用volatile保证可见性，开销可忽略。
         */
        private volatile boolean missed = false;
        /**
         * 每次shrink最多回收的空闲对象比例（相对当前空闲数）。
         */
        private static final double SHRINK_RATIO = 0.05;
        /**
         * 每次shrink至少回收的对象数（不足此数则不回收，避免无意义的微量操作）。
         */
        private static final int SHRINK_MIN_STEP = 1;

        /**
         * 所有 JcPool 实例。shrink 守护线程遍历此列表。
         * 使用 CopyOnWriteArrayList：注册发生在 JcPool 构造时（极少），遍历发生在后台线程（周期性），写远少于读。
         */
        private static final CopyOnWriteArrayList<JcPool<?>> SHRINK_TARGETS = new CopyOnWriteArrayList<>();

        static {
            // 单个 daemon 守护线程，周期性对所有注册的池执行 shrinkToMin。
            // 不用 ScheduledExecutorService：低频任务下裸线程 + sleep 开销最小。
            Thread shrinker = new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(SHRINK_INTERVAL_MILLIS);
                    } catch (InterruptedException e) {
                        // 被中断即退出
                        Thread.currentThread().interrupt();
                        return;
                    }
                    for (JcPool<?> pool : SHRINK_TARGETS) {
                        try {
                            pool.shrinkToMin();
                        } catch (Throwable e) {
                            log.warn("Pool shrink failed: {}", e.getMessage(), e);
                        }
                    }
                }
            }, "KryoUtils-PoolShrinker");
            shrinker.setDaemon(true);
            shrinker.setPriority(Thread.MIN_PRIORITY);
            shrinker.start();
        }

        /**
         * @param minCapacity 稳态最小容量（shrink 下限）
         * @param maxCapacity 峰值最大容量（队列容量，free 上限）
         */
        private JcPool(int minCapacity, int maxCapacity) {
            if (minCapacity < 0 || maxCapacity <= 0 || minCapacity > maxCapacity) {
                throw new IllegalArgumentException("illegal capacity: min=" + minCapacity + ", max=" + maxCapacity);
            }
            this.minCapacity = minCapacity;
            this.maxCapacity = maxCapacity;
            this.queue = new MpmcArrayQueue<>(maxCapacity);
            SHRINK_TARGETS.add(this);
        }

        /**
         * 创建新对象。由子类实现。
         * <p>
         * 注：abstract方法无法用private（Java禁止private abstract），故保留protected。
         *
         * @return 新对象
         */
        protected abstract T create();

        /**
         * 获取一个对象。池空则 {@link #create()} 新建。
         * <p>
         * 扑空（池空）时会置 {@code missed=true}，作为shrink门控信号：
         * 观测窗口内扑空过说明负载吃紧，shrink不会回收峰值遗留对象。
         *
         * @return 对象（可能新建）
         */
        public T obtain() {
            T obj = queue.relaxedPoll();
            if (obj == null) {
                missed = true;
                return create();
            }
            return obj;
        }

        /**
         * 归还对象到池。池已满则丢弃（由 GC 回收）。
         * <p>
         * 注意：本方法不负责清理对象状态。若对象需要重置（如 Output 清零 position），
         * 调用方须在 free 之前自行处理，否则下次复用可能读到上次残留的脏数据。
         *
         * @param object 对象，不能为null
         */
        public void free(T object) {
            if (object == null) {
                throw new IllegalArgumentException("object cannot be null.");
            }
            queue.relaxedOffer(object);
        }

        /**
         * 渐进式回收峰值遗留的冷对象，逐步把空闲数压向 minCapacity。
         * <p>
         * 算法：
         * <ol>
         *   <li><b>扑空门控</b>：若观测窗口内 obtain 扑空过（missed=true），说明负载接近或超过池容量，
         *       峰值遗留对象有复用价值，本轮不回收；</li>
         *   <li><b>渐进回收</b>：未扑空时，本轮只回收 当前空闲数×{@link #SHRINK_RATIO}（向下取整，至少{@link #SHRINK_MIN_STEP}）个对象，
         *       而非一次性清空——让池逐步收敛到稳态，避免清过头引发下一波 create 抖动；</li>
         *   <li><b>min下限</b>：回收后空闲数不低于 minCapacity，到达即停。</li>
         * </ol>
         * 每轮执行后清零 missed，开启新一轮观测窗口。
         * 线程安全，可与 obtain/free 并发。
         */
        public void shrinkToMin() {
            // 无论本轮是否回收，都重置观测窗口，用最新一轮的扑空情况决策
            if (missed) {
                missed = false;
                // 观测窗口内扑空过，负载吃紧，不动池
                return;
            }
            int currentSize = queue.size();
            // 本轮回收量 = 当前空闲数的 SHRINK_RATIO，但不超过"降到minCapacity"的总量
            int maxRemovable = currentSize - minCapacity;
            if (maxRemovable <= 0) {
                return; // 已在min或更低
            }
            int step = (int) (currentSize * SHRINK_RATIO);
            if (step < SHRINK_MIN_STEP) {
                step = SHRINK_MIN_STEP;
            }
            if (step > maxRemovable) {
                step = maxRemovable;
            }
            for (int i = 0; i < step; i++) {
                if (queue.relaxedPoll() == null) {
                    break; // 队列已空（与obtain并发）
                }
            }
        }

        /**
         * 清空池中所有对象。
         */
        public void clear() {
            queue.clear();
        }

        /**
         * 当前可被 obtain 的对象数。
         *
         * @return 空闲对象数
         */
        public int getFree() {
            return queue.size();
        }

        /**
         * 队列容量（固定为 maxCapacity）。
         *
         * @return 容量
         */
        public int capacity() {
            return queue.capacity();
        }

        /**
         * 最小容量。
         *
         * @return minCapacity
         */
        public int getMinCapacity() {
            return minCapacity;
        }

        /**
         * 最大容量。
         *
         * @return maxCapacity
         */
        public int getMaxCapacity() {
            return maxCapacity;
        }

    }

}
