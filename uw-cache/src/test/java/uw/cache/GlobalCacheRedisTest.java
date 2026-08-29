package uw.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import uw.cache.vo.CacheValueWrapper;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GlobalCache} 真实 Redis 集成测试（见 test/resources/application.yml）。
 * <p>重点覆盖 2026-08-29 修复的保护时间语义：nullProtectMillis（loader 正常返回 null）与
 * failProtectMillis（loader 重试耗尽）此前被硬编码 DEFAULT 值忽略，现须按参数生效且两场景区分。</p>
 * <p>每个 case 用独立 cacheName（时间戳后缀）并在 @AfterEach 清理。</p>
 */
@SpringBootTest(classes = GlobalCacheRedisTest.TestApp.class)
class GlobalCacheRedisTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApp {
    }

    private static final String CACHE = "TestGlobalCache" + System.currentTimeMillis();

    @AfterEach
    void cleanUp() {
        GlobalCache.invalidate(CACHE, null);
    }

    /**
     * 测试载体（kryo 要求具体类 + 无参构造）。
     */
    public static class UserVo {
        private String name;

        public UserVo() {
        }

        public UserVo(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @Test
    void putGetRoundTripAndTtl() throws InterruptedException {
        GlobalCache.put(CACHE, "k1", new UserVo("tom"), 60_000L);
        CacheValueWrapper<UserVo> wrapper = GlobalCache.get(CACHE, "k1", UserVo.class);
        assertThat(wrapper).isNotNull();
        assertThat(wrapper.getValue().getName()).isEqualTo("tom");
        assertThat(GlobalCache.containsKey(CACHE, "k1")).isTrue();
        //短TTL:过期后 get 返回 null(Redis 已回收)
        GlobalCache.put(CACHE, "k2", new UserVo("jerry"), 100L);
        Thread.sleep(300L);
        assertThat(GlobalCache.get(CACHE, "k2", UserVo.class)).isNull();
        assertThat(GlobalCache.containsKey(CACHE, "k2")).isFalse();
    }

    @Test
    void loadValueWrapperLoadsAndCachesToRedis() {
        AtomicInteger loadCount = new AtomicInteger();
        CacheValueWrapper<UserVo> wrapper = GlobalCache.loadValueWrapper(CACHE, "L1", new CacheDataLoader<String, UserVo>() {
            @Override
            public UserVo load(String key) {
                loadCount.incrementAndGet();
                return new UserVo("loaded");
            }
        }, 60_000L);
        assertThat(wrapper.getValue().getName()).isEqualTo("loaded");
        assertThat(loadCount.get()).isEqualTo(1);
        //二次加载走 Redis 缓存,不再触发业务 loader
        CacheValueWrapper<UserVo> wrapper2 = GlobalCache.loadValueWrapper(CACHE, "L1", new CacheDataLoader<String, UserVo>() {
            @Override
            public UserVo load(String key) {
                loadCount.incrementAndGet();
                return new UserVo("loaded2");
            }
        }, 60_000L);
        assertThat(wrapper2.getValue().getName()).isEqualTo("loaded");
        assertThat(loadCount.get()).isEqualTo(1);
    }

    /**
     * R1 修复验证:loader 正常返回 null 时按自定义 nullProtectMillis 生效(此前硬编码 60s)。
     */
    @Test
    void nullProtectMillisTakesEffect() throws InterruptedException {
        long nullProtect = 800L;
        CacheValueWrapper<UserVo> wrapper = GlobalCache.loadValueWrapper(CACHE, "N1", new CacheDataLoader<String, UserVo>() {
            @Override
            public UserVo load(String key) {
                return null;
            }
        }, 60_000L, nullProtect, 60_000L, 50L, 1);
        assertThat(wrapper).isNotNull();
        assertThat(wrapper.getValue()).isNull();
        //保护期内:未过期(防穿透)
        assertThat(wrapper.checkExpired()).isFalse();
        //保护期过后:过期(可重新回源)
        Thread.sleep(nullProtect + 200L);
        assertThat(wrapper.checkExpired()).isTrue();
    }

    /**
     * R1 修复验证:loader 持续异常重试耗尽后按自定义 failProtectMillis 生效,且与 nullProtect 区分。
     */
    @Test
    void failProtectMillisTakesEffect() throws InterruptedException {
        long failProtect = 800L;
        long nullProtect = 60_000L;
        CacheValueWrapper<UserVo> wrapper = GlobalCache.loadValueWrapper(CACHE, "F1", new CacheDataLoader<String, UserVo>() {
            @Override
            public UserVo load(String key) {
                throw new IllegalStateException("load failed");
            }
        }, 60_000L, nullProtect, failProtect, 50L, 2);
        assertThat(wrapper).isNotNull();
        assertThat(wrapper.getValue()).isNull();
        //失败保护期内未过期;若参数仍被忽略(硬编码60s),此处应未过期、下一断言(0.8s后过期)将失败
        assertThat(wrapper.checkExpired()).isFalse();
        Thread.sleep(failProtect + 200L);
        assertThat(wrapper.checkExpired()).isTrue();
    }

    @Test
    void invalidateSingleKeyAndNullAllAndPrefix() {
        GlobalCache.put(CACHE, "p1", new UserVo("a"), 60_000L);
        GlobalCache.put(CACHE, "p2", new UserVo("b"), 60_000L);
        GlobalCache.put(CACHE, "other1", new UserVo("c"), 60_000L);
        //单key删除
        assertThat(GlobalCache.invalidate(CACHE, "p1")).isTrue();
        assertThat(GlobalCache.containsKey(CACHE, "p1")).isFalse();
        assertThat(GlobalCache.containsKey(CACHE, "p2")).isTrue();
        //前缀删除:只删 p*
        assertThat(GlobalCache.invalidatePrefix(CACHE, "p")).isTrue();
        assertThat(GlobalCache.containsKey(CACHE, "p2")).isFalse();
        assertThat(GlobalCache.containsKey(CACHE, "other1")).isTrue();
        //keys
        Set<String> keys = GlobalCache.keys(CACHE, null);
        assertThat(keys).containsExactly("other1");
        //key=null 全清
        assertThat(GlobalCache.invalidate(CACHE, null)).isTrue();
        assertThat(GlobalCache.containsKey(CACHE, "other1")).isFalse();
    }
}
