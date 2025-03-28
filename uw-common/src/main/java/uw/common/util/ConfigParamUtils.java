package uw.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.constant.TypeConfigParam;
import uw.common.dto.ResponseData;
import uw.common.vo.ConfigParam;
import uw.common.vo.ConfigParamBox;

import java.util.*;

public class ConfigParamUtils {

    /**
     * 对象映射器。
     */
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger( ConfigParamUtils.class );


    /**
     * 构建配置参数盒子。
     *
     * @param configParamList 配置参数列表
     * @param configDataMap   配置数据Map
     * @return
     */
    public static ResponseData<ConfigParamBox> buildParamBox(List<ConfigParam> configParamList, Map<String, String> configDataMap) {
        if (configParamList == null) {
            return ResponseData.error( ConfigParamBox.EMPTY_PARAM_BOX, "", "配置参数为空！" );
        }
        Map<String, String> configMixMap = new HashMap<>();
        for (ConfigParam configParam : configParamList) {
            configMixMap.put( configParam.getKey(), configParam.getValue() );
        }
        if (configDataMap == null) {
            return ResponseData.warn( new ConfigParamBox( configMixMap ), "", "配置数据加载失败，使用默认配置信息！" );
        }
        configMixMap.putAll( configDataMap );
        return ResponseData.success( new ConfigParamBox( configMixMap ) );
    }

    /**
     * 构建配置参数盒子。
     *
     * @param configParamList 配置参数列表
     * @param configDataJson  配置数据Json
     * @return
     */
    public static ResponseData<ConfigParamBox> buildParamBox(List<ConfigParam> configParamList, String configDataJson) {
        Map<String, String> configDataMap = null;
        if (StringUtils.isNotBlank( configDataJson )) {
            try {
                configDataMap = OBJECT_MAPPER.readValue( configDataJson, new TypeReference<Map<String, String>>() {
                } );
            } catch (JsonProcessingException e) {
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
    public static ResponseData<ConfigParamBox> buildParamBox(String configParamJson, String configDataJson) {
        List<ConfigParam> configParams = null;
        Map<String, String> configDataMap = null;
        if (StringUtils.isNotBlank( configParamJson )) {
            try {
                configParams = Arrays.asList( OBJECT_MAPPER.readValue( configParamJson, ConfigParam[].class ) );
            } catch (JsonProcessingException e) {
                logger.error( "配置参数解析失败！{}", e.getMessage(), e );
                return ResponseData.error( ConfigParamBox.EMPTY_PARAM_BOX, "", "配置参数解析失败！" + e.getMessage() );
            }
        }
        if (StringUtils.isNotBlank( configDataJson )) {
            try {
                configDataMap = OBJECT_MAPPER.readValue( configDataJson, new TypeReference<Map<String, String>>() {
                } );
            } catch (JsonProcessingException e) {
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
    public ResponseData<List<ConfigParam>> validateConfigData(List<ConfigParam> configParamList, Map<String, String> configDataMap) {
        if (configParamList == null) {
            return ResponseData.errorMsg( "配置参数为空！" );
        }
        if (configDataMap == null) {
            return ResponseData.errorMsg( "配置数据为空！" );
        }
        List<ConfigParam> errorParamList = new ArrayList<>();
        for (ConfigParam configParam : configParamList) {
            String paramValue = configDataMap.get( configParam.getKey() );
            if (paramValue == null) {
                continue;
            }
            if (configParam.getType().equals( TypeConfigParam.STRING.getValue() )) {
                if (StringUtils.isBlank( paramValue )) {
                    errorParamList.add( configParam );
                }
            } else if (configParam.getType().equals( TypeConfigParam.INT.getValue() )) {
                try {
                    Integer.parseInt( paramValue );
                } catch (Exception ignored) {
                    errorParamList.add( configParam );
                }
            } else if (configParam.getType().equals( TypeConfigParam.LONG.getValue() )) {
                try {
                    Long.parseLong( paramValue );
                } catch (Exception ignored) {
                    errorParamList.add( configParam );
                }
            } else if (configParam.getType().equals( TypeConfigParam.FLOAT.getValue() )) {
                try {
                    Float.parseFloat( paramValue );
                } catch (Exception ignored) {
                    errorParamList.add( configParam );
                }
            } else if (configParam.getType().equals( TypeConfigParam.DOUBLE.getValue() )) {
                try {
                    Double.parseDouble( paramValue );
                } catch (Exception ignored) {
                    errorParamList.add( configParam );
                }

            } else if (configParam.getType().equals( TypeConfigParam.BOOLEAN.getValue() )) {
                try {
                    Boolean.parseBoolean( paramValue );
                } catch (Exception ignored) {
                    errorParamList.add( configParam );
                }
            } else if (configParam.getType().equals( TypeConfigParam.DATE.getValue() )) {
                if (DateUtils.stringToDate( paramValue, DateUtils.DATE ) == null) {
                    errorParamList.add( configParam );
                }
            } else if (configParam.getType().equals( TypeConfigParam.TIME.getValue() )) {
                if (DateUtils.stringToDate( paramValue, DateUtils.TIME ) == null) {
                    errorParamList.add( configParam );
                }
            } else if (configParam.getType().equals( TypeConfigParam.DATETIME.getValue() )) {
                if (DateUtils.stringToDate( paramValue, DateUtils.DATE_TIME ) == null) {
                    errorParamList.add( configParam );
                }
            } else if (configParam.getType().equals( TypeConfigParam.ENUM.getValue() )) {
                Set<String> enumSet = null;
                try {
                    enumSet = OBJECT_MAPPER.readValue( configParam.getValue(), new TypeReference<Set<String>>() {
                    } );
                } catch (JsonProcessingException ignored) {
                }
                if (enumSet == null || !enumSet.contains( paramValue )) {
                    errorParamList.add( configParam );
                }
            } else if (configParam.getType().equals( TypeConfigParam.MAP.getValue() )) {
                try {
                    Map<String, String> map = OBJECT_MAPPER.readValue( paramValue, new TypeReference<Map<String, String>>() {
                    } );
                } catch (JsonProcessingException ignored) {
                    errorParamList.add( configParam );
                }
            } else if (configParam.getType().equals( TypeConfigParam.SET_STRING.getValue() )) {
                try {
                    OBJECT_MAPPER.readValue( paramValue, new TypeReference<List<String>>() {
                    } );
                } catch (JsonProcessingException ignored) {
                    errorParamList.add( configParam );
                }
            } else if (configParam.getType().equals( TypeConfigParam.SET_INT.getValue() )) {
                try {
                    String[] list = OBJECT_MAPPER.readValue( paramValue, String[].class );
                    for (String s : list) {
                        Integer.parseInt( s );
                    }
                } catch (JsonProcessingException ignored) {
                    errorParamList.add( configParam );
                }
            } else if (configParam.getType().equals( TypeConfigParam.SET_LONG.getValue() )) {
                try {
                    String[] list = OBJECT_MAPPER.readValue( paramValue, String[].class );
                    for (String s : list) {
                        Long.parseLong( s );
                    }
                } catch (JsonProcessingException ignored) {
                    errorParamList.add( configParam );
                }

            } else if (configParam.getType().equals( TypeConfigParam.SET_DOUBLE.getValue() )) {
                try {
                    String[] list = OBJECT_MAPPER.readValue( paramValue, String[].class );
                    for (String s : list) {
                        Double.parseDouble( s );
                    }
                } catch (JsonProcessingException ignored) {
                    errorParamList.add( configParam );
                }

            } else if (configParam.getType().equals( TypeConfigParam.SET_FLOAT.getValue() )) {
                try {
                    String[] list = OBJECT_MAPPER.readValue( paramValue, String[].class );
                    for (String s : list) {
                        Float.parseFloat( s );
                    }
                } catch (JsonProcessingException ignored) {
                    errorParamList.add( configParam );
                }

            } else if (configParam.getType().equals( TypeConfigParam.SET_BOOLEAN.getValue() )) {
                try {
                    String[] list = OBJECT_MAPPER.readValue( paramValue, String[].class );
                    for (String s : list) {
                        Boolean.parseBoolean( s );
                    }
                } catch (JsonProcessingException ignored) {
                    errorParamList.add( configParam );
                }

            } else if (configParam.getType().equals( TypeConfigParam.SET_DATE.getValue() )) {
                try {
                    String[] list = OBJECT_MAPPER.readValue( paramValue, String[].class );
                    for (String s : list) {
                        if (DateUtils.stringToDate( s, DateUtils.DATE ) == null) {
                            errorParamList.add( configParam );
                            break;
                        }
                    }
                } catch (Exception ignored) {
                    errorParamList.add( configParam );
                }
            } else if (configParam.getType().equals( TypeConfigParam.SET_TIME.getValue() )) {
                try {
                    String[] list = OBJECT_MAPPER.readValue( paramValue, String[].class );
                    for (String s : list) {
                        if (DateUtils.stringToDate( s, DateUtils.TIME ) == null) {
                            errorParamList.add( configParam );
                            break;
                        }
                    }
                } catch (Exception ignored) {
                    errorParamList.add( configParam );
                }
            } else if (configParam.getType().equals( TypeConfigParam.SET_DATETIME.getValue() )) {
                try {
                    String[] list = OBJECT_MAPPER.readValue( paramValue, String[].class );
                    for (String s : list) {
                        if (DateUtils.stringToDate( s, DateUtils.DATE_TIME ) == null) {
                            errorParamList.add( configParam );
                            break;
                        }
                    }
                } catch (Exception ignored) {
                    errorParamList.add( configParam );
                }
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
    public ResponseData<List<ConfigParam>> validateConfigData(List<ConfigParam> configParamList, String configDataJson) {
        Map<String, String> configDataMap = null;
        if (StringUtils.isNotBlank( configDataJson )) {
            try {
                configDataMap = OBJECT_MAPPER.readValue( configDataJson, new TypeReference<Map<String, String>>() {
                } );
            } catch (JsonProcessingException e) {
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
    public ResponseData<List<ConfigParam>> validateConfigData(String configParamJson, String configDataJson) {
        Map<String, String> configDataMap = null;
        List<ConfigParam> configParamList = null;
        if (StringUtils.isNotBlank( configDataJson )) {
            try {
                configParamList = OBJECT_MAPPER.readValue( configParamJson, new TypeReference<List<ConfigParam>>() {
                } );
            } catch (JsonProcessingException e) {
                logger.error( "配置参数解析失败！{}", e.getMessage(), e );
            }
        }
        if (StringUtils.isNotBlank( configDataJson )) {
            try {
                configDataMap = OBJECT_MAPPER.readValue( configDataJson, new TypeReference<Map<String, String>>() {
                } );
            } catch (JsonProcessingException e) {
                logger.error( "配置数据解析失败！{}", e.getMessage(), e );
            }
        }
        return validateConfigData( configParamList, configDataMap );

    }


}
