package uw.kv;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import uw.common.util.KryoUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GlobalHashSet} 真实 Redis 集成测试。
 * <p>覆盖：add/contains/remove/size、重复 add 的 member 语义、pop/random、list 全量、
 * 脏数据逐条跳过（符合 Kryo 批量读取安全规范）、keys/delete。</p>
 */
@SpringBootTest(classes = GlobalHashSetRedisTest.TestApp.class)
class GlobalHashSetRedisTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(uw.kv.conf.KvTestTemplateConfig.class)
    static class TestApp {
    }

    private static final String SET = "TestHashSet" + System.currentTimeMillis();

    @Autowired
    @Qualifier("dataCacheRedisTemplate")
    private RedisTemplate<String, byte[]> dataCacheRedisTemplate;

    @AfterEach
    void cleanUp() {
        GlobalHashSet.delete(SET);
    }

    @Test
    void addContainsSizeRemove() {
        assertThat(GlobalHashSet.add(SET, "a")).isEqualTo(1);
        assertThat(GlobalHashSet.add(SET, "b")).isEqualTo(1);
        //重复元素:add 返回 false(set member 语义),size 不增
        assertThat(GlobalHashSet.add(SET, "a")).isEqualTo(0);
        assertThat(GlobalHashSet.size(SET)).isEqualTo(2);
        assertThat(GlobalHashSet.contains(SET, "a")).isTrue();
        assertThat(GlobalHashSet.contains(SET, "c")).isFalse();
        //remove 返回删除数,删除不存在返回 0
        assertThat(GlobalHashSet.remove(SET, "a")).isEqualTo(1);
        assertThat(GlobalHashSet.remove(SET, "a")).isZero();
        assertThat(GlobalHashSet.size(SET)).isEqualTo(1);
        //null 参数防御
        assertThat(GlobalHashSet.add(SET, null)).isEqualTo(-1);
        assertThat(GlobalHashSet.contains(null, "x")).isFalse();
        assertThat(GlobalHashSet.size(null)).isEqualTo(-1);
    }

    @Test
    void listAllElements() {
        GlobalHashSet.add(SET, "x");
        GlobalHashSet.add(SET, "y");
        Set<String> all = GlobalHashSet.list(SET, String.class);
        assertThat(all).containsExactlyInAnyOrder("x", "y");
    }

    /**
     * 脏数据逐条跳过:直接向 Redis set 写入坏字节(声明长串但数据不足→KryoBufferUnderflow),
     * 批量 list 不拖垮、返回其余好数据。
     */
    @Test
    void listSkipsDirtyData() {
        GlobalHashSet.add(SET, "good");
        //varint 长度声明 99 但无足够数据,反序列化必抛 Buffer underflow
        dataCacheRedisTemplate.opsForSet().add("uw-set:" + SET, new byte[]{99});
        Set<String> all = GlobalHashSet.list(SET, String.class);
        assertThat(all).containsExactly("good");
        //pop 批量同样跳过脏数据
        Set<String> popped = GlobalHashSet.pop(SET, 10, String.class);
        assertThat(popped).containsExactly("good");
    }

    @Test
    void randomAndPop() {
        GlobalHashSet.add(SET, "r1");
        //随机取一个(存在性弱断言)
        assertThat(GlobalHashSet.random(SET, String.class)).isNotNull();
        //弹出后集合为空
        assertThat(GlobalHashSet.pop(SET, String.class)).isNotNull();
        assertThat(GlobalHashSet.size(SET)).isZero();
    }
}
