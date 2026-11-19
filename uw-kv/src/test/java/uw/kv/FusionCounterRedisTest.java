package uw.kv;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import uw.kv.util.RedisKeyUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FusionCounter/GlobalCounter 本轮改造的真实 Redis 集成验证（见 test/resources/application.yml）。
 * <p>覆盖：</p>
 * <ol>
 *   <li>Redis 版本与 INCREX 可用性探针 + 裸 dispatch 返回形态（离线无法验证的发布检查项）；</li>
 *   <li>{@link GlobalCounter#incrementTtl}（PX 滑动TTL）/ {@link GlobalCounter#incrementExpireAt}（PXAT 固定时刻）
 *       的返回值、TTL 生效、幂等重设与过期回收；</li>
 *   <li>{@link FusionCounter} 本地聚合 → forceSync 强一致读（含无增量拉全局）→ writeBack 回调触发；</li>
 *   <li>keyExpireAtResolver 三级分发路径；</li>
 *   <li>setIfAbsent 基线 / init / delete。</li>
 * </ol>
 * <p>每个 case 用独立 counterType（时间戳后缀）并在 @AfterEach 清理，不污染共享 key 空间。</p>
 */
@SpringBootTest(classes = FusionCounterRedisTest.TestApp.class)
class FusionCounterRedisTest {

    /**
     * 最小启动配置：仅触发自动配置（加载 UwCacheAutoConfiguration），不做组件扫描。
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApp {
    }

    /**
     * 本轮测试的 counterType 前缀（唯一化，防共享环境冲突）。
     */
    private static final String TYPE = "TestFusionCounter" + System.currentTimeMillis();

    private static final String TYPE_TTL = TYPE + "Ttl";
    private static final String TYPE_PXAT = TYPE + "Pxat";
    private static final String TYPE_FUSION = TYPE + "Fusion";
    private static final String TYPE_RESOLVER = TYPE + "Resolver";
    private static final String TYPE_INIT = TYPE + "Init";

    @Autowired
    @Qualifier("longCacheRedisTemplate")
    private RedisTemplate<String, Long> longCacheRedisTemplate;

    @AfterEach
    void cleanUp() {
        for (String type : new String[]{TYPE_TTL, TYPE_PXAT, TYPE_FUSION, TYPE_RESOLVER, TYPE_INIT}) {
            for (String id : new String[]{"p1", "p2", "p3", "p4", "f1", "f2", "r1", "i1", "i2"}) {
                GlobalCounter.delete(type, id);
            }
        }
    }

    /**
     * 探针1：Redis 版本须 >= 8.8（INCREX 硬门槛）。
     * 低版本直接给出明确失败原因（而非后续 case 的 unknown command 模糊错误）。
     */
    @Test
    void redisVersionProbe() {
        Properties info = longCacheRedisTemplate.getRequiredConnectionFactory().getConnection().serverCommands().info("Server");
        String version = info.getProperty("redis_version");
        System.out.println("[PROBE] redis_version = " + version);
        assertThat(version).isNotNull();
        String[] parts = version.split("[.-]");
        int major = Integer.parseInt(parts[0]);
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        assertThat(major > 8 || (major == 8 && minor >= 8))
                .as("INCREX 需要 Redis Open Source 8.8+，当前 %s（升级测试环境后重跑本测试）", version)
                .isTrue();
    }

    /**
     * 探针2（发布检查项）：INCREX 经 Lettuce 原生 dispatch + LongArrayOutput 的返回解析。
     * 历史教训：Spring 的 connection.execute 对未收录新命令用 ByteArrayOutput，遇 integer 数组
     * 直接抛 UnsupportedOperationException（服务端已执行）——已改为原生 dispatch 自定义 output。
     * 本探针断言增量累计与 TTL 双正确（解析失败会走 GET 兜底,值对但每次多一次 RTT,看日志可辨）。
     */
    @Test
    void increxRawReturnProbe() {
        long v1 = GlobalCounter.incrementTtl(TYPE_TTL, "p1", 5, 60000);
        System.out.println("[PROBE] incrementTtl(+5) return = " + v1 + " (expect 5)");
        assertThat(v1).isEqualTo(5L);
        long v2 = GlobalCounter.incrementTtl(TYPE_TTL, "p1", 3, 60000);
        System.out.println("[PROBE] incrementTtl(+3) return = " + v2 + " (expect 8)");
        assertThat(v2).isEqualTo(8L);
        Long ttl = longCacheRedisTemplate.getExpire(RedisKeyUtils.buildTypeId("uw-counter:", TYPE_TTL, "p1"));
        System.out.println("[PROBE] p1 ttl(sec) = " + ttl);
        assertThat(ttl).isBetween(30L, 60L);
    }

    /**
     * PX 滑动TTL：返回值正确、TTL 生效、每次重设。
     */
    @Test
    void incrementTtlSetsAndRefreshesTtl() {
        long v1 = GlobalCounter.incrementTtl(TYPE_TTL, "p2", 10, 60000);
        assertThat(v1).isEqualTo(10L);
        String key = RedisKeyUtils.buildTypeId("uw-counter:", TYPE_TTL, "p2");
        Long ttlSec = longCacheRedisTemplate.getExpire(key);
        System.out.println("[TTL] px ttl(sec) = " + ttlSec);
        assertThat(ttlSec).isBetween(30L, 60L);
        long v2 = GlobalCounter.incrementTtl(TYPE_TTL, "p2", 5, 60000);
        assertThat(v2).isEqualTo(15L);
        assertThat(GlobalCounter.get(TYPE_TTL, "p2")).isEqualTo(15L);
    }

    /**
     * PXAT 固定时刻：幂等重设（TTL 不随增量延长）、过期时间戳到达后 key 回收清零。
     */
    @Test
    void incrementExpireAtFixedTimestampAndRecycle() throws InterruptedException {
        long expireAt = System.currentTimeMillis() + 60_000L;
        assertThat(GlobalCounter.incrementExpireAt(TYPE_PXAT, "p3", 7, expireAt)).isEqualTo(7L);
        String key = RedisKeyUtils.buildTypeId("uw-counter:", TYPE_PXAT, "p3");
        Long ttl1 = longCacheRedisTemplate.getExpire(key);
        System.out.println("[PXAT] ttl after first incr(sec) = " + ttl1);
        assertThat(ttl1).isBetween(45L, 60L);
        //幂等重设:同 expireAt 再次增量,TTL 不延长(仍指向固定时刻)
        assertThat(GlobalCounter.incrementExpireAt(TYPE_PXAT, "p3", 3, expireAt)).isEqualTo(10L);
        Long ttl2 = longCacheRedisTemplate.getExpire(key);
        assertThat(ttl2).isLessThanOrEqualTo(ttl1 + 1L);
        assertThat(GlobalCounter.get(TYPE_PXAT, "p3")).isEqualTo(10L);
        //过期回收:设 2s 后过期,sleep 后计数清零(窗口过期=计数作废语义)
        long shortExpireAt = System.currentTimeMillis() + 2_000L;
        assertThat(GlobalCounter.incrementExpireAt(TYPE_PXAT, "p3", 5, shortExpireAt)).isEqualTo(15L);
        Thread.sleep(2600L);
        assertThat(GlobalCounter.get(TYPE_PXAT, "p3")).isZero();
    }

    /**
     * FusionCounter 本地聚合:未到同步间隔不推 Redis,forceSync 推送且无增量时拉全局。
     */
    @Test
    void localAggregateAndForceSyncStrongRead() {
        //同步间隔 60s:测试内不自动同步,由 forceSync 主动推
        FusionCounter.config(TYPE_FUSION, 60_000L, 60_000L, null);
        FusionCounter.increment(TYPE_FUSION, "f1", 10);
        FusionCounter.increment(TYPE_FUSION, "f1", 20);
        FusionCounter.increment(TYPE_FUSION, "f1", 30);
        //本地聚合未同步:Redis 无值
        assertThat(GlobalCounter.get(TYPE_FUSION, "f1")).isZero();
        //forceSync 推增量,返回全局现值
        assertThat(FusionCounter.get(TYPE_FUSION, "f1", true)).isEqualTo(60L);
        assertThat(GlobalCounter.get(TYPE_FUSION, "f1")).isEqualTo(60L);
        //模拟其他实例直接向 Redis 加 50,本地无增量:forceSync 应拉全局(P1-1 修复验证)
        GlobalCounter.increment(TYPE_FUSION, "f1", 50);
        assertThat(FusionCounter.get(TYPE_FUSION, "f1", true)).isEqualTo(110L);
    }

    /**
     * writeBack 回调在首次同步时触发(新实例 lastWriteBackTime=0),回写值为全局现值快照。
     */
    @Test
    void writeBackCallbackFiredOnFirstSync() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Long> writeBackNum = new AtomicReference<>();
        FusionCounter.config(TYPE_FUSION + "Wb", 60_000L, 60_000L, (id, num) -> {
            writeBackNum.set(num);
            latch.countDown();
        });
        FusionCounter.increment(TYPE_FUSION + "Wb", "f2", 42);
        FusionCounter.get(TYPE_FUSION + "Wb", "f2", true);
        assertThat(latch.await(5, TimeUnit.SECONDS)).as("writeBack 回调应在首次同步后 5s 内触发").isTrue();
        assertThat(writeBackNum.get()).isEqualTo(42L);
        GlobalCounter.delete(TYPE_FUSION + "Wb", "f2");
    }

    /**
     * keyExpireAtResolver 分发路径:resolver 返回的固定时刻写入 key TTL。
     */
    @Test
    void keyExpireAtResolverPath() {
        long expireAt = System.currentTimeMillis() + 90_000L;
        FusionCounter.config(TYPE_RESOLVER, 60_000L, 60_000L, null, counterId -> expireAt);
        FusionCounter.increment(TYPE_RESOLVER, "r1", 11);
        assertThat(FusionCounter.get(TYPE_RESOLVER, "r1", true)).isEqualTo(11L);
        String key = RedisKeyUtils.buildTypeId("uw-counter:", TYPE_RESOLVER, "r1");
        Long ttl = longCacheRedisTemplate.getExpire(key);
        System.out.println("[RESOLVER] ttl(sec) = " + ttl);
        assertThat(ttl).isBetween(60L, 90L);
        //resolver 返回 0 降级常驻:无 TTL(打印形态,持久 key 的 Duration 表现为负或零)
        FusionCounter.config(TYPE_RESOLVER + "0", 60_000L, 60_000L, null, counterId -> 0L);
        FusionCounter.increment(TYPE_RESOLVER + "0", "r1", 5);
        FusionCounter.get(TYPE_RESOLVER + "0", "r1", true);
        Long noTtl = longCacheRedisTemplate.getExpire(RedisKeyUtils.buildTypeId("uw-counter:", TYPE_RESOLVER + "0", "r1"));
        System.out.println("[RESOLVER] 降级常驻 key 的 getExpire(sec) = " + noTtl);
        assertThat(noTtl).as("降级常驻 key 应无 TTL(-1=persist)").isEqualTo(-1L);
        GlobalCounter.delete(TYPE_RESOLVER + "0", "r1");
    }

    /**
     * setIfAbsent 基线 / init / delete 行为回归。
     */
    @Test
    void initBaselineAndDelete() {
        assertThat(GlobalCounter.setIfAbsent(TYPE_INIT, "i1", 100)).isTrue();
        assertThat(GlobalCounter.setIfAbsent(TYPE_INIT, "i1", 200)).isFalse();
        assertThat(GlobalCounter.get(TYPE_INIT, "i1")).isEqualTo(100L);
        //init:Redis 有值不覆盖,本地基线取热值
        FusionCounter.LocalCounter local = FusionCounter.init(TYPE_INIT, "i1", 300);
        assertThat(local.get()).isEqualTo(100L);
        //delete:本地与 Redis 同删
        FusionCounter.increment(TYPE_INIT, "i1", 1);
        assertThat(FusionCounter.delete(TYPE_INIT, "i1")).isTrue();
        assertThat(GlobalCounter.get(TYPE_INIT, "i1")).isZero();
        //delete 前增量已被 sync(true) 冲上——key 又被删除,终值归零
        assertThat(FusionCounter.get(TYPE_INIT, "i1", true)).isZero();
    }

    /**
     * unknown command 特征识别：只认服务端命令不存在错误，连接类故障不触发降级。
     */
    @Test
    void unknownCommandErrorDetection() {
        io.lettuce.core.RedisCommandExecutionException cause =
                new io.lettuce.core.RedisCommandExecutionException("ERR unknown command 'INCREX', with args beginning with:");
        IllegalStateException wrapped = new IllegalStateException("INCREX执行失败", cause);
        assertThat(GlobalCounter.isUnknownCommandError(wrapped)).isTrue();
        assertThat(GlobalCounter.isUnknownCommandError(cause)).isTrue();
        assertThat(GlobalCounter.isUnknownCommandError(new IllegalStateException("connection reset"))).isFalse();
        assertThat(GlobalCounter.isUnknownCommandError(null)).isFalse();
    }

    /**
     * INCREX 不可用时的降级路径：计数走裸 INCRBY（值正确），key 无 TTL。
     */
    @Test
    void degradeToIncrementWhenIncrexUnavailable() {
        GlobalCounter.markIncrexUnavailableForTest();
        try {
            long v = GlobalCounter.incrementTtl(TYPE_TTL, "p4", 7, 60000);
            assertThat(v).isEqualTo(7L);
            Long ttl = longCacheRedisTemplate.getExpire(RedisKeyUtils.buildTypeId("uw-counter:", TYPE_TTL, "p4"));
            System.out.println("[DEGRADE] 降级 key ttl(sec) = " + ttl + " (expect -1)");
            assertThat(ttl).isEqualTo(-1L);
        } finally {
            GlobalCounter.resetIncrexUnavailableForTest();
        }
    }
}
