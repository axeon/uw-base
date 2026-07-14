package uw.common.vo;

import com.fasterxml.jackson.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import uw.common.response.ResponseCode;
import uw.common.util.EnumUtils;

import java.util.Locale;

/**
 * JSON 配置参数定义接口。
 * <p>
 * 通常以枚举形式实现，每个枚举常量描述一个配置项的类型、默认值、标题与校验正则。
 * 配合 {@link uw.common.helper.JsonConfigHelper} 构建 {@link JsonConfigBox} 进行结构化读取与校验。
 * </p>
 * <p>
 * 实现类只需提供 {@link #getParamData()}（返回 {@link ParamData} 元数据载体），{@link #getKey()} 由枚举名自动派生，
 * {@link #getType}/{@link #getValue}/{@link #getTitle}/{@link #getRegex} 由接口 default 委托 {@link #getParamData()}。
 * 其中 {@link #getTitle()} 为原始标题（标注 {@code @JsonIgnore} 不参与序列化），对外输出的标题字段走
 * {@link #getLocalizedTitle()}（标注 {@code @JsonProperty("title")}），默认按当前请求 {@link Locale} 国际化：
 * 实现类覆写 {@link #configPrefix()} 指定 i18n 资源前缀后，标题即按 {@link #getKey()} 为资源 key 解析本地化文案，
 * 未命中时回退到 {@link #getTitle()} 原始值。
 * </p>
 */
@Schema(title = "Json配置参数定义", description = "Json配置参数定义")
public interface JsonConfigParam {

    /**
     * 获取配置参数数据载体（由实现类提供元数据）。
     *
     * @return 参数数据
     */
    @JsonIgnore
    ParamData getParamData();

    /**
     * 获取配置参数名（key）。
     * <p>
     * 由枚举常量名按 {@code 大写转小写、下划线转点} 规范自动派生（如 {@code MAX_TOKENS} → {@code max.tokens}），
     * 通过将 {@code this} 强转为 {@link Enum} 访问 {@link Enum#name()}，因此实现类必须是枚举。
     * </p>
     *
     * @return 配置 key
     */
    @Schema(title = "配置名", description = "配置名")
    default String getKey() {
        return EnumUtils.enumNameToDotCase(((Enum<?>) this).name());
    }

    /**
     * 获取配置类型（数值/字符串/布尔/日期/枚举等，详见 {@link ParamType}），委托 {@link #getParamData()}。
     *
     * @return 配置类型
     */
    @Schema(title = "配置类型", description = "配置类型")
    default ParamType getType() {
        return getParamData().getType();
    }

    /**
     * 获取配置默认值（未提供实际值时使用；ENUM 类型时也可承载可选值集合），委托 {@link #getParamData()}。
     *
     * @return 默认值
     */
    @Schema(title = "配置默认值", description = "配置默认值")
    default String getValue() {
        return getParamData().getValue();
    }

    /**
     * 获取原始配置标题（兜底文案，不经 i18n 解析），委托 {@link #getParamData()}。
     * <p>
     * 作为 {@link #getLocalizedTitle()} 与 {@link #getLocalizedTitle(Locale)} 在 i18n 资源缺失时的回退值。
     * 标注 {@code @JsonIgnore} 以禁止输出到 JSON：对外暴露的标题固定走 {@link #getLocalizedTitle()}（已按 Locale 国际化），
     * 杜绝原始中文因实现类误覆写 i18n 出口而泄漏。
     * </p>
     *
     * @return 原始标题文本
     */
    @JsonIgnore
    default String getTitle() {
        return getParamData().getTitle();
    }

    /**
     * 获取配置标题（自动按当前请求 {@link Locale} 国际化），作为 JSON 输出的标题字段。
     * <p>
     * 序列化输出字段名固定为 {@code title}（由 {@link JsonProperty} 指定，与方法名解耦）。
     * 未配置 {@link #configPrefix()}、资源缺失或 Locale 为 null 时，自动回退到 {@link #getTitle()} 原始值。
     * </p>
     *
     * @return 标题文本（已按当前 Locale 解析；无 i18n 配置时返回原始值）
     */
    @JsonProperty("title")
    default String getLocalizedTitle() {
        return getLocalizedTitle(LocaleContextHolder.getLocale());
    }

    /**
     * 获取校验正则表达式（无则为 null），委托 {@link #getParamData()}。
     *
     * @return 正则表达式
     */
    @Schema(title = "正则表达式", description = "正则表达式")
    default String getRegex() {
        return getParamData().getRegex();
    }

    /**
     * 获取配置 i18n 资源前缀（逻辑前缀，非完整 basename）。
     * <p>
     * 默认返回 null（不启用 i18n，{@link #getLocalizedTitle()} 自动回退到原始值）。
     * 实现方返回前缀后，{@link #messageSource()} 即按规范推导 basename 并加载、缓存 {@link ResourceBundleMessageSource}：
     * basename 约定为 {@code i18n/config/<前缀点转下划线>}（如前缀 {@code openai.config} → {@code i18n/config/openai_config}），
     * 与 {@link ResponseCode#codePrefix()} 的推导规则对称（后者走 {@code i18n/messages/} 目录）。
     * </p>
     * <p>
     * {@link #getLocalizedTitle(Locale)} 据此按资源 key（约定为 {@link #getKey()}）与 Locale 解析本地化文案，未命中时回退原始值。
     * </p>
     *
     * @return 配置 i18n 资源前缀；null 表示不启用 i18n
     */
    @JsonIgnore
    default String configPrefix() {
        return null;
    }

    /**
     * 获取国际化消息源，用于按 {@link Locale} 解析本地化标题。
     * <p>
     * 默认按 {@link #configPrefix()} 自动加载并缓存 {@link ResourceBundleMessageSource}（UTF-8，复用
     * {@link ResponseCode#MESSAGE_SOURCE_CACHE} 缓存）。前缀为 null 时不加载，返回 null。
     * 实现方若有特殊需求（如自定义 basename 解析逻辑），可覆写本方法。
     * </p>
     *
     * @return 消息源；configPrefix 为 null 时返回 null
     */
    @JsonIgnore
    default MessageSource messageSource() {
        String prefix = configPrefix();
        if (prefix == null || prefix.isEmpty()) {
            return null;
        }
        String basename = "i18n/config/" + prefix.replace('.', '_');
        return ResponseCode.MESSAGE_SOURCE_CACHE.computeIfAbsent(basename, name -> {
            ResourceBundleMessageSource source = new ResourceBundleMessageSource();
            source.setBasename(name);
            source.setDefaultEncoding("UTF-8");
            return source;
        });
    }

    /**
     * 获取配置标题的本地化文案。
     * <p>
     * 若 {@link #messageSource()} 返回非 null（即配置了 {@link #configPrefix()}），则按 {@code locale}
     * 以 {@link #getKey()} 为资源 key 解析；解析失败、资源缺失、未配置 basename 或 locale 为 null 时回退到
     * {@link #getTitle()} 原始值。
     * </p>
     *
     * @param locale 语言环境；为 null 时回退到原始值
     * @return 本地化标题
     */
    @JsonIgnore
    default String getLocalizedTitle(Locale locale) {
        String title = getTitle();
        MessageSource source = messageSource();
        if (source == null || locale == null) {
            return title;
        }
        // 以兜底文案作为 defaultMessage，缺失资源 key 时直接返回兜底，避免依赖异常控制流。
        String localized = source.getMessage(getKey(), null, title, locale);
        return localized != null ? localized : title;
    }

    /**
     * 配置参数数据载体（不可变）。
     * <p>
     * 实现类通过 {@link #getParamData()} 返回本类的实例，承载 type/value/title/regex 四项元数据。
     * key 不在此载体中——由接口 {@link #getKey()} 从枚举名自动派生，避免实现类手写 {@code enumNameToDotCase(name())}。
     * </p>
     */
    class ParamData {

        /**
         * 配置类型。详见 {@link ParamType}。
         */
        private final ParamType type;

        /**
         * 配置默认值（ENUM 类型时也可承载可选值集合）。
         */
        private final String value;

        /**
         * 配置标题（i18n 兜底文案）。
         */
        private final String title;

        /**
         * 校验正则表达式。
         */
        private final String regex;

        /**
         * 构造参数数据。
         *
         * @param type  配置类型
         * @param value 默认值（ENUM 类型时也可承载可选值集合）
         * @param title 标题文本（i18n 兜底文案）
         * @param regex 校验正则表达式（无则传 null）
         */
        public ParamData(ParamType type, String value, String title, String regex) {
            this.type = type;
            this.value = value;
            this.title = title;
            this.regex = regex;
        }

        /**
         * @return 配置类型
         */
        public ParamType getType() {
            return type;
        }

        /**
         * @return 默认值
         */
        public String getValue() {
            return value;
        }

        /**
         * @return 标题文本
         */
        public String getTitle() {
            return title;
        }

        /**
         * @return 校验正则表达式
         */
        public String getRegex() {
            return regex;
        }
    }

    /**
     * JSON配置数据类型
     */
    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    @Schema(title = "JSON配置数据类型", description = "JSON配置数据类型")
    enum ParamType {

        /**
         * 字符串类型
         */
        STRING("string", "字符串类型"),

        /**
         * 列表字符串类型
         */
        SET_STRING("set<string>", "字符串类型集合"),

        /**
         * 字符串类型
         */
        TEXT("text", "长文本类型"),

        /**
         * 字符串类型
         */
        TEXT_RICH("textRich", "富文本类型"),

        /**
         * 数值类型
         */
        INT("int", "INT类型"),

        /**
         * 列表数值类型
         */
        SET_INT("set<int>", "INT类型集合"),

        /**
         * 数值类型
         */
        LONG("long", "LONG类型"),

        /**
         * 列表数值类型
         */
        SET_LONG("set<long>", "LONG类型集合"),

        /**
         * 布尔类型
         */
        BOOLEAN("boolean", "布尔类型"),

        /**
         * 列表布尔类型
         */
        SET_BOOLEAN("set<boolean>", "布尔类型集合"),

        /**
         * 浮点类型
         */
        FLOAT("float", "浮点类型"),

        /**
         * 列表浮点类型
         */
        SET_FLOAT("set<float>", "浮点类型集合"),

        /**
         * 双精度浮点类型
         */
        DOUBLE("double", "双精度浮点类型"),

        /**
         * 列表双精度浮点类型
         */
        SET_DOUBLE("set<double>", "双精度浮点类型集合"),

        /**
         * 日期类型
         */
        DATE("date", "日期类型"),

        /**
         * 列表日期类型
         */
        SET_DATE("set<date>", "日期类型集合"),

        /**
         * 日期类型
         */
        TIME("time", "时间类型"),

        /**
         * 列表日期类型
         */
        SET_TIME("set<time>", "时间类型集合"),

        /**
         * 日期时间类型
         */
        DATETIME("datetime", "日期时间类型"),

        /**
         * 列表日期时间类型
         */
        SET_DATETIME("set<datetime>", "日期时间类型集合"),

        /**
         * 枚举类型
         */
        ENUM("enum", "枚举类型"),

        /**
         * 列表枚举类型
         */
        SET_ENUM("set<enum>", "枚举类型集合"),

        /**
         * map类型
         */
        MAP("map", "MAP类型"),
        ;

        /**
         * 序列化输出的类型标识（如 string/int/set&lt;int&gt;）。
         */
        @JsonValue
        private final String value;

        /**
         * 类型中文描述。
         */
        private final String label;

        /**
         * 构造参数类型。
         *
         * @param value 类型标识
         * @param label 类型描述
         */
        ParamType(String value, String label) {
            this.value = value;
            this.label = label;
        }

        /**
         * 反序列化时根据 value 值解析枚举。
         *
         * @param value 类型标识
         * @return 匹配的 ParamType
         * @throws IllegalArgumentException 未知 value 时抛出
         */
        @JsonCreator
        public static ParamType fromValue(String value) {
            for (ParamType type : ParamType.values()) {
                if (type.getValue().equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown ParamType value: " + value);
        }

        /**
         * @return 类型中文描述
         */
        public String getLabel() {
            return label;
        }

        /**
         * @return 类型标识
         */
        public String getValue() {
            return value;
        }
    }
}
