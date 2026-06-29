package uw.common.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import uw.common.util.EnumUtils;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 响应码定义接口。
 * <p>
 * 约定以枚举形式实现：每个枚举常量仅需提供 {@link #getMessage()}（兜底文案）与 {@link #codePrefix()}。
 * 完整响应码（{@link #getCode()}）由接口自动组装，对外消息走 {@link #getLocalizedMessage(Object...)}（按当前 Locale 国际化），
 * 并按 {@link #codePrefix()} 自动加载国际化资源（basename 形如 {@code i18n/messages/uw_validate}）。
 * </p>
 *
 * @author axeon
 */
public interface ResponseCode {

    /**
     * 按 codePrefix 自动加载的消息源缓存。
     * <p>
     * key 为 basename（如 {@code i18n/messages/uw_validate}），value 为对应的 {@link ResourceBundleMessageSource}。
     * 多个枚举常量共享同一 prefix 时复用同一实例，避免重复加载资源包。
     * </p>
     */
    Map<String, MessageSource> MESSAGE_SOURCE_CACHE = new ConcurrentHashMap<>();

    /**
     * 获取消息源，用于按 {@link Locale} 解析本地化文案。
     * <p>
     * 默认按 {@link #codePrefix()} 自动加载：basename 约定为 {@code i18n/messages/<codePrefix 点转下划线>}，
     * 资源包按 UTF-8 编码加载并缓存。{@link #codePrefix()} 为 null 时不加载（返回 null）。
     * 实现方若有特殊 basename 需求，可覆写本方法自行提供。
     * </p>
     *
     * @return 消息源；codePrefix 为 null 时返回 null
     */
    @JsonIgnore
    default MessageSource messageSource() {
        String prefix = codePrefix();
        if (prefix == null || prefix.isEmpty()) {
            return null;
        }
        String basename = "i18n/messages/" + prefix.replace('.', '_');
        return MESSAGE_SOURCE_CACHE.computeIfAbsent(basename, name -> {
            ResourceBundleMessageSource source = new ResourceBundleMessageSource();
            source.setBasename(name);
            source.setDefaultEncoding("UTF-8");
            return source;
        });
    }

    /**
     * 获取兜底消息（未经 i18n 解析的默认文案）。
     * <p>
     * 这是实现类唯一必须提供的内容：通常返回枚举构造时存入的字段值（如 {@code "不能为空"}）。
     * 标注 {@code @JsonIgnore} 以禁止输出到 JSON：对外暴露的消息固定走 {@link #getLocalizedMessage(Object...)}
     * （已按 Locale 国际化），杜绝原始文案因实现类误覆写 i18n 出口而泄漏。
     * </p>
     *
     * @return 兜底消息文案
     */
    @JsonIgnore
    String getMessage();

    /**
     * 获取响应码前缀。
     * <p>
     * 与短码拼接构成完整响应码（如 {@code uw.validate.not.null}），同时作为 i18n basename 的推导依据
     * （{@code uw.validate} → {@code i18n/messages/uw_validate}）。返回 null 时不拼接前缀、不加载资源。
     * </p>
     *
     * @return 响应码前缀；null 表示无前缀
     */
    @JsonIgnore
    default String codePrefix() {
        return null;
    }

    /**
     * 获取短码（由枚举常量名派生，不经前缀拼接）。
     * <p>
     * 将枚举常量名按 {@code 大写转小写、下划线转点} 规范转换，如 {@code NOT_NULL} → {@code not.null}。
     * 通过将 {@code this} 强转为 {@link Enum} 访问 {@link Enum#name()}，因此实现类必须是枚举。
     * </p>
     *
     * @return 短码
     */
    @JsonIgnore
    default String code() {
        return EnumUtils.enumNameToDotCase(((Enum<?>) this).name());
    }

    /**
     * 获取完整响应码（对外暴露，序列化输出）。
     * <p>
     * {@link #codePrefix()} 非空时返回 {@code 前缀.短码}（如 {@code uw.validate.not.null}），否则返回短码本身。
     * </p>
     *
     * @return 完整响应码
     */
    default String getCode() {
        String prefix = codePrefix();
        String rawCode = code();
        return prefix != null ? prefix + '.' + rawCode : rawCode;
    }

    /**
     * 获取响应消息（自动按当前请求 {@link Locale} 国际化），作为 JSON 输出的消息字段。
     * <p>
     * 序列化输出字段名固定为 {@code message}（由 {@link JsonProperty} 指定，与方法名解耦）。
     * Jackson 仅识别严格无参方法为 getter，故本方法必须无参（变参重载不参与序列化）。
     * 有匹配的 i18n 资源时返回本地化文案，否则回退到 {@link #getMessage()} 兜底文案。
     * </p>
     *
     * @return 响应消息（已按当前 Locale 解析）
     */
    @JsonProperty("message")
    default String getLocalizedMessage() {
        return getLocalizedMessage(LocaleContextHolder.getLocale());
    }

    /**
     * 获取响应消息（自动按当前请求 {@link Locale} 国际化），支持格式化参数。
     * <p>
     * 供业务代码带参调用（如填充 {@code %s} 占位符）。由于带参数，不参与 JSON 序列化——
     * 序列化固定走无参 {@link #getLocalizedMessage()}。
     * </p>
     *
     * @param params 格式化参数数组（可选，无参时直接返回解析后的文案）
     * @return 响应消息（已按当前 Locale 解析）
     */
    @JsonIgnore
    default String getLocalizedMessage(Object... params) {
        return getLocalizedMessage(LocaleContextHolder.getLocale(), params);
    }

    /**
     * 获取响应消息的本地化文案。
     * <p>
     * 若 {@link #messageSource()} 返回非 null，则按 {@code locale} 以 {@link #code()} 为资源 key 解析；
     * 解析失败、资源缺失、未配置消息源时回退到 {@link #getMessage()} 兜底文案。
     * </p>
     *
     * @param locale 语言环境
     * @param params 格式化参数数组（可选）
     * @return 本地化响应消息
     */
    @JsonIgnore
    default String getLocalizedMessage(Locale locale, Object... params) {
        String message = getMessage();
        MessageSource source = messageSource();
        if (source != null) {
            try {
                message = source.getMessage(code(), null, message, locale);
            } catch (Exception ignored) {
            }
        }
        if (params == null || params.length == 0) {
            return message;
        }
        try {
            return String.format(message, params);
        } catch (Exception ignored) {
            return message;
        }
    }
}