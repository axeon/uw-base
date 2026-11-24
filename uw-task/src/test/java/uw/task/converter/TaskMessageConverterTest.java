package uw.task.converter;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import uw.task.TaskData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link TaskMessageConverter} 的纯单元测试，覆盖 kryo 往返、fail-fast、未知 content-type 降级三条核心路径。
 *
 * @author axeon
 */
class TaskMessageConverterTest {

    private final TaskMessageConverter converter = new TaskMessageConverter();

    @Test
    void roundTrip_preservesTaskDataFields() {
        TaskData<String, String> original = TaskData.<String, String>builder("com.demo.MyTask")
                .taskParam("hello-param")
                .taskTag("tag-1")
                .runTarget("default")
                .build();
        original.setResultData("ok");

        Message message = converter.toMessage(original, new MessageProperties());
        assertNotNull(message);
        // content-type 必须被设置（非默认），消费端据此识别本框架消息
        assertNotNull(message.getMessageProperties().getContentType());

        Object restored = converter.fromMessage(message);
        assertNotNull(restored);
        @SuppressWarnings("unchecked")
        TaskData<String, String> back = (TaskData<String, String>) restored;
        assertEquals("com.demo.MyTask", back.getTaskClass());
        assertEquals("hello-param", back.getTaskParam());
        assertEquals("ok", back.getResultData());
        assertEquals("tag-1", back.getTaskTag());
    }

    @Test
    void toMessage_rejectsNull() {
        // fail-fast：误传 null 不应静默发送空消息
        assertThrows(MessageConversionException.class, () -> converter.toMessage(null, new MessageProperties()));
    }

    @Test
    void toMessage_rejectsNonTaskData() {
        // fail-fast：非 TaskData 对象直接拒绝
        assertThrows(MessageConversionException.class, () -> converter.toMessage("not-a-task", new MessageProperties()));
    }

    @Test
    void fromMessage_returnsNullForUnknownContentType() {
        // 未知 content-type：不处理、返回 null（消费端据此跳过），不抛异常
        MessageProperties props = new MessageProperties();
        props.setContentType("text/plain");
        Message message = new Message("garbage".getBytes(), props);
        assertNull(converter.fromMessage(message));
    }

    @Test
    void fromMessage_returnsNullForNullOrEmptyMessage() {
        // message 为 null：fromMessage 第一道判空直接返回 null
        assertNull(converter.fromMessage(null));
        // body 为空：Spring AMQP 的 Message 构造不允许 body=null（抛 IllegalArgument），
        // 故用空 byte[]；空 body 经 fromMessage 走未知 content-type 的 warn 分支（或匹配 content-type 时
        // KryoUtils.deserialize 对 length==0 返回 null），最终返回 null
        MessageProperties props = new MessageProperties();
        props.setContentType("text/plain");
        assertNull(converter.fromMessage(new Message(new byte[0], props)));
    }

    @Test
    void toMessage_doesNotSerializeRefObject() {
        // refObject 是内存态字段（@JsonIgnore），不参与 MQ 序列化：往返后必为 null，其他字段正常
        TaskData<String, String> td = TaskData.<String, String>builder("com.demo.MyTask").taskParam("p").build();
        td.setRefObject(new Object());
        Message message = converter.toMessage(td, new MessageProperties());
        @SuppressWarnings("unchecked")
        TaskData<String, String> back = (TaskData<String, String>) converter.fromMessage(message);
        assertNotNull(back);
        assertNull(back.getRefObject(), "refObject 不应被序列化（内存态字段，不跨 MQ 传输）");
        assertEquals("p", back.getTaskParam());
    }
}
