package uw.httpclient.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import uw.httpclient.http.ObjectMapper;
import uw.httpclient.exception.DataMapperException;

import java.io.OutputStream;

/**
 * 基于Jackson2 的ObjectMapper
 *
 * @since 2018-03-01
 */
public class JsonObjectMapperImpl implements ObjectMapper {

    private static final com.fasterxml.jackson.databind.ObjectMapper jsonMapper = jsonMapperInit();

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
        // Java时间模块
        jsonMapper.registerModule( new JavaTimeModule() );
        jsonMapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
        return jsonMapper;
    }
}
