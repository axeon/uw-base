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
 * 基于Jackson2 的ObjectMapper
 *
 * @since 2018-03-01
 */
public class JsonObjectMapperImpl implements DataObjectMapper {

    private static final com.fasterxml.jackson.databind.ObjectMapper jsonMapper = jsonMapperInit();

    /**
     * 初始化JsonMapper。
     *
     * @return
     */
    public static ObjectMapper getJsonMapper() {
        return jsonMapper;
    }

    /**
     * Java 泛型绑定
     *
     * @param parametrized
     * @param parameterClasses
     * @return
     */
    @Override
    public JavaType constructParametricType(Class<?> parametrized, Class<?>... parameterClasses) {
        return jsonMapper.getTypeFactory().constructParametricType( parametrized, parameterClasses );
    }

    @Override
    public <T> T parse(String content, Class<T> classType) throws DataMapperException {
        if (classType == String.class) return (T) content;
        try {
            return (T) jsonMapper.readValue( content, classType );
        } catch (Exception e) {
            throw new DataMapperException( e.getMessage() + ",data: " + content, e );
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T parse(String content, TypeReference<T> typeRef) throws DataMapperException {
        try {
            return (T) jsonMapper.readValue( content, typeRef );
        } catch (Exception e) {
            throw new DataMapperException( e.getMessage() + ",data: " + content, e );
        }
    }

    @Override
    public <T> T parse(String content, JavaType type) throws DataMapperException {
        try {
            return jsonMapper.readValue( content, type );
        } catch (Exception e) {
            throw new DataMapperException( e.getMessage() + ",data: " + content, e );
        }
    }

    @Override
    public void write(OutputStream out, Object value) throws DataMapperException {
        try {
            jsonMapper.writeValue( out, value );
        } catch (Exception e) {
            throw new DataMapperException( e.getMessage() + ",data: " + value, e );
        }
    }

    @Override
    public String toString(Object object) throws DataMapperException {
        if (object instanceof String string) return string;
        try {
            return jsonMapper.writeValueAsString( object );
        } catch (Throwable t) {
            throw new DataMapperException( t );
        }
    }

    /**
     * 初始化jsonMapper
     *
     * @return
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
