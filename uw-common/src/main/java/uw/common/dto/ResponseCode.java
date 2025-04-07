package uw.common.dto;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

/**
 * 响应码定义接口。
 *
 * @author axeon
 */
public interface ResponseCode {

    /**
     * 获取配置前缀.
     *
     * @return
     */
    String configPrefix();

    /**
     * 获取响应码.
     *
     * @return
     */
    String getCode();

    /**
     * 获取响应消息.
     *
     * @return
     */
    String getMessage();

    /**
     * 获取消息源.
     *
     * @return
     */
    default MessageSource getMessageSource() {
        return null;
    }

    /**
     * 获取响应消息。
     *
     * @param params 参数数组
     * @return
     */
    default String getMessage(Object... params) {
        String message = getMessage();
        if (params == null || params.length == 0) {
            return message;
        } else {
            return String.format( message, params );
        }
    }

    /**
     * 获取I18n响应消息。
     *
     * @param locale 语言环境
     * @param params 参数数组
     * @return
     */
    default String getLocalizedMessage(Locale locale, Object... params) {
        String message = getMessage();
        if (getMessageSource() != null) {
            try {
                message = getMessageSource().getMessage( configPrefix() + '.' + getCode(), null, locale );
            } catch (Exception ignored) {
            }
        }
        if (params == null || params.length == 0) {
            return message;
        } else {
            // 防止异常
            try {
                return String.format( message, params );
            } catch (Exception ignored) {
                return message;
            }
        }
    }

    /**
     * 获取I18n响应消息。
     *
     * @param params 参数数组
     * @return
     */
    default String getLocalizedMessage(Object... params) {
        return getLocalizedMessage( LocaleContextHolder.getLocale(), params );
    }

    /**
     * 获取I18n响应消息。
     *
     * @return
     */
    default String getLocalizedMessage() {
        return getLocalizedMessage( LocaleContextHolder.getLocale(), (Object) null );
    }

}
