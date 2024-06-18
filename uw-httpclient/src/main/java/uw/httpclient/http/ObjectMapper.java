package uw.httpclient.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import uw.httpclient.exception.DataMapperException;

import java.io.OutputStream;

/**
 * 对象Mapper
 *
 * 
 * @since 2017/9/20
 */
public interface ObjectMapper {

    /**
     * Java 泛型绑定
     *
     * @param parametrized
     * @param parameterClasses
     * @return
     */
    JavaType constructParametricType(Class<?> parametrized, Class<?>... parameterClasses);

    /**
     * 解析
     *
     * @param content
     * @param cls
     * @param <T>
     * @return
     */
    <T> T parse(String content, Class<T> cls) throws DataMapperException;

    /**
     * 解析
     *
     * @param content
     * @param typeRef
     * @param <T>
     * @return
     */
    <T> T parse(String content, TypeReference<T> typeRef) throws DataMapperException;

    /**
     * 解析
     *
     * @param content
     * @param type
     * @param <T>
     * @return
     * @throws DataMapperException
     */
    <T> T parse(String content, JavaType type) throws DataMapperException;

    /**
     * 将Json写入流
     *
     * @param out
     * @param value
     * @throws DataMapperException
     */
    void write(OutputStream out, Object value) throws DataMapperException;

    /**
     * 转Json
     *
     * @param object
     * @return
     */
    String toString(Object object) throws DataMapperException;

}
