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
     * 定时换出统计列表写库（周期由DaoService调度处决定）.
     */
    @Override
    public void run() {
        ArrayList<SqlExecuteStats> list = DaoService.getStatsList();
        if (list.isEmpty()) {
            return;
        }
        writeStatsList(list);
    }

    /**
     * 执行数据库插入（迭代推进，跨日期分片时换表续写）.
     * <p>任一段写失败即丢弃整批剩余数据并记日志：统计通道允许有损，不允许为救数据把故障放大。
     * 旧递归实现在 getConnection 失败时 pos 不前进，剩余列表原样递归重试，几层之内即 StackOverflowError，
     * 且 run() 无兜底，Error 冒泡后 scheduleAtFixedRate 静默取消后续调度，dataList 随之无界增长。</p>
     *
     * @param list SqlExecuteStats集合
     */
    private void writeStatsList(List<SqlExecuteStats> list) {
        int from = 0;
        while (from < list.size()) {
            // 段内已 addBatch 未 executeBatch 的条数
            int pendingCount = 0;
            Connection conn = null;
            PreparedStatement pstmt = null;
            int pos = from;
            try {
                String tableName = ShardingTableUtils.getTableNameByDate(DaoService.STATS_BASE_TABLE,
                        list.get(from).getActionDate());
                String pdsql = "INSERT INTO " + tableName
                        + "(conn_name,conn_id,sql_info,sql_param,row_num,conn_millis,db_millis,all_millis,exception,exe_date) values "
                        + "(?,?,?,?,?,?,?,?,?,?) ";
                conn = dao.getConnection(tableName, "write");
                conn.setAutoCommit(false);
                pstmt = conn.prepareStatement(pdsql);
                for (pos = from; pos < list.size(); pos++) {
                    SqlExecuteStats ss = list.get(pos);
                    // 发现已经跨时间分片了，此时要退出本段
                    if (pos > from && !tableName.equals(
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
                    pendingCount++;
                    // 每满100条提交一次，避免单次 batch 过大
                    if (pendingCount == 100) {
                        pstmt.executeBatch();
                        pendingCount = 0;
                    }
                }
                // 提交剩余未满批次的数据
                if (pendingCount > 0) {
                    pstmt.executeBatch();
                }
            } catch (Exception e) {
                logger.error("write dao_sql_stats failed, drop the remaining stats of this round.", e);
                // 丢弃整批剩余：失败多为连接不可用，继续重试同批数据只会放大故障
                pos = list.size();
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
            from = pos;
        }
    }

}
