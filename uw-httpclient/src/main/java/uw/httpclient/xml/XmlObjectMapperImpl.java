package uw.httpclient.xml;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import uw.common.util.DateUtils;
import uw.httpclient.exception.DataMapperException;
import uw.httpclient.http.DataObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.TimeZone;

/**
 * 基于Jackson Xml 的ObjectMapper
 *
 * @since 2018-03-01
 */
public class XmlObjectMapperImpl implements DataObjectMapper {

    private final static com.fasterxml.jackson.dataformat.xml.XmlMapper xmlMapper = xmlMapperInit();

    /**
     * Java 泛型绑定
     *
     * @param parametrized
     * @param parameterClasses
     * @return
     */
    @Override
    public JavaType constructParametricType(Class<?> parametrized, Class<?>... parameterClasses) {
        return xmlMapper.getTypeFactory().constructParametricType( parametrized, parameterClasses );
    }

    @Override
    public <T> T parse(String content, Class<T> classType) throws DataMapperException {
        if (classType == String.class) return (T) content;
        try {
            return (T) xmlMapper.readValue( content, classType );
        } catch (Exception e) {
            throw new DataMapperException( e.getMessage() + ",data: " + content, e );
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T parse(String content, TypeReference<T> typeRef) throws DataMapperException {
        try {
            return (T) xmlMapper.readValue( content, typeRef );
        } catch (Exception e) {
            throw new DataMapperException( e.getMessage() + ",data: " + content, e );
        }
    }

    @Override
    public <T> T parse(String content, JavaType type) throws DataMapperException {
        try {
            return xmlMapper.readValue( content, type );
        } catch (Exception e) {
            throw new DataMapperException( e.getMessage() + ",data: " + content, e );
        }
    }

    @Override
    public void write(OutputStream out, Object value) throws DataMapperException {
        try {
            xmlMapper.writeValue( out, value );
        } catch (Exception e) {
            throw new DataMapperException( e.getMessage() + ",data: " + value, e );
        }
    }

    @Override
    public String toString(Object object) throws DataMapperException {
        if (object instanceof String string) return string;
        try {
            return xmlMapper.writeValueAsString( object );
        } catch (Throwable t) {
            throw new DataMapperException( t );
        }
    }

    /**
     * 初始化xmlMapper
     *
     * @return
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
                return DateUtils.stringToDate(dateString);
            }
        });
        dateUtilModule.addSerializer(Date.class, new JsonSerializer<Date>() {
            @Override
            public void serialize(Date date, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JacksonException {
                jsonGenerator.writeString(DateUtils.dateToString(date, DateUtils.DATE_MILLIS_ISO));
            }
        });
        xmlMapper.registerModule(dateUtilModule);
        xmlMapper.setTimeZone(TimeZone.getDefault());
        xmlMapper.registerModule(new JavaTimeModule());
        return xmlMapper;
    }
}
