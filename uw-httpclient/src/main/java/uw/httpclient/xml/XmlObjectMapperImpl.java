package uw.httpclient.xml;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import uw.common.util.DateTools;
import uw.httpclient.exception.DataMapperException;
import uw.httpclient.http.DataObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.TimeZone;

/**
 * 基于 Jackson XML 的 {@link DataObjectMapper} XML 实现。
 * <p>
 * 内部持有一个全局共享的 {@link com.fasterxml.jackson.dataformat.xml.XmlMapper}，预配置：
 * 关闭未知属性报错、关闭日期时间戳、注册 JSR310 模块、按统一 ISO 毫秒格式序列化 {@link Date}。
 *
 * @since 2018-03-01
 */
public class XmlObjectMapperImpl implements DataObjectMapper {

    /**
     * 共享的 Jackson XmlMapper 实例，由 {@link #xmlMapperInit()} 初始化。
     */
    private final static com.fasterxml.jackson.dataformat.xml.XmlMapper xmlMapper = xmlMapperInit();

    /**
     * {@inheritDoc}
     */
    @Override
    public JavaType constructParametricType(Class<?> parametrized, Class<?>... parameterClasses) {
        return xmlMapper.getTypeFactory().constructParametricType(parametrized, parameterClasses);
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
            return (T) xmlMapper.readValue(content, classType);
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
            return (T) xmlMapper.readValue(content, typeRef);
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
            return xmlMapper.readValue(content, type);
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
            xmlMapper.writeValue(out, value);
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
            return xmlMapper.writeValueAsString(object);
        } catch (Throwable t) {
            throw new DataMapperException(t);
        }
    }

    /**
     * 初始化并返回预配置的 Jackson XmlMapper。
     *
     * @return 预配置的 XmlMapper。
     */
    private static com.fasterxml.jackson.dataformat.xml.XmlMapper xmlMapperInit() {
        com.fasterxml.jackson.dataformat.xml.XmlMapper xmlMapper = new com.fasterxml.jackson.dataformat.xml.XmlMapper();
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        xmlMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        // 设置日期格式
        SimpleModule dateUtilModule = new SimpleModule();
        dateUtilModule.addDeserializer(Date.class, new JsonDeserializer<Date>() {
            @Override
            public Date deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
                String dateString = jsonParser.getText();
                return DateTools.stringToDate(dateString);
            }
        });
        dateUtilModule.addSerializer(Date.class, new JsonSerializer<Date>() {
            @Override
            public void serialize(Date date, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JacksonException {
                jsonGenerator.writeString(DateTools.dateToString(date, DateTools.DATE_MILLIS_ISO));
            }
        });
        xmlMapper.registerModule(dateUtilModule);
        xmlMapper.setTimeZone(TimeZone.getDefault());
        xmlMapper.registerModule(new JavaTimeModule());
        return xmlMapper;
    }
}
