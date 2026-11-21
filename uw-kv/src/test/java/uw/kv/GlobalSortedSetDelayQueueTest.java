package uw.kv;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.Serializable;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 {@link GlobalSortedSet} 能否支撑延迟队列场景。
 * <p>
 * 用 zset 的 score 承载到期时间戳，按 score 范围查询/删除到期元素。覆盖四个延迟队列关心的能力：
 * <ol>
 *   <li>基本延迟与到期：score=到期时间，listRangeByScore 只返回已到期元素；</li>
 *   <li>member 唯一性陷阱：zset member 为 kryo 序列化字节，相同对象会互相覆盖（延迟任务载体必须带唯一 id）；</li>
 *   <li>不队头阻塞：长延迟元素不挡短延迟元素（对照 classic TTL+DLX 的队头阻塞痛点）；</li>
 *   <li>脏数据逐条兜底：反序列化失败的元素被跳过，不拖垮整批。</li>
 * </ol>
 * <p>需真实 Redis（见 test/resources/application.yml）。每个 case 用独立 setName 并在 finally 清理。
 */
@SpringBootTest(classes = GlobalSortedSetDelayQueueTest.TestApp.class)
class GlobalSortedSetDelayQueueTest {

    /**
     * 最小启动配置：仅触发自动配置（加载 UwCacheAutoConfiguration + RedisAutoConfiguration 提供 ClientResources），
     * 不做组件扫描，避免扫到同包下其他 UwCacheTest*Application 的 @SpringBootApplication 造成多配置冲突。
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(uw.kv.conf.KvTestTemplateConfig.class)
    static class TestApp {
    }

    /**
     * dataCacheRedisTemplate（byte[] 值），用于脏数据用例直接注入坏字节到 zset。
     */
    @Qualifier("dataCacheRedisTemplate")
    @Autowired
    private RedisTemplate<String, byte[]> dataCacheRedisTemplate;

    private static String setName(String tag) {
        return "delay-test-" + tag + "-" + System.currentTimeMillis();
    }

    /**
     * 基本延迟与到期：投递 3 个不同到期时间的元素，等待后只应取出已到期的那个。
     */
    @Test
    void basicDelayAndExpiry() throws InterruptedException {
        String set = setName("basic");
        try {
            long now = System.currentTimeMillis();
            assertThat(GlobalSortedSet.add(set, new DelayItem("a", "1"), now + 1000)).isTrue();
            assertThat(GlobalSortedSet.add(set, new DelayItem("b", "2"), now + 2000)).isTrue();
            assertThat(GlobalSortedSet.add(set, new DelayItem("c", "3"), now + 3000)).isTrue();
            assertThat(GlobalSortedSet.size(set)).isEqualTo(3);

            // 等待 a(now+1000) 到期，b/c 仍未到期
            Thread.sleep(1500);
            long now2 = System.currentTimeMillis();
            Set<DelayItem> due = GlobalSortedSet.listRangeByScore(set, DelayItem.class, 0, now2);
            assertThat(due).hasSize(1);
            assertThat(due.iterator().next().getId()).isEqualTo("a");

            // 删除已到期范围，剩余 b/c
            assertThat(GlobalSortedSet.removeRangeByScore(set, 0, now2)).isEqualTo(1);
            assertThat(GlobalSortedSet.size(set)).isEqualTo(2);
        } finally {
            GlobalSortedSet.delete(set);
        }
    }

    /**
     * member 唯一性陷阱：kryo 序列化后字节相同的两个元素会被 zset 视为同一 member 互相覆盖。
     * 延迟任务载体必须带唯一标识（如 taskId），否则同参数任务会丢。
     */
    @Test
    void memberUniquenessTrap() {
        String set = setName("uniq");
        try {
            // 完全相同的两个对象 → kryo 字节相同 → zadd 视为同一 member 仅更新 score：
            // 第二次 add 返回 false（Spring 契约：仅更新 score 时返回 false）——这正是"覆盖"的证明，size 仍为 1
            assertThat(GlobalSortedSet.add(set, new DelayItem("id1", "x"), 1000)).isTrue();
            assertThat(GlobalSortedSet.add(set, new DelayItem("id1", "x"), 2000)).isFalse();
            assertThat(GlobalSortedSet.size(set)).as("相同 payload 被 zset 覆盖，只保留一条").isEqualTo(1);

            // id 不同 → 字节不同 → 共存
            assertThat(GlobalSortedSet.add(set, new DelayItem("id2", "x"), 3000)).isTrue();
            assertThat(GlobalSortedSet.size(set)).isEqualTo(2);
        } finally {
            GlobalSortedSet.delete(set);
        }
    }

    /**
     * 不队头阻塞：先投一条长延迟(30s)，再投一批短延迟(1s)；短延迟到期应全部被取出，不被长延迟挡住。
     * 这正是 zset 相对 classic TTL+DLX 的核心优势。
     */
    @Test
    void noHeadOfLineBlocking() throws InterruptedException {
        String set = setName("noblk");
        try {
            long now = System.currentTimeMillis();
            assertThat(GlobalSortedSet.add(set, new DelayItem("long", "L"), now + 30000)).isTrue();
            for (int i = 0; i < 5; i++) {
                assertThat(GlobalSortedSet.add(set, new DelayItem("short" + i, "s"), now + 1000)).isTrue();
            }
            assertThat(GlobalSortedSet.size(set)).isEqualTo(6);

            Thread.sleep(1500);
            Set<DelayItem> due = GlobalSortedSet.listRangeByScore(set, DelayItem.class, 0, System.currentTimeMillis());
            assertThat(due).as("5 个短延迟元素全部到期，未被长延迟队头阻塞").hasSize(5);

            // 删除已到期，长延迟应仍在队列
            GlobalSortedSet.removeRangeByScore(set, 0, System.currentTimeMillis());
            assertThat(GlobalSortedSet.size(set)).isEqualTo(1);
        } finally {
            GlobalSortedSet.delete(set);
        }
    }

    /**
     * 脏数据逐条兜底：注入一条非 kryo 格式的坏字节，listRangeByScore 应跳过它只返回正常元素，不抛异常。
     * 验证 {@link GlobalSortedSet#listRangeByScore} 的逐条 try/catch 行为（遵循 KryoUtils 异常策略）。
     */
    @Test
    void dirtyDataSkipped() {
        String set = setName("dirty");
        try {
            long now = System.currentTimeMillis();
            assertThat(GlobalSortedSet.add(set, new DelayItem("good", "g"), now + 1000)).isTrue();
            // 直接注入坏字节（非 kryo 格式）到同一 zset，需带 GlobalSortedSet 的 REDIS_PREFIX
            dataCacheRedisTemplate.opsForZSet().add("uw-zset:" + set, new byte[]{1, 2, 3, 4, 5}, now + 1000);

            Set<DelayItem> due = GlobalSortedSet.listRangeByScore(set, DelayItem.class, 0, now + 2000);
            assertThat(due).as("坏数据被跳过，只返回正常元素").hasSize(1);
            assertThat(due.iterator().next().getId()).isEqualTo("good");
        } finally {
            GlobalSortedSet.delete(set);
        }
    }

    /**
     * 测试用延迟任务载体（模拟延迟队列里的一个元素）。
     * kryo 序列化要求具体类 + 无参构造。
     */
    public static class DelayItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private String id;
        private String name;

        public DelayItem() {
        }

        public DelayItem(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
