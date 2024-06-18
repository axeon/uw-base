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
public abstract class DaoFactory {

    /**
     * 获取一个MainFactory实例, 此实例是线程安全的.
     *
     * @return DAOFactoryImpl对象
     */
    public static DaoFactory getInstance() {
        return new DaoFactoryImpl();
    }

    /**
     * 获取一个DAOFactory实例。 指定connName，这时候将不会使用dao来决定数据库联接.
     *
     * @return DAOFactoryImpl对象
     */
    public static DaoFactory getInstance(String connName) {
        return new DaoFactoryImpl(connName);
    }

    /**
     * 获得一个batchupdate handle.
     *
     * @return BatchupdateManager对象
     * @throws TransactionException 事务异常
     */
    public abstract BatchUpdateManager beginBatchUpdate();

    /**
     * 开始一个数据库事务.
     *
     * @return TransactionManager对象
     */
    public abstract TransactionManager beginTransaction();

    /**
     * 根据主键删除一个Entity实例，等效于delete.
     *
     * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entity   要更新的对象
     * @param <T>      映射的类型
     * @return int
     * @throws TransactionException 事务异常
     */
    public abstract <T extends DataEntity> int delete(String connName, T entity) throws TransactionException;

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
    public abstract <T extends DataEntity> int delete(String connName, T entity, String tableName)
            throws TransactionException;

    /**
     * 根据主键删除一个Entity实例，等效于delete.
     *
     * @param entity 要更新的对象
     * @param <T>    映射的类型
     * @return int
     * @throws TransactionException 事务异常
     */
    public abstract <T extends DataEntity> int delete(T entity) throws TransactionException;

    /**
     * 根据主键删除一个Entity实例，等效于delete.
     *
     * @param entity    要更新的对象
     * @param tableName 指定表名
     * @param <T>       映射的类型
     * @return int
     * @throws TransactionException 事务异常
     */
    public abstract <T extends DataEntity> int delete(T entity, String tableName) throws TransactionException;

    /**
     * 关闭sql执行统计，将会影响getSqlExecuteStatsList的数据.
     */
    public abstract void disableSqlExecuteStats();

    /**
     * 打开sql执行统计，将会影响getSqlExecuteStatsList的数据.
     */
    public abstract void enableSqlExecuteStats();

    /**
     * 执行一条SQL语句.
     *
     * @param sql 查询的SQL
     * @return 影响的行数
     * @throws TransactionException 事务异常
     */
    public abstract int executeCommand(String sql) throws TransactionException;

    /**
     * 执行一条SQL语句.
     *
     * @param sql       查询的SQL
     * @param paramList 查询SQL的参数
     * @return 影响的行数
     * @throws TransactionException 事务异常
     */
    public abstract int executeCommand(String sql, Object[] paramList) throws TransactionException;

    /**
     * 执行一条SQL语句.
     *
     * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param sql      查询的SQL
     * @return 影响的行数
     * @throws TransactionException 事务异常
     */
    public abstract int executeCommand(String connName, String sql) throws TransactionException;

    /**
     * 执行一条SQL语句.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param sql       查询的SQL
     * @param paramList 查询SQL的参数
     * @return 影响的行数
     * @throws TransactionException 事务异常
     */
    public abstract int executeCommand(String connName, String sql, Object[] paramList) throws TransactionException;

    /**
     * 获得一个java.sql.Connection连接。 请注意，这是一个原生的Connection对象，需确保手工关闭.
     *
     * @param configName 配置名
     * @return Connection对象
     * @throws SQLException SQL异常
     */
    public abstract Connection getConnection(String configName) throws SQLException;

    /**
     * 根据表名和访问类型获得一个java.sql.Connection。 请注意，这是一个原生的Connection对象，需确保手工关闭.
     *
     * @param table  表名
     * @param access 访问类型。支持all/read/write
     * @return Connection对象
     * @throws SQLException SQL异常
     */
    public abstract Connection getConnection(String table, String access) throws SQLException;

    /**
     * 根据表名和访问类型获得一个数据库连接配置名.
     *
     * @param table  表名
     * @param access 访问类型。支持all/read/write
     * @return 数据库连接配置名
     */
    public abstract String getConnectionName(String table, String access);

    /**
     * 获得当前DAOFactory实例下sql执行次数.
     *
     * @return sql执行次数
     */
    public abstract int getInvokeCount();

    /**
     * 根据Entity来获得seq序列。 此序列通过一个系统数据库来维护，可以保证在分布式下的可用性.
     *
     * @param entityCls 实体类类型
     * @return seq序列
     */
    public abstract long getSequenceId(Class<?> entityCls);

    /**
     * 根据表名来获得seq序列。 此序列通过一个系统数据库来维护，可以保证在分布式下的可用性.
     *
     * @param seqName 表名
     * @return seq序列
     */
    public abstract long getSequenceId(String seqName);

    /**
     * 获得当前DAOFactory实例下的sql执行统计列表.
     *
     * @return 统计列表
     */
    public abstract ArrayList<SqlExecuteStats> getSqlExecuteStatsList();

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param entityCls 要映射的对象类型
     * @param selectSql 查询的SQL
     * @param <T>       映射的类型
     * @return DataList列表
     * @throws TransactionException 事务异常
     */
    public abstract <T> DataList<T> list(Class<T> entityCls, String selectSql) throws TransactionException;


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
    public abstract <T> DataList<T> list(Class<T> entityCls, String selectSql, int startIndex, int resultNum)
            throws TransactionException;

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
    public abstract <T> DataList<T> list(Class<T> entityCls, String selectSql, int startIndex, int resultNum,
                                         boolean autoCount) throws TransactionException;

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
    public abstract <T> DataList<T> list(Class<T> entityCls, String selectSql, Object[] paramList)
            throws TransactionException;

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
    public abstract <T> DataList<T> list(Class<T> entityCls, String selectSql, Object[] paramList, int startIndex,
                                         int resultNum) throws TransactionException;

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
    public abstract <T> DataList<T> list(Class<T> entityCls, String selectSql, Object[] paramList, int startIndex,
                                         int resultNum, boolean autoCount) throws TransactionException;

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
    public abstract <T> DataList<T> list(String connName, Class<T> entityCls, String selectSql) throws TransactionException;

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
    public abstract <T> DataList<T> list(String connName, Class<T> entityCls, String selectSql, int startIndex, int resultNum)
            throws TransactionException;

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
    public abstract <T> DataList<T> list(String connName, Class<T> entityCls, String selectSql, int startIndex, int resultNum,
                                         boolean autoCount) throws TransactionException;

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
    public abstract <T> DataList<T> list(String connName, Class<T> entityCls, String selectSql, Object[] paramList)
            throws TransactionException;

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
    public abstract <T> DataList<T> list(String connName, Class<T> entityCls, String selectSql, Object[] paramList,
                                         int startIndex, int resultNum) throws TransactionException;

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
    public abstract <T> DataList<T> list(String connName, Class<T> entityCls, String selectSql, Object[] paramList,
                                         int startIndex, int resultNum, boolean autoCount) throws TransactionException;


    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param entityCls      要映射的对象类型
     * @param pageQueryParam 分页查询对象
     * @param <T>            映射的类型
     * @return DataList列表
     * @throws TransactionException 事务异常
     */
    public abstract <T> DataList<T> list(String connName, Class<T> entityCls, PageQueryParam pageQueryParam) throws TransactionException;


    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param entityCls      要映射的对象类型
     * @param pageQueryParam 分页查询对象
     * @param <T>            映射的类型
     * @return DataList列表
     * @throws TransactionException 事务异常
     */
    public abstract <T> DataList<T> list(Class<T> entityCls, PageQueryParam pageQueryParam) throws TransactionException;

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
    public abstract <T> DataList<T> list(String connName, Class<T> entityCls, String tableName, PageQueryParam pageQueryParam) throws TransactionException;

    /**
     * 根据指定的主键ID载入一个Entity实例.
     *
     * @param entityCls 要映射的对象类型
     * @param <T>       映射的类型
     * @param id        主键数值
     * @return DataList对象
     * @throws TransactionException 事务异常
     */
    public abstract <T> T load(Class<T> entityCls, Serializable id) throws TransactionException;

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
    public abstract <T> T load(Class<T> entityCls, String tableName, Serializable id) throws TransactionException;

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
    public abstract <T> T load(String connName, Class<T> entityCls, Serializable id) throws TransactionException;

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
    public abstract <T> T load(String connName, Class<T> entityCls, String tableName, Serializable id)
            throws TransactionException;

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param queryParam 查询参数
     * @return DataSet数据列表
     * @throws TransactionException 事务异常
     */
    public abstract DataSet queryForDataSet(String connName, QueryParam queryParam) throws TransactionException;

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param queryParam 查询参数
     * @return DataSet数据列表
     * @throws TransactionException 事务异常
     */
    public abstract DataSet queryForDataSet(QueryParam queryParam) throws TransactionException;

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param selectSql 查询的SQL
     * @return DataSet数据列表
     * @throws TransactionException 事务异常
     */
    public abstract DataSet queryForDataSet(String selectSql) throws TransactionException;

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param selectSql  查询的SQL
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @return DataSet数据列表
     * @throws TransactionException 事务异常
     */
    public abstract DataSet queryForDataSet(String selectSql, int startIndex, int resultNum)
            throws TransactionException;

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
    public abstract DataSet queryForDataSet(String selectSql, int startIndex, int resultNum, boolean autoCount)
            throws TransactionException;

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param selectSql 查询的SQL
     * @param paramList 查询SQL的绑定参数
     * @return DataSet数据列表
     * @throws TransactionException 事务异常
     */
    public abstract DataSet queryForDataSet(String selectSql, Object[] paramList) throws TransactionException;

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
    public abstract DataSet queryForDataSet(String selectSql, Object[] paramList, int startIndex, int resultNum)
            throws TransactionException;

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
    public abstract DataSet queryForDataSet(String selectSql, Object[] paramList, int startIndex, int resultNum,
                                            boolean autoCount) throws TransactionException;

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param connName  连接名，当设置为null时候，根据sql语句或表名确定
     * @param selectSql 查询的SQL
     * @return DataSet数据列表
     * @throws TransactionException 事务异常
     */
    public abstract DataSet queryForDataSet(String connName, String selectSql) throws TransactionException;

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
    public abstract DataSet queryForDataSet(String connName, String selectSql, int startIndex, int resultNum)
            throws TransactionException;

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param selectsql  查询的SQL
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @param autoCount  是否统计全部数据（用于分页算法），默认为false。
     * @return DataSet数据列表
     * @throws TransactionException 事务异常
     */
    public abstract DataSet queryForDataSet(String connName, String selectsql, int startIndex, int resultNum,
                                            boolean autoCount) throws TransactionException;

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param selectsql 查询的SQL
     * @param paramList 查询SQL的绑定参数
     * @return DataSet数据列表
     * @throws TransactionException 事务异常
     */
    public abstract DataSet queryForDataSet(String connName, String selectsql, Object[] paramList)
            throws TransactionException;

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param selectsql  查询的SQL
     * @param paramList  查询SQL的绑定参数
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @return DataSet数据列表
     * @throws TransactionException 事务异常
     */
    public abstract DataSet queryForDataSet(String connName, String selectsql, Object[] paramList, int startIndex,
                                            int resultNum) throws TransactionException;

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param selectsql  查询的SQL
     * @param paramList  查询SQL的绑定参数
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @param autoCount  是否统计全部数据（用于分页算法），默认为false。
     * @return DataSet数据列表
     * @throws TransactionException 事务异常
     */
    public abstract DataSet queryForDataSet(String connName, String selectsql, Object[] paramList, int startIndex,
                                            int resultNum, boolean autoCount) throws TransactionException;

    /**
     * 查询单个基本数值列表（多行单个字段）.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param valueCls        要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param queryParam 查询参数
     * @return DataSet数据列表
     * @throws TransactionException 事务异常
     */
    public abstract <T> ArrayList<T> queryForSingleList(String connName, Class<T> valueCls, QueryParam queryParam) throws TransactionException;

    /**
     * 查询单个基本数值列表（多行单个字段）.
     *
     * @param valueCls        要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param queryParam 查询参数
     * @return DataSet数据列表
     * @throws TransactionException 事务异常
     */
    public abstract <T> ArrayList<T> queryForSingleList(Class<T> valueCls, QueryParam queryParam) throws TransactionException;

    /**
     * 查询单个基本数值列表（多行单个字段）.
     *
     * @param valueCls 要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param <T> 映射的类型
     * @param sql 查询的SQL
     * @return DataSet对象
     * @throws TransactionException 事务异常
     */
    public abstract <T> ArrayList<T> queryForSingleList(Class<T> valueCls, String sql) throws TransactionException;

    /**
     * 查询单个基本数值列表（多行单个字段）.
     *
     * @param valueCls       要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param <T>       映射的类型
     * @param sql       查询的SQL
     * @param paramList 查询SQL的参数
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    public abstract <T> ArrayList<T> queryForSingleList(Class<T> valueCls, String sql, Object[] paramList)
            throws TransactionException;

    /**
     * 查询单个基本数值列表（多行单个字段）.
     *
     * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param valueCls      要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param <T>      映射的类型
     * @param sql      查询的SQL
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    public abstract <T> ArrayList<T> queryForSingleList(String connName, Class<T> valueCls, String sql)
            throws TransactionException;

    /**
     * 查询单个基本数值列表（多行单个字段）.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param valueCls       要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param <T>       映射的类型
     * @param sql       查询的SQL
     * @param paramList 查询SQL的参数
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    public abstract <T> ArrayList<T> queryForSingleList(String connName, Class<T> valueCls, String sql, Object[] paramList)
            throws TransactionException;

    /**
     * 查询单个对象（单行数据）。 使用sql中探测到的表名来决定连接名.
     *
     * @param entityCls 要映射的对象类型
     * @param <T>       映射的类型
     * @param selectsql 查询的SQL
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    public abstract <T> T queryForSingleObject(Class<T> entityCls, String selectsql) throws TransactionException;

    /**
     * 查询单个对象（单行数据）。 使用sql中探测到的表名来决定连接名.
     *
     * @param entityCls 要映射的对象类型
     * @param <T>       映射的类型
     * @param selectsql 查询的SQL
     * @param paramList 查询SQL的参数
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    public abstract <T> T queryForSingleObject(Class<T> entityCls, String selectsql, Object[] paramList)
            throws TransactionException;

    /**
     * 查询单个对象（单行数据）.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls 要映射的对象类型
     * @param <T>       映射的类型
     * @param selectsql 查询的SQL
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    public abstract <T> T queryForSingleObject(String connName, Class<T> entityCls, String selectsql)
            throws TransactionException;

    /**
     * 查询单个对象（单行数据）.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls 要映射的对象类型
     * @param <T>       映射的类型
     * @param selectsql 查询的SQL
     * @param paramList 查询SQL的参数
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    public abstract <T> T queryForSingleObject(String connName, Class<T> entityCls, String selectsql, Object[] paramList)
            throws TransactionException;

    /**
     * 查询单个对象（单行数据）.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls  要映射的对象类型
     * @param queryParam 查询参数
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    public abstract <T> T queryForSingleObject(String connName, Class<T> entityCls, String tableName, QueryParam queryParam) throws TransactionException;

    /**
     * 查询单个对象（单行数据）.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls  要映射的对象类型
     * @param <T>        映射的类型
     * @param queryParam 查询参数
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    public abstract <T> T queryForSingleObject(String connName, Class<T> entityCls, QueryParam queryParam)
            throws TransactionException;


    /**
     * 查询单个对象（单行数据）.
     *
     * @param entityCls  要映射的对象类型
     * @param <T>        映射的类型
     * @param queryParam 查询参数
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    public abstract <T> T queryForSingleObject(Class<T> entityCls, QueryParam queryParam)
            throws TransactionException;

    /**
     * 查询单个基本数值（单个字段）.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param valueCls   要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param queryParam 查询参数
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    public abstract <T> T queryForSingleValue(String connName, Class<T> valueCls, QueryParam queryParam) throws TransactionException;

    /**
     * 查询单个基本数值（单个字段）.
     *
     * @param valueCls   要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param queryParam 查询参数
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    public abstract <T> T queryForSingleValue(Class<T> valueCls, QueryParam queryParam) throws TransactionException;

    /**
     * 查询单个基本数值（单个字段）.
     *
     * @param cls 要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param <T> 映射的类型
     * @param sql 查询的SQL
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    public abstract <T> T queryForSingleValue(Class<T> cls, String sql) throws TransactionException;

    /**
     * 查询单个基本数值（单个字段）.
     *
     * @param cls       要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param <T>       映射的类型
     * @param sql       查询的SQL
     * @param paramList 查询SQL的参数
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    public abstract <T> T queryForSingleValue(Class<T> cls, String sql, Object[] paramList) throws TransactionException;

    /**
     * 查询单个基本数值（单个字段）.
     *
     * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param cls      要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param <T>      映射的类型
     * @param sql      查询的SQL
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    public abstract <T> T queryForSingleValue(String connName, Class<T> cls, String sql) throws TransactionException;

    /**
     * 查询单个基本数值（单个字段）.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param cls       要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param <T>       映射的类型
     * @param sql       查询的SQL
     * @param paramList 查询SQL的参数
     * @return 单个对象
     * @throws TransactionException 事务异常
     */
    public abstract <T> T queryForSingleValue(String connName, Class<T> cls, String sql, Object[] paramList)
            throws TransactionException;


    /**
     * 保存一个Entity实例，等效于insert.
     *
     * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param <T>      映射的类型
     * @param entity   要更新的对象
     * @return Entity实例
     * @throws TransactionException 事务异常
     */
    public abstract <T extends DataEntity> T save(String connName, T entity) throws TransactionException;

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
    public abstract <T extends DataEntity> T save(String connName, T entity, String tableName)
            throws TransactionException;

    /**
     * 保存一个Entity实例，等效于insert.
     *
     * @param <T>    映射的类型
     * @param entity 要更新的对象
     * @return Entity实例
     * @throws TransactionException 事务异常
     */
    public abstract <T extends DataEntity> T save(T entity) throws TransactionException;

    /**
     * 保存一个Entity实例，等效于insert.
     *
     * @param <T>       映射的类型
     * @param entity    要更新的对象
     * @param tableName 指定表名
     * @return Entity实例
     * @throws TransactionException 事务异常
     */
    public abstract <T extends DataEntity> T save(T entity, String tableName) throws TransactionException;

    /**
     * 保存多个Entity实例，等效于insert values()().
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityList 要保存的Entity实例集合
     * @param <T>        映射的类型
     * @return Entity实例集合
     * @throws TransactionException 事务异常
     */
    public abstract <T extends DataEntity> List<T> batchSave(String connName, List<T> entityList) throws TransactionException;

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
    public abstract <T extends DataEntity> List<T> batchSave(String connName, List<T> entityList, String tableName)
            throws TransactionException;

    /**
     * 保存多个Entity实例，等效于insert values()().
     *
     * @param <T>        映射的类型
     * @param entityList 要保存的Entity实例集合
     * @return Entity实例集合
     * @throws TransactionException 事务异常
     */
    public abstract <T extends DataEntity> List<T> batchSave(List<T> entityList) throws TransactionException;

    /**
     * 保存多个Entity实例，等效于insert values()().
     *
     * @param <T>        映射的类型
     * @param entityList 要保存的Entity实例集合
     * @param tableName  指定表名
     * @return Entity实例集合
     * @throws TransactionException 事务异常
     */
    public abstract <T extends DataEntity> List<T> batchSave(List<T> entityList, String tableName) throws TransactionException;

    /**
     * 根据主键更新一个Entity实例，等效于update.
     *
     * @param <T>      映射的类型
     * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entity   要更新的对象
     * @return Entity实例
     * @throws TransactionException 事务异常
     */
    public abstract <T extends DataEntity> int update(String connName, T entity) throws TransactionException;

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
    public abstract <T extends DataEntity> int update(String connName, T entity, String tableName)
            throws TransactionException;

    /**
     * 根据主键更新一个Entity实例，等效于update.
     *
     * @param <T>    映射的类型
     * @param entity 要更新的对象
     * @return Entity实例
     * @throws TransactionException 事务异常
     */
    public abstract <T extends DataEntity> int update(T entity) throws TransactionException;

    /**
     * 根据主键更新一个Entity实例，等效于update.
     *
     * @param <T>       映射的类型
     * @param entity    要更新的对象
     * @param tableName 指定表名
     * @return Entity实例
     * @throws TransactionException 事务异常
     */
    public abstract <T extends DataEntity> int update(T entity, String tableName) throws TransactionException;

    /**
     * 解析QueryParam信息。
     *
     * @param cls        要返回的对象。
     * @param tableName  附加表名，用于特殊的分表场景。
     * @param queryParam 查询参数类。
     * @return
     * @throws TransactionException
     */
    public abstract QueryParamResult parseQueryParam(Class cls, String tableName, QueryParam queryParam) throws TransactionException;

    /**
     * 解析QueryParam信息。
     *
     * @param cls        要返回的对象。
     * @param queryParam 查询参数类。
     * @return
     * @throws TransactionException
     */
    public abstract QueryParamResult parseQueryParam(Class cls, QueryParam queryParam) throws TransactionException;

    /**
     * 解析QueryParam信息。
     *
     * @param queryParam 查询参数类。
     * @return
     * @throws TransactionException
     */
    public abstract QueryParamResult parseQueryParam(QueryParam queryParam) throws TransactionException;
}
