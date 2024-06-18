package uw.log.es.util;

import org.apache.commons.lang3.time.FastDateFormat;

/**
 * 索引配置Vo
 */
public class IndexConfigVo {

    /**
     * 索引名称
     */
    private final String rawName;

    /**
     * 查询索引,它通常可以是index*模式
     */
    private final String queryName;

    /**
     * 索引Pattern
     */
    private final FastDateFormat indexPattern;

    public IndexConfigVo(String rawName, String queryName, final FastDateFormat indexPattern) {
        this.rawName = rawName;
        this.queryName = queryName;
        this.indexPattern = indexPattern;
    }

    public String getRawName() {
        return rawName;
    }

    public String getQueryName() {
        return queryName;
    }

    public FastDateFormat getIndexPattern() {
        return indexPattern;
    }
}
