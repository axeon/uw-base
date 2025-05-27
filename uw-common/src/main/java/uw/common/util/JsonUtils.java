package uw.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

/**
 * 基于Jackson2 的Json工具类。
 *
 * @since 2018-03-01
 */
public class JsonUtils {

    private static final com.fasterxml.jackson.databind.ObjectMapper jsonMapper = jsonMapperInit();

    /**
     * Java 泛型绑定。
     *
     * @param parametrized
     * @param parameterClasses
     * @return
     */
    public static JavaType constructParametricType(Class<?> parametrized, Class<?>... parameterClasses) {
        return jsonMapper.getTypeFactory().constructParametricType(parametrized, parameterClasses);
    }

    /**
     * 解析json字节数组为对象。
     *
     * @param data
     * @param classType
     * @param <T>
     * @return
     */
    public static <T> T parse(byte[] data, Class<T> classType) throws RuntimeException {
        try {
            return (T) jsonMapper.readValue(data, classType);
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage() + "! data: " + Arrays.toString(data), e);
        }
    }

    /**
     * 解析json字节数组为对象。
     *
     * @param data
     * @param typeRef
     * @param <T>
     * @return
     * @throws RuntimeException
     */
    public static <T> T parse(byte[] data, TypeReference<T> typeRef) throws RuntimeException {
        try {
            return (T) jsonMapper.readValue(data, typeRef);
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage() + "! data: " + Arrays.toString(data), e);
        }
    }

    /**
     * 解析json字节数组为对象。
     *
     * @param data
     * @param type
     * @param <T>
     * @return
     * @throws RuntimeException
     */
    public static <T> T parse(byte[] data, JavaType type) throws RuntimeException {
        try {
            return jsonMapper.readValue(data, type);
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage() + "! data: " + Arrays.toString(data), e);
        }
    }

    /**
     * 解析json字符串为对象。
     *
     * @param data
     * @param classType
     * @param <T>
     * @return
     */
    public static <T> T parse(String data, Class<T> classType) throws RuntimeException {
        try {
            return (T) jsonMapper.readValue(data, classType);
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage() + "! data: " + data, e);
        }
    }

    /**
     * 解析json字符串为对象。
     *
     * @param data
     * @param typeRef
     * @param <T>
     * @return
     * @throws RuntimeException
     */
    public static <T> T parse(String data, TypeReference<T> typeRef) throws RuntimeException {
        try {
            return (T) jsonMapper.readValue(data, typeRef);
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage() + "! data: " + data, e);
        }
    }

    /**
     * 解析json字符串为对象。
     *
     * @param data
     * @param type
     * @param <T>
     * @return
     * @throws RuntimeException
     */
    public static <T> T parse(String data, JavaType type) throws RuntimeException {
        try {
            return jsonMapper.readValue(data, type);
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage() + "! data: " + data, e);
        }
    }


    /**
     * 解析json输入流为对象。
     *
     * @param inputStream
     * @param classType
     * @param <T>
     * @return
     */
    public static <T> T parse(InputStream inputStream, Class<T> classType) throws RuntimeException {
        try {
            return (T) jsonMapper.readValue(inputStream, classType);
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage() + "! data: " + inputStream, e);
        }
    }

    /**
     * 解析json输入流为对象。
     *
     * @param inputStream
     * @param typeRef
     * @param <T>
     * @return
     * @throws RuntimeException
     */
    public static <T> T parse(InputStream inputStream, TypeReference<T> typeRef) throws RuntimeException {
        try {
            return (T) jsonMapper.readValue(inputStream, typeRef);
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage() + "! data: " + inputStream, e);
        }
    }

    /**
     * 解析json输入流为对象。
     *
     * @param content
     * @param type
     * @param <T>
     * @return
     * @throws RuntimeException
     */
    public static <T> T parse(InputStream content, JavaType type) throws RuntimeException {
        try {
            return jsonMapper.readValue(content, type);
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage() + "! data: " + content, e);
        }
    }

    /**
     * 转换对象。
     *
     * @param object
     * @param classType
     * @param <T>
     * @return
     */
    public static <T> T convert(Object object, Class<T> classType) throws RuntimeException {
        try {
            return (T) jsonMapper.convertValue(object, classType);
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage() + "! data: " + toString(object), e);
        }
    }

    /**
     * 转换对象。
     *
     * @param object
     * @param typeRef
     * @param <T>
     * @return
     * @throws RuntimeException
     */
    public static <T> T convert(Object object, TypeReference<T> typeRef) throws RuntimeException {
        try {
            return (T) jsonMapper.convertValue(object, typeRef);
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage() + "! data: " + toString(object), e);
        }
    }

    /**
     * 转换对象。
     *
     * @param object
     * @param type
     * @param <T>
     * @return
     * @throws RuntimeException
     */
    public static <T> T convert(Object object, JavaType type) throws RuntimeException {
        try {
            return jsonMapper.convertValue(object, type);
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage() + "! data: " + toString(object), e);
        }
    }

    /**
     * 将对象序列化写入输出流。
     *
     * @param object
     * @return
     * @throws RuntimeException
     */
    public static void write(Object object, OutputStream out) throws RuntimeException {
        try {
            jsonMapper.writeValue(out, object);
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage() + "! data: " + toString(object), e);
        }
    }


    /**
     * 将对象序列化为json字节数组。
     *
     * @param object
     * @return
     * @throws RuntimeException
     */
    public static byte[] toBytes(Object object) throws RuntimeException {
        try {
            return jsonMapper.writeValueAsBytes(object);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * 将对象序列化为json字符串。
     *
     * @param object
     * @return
     * @throws RuntimeException
     */
    public static String toString(Object object) throws RuntimeException {
        if (object instanceof String string) return string;
        try {
            return jsonMapper.writeValueAsString(object);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * 初始化jsonMapper。
     *
     * @return
     */
    private static com.fasterxml.jackson.databind.ObjectMapper jsonMapperInit() {
        com.fasterxml.jackson.databind.ObjectMapper jsonMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        jsonMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        jsonMapper.registerModule(new JavaTimeModule());
        jsonMapper.setTimeZone(TimeZone.getDefault());
        return jsonMapper;
    }

}
