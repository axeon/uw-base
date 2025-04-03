package uw.app.common.helper;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.app.common.constant.ConfigParamType;
import uw.common.dto.ResponseData;
import uw.common.util.DateUtils;
import uw.common.util.JsonUtils;
import uw.app.common.vo.ConfigParam;
import uw.app.common.vo.ConfigParamBox;

import java.util.*;

public class ConfigParamHelper {

    /**
     * 对象映射器。
     */
    private static final Logger logger = LoggerFactory.getLogger( ConfigParamHelper.class );


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
    public static ResponseData<ConfigParamBox> buildParamBox(String configParamJson, String configDataJson) {
        List<ConfigParam> configParams = null;
        Map<String, String> configDataMap = null;
        if (StringUtils.isNotBlank( configParamJson )) {
            try {
                configParams = Arrays.asList( JsonUtils.parse( configParamJson, ConfigParam[].class ) );
            } catch (Exception e) {
                logger.error( "配置参数解析失败！{}", e.getMessage(), e );
                return ResponseData.error( ConfigParamBox.EMPTY_PARAM_BOX, "", "配置参数解析失败！" + e.getMessage() );
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
            if (configParam.getType().equals( ConfigParamType.STRING.getValue() )) {
                if (StringUtils.isBlank( paramValue )) {
                    errorParamList.add( configParam );
                }
            } else if (configParam.getType().equals( ConfigParamType.INT.getValue() )) {
                try {
                    Integer.parseInt( paramValue );
                } catch (Exception ignored) {
                    errorParamList.add( configParam );
                }
            } else if (configParam.getType().equals( ConfigParamType.LONG.getValue() )) {
                try {
                    Long.parseLong( paramValue );
                } catch (Exception ignored) {
                    errorParamList.add( configParam );
                }
            } else if (configParam.getType().equals( ConfigParamType.FLOAT.getValue() )) {
                try {
                    Float.parseFloat( paramValue );
                } catch (Exception ignored) {
                    errorParamList.add( configParam );
                }
            } else if (configParam.getType().equals( ConfigParamType.DOUBLE.getValue() )) {
                try {
                    Double.parseDouble( paramValue );
                } catch (Exception ignored) {
                    errorParamList.add( configParam );
                }

            } else if (configParam.getType().equals( ConfigParamType.BOOLEAN.getValue() )) {
                try {
                    Boolean.parseBoolean( paramValue );
                } catch (Exception ignored) {
                    errorParamList.add( configParam );
                }
            } else if (configParam.getType().equals( ConfigParamType.DATE.getValue() )) {
                if (DateUtils.stringToDate( paramValue, DateUtils.DATE ) == null) {
                    errorParamList.add( configParam );
                }
            } else if (configParam.getType().equals( ConfigParamType.TIME.getValue() )) {
                if (DateUtils.stringToDate( paramValue, DateUtils.TIME ) == null) {
                    errorParamList.add( configParam );
                }
            } else if (configParam.getType().equals( ConfigParamType.DATETIME.getValue() )) {
                if (DateUtils.stringToDate( paramValue, DateUtils.DATE_TIME ) == null) {
                    errorParamList.add( configParam );
                }
            } else if (configParam.getType().equals( ConfigParamType.ENUM.getValue() )) {
                Set<String> enumSet = null;
                try {
                    enumSet = JsonUtils.parse( configParam.getValue(), new TypeReference<Set<String>>() {
                    } );
                } catch (Exception ignored) {
                }
                if (enumSet == null || !enumSet.contains( paramValue )) {
                    errorParamList.add( configParam );
                }
            } else if (configParam.getType().equals( ConfigParamType.MAP.getValue() )) {
                try {
                    Map<String, String> map = JsonUtils.parse( paramValue, new TypeReference<Map<String, String>>() {
                    } );
                } catch (Exception ignored) {
                    errorParamList.add( configParam );
                }
            } else if (configParam.getType().equals( ConfigParamType.SET_STRING.getValue() )) {
                try {
                    JsonUtils.parse( paramValue, new TypeReference<List<String>>() {
                    } );
                } catch (Exception ignored) {
                    errorParamList.add( configParam );
                }
            } else if (configParam.getType().equals( ConfigParamType.SET_INT.getValue() )) {
                try {
                    String[] list = JsonUtils.parse( paramValue, String[].class );
                    for (String s : list) {
                        Integer.parseInt( s );
                    }
                } catch (Exception ignored) {
                    errorParamList.add( configParam );
                }
            } else if (configParam.getType().equals( ConfigParamType.SET_LONG.getValue() )) {
                try {
                    String[] list = JsonUtils.parse( paramValue, String[].class );
                    for (String s : list) {
                        Long.parseLong( s );
                    }
                } catch (Exception ignored) {
                    errorParamList.add( configParam );
                }

            } else if (configParam.getType().equals( ConfigParamType.SET_DOUBLE.getValue() )) {
                try {
                    String[] list = JsonUtils.parse( paramValue, String[].class );
                    for (String s : list) {
                        Double.parseDouble( s );
                    }
                } catch (Exception ignored) {
                    errorParamList.add( configParam );
                }

            } else if (configParam.getType().equals( ConfigParamType.SET_FLOAT.getValue() )) {
                try {
                    String[] list = JsonUtils.parse( paramValue, String[].class );
                    for (String s : list) {
                        Float.parseFloat( s );
                    }
                } catch (Exception ignored) {
                    errorParamList.add( configParam );
                }

            } else if (configParam.getType().equals( ConfigParamType.SET_BOOLEAN.getValue() )) {
                try {
                    String[] list = JsonUtils.parse( paramValue, String[].class );
                    for (String s : list) {
                        Boolean.parseBoolean( s );
                    }
                } catch (Exception ignored) {
                    errorParamList.add( configParam );
                }

            } else if (configParam.getType().equals( ConfigParamType.SET_DATE.getValue() )) {
                try {
                    String[] list = JsonUtils.parse( paramValue, String[].class );
                    for (String s : list) {
                        if (DateUtils.stringToDate( s, DateUtils.DATE ) == null) {
                            errorParamList.add( configParam );
                            break;
                        }
                    }
                } catch (Exception ignored) {
                    errorParamList.add( configParam );
                }
            } else if (configParam.getType().equals( ConfigParamType.SET_TIME.getValue() )) {
                try {
                    String[] list = JsonUtils.parse( paramValue, String[].class );
                    for (String s : list) {
                        if (DateUtils.stringToDate( s, DateUtils.TIME ) == null) {
                            errorParamList.add( configParam );
                            break;
                        }
                    }
                } catch (Exception ignored) {
                    errorParamList.add( configParam );
                }
            } else if (configParam.getType().equals( ConfigParamType.SET_DATETIME.getValue() )) {
                try {
                    String[] list = JsonUtils.parse( paramValue, String[].class );
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
    public ResponseData<List<ConfigParam>> validateConfigData(String configParamJson, String configDataJson) {
        Map<String, String> configDataMap = null;
        List<ConfigParam> configParamList = null;
        if (StringUtils.isNotBlank( configDataJson )) {
            try {
                configParamList = JsonUtils.parse( configParamJson, new TypeReference<List<ConfigParam>>() {
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
