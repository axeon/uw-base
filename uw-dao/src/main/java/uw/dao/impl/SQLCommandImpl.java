package uw.dao.impl;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.util.JsonUtils;
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
import java.util.ArrayList;
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
        long startMillis = SystemClock.now();
        long connMillis = 0, dbMillis = 0, allMillis = 0;
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
            for (int i = 0; i < paramList.length; i++) {
                DaoReflectUtils.CommandUpdateReflect(pstmt, i + 1, paramList[i]);
            }
            long dbStartMillis = SystemClock.now();
            connMillis = dbStartMillis - startMillis;
            ResultSet rs = pstmt.executeQuery();
            dbMillis = SystemClock.now() - dbStartMillis;
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
            throw new TransactionException(exception + connName + "@" + connId + ": " + selectSql + "#" + JsonUtils.toString(paramList), e);
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
            allMillis = SystemClock.now() - startMillis;
            dao.addSqlExecuteStats(connName, connId, selectSql, paramList, value == null ? 0 : 1, connMillis, dbMillis, allMillis, exception);
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
        long startMillis = SystemClock.now();
        long connMillis = 0, dbMillis = 0, allMillis = 0;
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
        ArrayList<Object> list = new ArrayList<Object>();
        try {
            con = dao.getTransactionController().getConnection(connName);
            connId = con.hashCode();
            pstmt = con.prepareStatement(selectSql);
            for (int i = 0; i < paramList.length; i++) {
                DaoReflectUtils.CommandUpdateReflect(pstmt, i + 1, paramList[i]);
            }
            long dbStartMillis = SystemClock.now();
            connMillis = dbStartMillis - startMillis;
            ResultSet rs = pstmt.executeQuery();
            dbMillis = SystemClock.now() - dbStartMillis;

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
            throw new TransactionException(exception + connName + "@" + connId + ": " + selectSql + "#" + JsonUtils.toString(paramList), e);
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
            allMillis = SystemClock.now() - startMillis;
            dao.addSqlExecuteStats(connName, connId, selectSql, paramList, list.size(), connMillis, dbMillis, allMillis, exception);
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
        long startMillis = SystemClock.now();
        long connMillis = 0, dbMillis = 0, allMillis = 0;
        int connId = 0, dsSize = 0;
        String exception = null;
        if (connName == null) {
            connName = SQLUtils.getConnNameFromSQL(selectSql);
        }
        if (paramList == null) {
            paramList = new Object[0];
        }

        int allSize = 0;
        // 分页sql
        String pagedSelectSql = selectSql;
        // 分页参数
        Object[] pagedParamList = paramList;
        //原始参数长度
        int paramListSize = paramList.length;
        //判断是否需要分页
        boolean needPagination = resultNum > 0 && startIndex >= 0;
        if (needPagination) {
            Dialect dialect = ConnectionManager.getDialect(connName);
            Object[] po = dialect.getPagedSQL(selectSql, startIndex, resultNum);
            pagedSelectSql = po[0].toString();
            pagedParamList = ArrayUtils.addAll(paramList, po[1], po[2]);
        }

        DataSet ds = DataSet.EMPTY;
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = dao.getTransactionController().getConnection(connName);
            connId = con.hashCode();
            pstmt = con.prepareStatement(pagedSelectSql);
            int seq;
            for (seq = 0; seq < paramListSize; seq++) {
                DaoReflectUtils.CommandUpdateReflect(pstmt, seq + 1, pagedParamList[seq]);
            }
            if (needPagination) {
                pstmt.setInt(seq + 1, (Integer) pagedParamList[seq]);
                pstmt.setInt(seq + 2, (Integer) pagedParamList[seq + 1]);
            }
            long dbStartMillis = SystemClock.now();
            connMillis = dbStartMillis - startMillis;
            ResultSet rs = pstmt.executeQuery();
            dbMillis = SystemClock.now() - dbStartMillis;
            ds = new DataSet(rs, startIndex, resultNum, allSize);
            rs.close();
            dsSize = ds.size();
        } catch (Exception e) {
            exception = e.toString();
            throw new TransactionException(exception + connName + "@" + connId + ": " + pagedSelectSql + "#" + JsonUtils.toString(pagedParamList), e);
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
            allMillis = SystemClock.now() - startMillis;
            dao.addSqlExecuteStats(connName, connId, pagedSelectSql, pagedParamList, dsSize, connMillis, dbMillis, allMillis, exception);
        }
        //自动Count场景下，如果数据长度小于结果集长度，直接使用数据长度作为allSize，否则去数据库count
        if (autoCount) {
            if (startIndex == 0 && ds.size() < resultNum) {
                allSize = ds.size();
            } else {
                String countSql = "select count(1) from (" + selectSql + ") must_alias";
                allSize = SQLCommandImpl.selectForSingleValue(dao, connName, Integer.class, countSql, paramList);
            }
            ds.calcPages(allSize);
        }
        return ds;
    }

    /**
     * 执行任意sql.
     *
     * @param dao       DAOFactoryImpl对象
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param exeSql    执行的SQL
     * @param paramList 执行SQL的绑定参数
     * @return int
     * @throws TransactionException 事务异常
     */
    public static int executeSQL(DaoFactoryImpl dao, String connName, String exeSql, Object[] paramList) throws TransactionException {
        long startMillis = SystemClock.now();
        long connMillis = 0, dbMillis = 0, allMillis = 0;
        int connId = 0;
        String exception = null;

        if (connName == null) {
            connName = SQLUtils.getConnNameFromSQL(exeSql);
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
            pstmt = dao.getBatchUpdateController().prepareStatement(connName, connId, con, exeSql);
            if (paramList.length > 0) {
                for (int i = 0; i < paramList.length; i++) {
                    DaoReflectUtils.CommandUpdateReflect(pstmt, i + 1, paramList[i]);
                }
            }
            long dbStartMillis = SystemClock.now();
            connMillis = dbStartMillis - startMillis;
            if (dao.getBatchUpdateController().getBatchStatus()) {
                pstmt.addBatch();
            } else {
                effectedNum = pstmt.executeUpdate();
            }
            dbMillis = SystemClock.now() - dbStartMillis;
        } catch (Exception e) {
            exception = e.toString();
            throw new TransactionException(exception + connName + "@" + connId + ": " + exeSql + "#" + JsonUtils.toString(paramList), e);
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
            allMillis = SystemClock.now() - startMillis;
            if (!dao.getBatchUpdateController().getBatchStatus()) {
                dao.addSqlExecuteStats(connName, connId, exeSql, paramList, effectedNum, connMillis, dbMillis, allMillis, exception);
            }
        }
        return effectedNum;
    }

}
