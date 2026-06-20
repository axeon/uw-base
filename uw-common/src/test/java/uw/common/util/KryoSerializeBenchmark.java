package uw.common.util;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 四种 kryo 序列化方式性能基准（1/20/50线程）：
 * <ol>
 *   <li><b>对象读写</b>（objSerialize/objDeserialize）：{@link KryoUtils#serialize(Object)} 反射读写 POJO；</li>
 *   <li><b>手工读写-lambda</b>（manualSerialize/manualDeserialize）：{@link KryoAuthTokenSerializer} 手工原语读写，
 *       走 {@link KryoUtils#serialize(int, java.util.function.Consumer)}（new Output，无池）；</li>
 *   <li><b>KryoSerializable</b>（ksSerialize/ksDeserialize）：对象实现 KryoSerializable，
 *       用 {@link KryoUtils#serialize(Object)}，由 kryo 回调对象自身 write/read（走池）；</li>
 *   <li><b>KryoData 接口式</b>（kdSerialize/kdDeserialize）：对象实现 {@link uw.common.data.KryoData}，
 *       走 {@link KryoUtils#serializeData(KryoData)} / {@link KryoUtils#deserializeData(byte[], uw.common.data.KryoData)}（new Output/Input，无池）。</li>
 * </ol>
 * <p>
 * <b>数据集</b>：预生成 {@link #DATASET_SIZE} 条内容随机的 token，benchmark 轮询取用（每线程独立计数器，无竞争）。
 * <b>必须用随机数据</b>——单份常量数据会让 JIT 做常量传播/标量替换，虚高单线程吞吐。
 * 反序列化消费多个字段（userId+userName+permSet+configMap），防 JIT 判定"只用了部分字段"而消除其余写入。
 * <p>
 * 运行方式：执行 main，依次以 1/20/50 线程运行。
 *
 * <h3>实测结果（JDK 25.0.1、JMH 1.37、单机，Throughput ops/ms，200条随机数据）</h3>
 * <pre>
 * Benchmark               线程=1     线程=20      线程=50
 * objSerialize            1500       3369         2166      // 反射写（走池）
 * objDeserialize          1595       3480         2646      // 反射读（走池）
 * manualSerialize         2512       23664        21383     // 手工写-lambda（new Output，无池）
 * manualDeserialize       2094       15360        16015     // 手工读-lambda（new Input，无池）
 * ksSerialize             3033       2590         1580      // KryoSerializable写（走serializePool）
 * ksDeserialize           2045       3340         2561      // KryoSerializable读（走unserializePool）
 * kdSerialize             1950       14681        14416     // KryoData写（new Output，无池）
 * kdDeserialize           2038       16868        16061     // KryoData读（new Input，无池）
 * </pre>
 *
 * <h3>结论</h3>
 * <ul>
 *   <li><b>体积</b>（见 {@link KryoSerializeSizeTest}）：三种手工方式（manual/ks/kd）均 185 字节，反射 188 字节。</li>
 *   <li><b>单线程</b>：手工类（manual/ks/kd，1950~3033）显著快于反射（obj，1500~1595）。
 *       manual(ks/kd) 走无池 new Output，ks 走池，单线程下池无竞争、差异在 JIT 优化波动内。</li>
 *   <li><b>多线程-无池方案（manual/kd）碾压走池方案（obj/ks）</b>：
 *       manual/kd 走 new Output/Input，<b>完全不碰共享池，每线程独立分配、零竞争</b>，多核线性扩展，吞吐 1.4万~2.4万；
 *       obj/ks 走 serializePool/unserializePool（共享池），受池竞争限制，吞吐被压在 2000~3500。
 *       <b>无池方案是走池方案的 5~10 倍</b>。</li>
 *   <li><b>manual vs kd</b>：两者都是无池纯原语路径，kd（接口式）略低于 manual（lambda），因 kd 多一次接口方法分发
 *       （对象自身的 serialize/deserialize 方法调用）。差异在 5%~15%，权衡"逻辑内聚到数据类"的代码组织收益。</li>
 *   <li><b>ks（KryoSerializable）虽是手工原语但走池</b>：单线程快（无竞争），多线程被 serializePool/unserializePool 拖累。
 *       若要 KryoSerializable 的多线程吞吐，需配合 ThreadLocal 池（但虚拟线程不兼容，见 {@link KryoUtils} 类 javadoc）。</li>
 *   <li><b>无池高吞吐是真实的</b>（非 JIT 虚高）：已用随机数据 + 多字段消费验证。
 *       代价是每次 new 产生垃圾，属"用 GC 换无锁并行"，高并发下吞吐收益远超 GC 代价（见 {@link KryoUtils#serialize(int, java.util.function.Consumer)}）。</li>
 * </ul>
 *
 * <h3>与"池方案对比"的关系</h3>
 * 本 benchmark 对比<b>序列化方式</b>（对象/手工lambda/KryoSerializable/KryoData）。
 * 另一维度是<b>池实现</b>（JCTools / kryo官方Pool / ThreadLocal）的对比，
 * 结论（ThreadLocal 吞吐最优但不兼容虚拟线程，最终用 JCTools 池）已固化到 {@link KryoUtils} 类 javadoc，
 * 临时对比类（KryoJcUtils/KryoPoolUtils/KryoThreadUtils/KryoPoolBenchmark）已删除。
 *
 * @author axeon
 */
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Threads(1)
@State(Scope.Benchmark)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class KryoSerializeBenchmark {

    /** 随机数据集大小。 */
    private static final int DATASET_SIZE = 200;

    private AuthTokenData[] dataset;
    private AuthTokenData2[] dataset2;
    private AuthTokenData3[] dataset3;
    private byte[][] objBytesSet;
    private byte[][] manualBytesSet;
    private byte[][] ksBytesSet;
    private byte[][] kdBytesSet;

    /** 每线程独立的轮询计数器（普通int，无竞争），用于从数据集取下一条数据。 */
    @State(Scope.Thread)
    public static class ThreadIdx {
        private int idx = 0;

        int next(int size) {
            int i = idx;
            if (++idx >= size) {
                idx = 0;
            }
            return i;
        }
    }

    @Setup(Level.Trial)
    public void setup() {
        Random rnd = new Random(0x5EED);
        dataset = new AuthTokenData[DATASET_SIZE];
        dataset2 = new AuthTokenData2[DATASET_SIZE];
        dataset3 = new AuthTokenData3[DATASET_SIZE];
        objBytesSet = new byte[DATASET_SIZE][];
        manualBytesSet = new byte[DATASET_SIZE][];
        ksBytesSet = new byte[DATASET_SIZE][];
        kdBytesSet = new byte[DATASET_SIZE][];
        for (int i = 0; i < DATASET_SIZE; i++) {
            dataset[i] = randomToken(rnd, i);
            dataset2[i] = randomToken2(dataset[i]);
            dataset3[i] = randomToken3(dataset[i]);
            objBytesSet[i] = KryoUtils.serialize(dataset[i]);
            manualBytesSet[i] = KryoAuthTokenSerializer.serialize(dataset[i]);
            ksBytesSet[i] = KryoUtils.serialize(dataset2[i]);
            kdBytesSet[i] = KryoUtils.serializeData(dataset3[i]);
        }
    }

    /**
     * 按与 {@link #randomToken} 完全相同的字段值生成 AuthTokenData2（保证三种方式输入一致）。
     */
    private static AuthTokenData2 randomToken2(AuthTokenData src) {
        AuthTokenData2 d = new AuthTokenData2();
        d.tokenType = src.tokenType;
        d.saasId = src.saasId;
        d.userType = src.userType;
        d.userId = src.userId;
        d.mchId = src.mchId;
        d.groupId = src.groupId;
        d.isMaster = src.isMaster;
        d.userGrade = src.userGrade;
        d.userName = src.userName;
        d.nickName = src.nickName;
        d.realName = src.realName;
        d.mobile = src.mobile;
        d.email = src.email;
        d.userIp = src.userIp;
        d.createAt = src.createAt;
        d.expireAt = src.expireAt;
        d.permSet = src.permSet;
        d.configMap = src.configMap;
        return d;
    }

    /**
     * 按与 {@link #randomToken} 完全相同的字段值生成 AuthTokenData3（保证四种方式输入一致）。
     */
    private static AuthTokenData3 randomToken3(AuthTokenData src) {
        AuthTokenData3 d = new AuthTokenData3();
        d.tokenType = src.tokenType;
        d.saasId = src.saasId;
        d.userType = src.userType;
        d.userId = src.userId;
        d.mchId = src.mchId;
        d.groupId = src.groupId;
        d.isMaster = src.isMaster;
        d.userGrade = src.userGrade;
        d.userName = src.userName;
        d.nickName = src.nickName;
        d.realName = src.realName;
        d.mobile = src.mobile;
        d.email = src.email;
        d.userIp = src.userIp;
        d.createAt = src.createAt;
        d.expireAt = src.expireAt;
        d.permSet = src.permSet;
        d.configMap = src.configMap;
        return d;
    }
    private static AuthTokenData randomToken(Random rnd, int i) {
        AuthTokenData d = new AuthTokenData();
        d.tokenType = rnd.nextInt(4);
        d.saasId = 1000L + rnd.nextInt(100000);
        d.userType = rnd.nextInt(4);
        d.userId = 1_000_000_000L + rnd.nextInt(1_000_000_000) + i;
        d.mchId = rnd.nextInt(10000);
        d.groupId = rnd.nextInt(1000);
        d.isMaster = rnd.nextInt(2);
        d.userGrade = rnd.nextInt(10);
        d.userName = "user" + i + "_" + rnd.nextInt(99999);
        d.nickName = "nick" + rnd.nextInt(99999);
        d.realName = "real" + rnd.nextInt(99999);
        d.mobile = "1" + (Math.abs(rnd.nextLong()) % 9000000000L + 1000000000L);
        d.email = "mail" + rnd.nextInt(99999) + "@example.com";
        d.userIp = (1 + rnd.nextInt(223)) + "." + rnd.nextInt(256) + "." + rnd.nextInt(256) + "." + rnd.nextInt(256);
        d.createAt = 1700000000000L + rnd.nextInt(1_000_000_000);
        d.expireAt = d.createAt + 3600_000L;
        int permCount = 5 + rnd.nextInt(30);
        java.util.HashSet<Integer> perm = new java.util.HashSet<>(permCount);
        for (int j = 0; j < permCount; j++) {
            perm.add(1000 + rnd.nextInt(5000));
        }
        d.permSet = perm;
        int cfgCount = rnd.nextInt(5);
        java.util.HashMap<String, String> cfg = new java.util.HashMap<>(cfgCount);
        for (int j = 0; j < cfgCount; j++) {
            cfg.put("k" + rnd.nextInt(999), "v" + rnd.nextInt(999));
        }
        d.configMap = cfg;
        return d;
    }

    // ===== 方式一：对象读写（反射） =====

    @Benchmark
    public void objSerialize(ThreadIdx idx, Blackhole bh) {
        bh.consume(KryoUtils.serialize(dataset[idx.next(DATASET_SIZE)]).length);
    }

    @Benchmark
    public void objDeserialize(ThreadIdx idx, Blackhole bh) {
        AuthTokenData r = KryoUtils.deserialize(objBytesSet[idx.next(DATASET_SIZE)], AuthTokenData.class);
        bh.consume(r.userId);
        bh.consume(r.userName);
        bh.consume(r.permSet);
        bh.consume(r.configMap);
    }

    // ===== 方式二：手工读写 =====

    @Benchmark
    public void manualSerialize(ThreadIdx idx, Blackhole bh) {
        AuthTokenData d = dataset[idx.next(DATASET_SIZE)];
        bh.consume(KryoAuthTokenSerializer.serialize(d).length);
    }

    @Benchmark
    public void manualDeserialize(ThreadIdx idx, Blackhole bh) {
        AuthTokenData r = KryoAuthTokenSerializer.deserialize(manualBytesSet[idx.next(DATASET_SIZE)]);
        bh.consume(r.userId);
        bh.consume(r.userName);
        bh.consume(r.permSet);
        bh.consume(r.configMap);
    }

    // ===== 方式三：KryoSerializable =====

    @Benchmark
    public void ksSerialize(ThreadIdx idx, Blackhole bh) {
        bh.consume(KryoUtils.serialize(dataset2[idx.next(DATASET_SIZE)]).length);
    }

    @Benchmark
    public void ksDeserialize(ThreadIdx idx, Blackhole bh) {
        AuthTokenData2 r = KryoUtils.deserialize(ksBytesSet[idx.next(DATASET_SIZE)], AuthTokenData2.class);
        bh.consume(r.userId);
        bh.consume(r.userName);
        bh.consume(r.permSet);
        bh.consume(r.configMap);
    }

    // ===== 方式四：KryoData 接口式手工读写（无池，serializeData/deserializeData） =====

    @Benchmark
    public void kdSerialize(ThreadIdx idx, Blackhole bh) {
        bh.consume(KryoUtils.serializeData(dataset3[idx.next(DATASET_SIZE)]).length);
    }

    @Benchmark
    public void kdDeserialize(ThreadIdx idx, Blackhole bh) {
        AuthTokenData3 r = KryoUtils.deserializeData(kdBytesSet[idx.next(DATASET_SIZE)], new AuthTokenData3());
        bh.consume(r.userId);
        bh.consume(r.userName);
        bh.consume(r.permSet);
        bh.consume(r.configMap);
    }

    public static void main(String[] args) throws RunnerException {
        // 四种序列化方式 × serialize/deserialize × 1/20/50线程。
        for (int threads : new int[]{1, 20, 50}) {
            System.out.println("\n==================== threads = " + threads + " ====================");
            Options opt = new OptionsBuilder()
                    .include(KryoSerializeBenchmark.class.getSimpleName())
                    .threads(threads)
                    .build();
            new Runner(opt).run();
        }
    }
}
