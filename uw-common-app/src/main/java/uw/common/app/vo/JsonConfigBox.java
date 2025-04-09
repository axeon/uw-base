package uw.common.app.vo;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.util.JsonUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * Json配置参数盒子。
 * 使用Json保存配置信息，同时支持强类型校验和参数获取。
 */
public class JsonConfigBox {

    /**
     * 空的配置参数盒子。
     */
    public static final JsonConfigBox EMPTY_PARAM_BOX = new JsonConfigBox( new HashMap<>( 0 ) );
    /**
     * 日志记录器。
     */
    private static final Logger logger = LoggerFactory.getLogger( JsonConfigBox.class );
    /**
     * 配置混合Map。
     */
    public final Map<String, String> configMixMap;


    public JsonConfigBox(Map<String, String> configMixMap) {
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

    /**
     * 根据配置参数对象获取字符串参数值。
     * 若参数不存在或值为空，返回空字符串。
     *
     * @param param 配置参数对象，用于获取参数名称和类型
     * @return 参数的字符串值，若未找到或值为空则返回空字符串
     */
    public String getParam(JsonConfigParam param) {
        return getParam( param.getKey() );
    }

    /**
     * 根据配置参数对象获取字符串参数值。
     * 若参数不存在或值为空，则返回默认值。
     *
     * @param param        配置参数对象，用于获取参数名称和类型
     * @param defaultValue 参数未找到时返回的默认值
     * @return 参数的字符串值或默认值
     */
    public String getParam(JsonConfigParam param, String defaultValue) {
        return getParam( param.getKey(), defaultValue );
    }

    /**
     * 根据配置参数对象获取字符串数组参数值。
     * 若参数不存在或值为空，返回空数组。
     *
     * @param param 配置参数对象，用于获取参数名称和类型
     * @return 参数的字符串数组值，若未找到或解析失败则返回空数组
     */
    public String[] getParams(JsonConfigParam param) {
        return getParams( param.getKey() );
    }

    /**
     * 根据配置参数对象获取整数参数值。
     * 若参数不存在、值为空或解析失败，返回默认值 0。
     *
     * @param param 配置参数对象，用于获取参数名称和类型
     * @return 参数的整数值，解析失败时返回 0
     */
    public int getIntParam(JsonConfigParam param) {
        return getIntParam( param.getKey() );
    }

    /**
     * 根据配置参数对象获取整数参数值。
     * 若参数不存在、值为空或解析失败，返回默认值。
     *
     * @param param        配置参数对象，用于获取参数名称和类型
     * @param defaultValue 参数未找到或解析失败时返回的默认值
     * @return 参数的整数值或默认值
     */
    public int getIntParam(JsonConfigParam param, int defaultValue) {
        return getIntParam( param.getKey(), defaultValue );
    }

    /**
     * 根据配置参数对象获取整数数组参数值。
     * 若参数不存在或值为空，返回空数组。
     *
     * @param param 配置参数对象，用于获取参数名称和类型
     * @return 参数的整数数组值，若未找到或解析失败则返回空数组
     */
    public int[] getIntParams(JsonConfigParam param) {
        return getIntParams( param.getKey() );
    }

    /**
     * 根据配置参数对象获取长整数参数值。
     * 若参数不存在、值为空或解析失败，返回默认值 0。
     *
     * @param param 配置参数对象，用于获取参数名称和类型
     * @return 参数的长整数值，解析失败时返回 0
     */
    public long getLongParam(JsonConfigParam param) {
        return getLongParam( param.getKey() );
    }

    /**
     * 根据配置参数对象获取长整数参数值。
     * 若参数不存在、值为空或解析失败，返回默认值。
     *
     * @param param        配置参数对象，用于获取参数名称和类型
     * @param defaultValue 参数未找到或解析失败时返回的默认值
     * @return 参数的长整数值或默认值
     */
    public long getLongParam(JsonConfigParam param, long defaultValue) {
        return getLongParam( param.getKey(), defaultValue );
    }

    /**
     * 根据配置参数对象获取长整数数组参数值。
     * 若参数不存在或值为空，返回空数组。
     *
     * @param param 配置参数对象，用于获取参数名称和类型
     * @return 参数的长整数数组值，若未找到或解析失败则返回空数组
     */
    public long[] getLongParams(JsonConfigParam param) {
        return getLongParams( param.getKey() );
    }

    /**
     * 根据配置参数对象获取浮点数参数值。
     * 若参数不存在、值为空或解析失败，返回默认值 0.0。
     *
     * @param param 配置参数对象，用于获取参数名称和类型
     * @return 参数的浮点数值，解析失败时返回 0.0
     */
    public float getFloatParam(JsonConfigParam param) {
        return getFloatParam( param.getKey() );
    }

    /**
     * 根据配置参数对象获取浮点数参数值。
     * 若参数不存在、值为空或解析失败，返回默认值。
     *
     * @param param        配置参数对象，用于获取参数名称和类型
     * @param defaultValue 参数未找到或解析失败时返回的默认值
     * @return 参数的浮点数值或默认值
     */
    public float getFloatParam(JsonConfigParam param, float defaultValue) {
        return getFloatParam( param.getKey(), defaultValue );
    }

    /**
     * 根据配置参数对象获取浮点数数组参数值。
     * 若参数不存在或值为空，返回空数组。
     *
     * @param param 配置参数对象，用于获取参数名称和类型
     * @return 参数的浮点数数组值，若未找到或解析失败则返回空数组
     */
    public float[] getFloatParams(JsonConfigParam param) {
        return getFloatParams( param.getKey() );
    }

    /**
     * 根据配置参数对象获取双精度参数值。
     * 若参数不存在、值为空或解析失败，返回默认值 0.0。
     *
     * @param param 配置参数对象，用于获取参数名称和类型
     * @return 参数的双精度值，解析失败时返回 0.0
     */
    public double getDoubleParam(JsonConfigParam param) {
        return getDoubleParam( param.getKey() );
    }

    /**
     * 根据配置参数对象获取双精度参数值。
     * 若参数不存在、值为空或解析失败，返回默认值。
     *
     * @param param        配置参数对象，用于获取参数名称和类型
     * @param defaultValue 参数未找到或解析失败时返回的默认值
     * @return 参数的双精度值或默认值
     */
    public double getDoubleParam(JsonConfigParam param, double defaultValue) {
        return getDoubleParam( param.getKey(), defaultValue );
    }

    /**
     * 根据配置参数对象获取双精度数组参数值。
     * 若参数不存在或值为空，返回空数组。
     *
     * @param param 配置参数对象，用于获取参数名称和类型
     * @return 参数的双精度数组值，若未找到或解析失败则返回空数组
     */
    public double[] getDoubleParams(JsonConfigParam param) {
        return getDoubleParams( param.getKey() );
    }

    /**
     * 根据配置参数对象获取布尔型参数值。
     * 若参数不存在、值为空或解析失败，返回默认值 false。
     *
     * @param param 配置参数对象，用于获取参数名称和类型
     * @return 参数的布尔值，解析失败时返回 false
     */
    public boolean getBooleanParam(JsonConfigParam param) {
        return getBooleanParam( param.getKey() );
    }

    /**
     * 根据配置参数对象获取布尔型参数值。
     * 若参数不存在、值为空或解析失败，返回默认值。
     *
     * @param param        配置参数对象，用于获取参数名称和类型
     * @param defaultValue 参数未找到或解析失败时返回的默认值
     * @return 参数的布尔值或默认值
     */
    public boolean getBooleanParam(JsonConfigParam param, boolean defaultValue) {
        return getBooleanParam( param.getKey(), defaultValue );
    }

    /**
     * 根据配置参数对象获取布尔型数组参数值。
     * 若参数不存在或值为空，返回空数组。
     *
     * @param param 配置参数对象，用于获取参数名称和类型
     * @return 参数的布尔数组值，若未找到或解析失败则返回空数组
     */
    public boolean[] getBooleanParams(JsonConfigParam param) {
        return getBooleanParams( param.getKey() );
    }

    /**
     * 根据配置参数对象获取 Map 类型参数值。
     * 若参数不存在或值为空，返回空 Map。
     *
     * @param param 配置参数对象，用于获取参数名称和类型
     * @return 参数的 Map 值（String 到 String 的映射），若未找到或解析失败则返回空 Map
     */
    public Map<String, String> getMapParam(JsonConfigParam param) {
        return getMapParam( param.getKey() );
    }
}




