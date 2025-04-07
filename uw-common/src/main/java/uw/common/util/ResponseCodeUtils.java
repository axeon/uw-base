package uw.common.util;

import uw.common.dto.ResponseCode;

import java.util.Properties;

/**
 * 响应码工具类。
 *
 * @author axeon
 */
public class ResponseCodeUtils {

    /**
     * 将实现 {@link ResponseCode} 接口的枚举类的所有枚举项转换为 Properties。
     * Key为 {@link ResponseCode#getCode()}，Value为 {@link ResponseCode#getMessage()}。
     *
     * @param <E>       枚举类型，必须实现 ResponseCode 接口
     * @param enumClass 枚举类的 Class 对象
     * @return 包含所有枚举项的 Properties
     */
    public static <E extends Enum<E> & ResponseCode> Properties toProperties(Class<E> enumClass) {
        // 参数校验
        if (enumClass == null) {
            throw new IllegalArgumentException("枚举类不能为空");
        }
        if (!enumClass.isEnum()) {
            throw new IllegalArgumentException("参数必须是枚举类");
        }
        // 获取所有枚举常量
        E[] enumConstants = enumClass.getEnumConstants();
        if (enumConstants == null) {
            throw new IllegalArgumentException("枚举类未定义枚举常量");
        }
        Properties properties = new Properties();
        // 遍历枚举类的所有枚举常量
        for (E enumConstant : enumClass.getEnumConstants()) {
            String code = enumConstant.getCode();
            String message = enumConstant.getMessage();
            if (code == null || message == null) {
                throw new IllegalArgumentException("枚举项 " + enumConstant.name() + " 的 code 或 message 不能为空");
            }
            properties.put(code, message);
        }
        return properties;
    }

    /**
     * 将实现 {@link ResponseCode} 接口的枚举类的所有枚举项转换为 Properties。
     * Key为 {@link ResponseCode#getCode()}，Value为 {@link ResponseCode#getMessage()}。
     * 然后输出到一个String中。
     * @param enumClass
     * @return
     * @param <E>
     */
    public static <E extends Enum<E> & ResponseCode> String toPropertyString(Class<E> enumClass) {
        Properties properties = toProperties(enumClass);
        StringBuilder sb = new StringBuilder();
        for (Object key : properties.keySet()) {
            sb.append(key).append("=");
            sb.append(properties.get(key)).append("\n");
        }
        return sb.toString();
    }


}
