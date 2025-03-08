package uw.ai.tool;

import java.util.function.Function;

/**
 * 定义AI工具接口。
 */
public interface AiTool<T, R> extends Function<T, R> {

    /**
     * 定义工具名称。
     *
     * @return
     */
    String name();

    /**
     * 定义工具描述。
     *
     * @return
     */
    String desc();

    /**
     * 定义工具版本。
     *
     * @return
     */
    String version();
}
