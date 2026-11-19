package uw.kv;

import org.junit.jupiter.api.Test;
import uw.kv.util.RedisKeyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RedisKeyUtils} 纯函数测试。
 */
class RedisKeyUtilsTest {

    @Test
    void buildTypeIdWithId() {
        assertThat(RedisKeyUtils.buildTypeId("uw-counter:", "Type", 123L)).isEqualTo("uw-counter:Type:123");
        assertThat(RedisKeyUtils.buildTypeId("uw-counter:", "Type", "a:b")).isEqualTo("uw-counter:Type:a:b");
    }

    @Test
    void buildTypeIdWithNullIdReturnsTypeOnly() {
        assertThat(RedisKeyUtils.buildTypeId("uw-counter:", "Type", null)).isEqualTo("uw-counter:Type");
    }

    @Test
    void buildKeyJoinsArgs() {
        assertThat(RedisKeyUtils.buildKey("prefix", "a", 1, "b")).isEqualTo("prefix:a:1:b");
    }

    @Test
    void buildKeyWithBlankPrefixOmitsPrefix() {
        assertThat(RedisKeyUtils.buildKey(null, "a", "b")).isEqualTo("a:b");
        assertThat(RedisKeyUtils.buildKey("", "a", "b")).isEqualTo("a:b");
    }

    @Test
    void buildKeyWithNoArgs() {
        //有前缀无参数:仅剩前缀(尾分隔符被删除)
        assertThat(RedisKeyUtils.buildKey("prefix")).isEqualTo("prefix");
        //无前缀无参数:空串
        assertThat(RedisKeyUtils.buildKey(null)).isEmpty();
    }
}
