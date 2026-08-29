package uw.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GlobalSortedSet} 真实 Redis 集成测试（延迟队列场景见 GlobalSortedSetDelayQueueTest，此处补齐其余能力）。
 */
@SpringBootTest(classes = GlobalSortedSetRedisTest.TestApp.class)
class GlobalSortedSetRedisTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApp {
    }

    private static final String SET = "TestSortedSet" + System.currentTimeMillis();

    @AfterEach
    void cleanUp() {
        GlobalSortedSet.delete(SET);
    }

    @Test
    void addScoreUpdateAndBatchRemove() {
        assertThat(GlobalSortedSet.add(SET, "m1", 100)).isTrue();
        //相同 member:仅更新 score,add 返回 false
        assertThat(GlobalSortedSet.add(SET, "m1", 200)).isFalse();
        assertThat(GlobalSortedSet.size(SET)).isEqualTo(1);
        assertThat(GlobalSortedSet.add(SET, "m2", 300)).isTrue();
        assertThat(GlobalSortedSet.add(SET, "m3", 400)).isTrue();
        //批量删除(varargs):删2个
        assertThat(GlobalSortedSet.remove(SET, "m1", "m2")).isEqualTo(2);
        assertThat(GlobalSortedSet.size(SET)).isEqualTo(1);
        //单个删除
        assertThat(GlobalSortedSet.remove(SET, "m3")).isEqualTo(1);
        assertThat(GlobalSortedSet.size(SET)).isZero();
    }

    @Test
    void rangeByScore() {
        GlobalSortedSet.add(SET, "low", 1.0);
        GlobalSortedSet.add(SET, "mid", 5.0);
        GlobalSortedSet.add(SET, "high", 9.0);
        Set<String> range = GlobalSortedSet.listRangeByScore(SET, String.class, 2.0, 8.0);
        assertThat(range).containsExactly("mid");
        //闭区间语义:含边界
        Set<String> range2 = GlobalSortedSet.listRangeByScore(SET, String.class, 1.0, 1.0);
        assertThat(range2).containsExactly("low");
    }

    @Test
    void removeRangeByScore() {
        GlobalSortedSet.add(SET, "a", 1.0);
        GlobalSortedSet.add(SET, "b", 2.0);
        GlobalSortedSet.add(SET, "c", 3.0);
        assertThat(GlobalSortedSet.removeRangeByScore(SET, 1.0, 2.0)).isEqualTo(2);
        assertThat(GlobalSortedSet.size(SET)).isEqualTo(1);
    }
}
