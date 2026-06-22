package uw.httpclient.exception;

import uw.task.exception.TaskDataException;

/**
 * 数据映射异常。
 * <p>
 * 在请求/响应对象序列化或反序列化（JSON/XML）失败时抛出，例如：
 * 响应内容格式与 {@code DataObjectMapper} 支持的格式不一致、目标类型选择错误、
 * 输入内容为非法 JSON/XML 等。
 *
 * @since 2017/9/22
 */
public class DataMapperException extends TaskDataException {

    /**
     * 构造一个不带消息和原因的数据映射异常。
     */
    public DataMapperException() {
        super();
    }

    /**
     * 构造一个带消息的数据映射异常。
     *
     * @param msg 异常消息（通常会附带原始内容便于排查）。
     */
    public DataMapperException(String msg) {
        super(msg);
    }

    /**
     * 构造一个带原因的数据映射异常。
     *
     * @param nestedThrowable 原始异常（通常是 Jackson 的 JsonProcessingException）。
     */
    public DataMapperException(Throwable nestedThrowable) {
        super(nestedThrowable);
    }

    /**
     * 构造一个带消息和原因的数据映射异常。
     *
     * @param msg             异常消息。
     * @param nestedThrowable 原始异常。
     */
    public DataMapperException(String msg, Throwable nestedThrowable) {
        super(msg, nestedThrowable);
    }
}
