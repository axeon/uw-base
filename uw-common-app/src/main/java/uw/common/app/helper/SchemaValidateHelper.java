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
 * Schema数据校验工具类。
 */
public final class SchemaValidateHelper {

    /**
     * vo类缓存。
     */
    private static final LoadingCache<Class<?>, Map<String, SchemaPropertyInfo>> CLS_PROP_CACHE = Caffeine.newBuilder().maximumSize(10000).build(SchemaValidateHelper::parsePropertyInfo);

    /**
     * 校验vo类。
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
     * 获取vo类信息。
     */
    private static Map<String, SchemaPropertyInfo> getPropertyInfo(Class<?> clz) {
        return CLS_PROP_CACHE.get(clz);
    }

    /**
     * 解析vo类信息。
     */
    private static Map<String, SchemaPropertyInfo> parsePropertyInfo(Class<?> clz) {
        Map<String, SchemaPropertyInfo> map = new LinkedHashMap<>();
        for (Field f : clz.getDeclaredFields()) {
            Schema s = f.getAnnotation(Schema.class);
            if (s == null) continue;
            f.setAccessible(true);

            double min = s.minimum().isEmpty() ? Double.NaN : Double.parseDouble(s.minimum());
            double max = s.maximum().isEmpty() ? Double.NaN : Double.parseDouble(s.maximum());

            SchemaPropertyInfo node = new SchemaPropertyInfo(f, f.getName(), s.title().isEmpty() ? s.description() : s.title(), s.requiredMode() == Schema.RequiredMode.REQUIRED, s.pattern().isEmpty() ? null : s.pattern(), min, max, s.minLength(), s.maxLength());
            map.put(f.getName(), node);
        }
        return map;
    }

    /**
     * 属性Schema信息。
     */
    static class SchemaPropertyInfo {
        /**
         * 缓存字段。
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

        public Field getField() {
            return field;
        }

        public String getName() {
            return name;
        }

        public String getTitle() {
            return title;
        }

        public boolean isRequired() {
            return required;
        }

        public String getPattern() {
            return pattern;
        }

        public double getMin() {
            return min;
        }

        public double getMax() {
            return max;
        }

        public int getMinLength() {
            return minLength;
        }

        public int getMaxLength() {
            return maxLength;
        }

        /**
         * 检查字段值。
         */
        public ValidateResult check(Object form) {
            Object value = getValue(form);
            if (required) {
                if (value == null) {
                    return new ValidateResult(name, title, ValidateResponseCode.NOT_EMPTY, null);
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
            }
            return null;
        }

        /**
         * 获取字段值。
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