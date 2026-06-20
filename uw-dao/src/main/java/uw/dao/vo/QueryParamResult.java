package uw.dao.vo;


import uw.dao.util.DaoValueUtils;

/**
 * QueryParam 解析结果。
 * <p>持有 {@link uw.dao.util.QueryParamUtils#parseQueryParam} 解析出的 SQL 片段（含 where/order by 等）
 * 与对应的绑定参数数组，供后续 {@code prepareStatement} 绑定执行。</p>
 */
public class QueryParamResult {


    /**
     * 解析出的查询 SQL（含 select/where/order by，占位符为 ?）。
     */
    private StringBuilder sql;

    /**
     * 与 SQL 占位符一一对应的绑定参数数组。
     */
    private Object[] paramList;

    /**
     * 生成带参数替换的完整 SQL（仅用于日志/调试展示，不可用于实际执行）。
     *
     * @return 参数已内联的完整 SQL 字符串
     */
    public String genFullSql() {
        return DaoValueUtils.combineSqlAndParam(sql.toString(), paramList);
    }

    /**
     * 构造解析结果。
     *
     * @param sql       查询 SQL（StringBuilder，含 ? 占位符）
     * @param paramList 绑定参数数组
     */
    public QueryParamResult(StringBuilder sql, Object[] paramList) {
        this.sql = sql;
        this.paramList = paramList;
    }

    /**
     * 获取查询 SQL。
     *
     * @return SQL（StringBuilder）
     */
    public StringBuilder getSql() {
        return sql;
    }

    /**
     * 设置查询 SQL。
     *
     * @param sql SQL（StringBuilder）
     */
    public void setSql(StringBuilder sql) {
        this.sql = sql;
    }

    /**
     * 获取绑定参数数组。
     *
     * @return 参数数组
     */
    public Object[] getParamList() {
        return paramList;
    }

    /**
     * 设置绑定参数数组。
     *
     * @param paramList 参数数组
     */
    public void setParamList(Object[] paramList) {
        this.paramList = paramList;
    }


}
