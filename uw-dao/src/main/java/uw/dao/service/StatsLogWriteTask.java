package uw.dao.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.util.JsonUtils;
import uw.dao.DaoFactory;
import uw.dao.util.DaoValueUtils;
import uw.dao.util.ShardingTableUtils;
import uw.dao.vo.SqlExecuteStats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 性能日志写入任务.
 *
 * @author axeon
 */
public class StatsLogWriteTask implements Runnable {
    /**
     * 日志.
     */
    private static final Logger logger = LoggerFactory.getLogger(StatsLogWriteTask.class);
    /**
     * DAOFactory对象.
     */
    private final DaoFactory dao = DaoFactory.getInstance();

    /**
     * 10秒写一次数据.
     */
    @Override
    public void run() {
        ArrayList<SqlExecuteStats> list = DaoService.getStatsList();
        if (list.size() == 0) {
            return;
        }
        writeStatsList(list);
    }

    /**
     * 执行数据库插入.
     *
     * @param list SqlExecuteStats集合
     */
    private void writeStatsList(List<SqlExecuteStats> list) {

        String tableName = ShardingTableUtils.getTableNameByDate(DaoService.STATS_BASE_TABLE,
                list.getFirst().getActionDate());
        Connection conn = null;
        PreparedStatement pstmt = null;
        String pdsql = "INSERT INTO " + tableName
                + "(conn_name,conn_id,sql_info,sql_param,row_num,conn_millis,db_millis,all_millis,exception,exe_date) values "
                + "(?,?,?,?,?,?,?,?,?,?) ";
        int pos = 0;
        try {
            conn = dao.getConnection(tableName, "write");
            conn.setAutoCommit(false);
            pstmt = conn.prepareStatement(pdsql);
            for (pos = 0; pos < list.size(); pos++) {
                SqlExecuteStats ss = list.get(pos);
                // 发现已经跨时间分片了，此时要退出
                if (pos > 0 && !tableName.equals(
                        ShardingTableUtils.getTableNameByDate(DaoService.STATS_BASE_TABLE, ss.getActionDate()))) {
                    break;
                }
                if (ss.getConnName() != null && ss.getConnName().length() > 100) {
                    ss.setConnName(ss.getConnName().substring(0, 100));
                }
                pstmt.setString(1, ss.getConnName());
                pstmt.setInt(2, ss.getConnId());
                if (ss.getSql() != null && ss.getSql().length() > 2000) {
                    ss.setSql(ss.getSql().substring(0, 2000));
                }
                pstmt.setString(3, ss.getSql());
                String paramSqlInfo = JsonUtils.toString(ss.getParamList());
                if (paramSqlInfo != null && paramSqlInfo.length() > 2000) {
                    paramSqlInfo = paramSqlInfo.substring(0, 2000);
                }
                pstmt.setString(4, paramSqlInfo);
                pstmt.setInt(5, ss.getRowNum());
                pstmt.setInt(6, (int) ss.getConnMillis());
                pstmt.setInt(7, (int) ss.getDbMillis());
                pstmt.setInt(8, (int) ss.getAllMillis());
                if (ss.getException() != null && ss.getException().length() > 1000) {
                    ss.setException(ss.getException().substring(0, 1000));
                }
                pstmt.setString(9, ss.getException());
                pstmt.setTimestamp(10, DaoValueUtils.dateToTimestamp(ss.getActionDate()));
                pstmt.addBatch();
                if ((pos + 1) % 100 == 0) {
                    // 每隔100次自动提交
                    pstmt.executeBatch();
                }
            }
            // 剩余部分也要执行提交。
            if (pos % 100 > 0) {
                pstmt.executeBatch();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    logger.error(e.getMessage(), e);
                }
                try {
                    conn.close();
                } catch (SQLException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        // 接着往这里写
        if (list.size() - pos >= 1) {
            writeStatsList(list.subList(pos, list.size()));
        }
    }

}
