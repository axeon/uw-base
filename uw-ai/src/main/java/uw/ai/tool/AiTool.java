package uw.ai.tool;

import uw.common.util.JsonUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Function;

/**
 * 定义AI工具接口。
 * <p>
 * 实现 Spring Bean（标注 {@code @Component}）后，启动时由 {@link uw.ai.conf.UwAiAutoConfiguration}
 * 扫描并按 {@code toolClass + toolVersion} 与服务中心比对，新增或版本升级时自动同步工具元数据。
 * 服务中心执行工具时，通过 {@link uw.ai.controller.AiToolExecuteController} 回调本应用执行。
 *
 * @param <P>           工具参数类型，必须继承 {@link AiToolParam}（内置认证四元组）
 * @param <ResponseData> 工具返回类型，通常为 {@code uw.common.response.ResponseData<T>}
 */
public interface AiTool<P extends AiToolParam, ResponseData> extends Function<P, ResponseData> {

    /**
     * 定义工具名称（展示用）。
     *
     * @return 工具名称
     */
    String toolName();

    /**
     * 定义工具描述（供大模型理解工具用途）。
     *
     * @return 工具描述
     */
    String toolDesc();

    /**
     * 定义工具版本。
     * <p>
     * 升级工具逻辑时需递增版本号，框架据此判断是否需向服务中心同步新元数据。
     *
     * @return 工具版本
     */
    String toolVersion();

    /**
     * 定义工具参数类型。
     * <p>
     * 默认通过反射本接口的泛型参数获取。要求实现类直接声明泛型（如
     * {@code implements AiTool<MyParam, ...>}）；通过抽象类间接继承时可能返回 null。
     *
     * @return 工具参数类型，无法确定时返回 null
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
     * 将服务中心回传的 JSON 字符串反序列化为工具输入类型。
     *
     * @param toolTip 工具输入的 JSON 字符串
     * @return 反序列化后的工具参数
     * @throws IllegalStateException 当无法确定参数类型时抛出
     */
    default P convertParam(String toolTip) {
        Class<?> paramType = getParamType();
        if (paramType == null) {
            throw new IllegalStateException("Cannot determine param type for " + getClass().getName() + ", ensure generic type parameter is specified");
        }
        return (P) JsonUtils.parse(toolTip, paramType);
    }


}
