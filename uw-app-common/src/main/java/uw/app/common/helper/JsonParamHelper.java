package uw.app.common.helper;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.app.common.constant.JsonParamType;
import uw.common.dto.ResponseData;
import uw.common.util.DateUtils;
import uw.common.util.JsonUtils;
import uw.app.common.vo.JsonParam;
import uw.app.common.vo.JsonParamBox;

import java.util.*;
import java.util.regex.PatternSyntaxException;

/**
 * Json配置参数帮助类。
 * 通过JsonParam定义的配置参数，构建JsonParamBox来获取结构化和类型化参数。
 */
public class JsonParamHelper {

    /**
     * 对象映射器。
     */
    private static final Logger logger = LoggerFactory.getLogger( JsonParamHelper.class );


    /**
     * 构建配置参数盒子。
     *
     * @param configParamList 配置参数列表
     * @param configDataMap   配置数据Map
     * @return
     */
    public static ResponseData<JsonParamBox> buildParamBox(List<JsonParam> configParamList, Map<String, String> configDataMap) {
        if (configParamList == null) {
            return ResponseData.error( JsonParamBox.EMPTY_PARAM_BOX, "", "配置参数为空！" );
        }
        Map<String, String> configMixMap = new HashMap<>();
        for (JsonParam configParam : configParamList) {
            configMixMap.put( configParam.getKey(), configParam.getValue() );
        }
        if (configDataMap == null) {
            return ResponseData.warn( new JsonParamBox( configMixMap ), "", "配置数据加载失败，使用默认配置信息！" );
        }
        configMixMap.putAll( configDataMap );
        return ResponseData.success( new JsonParamBox( configMixMap ) );
    }

    /**
     * 构建配置参数盒子。
     *
     * @param configParamList 配置参数列表
     * @param configDataJson  配置数据Json
     * @return
     */
    public static ResponseData<JsonParamBox> buildParamBox(List<JsonParam> configParamList, String configDataJson) {
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
    public static ResponseData<JsonParamBox> buildParamBox(String configParamJson, String configDataJson) {
        List<JsonParam> configParams = null;
        Map<String, String> configDataMap = null;
        if (StringUtils.isNotBlank( configParamJson )) {
            try {
                configParams = Arrays.asList( JsonUtils.parse( configParamJson, JsonParam[].class ) );
            } catch (Exception e) {
                logger.error( "配置参数解析失败！{}", e.getMessage(), e );
                return ResponseData.error( JsonParamBox.EMPTY_PARAM_BOX, "", "配置参数解析失败！" + e.getMessage() );
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
    public ResponseData<List<JsonParam>> validateConfigData(List<JsonParam> configParamList, Map<String, String> configDataMap) {
        if (configParamList == null) {
            return ResponseData.errorMsg( "配置参数为空！" );
        }
        if (configDataMap == null) {
            return ResponseData.errorMsg( "配置数据为空！" );
        }
        // 对配置参数进行验证
        List<JsonParam> errorParamList = new ArrayList<>();
        for (JsonParam configParam : configParamList) {
            String paramValue = configDataMap.get(configParam.getKey());
            if (paramValue == null) {
                continue;
            }
            boolean isValid = true;
            String errorMessage = "";

            // 直接比较枚举类型，无需调用getValue()
            if (configParam.getType() == JsonParamType.STRING) { // 修复核心点
                if (StringUtils.isBlank(paramValue)) {
                    errorMessage += "参数不能为空";
                    isValid = false;
                }
            } else if (configParam.getType() == JsonParamType.INT) {
                try {
                    Integer.parseInt(paramValue);
                } catch (Exception e) {
                    errorMessage += "值不是有效的整数";
                    isValid = false;
                }
            } else if (configParam.getType() == JsonParamType.LONG) {
                try {
                    Long.parseLong(paramValue);
                } catch (Exception e) {
                    errorMessage += "值不是有效的长整型";
                    isValid = false;
                }
            } else if (configParam.getType() == JsonParamType.FLOAT) {
                try {
                    Float.parseFloat(paramValue);
                } catch (Exception e) {
                    errorMessage += "值不是有效的浮点数";
                    isValid = false;
                }
            } else if (configParam.getType() == JsonParamType.DOUBLE) {
                try {
                    Double.parseDouble(paramValue);
                } catch (Exception e) {
                    errorMessage += "值不是有效的双精度数";
                    isValid = false;
                }
            } else if (configParam.getType() == JsonParamType.BOOLEAN) {
                try {
                    Boolean.parseBoolean(paramValue);
                } catch (Exception e) {
                    errorMessage += "值不是有效的布尔值";
                    isValid = false;
                }
            } else if (configParam.getType() == JsonParamType.DATE) {
                if (DateUtils.stringToDate(paramValue, DateUtils.DATE) == null) {
                    errorMessage += "日期格式错误（格式：yyyy-MM-dd）";
                    isValid = false;
                }
            } else if (configParam.getType() == JsonParamType.TIME) {
                if (DateUtils.stringToDate(paramValue, DateUtils.TIME) == null) {
                    errorMessage += "时间格式错误（格式：HH:mm:ss）";
                    isValid = false;
                }
            } else if (configParam.getType() == JsonParamType.DATETIME) {
                if (DateUtils.stringToDate(paramValue, DateUtils.DATE_TIME) == null) {
                    errorMessage += "日期时间格式错误（格式：yyyy-MM-dd HH:mm:ss）";
                    isValid = false;
                }
            } else if (configParam.getType() == JsonParamType.ENUM) {
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
            } else if (configParam.getType() == JsonParamType.MAP) {
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
                configParam.setMsg(errorMessage);
                errorParamList.add(configParam);
            }
        }
        if (!errorParamList.isEmpty()) {
            return ResponseData.error( errorParamList, "", "配置数据校验失败！" );
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
    public ResponseData<List<JsonParam>> validateConfigData(List<JsonParam> configParamList, String configDataJson) {
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
    public ResponseData<List<JsonParam>> validateConfigData(String configParamJson, String configDataJson) {
        Map<String, String> configDataMap = null;
        List<JsonParam> configParamList = null;
        if (StringUtils.isNotBlank( configParamJson )) {
            try {
                configParamList = JsonUtils.parse( configParamJson, new TypeReference<List<JsonParam>>() {
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
