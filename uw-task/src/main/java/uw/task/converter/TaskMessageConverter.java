package uw.task.converter;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import uw.task.TaskData;

import java.util.*;

/**
 * 用于spring-amqp的消息转换器。
 *
 * @author axeon
 */
public class TaskMessageConverter implements MessageConverter {

    private static final Logger log = LoggerFactory.getLogger(TaskMessageConverter.class);

    /**
     * 数据类型
     */
    private static final String CONTENT_TYPE_TASK_DATA = "UT_DATA";

    /**
     * 默认池子容量32。
     */
    private static final int CAPACITY = 32;

    /**
     * kryo池。
     */
    private static final Pool<Kryo> kryoPool = new Pool<Kryo>(true, true, CAPACITY) {
        protected Kryo create() {
            Kryo kryo = new Kryo();
            kryo.setClassLoader(Thread.currentThread().getContextClassLoader());
            kryo.setRegistrationRequired(false);
            kryo.setReferences(false);
            kryo.setOptimizedGenerics(true);
            kryo.register(TaskData.class);
            // 预注册常用 JDK 类
            kryo.register(ArrayList.class);
            kryo.register(LinkedList.class);
            kryo.register(HashMap.class);
            kryo.register(LinkedHashMap.class);
            kryo.register(TreeMap.class);
            kryo.register(HashSet.class);
            kryo.register(LinkedHashSet.class);
            kryo.register(Date.class);
            return kryo;
        }
    };

    /**
     * 输出池。没有搞input pull的原因是已经拿到了完整数组，没有重用价值。
     */
    private static final Pool<Output> outputPool = new Pool<Output>(true, true, CAPACITY) {
        protected Output create() {
            return new Output(2560, -1);
        }
    };


    /**
     * Construct with an internal {@link ObjectMapper} instance. The
     * {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} is set to false
     * on the {@link ObjectMapper}.
     */
    public TaskMessageConverter() {

    }


    @Override
    public Message toMessage(Object objectToConvert, MessageProperties messageProperties) throws MessageConversionException {
        byte[] bytes = new byte[0];
        if (objectToConvert instanceof TaskData) {
            messageProperties.setContentType(CONTENT_TYPE_TASK_DATA);
            //序列化操作
            final Kryo kryo = kryoPool.obtain();
            final Output output = outputPool.obtain();
            try {
                kryo.writeClassAndObject(output, objectToConvert);
                output.flush();
                //此时复制出数据
                bytes = output.toBytes();
            } catch (Exception e) {
                throw new MessageConversionException("Failed to convert Message content. " + e.getMessage(), e);
            } finally {
                //重置output
                output.reset();
                outputPool.free(output);
                kryoPool.free(kryo);
            }
        }
        messageProperties.setContentLength(bytes.length);
        return new Message(bytes, messageProperties);
    }

    @Override
    public Object fromMessage(Message message) throws MessageConversionException {
        Object content = null;
        if (message == null || message.getBody() == null || message.getMessageProperties() == null) {
            return null;
        }
        MessageProperties properties = message.getMessageProperties();
        String contentType = properties.getContentType();
        if (CONTENT_TYPE_TASK_DATA.equals(contentType)) {
            final Kryo kryo = kryoPool.obtain();
            try (Input input = new Input(message.getBody())) {
                content = kryo.readClassAndObject(input);
            } catch (Exception e) {
                throw new MessageConversionException("Failed to convert Message content. " + e.getMessage(), e);
            } finally {
                kryoPool.free(kryo);
            }
        } else {
            if (log.isWarnEnabled()) {
                try {
                    log.warn("Could not convert incoming message with content-type [{}],message: {} ",
                            contentType, new String(message.getBody(), "UTF-8"));
                } catch (Exception e) {
                    log.warn("Could not convert incoming message with content-type [{}],message cannot be decode. {}", contentType, e.getMessage());
                }
            }
        }
        return content;
    }


}
