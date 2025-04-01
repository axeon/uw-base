package uw.httpclient.exception;

import uw.task.exception.TaskDataException;

/**
 * 数据映射异常。
 *
 * @since 2017/9/22
 */
public class DataMapperException extends TaskDataException {

    public DataMapperException() {
        super();
    }

    public DataMapperException(String msg) {
        super( msg );
    }

    public DataMapperException(Throwable nestedThrowable) {
        super( nestedThrowable );
    }

    public DataMapperException(String msg, Throwable nestedThrowable) {
        super( msg, nestedThrowable );
    }
}
