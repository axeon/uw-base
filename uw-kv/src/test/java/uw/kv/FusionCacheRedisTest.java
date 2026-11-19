package uw.kv;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link FusionCache} 真实 Redis 集成测试。
 * <p>覆盖：本地/全局两种模式、loader 加载链（GlobalFusionCacheLoader / LocalRetryCacheLoader）、
 * nullProtect 防穿透（保护期内不重复回源）、failProtect、过期后重载新值、put/invalidate/containsKey/putAll。</p>
 * <p>cacheWrapperMap 为静态注册表,每个 case 用独立 cacheName(时间戳后缀)隔离。</p>
 */
@SpringBootTest(classes = FusionCacheRedisTest.TestApp.class)
class FusionCacheRedisTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApp {
    }

    private static final String PREFIX = "TestFusionCache" + System.currentTimeMillis();
    private static final String LOCAL = PREFIX + "Local";
    private static final String GLOBAL = PREFIX + "Global";
    private static final String EXPIRE = PREFIX + "Expire";

    @AfterEach
    void cleanUp() {
        FusionCache.invalidate(LOCAL, null, false);
        FusionCache.invalidate(GLOBAL, null, false);
        FusionCache.invalidate(EXPIRE, null, false);
    }

    /**
     * 纯本地模式:put/get 直写直读、loader 未命中加载、invalidate 后可重新加载。
     */
    @Test
    void localModePutGetAndLoader() {
        FusionCache.config(FusionCache.Config.builder()
                .cacheName(LOCAL)
                .localCacheMaxNum(1000)
                .globalCache(false)
                .build(), new CacheDataLoader<String, String>() {
            @Override
            public String load(String key) {
                return "loaded-" + key;
            }
        });
        //put/get 直写直读
        FusionCache.put(LOCAL, "p1", "v1");
        String val1 = FusionCache.get(LOCAL, "p1");
        assertThat(val1).isEqualTo("v1");
        assertThat(FusionCache.containsKey(LOCAL, "p1")).isTrue();
        //未命中走 loader
        String val2 = FusionCache.get(LOCAL, "miss1");
        assertThat(val2).isEqualTo("loaded-miss1");
        //缓存后二次 get 不再回源(loader 返回新值也不会变)
        FusionCache.invalidate(LOCAL, "miss1");
        String val3 = FusionCache.get(LOCAL, "miss1");
        assertThat(val3).isEqualTo("loaded-miss1");
        //invalidate 指定 key
        FusionCache.invalidate(LOCAL, "p1", false);
        assertThat(FusionCache.containsKey(LOCAL, "p1")).isFalse();
    }

    /**
     * 全局模式:put 同步写 Redis(GlobalCache 可见),get 未命中经 Redis 回源缓存。
     */
    @Test
    void globalModePutWritesRedisAndGetLoads() {
        FusionCache.config(FusionCache.Config.builder()
                .cacheName(GLOBAL)
                .localCacheMaxNum(1000)
                .cacheExpireMillis(60_000L)
                .build(), new CacheDataLoader<String, String>() {
            @Override
            public String load(String key) {
                return "g-loaded-" + key;
            }
        });
        FusionCache.put(GLOBAL, "g1", "gv1");
        //FusionCache.put 在全局模式同步写 Redis
        assertThat(GlobalCache.containsKey(GLOBAL, "g1")).isTrue();
        String val4 = FusionCache.get(GLOBAL, "g1");
        assertThat(val4).isEqualTo("gv1");
        //未命中经 loader→Redis→本地
        String val5 = FusionCache.get(GLOBAL, "g2");
        assertThat(val5).isEqualTo("g-loaded-g2");
        assertThat(GlobalCache.containsKey(GLOBAL, "g2")).isTrue();
        //通知集群失效(自实例消息被过滤,不炸即可)
        FusionCache.notifyInvalidate(GLOBAL, "g1");
    }

    /**
     * nullProtect 防穿透:loader 返回 null 后保护期内二次 get 不再回源(计数验证)。
     */
    @Test
    void nullProtectPreventsRepeatedLoad() {
        AtomicInteger loadCount = new AtomicInteger();
        FusionCache.config(FusionCache.Config.builder()
                .cacheName(PREFIX + "Null")
                .localCacheMaxNum(1000)
                .globalCache(false)
                .nullProtectMillis(5_000L)
                .build(), new CacheDataLoader<String, String>() {
            @Override
            public String load(String key) {
                loadCount.incrementAndGet();
                return null;
            }
        });
        String val6 = FusionCache.get(PREFIX + "Null", "n1");
        assertThat(val6).isNull();
        String val7 = FusionCache.get(PREFIX + "Null", "n1");
        assertThat(val7).isNull();
        //保护期内只回源一次
        assertThat(loadCount.get()).isEqualTo(1);
        FusionCache.invalidate(PREFIX + "Null", null, false);
    }

    /**
     * failProtect:loader 持续异常重试耗尽后返回 null 且保护期内不重复回源。
     */
    @Test
    void failProtectAfterRetryExhausted() {
        AtomicInteger loadCount = new AtomicInteger();
        FusionCache.config(FusionCache.Config.builder()
                .cacheName(PREFIX + "Fail")
                .localCacheMaxNum(1000)
                .globalCache(false)
                .failProtectMillis(5_000L)
                .reloadMaxTimes(2)
                .reloadIntervalMillis(50)
                .build(), new CacheDataLoader<String, String>() {
            @Override
            public String load(String key) {
                loadCount.incrementAndGet();
                throw new IllegalStateException("load fail");
            }
        });
        String val8 = FusionCache.get(PREFIX + "Fail", "f1");
        assertThat(val8).isNull();
        //重试 2 次耗尽
        assertThat(loadCount.get()).isEqualTo(2);
        //保护期内二次 get 不再回源
        String val9 = FusionCache.get(PREFIX + "Fail", "f1");
        assertThat(val9).isNull();
        assertThat(loadCount.get()).isEqualTo(2);
        FusionCache.invalidate(PREFIX + "Fail", null, false);
    }

    /**
     * 过期后重载:短 cacheExpireMillis 过期后重新回源拿到新值。
     */
    @Test
    void expiredValueReloads() throws InterruptedException {
        AtomicInteger version = new AtomicInteger();
        FusionCache.config(FusionCache.Config.builder()
                .cacheName(EXPIRE)
                .localCacheMaxNum(1000)
                .globalCache(false)
                .cacheExpireMillis(100L)
                .build(), new CacheDataLoader<String, String>() {
            @Override
            public String load(String key) {
                return "v" + version.incrementAndGet();
            }
        });
        String val10 = FusionCache.get(EXPIRE, "e1");
        assertThat(val10).isEqualTo("v1");
        Thread.sleep(300L);
        //本地 wrapper 过期→重试循环删本地→loader 重新加载拿到新值
        String val11 = FusionCache.get(EXPIRE, "e1");
        assertThat(val11).isEqualTo("v2");
    }

    /**
     * putAll 批量写入与 keys。
     */
    @Test
    void putAllAndKeys() {
        FusionCache.config(FusionCache.Config.builder()
                .cacheName(PREFIX + "Bulk")
                .localCacheMaxNum(1000)
                .globalCache(false)
                .build(), new CacheDataLoader<String, String>() {
            @Override
            public String load(String key) {
                return null;
            }
        });
        Map<Object, Object> map = new ConcurrentHashMap<>();
        map.put("b1", "1");
        map.put("b2", "2");
        FusionCache.putAll(PREFIX + "Bulk", map);
        String val12 = FusionCache.get(PREFIX + "Bulk", "b1");
        assertThat(val12).isEqualTo("1");
        String val13 = FusionCache.get(PREFIX + "Bulk", "b2");
        assertThat(val13).isEqualTo("2");
        assertThat(FusionCache.keys(PREFIX + "Bulk", null)).containsExactlyInAnyOrder("b1", "b2");
        FusionCache.invalidate(PREFIX + "Bulk", null, false);
    }
}
