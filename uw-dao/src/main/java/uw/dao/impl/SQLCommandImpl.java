package uw.dao.impl;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.util.SystemClock;
import uw.dao.DataSet;
import uw.dao.TransactionException;
import uw.dao.connectionpool.ConnectionManager;
import uw.dao.dialect.Dialect;
import uw.dao.util.DaoReflectUtils;
import uw.dao.util.SQLUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * 为了更为高效的执行数据库命令，是该类产生的根本原因。 具体使用请自行参照源代码.
 */
public class SQLCommandImpl {

    /**
     * 日志.
     */
    private static final Logger logger = LoggerFactory.getLogger(SQLCommandImpl.class);

    /**
     * 获取单个数值.
     *
     * @param dao       DAOFactoryImpl对象
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param selectSql 查询的SQL
     * @param cls       要映射的对象类型
     * @param paramList 查询SQL的绑定参数
     * @param <T>       要映射的对象类型
     * @return 单个数值
     * @throws TransactionException 事务异常
     */
    @SuppressWarnings("unchecked")
    public static <T> T selectForSingleValue(DaoFactoryImpl dao, String connName, Class<T> cls, String selectSql, Object[] paramList) throws TransactionException {
        long start = SystemClock.now();
        long connTime = 0, dbTime = 0;
        int connId = 0;
        String exception = null;
        if (connName == null) {
            connName = SQLUtils.getConnNameFromSQL(selectSql);
        }
        if (paramList == null) {
            paramList = new Object[0];
        }

        Connection con = null;
        PreparedStatement pstmt = null;
        Object value = null;
        try {
            con = dao.getTransactionController().getConnection(connName);
            connId = con.hashCode();
            pstmt = con.prepareStatement(selectSql);
            if (paramList != null && paramList.length > 0) {
                for (int i = 0; i < paramList.length; i++) {
                    DaoReflectUtils.CommandUpdateReflect(pstmt, i + 1, paramList[i]);
                }
            }
            connTime = SystemClock.now() - start;
            long dbStart = SystemClock.now();
            ResultSet rs = pstmt.executeQuery();
            dbTime = SystemClock.now() - dbStart;
            if (rs.next()) {
                if (cls == int.class || cls == Integer.class) {
                    value = rs.getInt(1);
                } else if (cls == String.class) {
                    value = rs.getString(1);
                } else if (cls == long.class || cls == Long.class) {
                    value = rs.getLong(1);
                } else if (cls == Date.class) {
                    value = rs.getTimestamp(1);
                } else if (cls == double.class || cls == Double.class) {
                    value = rs.getDouble(1);
                } else if (cls == float.class || cls == Float.class) {
                    value = rs.getFloat(1);
                } else if (cls == short.class || cls == Short.class) {
                    value = rs.getShort(1);
                } else if (cls == byte.class || cls == Byte.class) {
                    value = rs.getByte(1);
                } else if (cls == boolean.class || cls == Boolean.class) {
                    value = rs.getBoolean(1);
                } else {
                    value = rs.getObject(1);
                }
            }
            rs.close();
        } catch (Exception e) {
            exception = e.toString();
            throw new TransactionException(exception + connName + "@" + connId + ": " + selectSql + "#" + Arrays.toString(paramList), e);
        } finally {
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (dao.getTransactionController().isAutoCommit() && con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
            long allTime = SystemClock.now() - start;
            dao.addSqlExecuteStats(connName, connId, selectSql, paramList, value == null ? 0 : 1, connTime, dbTime, allTime, exception);
        }
        return (T) value;
    }

    /**
     * 获取单列数据列表.
     *
     * @param dao       DAOFactoryImpl对象
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param selectSql 查询的SQL
     * @param cls       要映射的对象类型
     * @param <T>       要映射的对象类型
     * @param paramList 查询SQL的绑定参数
     * @return 单列数据列表
     * @throws TransactionException 事务异常
     */
    @SuppressWarnings("unchecked")
    public static <T> ArrayList<T> selectForSingleList(DaoFactoryImpl dao, String connName, Class<T> cls, String selectSql, Object[] paramList) throws TransactionException {
        long start = SystemClock.now();
        long connTime = 0, dbTime = 0;
        int connId = 0;
        String exception = null;
        if (connName == null) {
            connName = SQLUtils.getConnNameFromSQL(selectSql);
        }
        if (paramList == null) {
            paramList = new Object[0];
        }

        Connection con = null;
        PreparedStatement pstmt = null;
        ArrayList<Object> list = new ArrayList<Object>(128);
        try {
            con = dao.getTransactionController().getConnection(connName);
            connId = con.hashCode();
            pstmt = con.prepareStatement(selectSql);
            int i = 0;
            if (paramList != null && paramList.length > 0) {
                for (i = 0; i < paramList.length; i++) {
                    DaoReflectUtils.CommandUpdateReflect(pstmt, i + 1, paramList[i]);
                }
            }
            connTime = SystemClock.now() - start;
            long dbStart = SystemClock.now();
            ResultSet rs = pstmt.executeQuery();
            dbTime = SystemClock.now() - dbStart;

            if (cls == int.class || cls == Integer.class) {
                while (rs.next()) {
                    list.add(rs.getInt(1));
                }
            } else if (cls == String.class) {
                while (rs.next()) {
                    list.add(rs.getString(1));
                }
            } else if (cls == long.class || cls == Long.class) {
                while (rs.next()) {
                    list.add(rs.getLong(1));
                }
            } else if (cls == Date.class) {
                while (rs.next()) {
                    list.add(rs.getTimestamp(1));
                }
            } else if (cls == double.class || cls == Double.class) {
                while (rs.next()) {
                    list.add(rs.getDouble(1));
                }
            } else if (cls == float.class || cls == Float.class) {
                while (rs.next()) {
                    list.add(rs.getFloat(1));
                }
            } else if (cls == short.class || cls == Short.class) {
                while (rs.next()) {
                    list.add(rs.getShort(1));
                }
            } else if (cls == byte.class || cls == Byte.class) {
                while (rs.next()) {
                    list.add(rs.getByte(1));
                }
            } else if (cls == boolean.class || cls == Boolean.class) {
                while (rs.next()) {
                    list.add(rs.getBoolean(1));
                }
            } else {
                while (rs.next()) {
                    list.add(rs.getObject(1));
                }
            }
            rs.close();
        } catch (Exception e) {
            exception = e.toString();
            throw new TransactionException(exception + connName + "@" + connId + ": " + selectSql + "#" + Arrays.toString(paramList), e);
        } finally {
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (dao.getTransactionController().isAutoCommit() && con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
            long allTime = SystemClock.now() - start;
            dao.addSqlExecuteStats(connName, connId, selectSql, paramList, list.size(), connTime, dbTime, allTime, exception);
        }
        return (ArrayList<T>) list;
    }

    /**
     * 获取以DataSet为结果的数据集合.
     *
     * @param dao        DAOFactoryImpl对象
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param selectSql  查询的SQL
     * @param paramList  查询SQL的绑定参数
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @param autoCount  是否统计全部数据（用于分页算法），默认为false。
     * @return 数据集合
     * @throws TransactionException 事务异常
     */
    public static DataSet selectForDataSet(DaoFactoryImpl dao, String connName, String selectSql, Object[] paramList, int startIndex, int resultNum, boolean autoCount) throws TransactionException {
        long start = SystemClock.now();
        long connTime = 0, dbTime = 0;
        int connId = 0, dsSize = 0;
        String exception = null;
        if (connName == null) {
            connName = SQLUtils.getConnNameFromSQL(selectSql);
        }
        if (paramList == null) {
            paramList = new Object[0];
        }

        int allsize = 0;

        //自动count
        if (autoCount) {
            String countSql = "select count(1) from (" + selectSql + ") must_alias";
            allsize = SQLCommandImpl.selectForSingleValue(dao, connName, Integer.class, countSql, paramList);
        }
        //原始参数长度
        int originParamSize = paramList.length;
        //判断是否需要分页
        boolean needPagination = resultNum > 0 && startIndex >= 0;
        if (needPagination) {
            Dialect dialect = ConnectionManager.getDialect(connName);
            Object[] po = dialect.getPagedSQL(selectSql, startIndex, resultNum);
            selectSql = po[0].toString();
            paramList = ArrayUtils.addAll(paramList, po[1], po[2]);
        }

        DataSet ds = null;
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = dao.getTransactionController().getConnection(connName);
            connId = con.hashCode();
            pstmt = con.prepareStatement(selectSql);
            int seq = 0;
            if (paramList != null) {
                for (seq = 0; seq < originParamSize; seq++) {
                    DaoReflectUtils.CommandUpdateReflect(pstmt, seq + 1, paramList[seq]);
                }
            }
            if (needPagination) {
                pstmt.setInt(seq + 1, (Integer) paramList[seq]);
                pstmt.setInt(seq + 2, (Integer) paramList[seq + 1]);
            }
            connTime = SystemClock.now() - start;
            long dbStart = SystemClock.now();
            ResultSet rs = pstmt.executeQuery();
            dbTime = SystemClock.now() - dbStart;
            ds = new DataSet(rs, startIndex, resultNum, allsize);
            rs.close();
            dsSize = ds.size();
        } catch (Exception e) {
            exception = e.toString();
            throw new TransactionException(exception + connName + "@" + connId + ": " + selectSql + "#" + Arrays.toString(paramList), e);
        } finally {
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (dao.getTransactionController().isAutoCommit() && con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
            long allTime = SystemClock.now() - start;
            dao.addSqlExecuteStats(connName, connId, selectSql, paramList, dsSize, connTime, dbTime, allTime, exception);
        }
        return ds;
    }

    /**
     * 执行任意sql.
     *
     * @param dao        DAOFactoryImpl对象
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param executesql 执行的SQL
     * @param paramList  执行SQL的绑定参数
     * @return int
     * @throws TransactionException 事务异常
     */
    public static int executeSQL(DaoFactoryImpl dao, String connName, String executesql, Object[] paramList) throws TransactionException {
        long start = SystemClock.now();
        long connTime = 0, dbTime = 0;
        int connId = 0;
        String exception = null;

        if (connName == null) {
            connName = SQLUtils.getConnNameFromSQL(executesql);
        }
        if (paramList == null) {
            paramList = new Object[0];
        }

        Connection con = null;
        PreparedStatement pstmt = null;
        int effectedNum = 0;
        try {
            con = dao.getTransactionController().getConnection(connName);
            connId = con.hashCode();
            pstmt = dao.getBatchUpdateController().prepareStatement(connName, connId, con, executesql);
            if (paramList != null && paramList.length > 0) {
                for (int i = 0; i < paramList.length; i++) {
                    DaoReflectUtils.CommandUpdateReflect(pstmt, i + 1, paramList[i]);
                }
            }
            connTime = SystemClock.now() - start;
            long dbStart = SystemClock.now();
            if (dao.getBatchUpdateController().getBatchStatus()) {
                pstmt.addBatch();
            } else {
                effectedNum = pstmt.executeUpdate();
            }
            dbTime = SystemClock.now() - dbStart;
        } catch (Exception e) {
            exception = e.toString();
            throw new TransactionException(exception + connName + "@" + connId + ": " + executesql + "#" + Arrays.toString(paramList), e);
        } finally {
            if (!dao.getBatchUpdateController().getBatchStatus() && pstmt != null) {
                try {
                    pstmt.close();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (dao.getTransactionController().isAutoCommit() && con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
            long allTime = SystemClock.now() - start;
            if (!dao.getBatchUpdateController().getBatchStatus()) {
                dao.addSqlExecuteStats(connName, connId, executesql, paramList, effectedNum, connTime, dbTime, allTime, exception);
            }
        }
        return effectedNum;
    }

}
