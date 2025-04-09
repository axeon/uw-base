package uw.dao.impl;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.util.SystemClock;
import uw.dao.*;
import uw.dao.conf.DaoConfigManager;
import uw.dao.connectionpool.ConnectionManager;
import uw.dao.dialect.Dialect;
import uw.dao.util.DaoReflectUtils;
import uw.dao.util.EntityMetaUtils;
import uw.dao.util.SQLUtils;
import uw.dao.vo.FieldMetaInfo;
import uw.dao.vo.TableMetaInfo;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.*;

/**
 * 实体类命令实现.
 *
 * @author axeon
 */
public class EntityCommandImpl {

    /**
     * 日志.
     */
    private static final Logger logger = LoggerFactory.getLogger(EntityCommandImpl.class);


    /**
     * 保存一个实体.
     *
     * @param dao       DAOFactoryImpl对象
     * @param connName  连接名字
     * @param entity    实体类
     * @param tableName 表名
     * @param <T>       实体类类型
     * @return 实体类
     * @throws TransactionException 事务异常
     */
    @SuppressWarnings("resource")
    static <T extends DataEntity> T save(DaoFactoryImpl dao, String connName, T entity, String tableName) throws TransactionException {
        long start = SystemClock.now();
        long connTime = 0, dbTime = 0;
        int connId = 0;
        String exception = null;
        TableMetaInfo emi = EntityMetaUtils.loadEntityMetaInfo(entity.getClass());
        if (emi == null) {
            throw new TransactionException("TableMetaInfo[" + entity.getClass() + "] not found! ");
        }
        if (tableName == null || tableName.equals("")) {
            tableName = emi.getTableName();
        }

        if (connName == null || connName.equals("")) {
            connName = DaoConfigManager.getRouteMapping(tableName, "write");
        }
        StringBuilder sb = new StringBuilder();
        // 写入所有的列
        ArrayList<String> cols = new ArrayList<>(emi.getColumnMap().keySet());
        //参数列表。
        Object[] paramList = new Object[cols.size()];
        if (cols.size() > 0) {
            sb.append("insert into ").append(tableName).append(" (");
            for (String col : cols) {
                sb.append(col).append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append(") values (");
            sb.append("?,".repeat(cols.size()));
            sb.deleteCharAt(sb.length() - 1);
            sb.append(")");
        }
        Connection con = null;
        PreparedStatement pstmt = null;
        int effect = 0;
        try {
            con = dao.getTransactionController().getConnection(connName);
            connId = con.hashCode();
            pstmt = dao.getBatchUpdateController().prepareStatement(connName, connId, con, sb.toString());
            int seq = 0;
            for (String col : cols) {
                FieldMetaInfo fmi = emi.getFieldMetaInfo(col);
                if (fmi == null) {
                    throw new TransactionException("FieldMetaInfo[" + col + "@" + entity.getClass() + "] not found! ");
                }
                paramList[seq] = DaoReflectUtils.DAOLiteSaveReflect(pstmt, entity, fmi, seq + 1);
                seq++;
            }
            connTime = SystemClock.now() - start;
            long dbStart = SystemClock.now();
            if (dao.getBatchUpdateController().getBatchStatus()) {
                pstmt.addBatch();
            } else {
                effect = pstmt.executeUpdate();
            }
            dbTime = SystemClock.now() - dbStart;
        } catch (Exception e) {
            exception = e.toString();
            throw new TransactionException(exception + connName + "@" + connId + ": " + sb.toString() + "#" + Arrays.toString(paramList), e);
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
                dao.addSqlExecuteStats(connName, connId, sb.toString(), paramList, effect, connTime, dbTime, allTime, exception);
            }
        }
        return entity;
    }

    /**
     * 批量保存实体.
     *
     * @param dao        DAOFactoryImpl对象
     * @param connName   连接名字
     * @param entityList 实体集合
     * @param tableName  表名
     * @param <T>        实体类类型
     * @return 实体类
     * @throws TransactionException 事务异常
     */
    @SuppressWarnings("resource")
    static <T extends DataEntity> List<T> save(DaoFactoryImpl dao, String connName, List<T> entityList, String tableName) throws TransactionException {
        long start = SystemClock.now();
        long connTime = 0, dbTime = 0;
        int connId = 0;
        String exception = null;
        if (entityList == null || entityList.size() == 0) {
            return entityList;
        }
        Class<? extends DataEntity> tClass = entityList.get(0).getClass();
        TableMetaInfo emi = EntityMetaUtils.loadEntityMetaInfo(tClass);
        if (emi == null) {
            throw new TransactionException("TableMetaInfo[" + tClass + "] not found! ");
        }
        if (tableName == null || tableName.equals("")) {
            tableName = emi.getTableName();
        }

        if (connName == null || connName.equals("")) {
            connName = DaoConfigManager.getRouteMapping(tableName, "write");
        }
        StringBuilder sb = new StringBuilder();
        // 写入所有的列
        ArrayList<String> cols = new ArrayList<>(emi.getColumnMap().keySet());
        //参数列表。 输出日志和统计使用
        Object[] paramList = new Object[cols.size() * entityList.size()];
        if (cols.size() > 0) {
            sb.append("insert into ").append(tableName).append(" (");
            for (String col : cols) {
                sb.append(col).append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append(") values ");
            sb.deleteCharAt(sb.length() - 1);
            for (T entity : entityList) {
                sb.append("(");
                sb.append("?,".repeat(cols.size()));
                sb.deleteCharAt(sb.length() - 1);
                sb.append("),");
            }
            sb.deleteCharAt(sb.length() - 1);
        }
        Connection con = null;
        PreparedStatement pstmt = null;
        int effect = 0;
        try {
            con = dao.getTransactionController().getConnection(connName);
            connId = con.hashCode();
            pstmt = dao.getBatchUpdateController().prepareStatement(connName, connId, con, sb.toString());
            int seq = 0;
            for (T entity : entityList) {
                for (String col : cols) {
                    FieldMetaInfo fmi = emi.getFieldMetaInfo(col);
                    if (fmi == null) {
                        throw new TransactionException("FieldMetaInfo[" + col + "@" + tClass + "] not found! ");
                    }
                    paramList[seq] = DaoReflectUtils.DAOLiteSaveReflect(pstmt, entity, fmi, seq + 1);
                    seq++;
                }
            }
            connTime = SystemClock.now() - start;
            long dbStart = SystemClock.now();
            if (dao.getBatchUpdateController().getBatchStatus()) {
                pstmt.addBatch();
            } else {
                effect = pstmt.executeUpdate();
            }
            dbTime = SystemClock.now() - dbStart;
        } catch (Exception e) {
            exception = e.toString();
            throw new TransactionException(exception + connName + "@" + connId + ": " + sb.toString() + "#" + Arrays.toString(paramList), e);
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
                dao.addSqlExecuteStats(connName, connId, sb.toString(), paramList, effect, connTime, dbTime, allTime, exception);
            }
        }
        return entityList;
    }

    /**
     * 加载一个实体.
     *
     * @param dao       DAOFactoryImpl对象
     * @param connName  连接名
     * @param cls       要映射的对象类型
     * @param tableName 表名
     * @param id        主键
     * @param <T>       要映射的对象类型
     * @return 实体类
     * @throws TransactionException 事务异常
     */
    static <T> T load(DaoFactoryImpl dao, String connName, Class<T> cls, String tableName, Serializable id) throws TransactionException {
        long start = SystemClock.now();
        long connTime = 0, dbTime = 0;
        int connId = 0, rowNum = 0;
        String exception = null;
        TableMetaInfo emi = EntityMetaUtils.loadEntityMetaInfo(cls);
        if (emi == null) {
            throw new TransactionException("TableMetaInfo[" + cls + "] not found! ");
        }
        if (tableName == null || tableName.equals("")) {
            tableName = emi.getTableName();
        }

        if (connName == null || connName.equals("")) {
            connName = DaoConfigManager.getRouteMapping(tableName, "write");
        }
        StringBuilder sb = new StringBuilder();
        List<FieldMetaInfo> pks = emi.getPkList();
        sb.append("select * from ").append(tableName).append(" where ");
        if (pks.size() > 0) {
            FieldMetaInfo fmi = pks.get(0);
            sb.append(fmi.getColumnName()).append("=? ");
        }

        T entity = null;

        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = dao.getTransactionController().getConnection(connName);
            connId = con.hashCode();
            pstmt = con.prepareStatement(sb.toString());
            DaoReflectUtils.CommandUpdateReflect(pstmt, 1, id);
            connTime = SystemClock.now() - start;
            long dbStart = SystemClock.now();
            ResultSet rs = pstmt.executeQuery();
            dbTime = SystemClock.now() - dbStart;

            // 获得字段列表
            ResultSetMetaData rsm = rs.getMetaData();
            int colsCount = rsm.getColumnCount();
            String[] cols = new String[colsCount];
            for (int k = 0; k < colsCount; k++) {
                cols[k] = rsm.getColumnLabel(k + 1).toLowerCase();
            }

            if (rs.next()) {
                rowNum = 1;
                entity = cls.getDeclaredConstructor().newInstance();
                for (String col : cols) {
                    FieldMetaInfo fmi = emi.getFieldMetaInfo(col);
                    if (fmi != null) {
                        DaoReflectUtils.DAOLiteLoadReflect(rs, entity, fmi);
                    }
                }
            }
            rs.close();
        } catch (Exception e) {
            exception = e.toString();
            throw new TransactionException(exception + connName + "@" + connId + ": " + sb.toString() + "#" + id.toString(), e);
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
            dao.addSqlExecuteStats(connName, connId, sb.toString(), new Object[]{id}, rowNum, connTime, dbTime, allTime, exception);
        }
        return entity;
    }

    /**
     * 加载一个实体.
     *
     * @param dao       DAOFactoryImpl对象
     * @param connName  连接名
     * @param cls       要映射的对象类型
     * @param selectSql 查询的SQL语句
     * @param paramList 参数的Object数组
     * @param <T>       要映射的对象类型
     * @return 实体类
     * @throws TransactionException 事务异常
     */
    static <T> T listSingle(DaoFactoryImpl dao, String connName, Class<T> cls, String selectSql, Object[] paramList) throws TransactionException {
        long start = SystemClock.now();
        long connTime = 0, dbTime = 0;
        int connId = 0, rowNum = 0;
        String exception = null;
        if (connName == null) {
            connName = SQLUtils.getConnNameFromSQL(selectSql);
        }
        if (paramList == null) {
            paramList = new Object[0];
        }

        TableMetaInfo emi = EntityMetaUtils.loadEntityMetaInfo(cls);
        if (emi == null) {
            throw new TransactionException("TableMetaInfo[" + cls.getName() + "] not found! ");
        }
        T entity = null;
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = dao.getTransactionController().getConnection(connName);
            connId = con.hashCode();
            pstmt = con.prepareStatement(selectSql);
            int seq = 0;
            if (paramList != null && paramList.length > 0) {
                for (seq = 0; seq < paramList.length; seq++) {
                    DaoReflectUtils.CommandUpdateReflect(pstmt, seq + 1, paramList[seq]);
                }
            }
            connTime = SystemClock.now() - start;
            long dbStart = SystemClock.now();
            ResultSet rs = pstmt.executeQuery();
            dbTime = SystemClock.now() - dbStart;

            // 获得字段列表
            ResultSetMetaData rsm = rs.getMetaData();
            int colsCount = rsm.getColumnCount();
            String[] cols = new String[colsCount];
            for (int k = 0; k < colsCount; k++) {
                cols[k] = rsm.getColumnLabel(k + 1).toLowerCase();
            }

            if (rs.next()) {
                rowNum = 1;
                entity = cls.getDeclaredConstructor().newInstance();
                for (String col : cols) {
                    FieldMetaInfo fmi = emi.getFieldMetaInfo(col);
                    if (fmi != null) {
                        DaoReflectUtils.DAOLiteLoadReflect(rs, entity, fmi);
                    }
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
            dao.addSqlExecuteStats(connName, connId, selectSql, paramList, rowNum, connTime, dbTime, allTime, exception);
        }
        return entity;
    }

    /**
     * 保存一个实体.
     *
     * @param dao       DAOFactoryImpl对象
     * @param connName  连接名
     * @param entity    实体类
     * @param tableName 表名
     * @return 实体类
     * @throws TransactionException 事务异常
     */
    static int update(DaoFactoryImpl dao, String connName, DataEntity entity, String tableName) throws TransactionException {
        // 有时候从数据库中load数据，并无实质更新，此时直接返回-1.
        if (entity.GET_UPDATED_COLUMN() == null) {
            return 0;
        }
        long start = SystemClock.now();
        long connTime = 0, dbTime = 0;
        int connId = 0;
        String exception = null;
        TableMetaInfo emi = EntityMetaUtils.loadEntityMetaInfo(entity.getClass());
        if (emi == null) {
            throw new TransactionException("TableMetaInfo[" + entity.getClass() + "] not found! ");
        }
        if (tableName == null || tableName.isEmpty()) {
            tableName = emi.getTableName();
        }

        if (connName == null || connName.isEmpty()) {
            connName = DaoConfigManager.getRouteMapping(tableName, "write");
        }
        StringBuilder sb = new StringBuilder();
        ArrayList<String> cols = new ArrayList<String>(entity.GET_UPDATED_COLUMN());
        List<FieldMetaInfo> pks = emi.getPkList();
        //参数列表。
        Object[] paramList = new Object[cols.size() + pks.size()];
        sb.append("update ").append(tableName).append(" set ");
        for (String col : cols) {
            sb.append(col).append("=?,");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(" where ");
        for (int i = 0; i < pks.size(); i++) {
            FieldMetaInfo fmi = pks.get(i);
            if (i > 0) {
                sb.append("and ");
            }
            sb.append(fmi.getColumnName()).append("=? ");
        }

        Connection con = null;
        PreparedStatement pstmt = null;
        int effect = 0;
        try {
            con = dao.getTransactionController().getConnection(connName);
            connId = con.hashCode();
            pstmt = dao.getBatchUpdateController().prepareStatement(connName, connId, con, sb.toString());
            int seq = 0;
            for (String col : cols) {
                FieldMetaInfo fmi = emi.getFieldMetaInfo(col);
                if (fmi == null) {
                    throw new TransactionException("FieldMetaInfo[" + col + "@" + entity.getClass() + "] not found! ");
                }
                paramList[seq] = DaoReflectUtils.DAOLiteSaveReflect(pstmt, entity, fmi, seq + 1);
                seq++;
            }
            // 开始where主键。
            for (FieldMetaInfo fmi : pks) {
                paramList[seq] = DaoReflectUtils.DAOLiteSaveReflect(pstmt, entity, fmi, seq + 1);
                seq++;
            }
            connTime = SystemClock.now() - start;
            long dbStart = SystemClock.now();
            if (dao.getBatchUpdateController().getBatchStatus()) {
                pstmt.addBatch();
            } else {
                effect = pstmt.executeUpdate();
            }
            dbTime = SystemClock.now() - dbStart;
        } catch (Exception e) {
            exception = e.toString();
            throw new TransactionException(exception + connName + "@" + connId + ": " + sb.toString() + "#" + Arrays.toString(paramList), e);
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
                dao.addSqlExecuteStats(connName, connId, sb.toString(), paramList, effect, connTime, dbTime, allTime, exception);
            }
        }
        return effect;
    }

    /**
     * 删除一个实体.
     *
     * @param dao       DAOFactoryImpl对象
     * @param connName  连接名
     * @param entity    实体类
     * @param tableName 表名
     * @return int
     * @throws TransactionException 事务异常
     */
    static int delete(DaoFactoryImpl dao, String connName, DataEntity entity, String tableName) throws TransactionException {
        long start = SystemClock.now();
        long connTime = 0, dbTime = 0;
        int connId = 0;
        String exception = null;
        TableMetaInfo emi = EntityMetaUtils.loadEntityMetaInfo(entity.getClass());
        if (emi == null) {
            throw new TransactionException("TableMetaInfo[" + entity.getClass() + "] not found! ");
        }
        if (tableName == null || tableName.equals("")) {
            tableName = emi.getTableName();
        }

        if (connName == null || connName.equals("")) {
            connName = DaoConfigManager.getRouteMapping(tableName, "write");
        }

        StringBuilder sb = new StringBuilder();
        List<FieldMetaInfo> pks = emi.getPkList();
        //参数列表。
        Object[] paramList = new Object[pks.size()];
        sb.append("delete from ").append(tableName);
        sb.append(" where ");
        for (int i = 0; i < pks.size(); i++) {
            FieldMetaInfo fmi = pks.get(i);
            if (i > 0) {
                sb.append("and ");
            }
            sb.append(fmi.getColumnName()).append("=? ");
        }

        Connection con = null;
        PreparedStatement pstmt = null;
        int effect = 0;
        try {
            con = dao.getTransactionController().getConnection(connName);
            connId = con.hashCode();
            pstmt = dao.getBatchUpdateController().prepareStatement(connName, connId, con, sb.toString());
            int seq = 0;
            // 开始where主键。
            for (FieldMetaInfo fmi : pks) {
                paramList[seq] = DaoReflectUtils.DAOLiteSaveReflect(pstmt, entity, fmi, seq + 1);
                seq++;
            }
            connTime = SystemClock.now() - start;
            long dbStart = SystemClock.now();
            if (dao.getBatchUpdateController().getBatchStatus()) {
                pstmt.addBatch();
            } else {
                effect = pstmt.executeUpdate();
            }
            dbTime = SystemClock.now() - dbStart;
        } catch (Exception e) {
            exception = e.toString();
            throw new TransactionException(exception + connName + "@" + connId + ": " + sb.toString() + "#" + Arrays.toString(paramList), e);
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
                dao.addSqlExecuteStats(connName, connId, sb.toString(), paramList, effect, connTime, dbTime, allTime, exception);
            }
        }
        return effect;
    }

    /**
     * 获得列表.
     *
     * @param dao        DAOFactoryImpl对象
     * @param connName   连接名
     * @param cls        要映射的对象类型
     * @param selectSql  查询SQL语句
     * @param paramList  参数的Object数组
     * @param startIndex 开始位置
     * @param resultNum  结果集大小
     * @param autoCount  是否统计全部数据（用于分页算法），默认为false。
     * @param <T>        要映射的对象类型
     * @return 列表
     * @throws TransactionException 事务异常
     */
    static <T> DataList<T> list(DaoFactoryImpl dao, String connName, Class<T> cls, String selectSql, Object[] paramList, int startIndex, int resultNum, boolean autoCount) throws TransactionException {
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

        TableMetaInfo emi = EntityMetaUtils.loadEntityMetaInfo(cls);
        if (emi == null) {
            throw new TransactionException("TableMetaInfo[" + cls.getName() + "] not found! ");
        }

        int allSize = 0;

        //自动count
        if (autoCount) {
            String countSql = "select count(1) from (" + selectSql + ") must_alias";
            allSize = SQLCommandImpl.selectForSingleValue(dao, connName, Integer.class, countSql, paramList);
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

        ArrayList<T> list = new ArrayList<T>();
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

            // 获得字段列表
            ResultSetMetaData rsm = rs.getMetaData();
            int colsCount = rsm.getColumnCount();
            String[] cols = new String[colsCount];
            for (int k = 0; k < colsCount; k++) {
                cols[k] = rsm.getColumnLabel(k + 1).toLowerCase();
            }

            while (rs.next()) {
                T entity = cls.getDeclaredConstructor().newInstance();
                for (String col : cols) {
                    FieldMetaInfo fmi = emi.getFieldMetaInfo(col);
                    if (fmi != null) {
                        DaoReflectUtils.DAOLiteLoadReflect(rs, entity, fmi);
                    }
                }
                list.add(entity);
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
        return new DataList<T>(list, startIndex, resultNum, allSize);
    }

    /**
     * 获取表名.
     *
     * @param cls 类型
     * @return 表名
     */
    static String getTableName(Class<?> cls) {
        TableMetaInfo emi = EntityMetaUtils.loadEntityMetaInfo(cls);
        if (emi != null) {
            return emi.getTableName();
        } else {
            return null;
        }
    }
}
