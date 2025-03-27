package uw.common.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 配置信息解析类
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(title = "配置信息解析类", description = "配置信息解析类")
public class ConfigParam {

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
     * 配置类型。 数值，字符串，布尔值，浮点数，浮点数，日期，时间，日期时间,枚举.
     * 详见TypeConfigParam。
     */
    @Schema(title = "配置类型", description = "配置类型")
    private String type;

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

    public ConfigParam() {
    }

    public ConfigParam(String key, String value, String type, String name, String desc) {
        this.key = key;
        this.value = value;
        this.type = type;
        this.name = name;
        this.desc = desc;
    }

    /**
     * 获取配置参数名。
     *
     * @return 配置参数名。
     */
    public String getKey() {
        return key;
    }

    /**
     * 获取配置默认值。
     * @return
     */
    public String getValue() {
        return value;
    }

    /**
     * 获取配置类型。
     * @return
     */
    public String getType() {
        return type;
    }

    /**
     * 获取配置名称。
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * 获取配置描述。
     * @return
     */
    public String getDesc() {
        return desc;
    }
}
