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
     * 获取响应码.
     *
     */
    String getCode();

    /**
     * 获取响应消息.
     *
     */
    String getMessage();

    /**
     * 获取消息源.
     *
     */
    default MessageSource getMessageSource() {
        return null;
    }

    /**
     * 获取配置前缀.
     *
     */
    default String codePrefix() {
        return null;
    }

    /**
     * 获取响应消息。
     *
     * @param params 参数数组
     */
    default String getMessage(Object... params) {
        String message = getMessage();
        if (params == null || params.length == 0) {
            return message;
        } else {
            return String.format(message, params);
        }
    }

    /**
     * 获取完整响应码。
     *
     */
    default String getFullCode() {
        return getFullCode(getCode());
    }

    /**
     * 获取完整响应码。
     *
     */
    default String getFullCode(String code) {
        if (codePrefix() != null) {
            return codePrefix() + '.' + code;
        } else {
            return code;
        }
    }

    /**
     * 获取I18n响应消息。
     *
     * @param locale 语言环境
     * @param params 参数数组
     */
    default String getLocalizedMessage(Locale locale, Object... params) {
        String message = getMessage();
        if (getMessageSource() != null) {
            try {
                message = getMessageSource().getMessage(getCode(), null, locale);
            } catch (Exception ignored) {
            }
        }
        if (params == null || params.length == 0) {
            return message;
        } else {
            // 防止异常
            try {
                return String.format(message, params);
            } catch (Exception ignored) {
                return message;
            }
        }
    }

    /**
     * 获取I18n响应消息。
     *
     * @param params 参数数组
     */
    default String getLocalizedMessage(Object... params) {
        return getLocalizedMessage(LocaleContextHolder.getLocale(), params);
    }

    /**
     * 获取I18n响应消息。
     *
     */
    default String getLocalizedMessage() {
        return getLocalizedMessage(LocaleContextHolder.getLocale());
    }

}
