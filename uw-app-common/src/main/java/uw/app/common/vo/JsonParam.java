package uw.app.common.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import uw.app.common.constant.JsonParamType;

/**
 * Json配置参数定义。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(title = "Json配置参数定义", description = "Json配置参数定义")
public class JsonParam {

    /**
     * 配置类型。 数值，字符串，布尔值，浮点数，浮点数，日期，时间，日期时间,枚举.
     * 详见ConfigParamType。
     */
    @Schema(title = "配置类型", description = "配置类型")
    private JsonParamType type;

    /**
     * 配置参数名。
     */
    @Schema(title = "配置参数名", description = "配置参数名")
    private String key;

    /**
     * 配置默认值。
     */
    @Schema(title = "配置默认值", description = "配置默认值")
    private String value;

    /**
     * 配置名称。
     */
    @Schema(title = "配置名称", description = "配置名称")
    private String name;

    /**
     * 配置描述。
     */
    @Schema(title = "配置描述", description = "配置描述")
    private String desc;

    /**
     * 正则表达式。
     */
    @Schema(title = "正则表达式", description = "正则表达式")
    private String regex;

    /**
     * 错误提示信息，仅校验时使用。
     */
    @Schema(title = "错误提示信息", description = "错误提示信息，仅校验时使用")
    private String msg;

    public JsonParam() {
    }

    /**
     * 构造函数。
     *
     * @param type
     * @param key
     * @param value
     */
    public JsonParam(JsonParamType type, String key, String value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }

    /**
     * 构造函数。
     *
     * @param type
     * @param key
     * @param value
     * @param name
     * @param desc
     */
    public JsonParam(JsonParamType type, String key, String value, String name, String desc) {
        this.type = type;
        this.key = key;
        this.value = value;
        this.name = name;
        this.desc = desc;
    }

    /**
     * 构造函数。
     *
     * @param type
     * @param key
     * @param value
     * @param name
     * @param desc
     * @param regex
     */
    public JsonParam(JsonParamType type, String key, String value, String name, String desc, String regex) {
        this.type = type;
        this.key = key;
        this.value = value;
        this.name = name;
        this.desc = desc;
        this.regex = regex;
    }

    public JsonParamType getType() {
        return type;
    }

    public void setType(JsonParamType type) {
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
