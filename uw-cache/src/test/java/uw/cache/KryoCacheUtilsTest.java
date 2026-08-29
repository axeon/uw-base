package uw.cache;

import org.junit.jupiter.api.Test;
import uw.cache.util.KryoCacheUtils;
import uw.cache.vo.CacheValueWrapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link KryoCacheUtils} 序列化协议纯函数测试（无 Redis 依赖）。
 */
class KryoCacheUtilsTest {

    /**
     * 测试载体：kryo 要求具体类 + 无参构造。
     */
    public static class Payload {
        private String id;
        private int num;

        public Payload() {
        }

        public Payload(String id, int num) {
            this.id = id;
            this.num = num;
        }

        public String getId() {
            return id;
        }

        public int getNum() {
            return num;
        }
    }

    @Test
    void roundTripWithValue() {
        CacheValueWrapper<Payload> src = new CacheValueWrapper<>(new Payload("a", 7), 60_000L);
        byte[] data = KryoCacheUtils.serializeValueWrapper(src);
        assertThat(data).isNotNull().isNotEmpty();
        CacheValueWrapper<Payload> dst = KryoCacheUtils.deserializeValueWrapper(data, Payload.class);
        assertThat(dst).isNotNull();
        assertThat(dst.getValue()).isNotNull();
        assertThat(dst.getValue().getId()).isEqualTo("a");
        assertThat(dst.getValue().getNum()).isEqualTo(7);
        assertThat(dst.getExpiredAt()).isEqualTo(src.getExpiredAt());
    }

    @Test
    void roundTripWithNullValueKeepsExpiryOnly() {
        CacheValueWrapper<Payload> src = new CacheValueWrapper<>(null, 60_000L);
        byte[] data = KryoCacheUtils.serializeValueWrapper(src);
        //协议:仅 8 字节过期时间戳,无业务值
        assertThat(data).hasSize(8);
        CacheValueWrapper<Payload> dst = KryoCacheUtils.deserializeValueWrapper(data, Payload.class);
        assertThat(dst).isNotNull();
        assertThat(dst.getValue()).isNull();
        assertThat(dst.getExpiredAt()).isEqualTo(src.getExpiredAt());
    }

    @Test
    void nullInputsReturnNull() {
        assertThat(KryoCacheUtils.serializeValueWrapper(null)).isNull();
        assertThat(KryoCacheUtils.deserializeValueWrapper(null, Payload.class)).isNull();
        assertThat(KryoCacheUtils.deserializeValueWrapper(new byte[0], Payload.class)).isNull();
    }
}
