package uw.common.app.helper;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.app.constant.ValidateResponseCode;
import uw.common.app.vo.JsonConfigBox;
import uw.common.app.vo.JsonConfigParam;
import uw.common.app.vo.ValidateResult;
import uw.common.response.ResponseData;
import uw.common.util.DateUtils;
import uw.common.util.JsonUtils;

import java.util.*;
import java.util.regex.PatternSyntaxException;

/**
 * JSON 配置参数帮助类。
 * <p>
 * 通过 {@link JsonConfigParam} 定义配置参数元数据（类型、默认值、校验规则），构建 {@link JsonConfigBox}
 * 以获取结构化和强类型的配置值，并支持配置数据的校验。
 * </p>
 */
public class JsonConfigHelper {

    /**
     * 日志记录器。
     */
    private static final Logger logger = LoggerFactory.getLogger(JsonConfigHelper.class);


    /**
     * 构建配置参数盒子。
     * <p>
     * 参数定义列表为 null 时返回错误（data 为 null）；配置数据 Map 为 null 时使用默认值并返回 WARN；
     * 否则将默认值与实际数据合并后返回成功。
     * </p>
     *
     * @param configParamList 配置参数定义列表
     * @param configDataMap   配置数据 Map（可为 null）
     * @return 携带 {@link JsonConfigBox} 的响应数据
     */
    public static ResponseData<JsonConfigBox> buildParamBox(List<JsonConfigParam> configParamList, Map<String, String> configDataMap) {
        if (configParamList == null) {
            // 配置参数定义缺失属于错误，data 返回 null，避免调用方误用空盒子。
            return ResponseData.error(null, "", "配置参数为空！");
        }
        Map<String, String> configMixMap = new HashMap<>();
        for (JsonConfigParam configParam : configParamList) {
            configMixMap.put(configParam.getKey(), configParam.getValue());
        }
        if (configDataMap == null) {
            return ResponseData.warn(new JsonConfigBox(configMixMap), "", "配置数据加载失败，使用默认配置信息！");
        }
        configMixMap.putAll(configDataMap);
        return ResponseData.success(new JsonConfigBox(configMixMap));
    }

    /**
     * 构建配置参数盒子。
     *
     * @param configParamList 配置参数定义列表
     * @param configDataJson  配置数据 JSON 字符串（可为空）
     * @return 携带 {@link JsonConfigBox} 的响应数据
     */
    public static ResponseData<JsonConfigBox> buildParamBox(List<JsonConfigParam> configParamList, String configDataJson) {
        Map<String, String> configDataMap = null;
        if (StringUtils.isNotBlank(configDataJson)) {
            try {
                configDataMap = JsonUtils.parse(configDataJson, new TypeReference<Map<String, String>>() {
                });
            } catch (Exception e) {
                logger.error("配置数据解析失败！{}", e.getMessage(), e);
            }
        }
        return buildParamBox(configParamList, configDataMap);
    }

    /**
     * 构建配置参数盒子。
     *
     * @param configParamJson 配置参数定义 JSON 字符串（可为空）
     * @param configDataJson  配置数据 JSON 字符串（可为空）
     * @return 携带 {@link JsonConfigBox} 的响应数据；参数解析失败时 data 为 null
     */
    public static ResponseData<JsonConfigBox> buildParamBox(String configParamJson, String configDataJson) {
        List<JsonConfigParam> configParams = null;
        Map<String, String> configDataMap = null;
        if (StringUtils.isNotBlank(configParamJson)) {
            try {
                configParams = Arrays.asList(JsonUtils.parse(configParamJson, JsonConfigParam[].class));
            } catch (Exception e) {
                logger.error("配置参数解析失败！{}", e.getMessage(), e);
                return ResponseData.error(null, "", "配置参数解析失败！" + e.getMessage());
            }
        }
        if (StringUtils.isNotBlank(configDataJson)) {
            try {
                configDataMap = JsonUtils.parse(configDataJson, new TypeReference<Map<String, String>>() {
                });
            } catch (Exception e) {
                logger.error("配置数据解析失败！{}", e.getMessage(), e);
            }
        }
        return buildParamBox(configParams, configDataMap);
    }

    /**
     * 校验单个配置参数值（按类型与正则）。
     *
     * @param configParam 配置参数定义
     * @param paramValue  参数值
     * @return 校验失败结果；通过时返回 null
     */
    private static ValidateResult validateParam(JsonConfigParam configParam, String paramValue) {
        if (paramValue == null) {
            return new ValidateResult(configParam.getKey(), configParam.getDesc(), ValidateResponseCode.NOT_NULL, null);
        }
        // 直接比较枚举类型，无需调用getValue()
        if (configParam.getType() == JsonConfigParam.ParamType.STRING) {
            if (StringUtils.isBlank(paramValue)) {
                return new ValidateResult(configParam.getKey(), configParam.getDesc(), ValidateResponseCode.NOT_EMPTY, null);
            }
        } else if (configParam.getType() == JsonConfigParam.ParamType.TEXT) {
            if (StringUtils.isBlank(paramValue)) {
                return new ValidateResult(configParam.getKey(), configParam.getDesc(), ValidateResponseCode.NOT_EMPTY, null);
            }
        } else if (configParam.getType() == JsonConfigParam.ParamType.TEXT_RICH) {
            if (StringUtils.isBlank(paramValue)) {
                return new ValidateResult(configParam.getKey(), configParam.getDesc(), ValidateResponseCode.NOT_EMPTY, null);
            }
        } else if (configParam.getType() == JsonConfigParam.ParamType.INT) {
            try {
                Integer.parseInt(paramValue);
            } catch (Exception e) {
                return new ValidateResult(configParam.getKey(), configParam.getDesc(), ValidateResponseCode.DATA_FORMAT_ERROR, null);
            }
        } else if (configParam.getType() == JsonConfigParam.ParamType.LONG) {
            try {
                Long.parseLong(paramValue);
            } catch (Exception e) {
                return new ValidateResult(configParam.getKey(), configParam.getDesc(), ValidateResponseCode.DATA_FORMAT_ERROR, null);
            }
        } else if (configParam.getType() == JsonConfigParam.ParamType.FLOAT) {
            try {
                Float.parseFloat(paramValue);
            } catch (Exception e) {
                return new ValidateResult(configParam.getKey(), configParam.getDesc(), ValidateResponseCode.DATA_FORMAT_ERROR, null);
            }
        } else if (configParam.getType() == JsonConfigParam.ParamType.DOUBLE) {
            try {
                Double.parseDouble(paramValue);
            } catch (Exception e) {
                return new ValidateResult(configParam.getKey(), configParam.getDesc(), ValidateResponseCode.DATA_FORMAT_ERROR, null);
            }
        } else if (configParam.getType() == JsonConfigParam.ParamType.BOOLEAN) {
            try {
                Boolean.parseBoolean(paramValue);
            } catch (Exception e) {
                return new ValidateResult(configParam.getKey(), configParam.getDesc(), ValidateResponseCode.DATA_FORMAT_ERROR, null);
            }
        } else if (configParam.getType() == JsonConfigParam.ParamType.DATE) {
            if (DateUtils.stringToDate(paramValue) == null) {
                return new ValidateResult(configParam.getKey(), configParam.getDesc(), ValidateResponseCode.DATA_FORMAT_ERROR, null);
            }
        } else if (configParam.getType() == JsonConfigParam.ParamType.TIME) {
            if (DateUtils.stringToDate(paramValue) == null) {
                return new ValidateResult(configParam.getKey(), configParam.getDesc(), ValidateResponseCode.DATA_FORMAT_ERROR, null);
            }
        } else if (configParam.getType() == JsonConfigParam.ParamType.DATETIME) {
            if (DateUtils.stringToDate(paramValue) == null) {
                return new ValidateResult(configParam.getKey(), configParam.getDesc(), ValidateResponseCode.DATA_FORMAT_ERROR, null);
            }
        } else if (configParam.getType() == JsonConfigParam.ParamType.ENUM) {
            // ENUM 的可选值集合约定存放在 getValue() 中（JSON 数组形式）。
            // 若 value 不是合法集合（如未配置或为单值默认值），跳过集合校验，仅依赖后续正则校验，避免误判。
            Set<String> enumSet = null;
            if (StringUtils.isNotBlank(configParam.getValue())) {
                try {
                    enumSet = JsonUtils.parse(configParam.getValue(), new TypeReference<Set<String>>() {
                    });
                } catch (Exception ignored) {
                    // value 非集合形式，跳过枚举集合校验
                }
            }
            if (enumSet != null && !enumSet.isEmpty() && !enumSet.contains(paramValue)) {
                return new ValidateResult(configParam.getKey(), configParam.getDesc(), ValidateResponseCode.DATA_FORMAT_ERROR, null);
            }
        } else if (configParam.getType() == JsonConfigParam.ParamType.MAP) {
            try {
                Map<String, String> map = JsonUtils.parse(paramValue, new TypeReference<>() {
                });
            } catch (Exception e) {
                return new ValidateResult(configParam.getKey(), configParam.getDesc(), ValidateResponseCode.DATA_FORMAT_ERROR, null);
            }
        }

        // 正则表达式校验
        String regex = configParam.getRegex();
        if (StringUtils.isNotBlank(regex) && StringUtils.isNotBlank(paramValue)) {
            try {
                if (!paramValue.matches(regex)) {
                    return new ValidateResult(configParam.getKey(), configParam.getDesc(), ValidateResponseCode.REGEX_FORMAT_ERROR, null);
                }
            } catch (PatternSyntaxException e) {
                logger.error("正则表达式格式错误: {}", regex, e);
            }
        }
        return null;
    }

    /**
     * 校验配置数据。
     *
     * @param configParamList 配置参数定义列表
     * @param configDataMap   配置数据 Map
     * @return 校验通过返回成功；失败时 data 为校验结果列表
     */
    public static ResponseData<List<ValidateResult>> validateConfigData(List<JsonConfigParam> configParamList, Map<String, String> configDataMap) {
        if (configParamList == null) {
            return ResponseData.errorMsg("配置参数为空！");
        }
        if (configDataMap == null) {
            return ResponseData.errorMsg("配置数据为空！");
        }
        // 对配置参数进行验证
        List<ValidateResult> validateResultList = new ArrayList<>();
        for (JsonConfigParam configParam : configParamList) {
            String paramValue = configDataMap.get(configParam.getKey());
            ValidateResult validateResult = validateParam(configParam, paramValue);
            if (validateResult != null) {
                validateResultList.add(validateResult);
            }
        }
        if (!validateResultList.isEmpty()) {
            return ResponseData.error(validateResultList, "", "配置数据校验失败！");
        }
        return ResponseData.success();
    }

    /**
     * 校验配置数据。
     *
     * @param configParamList 配置参数定义列表
     * @param configDataJson  配置数据 JSON 字符串（可为空）
     * @return 校验通过返回成功；失败时 data 为校验结果列表
     */
    public static ResponseData<List<ValidateResult>> validateConfigData(List<JsonConfigParam> configParamList, String configDataJson) {
        Map<String, String> configDataMap = null;
        if (StringUtils.isNotBlank(configDataJson)) {
            try {
                configDataMap = JsonUtils.parse(configDataJson, new TypeReference<Map<String, String>>() {
                });
            } catch (Exception e) {
                logger.error("配置数据解析失败！{}", e.getMessage(), e);
            }
        }
        return validateConfigData(configParamList, configDataMap);

    }

    /**
     * 校验配置数据。
     *
     * @param configParamJson 配置参数定义 JSON 字符串（可为空）
     * @param configDataJson  配置数据 JSON 字符串（可为空）
     * @return 校验通过返回成功；失败时 data 为校验结果列表
     */
    public static ResponseData<List<ValidateResult>> validateConfigData(String configParamJson, String configDataJson) {
        Map<String, String> configDataMap = null;
        List<JsonConfigParam> configParamList = null;
        if (StringUtils.isNotBlank(configParamJson)) {
            try {
                configParamList = JsonUtils.parse(configParamJson, new TypeReference<List<JsonConfigParam>>() {
                });
            } catch (Exception e) {
                logger.error("配置参数解析失败！{}", e.getMessage(), e);
            }
        }
        if (StringUtils.isNotBlank(configDataJson)) {
            try {
                configDataMap = JsonUtils.parse(configDataJson, new TypeReference<Map<String, String>>() {
                });
            } catch (Exception e) {
                logger.error("配置数据解析失败！{}", e.getMessage(), e);
            }
        }
        return validateConfigData(configParamList, configDataMap);

    }


}
