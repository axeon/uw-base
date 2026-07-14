package uw.common.util;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

/**
 * 基于Jackson 3 的Json工具类。
 *
 * @since 2018-03-01
 */
public class JsonUtils {

    private static final JsonMapper jsonMapper = jsonMapperInit();

    /**
     * 初始化JsonMapper。
     *
     * @return
     */
    public static JsonMapper getJsonMapper() {
        return jsonMapper;
    }

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
            throw new RuntimeException(e.getMessage() + "! data: InputStream", e);
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
    private static JsonMapper jsonMapperInit() {
        // 设置DateUtils日期格式
        SimpleModule dateUtilModule = new SimpleModule();
        dateUtilModule.addDeserializer(Date.class, new ValueDeserializer<Date>() {
            @Override
            public Date deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
                String dateString = jsonParser.getString();
                return DateTools.stringToDate(dateString);
            }
        });
        dateUtilModule.addSerializer(Date.class, new ValueSerializer<Date>() {
            @Override
            public void serialize(Date date, JsonGenerator jsonGenerator, SerializationContext serializationContext) {
                jsonGenerator.writeString(DateTools.dateToString(date, DateTools.DATE_MILLIS_ISO));
            }
        });
        // Jackson 3 中 ObjectMapper 构建后不可变，统一通过 JsonMapper.builder() 链式配置。
        // 注意：Jackson 3 的所有 JsonGenerator/JsonParser 方法只抛出 unchecked 的 JacksonException，
        // 因此 serialize/deserialize 重写不再声明 checked IOException（父类也未声明）。
        return JsonMapper.builder()
                // 关闭未知属性报错
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                // 添加DateUtils日期格式模块
                .addModule(dateUtilModule)
                // 设置默认时区
                .defaultTimeZone(TimeZone.getDefault())
                .build();
    }

}
