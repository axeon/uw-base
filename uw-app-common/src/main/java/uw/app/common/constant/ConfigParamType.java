package uw.app.common.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 描述: 数据类型
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@Schema(title = "配置参数类型", description = "配置参数类型")
public enum ConfigParamType {

    /**
     * 字符串类型
     */
    STRING( "string", "字符串类型" ),

    /**
     * 列表字符串类型
     */
    SET_STRING( "set<string>", "字符串类型集合" ),

    /**
     * 数值类型
     */
    INT( "int", "INT类型" ),

    /**
     * 列表数值类型
     */
    SET_INT( "set<int>", "INT类型集合" ),

    /**
     * 数值类型
     */
    LONG( "long", "LONG类型" ),

    /**
     * 列表数值类型
     */
    SET_LONG( "set<long>", "LONG类型集合" ),

    /**
     * 布尔类型
     */
    BOOLEAN( "boolean", "布尔类型" ),

    /**
     * 列表布尔类型
     */
    SET_BOOLEAN( "set<boolean>", "布尔类型集合" ),

    /**
     * 浮点类型
     */
    FLOAT( "float", "浮点类型" ),

    /**
     * 列表浮点类型
     */
    SET_FLOAT( "set<float>", "浮点类型集合" ),
    
    /**
     * 双精度浮点类型
     */
    DOUBLE( "double", "双精度浮点类型" ),

    /**
     * 列表双精度浮点类型
     */
    SET_DOUBLE( "set<double>", "双精度浮点类型集合" ),

    /**
     * 日期类型
     */
    DATE( "date", "日期类型" ),

    /**
     * 列表日期类型
     */
    SET_DATE( "set<date>", "日期类型集合" ),

    /**
     * 日期类型
     */
    TIME( "time", "时间类型" ),

    /**
     * 列表日期类型
     */
    SET_TIME( "set<time>", "时间类型集合" ),

    /**
     * 日期时间类型
     */
    DATETIME( "datetime", "日期时间类型" ),

    /**
     * 列表日期时间类型
     */
    SET_DATETIME( "set<datetime>", "日期时间类型集合" ),

    /**
     * 枚举类型
     */
    ENUM( "enum", "枚举类型" ),

    /**
     * 列表枚举类型
     */
    SET_ENUM( "set<enum>", "枚举类型集合" ),

    /**
     * map类型
     */
    MAP( "map", "MAP类型" ),
    ;

    private final String value;

    private final String label;

    ConfigParamType(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }
}