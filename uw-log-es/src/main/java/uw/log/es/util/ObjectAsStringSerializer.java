package uw.log.es.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.util.JsonUtils;

import java.io.IOException;

/**
 * Json 对象序列化成字符串而非嵌套 Json 对象。
 * <p>主要用于序列化日志对象内嵌套对象：将其整体序列化为 JSON 字符串后写入字段值，
 * 序列化失败时降级为空串（避免产出含未转义引号的非法 JSON 片段破坏整条记录）。
 *
 * @param <T> 被序列化对象类型
 */
public class ObjectAsStringSerializer<T> extends JsonSerializer<T> {

    private static final Logger logger = LoggerFactory.getLogger(ObjectAsStringSerializer.class);

    /**
     * 将对象序列化为 JSON 字符串写入生成器。
     *
     * @param value      待序列化对象，为 {@code null} 时写出 JSON null
     * @param gen        JSON 生成器
     * @param serializers 序列化上下文
     * @throws IOException 写出异常
     */
    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        try {
            gen.writeString(JsonUtils.toString(value));
        } catch (Exception e) {
            logger.error("ObjectAsStringSerializer serialize failed for [{}]", value.getClass().getName(), e);
            //写空串而非toString()，避免产出含未转义引号的非法JSON片段破坏整条记录
            gen.writeString("");
        }
    }
}
