package uw.dao.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.util.SystemClock;
import uw.dao.BatchUpdateManager;
import uw.dao.TransactionException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BatchupdateManager实现类.
 *
 * @author axeon
 */
public class BatchUpdateManagerImpl implements BatchUpdateManager {

    /**
     * 日志.
     */
    private static final Logger logger = LoggerFactory.getLogger(BatchUpdateManagerImpl.class);

    /**
     * pstmt集合.key:sql,value:pstmt
     */
    private LinkedHashMap<String, PreparedStatementWrapper> pstmtMap = null;


    /**
     * 是否批量模式.
     */
    private boolean isBatch = false;

    /**
     * 批量更新大小.
     */
    private int batchSize = 100;

    /**
     * 批量更新的sizeMap.key:sql
     */
    private LinkedHashMap<String, Integer> sizeMap = null;

    /**
     * 批量更新的resultMap.key:sql
     */
    private LinkedHashMap<String, List<Integer>> resultMap = null;


    /**
     * dao实例。
     */
    private final DaoFactoryImpl dao;

    /**
     * 默认构造器,只能在本包内调用.
     */
    protected BatchUpdateManagerImpl(DaoFactoryImpl dao) {
        this.dao = dao;
    }

    /**
     * 获取pstmt.
     *
     * @param conn Connection对象
     * @param sql  SQL语句
     * @return PreparedStatement对象
     * @throws SQLException SQL异常
     */
    public PreparedStatement prepareStatement(String connName, int connId, Connection conn, String sql) throws SQLException, TransactionException {
        // pstmt对象
        PreparedStatement pstmt = null;
        if (!this.isBatch) { // 非batchUpdate
            pstmt = conn.prepareStatement(sql);
        } else { // 进入update状态
            int bsize = 0; // 该pstmt已经addBatch的次数
            PreparedStatementWrapper wrapper = pstmtMap.get(sql);
            if (wrapper != null) {
                pstmt = wrapper.pstmt;
                // 判断连接是否有效，是否打开事务处理
                if (pstmt.getConnection().isClosed() || pstmt.getConnection().getAutoCommit()) {
                    throw new TransactionException("TransactionException in BatchUpdateManagerImpl.java:prepareStatement is closed or auto commit!");
                }
                int currentBatchSize = sizeMap.get(sql);
                //检查是否需要提交批量。
                if (currentBatchSize >= batchSize) {
                    checkToBatchUpdate(connName, conn.hashCode(), sql, pstmt);
                    bsize = 1;
                } else {
                    bsize = currentBatchSize + 1;
                }
            } else {
                pstmt = conn.prepareStatement(sql);
                pstmtMap.put(sql, new PreparedStatementWrapper(connName, connId, pstmt));
                // 初始化返回值
                resultMap.put(sql, new ArrayList<>());
                bsize = 1;
            }
            // 把当前bsize更新
            sizeMap.put(sql, bsize);
        }
        // 返回该pstmt
        return pstmt;
    }

    /**
     * 当调用次方法的时候，自动设置开始transaction.
     *
     * @throws TransactionException 事务异常
     */
    public void startBatchUpdate() {
        this.isBatch = true;
        // 初始化map
        pstmtMap = new LinkedHashMap<>();
        sizeMap = new LinkedHashMap<>();
        resultMap = new LinkedHashMap<>();
    }

    /**
     * 获取批量更新的数量.
     *
     * @return boolean
     */
    @Override
    public int getBatchSize() {
        return this.batchSize;
    }

    /**
     * 设置批量更新的数量.
     *
     * @param batchSize 数量
     */
    @Override
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * 获取是否在批量模式下.
     *
     * @return boolean
     * @throws TransactionException 事务异常
     */
    public boolean getBatchStatus() {
        return this.isBatch;
    }

    /**
     * 检查pstmt需要更新.
     *
     * @throws Exception 异常
     */
    private void checkToBatchUpdate(String connName, int connId, String sql, PreparedStatement pstmt) throws TransactionException {
        // 开始做批量更新
        int[] effectedNums = executeBatch(connName, connId, sql, pstmt);
        List<Integer> list = resultMap.get(sql);
        for (int effectedNum : effectedNums) {
            list.add(effectedNum);
        }
    }

    /**
     * 执行batch操作。
     *
     * @param connName
     * @param connId
     * @param sql
     * @param pstmt
     * @return
     * @throws TransactionException
     */
    private int[] executeBatch(String connName, int connId, String sql, PreparedStatement pstmt) throws TransactionException {
        long start = SystemClock.now();
        int[] effects;
        String exception = "";
        try {
            effects = pstmt.executeBatch();
        } catch (Throwable e) {
            exception = e.toString();
            throw new TransactionException(exception + connName + "@" + connId + ": " + sql + "#" + batchSize, e);
        }
        dao.addSqlExecuteStats(connName, connId, sql, null, batchSize, 0, 0, SystemClock.now() - start, exception);
        return effects;
    }

    /**
     * 完成需要清空map，并关闭全部pstmt.
     *
     * @return 结果List
     * @throws TransactionException 事务异常
     */
    @Override
    public Map<String, List<Integer>> submit() throws TransactionException {
        StringBuilder errorMsg = new StringBuilder();
        LinkedHashMap<String, List<Integer>> returnMap = null;
        for (Map.Entry<String, PreparedStatementWrapper> kv : pstmtMap.entrySet()) {
            try {
                // 先执行未完成执行的batchUpdate
                PreparedStatementWrapper wrapper = kv.getValue();

                int[] effectedNums = executeBatch(wrapper.connName, wrapper.connId, kv.getKey(), wrapper.pstmt);
                // 加入结果map
                List<Integer> list = resultMap.get(kv.getKey());
                for (int effectNum : effectedNums) {
                    list.add(effectNum);
                }
            } catch (Exception e) {
                errorMsg.append("\n\t\t").append(e.getMessage());
                logger.error(e.getMessage(), e);
            } finally {
                try {
                    // 最后关闭该pstmt
                    kv.getValue().pstmt.close();
                } catch (Exception e) {
                    errorMsg.append("\n\t\t").append(e.getMessage());
                    logger.error(e.getMessage(), e);
                }
            }
        }
        if (errorMsg.length() > 0) {
            throw new TransactionException("TransactionException in BatchUpdateManagerImpl.java: submit()!" + errorMsg.toString());
        }
        returnMap = resultMap;
        this.isBatch = false;
        pstmtMap = null;
        sizeMap = null;
        resultMap = null;
        batchSize = 50;
        return returnMap;
    }

    /**
     * 获取Batch的sql列表.
     *
     * @return sql列表
     */
    @Override
    public List<String> getBatchList() {
        ArrayList<String> sqlList = new ArrayList<String>(sizeMap.size());
        // 获取相关sql和pstmt
        sqlList.addAll(sizeMap.keySet());
        return sqlList;
    }

    /**
     * PreparedStatementWrapper。
     * 用来保存conn信息，输出日志。
     */
    private static class PreparedStatementWrapper {
        String connName;

        int connId;

        PreparedStatement pstmt;

        public PreparedStatementWrapper(String connName, int connId, PreparedStatement pstmt) {
            this.connName = connName;
            this.connId = connId;
            this.pstmt = pstmt;
        }
    }
}
