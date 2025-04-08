package uw.dao;

import uw.dao.impl.DaoFactoryImpl;
import uw.dao.vo.QueryParamResult;
import uw.dao.vo.SqlExecuteStats;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 整个Dao模块的入口，所有数据库操作都从这个类开始.
 *
 * @author axeon
 */
public class DaoManager {

    private final DaoFactory daoFactory;

    /**
     * 构造函数.
     * @param daoFactory
     */
    private DaoManager(DaoFactory daoFactory) {
        this.daoFactory = daoFactory;
    }

    /**
     * 获取一个MainFactory实例, 此实例是线程安全的.
     *
     * @return DaoManager
     */
    public static DaoManager getInstance() {
        return new DaoManager(DaoFactory.getInstance());
    }

    /**
     * 获取一个DAOManager实例。 指定connName，这时候将不会使用dao来决定数据库连接.
     *
     * @return DaoManager
     */
    public static DaoManager getInstance(String connName) {
        return new DaoManager(DaoFactory.getInstance(connName));
    }

    /**
     * 根据主键删除一个Entity实例，等效于delete.
     *
     * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entity   要更新的对象
     * @return int
     * 
     */
    public <T extends DataEntity> int delete(String connName, T entity) {
        return daoFactory.delete( connName, entity );
    }

    /**
     * 获得一个batchupdate handle.
     *
     * @return BatchupdateManager对象
     * 
     */
    public BatchUpdateManager beginBatchUpdate() {
        return daoFactory.beginBatchUpdate();
    }

    /**
     * 获得一个java.sql.Connection连接。 请注意，这是一个原生的Connection对象，需确保手工关闭.
     *
     * @param configName 配置名
     * @return Connection对象
     * @throws SQLException SQL异常
     */
    public Connection getConnection(String configName) throws SQLException {
        return daoFactory.getConnection( configName );
    }

    /**
     * 根据Entity来获得seq序列。 此序列通过一个系统数据库来维护，可以保证在分布式下的可用性.
     *
     * @param entityCls 实体类类型
     * @return seq序列
     */
    public long getSequenceId(Class<?> entityCls) {
        return daoFactory.getSequenceId( entityCls );
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
     * 
     */
    public DataSet queryForDataSet(String selectSql, Object[] paramList, int startIndex, int resultNum, boolean autoCount) {
        return daoFactory.queryForDataSet( selectSql, paramList, startIndex, resultNum, autoCount );
    }

    /**
     * 查询单个基本数值列表（多行单个字段）.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param valueCls   要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param queryParam 查询参数
     * @return DataSet数据列表
     * 
     */
    public <T> ArrayList<T> queryForSingleList(String connName, Class<T> valueCls, QueryParam queryParam) {
        return daoFactory.queryForSingleList( connName, valueCls, queryParam );
    }

    /**
     * 查询单个对象（单行数据）.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls 要映射的对象类型
     * @param selectSql 查询的SQL
     * @return 单个对象
     * 
     */
    public <T> T queryForSingleObject(String connName, Class<T> entityCls, String selectSql) {
        return daoFactory.queryForSingleObject( connName, entityCls, selectSql );
    }

    /**
     * 执行一条SQL语句.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param sql       查询的SQL
     * @param paramList 查询SQL的参数
     * @return 影响的行数
     * 
     */
    public int executeCommand(String connName, String sql, Object[] paramList) {
        return daoFactory.executeCommand( connName, sql, paramList );
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls  要映射的对象类型
     * @param selectSql  查询的SQL
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @return DataList列表
     * 
     */
    public <T> DataList<T> list(String connName, Class<T> entityCls, String selectSql, int startIndex, int resultNum) {
        return daoFactory.list( connName, entityCls, selectSql, startIndex, resultNum );
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
    public QueryParamResult parseQueryParam(Class cls, String tableName, QueryParam queryParam) {
        return daoFactory.parseQueryParam( cls, tableName, queryParam );
    }

    /**
     * 根据指定的主键ID载入一个Entity实例.
     *
     * @param entityCls 要映射的对象类型
     * @param tableName 指定表名
     * @param id        主键数值
     * @return DataList对象
     * 
     */
    public <T> T load(Class<T> entityCls, String tableName, Serializable id) {
        return daoFactory.load( entityCls, tableName, id );
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param selectSql 查询的SQL
     * @param paramList 查询SQL的绑定参数
     * @return DataSet数据列表
     * 
     */
    public DataSet queryForDataSet(String connName, String selectSql, Object[] paramList) {
        return daoFactory.queryForDataSet( connName, selectSql, paramList );
    }

    /**
     * 查询单个基本数值列表（多行单个字段）.
     *
     * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param valueCls 要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param sql      查询的SQL
     * @return 单个对象
     * 
     */
    public <T> ArrayList<T> queryForSingleList(String connName, Class<T> valueCls, String sql) {
        return daoFactory.queryForSingleList( connName, valueCls, sql );
    }

    /**
     * 查询单个对象（单行数据）.
     *
     * @param entityCls  要映射的对象类型
     * @param queryParam 查询参数
     * @return 单个对象
     * 
     */
    public <T> T queryForSingleObject(Class<T> entityCls, QueryParam queryParam) {
        return daoFactory.queryForSingleObject( entityCls, queryParam );
    }

    /**
     * 保存一个Entity实例，等效于insert.
     *
     * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entity   要更新的对象
     * @return Entity实例
     * 
     */
    public <T extends DataEntity> T save(String connName, T entity) {
        return daoFactory.save( connName, entity );
    }

    /**
     * 解析QueryParam信息。
     *
     * @param queryParam 查询参数类。
     * @return
     * @throws TransactionException
     */
    public QueryParamResult parseQueryParam(QueryParam queryParam) {
        return daoFactory.parseQueryParam( queryParam );
    }

    /**
     * 获得当前DAOFactory实例下sql执行次数.
     *
     * @return sql执行次数
     */
    public int getInvokeCount() {
        return daoFactory.getInvokeCount();
    }

    /**
     * 查询单个基本数值（单个字段）.
     *
     * @param cls 要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param sql 查询的SQL
     * @return 单个对象
     * 
     */
    public <T> T queryForSingleValue(Class<T> cls, String sql) {
        return daoFactory.queryForSingleValue( cls, sql );
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param selectSql 查询的SQL
     * @param paramList 查询SQL的绑定参数
     * @return DataSet数据列表
     * 
     */
    public DataSet queryForDataSet(String selectSql, Object[] paramList) {
        return daoFactory.queryForDataSet( selectSql, paramList );
    }

    /**
     * 查询单个基本数值（单个字段）.
     *
     * @param cls       要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param sql       查询的SQL
     * @param paramList 查询SQL的参数
     * @return 单个对象
     * 
     */
    public <T> T queryForSingleValue(Class<T> cls, String sql, Object[] paramList) {
        return daoFactory.queryForSingleValue( cls, sql, paramList );
    }

    /**
     * 开始一个数据库事务.
     *
     * @return TransactionManager对象
     */
    public TransactionManager beginTransaction() {
        return daoFactory.beginTransaction();
    }

    /**
     * 根据主键删除一个Entity实例，等效于delete.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entity    要更新的对象
     * @param tableName 指定表名
     * @return int
     * 
     */
    public <T extends DataEntity> int delete(String connName, T entity, String tableName) {
        return daoFactory.delete( connName, entity, tableName );
    }

    /**
     * 查询单个对象（单行数据）.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls 要映射的对象类型
     * @param selectSql 查询的SQL
     * @param paramList 查询SQL的参数
     * @return 单个对象
     * 
     */
    public <T> T queryForSingleObject(String connName, Class<T> entityCls, String selectSql, Object[] paramList) {
        return daoFactory.queryForSingleObject( connName, entityCls, selectSql, paramList );
    }

    /**
     * 根据主键更新一个Entity实例，等效于update.
     *
     * @param entity    要更新的对象
     * @param tableName 指定表名
     * @return Entity实例
     * 
     */
    public <T extends DataEntity> int update(T entity, String tableName) {
        return daoFactory.update( entity, tableName );
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param entityCls      要映射的对象类型
     * @param pageQueryParam 分页查询对象
     * @return DataList列表
     * 
     */
    public <T> DataList<T> list(Class<T> entityCls, PageQueryParam pageQueryParam) {
        return daoFactory.list( entityCls, pageQueryParam );
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param selectSql  查询的SQL
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @return DataSet数据列表
     * 
     */
    public DataSet queryForDataSet(String connName, String selectSql, int startIndex, int resultNum) {
        return daoFactory.queryForDataSet( connName, selectSql, startIndex, resultNum );
    }

    /**
     * 根据主键删除一个Entity实例，等效于delete.
     *
     * @param entity 要更新的对象
     * @return int
     * 
     */
    public <T extends DataEntity> int delete(T entity) {
        return daoFactory.delete( entity );
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param entityCls 要映射的对象类型
     * @param selectSql 查询的SQL
     * @param paramList 查询SQL的绑定参数
     * @return DataList列表
     * 
     */
    public <T> DataList<T> list(Class<T> entityCls, String selectSql, Object[] paramList) {
        return daoFactory.list( entityCls, selectSql, paramList );
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
     * 
     */
    public DataSet queryForDataSet(String connName, String selectSql, Object[] paramList, int startIndex, int resultNum) {
        return daoFactory.queryForDataSet( connName, selectSql, paramList, startIndex, resultNum );
    }

    /**
     * 根据指定的主键ID载入一个Entity实例.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls 要映射的对象类型
     * @param id        主键数值
     * @return DataList对象
     * 
     */
    public <T> T load(String connName, Class<T> entityCls, Serializable id) {
        return daoFactory.load( connName, entityCls, id );
    }

    /**
     * 保存一个Entity实例，等效于insert.
     *
     * @param entity 要更新的对象
     * @return Entity实例
     * 
     */
    public <T extends DataEntity> T save(T entity) {
        return daoFactory.save( entity );
    }

    /**
     * 根据主键删除一个Entity实例，等效于delete.
     *
     * @param entity    要更新的对象
     * @param tableName 指定表名
     * @return int
     * 
     */
    public <T extends DataEntity> int delete(T entity, String tableName) {
        return daoFactory.delete( entity, tableName );
    }

    /**
     * 根据表名和访问类型获得一个数据库连接配置名.
     *
     * @param table  表名
     * @param access 访问类型。支持all/read/write
     * @return 数据库连接配置名
     */
    public String getConnectionName(String table, String access) {
        return daoFactory.getConnectionName( table, access );
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls  要映射的对象类型
     * @param selectSql  查询的SQL
     * @param paramList  查询SQL的绑定参数
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @param autoCount  是否统计全部数据（用于分页算法），默认为false。
     * @return DataList列表
     * 
     */
    public <T> DataList<T> list(String connName, Class<T> entityCls, String selectSql, Object[] paramList, int startIndex, int resultNum, boolean autoCount) {
        return daoFactory.list( connName, entityCls, selectSql, paramList, startIndex, resultNum, autoCount );
    }

    /**
     * 根据指定的主键ID载入一个Entity实例.
     *
     * @param entityCls 要映射的对象类型
     * @param id        主键数值
     * @return DataList对象
     * 
     */
    public <T> T load(Class<T> entityCls, Serializable id) {
        return daoFactory.load( entityCls, id );
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
     * @return DataList列表
     * 
     */
    public <T> DataList<T> list(Class<T> entityCls, String selectSql, Object[] paramList, int startIndex, int resultNum, boolean autoCount) {
        return daoFactory.list( entityCls, selectSql, paramList, startIndex, resultNum, autoCount );
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls  要映射的对象类型
     * @param selectSql  查询的SQL
     * @param paramList  查询SQL的绑定参数
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @return DataList列表
     * 
     */
    public <T> DataList<T> list(String connName, Class<T> entityCls, String selectSql, Object[] paramList, int startIndex, int resultNum) {
        return daoFactory.list( connName, entityCls, selectSql, paramList, startIndex, resultNum );
    }

    /**
     * 保存多个Entity实例，等效于insert values()().
     *
     * @param entityList 要保存的Entity实例集合
     * @return Entity实例集合
     * 
     */
    public <T extends DataEntity> List<T> batchSave(List<T> entityList) {
        return daoFactory.batchSave( entityList );
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param selectSql  查询的SQL
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @return DataSet数据列表
     * 
     */
    public DataSet queryForDataSet(String selectSql, int startIndex, int resultNum) {
        return daoFactory.queryForDataSet( selectSql, startIndex, resultNum );
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
     * 
     */
    public DataSet queryForDataSet(String connName, String selectSql, Object[] paramList, int startIndex, int resultNum, boolean autoCount) {
        return daoFactory.queryForDataSet( connName, selectSql, paramList, startIndex, resultNum, autoCount );
    }

    /**
     * 根据主键更新一个Entity实例，等效于update.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entity    要更新的对象
     * @param tableName 指定表名
     * @return Entity实例
     * 
     */
    public <T extends DataEntity> int update(String connName, T entity, String tableName) {
        return daoFactory.update( connName, entity, tableName );
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param connName
     * @param entityCls      要映射的对象类型
     * @param pageQueryParam 分页查询对象
     * @return DataList列表
     * 
     */
    public <T> DataList<T> list(String connName, Class<T> entityCls, PageQueryParam pageQueryParam) {
        return daoFactory.list( connName, entityCls, pageQueryParam );
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param queryParam 查询参数
     * @return DataSet数据列表
     * 
     */
    public DataSet queryForDataSet(String connName, QueryParam queryParam) {
        return daoFactory.queryForDataSet( connName, queryParam );
    }

    /**
     * 查询单个基本数值（单个字段）.
     *
     * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param cls      要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param sql      查询的SQL
     * @return 单个对象
     * 
     */
    public <T> T queryForSingleValue(String connName, Class<T> cls, String sql) {
        return daoFactory.queryForSingleValue( connName, cls, sql );
    }

    /**
     * 关闭sql执行统计，将会影响getSqlExecuteStatsList的数据.
     */
    public void disableSqlExecuteStats() {
        daoFactory.disableSqlExecuteStats();
    }

    /**
     * 打开sql执行统计，将会影响getSqlExecuteStatsList的数据.
     */
    public void enableSqlExecuteStats() {
        daoFactory.enableSqlExecuteStats();
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls 要映射的对象类型
     * @param selectSql 查询的SQL
     * @param paramList 查询SQL的绑定参数
     * @return DataList列表
     * 
     */
    public <T> DataList<T> list(String connName, Class<T> entityCls, String selectSql, Object[] paramList) {
        return daoFactory.list( connName, entityCls, selectSql, paramList );
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param selectSql  查询的SQL
     * @param paramList  查询SQL的绑定参数
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @return DataSet数据列表
     * 
     */
    public DataSet queryForDataSet(String selectSql, Object[] paramList, int startIndex, int resultNum) {
        return daoFactory.queryForDataSet( selectSql, paramList, startIndex, resultNum );
    }

    /**
     * 根据主键更新一个Entity实例，等效于update.
     *
     * @param entity 要更新的对象
     * @return Entity实例
     * 
     */
    public <T extends DataEntity> int update(T entity) {
        return daoFactory.update( entity );
    }

    /**
     * 保存多个Entity实例，等效于insert values()().
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityList 要保存的Entity实例集合
     * @param tableName  指定表名
     * @return Entity实例集合
     * 
     */
    public <T extends DataEntity> List<T> batchSave(String connName, List<T> entityList, String tableName) {
        return daoFactory.batchSave( connName, entityList, tableName );
    }

    /**
     * 根据表名来获得seq序列。 此序列通过一个系统数据库来维护，可以保证在分布式下的可用性.
     *
     * @param seqName 表名
     * @return seq序列
     */
    public long getSequenceId(String seqName) {
        return daoFactory.getSequenceId( seqName );
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param entityCls  要映射的对象类型
     * @param selectSql  查询的SQL
     * @param paramList  查询SQL的绑定参数
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @return DataList列表
     * 
     */
    public <T> DataList<T> list(Class<T> entityCls, String selectSql, Object[] paramList, int startIndex, int resultNum) {
        return daoFactory.list( entityCls, selectSql, paramList, startIndex, resultNum );
    }

    /**
     * 根据指定的主键ID载入一个Entity实例.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls 要映射的对象类型
     * @param tableName 指定表名
     * @param id        主键数值
     * @return DataList对象
     * 
     */
    public <T> T load(String connName, Class<T> entityCls, String tableName, Serializable id) {
        return daoFactory.load( connName, entityCls, tableName, id );
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param selectSql 查询的SQL
     * @return DataSet数据列表
     * 
     */
    public DataSet queryForDataSet(String selectSql) {
        return daoFactory.queryForDataSet( selectSql );
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param connName  连接名，当设置为null时候，根据sql语句或表名确定
     * @param selectSql 查询的SQL
     * @return DataSet数据列表
     * 
     */
    public DataSet queryForDataSet(String connName, String selectSql) {
        return daoFactory.queryForDataSet( connName, selectSql );
    }

    /**
     * 查询单个基本数值列表（多行单个字段）.
     *
     * @param valueCls  要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param sql       查询的SQL
     * @param paramList 查询SQL的参数
     * @return 单个对象
     * 
     */
    public <T> ArrayList<T> queryForSingleList(Class<T> valueCls, String sql, Object[] paramList) {
        return daoFactory.queryForSingleList( valueCls, sql, paramList );
    }

    /**
     * 查询单个对象（单行数据）。 使用sql中探测到的表名来决定连接名.
     *
     * @param entityCls 要映射的对象类型
     * @param selectSql 查询的SQL
     * @param paramList 查询SQL的参数
     * @return 单个对象
     * 
     */
    public <T> T queryForSingleObject(Class<T> entityCls, String selectSql, Object[] paramList) {
        return daoFactory.queryForSingleObject( entityCls, selectSql, paramList );
    }

    /**
     * 根据主键更新一个Entity实例，等效于update.
     *
     * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entity   要更新的对象
     * @return Entity实例
     * 
     */
    public <T extends DataEntity> int update(String connName, T entity) {
        return daoFactory.update( connName, entity );
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param entityCls  要映射的对象类型
     * @param selectSql  查询的SQL
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @return DataList列表
     * 
     */
    public <T> DataList<T> list(Class<T> entityCls, String selectSql, int startIndex, int resultNum) {
        return daoFactory.list( entityCls, selectSql, startIndex, resultNum );
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
     * 
     */
    public DataSet queryForDataSet(String connName, String selectSql, int startIndex, int resultNum, boolean autoCount) {
        return daoFactory.queryForDataSet( connName, selectSql, startIndex, resultNum, autoCount );
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param connName
     * @param entityCls      要映射的对象类型
     * @param tableName      附加表名，在特定分表情况下。
     * @param pageQueryParam 分页查询对象
     * @return DataList列表
     * 
     */
    public <T> DataList<T> list(String connName, Class<T> entityCls, String tableName, PageQueryParam pageQueryParam) {
        return daoFactory.list( connName, entityCls, tableName, pageQueryParam );
    }

    /**
     * 查询单个基本数值（单个字段）.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param valueCls   要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param queryParam 查询参数
     * @return 单个对象
     * 
     */
    public <T> T queryForSingleValue(String connName, Class<T> valueCls, QueryParam queryParam) {
        return daoFactory.queryForSingleValue( connName, valueCls, queryParam );
    }

    /**
     * 查询单个基本数值（单个字段）.
     *
     * @param valueCls   要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param queryParam 查询参数
     * @return 单个对象
     * 
     */
    public <T> T queryForSingleValue(Class<T> valueCls, QueryParam queryParam) {
        return daoFactory.queryForSingleValue( valueCls, queryParam );
    }

    /**
     * 解析QueryParam信息。
     *
     * @param cls        要返回的对象。
     * @param queryParam 查询参数类。
     * @return
     * @throws TransactionException
     */
    public QueryParamResult parseQueryParam(Class cls, QueryParam queryParam) {
        return daoFactory.parseQueryParam( cls, queryParam );
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param entityCls 要映射的对象类型
     * @param selectSql 查询的SQL
     * @return DataList列表
     * 
     */
    public <T> DataList<T> list(Class<T> entityCls, String selectSql) {
        return daoFactory.list( entityCls, selectSql );
    }

    /**
     * 执行一条SQL语句.
     *
     * @param sql 查询的SQL
     * @return 影响的行数
     * 
     */
    public int executeCommand(String sql) {
        return daoFactory.executeCommand( sql );
    }

    /**
     * 执行一条SQL语句.
     *
     * @param sql       查询的SQL
     * @param paramList 查询SQL的参数
     * @return 影响的行数
     * 
     */
    public int executeCommand(String sql, Object[] paramList) {
        return daoFactory.executeCommand( sql, paramList );
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param queryParam 查询参数
     * @return DataSet数据列表
     * 
     */
    public DataSet queryForDataSet(QueryParam queryParam) {
        return daoFactory.queryForDataSet( queryParam );
    }

    /**
     * 根据表名和访问类型获得一个java.sql.Connection。 请注意，这是一个原生的Connection对象，需确保手工关闭.
     *
     * @param table  表名
     * @param access 访问类型。支持all/read/write
     * @return Connection对象
     * @throws SQLException SQL异常
     */
    public Connection getConnection(String table, String access) throws SQLException {
        return daoFactory.getConnection( table, access );
    }

    /**
     * 查询单个基本数值列表（多行单个字段）.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param valueCls  要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param sql       查询的SQL
     * @param paramList 查询SQL的参数
     * @return 单个对象
     * 
     */
    public <T> ArrayList<T> queryForSingleList(String connName, Class<T> valueCls, String sql, Object[] paramList) {
        return daoFactory.queryForSingleList( connName, valueCls, sql, paramList );
    }

    /**
     * 保存多个Entity实例，等效于insert values()().
     *
     * @param entityList 要保存的Entity实例集合
     * @param tableName  指定表名
     * @return Entity实例集合
     * 
     */
    public <T extends DataEntity> List<T> batchSave(List<T> entityList, String tableName) {
        return daoFactory.batchSave( entityList, tableName );
    }

    /**
     * 查询单个基本数值列表（多行单个字段）.
     *
     * @param valueCls   要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param queryParam 查询参数
     * @return DataSet数据列表
     * 
     */
    public <T> ArrayList<T> queryForSingleList(Class<T> valueCls, QueryParam queryParam) {
        return daoFactory.queryForSingleList( valueCls, queryParam );
    }

    /**
     * 查询单个对象（单行数据）.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls  要映射的对象类型
     * @param queryParam 查询参数
     * @return 单个对象
     * 
     */
    public <T> T queryForSingleObject(String connName, Class<T> entityCls, QueryParam queryParam) {
        return daoFactory.queryForSingleObject( connName, entityCls, queryParam );
    }

    /**
     * 查询单个基本数值列表（多行单个字段）.
     *
     * @param valueCls 要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param sql      查询的SQL
     * @return DataSet对象
     * 
     */
    public <T> ArrayList<T> queryForSingleList(Class<T> valueCls, String sql) {
        return daoFactory.queryForSingleList( valueCls, sql );
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param entityCls  要映射的对象类型
     * @param selectSql  查询的SQL
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @param autoCount  是否统计全部数据（用于分页算法），默认为false。
     * @return DataList列表
     * 
     */
    public <T> DataList<T> list(Class<T> entityCls, String selectSql, int startIndex, int resultNum, boolean autoCount) {
        return daoFactory.list( entityCls, selectSql, startIndex, resultNum, autoCount );
    }

    /**
     * 查询单个基本数值（单个字段）.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param cls       要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param sql       查询的SQL
     * @param paramList 查询SQL的参数
     * @return 单个对象
     * 
     */
    public <T> T queryForSingleValue(String connName, Class<T> cls, String sql, Object[] paramList) {
        return daoFactory.queryForSingleValue( connName, cls, sql, paramList );
    }

    /**
     * 保存一个Entity实例，等效于insert.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entity    要更新的对象
     * @param tableName 指定表名
     * @return Entity实例
     * 
     */
    public <T extends DataEntity> T save(String connName, T entity, String tableName) {
        return daoFactory.save( connName, entity, tableName );
    }

    /**
     * 查询单个对象（单行数据）。 使用sql中探测到的表名来决定连接名.
     *
     * @param entityCls 要映射的对象类型
     * @param selectSql 查询的SQL
     * @return 单个对象
     * 
     */
    public <T> T queryForSingleObject(Class<T> entityCls, String selectSql) {
        return daoFactory.queryForSingleObject( entityCls, selectSql );
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
     * @return DataList列表
     * 
     */
    public <T> DataList<T> list(String connName, Class<T> entityCls, String selectSql, int startIndex, int resultNum, boolean autoCount) {
        return daoFactory.list( connName, entityCls, selectSql, startIndex, resultNum, autoCount );
    }

    /**
     * 保存多个Entity实例，等效于insert values()().
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityList 要保存的Entity实例集合
     * @return Entity实例集合
     * 
     */
    public <T extends DataEntity> List<T> batchSave(String connName, List<T> entityList) {
        return daoFactory.batchSave( connName, entityList );
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param selectSql  查询的SQL
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @param autoCount  是否统计全部数据（用于分页算法），默认为false。
     * @return DataSet数据列表
     * 
     */
    public DataSet queryForDataSet(String selectSql, int startIndex, int resultNum, boolean autoCount) {
        return daoFactory.queryForDataSet( selectSql, startIndex, resultNum, autoCount );
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls 要映射的对象类型
     * @param selectSql 查询的SQL
     * @return DataList列表
     * 
     */
    public <T> DataList<T> list(String connName, Class<T> entityCls, String selectSql) {
        return daoFactory.list( connName, entityCls, selectSql );
    }

    /**
     * 获得当前DAOFactory实例下的sql执行统计列表.
     *
     * @return 统计列表
     */
    public ArrayList<SqlExecuteStats> getSqlExecuteStatsList() {
        return daoFactory.getSqlExecuteStatsList();
    }

    /**
     * 执行一条SQL语句.
     *
     * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param sql      查询的SQL
     * @return 影响的行数
     * 
     */
    public int executeCommand(String connName, String sql) {
        return daoFactory.executeCommand( connName, sql );
    }

    /**
     * 查询单个对象（单行数据）.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls  要映射的对象类型
     * @param tableName
     * @param queryParam 查询参数
     * @return 单个对象
     * 
     */
    public <T> T queryForSingleObject(String connName, Class<T> entityCls, String tableName, QueryParam queryParam) {
        return daoFactory.queryForSingleObject( connName, entityCls, tableName, queryParam );
    }

    /**
     * 保存一个Entity实例，等效于insert.
     *
     * @param entity    要更新的对象
     * @param tableName 指定表名
     * @return Entity实例
     * 
     */
    public <T extends DataEntity> T save(T entity, String tableName) {
        return daoFactory.save( entity, tableName );
    }
}
