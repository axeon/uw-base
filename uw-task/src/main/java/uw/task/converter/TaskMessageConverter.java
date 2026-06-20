package uw.task.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import uw.common.util.KryoUtils;
import uw.task.TaskData;

import java.nio.charset.StandardCharsets;

/**
 * 用于spring-amqp的消息转换器。
 * <p>
 * 序列化/反序列化委托给 {@link KryoUtils}。当前场景下消息体固定为 {@link TaskData}，
 * 故两端类型已知，用 {@link KryoUtils#serialize(Object)} / {@link KryoUtils#deserialize(byte[], Class)}
 * （不带外层类信息），比 writeClassAndObject 更省体积。
 * TaskData 内部的泛型/Object 字段（taskParam、resultData、refObject）仍按需自带类信息（由 kryo 处理）。
 *
 * @author axeon
 */
public class TaskMessageConverter implements MessageConverter {

    private static final Logger log = LoggerFactory.getLogger(TaskMessageConverter.class);

    /**
     * 消息 content-type 标识：标记消息体为 kryo 序列化的 {@link TaskData}。
     * <p>
     * 序列化时写入消息头，反序列化时据此判断是否本转换器处理的消息；非此类型的消息会被忽略并告警。
     */
    private static final String CONTENT_TYPE_TASK_DATA = "UT_DATA";

    /**
     * 默认构造器。
     */
    public TaskMessageConverter() {

    }

    /**
     * 将对象转换为 spring-amqp 的 {@link Message}。
     * <p>
     * 仅支持 {@link TaskData}：用 {@link KryoUtils#serialize(Object)} 转为字节数组，
     * 并在消息头设置 content-type 为 {@link #CONTENT_TYPE_TASK_DATA}。
     * <p>
     * 若对象为 null 或非 TaskData，<b>抛 {@link MessageConversionException}</b>（fail-fast），
     * 避免误传导致空消息静默发送、消费端拿不到数据却不报错。
     * 序列化失败（如 kryo 异常）同样包装为 MessageConversionException 抛出。
     *
     * @param objectToConvert    待转换对象，必须为 TaskData
     * @param messageProperties  消息属性（会设置 content-type 与 content-length）
     * @return 序列化后的 amqp 消息
     * @throws MessageConversionException 对象为 null、非 TaskData、或序列化失败时抛出
     */
    @Override
    public Message toMessage(Object objectToConvert, MessageProperties messageProperties) throws MessageConversionException {
        if (!(objectToConvert instanceof TaskData)) {
            throw new MessageConversionException("Failed to convert Message content: expected TaskData but got "
                    + (objectToConvert == null ? "null" : objectToConvert.getClass().getName()));
        }
        messageProperties.setContentType(CONTENT_TYPE_TASK_DATA);
        byte[] bytes;
        try {
            bytes = KryoUtils.serialize(objectToConvert);
        } catch (Exception e) {
            throw new MessageConversionException("Failed to convert Message content. " + e.getMessage(), e);
        }
        messageProperties.setContentLength(bytes.length);
        return new Message(bytes, messageProperties);
    }

    /**
     * 将 spring-amqp 的 {@link Message} 转换为对象。
     * <p>
     * 仅当消息 content-type 为 {@link #CONTENT_TYPE_TASK_DATA} 时处理：用
     * {@link KryoUtils#deserialize(byte[], Class)} 还原为 {@link TaskData}。
     * 其他 content-type 的消息不处理（返回 null），并以 WARN 级别记录消息内容便于排查。
     * <p>
     * null 消息、空 body、无消息头时直接返回 null。
     *
     * @param message 待转换的 amqp 消息
     * @return 反序列化得到的 TaskData；不处理的消息返回 null
     * @throws MessageConversionException 反序列化失败时抛出
     */
    @Override
    public Object fromMessage(Message message) throws MessageConversionException {
        Object content = null;
        if (message == null || message.getBody() == null || message.getMessageProperties() == null) {
            return null;
        }
        MessageProperties properties = message.getMessageProperties();
        String contentType = properties.getContentType();
        if (CONTENT_TYPE_TASK_DATA.equals(contentType)) {
            try {
                content = KryoUtils.deserialize(message.getBody(), TaskData.class);
            } catch (Exception e) {
                throw new MessageConversionException("Failed to convert Message content. " + e.getMessage(), e);
            }
        } else {
            if (log.isWarnEnabled()) {
                try {
                    log.warn("Could not convert incoming message with content-type [{}], message: {} ",
                            contentType, new String(message.getBody(), StandardCharsets.UTF_8));
                } catch (Exception e) {
                    log.warn("Could not convert incoming message with content-type [{}],message cannot be decode. {}", contentType, e.getMessage());
                }
            }
        }
        return content;
    }


}
