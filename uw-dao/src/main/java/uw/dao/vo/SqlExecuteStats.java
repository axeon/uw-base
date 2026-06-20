package uw.dao.vo;

import org.apache.commons.lang3.StringUtils;
import uw.common.util.JsonUtils;
import uw.common.util.SystemClock;
import uw.dao.util.DaoValueUtils;

import java.util.Date;

/**
 * 用于统计sql执行的性能数据.
 */
public class SqlExecuteStats {

    /**
     * connName 连接名.
     */
    private String connName;

    /**
     * ConnId.
     */
    private int connId;

    /**
     * 执行的具体sql.
     */
    private String sql;

    /**
     * 附加的参数.
     */
    private Object[] paramList;

    /**
     * 返回/影响的行数.
     */
    private int rowNum;

    /**
     * Conn时间.
     */
    private long connMillis;

    /**
     * 数据库操作消耗的时间.
     */
    private long dbMillis;

    /**
     * 数据库层消耗的时间.
     */
    private long allMillis;

    /**
     * 异常类.
     */
    private String exception;

    /**
     * 动作时间.
     */
    private Date actionDate;

    /**
     * 构造 SQL 执行统计对象。
     *
     * @param connName   连接名
     * @param connId     连接 ID（Connection.hashCode）
     * @param sql        SQL语句
     * @param paramList  参数
     * @param rowNum     返回/影响的行数
     * @param connMillis 数据库层建立连接消耗的时间
     * @param dbMillis   数据库层操作数据库消耗的时间
     * @param allMillis  数据库层消耗的总时间
     * @param exception  异常
     */
    public SqlExecuteStats(String connName, int connId, String sql, Object[] paramList, int rowNum, long connMillis, long dbMillis, long allMillis,
                           String exception) {
        this.connName = connName;
        this.connId = connId;
        this.sql = sql;
        this.paramList = paramList;
        this.rowNum = rowNum;
        this.connMillis = connMillis;
        this.dbMillis = dbMillis;
        this.allMillis = allMillis;
        this.exception = exception;
    }

    /**
     * 初始化动作时间。
     */
    public void initActionDate() {
        if (this.actionDate == null) {
            this.actionDate = SystemClock.nowDate();
        }
    }

    /**
     * 输出完整 SQL 信息（参数已内联，用于日志展示）。
     *
     * @return 连接、耗时、参数化 SQL、异常信息的可读字符串
     */
    public String genFullSqlInfo() {
        StringBuilder sb = new StringBuilder(512);
        sb.append("Connection ").append(connName).append('@').append(connId).append(" run ");
        sb.append("effectNum:").append(rowNum).append(", connMillis:").append(connMillis).append(", dbMillis:").append(dbMillis).append(", allMillis:").append(allMillis).append(".");
        sb.append("\n\tsql: ").append(DaoValueUtils.combineSqlAndParam(sql, paramList));
        if (StringUtils.isNotBlank(exception)) {
            sb.append("\n\texception: ").append(exception);
        }
        return sb.toString();
    }

    /**
     * 输出参数化 SQL 信息（SQL 与参数分开，用于日志展示）。
     *
     * @return 连接、耗时、占位符 SQL、参数 JSON、异常信息的可读字符串
     */
    public String genParamSqlInfo() {
        StringBuilder sb = new StringBuilder(512);
        sb.append("Connection ").append(connName).append('@').append(connId).append(" run ");
        sb.append("effectNum:").append(rowNum).append(", connMillis:").append(connMillis).append(", dbMillis:").append(dbMillis).append(", allMillis:").append(allMillis).append(".\n");
        sb.append("\n\tsql: ").append(sql);
        if (paramList != null) {
            sb.append("\n\tparam: ").append(JsonUtils.toString(paramList));
        }
        if (StringUtils.isNotBlank(exception)) {
            sb.append("\n\texception: ").append(exception);
        }

        return sb.toString();
    }

    /**
     * 转化成字符串形式.
     *
     * @return String
     */
    @Override
    public String toString() {
        return JsonUtils.toString(this);
    }

    /**
     * 获取连接名。
     *
     * @return 连接名
     */
    public String getConnName() {
        return connName;
    }

    /**
     * 设置连接名。
     *
     * @param connName 连接名
     */
    public void setConnName(String connName) {
        this.connName = connName;
    }

    /**
     * 获取执行的 SQL（含 ? 占位符）。
     *
     * @return SQL
     */
    public String getSql() {
        return sql;
    }

    /**
     * 设置执行的 SQL。
     *
     * @param sql SQL
     */
    public void setSql(String sql) {
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

    /**
     * 获取返回/影响行数。
     *
     * @return 行数
     */
    public int getRowNum() {
        return rowNum;
    }

    /**
     * 设置返回/影响行数。
     *
     * @param rowNum 行数
     */
    public void setRowNum(int rowNum) {
        this.rowNum = rowNum;
    }

    /**
     * 获取数据库执行耗时（毫秒）。
     *
     * @return 数据库执行毫秒数
     */
    public long getDbMillis() {
        return dbMillis;
    }

    /**
     * 设置数据库执行耗时（毫秒）。
     *
     * @param dbMillis 数据库执行毫秒数
     */
    public void setDbMillis(long dbMillis) {
        this.dbMillis = dbMillis;
    }

    /**
     * 获取总耗时（毫秒）。
     *
     * @return 总毫秒数
     */
    public long getAllMillis() {
        return allMillis;
    }

    /**
     * 设置总耗时（毫秒）。
     *
     * @param allMillis 总毫秒数
     */
    public void setAllMillis(long allMillis) {
        this.allMillis = allMillis;
    }

    /**
     * 获取异常信息（无异常为 null）。
     *
     * @return 异常信息
     */
    public String getException() {
        return exception;
    }

    /**
     * 设置异常信息。
     *
     * @param exception 异常信息
     */
    public void setException(String exception) {
        this.exception = exception;
    }

    /**
     * 获取执行时间。
     *
     * @return 执行时间
     */
    public Date getActionDate() {
        return actionDate;
    }

    /**
     * 设置执行时间。
     *
     * @param actionDate 执行时间
     */
    public void setActionDate(Date actionDate) {
        this.actionDate = actionDate;
    }

    /**
     * 获取连接 ID（Connection.hashCode，用于关联同一连接的多次操作）。
     *
     * @return 连接 ID
     */
    public int getConnId() {
        return connId;
    }

    /**
     * 设置连接 ID。
     *
     * @param connId 连接 ID
     */
    public void setConnId(int connId) {
        this.connId = connId;
    }

    /**
     * 获取获取连接耗时（毫秒）。
     *
     * @return 获取连接毫秒数
     */
    public long getConnMillis() {
        return connMillis;
    }

    /**
     * 设置获取连接耗时（毫秒）。
     *
     * @param connMillis 获取连接毫秒数
     */
    public void setConnMillis(long connMillis) {
        this.connMillis = connMillis;
    }
}
