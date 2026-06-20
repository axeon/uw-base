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

    public DataMapperException() {
        super();
    }

    public DataMapperException(String msg) {
        super(msg);
    }

    public DataMapperException(Throwable nestedThrowable) {
        super(nestedThrowable);
    }

    public DataMapperException(String msg, Throwable nestedThrowable) {
        super(msg, nestedThrowable);
    }
}
