package uw.dao.vo;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import uw.dao.util.DaoValueUtils;

import java.util.Arrays;
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
    private long connTime;

    /**
     * 数据库操作消耗的时间.
     */
    private long dbTime;

    /**
     * 数据库层消耗的时间.
     */
    private long allTime;

    /**
     * 异常类.
     */
    private String exception;

    /**
     * 动作时间.
     */
    private Date actionDate;

    /**
     * SqlExecuteStats对象.
     *
     * @param connName  连接名
     * @param sql       SQL语句
     * @param paramList 参数
     * @param rowNum    返回/影响的行数
     * @param dbTime    数据库操作消耗的时间
     * @param allTime   数据库层消耗的时间
     * @param exception 异常
     */
    public SqlExecuteStats(String connName, int connId, String sql, Object[] paramList, int rowNum, long connTime, long dbTime, long allTime,
                           String exception) {
        this.connName = connName;
        this.connId = connId;
        this.sql = sql;
        this.paramList = paramList;
        this.rowNum = rowNum;
        this.connTime = connTime;
        this.dbTime = dbTime;
        this.allTime = allTime;
        this.exception = exception;
    }

    /**
     * 初始化动作时间。
     */
    public void initActionDate() {
        if (this.actionDate == null)
            this.actionDate = new Date();
    }


    /**
     * 输出完整sql信息。
     *
     * @return
     */
    public String genFullSqlInfo() {
        StringBuilder sb = new StringBuilder(512);
        sb.append("Connection ").append(connName).append('@').append(connId).append(" run ");
        sb.append("effectNum:").append(rowNum).append(", connTime:").append(connTime).append(", dbTime:").append(dbTime).append(", allTime:").append(allTime).append(".");
        sb.append("\n\tsql: ").append(DaoValueUtils.combineSqlAndParam(sql, paramList));
        if (StringUtils.isNotBlank(exception)) {
            sb.append("\n\texception: ").append(exception);
        }
        return sb.toString();
    }

    /**
     * 输出参数化sql信息。
     *
     * @return
     */
    public String genParamSqlInfo() {
        StringBuilder sb = new StringBuilder(512);
        sb.append("Connection ").append(connName).append('@').append(connId).append(" run ");
        sb.append("effectNum:").append(rowNum).append(", connTime:").append(connTime).append(", dbTime:").append(dbTime).append(", allTime:").append(allTime).append(".\n");
        sb.append("\n\tsql: ").append(sql);
        if (paramList != null) {
            sb.append("\n\tparam: ").append(Arrays.toString(paramList));
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
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    /**
     * @return the connName
     */
    public String getConnName() {
        return connName;
    }

    /**
     * @param connName the connName to set
     */
    public void setConnName(String connName) {
        this.connName = connName;
    }

    /**
     * @return the sql
     */
    public String getSql() {
        return sql;
    }

    /**
     * @param sql the sql to set
     */
    public void setSql(String sql) {
        this.sql = sql;
    }

    public Object[] getParamList() {
        return paramList;
    }

    public void setParamList(Object[] paramList) {
        this.paramList = paramList;
    }

    /**
     * @return the rowNum
     */
    public int getRowNum() {
        return rowNum;
    }

    /**
     * @param rowNum the rowNum to set
     */
    public void setRowNum(int rowNum) {
        this.rowNum = rowNum;
    }

    /**
     * @return the dbTime
     */
    public long getDbTime() {
        return dbTime;
    }

    /**
     * @param dbTime the dbTime to set
     */
    public void setDbTime(long dbTime) {
        this.dbTime = dbTime;
    }

    /**
     * @return the allTime
     */
    public long getAllTime() {
        return allTime;
    }

    /**
     * @param allTime the allTime to set
     */
    public void setAllTime(long allTime) {
        this.allTime = allTime;
    }

    /**
     * @return the exception
     */
    public String getException() {
        return exception;
    }

    /**
     * @param exception the exception to set
     */
    public void setException(String exception) {
        this.exception = exception;
    }

    /**
     * @return the actionDate
     */
    public Date getActionDate() {
        return actionDate;
    }

    /**
     * @param actionDate the actionDate to set
     */
    public void setActionDate(Date actionDate) {
        this.actionDate = actionDate;
    }

    public int getConnId() {
        return connId;
    }

    public void setConnId(int connId) {
        this.connId = connId;
    }

    public long getConnTime() {
        return connTime;
    }

    public void setConnTime(long connTime) {
        this.connTime = connTime;
    }
}
