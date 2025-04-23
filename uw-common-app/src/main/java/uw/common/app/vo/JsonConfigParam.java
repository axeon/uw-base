package uw.common.app.vo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Json配置参数定义。
 */
@Schema(title = "Json配置参数定义", description = "Json配置参数定义")
public interface JsonConfigParam {

    /**
     * 配置参数数据。
     * @return
     */
    @JsonIgnore
    ParamData getParamData();

    /**
     * 配置参数名。
     * 当使用enum的时候。
     */
    @Schema(title = "配置参数名", description = "配置参数名")
    default String getKey(){
        return getParamData().getKey();
    }

    /**
     * 配置类型。 数值，字符串，布尔值，浮点数，浮点数，日期，时间，日期时间,枚举.
     * 详见ConfigParamType。
     */
    @Schema(title = "配置类型", description = "配置类型")
    default ParamType getType(){
        return getParamData().getType();
    }

    /**
     * 配置默认值。
     */
    @Schema(title = "配置默认值", description = "配置默认值")
    default String getValue(){
        return getParamData().getValue();
    }

    /**
     * 配置名称。
     */
    @Schema(title = "配置名称", description = "配置名称")
    default String getName(){
        return getParamData().getName();
    }

    /**
     * 配置描述。
     */
    @Schema(title = "配置描述", description = "配置描述")
    default String getDesc(){
        return getParamData().getDesc();
    }

    /**
     * 正则表达式。
     */
    @Schema(title = "正则表达式", description = "正则表达式")
    default String getRegex(){
        return getParamData().getRegex();
    }

    /**
     * 配置参数数据。
     */
    class ParamData {

        /**
         * 配置类型。 数值，字符串，布尔值，浮点数，浮点数，日期，时间，日期时间,枚举.
         * 详见ConfigParamType。
         */
        private final ParamType type;

        /**
         * 配置key。
         */
        private final String key;

        /**
         * 配置默认值。
         */
        private final String value;

        /**
         * 配置名称。
         */
        private final String name;

        /**
         * 配置描述。
         */
        private final String desc;

        /**
         * 校验正则表达式。
         */
        private final String regex;

        public ParamData(String key, ParamType type, String value, String name, String desc, String regex) {
            this.key = key;
            this.type = type;
            this.value = value;
            this.name = name;
            this.desc = desc;
            this.regex = regex;
        }

        public ParamType getType() {
            return type;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public String getName() {
            return name;
        }

        public String getDesc() {
            return desc;
        }

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

        ParamType(String value, String label) {
            this.value = value;
            this.label = label;
        }

        /**
         * 反序列化时根据 value 值解析枚举。
         *
         * @param value
         * @return
         */
        @JsonCreator
        public static ParamType fromValue(String value) {
            for (ParamType type : ParamType.values()) {
                if (type.getValue().equals( value )) {
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
}
