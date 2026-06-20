package uw.log.es.util;

import org.apache.commons.lang3.time.FastDateFormat;

/**
 * 索引配置Vo。
 * <p>承载某个日志类型注册后的索引信息：原始名、查询名（可带通配）、时间滚动模式。
 */
public class IndexConfigVo {

    /**
     * 原始索引名（不含时间后缀与通配符）。
     */
    private final String rawName;

    /**
     * 查询索引名，设置模式时为 {@code rawName_*} 形式。
     */
    private final String queryName;

    /**
     * 索引时间滚动模式，为 {@code null} 表示不按时间分索引。
     */
    private final FastDateFormat indexPattern;

    /**
     * 构造索引配置。
     *
     * @param rawName      原始索引名
     * @param queryName    查询索引名
     * @param indexPattern 索引时间滚动模式
     */
    public IndexConfigVo(String rawName, String queryName, final FastDateFormat indexPattern) {
        this.rawName = rawName;
        this.queryName = queryName;
        this.indexPattern = indexPattern;
    }

    /**
     * 获取原始索引名。
     *
     * @return 原始索引名
     */
    public String getRawName() {
        return rawName;
    }

    /**
     * 获取查询索引名。
     *
     * @return 查询索引名
     */
    public String getQueryName() {
        return queryName;
    }

    /**
     * 获取索引时间滚动模式。
     *
     * @return 时间模式，无模式时为 {@code null}
     */
    public FastDateFormat getIndexPattern() {
        return indexPattern;
    }
}
