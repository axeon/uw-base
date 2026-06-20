package uw.httpclient.json;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import uw.common.util.DateUtils;
import uw.httpclient.exception.DataMapperException;
import uw.httpclient.http.DataObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.TimeZone;

/**
 * 基于 Jackson2 的 {@link DataObjectMapper} JSON 实现。
 * <p>
 * 内部持有一个全局共享的 {@link ObjectMapper}，预配置：
 * 关闭未知属性报错、关闭日期时间戳、注册 JDK8/JSR310/参数名模块、
 * 按统一 ISO 毫秒格式序列化 {@link Date}、使用系统默认时区。
 *
 * @since 2018-03-01
 */
public class JsonObjectMapperImpl implements DataObjectMapper {

    /**
     * 共享的 Jackson ObjectMapper 实例，由 {@link #jsonMapperInit()} 初始化。
     */
    private static final com.fasterxml.jackson.databind.ObjectMapper jsonMapper = jsonMapperInit();

    /**
     * 获取共享的 Jackson ObjectMapper。
     *
     * @return Jackson ObjectMapper。
     */
    public static ObjectMapper getJsonMapper() {
        return jsonMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JavaType constructParametricType(Class<?> parametrized, Class<?>... parameterClasses) {
        return jsonMapper.getTypeFactory().constructParametricType(parametrized, parameterClasses);
    }

    /**
     * {@inheritDoc}
     * <p>当目标类型为 String 时直接透传；content 为 null 时返回 null。</p>
     */
    @Override
    public <T> T parse(String content, Class<T> classType) throws DataMapperException {
        if (classType == String.class) return (T) content;
        if (content == null) return null;
        try {
            return (T) jsonMapper.readValue(content, classType);
        } catch (Exception e) {
            throw new DataMapperException(e.getMessage() + ",data: " + content, e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>content 为 null 时返回 null。</p>
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T parse(String content, TypeReference<T> typeRef) throws DataMapperException {
        if (content == null) return null;
        try {
            return (T) jsonMapper.readValue(content, typeRef);
        } catch (Exception e) {
            throw new DataMapperException(e.getMessage() + ",data: " + content, e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>content 为 null 时返回 null。</p>
     */
    @Override
    public <T> T parse(String content, JavaType type) throws DataMapperException {
        if (content == null) return null;
        try {
            return jsonMapper.readValue(content, type);
        } catch (Exception e) {
            throw new DataMapperException(e.getMessage() + ",data: " + content, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(OutputStream out, Object value) throws DataMapperException {
        try {
            jsonMapper.writeValue(out, value);
        } catch (Exception e) {
            throw new DataMapperException(e.getMessage() + ",data: " + value, e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>若对象已是 String 则直接返回。</p>
     */
    @Override
    public String toString(Object object) throws DataMapperException {
        if (object instanceof String string) return string;
        try {
            return jsonMapper.writeValueAsString(object);
        } catch (Throwable t) {
            throw new DataMapperException(t);
        }
    }

    /**
     * 初始化并返回预配置的 Jackson ObjectMapper。
     *
     * @return 预配置的 ObjectMapper。
     */
    private static com.fasterxml.jackson.databind.ObjectMapper jsonMapperInit() {
        com.fasterxml.jackson.databind.ObjectMapper jsonMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        // 关闭未知属性报错
        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 关闭时间戳输出
        jsonMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        // 添加JDK8模块
        jsonMapper.registerModule(new Jdk8Module());
        // 添加JSR310时间模块
        jsonMapper.registerModule(new JavaTimeModule());
        // 添加参数名模块
        jsonMapper.registerModule(new ParameterNamesModule());
        // 设置DateUtils日期格式
        SimpleModule dateUtilModule = new SimpleModule();
        dateUtilModule.addDeserializer(Date.class, new JsonDeserializer<Date>() {
            @Override
            public Date deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
                String dateString = jsonParser.getText();
                return DateUtils.stringToDate(dateString);
            }
        });
        dateUtilModule.addSerializer(Date.class, new JsonSerializer<Date>() {
            @Override
            public void serialize(Date date, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JacksonException {
                jsonGenerator.writeString(DateUtils.dateToString(date, DateUtils.DATE_MILLIS_ISO));
            }
        });
        jsonMapper.registerModule(dateUtilModule);
        jsonMapper.setTimeZone(TimeZone.getDefault());
        return jsonMapper;
    }
}
