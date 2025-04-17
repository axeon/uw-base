package uw.common.util;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.reflections.Reflections;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Enum的工具类。
 */
public class EnumUtils {

    /**
     * enum数据缓存。
     */
    private static final LoadingCache<String, Map<String, Object>> enumCache = Caffeine.newBuilder().build( basePackage -> {
        Map<String, Object> enumMap = new HashMap<>();
        Reflections reflections = new Reflections(basePackage);
        Set<Class<? extends Enum>> enumCls = reflections.getSubTypesOf(Enum.class);
        enumCls.forEach(m -> {
            enumMap.put(m.getSimpleName(), m.getEnumConstants());
        });
        return enumMap;
    });

    /**
     * 根据基础包名，获取此包下所有的Enum数据Map。
     * @param basePackage
     * @return
     */
    public static Map<String, Object> getEnumMap(String basePackage){
        return enumCache.get(basePackage);
    }

    /**
     * 将枚举名称转换为小写点分隔形式。
     * @param input
     * @return
     */
    public static String enumNameToDotCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        char[] chars = input.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '_') {
                chars[i] = '.'; // 将下划线替换为点
            } else if (Character.isUpperCase(c)) {
                chars[i] = Character.toLowerCase(c); // 将大写字母转为小写
            }
        }

        return new String(chars);
    }

    /**
     * 将枚举名称转换为小写连字符分隔形式。
     * @param input
     * @return
     */
    public static String enumNameToHyphenCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        char[] chars = input.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '_') {
                chars[i] = '-'; // 将下划线替换为点
            } else if (Character.isUpperCase(c)) {
                chars[i] = Character.toLowerCase(c); // 将大写字母转为小写
            }
        }

        return new String(chars);
    }

    /**
     * 将枚举名称转换为驼峰命名形式。
     * @param input
     * @return
     */
    public static String enumNameToCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        char[] chars = input.toCharArray();
        boolean nextCharToUpper = false;
        int writeIndex = 0; // 用于记录当前写入的位置

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            if (c == '_') {
                // 遇到下划线，跳过它，并将下一个字符转为大写
                nextCharToUpper = true;
            } else {
                if (nextCharToUpper) {
                    // 将下划线后的字符转为大写
                    chars[writeIndex] = Character.toUpperCase(c);
                    nextCharToUpper = false;
                } else {
                    // 其他字符转为小写
                    chars[writeIndex] = Character.toLowerCase(c);
                }
                writeIndex++;
            }
        }

        // 如果writeIndex小于chars.length，说明有未处理的字符（如结尾的下划线）
        if (writeIndex < chars.length) {
            char[] result = new char[writeIndex];
            System.arraycopy(chars, 0, result, 0, writeIndex);
            return new String(result);
        }

        return new String(chars, 0, writeIndex);
    }
}
