package uw.common.app.helper;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.app.vo.JsonConfigBox;
import uw.common.app.vo.JsonConfigParam;
import uw.common.dto.ResponseData;
import uw.common.util.DateUtils;
import uw.common.util.JsonUtils;

import java.util.*;
import java.util.regex.PatternSyntaxException;

/**
 * Json配置参数帮助类。
 * 通过JsonParam定义的配置参数，构建JsonParamBox来获取结构化和类型化参数。
 */
public class JsonConfigHelper {

    /**
     * 对象映射器。
     */
    private static final Logger logger = LoggerFactory.getLogger( JsonConfigHelper.class );


    /**
     * 构建配置参数盒子。
     *
     * @param configParamList 配置参数列表
     * @param configDataMap   配置数据Map
     * @return
     */
    public static ResponseData<JsonConfigBox> buildParamBox(List<JsonConfigParam> configParamList, Map<String, String> configDataMap) {
        if (configParamList == null) {
            return ResponseData.error( JsonConfigBox.EMPTY_PARAM_BOX, "", "配置参数为空！" );
        }
        Map<String, String> configMixMap = new HashMap<>();
        for (JsonConfigParam configParam : configParamList) {
            configMixMap.put( configParam.getKey(), configParam.getValue() );
        }
        if (configDataMap == null) {
            return ResponseData.warn( new JsonConfigBox( configMixMap ), "", "配置数据加载失败，使用默认配置信息！" );
        }
        configMixMap.putAll( configDataMap );
        return ResponseData.success( new JsonConfigBox( configMixMap ) );
    }

    /**
     * 构建配置参数盒子。
     *
     * @param configParamList 配置参数列表
     * @param configDataJson  配置数据Json
     * @return
     */
    public static ResponseData<JsonConfigBox> buildParamBox(List<JsonConfigParam> configParamList, String configDataJson) {
        Map<String, String> configDataMap = null;
        if (StringUtils.isNotBlank( configDataJson )) {
            try {
                configDataMap = JsonUtils.parse( configDataJson, new TypeReference<Map<String, String>>() {
                } );
            } catch (Exception e) {
                logger.error( "配置数据解析失败！{}", e.getMessage(), e );
            }
        }
        return buildParamBox( configParamList, configDataMap );
    }

    /**
     * 构建配置参数盒子。
     *
     * @param configParamJson 配置参数Json
     * @param configDataJson  配置数据Json
     * @return
     */
    public static ResponseData<JsonConfigBox> buildParamBox(String configParamJson, String configDataJson) {
        List<JsonConfigParam> configParams = null;
        Map<String, String> configDataMap = null;
        if (StringUtils.isNotBlank( configParamJson )) {
            try {
                configParams = Arrays.asList( JsonUtils.parse( configParamJson, JsonConfigParam[].class ) );
            } catch (Exception e) {
                logger.error( "配置参数解析失败！{}", e.getMessage(), e );
                return ResponseData.error( JsonConfigBox.EMPTY_PARAM_BOX, "", "配置参数解析失败！" + e.getMessage() );
            }
        }
        if (StringUtils.isNotBlank( configDataJson )) {
            try {
                configDataMap = JsonUtils.parse( configDataJson, new TypeReference<Map<String, String>>() {
                } );
            } catch (Exception e) {
                logger.error( "配置数据解析失败！{}", e.getMessage(), e );
            }
        }
        return buildParamBox( configParams, configDataMap );
    }

    /**
     * 验证配置数据。
     *
     * @param configParamList 配置参数列表
     * @param configDataMap   配置数据信息
     * @return
     */
    public ResponseData<Map<String, String>> validateConfigData(List<JsonConfigParam> configParamList, Map<String, String> configDataMap) {
        if (configParamList == null) {
            return ResponseData.errorMsg( "配置参数为空！" );
        }
        if (configDataMap == null) {
            return ResponseData.errorMsg( "配置数据为空！" );
        }
        // 对配置参数进行验证
        Map<String,String> errorParamMap = new LinkedHashMap<String, String>();
        for (JsonConfigParam configParam : configParamList) {
            String paramValue = configDataMap.get(configParam.getKey());
            if (paramValue == null) {
                continue;
            }
            boolean isValid = true;
            String errorMessage = "";

            // 直接比较枚举类型，无需调用getValue()
            if (configParam.getType() == JsonConfigParam.ParamType.STRING) { // 修复核心点
                if (StringUtils.isBlank(paramValue)) {
                    errorMessage += "参数不能为空";
                    isValid = false;
                }
            } else if (configParam.getType() == JsonConfigParam.ParamType.TEXT) {
                if (StringUtils.isBlank(paramValue)) {
                    errorMessage += "参数不能为空";
                    isValid = false;
                }
            } else if (configParam.getType() == JsonConfigParam.ParamType.INT) {
                try {
                    Integer.parseInt(paramValue);
                } catch (Exception e) {
                    errorMessage += "值不是有效的整数";
                    isValid = false;
                }
            } else if (configParam.getType() == JsonConfigParam.ParamType.LONG) {
                try {
                    Long.parseLong(paramValue);
                } catch (Exception e) {
                    errorMessage += "值不是有效的长整型";
                    isValid = false;
                }
            } else if (configParam.getType() == JsonConfigParam.ParamType.FLOAT) {
                try {
                    Float.parseFloat(paramValue);
                } catch (Exception e) {
                    errorMessage += "值不是有效的浮点数";
                    isValid = false;
                }
            } else if (configParam.getType() == JsonConfigParam.ParamType.DOUBLE) {
                try {
                    Double.parseDouble(paramValue);
                } catch (Exception e) {
                    errorMessage += "值不是有效的双精度数";
                    isValid = false;
                }
            } else if (configParam.getType() == JsonConfigParam.ParamType.BOOLEAN) {
                try {
                    Boolean.parseBoolean(paramValue);
                } catch (Exception e) {
                    errorMessage += "值不是有效的布尔值";
                    isValid = false;
                }
            } else if (configParam.getType() == JsonConfigParam.ParamType.DATE) {
                if (DateUtils.stringToDate(paramValue) == null) {
                    errorMessage += "日期格式错误（格式：yyyy-MM-dd）";
                    isValid = false;
                }
            } else if (configParam.getType() == JsonConfigParam.ParamType.TIME) {
                if (DateUtils.stringToDate(paramValue) == null) {
                    errorMessage += "时间格式错误（格式：HH:mm:ss）";
                    isValid = false;
                }
            } else if (configParam.getType() == JsonConfigParam.ParamType.DATETIME) {
                if (DateUtils.stringToDate(paramValue) == null) {
                    errorMessage += "日期时间格式错误（格式：yyyy-MM-dd HH:mm:ss）";
                    isValid = false;
                }
            } else if (configParam.getType() == JsonConfigParam.ParamType.ENUM) {
                Set<String> enumSet = null;
                try {
                    enumSet = JsonUtils.parse(configParam.getValue(), new TypeReference<Set<String>>() {});
                } catch (Exception e) {
                    errorMessage += "枚举值解析失败";
                    isValid = false;
                }
                if (enumSet != null && !enumSet.contains(paramValue)) {
                    errorMessage += "值不在枚举范围内";
                    isValid = false;
                }
            } else if (configParam.getType() == JsonConfigParam.ParamType.MAP) {
                try {
                    Map<String, String> map = JsonUtils.parse(paramValue, new TypeReference<>() {});
                } catch (Exception e) {
                    errorMessage += "无法解析为Map格式";
                    isValid = false;
                }
            }

            // 正则表达式校验保持不变
            String regex = configParam.getRegex();
            if (StringUtils.isNotBlank(regex)) {
                try {
                    if (!paramValue.matches(regex)) {
                        errorMessage += "值不符合正则表达式: " + regex;
                        isValid = false;
                    }
                } catch (PatternSyntaxException e) {
                    isValid = false;
                    logger.error("正则表达式格式错误: {}", regex, e);
                }
            }

            if (!isValid) {
                errorParamMap.put( configParam.getKey(), errorMessage );
            }
        }
        if (!errorParamMap.isEmpty()) {
            return ResponseData.error( errorParamMap, "", "配置数据校验失败！" );
        }
        return ResponseData.success();
    }

    /**
     * 校验配置数据。
     *
     * @param configParamList 配置参数列表
     * @param configDataJson  配置数据Json
     * @return
     */
    public ResponseData<Map<String, String>> validateConfigData(List<JsonConfigParam> configParamList, String configDataJson) {
        Map<String, String> configDataMap = null;
        if (StringUtils.isNotBlank( configDataJson )) {
            try {
                configDataMap = JsonUtils.parse( configDataJson, new TypeReference<Map<String, String>>() {
                } );
            } catch (Exception e) {
                logger.error( "配置数据解析失败！{}", e.getMessage(), e );
            }
        }
        return validateConfigData( configParamList, configDataMap );

    }

    /**
     * 校验配置数据。
     *
     * @param configParamJson 配置参数Json
     * @param configDataJson  配置数据Json
     * @return
     */
    public ResponseData<Map<String, String>> validateConfigData(String configParamJson, String configDataJson) {
        Map<String, String> configDataMap = null;
        List<JsonConfigParam> configParamList = null;
        if (StringUtils.isNotBlank( configParamJson )) {
            try {
                configParamList = JsonUtils.parse( configParamJson, new TypeReference<List<JsonConfigParam>>() {
                } );
            } catch (Exception e) {
                logger.error( "配置参数解析失败！{}", e.getMessage(), e );
            }
        }
        if (StringUtils.isNotBlank( configDataJson )) {
            try {
                configDataMap = JsonUtils.parse( configDataJson, new TypeReference<Map<String, String>>() {
                } );
            } catch (Exception e) {
                logger.error( "配置数据解析失败！{}", e.getMessage(), e );
            }
        }
        return validateConfigData( configParamList, configDataMap );

    }


}
