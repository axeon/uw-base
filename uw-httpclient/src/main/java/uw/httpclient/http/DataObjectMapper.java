package uw.httpclient.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import uw.httpclient.exception.DataMapperException;

import java.io.OutputStream;

/**
 * 对象映射器接口。
 * <p>
 * 抽象请求/响应对象的序列化与反序列化能力，屏蔽底层实现（JSON/XML 等）。
 * {@code HttpInterface} 通过此接口完成请求体序列化与响应体反序列化，
 * 由 {@code JsonObjectMapperImpl} / {@code XmlObjectMapperImpl} 提供默认实现。
 *
 * @since 2017/9/20
 */
public interface DataObjectMapper {

    /**
     * 构造参数化类型（泛型绑定），用于复杂的泛型响应解析。
     *
     * @param parametrized     容器类型，如 List/Map。
     * @param parameterClasses 泛型实参。
     * @return 构造出的 JavaType。
     */
    JavaType constructParametricType(Class<?> parametrized, Class<?>... parameterClasses);

    /**
     * 将字符串内容解析为指定类型的对象。
     *
     * @param content 字符串内容，为 null 时返回 null。
     * @param cls     目标类型。
     * @param <T>     目标类型。
     * @return 反序列化后的对象。
     * @throws DataMapperException 解析失败时抛出。
     */
    <T> T parse(String content, Class<T> cls) throws DataMapperException;

    /**
     * 将字符串内容按 {@link TypeReference} 指定的泛型类型解析。
     *
     * @param content 字符串内容，为 null 时返回 null。
     * @param typeRef 泛型类型引用。
     * @param <T>     目标类型。
     * @return 反序列化后的对象。
     * @throws DataMapperException 解析失败时抛出。
     */
    <T> T parse(String content, TypeReference<T> typeRef) throws DataMapperException;

    /**
     * 将字符串内容按 {@link JavaType} 指定的类型解析。
     *
     * @param content 字符串内容，为 null 时返回 null。
     * @param type    目标 JavaType。
     * @param <T>     目标类型。
     * @return 反序列化后的对象。
     * @throws DataMapperException 解析失败时抛出。
     */
    <T> T parse(String content, JavaType type) throws DataMapperException;

    /**
     * 将对象序列化写入输出流。
     *
     * @param out   输出流。
     * @param value 待序列化对象。
     * @throws DataMapperException 序列化失败时抛出。
     */
    void write(OutputStream out, Object value) throws DataMapperException;

    /**
     * 将对象序列化为字符串。
     *
     * @param object 待序列化对象；若已是 String 则直接返回。
     * @return 序列化后的字符串。
     * @throws DataMapperException 序列化失败时抛出。
     */
    String toString(Object object) throws DataMapperException;

}
