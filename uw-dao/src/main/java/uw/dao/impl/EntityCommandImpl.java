package uw.dao.impl;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.util.SystemClock;
import uw.dao.DataEntity;
import uw.dao.DataList;
import uw.dao.DataUpdateInfo;
import uw.dao.TransactionException;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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
        long startMillis = SystemClock.now();
        long connMillis = 0, dbMillis = 0, allMillis = 0;
        int connId = 0;
        String exception = null;
        TableMetaInfo emi = EntityMetaUtils.loadEntityMetaInfo(entity.getClass());
        if (emi == null) {
            throw new TransactionException("TableMetaInfo[" + entity.getClass() + "] not found! ");
        }
        if (StringUtils.isBlank(tableName)) {
            tableName = emi.getTableName();
        }

        if (StringUtils.isBlank(connName)) {
            connName = DaoConfigManager.getRouteMapping(tableName, "write");
        }
        StringBuilder sb = new StringBuilder();
        // 写入所有的列
        Collection<FieldMetaInfo> fieldMetaInfos = emi.getFieldInfoMap().values();
        //参数列表。
        Object[] paramList = new Object[fieldMetaInfos.size()];
        sb.append("insert into ").append(tableName).append(" (");
        for (FieldMetaInfo fmi : fieldMetaInfos) {
            sb.append(fmi.getColumnName()).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(") values (");
        sb.append("?,".repeat(fieldMetaInfos.size()));
        sb.deleteCharAt(sb.length() - 1);
        sb.append(")");
        Connection con = null;
        PreparedStatement pstmt = null;
        int effectedNum = 0;
        try {
            con = dao.getTransactionController().getConnection(connName);
            connId = con.hashCode();
            pstmt = dao.getBatchUpdateController().prepareStatement(connName, connId, con, sb.toString());
            int seq = 0;
            for (FieldMetaInfo fmi : fieldMetaInfos) {
                paramList[seq] = DaoReflectUtils.DAOLiteSaveReflect(pstmt, entity, fmi, seq + 1);
                seq++;
            }
            long dbStartMillis = SystemClock.now();
            connMillis = dbStartMillis - startMillis;
            if (dao.getBatchUpdateController().getBatchStatus()) {
                pstmt.addBatch();
            } else {
                effectedNum = pstmt.executeUpdate();
            }
            dbMillis = SystemClock.now() - dbStartMillis;
            // 设置主键值
            emi.setLoadFlag(entity);
            // 清除更新标记
            entity.CLEAR_UPDATED_INFO();
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
            allMillis = SystemClock.now() - startMillis;
            if (!dao.getBatchUpdateController().getBatchStatus()) {
                dao.addSqlExecuteStats(connName, connId, sb.toString(), paramList, effectedNum, connMillis, dbMillis, allMillis, exception);
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
        long startMillis = SystemClock.now();
        long connMillis = 0, dbMillis = 0, allMillis = 0;
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
        if (StringUtils.isBlank(tableName)) {
            tableName = emi.getTableName();
        }
        if (StringUtils.isBlank(connName)) {
            connName = DaoConfigManager.getRouteMapping(tableName, "write");
        }
        StringBuilder sb = new StringBuilder();
        // 写入所有的列
        Collection<FieldMetaInfo> fieldMetaInfos = emi.getFieldInfoMap().values();
        //参数列表。
        Object[] paramList = new Object[fieldMetaInfos.size() * entityList.size()];
        sb.append("insert into ").append(tableName).append(" (");
        for (FieldMetaInfo fmi : fieldMetaInfos) {
            sb.append(fmi.getColumnName()).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(") values ");
        sb.deleteCharAt(sb.length() - 1);
        for (T entity : entityList) {
            sb.append("(");
            sb.append("?,".repeat(fieldMetaInfos.size()));
            sb.deleteCharAt(sb.length() - 1);
            sb.append("),");
        }
        sb.deleteCharAt(sb.length() - 1);
        Connection con = null;
        PreparedStatement pstmt = null;
        int effectedNum = 0;
        try {
            con = dao.getTransactionController().getConnection(connName);
            connId = con.hashCode();
            pstmt = dao.getBatchUpdateController().prepareStatement(connName, connId, con, sb.toString());
            int seq = 0;
            for (T entity : entityList) {
                for (FieldMetaInfo fmi : fieldMetaInfos) {
                    paramList[seq] = DaoReflectUtils.DAOLiteSaveReflect(pstmt, entity, fmi, seq + 1);
                    seq++;
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
            for (T entity : entityList) {
                // 设置主键值
                emi.setLoadFlag(entity);
                // 清除更新标记
                entity.CLEAR_UPDATED_INFO();
            }
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
            allMillis = SystemClock.now() - startMillis;
            if (!dao.getBatchUpdateController().getBatchStatus()) {
                dao.addSqlExecuteStats(connName, connId, sb.toString(), paramList, effectedNum, connMillis, dbMillis, allMillis, exception);
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
        long startMillis = SystemClock.now();
        long connMillis = 0, dbMillis = 0, allMillis = 0;
        int connId = 0, rowNum = 0;
        String exception = null;
        TableMetaInfo emi = EntityMetaUtils.loadEntityMetaInfo(cls);
        if (emi == null) {
            throw new TransactionException("TableMetaInfo[" + cls + "] not found! ");
        }
        if (StringUtils.isBlank(tableName)) {
            tableName = emi.getTableName();
        }

        if (StringUtils.isBlank(connName)) {
            connName = DaoConfigManager.getRouteMapping(tableName, "write");
        }
        StringBuilder sb = new StringBuilder();
        List<FieldMetaInfo> pks = emi.getPkList();
        sb.append("select * from ").append(tableName).append(" where ");
        if (pks.size() > 0) {
            FieldMetaInfo fmi = pks.getFirst();
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
            long dbStartMillis = SystemClock.now();
            connMillis = dbStartMillis - startMillis;
            ResultSet rs = pstmt.executeQuery();
            dbMillis = SystemClock.now() - dbStartMillis;

            // 获取字段列表
            ResultSetMetaData rsm = rs.getMetaData();
            int colsCount = rsm.getColumnCount();
            List<FieldMetaInfo> fmiList = new ArrayList<>(colsCount);
            for (int k = 0; k < colsCount; k++) {
                FieldMetaInfo fmi = emi.getInfoByColumnName(rsm.getColumnLabel(k + 1).toLowerCase());
                if (fmi != null) {
                    fmiList.add(fmi);
                }
            }

            if (rs.next()) {
                rowNum = 1;
                entity = cls.getDeclaredConstructor().newInstance();
                for (FieldMetaInfo fmi : fmiList) {
                    DaoReflectUtils.DAOLiteLoadReflect(rs, entity, fmi);
                }
                // 设置主键值
                emi.setLoadFlag(entity);
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
            allMillis = SystemClock.now() - startMillis;
            dao.addSqlExecuteStats(connName, connId, sb.toString(), new Object[]{id}, rowNum, connMillis, dbMillis, allMillis, exception);
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
        long startMillis = SystemClock.now();
        long connMillis = 0, dbMillis = 0, allMillis = 0;
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
            if (paramList.length > 0) {
                for (seq = 0; seq < paramList.length; seq++) {
                    DaoReflectUtils.CommandUpdateReflect(pstmt, seq + 1, paramList[seq]);
                }
            }
            long dbStartMillis = SystemClock.now();
            connMillis = dbStartMillis - startMillis;
            ResultSet rs = pstmt.executeQuery();
            dbMillis = SystemClock.now() - dbStartMillis;

            // 获取字段列表
            ResultSetMetaData rsm = rs.getMetaData();
            int colsCount = rsm.getColumnCount();
            List<FieldMetaInfo> fmiList = new ArrayList<>(colsCount);
            for (int k = 0; k < colsCount; k++) {
                FieldMetaInfo fmi = emi.getInfoByColumnName(rsm.getColumnLabel(k + 1).toLowerCase());
                if (fmi != null) {
                    fmiList.add(fmi);
                }
            }

            if (rs.next()) {
                rowNum = 1;
                entity = cls.getDeclaredConstructor().newInstance();
                for (FieldMetaInfo fmi : fmiList) {
                    DaoReflectUtils.DAOLiteLoadReflect(rs, entity, fmi);
                }
                // 设置主键值
                emi.setLoadFlag(entity);
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
            allMillis = SystemClock.now() - startMillis;
            dao.addSqlExecuteStats(connName, connId, selectSql, paramList, rowNum, connMillis, dbMillis, allMillis, exception);
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
        if (!DataUpdateInfo.hasUpdateInfo(entity.GET_UPDATED_INFO())) {
            return 0;
        }
        long startMillis = SystemClock.now();
        long connMillis = 0, dbMillis = 0, allMillis = 0;
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
        StringBuilder sb = new StringBuilder(256);
        DataUpdateInfo dataUpdateInfo = entity.GET_UPDATED_INFO();
        List<FieldMetaInfo> updatedFields = emi.buildFieldMetaInfoList(dataUpdateInfo.getUpdateFieldSet());
        List<FieldMetaInfo> pks = emi.getPkList();
        //参数列表。
        Object[] paramList = new Object[updatedFields.size() + pks.size()];
        sb.append("update ").append(tableName).append(" set ");
        for (FieldMetaInfo fmi : updatedFields) {
            sb.append(fmi.getColumnName()).append("=?,");
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
        int effectedNum = 0;
        try {
            con = dao.getTransactionController().getConnection(connName);
            connId = con.hashCode();
            pstmt = dao.getBatchUpdateController().prepareStatement(connName, connId, con, sb.toString());
            int seq = 0;
            for (FieldMetaInfo fmi : updatedFields) {
                paramList[seq] = DaoReflectUtils.DAOLiteSaveReflect(pstmt, entity, fmi, seq + 1);
                seq++;
            }
            // 开始where主键。
            for (FieldMetaInfo fmi : pks) {
                paramList[seq] = DaoReflectUtils.DAOLiteSaveReflect(pstmt, entity, fmi, seq + 1);
                seq++;
            }
            long dbStartMillis = SystemClock.now();
            connMillis = dbStartMillis - startMillis;
            if (dao.getBatchUpdateController().getBatchStatus()) {
                pstmt.addBatch();
            } else {
                effectedNum = pstmt.executeUpdate();
            }
            dbMillis = SystemClock.now() - dbStartMillis;
            // 设置主键值
            emi.setLoadFlag(entity);
            // 注释掉，否则历史记录无法正常记录信息。
            // entity.CLEAR_UPDATED_INFO();
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
            allMillis = SystemClock.now() - startMillis;
            if (!dao.getBatchUpdateController().getBatchStatus()) {
                dao.addSqlExecuteStats(connName, connId, sb.toString(), paramList, effectedNum, connMillis, dbMillis, allMillis, exception);
            }
        }
        return effectedNum;
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
        long startMillis = SystemClock.now();
        long connMillis = 0, dbMillis = 0, allMillis = 0;
        int connId = 0;
        String exception = null;
        TableMetaInfo emi = EntityMetaUtils.loadEntityMetaInfo(entity.getClass());
        if (emi == null) {
            throw new TransactionException("TableMetaInfo[" + entity.getClass() + "] not found! ");
        }
        if (StringUtils.isBlank(tableName)) {
            tableName = emi.getTableName();
        }

        if (StringUtils.isBlank(connName)) {
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
        int effectedNum = 0;
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
            allMillis = SystemClock.now() - startMillis;
            if (!dao.getBatchUpdateController().getBatchStatus()) {
                dao.addSqlExecuteStats(connName, connId, sb.toString(), paramList, effectedNum, connMillis, dbMillis, allMillis, exception);
            }
        }
        return effectedNum;
    }

    /**
     * 获取列表.
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

        TableMetaInfo emi = EntityMetaUtils.loadEntityMetaInfo(cls);
        if (emi == null) {
            throw new TransactionException("TableMetaInfo[" + cls.getName() + "] not found! ");
        }

        int allSize = 0;

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
            long dbStartMillis = SystemClock.now();
            connMillis = dbStartMillis - startMillis;
            ResultSet rs = pstmt.executeQuery();
            dbMillis = SystemClock.now() - dbStartMillis;

            // 获取字段列表
            ResultSetMetaData rsm = rs.getMetaData();
            int colsCount = rsm.getColumnCount();
            List<FieldMetaInfo> fmiList = new ArrayList<>(colsCount);
            for (int k = 0; k < colsCount; k++) {
                FieldMetaInfo fmi = emi.getInfoByColumnName(rsm.getColumnLabel(k + 1).toLowerCase());
                if (fmi != null) {
                    fmiList.add(fmi);
                }
            }
            while (rs.next()) {
                T entity = cls.getDeclaredConstructor().newInstance();
                for (FieldMetaInfo fmi : fmiList) {
                    DaoReflectUtils.DAOLiteLoadReflect(rs, entity, fmi);
                }
                // 设置主键值
                emi.setLoadFlag(entity);
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
            allMillis = SystemClock.now() - startMillis;
            dao.addSqlExecuteStats(connName, connId, selectSql, paramList, list.size(), connMillis, dbMillis, allMillis, exception);
        }
        //自动Count场景下，如果数据长度小于结果集长度，直接使用数据长度作为allSize，否则去数据库count
        if (autoCount) {
            if (startIndex == 0 && list.size() < resultNum) {
                allSize = list.size();
            } else {
                String countSql = "select count(1) from (" + selectSql + ") must_alias";
                allSize = SQLCommandImpl.selectForSingleValue(dao, connName, Integer.class, countSql, paramList);
            }
        }
        return new DataList<>(list, startIndex, resultNum, allSize);
    }
}
