package uw.dao.vo;


import uw.dao.util.DaoValueUtils;

/**
 * query参数解析结果。
 */
public class QueryParamResult {


    /**
     * 查询sql。
     */
    private StringBuilder sql;

    /**
     * 参数结果。
     */
    private Object[] paramList;

    /**
     * 生成带参数的完整sql。
     *
     * @return
     */
    public String genFullSql() {
        return DaoValueUtils.combineSqlAndParam(sql.toString(), paramList);
    }

    public QueryParamResult(StringBuilder sql, Object[] paramList) {
        this.sql = sql;
        this.paramList = paramList;
    }

    public StringBuilder getSql() {
        return sql;
    }

    public void setSql(StringBuilder sql) {
        this.sql = sql;
    }

    public Object[] getParamList() {
        return paramList;
    }

    public void setParamList(Object[] paramList) {
        this.paramList = paramList;
    }


}
