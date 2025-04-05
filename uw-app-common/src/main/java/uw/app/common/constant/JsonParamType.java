package uw.app.common.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * JSON配置数据类型
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@Schema(title = "JSON配置数据类型", description = "JSON配置数据类型")
public enum JsonParamType {

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

    @JsonValue
    private final String value;

    private final String label;

    JsonParamType(String value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 反序列化时根据 value 值解析枚举。
     * @param value
     * @return
     */
    @JsonCreator
    public static JsonParamType fromValue(String value) {
        for (JsonParamType type : JsonParamType.values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        throw null;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }
}