package uw.kv;

import org.junit.jupiter.api.Test;
import uw.kv.util.KryoCacheUtils;
import uw.kv.vo.CacheValueWrapper;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;

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
    // ==================== 并发/原子类型序列化（JDK17+ 强封装回归） ====================

    /**
     * 载体：含原子/并发类型字段——JDK17+ 下 FieldSerializer 反射其内部字段会抛
     * InaccessibleObjectException（Kryo #885），显式 serializer 注册后必须通过。
     */
    public static class ConcurrentPayload {
        public AtomicInteger counter = new AtomicInteger();
        public AtomicLong stamp = new AtomicLong();
        public AtomicBoolean flag = new AtomicBoolean();
        public AtomicReference<String> ref = new AtomicReference<>();
        public LongAdder adder = new LongAdder();
        public ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        public CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
        public ConcurrentSkipListMap<String, String> skipMap = new ConcurrentSkipListMap<>();
        public AtomicIntegerArray intArr = new AtomicIntegerArray(3);
        public AtomicLongArray longArr = new AtomicLongArray(2);
    }

    @Test
    void testConcurrentTypesRoundTrip() {
        ConcurrentPayload payload = new ConcurrentPayload();
        payload.counter.set(42);
        payload.stamp.set(1790000000000L);
        payload.flag.set(true);
        payload.ref.set("hello");
        payload.adder.add(7);
        payload.adder.add(35);
        payload.map.put("a", 1);
        payload.map.put("b", 2);
        payload.list.add("x");
        payload.list.add("y");
        payload.skipMap.put("k", "v");
        payload.intArr.set(0, 10);
        payload.intArr.set(2, 30);
        payload.longArr.set(1, 999L);

        CacheValueWrapper<ConcurrentPayload> wrapper = new CacheValueWrapper<>(payload, 60000L);
        byte[] data = KryoCacheUtils.serializeValueWrapper(wrapper);
        CacheValueWrapper<ConcurrentPayload> parsed = KryoCacheUtils.deserializeValueWrapper(data, ConcurrentPayload.class);
        ConcurrentPayload back = parsed.getValue();

        assertThat(back.counter.get()).isEqualTo(42);
        assertThat(back.stamp.get()).isEqualTo(1790000000000L);
        assertThat(back.flag.get()).isTrue();
        assertThat(back.ref.get()).isEqualTo("hello");
        assertThat(back.adder.sum()).isEqualTo(42L);
        assertThat(back.map).containsEntry("a", 1).containsEntry("b", 2).hasSize(2);
        assertThat(back.list).containsExactly("x", "y");
        assertThat(back.skipMap).containsEntry("k", "v");
        assertThat(back.intArr.get(0)).isEqualTo(10);
        assertThat(back.intArr.get(2)).isEqualTo(30);
        assertThat(back.longArr.get(1)).isEqualTo(999L);
    }


    // ==================== 2026-10-10 补充注册回归（UUID/Pattern/时间族/数组） ====================

    /**
     * 载体二：UUID/Pattern（裸用当场炸的实证类）+时间族补全+高频数组。
     */
    public static class SupplementPayload {
        public UUID uuid = UUID.randomUUID();
        public Pattern pattern = Pattern.compile("a+b\\d{2}");
        public Instant instant = Instant.now();
        public Duration duration = Duration.ofMinutes(90);
        public ZonedDateTime zoned = ZonedDateTime.now();
        public String[] names = {"x", "y"};
        public long[] longs = {1L, 2L, 3L};
        public byte[] bytes = {7, 8, 9};
    }

    @Test
    void testSupplementTypesRoundTrip() {
        SupplementPayload payload = new SupplementPayload();
        CacheValueWrapper<SupplementPayload> wrapper = new CacheValueWrapper<>(payload, 60000L);
        byte[] data = KryoCacheUtils.serializeValueWrapper(wrapper);
        CacheValueWrapper<SupplementPayload> parsed = KryoCacheUtils.deserializeValueWrapper(data, SupplementPayload.class);
        SupplementPayload back = parsed.getValue();

        assertThat(back.uuid).isEqualTo(payload.uuid);
        assertThat(back.pattern.pattern()).isEqualTo("a+b\\d{2}");
        assertThat(back.instant).isEqualTo(payload.instant);
        assertThat(back.duration).isEqualTo(payload.duration);
        assertThat(back.zoned.toInstant()).isEqualTo(payload.zoned.toInstant());
        assertThat(back.names).containsExactly("x", "y");
        assertThat(back.longs).containsExactly(1L, 2L, 3L);
        assertThat(back.bytes).containsExactly(7, 8, 9);
    }
}
