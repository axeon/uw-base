package uw.common.vo;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.util.JsonUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * 配置参数盒子类。
 * 使用Json保存配置信息，同时支持强类型校验和参数获取。
 */
public class ConfigParamBox {

    /**
     * 空的配置参数盒子。
     */
    public static final ConfigParamBox EMPTY_PARAM_BOX = new ConfigParamBox( new HashMap<>( 0 ) );
    private static final Logger logger = LoggerFactory.getLogger( ConfigParamBox.class );
    /**
     * 配置混合Map。
     */
    public final Map<String, String> configMixMap;


    public ConfigParamBox(Map<String, String> configMixMap) {
        this.configMixMap = configMixMap;
    }

    /**
     * 获取字符串参数。
     *
     * @param paramName
     * @return
     */
    public String getParam(String paramName) {
        return getParam( paramName, StringUtils.EMPTY );
    }

    /**
     * 获取字符串参数。
     *
     * @param paramName 参数名字
     * @return 参数的值
     */
    public String[] getParams(String paramName) {
        String temp = configMixMap.get( paramName );
        if (StringUtils.isBlank( temp )) {
            return new String[0];
        }
        return JsonUtils.parse( temp, String[].class );
    }

    /**
     * 获取字符串参数。
     *
     * @param paramName    参数名字
     * @param defaultValue 集合没值时或者报异常，默认返回值
     * @return 参数的值
     */
    public String getParam(String paramName, String defaultValue) {
        String temp = configMixMap.get( paramName );
        if (temp != null && !temp.isEmpty()) {
            return temp;
        }
        return defaultValue;
    }

    /**
     * 获取整数参数。
     *
     * @param paramName 参数名字
     * @return 参数的值
     */
    public int getIntParam(String paramName) {
        return getIntParam( paramName, 0 );
    }

    /**
     * 获取整数参数。
     *
     * @param paramName 参数名字
     * @return 参数的值
     */
    public int[] getIntParams(String paramName) {
        String temp = configMixMap.get( paramName );
        if (StringUtils.isBlank( temp )) {
            return new int[0];
        }
        return JsonUtils.parse( temp, int[].class );
    }

    /**
     * 获取整数参数。
     *
     * @param paramName    参数名字
     * @param defaultValue 集合没值时或者报异常，默认的返回值
     * @return 参数的值
     */
    public int getIntParam(String paramName, int defaultValue) {
        String temp = configMixMap.get( paramName );
        if (temp != null && !temp.isEmpty()) {
            try {
                return Integer.parseInt( temp );
            } catch (Exception ignored) {
            }
        }
        return defaultValue;
    }

    /**
     * 获取长整数参数。
     *
     * @param paramName 参数名字
     */
    public long getLongParam(String paramName) {
        return getLongParam( paramName, 0 );
    }

    /**
     * 获取长整数参数。
     *
     * @param paramName
     * @return
     */
    public long[] getLongParams(String paramName) {
        String temp = configMixMap.get( paramName );
        if (StringUtils.isBlank( temp )) {
            return new long[0];
        }
        return JsonUtils.parse( temp, long[].class );
    }

    /**
     * 获取长整数参数。
     *
     * @param paramName    参数名字
     * @param defaultValue 集合没值时或者报异常，默认的返回值
     * @return 参数的值
     */
    public long getLongParam(String paramName, long defaultValue) {
        String temp = configMixMap.get( paramName );
        if (temp != null && !temp.isEmpty()) {
            try {
                return Long.parseLong( temp );
            } catch (Exception ignored) {
            }
        }
        return defaultValue;
    }

    /**
     * 获取Double参数。
     *
     * @param paramName 参数名字
     */
    public double getDoubleParam(String paramName) {
        return getDoubleParam( paramName, 0 );
    }

    /**
     * 获取Double参数。
     *
     * @param paramName
     * @return
     */
    public double[] getDoubleParams(String paramName) {
        String temp = configMixMap.get( paramName );
        if (StringUtils.isBlank( temp )) {
            return new double[0];
        }
        return JsonUtils.parse( temp, double[].class );
    }

    /**
     * 获取Double参数。
     *
     * @param paramName    参数名字
     * @param defaultValue 集合没值时或者报异常，默认的返回值
     * @return 参数的值
     */
    public double getDoubleParam(String paramName, double defaultValue) {
        String temp = configMixMap.get( paramName );
        if (temp != null && !temp.isEmpty()) {
            try {
                return Double.parseDouble( temp );
            } catch (Exception ignored) {
            }
        }
        return defaultValue;

    }

    /**
     * 获取浮点数参数。
     *
     * @param paramName
     * @return
     */
    public float getFloatParam(String paramName) {
        return getFloatParam( paramName, 0 );
    }

    /**
     * 获取浮点数参数。
     *
     * @param paramName
     * @return
     */
    public float[] getFloatParams(String paramName) {
        String temp = configMixMap.get( paramName );
        if (StringUtils.isBlank( temp )) {
            return new float[0];
        }
        return JsonUtils.parse( temp, float[].class );
    }

    /**
     * 获取浮点数参数。
     *
     * @param paramName    参数名字
     * @param defaultValue 集合没值时或者报异常，默认的返回值
     * @return 参数的值
     */
    public float getFloatParam(String paramName, float defaultValue) {
        String temp = configMixMap.get( paramName );
        if (temp != null && !temp.isEmpty()) {
            try {
                return Float.parseFloat( temp );
            } catch (Exception ignored) {
            }
        }
        return defaultValue;
    }

    /**
     * 获取boolean参数。
     *
     * @param paramName
     * @return
     */
    public boolean getBooleanParam(String paramName) {
        return getBooleanParam( paramName, false );
    }

    /**
     * 获取boolean参数。
     *
     * @param paramName
     * @return
     */
    public boolean[] getBooleanParams(String paramName) {
        String temp = configMixMap.get( paramName );
        if (StringUtils.isBlank( temp )) {
            return new boolean[0];
        }
        return JsonUtils.parse( temp, boolean[].class );
    }

    /**
     * 获取boolean参数。
     *
     * @param paramName    参数名字
     * @param defaultValue 集合没值时或者报异常，默认的返回值
     * @return 参数的值
     */
    public boolean getBooleanParam(String paramName, boolean defaultValue) {
        String temp = configMixMap.get( paramName );
        if (temp != null && !temp.isEmpty()) {
            try {
                return Boolean.parseBoolean( temp );
            } catch (Exception ignored) {
            }
        }
        return defaultValue;
    }

    /**
     * 获取Map参数。
     *
     * @param paramName
     * @return
     */
    public Map<String, String> getMapParam(String paramName) {
        String temp = configMixMap.get( paramName );
        if (StringUtils.isBlank( temp )) {
            return Collections.emptyMap();
        }
        return JsonUtils.parse( temp, new TypeReference<Map<String, String>>() {
        } );
    }

}
