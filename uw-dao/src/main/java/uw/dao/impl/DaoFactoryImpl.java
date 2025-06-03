package uw.dao.impl;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.dao.*;
import uw.dao.conf.DaoConfigManager;
import uw.dao.service.DaoService;
import uw.dao.util.EntityMetaUtils;
import uw.dao.util.QueryParamUtils;
import uw.dao.vo.FieldMetaInfo;
import uw.dao.vo.QueryParamResult;
import uw.dao.vo.SqlExecuteStats;
import uw.dao.vo.TableMetaInfo;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAOFactory实现类.
 *
 * @author axeon
 */
public class DaoFactoryImpl extends DaoFactory {

    private static final Logger log = LoggerFactory.getLogger(DaoFactoryImpl.class);

    /**
     * 批量更新实例.
     */
    private final BatchUpdateManagerImpl batchUpdateManager;

    /**
     * 事务处理实例.
     */
    private final TransactionManagerImpl transactionManager;

    /**
     * 统计信息.
     */
    private ArrayList<SqlExecuteStats> statsList = null;

    /**
     * 获取一个DAOFactory的实现.
     */
    public DaoFactoryImpl() {
        transactionManager = new TransactionManagerImpl();
        batchUpdateManager = new BatchUpdateManagerImpl(this);
    }

    /**
     * 添加性能统计数据.
     *
     * @param connName   连接名称
     * @param sql        sql
     * @param paramList  sql参数
     * @param rowNum     返回/影响的行数
     * @param connMillis 数据库层建立连接消耗的时间
     * @param dbMillis   数据库层操作数据库消耗的时间
     * @param allMillis  数据库层消耗的总时间
     * @param exception  异常信息
     */
    SqlExecuteStats addSqlExecuteStats(String connName, int connId, String sql, Object[] paramList, int rowNum, long connMillis, long dbMillis, long allMillis, String exception) {
        SqlExecuteStats ses = new SqlExecuteStats(connName, connId, sql, paramList, rowNum, connMillis, dbMillis, allMillis, exception);

        if (log.isDebugEnabled()) {
            log.debug(ses.genFullSqlInfo());
        }

        if (statsList != null) {
            ses.initActionDate();
            statsList.add(ses);
        }

        DaoService.logStats(ses);
        return ses;
    }

    /**
     * 开始批量更新.
     *
     * @return BatchupdateManager对象
     */
    @Override
    public BatchUpdateManager beginBatchUpdate() {
        this.batchUpdateManager.startBatchUpdate();
        return this.batchUpdateManager;
    }

    /**
     * 开始处理事务.
     *
     * @return TransactionManager对象
     */
    @Override
    public TransactionManager beginTransaction() {
        this.transactionManager.startTransaction();
        return this.transactionManager;
    }

    /**
     * 关闭sql执行统计，将会影响getSqlExecuteStatsList的数据.
     */
    @Override
    public void disableSqlExecuteStats() {
        statsList = null;
    }

    /**
     * 打开sql执行统计，将会影响getSqlExecuteStatsList的数据.
     */
    @Override
    public void enableSqlExecuteStats() {
        statsList = new ArrayList<SqlExecuteStats>();
    }

    /**
     * 获取一个java.sql.Connection连接。 请注意，这是一个原生的Connection对象，需确保手工关闭.
     *
     * @param configName 配置名
     * @return Connection对象
     * @throws SQLException SQL异常
     */
    @Override
    public Connection getConnection(String configName) throws SQLException {
        return transactionManager.getConnection(configName);
    }

    /**
     * 根据表名和访问类型获取一个java.sql.Connection。 请注意，这是一个原生的Connection对象，需确保手工关闭.
     *
     * @param table  表名
     * @param access 访问类型。支持all/read/write
     * @return Connection对象
     * @throws SQLException SQL异常
     */
    @Override
    public Connection getConnection(String table, String access) throws SQLException {
        return transactionManager.getConnection(table, access);
    }

    /**
     * 根据表名和访问类型获取一个数据库连接配置名.
     *
     * @param table  表名
     * @param access 访问类型。支持all/read/write
     * @return 数据库连接配置名
     */
    @Override
    public String getConnectionName(String table, String access) {
        return transactionManager.getConnName(table, access);
    }

    /**
     * 获取当前DAOFactory实例下sql执行次数.
     *
     * @return sql执行次数
     */
    @Override
    public int getInvokeCount() {
        return transactionManager.getInvokeCount();
    }

    /**
     * 根据Entity来获取seq序列。 此序列通过一个系统数据库来维护，可以保证在分布式下的可用性.
     *
     * @param entityCls 实体类类型
     * @return seq序列
     */
    @Override
    public long getSequenceId(Class<?> entityCls) {
        return getSequenceId(entityCls.getSimpleName());
    }

    /**
     * 根据名称来获取seq序列。 此序列通过一个系统数据库来维护，可以保证在分布式下的可用性.
     *
     * @param seqName 表名
     * @return seq序列
     */
    @Override
    public long getSequenceId(String seqName) {
        return SequenceFactory.getSequenceId(seqName);
    }

    /**
     * 获取当前DAOFactory实例下的sql执行统计列表.
     *
     * @return 统计列表
     */
    @Override
    public ArrayList<SqlExecuteStats> getSqlExecuteStatsList() {
        return statsList;
    }

    /**
     * 保存一个Entity实例，等效于insert.
     *
     * @param <T>    映射的类型
     * @param entity 要更新的对象
     * @return Entity实例
     * @throws TransactionException 事务异常
     */
    @Override
    public <T extends DataEntity> T save(T entity) throws TransactionException {
        return EntityCommandImpl.save(this, null, entity, null);
    }

    /**
     * 保存一个Entity实例，等效于insert.
     *
     * @param <T>       映射的类型
     * @param entity    要更新的对象
     * @param tableName 指定表名
     * @return Entity实例
     * @throws TransactionException 事务异常
     */
    @Override
    public <T extends DataEntity> T save(T entity, String tableName) throws TransactionException {
        return EntityCommandImpl.save(this, null, entity, tableName);
    }

    /**
     * 保存一个Entity实例，等效于insert.
     *
     * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param <T>      映射的类型
     * @param entity   要更新的对象
     * @return Entity实例
     * @throws TransactionException 事务异常
     */
    @Override
    public <T extends DataEntity> T save(String connName, T entity) throws TransactionException {
        return EntityCommandImpl.save(this, connName, entity, null);
    }

    /**
     * 保存一个Entity实例，等效于insert.
     *
     * @param <T>       映射的类型
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entity    要更新的对象
     * @param tableName 指定表名
     * @return Entity实例
     * @throws TransactionException 事务异常
     */
    @Override
    public <T extends DataEntity> T save(String connName, T entity, String tableName) throws TransactionException {
        return EntityCommandImpl.save(this, connName, entity, tableName);
    }

    /**
     * 保存多个Entity实例，等效于insert values()().
     *
     * @param <T>        映射的类型
     * @param entityList 要保存的Entity实例集合
     * @return Entity实例集合
     * @throws TransactionException 事务异常
     */
    @Override
    public <T extends DataEntity> List<T> save(List<T> entityList) throws TransactionException {
        return EntityCommandImpl.save(this, null, entityList, null);
    }

    /**
     * 保存多个Entity实例，等效于insert values()().
     *
     * @param <T>        映射的类型
     * @param entityList 要保存的Entity实例集合
     * @param tableName  指定表名
     * @return Entity实例集合
     * @throws TransactionException 事务异常
     */
    @Override
    public <T extends DataEntity> List<T> save(List<T> entityList, String tableName) throws TransactionException {
        return EntityCommandImpl.save(this, null, entityList, tableName);
    }

    /**
     * 保存多个Entity实例，等效于insert values()().
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityList 要保存的Entity实例集合
     * @param <T>        映射的类型
     * @return Entity实例集合
     * @throws TransactionException 事务异常
     */
    @Override
    public <T extends DataEntity> List<T> save(String connName, List<T> entityList) throws TransactionException {
        return EntityCommandImpl.save(this, connName, entityList, null);
    }

    /**
     * 保存多个Entity实例，等效于insert values()().
     *
     * @param <T>        映射的类型
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityList 要保存的Entity实例集合
     * @param tableName  指定表名
     * @return Entity实例集合
     * @throws TransactionException 事务异常
     */
    @Override
    public <T extends DataEntity> List<T> save(String connName, List<T> entityList, String tableName) throws TransactionException {
        return EntityCommandImpl.save(this, connName, entityList, tableName);
    }

    /**
     * 根据主键更新一个Entity实例，等效于update.
     *
     * @param <T>    映射的类型
     * @param entity 要更新的对象
     * @return Entity实例
     * @throws TransactionException 事务异常
     */
    @Override
    public <T extends DataEntity> int update(T entity) throws TransactionException {
        return EntityCommandImpl.update(this, null, entity, null);
    }

    /**
     * 根据主键更新一个Entity实例，等效于update.
     *
     * @param <T>       映射的类型
     * @param entity    要更新的对象
     * @param tableName 指定表名
     * @return Entity实例
     * @throws TransactionException 事务异常
     */
    @Override
    public <T extends DataEntity> int update(T entity, String tableName) throws TransactionException {
        return EntityCommandImpl.update(this, null, entity, tableName);
    }

    /**
     * 根据主键更新一个Entity实例，等效于update.
     *
     * @param <T>      映射的类型
     * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entity   要更新的对象
     * @return Entity实例
     * @throws TransactionException 事务异常
     */
    @Override
    public <T extends DataEntity> int update(String connName, T entity) throws TransactionException {
        return EntityCommandImpl.update(this, connName, entity, null);
    }

    /**
     * 根据主键更新一个Entity实例，等效于update.
     *
     * @param <T>       映射的类型
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entity    要更新的对象
     * @param tableName 指定表名
     * @return Entity实例
     * @throws TransactionException 事务异常
     */
    @Override
    public <T extends DataEntity> int update(String connName, T entity, String tableName) throws TransactionException {
        return EntityCommandImpl.update(this, connName, entity, tableName);
    }

    /**
     * 根据QueryParam更新一个Entity实例，等效于update.
     *
     * @param entity
     * @param queryParam
     * @return
     */
    @Override
    public <T extends DataEntity> int update(T entity, QueryParam queryParam) throws TransactionException {
        return update(null, entity, null, queryParam);
    }

    /**
     * 根据QueryParam更新一个Entity实例，等效于update.
     *
     * @param connName
     * @param entity
     * @param queryParam
     * @return
     */
    @Override
    public <T extends DataEntity> int update(String connName, T entity, QueryParam queryParam) throws TransactionException {
        return update(connName, entity, null, queryParam);
    }

    /**
     * 根据QueryParam更新一个Entity实例，等效于update.
     *
     * @param connName
     * @param entity
     * @param queryParam
     * @return
     */
    @Override
    public <T extends DataEntity> int update(String connName, T entity, String tableName, QueryParam queryParam) throws TransactionException {
        // 有时候从数据库中load数据，并无实质更新，此时直接返回-1.
        if (!DataUpdateInfo.hasUpdateInfo(entity.GET_UPDATED_INFO())) {
            return 0;
        }
        // 解析查询参数。
        QueryParamResult queryParamResult = QueryParamUtils.parseQueryParam(queryParam);
        // 获取TableMetaInfo。
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
        StringBuilder sb = new StringBuilder(128 + queryParamResult.getSql().length());
        DataUpdateInfo dataUpdateInfo = entity.GET_UPDATED_INFO();
        List<FieldMetaInfo> updatedFields = emi.buildFieldMetaInfoList(dataUpdateInfo.getUpdateFieldSet());
        //参数列表。
        Object[] paramList = new Object[updatedFields.size()];
        try {
            sb.append("update ").append(tableName).append(" set ");
            int pos = 0;
            for (FieldMetaInfo fmi : updatedFields) {
                sb.append(fmi.getColumnName()).append("=?,");
                paramList[pos++] = fmi.getField().get(entity);
            }
            sb.deleteCharAt(sb.length() - 1);
        } catch (Exception e) {
            throw new TransactionException("FieldMetaInfo@[" + entity.getClass() + "] get error! " + e.toString(), e);
        }
        sb.append(queryParamResult.getSql());
        Object[] allParamList = ArrayUtils.addAll(paramList, queryParamResult.getParamList());
        return SQLCommandImpl.executeSQL(this, connName, sb.toString(), allParamList);
    }

    /**
     * 根据主键删除一个Entity实例，等效于delete.
     *
     * @param entity 要更新的对象
     * @param <T>    映射的类型
     * @return int
     * @throws TransactionException 事务异常
     */
    @Override
    public <T extends DataEntity> int delete(T entity) throws TransactionException {
        return EntityCommandImpl.delete(this, null, entity, null);
    }

    /**
     * 根据主键删除一个Entity实例，等效于delete.
     *
     * @param entity    要更新的对象
     * @param tableName 指定表名
     * @param <T>       映射的类型
     * @return int
     * @throws TransactionException 事务异常
     */
    @Override
    public <T extends DataEntity> int delete(T entity, String tableName) throws TransactionException {
        return EntityCommandImpl.delete(this, null, entity, tableName);
    }

    /**
     * 根据主键删除一个Entity实例，等效于delete.
     *
     * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entity   要更新的对象
     * @param <T>      映射的类型
     * @return int
     * @throws TransactionException 事务异常
     */
    @Override
    public <T extends DataEntity> int delete(String connName, T entity) throws TransactionException {
        return EntityCommandImpl.delete(this, connName, entity, null);
    }

    /**
     * 根据主键删除一个Entity实例，等效于delete.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entity    要更新的对象
     * @param tableName 指定表名
     * @param <T>       映射的类型
     * @return int
     * @throws TransactionException 事务异常
     */
    @Override
    public <T extends DataEntity> int delete(String connName, T entity, String tableName) throws TransactionException {
        return EntityCommandImpl.delete(this, connName, entity, tableName);
    }

    /**
     * 根据QueryParam删除信息，等效于delete.
     *
     * @param entityCls
     * @param queryParam
     * @return
     */
    @Override
    public <T extends DataEntity> int delete(Class<T> entityCls, QueryParam queryParam) throws TransactionException {
        return delete(null, entityCls, null, queryParam);
    }

    /**
     * 根据QueryParam删除信息，等效于delete.
     *
     * @param connName
     * @param entityCls
     * @param queryParam
     * @return
     */
    @Override
    public <T extends DataEntity> int delete(String connName, Class<T> entityCls, QueryParam queryParam) throws TransactionException {
        return delete(connName, entityCls, null, queryParam);
    }

    /**
     * 根据QueryParam删除信息，等效于delete.
     *
     * @param connName
     * @param entityCls
     * @param tableName
     * @param queryParam
     * @return
     */
    @Override
    public <T extends DataEntity> int delete(String connName, Class<T> entityCls, String tableName, QueryParam queryParam) throws TransactionException {
        // 解析查询参数。
        QueryParamResult queryParamResult = QueryParamUtils.parseQueryParam(queryParam);
        // 获取TableMetaInfo。
        TableMetaInfo emi = EntityMetaUtils.loadEntityMetaInfo(entityCls);
        if (emi == null) {
            throw new TransactionException("TableMetaInfo[" + entityCls + "] not found! ");
        }
        if (StringUtils.isBlank(tableName)) {
            tableName = emi.getTableName();
        }
        if (StringUtils.isBlank(connName)) {
            connName = DaoConfigManager.getRouteMapping(tableName, "write");
        }
        String sb = "delete from " + tableName + queryParamResult.getSql();
        return SQLCommandImpl.executeSQL(this, connName, sb, queryParamResult.getParamList());
    }

    /**
     * 根据指定的主键ID载入一个Entity实例.
     *
     * @param entityCls 要映射的对象类型
     * @param <T>       映射的类型
     * @param id        主键数值
     * @return DataList对象
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> T load(Class<T> entityCls, Serializable id) throws TransactionException {
        return EntityCommandImpl.load(this, null, entityCls, null, id);
    }

    /**
     * 根据指定的主键ID载入一个Entity实例.
     *
     * @param entityCls 要映射的对象类型
     * @param <T>       映射的类型
     * @param tableName 指定表名
     * @param id        主键数值
     * @return DataList对象
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> T load(Class<T> entityCls, String tableName, Serializable id) throws TransactionException {
        return EntityCommandImpl.load(this, null, entityCls, tableName, id);
    }

    /**
     * 根据指定的主键ID载入一个Entity实例.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls 要映射的对象类型
     * @param <T>       映射的类型
     * @param id        主键数值
     * @return DataList对象
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> T load(String connName, Class<T> entityCls, Serializable id) throws TransactionException {
        return EntityCommandImpl.load(this, connName, entityCls, null, id);
    }

    /**
     * 根据指定的主键ID载入一个Entity实例.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls 要映射的对象类型
     * @param <T>       映射的类型
     * @param tableName 指定表名
     * @param id        主键数值
     * @return DataList对象
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> T load(String connName, Class<T> entityCls, String tableName, Serializable id) throws TransactionException {
        return EntityCommandImpl.load(this, connName, entityCls, tableName, id);
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param entityCls 要映射的对象类型
     * @param selectSql 查询的SQL
     * @param <T>       映射的类型
     * @return DataList列表
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> DataList<T> list(Class<T> entityCls, String selectSql) throws TransactionException {
        return EntityCommandImpl.list(this, null, entityCls, selectSql, null, 0, 0, false);
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param entityCls  要映射的对象类型
     * @param selectSql  查询的SQL
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @param <T>        映射的类型
     * @return DataList列表
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> DataList<T> list(Class<T> entityCls, String selectSql, int startIndex, int resultNum) throws TransactionException {
        return EntityCommandImpl.list(this, null, entityCls, selectSql, null, startIndex, resultNum, false);
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param entityCls  要映射的对象类型
     * @param selectSql  查询的SQL
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @param autoCount  是否统计全部数据（用于分页算法），默认为false。
     * @param <T>        映射的类型
     * @return DataList列表
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> DataList<T> list(Class<T> entityCls, String selectSql, int startIndex, int resultNum, boolean autoCount) throws TransactionException {
        return EntityCommandImpl.list(this, null, entityCls, selectSql, null, startIndex, resultNum, autoCount);
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param entityCls 要映射的对象类型
     * @param selectSql 查询的SQL
     * @param paramList 查询SQL的绑定参数
     * @param <T>       映射的类型
     * @return DataList列表
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> DataList<T> list(Class<T> entityCls, String selectSql, Object[] paramList) throws TransactionException {
        return EntityCommandImpl.list(this, null, entityCls, selectSql, paramList, 0, 0, false);
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param entityCls  要映射的对象类型
     * @param selectSql  查询的SQL
     * @param paramList  查询SQL的绑定参数
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @param <T>        映射的类型
     * @return DataList列表
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> DataList<T> list(Class<T> entityCls, String selectSql, Object[] paramList, int startIndex, int resultNum) throws TransactionException {
        return EntityCommandImpl.list(this, null, entityCls, selectSql, paramList, startIndex, resultNum, false);
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param entityCls  要映射的对象类型
     * @param selectSql  查询的SQL
     * @param paramList  查询SQL的绑定参数
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @param autoCount  是否统计全部数据（用于分页算法），默认为false。
     * @param <T>        映射的类型
     * @return DataList列表
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> DataList<T> list(Class<T> entityCls, String selectSql, Object[] paramList, int startIndex, int resultNum, boolean autoCount) throws TransactionException {
        return EntityCommandImpl.list(this, null, entityCls, selectSql, paramList, startIndex, resultNum, autoCount);
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls 要映射的对象类型
     * @param selectSql 查询的SQL
     * @param <T>       映射的类型
     * @return DataList列表
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> DataList<T> list(String connName, Class<T> entityCls, String selectSql) throws TransactionException {
        return EntityCommandImpl.list(this, connName, entityCls, selectSql, null, 0, 0, false);
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls  要映射的对象类型
     * @param selectSql  查询的SQL
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @param <T>        映射的类型
     * @return DataList列表
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> DataList<T> list(String connName, Class<T> entityCls, String selectSql, int startIndex, int resultNum) throws TransactionException {
        return EntityCommandImpl.list(this, connName, entityCls, selectSql, null, startIndex, resultNum, false);
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls  要映射的对象类型
     * @param selectSql  查询的SQL
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @param autoCount  是否统计全部数据（用于分页算法），默认为false。
     * @param <T>        映射的类型
     * @return DataList列表
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> DataList<T> list(String connName, Class<T> entityCls, String selectSql, int startIndex, int resultNum, boolean autoCount) throws TransactionException {
        return EntityCommandImpl.list(this, connName, entityCls, selectSql, null, startIndex, resultNum, autoCount);
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls 要映射的对象类型
     * @param selectSql 查询的SQL
     * @param paramList 查询SQL的绑定参数
     * @param <T>       映射的类型
     * @return DataList列表
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> DataList<T> list(String connName, Class<T> entityCls, String selectSql, Object[] paramList) throws TransactionException {
        return EntityCommandImpl.list(this, connName, entityCls, selectSql, paramList, 0, 0, false);
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls  要映射的对象类型
     * @param <T>        映射的类型
     * @param selectSql  查询的SQL
     * @param paramList  查询SQL的绑定参数
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @return DataList列表
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> DataList<T> list(String connName, Class<T> entityCls, String selectSql, Object[] paramList, int startIndex, int resultNum) throws TransactionException {
        return EntityCommandImpl.list(this, connName, entityCls, selectSql, paramList, startIndex, resultNum, false);
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls  要映射的对象类型
     * @param <T>        映射的类型
     * @param selectSql  查询的SQL
     * @param paramList  查询SQL的绑定参数
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @param autoCount  是否统计全部数据（用于分页算法），默认为false。
     * @return DataList列表
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> DataList<T> list(String connName, Class<T> entityCls, String selectSql, Object[] paramList, int startIndex, int resultNum, boolean autoCount) throws TransactionException {
        return EntityCommandImpl.list(this, connName, entityCls, selectSql, paramList, startIndex, resultNum, autoCount);
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param entityCls      要映射的对象类型
     * @param pageQueryParam 分页查询对象
     * @return DataList列表
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> DataList<T> list(Class<T> entityCls, PageQueryParam pageQueryParam) throws TransactionException {
        return list(null, entityCls, pageQueryParam);
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param entityCls      要映射的对象类型
     * @param tableName      附加表名，在特定分表情况下。
     * @param pageQueryParam 分页查询对象
     * @return DataList列表
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> DataList<T> list(Class<T> entityCls, String tableName, PageQueryParam pageQueryParam) throws TransactionException {
        return list(null, entityCls, tableName, pageQueryParam);
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param connName       指定连接名
     * @param entityCls      要映射的对象类型
     * @param pageQueryParam 分页查询对象
     * @return DataList列表
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> DataList<T> list(String connName, Class<T> entityCls, PageQueryParam pageQueryParam) throws TransactionException {
        return list(connName, entityCls, null, pageQueryParam);
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param connName
     * @param entityCls      要映射的对象类型
     * @param tableName      附加表名，在特定分表情况下。
     * @param pageQueryParam 分页查询对象
     * @return DataList列表
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> DataList<T> list(String connName, Class<T> entityCls, String tableName, PageQueryParam pageQueryParam) throws TransactionException {
        QueryParamResult queryParamResult = QueryParamUtils.parseQueryParam(entityCls, tableName, pageQueryParam);
        if (pageQueryParam.CHECK_ONLY_COUNT()) {
            String countSql = "select count(1) from (" + queryParamResult.getSql().toString() + ") must_alias";
            int allSize = SQLCommandImpl.selectForSingleValue(this, connName, Integer.class, countSql, queryParamResult.getParamList());
            return new DataList<>(null, pageQueryParam.START_INDEX(), pageQueryParam.RESULT_NUM(), allSize);
        } else {
            return EntityCommandImpl.list(this, connName, entityCls, queryParamResult.getSql().toString(), queryParamResult.getParamList(), pageQueryParam.START_INDEX(), pageQueryParam.RESULT_NUM(), pageQueryParam.CHECK_AUTO_COUNT());
        }
    }

    /**
     * 查询单个基本数值（单个字段）.
     *
     * @param valueCls 要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param <T>      映射的类型
     * @param sql      查询的SQL
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> T queryForSingleValue(Class<T> valueCls, String sql) throws TransactionException {
        return SQLCommandImpl.selectForSingleValue(this, null, valueCls, sql, null);
    }

    /**
     * 查询单个基本数值（单个字段）.
     *
     * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param valueCls 要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param <T>      映射的类型
     * @param sql      查询的SQL
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> T queryForSingleValue(String connName, Class<T> valueCls, String sql) throws TransactionException {
        return SQLCommandImpl.selectForSingleValue(this, connName, valueCls, sql, null);
    }

    /**
     * 查询单个基本数值（单个字段）.
     *
     * @param valueCls  要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param <T>       映射的类型
     * @param sql       查询的SQL
     * @param paramList 查询SQL的参数
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> T queryForSingleValue(Class<T> valueCls, String sql, Object[] paramList) throws TransactionException {
        return SQLCommandImpl.selectForSingleValue(this, null, valueCls, sql, paramList);
    }

    /**
     * 查询单个基本数值（单个字段）.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param valueCls  要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param <T>       映射的类型
     * @param sql       查询的SQL
     * @param paramList 查询SQL的参数
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> T queryForSingleValue(String connName, Class<T> valueCls, String sql, Object[] paramList) throws TransactionException {
        return SQLCommandImpl.selectForSingleValue(this, connName, valueCls, sql, paramList);
    }

    /**
     * 查询单个基本数值（单个字段）.
     *
     * @param valueCls   要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param queryParam 查询参数
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> T queryForSingleValue(Class<T> valueCls, QueryParam queryParam) throws TransactionException {
        QueryParamResult queryParamResult = QueryParamUtils.parseQueryParam(null, null, queryParam);
        return SQLCommandImpl.selectForSingleValue(this, null, valueCls, queryParamResult.getSql().toString(), queryParamResult.getParamList());
    }

    /**
     * 查询单个基本数值（单个字段）.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param valueCls   要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param queryParam 查询参数
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> T queryForSingleValue(String connName, Class<T> valueCls, QueryParam queryParam) throws TransactionException {
        QueryParamResult queryParamResult = QueryParamUtils.parseQueryParam(null, null, queryParam);
        return SQLCommandImpl.selectForSingleValue(this, connName, valueCls, queryParamResult.getSql().toString(), queryParamResult.getParamList());
    }

    /**
     * 查询单个基本数值列表（多行单个字段）.
     *
     * @param valueCls 要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param <T>      映射的类型
     * @param sql      查询的SQL
     * @return DataSet对象
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> ArrayList<T> queryForSingleList(Class<T> valueCls, String sql) throws TransactionException {
        return SQLCommandImpl.selectForSingleList(this, null, valueCls, sql, null);
    }

    /**
     * 查询单个基本数值列表（多行单个字段）.
     *
     * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param valueCls 要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param <T>      映射的类型
     * @param sql      查询的SQL
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> ArrayList<T> queryForSingleList(String connName, Class<T> valueCls, String sql) throws TransactionException {
        return SQLCommandImpl.selectForSingleList(this, connName, valueCls, sql, null);
    }

    /**
     * 查询单个基本数值列表（多行单个字段）.
     *
     * @param valueCls   要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param queryParam 查询参数
     * @return DataSet数据列表
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> ArrayList<T> queryForSingleList(Class<T> valueCls, QueryParam queryParam) throws TransactionException {
        return queryForSingleList(null, valueCls, queryParam);
    }

    /**
     * 查询单个基本数值列表（多行单个字段）.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param valueCls   要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param queryParam 查询参数
     * @return DataSet数据列表
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> ArrayList<T> queryForSingleList(String connName, Class<T> valueCls, QueryParam queryParam) throws TransactionException {
        QueryParamResult queryParamResult = QueryParamUtils.parseQueryParam(null, null, queryParam);
        return SQLCommandImpl.selectForSingleList(this, connName, valueCls, queryParamResult.getSql().toString(), queryParamResult.getParamList());
    }

    /**
     * 查询单个基本数值列表（多行单个字段）.
     *
     * @param valueCls  要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param <T>       映射的类型
     * @param sql       查询的SQL
     * @param paramList 查询SQL的参数
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> ArrayList<T> queryForSingleList(Class<T> valueCls, String sql, Object[] paramList) throws TransactionException {
        return SQLCommandImpl.selectForSingleList(this, null, valueCls, sql, paramList);
    }

    /**
     * 查询单个基本数值列表（多行单个字段）.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param valueCls  要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param <T>       映射的类型
     * @param sql       查询的SQL
     * @param paramList 查询SQL的参数
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> ArrayList<T> queryForSingleList(String connName, Class<T> valueCls, String sql, Object[] paramList) throws TransactionException {
        return SQLCommandImpl.selectForSingleList(this, connName, valueCls, sql, paramList);
    }

    /**
     * 查询单个对象（单行数据）。 使用sql中探测到的表名来决定连接名.
     *
     * @param entityCls 要映射的对象类型
     * @param <T>       映射的类型
     * @param selectSql 查询的SQL
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> T queryForSingleObject(Class<T> entityCls, String selectSql) throws TransactionException {
        return EntityCommandImpl.listSingle(this, null, entityCls, selectSql, null);
    }

    /**
     * 查询单个对象（单行数据）。 使用sql中探测到的表名来决定连接名.
     *
     * @param entityCls 要映射的对象类型
     * @param <T>       映射的类型
     * @param selectSql 查询的SQL
     * @param paramList 查询SQL的参数
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> T queryForSingleObject(Class<T> entityCls, String selectSql, Object[] paramList) throws TransactionException {
        return EntityCommandImpl.listSingle(this, null, entityCls, selectSql, paramList);
    }

    /**
     * 查询单个对象（单行数据）.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls 要映射的对象类型
     * @param <T>       映射的类型
     * @param selectSql 查询的SQL
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> T queryForSingleObject(String connName, Class<T> entityCls, String selectSql) throws TransactionException {
        return EntityCommandImpl.listSingle(this, connName, entityCls, selectSql, null);
    }

    /**
     * 查询单个对象（单行数据）.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls 要映射的对象类型
     * @param <T>       映射的类型
     * @param selectSql 查询的SQL
     * @param paramList 查询SQL的参数
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> T queryForSingleObject(String connName, Class<T> entityCls, String selectSql, Object[] paramList) throws TransactionException {
        return EntityCommandImpl.listSingle(this, connName, entityCls, selectSql, paramList);
    }

    /**
     * 查询单个对象（单行数据）.
     *
     * @param entityCls  要映射的对象类型
     * @param queryParam 查询参数
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> T queryForSingleObject(Class<T> entityCls, QueryParam queryParam) throws TransactionException {
        return queryForSingleObject(null, entityCls, null, queryParam);
    }

    /**
     * 查询单个对象（单行数据）.
     *
     * @param entityCls  要映射的对象类型
     * @param tableName  指定表名
     * @param queryParam 查询参数
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> T queryForSingleObject(Class<T> entityCls, String tableName, QueryParam queryParam) throws TransactionException {
        return queryForSingleObject(null, entityCls, tableName, queryParam);
    }

    /**
     * 查询单个对象（单行数据）.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls  要映射的对象类型
     * @param queryParam 查询参数
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> T queryForSingleObject(String connName, Class<T> entityCls, QueryParam queryParam) throws TransactionException {
        return queryForSingleObject(null, entityCls, null, queryParam);
    }

    /**
     * 查询单个对象（单行数据）.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls  要映射的对象类型
     * @param queryParam 查询参数
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    @Override
    public <T> T queryForSingleObject(String connName, Class<T> entityCls, String tableName, QueryParam queryParam) throws TransactionException {
        QueryParamResult queryParamResult = QueryParamUtils.parseQueryParam(entityCls, tableName, queryParam);
        return EntityCommandImpl.listSingle(this, connName, entityCls, queryParamResult.getSql().append(" limit 1").toString(), queryParamResult.getParamList());
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param selectSql 查询的SQL
     * @return DataSet数据列表
     * @throws TransactionException 事务异常
     */
    @Override
    public DataSet queryForDataSet(String selectSql) throws TransactionException {
        return SQLCommandImpl.selectForDataSet(this, null, selectSql, null, 0, 0, false);
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param selectSql  查询的SQL
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @return DataSet数据列表
     * @throws TransactionException 事务异常
     */
    @Override
    public DataSet queryForDataSet(String selectSql, int startIndex, int resultNum) throws TransactionException {
        return SQLCommandImpl.selectForDataSet(this, null, selectSql, null, startIndex, resultNum, false);
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param selectSql  查询的SQL
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @param autoCount  是否统计全部数据（用于分页算法），默认为false。
     * @return DataSet数据列表
     * @throws TransactionException 事务异常
     */
    @Override
    public DataSet queryForDataSet(String selectSql, int startIndex, int resultNum, boolean autoCount) throws TransactionException {
        return SQLCommandImpl.selectForDataSet(this, null, selectSql, null, startIndex, resultNum, autoCount);
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param selectSql 查询的SQL
     * @param paramList 查询SQL的绑定参数
     * @return DataSet数据列表
     * @throws TransactionException 事务异常
     */
    @Override
    public DataSet queryForDataSet(String selectSql, Object[] paramList) throws TransactionException {
        return SQLCommandImpl.selectForDataSet(this, null, selectSql, paramList, 0, 0, false);
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param selectSql  查询的SQL
     * @param paramList  查询SQL的绑定参数
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @return DataSet数据列表
     * @throws TransactionException 事务异常
     */
    @Override
    public DataSet queryForDataSet(String selectSql, Object[] paramList, int startIndex, int resultNum) throws TransactionException {
        return SQLCommandImpl.selectForDataSet(this, null, selectSql, paramList, startIndex, resultNum, false);
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param selectSql  查询的SQL
     * @param paramList  查询SQL的绑定参数
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @param autoCount  是否统计全部数据（用于分页算法），默认为false。
     * @return DataSet数据列表
     * @throws TransactionException 事务异常
     */
    @Override
    public DataSet queryForDataSet(String selectSql, Object[] paramList, int startIndex, int resultNum, boolean autoCount) throws TransactionException {
        return SQLCommandImpl.selectForDataSet(this, null, selectSql, paramList, startIndex, resultNum, autoCount);
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param connName  连接名，当设置为null时候，根据sql语句或表名确定
     * @param selectSql 查询的SQL
     * @return DataSet数据列表
     * @throws TransactionException 事务异常
     */
    @Override
    public DataSet queryForDataSet(String connName, String selectSql) throws TransactionException {
        return SQLCommandImpl.selectForDataSet(this, connName, selectSql, null, 0, 0, false);
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param selectSql  查询的SQL
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @return DataSet数据列表
     * @throws TransactionException 事务异常
     */
    @Override
    public DataSet queryForDataSet(String connName, String selectSql, int startIndex, int resultNum) throws TransactionException {
        return SQLCommandImpl.selectForDataSet(this, connName, selectSql, null, startIndex, resultNum, false);

    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param selectSql  查询的SQL
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @param autoCount  是否统计全部数据（用于分页算法），默认为false。
     * @return DataSet数据列表
     * @throws TransactionException 事务异常
     */
    @Override
    public DataSet queryForDataSet(String connName, String selectSql, int startIndex, int resultNum, boolean autoCount) throws TransactionException {
        return SQLCommandImpl.selectForDataSet(this, connName, selectSql, null, startIndex, resultNum, autoCount);
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param selectSql 查询的SQL
     * @param paramList 查询SQL的绑定参数
     * @return DataSet数据列表
     * @throws TransactionException 事务异常
     */
    @Override
    public DataSet queryForDataSet(String connName, String selectSql, Object[] paramList) throws TransactionException {
        return SQLCommandImpl.selectForDataSet(this, connName, selectSql, paramList, 0, 0, false);
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param selectSql  查询的SQL
     * @param paramList  查询SQL的绑定参数
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @return DataSet数据列表
     * @throws TransactionException 事务异常
     */
    @Override
    public DataSet queryForDataSet(String connName, String selectSql, Object[] paramList, int startIndex, int resultNum) throws TransactionException {
        return SQLCommandImpl.selectForDataSet(this, connName, selectSql, paramList, startIndex, resultNum, false);

    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param selectSql  查询的SQL
     * @param paramList  查询SQL的绑定参数
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @param autoCount  是否统计全部数据（用于分页算法），默认为false。
     * @return DataSet数据列表
     * @throws TransactionException 事务异常
     */
    @Override
    public DataSet queryForDataSet(String connName, String selectSql, Object[] paramList, int startIndex, int resultNum, boolean autoCount) throws TransactionException {
        return SQLCommandImpl.selectForDataSet(this, connName, selectSql, paramList, startIndex, resultNum, autoCount);
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param queryParam 查询参数
     * @return DataSet数据列表
     * @throws TransactionException 事务异常
     */
    @Override
    public DataSet queryForDataSet(QueryParam queryParam) throws TransactionException {
        return queryForDataSet(null, queryParam);
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param queryParam 查询参数
     * @return DataSet数据列表
     * @throws TransactionException 事务异常
     */
    @Override
    public DataSet queryForDataSet(String connName, QueryParam queryParam) throws TransactionException {
        QueryParamResult queryParamResult = QueryParamUtils.parseQueryParam(null, null, queryParam);
        int startIndex = 0;
        int resultNum = 0;
        boolean autoCount = false;
        if (queryParam instanceof PageQueryParam param) {
            startIndex = param.START_INDEX();
            resultNum = param.RESULT_NUM();
            autoCount = param.CHECK_AUTO_COUNT();
        }
        return SQLCommandImpl.selectForDataSet(this, connName, queryParamResult.getSql().toString(), queryParamResult.getParamList(), startIndex, resultNum, autoCount);
    }

    /**
     * 执行一条SQL语句.
     *
     * @param sql 查询的SQL
     * @return 影响的行数
     * @throws TransactionException 事务异常
     */
    @Override
    public int executeCommand(String sql) throws TransactionException {
        return SQLCommandImpl.executeSQL(this, null, sql, null);

    }

    /**
     * 执行一条SQL语句.
     *
     * @param sql       查询的SQL
     * @param paramList 查询SQL的参数
     * @return 影响的行数
     * @throws TransactionException 事务异常
     */
    @Override
    public int executeCommand(String sql, Object[] paramList) throws TransactionException {
        return SQLCommandImpl.executeSQL(this, null, sql, paramList);

    }

    /**
     * 执行一条SQL语句.
     *
     * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param sql      查询的SQL
     * @return 影响的行数
     * @throws TransactionException 事务异常
     */
    @Override
    public int executeCommand(String connName, String sql) throws TransactionException {
        return SQLCommandImpl.executeSQL(this, connName, sql, null);

    }

    /**
     * 执行一条SQL语句.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param sql       查询的SQL
     * @param paramList 查询SQL的参数
     * @return 影响的行数
     * @throws TransactionException 事务异常
     */
    @Override
    public int executeCommand(String connName, String sql, Object[] paramList) throws TransactionException {
        return SQLCommandImpl.executeSQL(this, connName, sql, paramList);
    }

    /**
     * 解析QueryParam信息。
     *
     * @param queryParam 查询参数类。
     * @return
     * @throws TransactionException
     */
    @Override
    public QueryParamResult parseQueryParam(QueryParam queryParam) {
        return QueryParamUtils.parseQueryParam(null, null, queryParam);
    }

    /**
     * 解析QueryParam信息。
     *
     * @param cls        要返回的对象。
     * @param queryParam 查询参数类。
     * @return
     * @throws TransactionException
     */
    @Override
    public QueryParamResult parseQueryParam(Class cls, QueryParam queryParam) {
        return QueryParamUtils.parseQueryParam(cls, null, queryParam);
    }

    /**
     * 解析QueryParam信息。
     *
     * @param cls        要返回的对象。
     * @param tableName  附加表名，用于特殊的分表场景。
     * @param queryParam 查询参数类。
     * @return
     * @throws TransactionException
     */
    @Override
    public QueryParamResult parseQueryParam(Class cls, String tableName, QueryParam queryParam) {
        return QueryParamUtils.parseQueryParam(cls, tableName, queryParam);
    }

    /**
     * 获取控制器.
     *
     * @return BatchupdateManagerImpl对象
     */
    BatchUpdateManagerImpl getBatchUpdateController() {
        return batchUpdateManager;
    }

    /**
     * 获取事务控制器.
     *
     * @return TransactionManagerImpl对象
     */
    TransactionManagerImpl getTransactionController() {
        return transactionManager;
    }
}
