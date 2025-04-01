package uw.ai.tool;

import uw.common.util.JsonUtils;
import uw.httpclient.json.JsonInterfaceHelper;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Function;

/**
 * 定义AI工具接口。
 */
public interface AiTool<P extends AiToolParam, ResponseData> extends Function<P, ResponseData> {

    /**
     * 定义工具名称。
     *
     * @return
     */
    String toolName();

    /**
     * 定义工具描述。
     *
     * @return
     */
    String toolDesc();

    /**
     * 定义工具版本。
     *
     * @return
     */
    String toolVersion();

    /**
     * 定义工具参数类型。
     *
     * @return
     */
    default Class<?> getParamType() {
        if (getClass().getGenericInterfaces()[0] instanceof ParameterizedType parameterizedType) {
            Type[] types = parameterizedType.getActualTypeArguments();
            if (types[0] instanceof Class) {
                return (Class<?>) types[0];
            }
        }
        return null;
    }

    /**
     * 将前端出入的Map结构转成输入类型。
     *
     * @param toolTip
     * @return
     */
    default P convertParam(String toolTip) {
        return (P) JsonUtils.parse( toolTip, getParamType() );
    }


}
