package uw.common.dto;

/**
 * 响应码消息接口
 *
 * @author axeon
 */
public interface ResponseCode {

    /**
     * 获取响应码
     *
     * @return
     */
    String getCode();

    /**
     * 获取响应消息
     *
     * @param params
     * @return
     */
    String getMessage(Object... params);

    /**
     * 获取国际化消息
     *
     * @param params
     * @return
     */
    String getI18Message(Object... params);

    /**
     * 获取响应消息
     *
     * @return
     */
    default String getMessage() {
        return getMessage( null );
    }

    /**
     * 获取国际化消息
     *
     * @return
     */
    default String getI18Message() {
        return getI18Message( null );
    }

}
