package uw.task;

import org.junit.jupiter.api.Test;
import uw.common.util.KryoUtils;

import java.util.Date;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link TaskData} 的 Kryo 序列化 round-trip 测试。
 * <p>
 * TaskData 实现 {@link com.esotericsoftware.kryo.KryoSerializable}，自写原语字段读写。
 * 本测试验证：① 全字段经 serialize→deserialize 后一致；② null 字段按约定处理（String null→""）；
 * ③ refObject 刻意不写入字节流（反序列化后必为 null）。
 *
 * @author axeon
 */
class TaskDataTest {

    /**
     * 测试用 taskParam/resultData 类型。
     * 需公开无参构造供 kryo 反射创建（writeClassAndObject 携带类信息，未注册走全限定类名）。
     */
    public static class TestParam {
        private String name;
        private int count;

        public TestParam() {
        }

        public TestParam(String name, int count) {
            this.name = name;
            this.count = count;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof TestParam that)) {
                return false;
            }
            return count == that.count && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, count);
        }
    }

    @Test
    void roundTrip_preservesAllFields() {
        TaskData<TestParam, TestParam> src = new TaskData<>("com.demo.FooRunner");
        src.setId(123L);
        src.setRefTag("refTag");
        src.setRefId(456L);
        src.setRefSubId(789L);
        src.setRateLimitTag("rate");
        src.setTaskTag("tag1");
        src.setTaskDelay(5000L);
        src.setTaskParam(new TestParam("hello", 42));
        src.setRunType(TaskData.RUN_TYPE_GLOBAL);
        src.setRetryType(TaskData.RETRY_TYPE_AUTO);
        src.setRunTarget("prod");
        Date q = new Date(1700000000000L);
        Date c = new Date(1700000001000L);
        Date r = new Date(1700000002000L);
        Date f = new Date(1700000003000L);
        src.setQueueDate(q);
        src.setConsumeDate(c);
        src.setRunDate(r);
        src.setFinishDate(f);
        src.setResultData(new TestParam("result", 99));
        src.setErrorInfo("some error");
        src.setRanTimes(3);
        src.setState(TaskData.STATE_SUCCESS);

        byte[] bytes = KryoUtils.serialize(src);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        TaskData<TestParam, TestParam> restored = KryoUtils.deserialize(bytes, TaskData.class);
        assertNotNull(restored);
        assertEquals(src.getId(), restored.getId());
        assertEquals(src.getRefTag(), restored.getRefTag());
        assertEquals(src.getRefId(), restored.getRefId());
        assertEquals(src.getRefSubId(), restored.getRefSubId());
        assertEquals(src.getRateLimitTag(), restored.getRateLimitTag());
        assertEquals(src.getTaskClass(), restored.getTaskClass());
        assertEquals(src.getTaskTag(), restored.getTaskTag());
        assertEquals(src.getTaskDelay(), restored.getTaskDelay());
        assertEquals(src.getTaskParam(), restored.getTaskParam());
        assertEquals(src.getRunType(), restored.getRunType());
        assertEquals(src.getRetryType(), restored.getRetryType());
        assertEquals(src.getRunTarget(), restored.getRunTarget());
        assertEquals(q, restored.getQueueDate());
        assertEquals(c, restored.getConsumeDate());
        assertEquals(r, restored.getRunDate());
        assertEquals(f, restored.getFinishDate());
        assertEquals(src.getResultData(), restored.getResultData());
        assertEquals(src.getErrorInfo(), restored.getErrorInfo());
        assertEquals(src.getRanTimes(), restored.getRanTimes());
        assertEquals(src.getState(), restored.getState());
    }

    @Test
    void roundTrip_handlesNullables() {
        TaskData<TestParam, TestParam> src = new TaskData<>("com.demo.BarRunner");
        // taskParam/resultData/Date/refTag/rateLimitTag/errorInfo 留 null，验证 null 约定
        byte[] bytes = KryoUtils.serialize(src);
        TaskData<TestParam, TestParam> restored = KryoUtils.deserialize(bytes, TaskData.class);

        // String null 按约定写空串，读回为 ""（非 null）
        assertEquals("", restored.getRefTag());
        assertEquals("", restored.getRateLimitTag());
        assertEquals("", restored.getErrorInfo());
        // 字段默认值
        assertEquals("com.demo.BarRunner", restored.getTaskClass());
        assertEquals("", restored.getTaskTag());
        assertEquals("default", restored.getRunTarget());
        // 任意对象与 Date 的 null 哨兵（长度 0 / 0）读回为 null
        assertNull(restored.getTaskParam());
        assertNull(restored.getResultData());
        assertNull(restored.getQueueDate());
        assertNull(restored.getConsumeDate());
        assertNull(restored.getRunDate());
        assertNull(restored.getFinishDate());
    }

    @Test
    void refObject_isNotSerialized() {
        TaskData<TestParam, TestParam> src = new TaskData<>("com.demo.BazRunner");
        src.setTaskParam(new TestParam("x", 1));
        src.setRefObject(new Object()); // 任意 refObject

        byte[] bytes = KryoUtils.serialize(src);
        TaskData<TestParam, TestParam> restored = KryoUtils.deserialize(bytes, TaskData.class);

        // refObject 刻意不写入字节流，反序列化后必为 null
        assertNull(restored.getRefObject());
        // taskParam 仍正常还原
        assertEquals(new TestParam("x", 1), restored.getTaskParam());
    }
}
