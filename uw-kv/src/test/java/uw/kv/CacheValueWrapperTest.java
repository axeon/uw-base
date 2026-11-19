package uw.kv;

import org.junit.jupiter.api.Test;
import uw.kv.vo.CacheValueWrapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CacheValueWrapper} 过期语义纯函数测试。
 */
class CacheValueWrapperTest {

    @Test
    void positiveMillisSetsAbsoluteExpiry() {
        CacheValueWrapper<String> wrapper = new CacheValueWrapper<>("v", 60_000L);
        assertThat(wrapper.getValue()).isEqualTo("v");
        //保护期内未过期
        assertThat(wrapper.checkExpired()).isFalse();
    }

    @Test
    void zeroOrNegativeMillisNeverExpires() {
        assertThat(new CacheValueWrapper<String>("v", 0L).checkExpired()).isFalse();
        assertThat(new CacheValueWrapper<String>("v", -1L).checkExpired()).isFalse();
        //单参构造=永不过期
        assertThat(new CacheValueWrapper<String>("v").checkExpired()).isFalse();
    }

    @Test
    void shortMillisExpiresAfterElapsed() throws InterruptedException {
        CacheValueWrapper<String> wrapper = new CacheValueWrapper<>("v", 50L);
        assertThat(wrapper.checkExpired()).isFalse();
        Thread.sleep(120L);
        assertThat(wrapper.checkExpired()).isTrue();
    }

    @Test
    void longConstructorKeepsNullValue() {
        CacheValueWrapper<String> wrapper = new CacheValueWrapper<>(60_000L);
        assertThat(wrapper.getValue()).isNull();
        assertThat(wrapper.checkExpired()).isFalse();
    }
}
