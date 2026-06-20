package uw.common.app.helper;


import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.swagger.v3.oas.annotations.media.Schema;
import uw.common.app.constant.ValidateResponseCode;
import uw.common.app.vo.ValidateResult;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 {@link Schema} 注解的数据校验工具类。
 * <p>
 * 解析 VO 类（含父类）字段的 {@code @Schema} 约束（必填、数值范围、字符串长度、正则），
 * 返回所有校验失败的 {@link ValidateResult} 列表。反射元数据通过 Caffeine 缓存以提升性能。
 * </p>
 * 使用示例：
 * <pre>{@code
 * List<ValidateResult> errors = SchemaValidateHelper.validate(form);
 * if (!errors.isEmpty()) {
 *     return ResponseData.error(errors, "", "数据校验失败！");
 * }
 * }</pre>
 */
public final class SchemaValidateHelper {

    /**
     * 私有构造器，禁止实例化。
     */
    private SchemaValidateHelper() {
    }

    /**
     * VO 类的 @Schema 属性元数据缓存（key 为类，value 为 字段名→属性信息）。
     */
    private static final LoadingCache<Class<?>, Map<String, SchemaPropertyInfo>> CLS_PROP_CACHE = Caffeine.newBuilder().maximumSize(10000).build(SchemaValidateHelper::parsePropertyInfo);

    /**
     * 校验 VO 对象的所有 {@code @Schema} 约束。
     *
     * @param form 待校验对象
     * @return 校验失败结果列表；全部通过时返回空列表
     */
    public static List<ValidateResult> validate(Object form) {
        Map<String, SchemaPropertyInfo> map = getPropertyInfo(form.getClass());
        List<ValidateResult> list = new ArrayList<>();
        for (SchemaPropertyInfo prop : map.values()) {
            ValidateResult result = prop.check(form);
            if (result != null) {
                list.add(result);
            }
        }
        return list;
    }

    /**
     * 从缓存获取 VO 类的属性元数据。
     *
     * @param clz VO 类
     * @return 字段名→属性信息
     */
    private static Map<String, SchemaPropertyInfo> getPropertyInfo(Class<?> clz) {
        return CLS_PROP_CACHE.get(clz);
    }

    /**
     * 解析 VO 类的 @Schema 属性元数据。
     * <p>
     * 遍历整个继承链（子类优先），收集带 {@link Schema} 注解的字段；同名子类字段覆盖父类字段。
     * 结果保持声明顺序（{@link LinkedHashMap}）。
     * </p>
     *
     * @param clz VO 类
     * @return 字段名→属性信息
     */
    private static Map<String, SchemaPropertyInfo> parsePropertyInfo(Class<?> clz) {
        Map<String, SchemaPropertyInfo> map = new LinkedHashMap<>();
        Class<?> current = clz;
        while (current != null && current != Object.class) {
            for (Field f : current.getDeclaredFields()) {
                // 子类已覆盖同名字段时跳过父类，保留子类定义
                if (map.containsKey(f.getName())) {
                    continue;
                }
                Schema s = f.getAnnotation(Schema.class);
                if (s == null) continue;
                f.setAccessible(true);

                double min = parseDoubleSafe(s.minimum());
                double max = parseDoubleSafe(s.maximum());

                SchemaPropertyInfo node = new SchemaPropertyInfo(f, f.getName(), s.title().isEmpty() ? s.description() : s.title(), s.requiredMode() == Schema.RequiredMode.REQUIRED, s.pattern().isEmpty() ? null : s.pattern(), min, max, s.minLength(), s.maxLength());
                map.put(f.getName(), node);
            }
            current = current.getSuperclass();
        }
        return map;
    }

    /**
     * 安全解析 Schema 注解中的数值边界。
     *
     * @param value 边界字符串
     * @return 解析结果；空值或非法值返回 {@link Double#NaN}（表示不校验）
     */
    private static double parseDoubleSafe(String value) {
        if (value == null || value.isEmpty()) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    /**
     * 属性 Schema 信息（单字段的校验元数据，缓存复用）。
     */
    static class SchemaPropertyInfo {
        /**
         * 反射字段（缓存复用，避免重复解析）。
         */
        private final Field field;          // 直接带缓存
        /**
         * 字段名。
         */
        private final String name;
        /**
         * 字段描述。
         */
        private final String title;
        /**
         * 是否必填。
         */
        private final boolean required;
        /**
         * 字段正则表达式。
         */
        private final String pattern;
        /**
         * 字段最小值。
         */
        private final double min;
        /**
         * 字段最大值。
         */
        private final double max;
        /**
         * 字段最小长度。
         */
        private final int minLength;
        /**
         * 字段最大长度。
         */
        private final int maxLength;

        /**
         * 构造属性校验元数据。
         *
         * @param field     反射字段
         * @param name      字段名
         * @param title     字段描述
         * @param required  是否必填
         * @param pattern   正则表达式（无则传 null）
         * @param min       最小值（NaN 表示不限）
         * @param max       最大值（NaN 表示不限）
         * @param minLength 最小长度（0 表示不限）
         * @param maxLength 最大长度（0 表示不限）
         */
        public SchemaPropertyInfo(Field field, String name, String title, boolean required, String pattern, double min, double max, int minLength, int maxLength) {
            this.field = field;
            this.name = name;
            this.title = title;
            this.required = required;
            this.pattern = pattern;
            this.min = min;
            this.max = max;
            this.minLength = minLength;
            this.maxLength = maxLength;
        }

        /**
         * @return 反射字段
         */
        public Field getField() {
            return field;
        }

        /**
         * @return 字段名
         */
        public String getName() {
            return name;
        }

        /**
         * @return 字段描述
         */
        public String getTitle() {
            return title;
        }

        /**
         * @return 是否必填
         */
        public boolean isRequired() {
            return required;
        }

        /**
         * @return 正则表达式
         */
        public String getPattern() {
            return pattern;
        }

        /**
         * @return 最小值（NaN 表示不限）
         */
        public double getMin() {
            return min;
        }

        /**
         * @return 最大值（NaN 表示不限）
         */
        public double getMax() {
            return max;
        }

        /**
         * @return 最小长度（0 表示不限）
         */
        public int getMinLength() {
            return minLength;
        }

        /**
         * @return 最大长度（0 表示不限）
         */
        public int getMaxLength() {
            return maxLength;
        }

        /**
         * 按本属性元数据校验目标对象中对应字段的值。
         *
         * @param form 待校验对象
         * @return 校验失败结果；通过时返回 null
         */
        public ValidateResult check(Object form) {
            Object value = getValue(form);
            if (value == null) {
                if (required) {
                    return new ValidateResult(name, title, ValidateResponseCode.NOT_EMPTY, null);
                }
                return null;
            }
            if (value instanceof Number num) {
                double d = num.doubleValue();
                if (!Double.isNaN(min) && d < min) {
                    return new ValidateResult(name, title, ValidateResponseCode.VALUE_TOO_SMALL, String.valueOf(min));
                }
                if (!Double.isNaN(max) && d > max) {
                    return new ValidateResult(name, title, ValidateResponseCode.VALUE_TOO_LARGE, String.valueOf(max));
                }
            }
            String str = String.valueOf(value);
            if ((minLength > 0 && str.length() < minLength)) {
                return new ValidateResult(name, title, ValidateResponseCode.LENGTH_TOO_SHORT, String.valueOf(minLength));
            }
            if (maxLength > 0 && str.length() > maxLength) {
                return new ValidateResult(name, title, ValidateResponseCode.LENGTH_TOO_LONG, String.valueOf(maxLength));
            }
            if (pattern != null && !str.matches(pattern)) {
                return new ValidateResult(name, title, ValidateResponseCode.DATA_FORMAT_ERROR, pattern);
            }
            return null;
        }

        /**
         * 反射获取目标对象中本字段的值。
         *
         * @param form 目标对象
         * @return 字段值
         */
        public Object getValue(Object form) {
            try {
                return field.get(form);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

}