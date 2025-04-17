package uw.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.dto.ResponseData;
import uw.dao.conf.DaoConfigManager;
import uw.dao.constant.DaoResponseCode;
import uw.dao.vo.QueryParamResult;
import uw.dao.vo.SqlExecuteStats;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DaoManager是DAO的入口类，提供了一系列的DAO操作方法.
 * DaoManager是DaoFactory的代理，和DaoFactory区别在于使用ResponseData返回信息。
 * 这大大简化了代码调用。
 *
 * @author axeon
 */
public class DaoManager {

    /**
     * logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(DaoManager.class);

    /**
     * DaoFactory实例.
     */
    private final DaoFactory daoFactory;

    /**
     * 构造函数.
     *
     * @param daoFactory
     */
    private DaoManager(DaoFactory daoFactory) {
        this.daoFactory = daoFactory;
    }

    /**
     * 获取一个DaoManager实例, 此实例是线程安全的.
     *
     * @return DaoManager
     */
    public static DaoManager getInstance() {
        return new DaoManager(DaoFactory.getInstance());
    }

    /**
     * 获取一个DaoFactory实例.
     *
     * @return
     */
    public DaoFactory getDaoFactory() {
        return daoFactory;
    }

    /**
     * 获取一个java.sql.Connection连接。 请注意，这是一个原生的Connection对象，需确保手工关闭.
     *
     * @param configName 配置名
     * @return Connection对象
     * @throws SQLException SQL异常
     */
    public Connection getConnection(String configName) throws SQLException {
        return daoFactory.getConnection(configName);
    }

    /**
     * 根据表名和访问类型获取一个java.sql.Connection。 请注意，这是一个原生的Connection对象，需确保手工关闭.
     *
     * @param table  表名
     * @param access 访问类型。支持all/read/write
     * @return Connection对象
     * @throws SQLException SQL异常
     */
    public Connection getConnection(String table, String access) throws SQLException {
        return daoFactory.getConnection(table, access);
    }

    /**
     * 根据Entity来获取seq序列。 此序列通过一个系统数据库来维护，可以保证在分布式下的可用性.
     *
     * @param entityCls 实体类类型
     * @return seq序列
     */
    public long getSequenceId(Class<?> entityCls) {
        return daoFactory.getSequenceId(entityCls);
    }

    /**
     * 根据表名来获取seq序列。 此序列通过一个系统数据库来维护，可以保证在分布式下的可用性.
     *
     * @param seqName 表名
     * @return seq序列
     */
    public long getSequenceId(String seqName) {
        return daoFactory.getSequenceId(seqName);
    }

    /**
     * 根据表名和访问类型获取一个数据库连接配置名.
     *
     * @param table  表名
     * @param access 访问类型。支持all/read/write
     * @return 数据库连接配置名
     */
    public String getConnectionName(String table, String access) {
        return daoFactory.getConnectionName(table, access);
    }

    /**
     * 获取当前DaoManager实例下sql执行次数.
     *
     * @return sql执行次数
     */
    public Integer getInvokeCount() {
        return daoFactory.getInvokeCount();
    }

    /**
     * 获取当前DaoManager实例下的sql执行统计列表.
     *
     * @return 统计列表
     */
    public ArrayList<SqlExecuteStats> getSqlExecuteStatsList() {
        return daoFactory.getSqlExecuteStatsList();
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


    // save相关方法

    /**
     * 保存一个Entity实例，等效于insert.
     *
     * @param entity 要更新的对象
     * @return Entity实例
     */
    public <T extends DataEntity> ResponseData<T> save(T entity) {
        try {
            return responseData(daoFactory.save(entity));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 保存一个Entity实例，等效于insert.
     *
     * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entity   要更新的对象
     * @return Entity实例
     */
    public <T extends DataEntity> ResponseData<T> save(String connName, T entity) {
        try {
            return responseData(daoFactory.save(connName, entity));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 保存一个Entity实例，等效于insert.
     *
     * @param entity    要更新的对象
     * @param tableName 指定表名
     * @return Entity实例
     */
    public <T extends DataEntity> ResponseData<T> save(T entity, String tableName) {
        try {
            return responseData(daoFactory.save(entity, tableName));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 保存一个Entity实例，等效于insert.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entity    要更新的对象
     * @param tableName 指定表名
     * @return Entity实例
     */
    public <T extends DataEntity> ResponseData<T> save(String connName, T entity, String tableName) {
        try {
            return responseData(daoFactory.save(connName, entity, tableName));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 保存多个Entity实例，等效于insert values()().
     *
     * @param entityList 要保存的Entity实例集合
     * @return Entity实例集合
     */
    public <T extends DataEntity> ResponseData<List<T>> save(List<T> entityList) {
        try {
            return responseData(daoFactory.save(entityList));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 保存多个Entity实例，等效于insert values()().
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityList 要保存的Entity实例集合
     * @return Entity实例集合
     */
    public <T extends DataEntity> ResponseData<List<T>> save(String connName, List<T> entityList) {
        try {
            return responseData(daoFactory.save(connName, entityList));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 保存多个Entity实例，等效于insert values()().
     *
     * @param entityList 要保存的Entity实例集合
     * @param tableName  指定表名
     * @return Entity实例集合
     */
    public <T extends DataEntity> ResponseData<List<T>> save(List<T> entityList, String tableName) {
        try {
            return responseData(daoFactory.save(entityList, tableName));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 保存多个Entity实例，等效于insert values()().
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityList 要保存的Entity实例集合
     * @param tableName  指定表名
     * @return Entity实例集合
     */
    public <T extends DataEntity> ResponseData<List<T>> save(String connName, List<T> entityList, String tableName) {
        try {
            return responseData(daoFactory.save(connName, entityList, tableName));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    // update相关方法

    /**
     * 根据主键更新一个Entity实例，等效于update.
     *
     * @param entity 要更新的对象
     * @return Entity实例
     */
    public <T extends DataEntity> ResponseData<T> update(T entity) {
        try {
            int effectedNum = daoFactory.update(entity);
            if (effectedNum < 1) {
                return responseData(null);
            } else {
                return responseData(entity);
            }
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 根据主键更新一个Entity实例，等效于update.
     *
     * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entity   要更新的对象
     * @return Entity实例
     */
    public <T extends DataEntity> ResponseData<T> update(String connName, T entity) {
        try {
            int effectedNum = daoFactory.update(connName, entity);
            if (effectedNum < 1) {
                return responseData(null);
            } else {
                return responseData(entity);
            }
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 根据主键更新一个Entity实例，等效于update.
     *
     * @param entity    要更新的对象
     * @param tableName 指定表名
     * @return Entity实例
     */
    public <T extends DataEntity> ResponseData<T> update(T entity, String tableName) {
        try {
            int effectedNum = daoFactory.update(entity, tableName);
            if (effectedNum < 1) {
                return responseData(null);
            } else {
                return responseData(entity);
            }
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 根据主键更新一个Entity实例，等效于update.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entity    要更新的对象
     * @param tableName 指定表名
     * @return Entity实例
     */
    public <T extends DataEntity> ResponseData<T> update(String connName, T entity, String tableName) {
        try {
            int effectedNum = daoFactory.update(connName, entity, tableName);
            if (effectedNum < 1) {
                return responseData(null);
            } else {
                return responseData(entity);
            }
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 根据QueryParam更新一个Entity实例，等效于update.
     *
     * @param entity
     * @param queryParam
     * @return
     */
    public <T extends DataEntity> ResponseData<Integer> update(T entity, QueryParam queryParam) {
        try {
            int effectedNum = daoFactory.update(entity, queryParam);
            if (effectedNum < 1) {
                return responseData(null);
            } else {
                return responseData(effectedNum);
            }
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 根据QueryParam更新一个Entity实例，等效于update.
     *
     * @param entity     要更新的对象
     * @param queryParam 查询条件
     * @return int
     */
    public <T extends DataEntity> ResponseData<Integer> update(String connName, T entity, QueryParam queryParam) {
        try {
            int effectedNum = daoFactory.update(connName, entity, queryParam);
            if (effectedNum < 1) {
                return responseData(null);
            } else {
                return responseData(effectedNum);
            }
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 根据QueryParam更新一个Entity实例，等效于update.
     *
     * @param connName
     * @param entity
     * @param tableName
     * @param queryParam
     * @return
     */
    public <T extends DataEntity> ResponseData<Integer> update(String connName, T entity, String tableName, QueryParam queryParam) {
        try {
            int effectedNum = daoFactory.update(connName, entity, tableName, queryParam);
            if (effectedNum < 1) {
                return responseData(null);
            } else {
                return responseData(effectedNum);
            }
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    // delete相关方法

    /**
     * 根据主键删除一个Entity实例，等效于delete.
     *
     * @param entity 要更新的对象
     * @return int
     */
    public <T extends DataEntity> ResponseData<Integer> delete(T entity) {
        try {
            int effectedNum = daoFactory.delete(entity);
            if (effectedNum < 1) {
                return responseData(null);
            } else {
                return responseData(effectedNum);
            }
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 根据主键删除一个Entity实例，等效于delete.
     *
     * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entity   要更新的对象
     * @return int
     */
    public <T extends DataEntity> ResponseData<Integer> delete(String connName, T entity) {
        try {
            int effectedNum = daoFactory.delete(connName, entity);
            if (effectedNum < 1) {
                return responseData(null);
            } else {
                return responseData(effectedNum);
            }
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 根据主键删除一个Entity实例，等效于delete.
     *
     * @param entity    要更新的对象
     * @param tableName 指定表名
     * @return int
     */
    public <T extends DataEntity> ResponseData<Integer> delete(T entity, String tableName) {
        try {
            int effectedNum = daoFactory.delete(entity, tableName);
            if (effectedNum < 1) {
                return responseData(null);
            } else {
                return responseData(effectedNum);
            }
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 根据主键删除一个Entity实例，等效于delete.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entity    要更新的对象
     * @param tableName 指定表名
     * @return int
     */
    public <T extends DataEntity> ResponseData<Integer> delete(String connName, T entity, String tableName) {
        try {
            int effectedNum = daoFactory.delete(connName, entity, tableName);
            if (effectedNum < 1) {
                return responseData(null);
            } else {
                return responseData(effectedNum);
            }
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 根据QueryParam删除信息，等效于delete.
     *
     * @param entityCls
     * @param queryParam
     * @return
     */
    public <T extends DataEntity> ResponseData<Integer> delete(Class<T> entityCls, QueryParam queryParam) {
        try {
            int effectedNum = daoFactory.delete(entityCls, queryParam);
            if (effectedNum < 1) {
                return responseData(null);
            } else {
                return responseData(effectedNum);
            }
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 根据QueryParam删除信息，等效于delete.
     *
     * @param connName
     * @param entityCls
     * @param queryParam
     * @return
     */
    public <T extends DataEntity> ResponseData<Integer> delete(String connName, Class<T> entityCls, QueryParam queryParam) {
        try {
            int effectedNum = daoFactory.delete(connName, entityCls, queryParam);
            if (effectedNum < 1) {
                return responseData(null);
            } else {
                return responseData(effectedNum);
            }
        } catch (TransactionException e) {
            return responseError(e);
        }
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
    public <T extends DataEntity> ResponseData<Integer> delete(String connName, Class<T> entityCls, String tableName, QueryParam queryParam) {
        try {
            int effectedNum = daoFactory.delete(connName, entityCls, tableName, queryParam);
            if (effectedNum < 1) {
                return responseData(null);
            } else {
                return responseData(effectedNum);
            }
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    // load相关方法

    /**
     * 根据指定的主键ID载入一个Entity实例.
     *
     * @param entityCls 要映射的对象类型
     * @param id        主键数值
     * @return DataList对象
     */
    public <T> ResponseData<T> load(Class<T> entityCls, Serializable id) {
        try {
            return responseData(daoFactory.load(entityCls, id));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 根据指定的主键ID载入一个Entity实例.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls 要映射的对象类型
     * @param id        主键数值
     * @return DataList对象
     */
    public <T> ResponseData<T> load(String connName, Class<T> entityCls, Serializable id) {
        try {
            return responseData(daoFactory.load(connName, entityCls, id));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 根据指定的主键ID载入一个Entity实例.
     *
     * @param entityCls 要映射的对象类型
     * @param tableName 指定表名
     * @param id        主键数值
     * @return DataList对象
     */
    public <T> ResponseData<T> load(Class<T> entityCls, String tableName, Serializable id) {
        try {
            return responseData(daoFactory.load(entityCls, tableName, id));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 根据指定的主键ID载入一个Entity实例.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls 要映射的对象类型
     * @param tableName 指定表名
     * @param id        主键数值
     * @return DataList对象
     */
    public <T> ResponseData<T> load(String connName, Class<T> entityCls, String tableName, Serializable id) {
        try {
            return responseData(daoFactory.load(connName, entityCls, tableName, id));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    // list相关方法

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param entityCls 要映射的对象类型
     * @param selectSql 查询的SQL
     * @return DataList列表
     */
    public <T> ResponseData<DataList<T>> list(Class<T> entityCls, String selectSql) {
        try {
            return responseData(daoFactory.list(entityCls, selectSql));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param entityCls  要映射的对象类型
     * @param selectSql  查询的SQL
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @return DataList列表
     */
    public <T> ResponseData<DataList<T>> list(Class<T> entityCls, String selectSql, int startIndex, int resultNum) {
        try {
            return responseData(daoFactory.list(entityCls, selectSql, startIndex, resultNum));
        } catch (TransactionException e) {
            return responseError(e);
        }
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
     * @throws TransactionException 事务异常
     */
    public <T> ResponseData<DataList<T>> list(Class<T> entityCls, String selectSql, int startIndex, int resultNum, boolean autoCount) {
        try {
            return responseData(daoFactory.list(entityCls, selectSql, startIndex, resultNum, autoCount));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls 要映射的对象类型
     * @param selectSql 查询的SQL
     * @return DataList列表
     */
    public <T> ResponseData<DataList<T>> list(String connName, Class<T> entityCls, String selectSql) {
        try {
            return responseData(daoFactory.list(connName, entityCls, selectSql));
        } catch (TransactionException e) {
            return responseError(e);
        }
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
     */
    public <T> ResponseData<DataList<T>> list(String connName, Class<T> entityCls, String selectSql, int startIndex, int resultNum) {
        try {
            return responseData(daoFactory.list(connName, entityCls, selectSql, startIndex, resultNum));
        } catch (TransactionException e) {
            return responseError(e);
        }
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
     * @throws TransactionException 事务异常
     */
    public <T> ResponseData<DataList<T>> list(String connName, Class<T> entityCls, String selectSql, int startIndex, int resultNum, boolean autoCount) {
        try {
            return responseData(daoFactory.list(connName, entityCls, selectSql, startIndex, resultNum, autoCount));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param entityCls 要映射的对象类型
     * @param selectSql 查询的SQL
     * @param paramList 查询SQL的绑定参数
     * @return DataList列表
     */
    public <T> ResponseData<DataList<T>> list(Class<T> entityCls, String selectSql, Object[] paramList) {
        try {
            return responseData(daoFactory.list(entityCls, selectSql, paramList));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls 要映射的对象类型
     * @param selectSql 查询的SQL
     * @param paramList 查询SQL的绑定参数
     * @return DataList列表
     */
    public <T> ResponseData<DataList<T>> list(String connName, Class<T> entityCls, String selectSql, Object[] paramList) {
        try {
            return responseData(daoFactory.list(connName, entityCls, selectSql, paramList));
        } catch (TransactionException e) {
            return responseError(e);
        }
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
     */
    public <T> ResponseData<DataList<T>> list(Class<T> entityCls, String selectSql, Object[] paramList, int startIndex, int resultNum) {
        try {
            return responseData(daoFactory.list(entityCls, selectSql, paramList, startIndex, resultNum));
        } catch (TransactionException e) {
            return responseError(e);
        }
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
     */
    public <T> ResponseData<DataList<T>> list(String connName, Class<T> entityCls, String selectSql, Object[] paramList, int startIndex, int resultNum) {
        try {
            return responseData(daoFactory.list(connName, entityCls, selectSql, paramList, startIndex, resultNum));
        } catch (TransactionException e) {
            return responseError(e);
        }
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
     */
    public <T> ResponseData<DataList<T>> list(Class<T> entityCls, String selectSql, Object[] paramList, int startIndex, int resultNum, boolean autoCount) {
        try {
            return responseData(daoFactory.list(entityCls, selectSql, paramList, startIndex, resultNum, autoCount));
        } catch (TransactionException e) {
            return responseError(e);
        }
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
     */
    public <T> ResponseData<DataList<T>> list(String connName, Class<T> entityCls, String selectSql, Object[] paramList, int startIndex, int resultNum, boolean autoCount) {
        try {
            return responseData(daoFactory.list(connName, entityCls, selectSql, paramList, startIndex, resultNum, autoCount));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param entityCls      要映射的对象类型
     * @param pageQueryParam 分页查询对象
     * @return DataList列表
     */
    public <T> ResponseData<DataList<T>> list(Class<T> entityCls, PageQueryParam pageQueryParam) {
        try {
            return responseData(daoFactory.list(entityCls, pageQueryParam));
        } catch (TransactionException e) {
            return responseError(e);
        }
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
    public <T> ResponseData<DataList<T>> list(Class<T> entityCls, String tableName, PageQueryParam pageQueryParam) {
        try {
            return responseData(daoFactory.list(entityCls, tableName, pageQueryParam));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param connName
     * @param entityCls      要映射的对象类型
     * @param pageQueryParam 分页查询对象
     * @return DataList列表
     */
    public <T> ResponseData<DataList<T>> list(String connName, Class<T> entityCls, PageQueryParam pageQueryParam) {
        try {
            return responseData(daoFactory.list(connName, entityCls, pageQueryParam));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 根据指定的映射类型，返回一个DataList列表.
     *
     * @param connName
     * @param entityCls      要映射的对象类型
     * @param tableName      附加表名，在特定分表情况下。
     * @param pageQueryParam 分页查询对象
     * @return DataList列表
     */
    public <T> ResponseData<DataList<T>> list(String connName, Class<T> entityCls, String tableName, PageQueryParam pageQueryParam) {
        try {
            return responseData(daoFactory.list(connName, entityCls, tableName, pageQueryParam));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    // query相关方法

    /**
     * 查询单个基本数值（单个字段）.
     *
     * @param cls 要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param sql 查询的SQL
     * @return 单个对象
     */
    public <T> ResponseData<T> queryForSingleValue(Class<T> cls, String sql) {
        try {
            return responseData(daoFactory.queryForSingleValue(cls, sql));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 查询单个基本数值（单个字段）.
     *
     * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param cls      要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param sql      查询的SQL
     * @return 单个对象
     */
    public <T> ResponseData<T> queryForSingleValue(String connName, Class<T> cls, String sql) {
        try {
            return responseData(daoFactory.queryForSingleValue(connName, cls, sql));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 查询单个基本数值（单个字段）.
     *
     * @param cls       要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param sql       查询的SQL
     * @param paramList 查询SQL的参数
     * @return 单个对象
     */
    public <T> ResponseData<T> queryForSingleValue(Class<T> cls, String sql, Object[] paramList) {
        try {
            return responseData(daoFactory.queryForSingleValue(cls, sql, paramList));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 查询单个基本数值（单个字段）.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param cls       要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param sql       查询的SQL
     * @param paramList 查询SQL的参数
     * @return 单个对象
     */
    public <T> ResponseData<T> queryForSingleValue(String connName, Class<T> cls, String sql, Object[] paramList) {
        try {
            return responseData(daoFactory.queryForSingleValue(connName, cls, sql, paramList));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 查询单个基本数值（单个字段）.
     *
     * @param valueCls   要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param queryParam 查询参数
     * @return 单个对象
     */
    public <T> ResponseData<T> queryForSingleValue(Class<T> valueCls, QueryParam queryParam) {
        try {
            return responseData(daoFactory.queryForSingleValue(valueCls, queryParam));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 查询单个基本数值（单个字段）.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param valueCls   要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param queryParam 查询参数
     * @return 单个对象
     */
    public <T> ResponseData<T> queryForSingleValue(String connName, Class<T> valueCls, QueryParam queryParam) {
        try {
            return responseData(daoFactory.queryForSingleValue(connName, valueCls, queryParam));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 查询单个基本数值列表（多行单个字段）.
     *
     * @param valueCls 要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param sql      查询的SQL
     * @return DataSet对象
     */
    public <T> ResponseData<ArrayList<T>> queryForSingleList(Class<T> valueCls, String sql) {
        try {
            return responseData(daoFactory.queryForSingleList(valueCls, sql));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 查询单个基本数值列表（多行单个字段）.
     *
     * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param valueCls 要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param sql      查询的SQL
     * @return 单个对象
     */
    public <T> ResponseData<ArrayList<T>> queryForSingleList(String connName, Class<T> valueCls, String sql) {
        try {
            return responseData(daoFactory.queryForSingleList(connName, valueCls, sql));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 查询单个基本数值列表（多行单个字段）.
     *
     * @param valueCls   要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param queryParam 查询参数
     * @return DataSet数据列表
     */
    public <T> ResponseData<ArrayList<T>> queryForSingleList(Class<T> valueCls, QueryParam queryParam) {
        try {
            return responseData(daoFactory.queryForSingleList(valueCls, queryParam));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 查询单个基本数值列表（多行单个字段）.
     *
     * @param connName
     * @param valueCls   要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param queryParam 查询参数
     * @return DataSet数据列表
     * @throws TransactionException 事务异常
     */
    public <T> ResponseData<ArrayList<T>> queryForSingleList(String connName, Class<T> valueCls, QueryParam queryParam) {
        try {
            return responseData(daoFactory.queryForSingleList(connName, valueCls, queryParam));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 查询单个基本数值列表（多行单个字段）.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param valueCls  要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param sql       查询的SQL
     * @param paramList 查询SQL的参数
     * @return 单个对象
     */
    public <T> ResponseData<ArrayList<T>> queryForSingleList(String connName, Class<T> valueCls, String sql, Object[] paramList) {
        try {
            return responseData(daoFactory.queryForSingleList(connName, valueCls, sql, paramList));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 查询单个基本数值列表（多行单个字段）.
     *
     * @param valueCls  要映射的基础类型，如int.class,long.class,String.class,Date.class
     * @param sql       查询的SQL
     * @param paramList 查询SQL的参数
     * @return 单个对象
     */
    public <T> ResponseData<ArrayList<T>> queryForSingleList(Class<T> valueCls, String sql, Object[] paramList) {
        try {
            return responseData(daoFactory.queryForSingleList(valueCls, sql, paramList));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 查询单个对象（单行数据）.
     *
     * @param entityCls 要映射的对象类型
     * @param selectSql 查询的SQL
     * @return 单个对象
     */
    public <T> ResponseData<T> queryForSingleObject(Class<T> entityCls, String selectSql) {
        try {
            return responseData(daoFactory.queryForSingleObject(entityCls, selectSql));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 查询单个对象（单行数据）.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls 要映射的对象类型
     * @param selectSql 查询的SQL
     * @return 单个对象
     */
    public <T> ResponseData<T> queryForSingleObject(String connName, Class<T> entityCls, String selectSql) {
        try {
            return responseData(daoFactory.queryForSingleObject(connName, entityCls, selectSql));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 查询单个对象（单行数据）.
     *
     * @param entityCls  要映射的对象类型
     * @param queryParam 查询参数
     * @return 单个对象
     */
    public <T> ResponseData<T> queryForSingleObject(Class<T> entityCls, QueryParam queryParam) {
        try {
            return responseData(daoFactory.queryForSingleObject(entityCls, queryParam));
        } catch (TransactionException e) {
            return responseError(e);
        }
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
    public <T> ResponseData<T> queryForSingleObject(Class<T> entityCls, String tableName, QueryParam queryParam) {
        try {
            return responseData(daoFactory.queryForSingleObject(entityCls, tableName, queryParam));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 查询单个对象（单行数据）.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls  要映射的对象类型
     * @param queryParam 查询参数
     * @return 单个对象
     */
    public <T> ResponseData<T> queryForSingleObject(String connName, Class<T> entityCls, QueryParam queryParam) {
        try {
            return responseData(daoFactory.queryForSingleObject(connName, entityCls, queryParam));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 查询单个对象（单行数据）.
     *
     * @param entityCls 要映射的对象类型
     * @param selectSql 查询的SQL
     * @param paramList 查询SQL的参数
     * @return 单个对象
     */
    public <T> ResponseData<T> queryForSingleObject(Class<T> entityCls, String selectSql, Object[] paramList) {
        try {
            return responseData(daoFactory.queryForSingleObject(entityCls, selectSql, paramList));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 查询单个对象（单行数据）.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls 要映射的对象类型
     * @param selectSql 查询的SQL
     * @param paramList 查询SQL的参数
     * @return 单个对象
     */
    public <T> ResponseData<T> queryForSingleObject(String connName, Class<T> entityCls, String selectSql, Object[] paramList) {
        try {
            return responseData(daoFactory.queryForSingleObject(connName, entityCls, selectSql, paramList));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 查询单个对象（单行数据）.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param entityCls  要映射的对象类型
     * @param tableName
     * @param queryParam 查询参数
     * @return 单个对象
     */
    public <T> ResponseData<T> queryForSingleObject(String connName, Class<T> entityCls, String tableName, QueryParam queryParam) {
        try {
            return responseData(daoFactory.queryForSingleObject(connName, entityCls, tableName, queryParam));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param selectSql 查询的SQL
     * @return DataSet数据列表
     */
    public ResponseData<DataSet> queryForDataSet(String selectSql) {
        try {
            return responseData(daoFactory.queryForDataSet(selectSql));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param connName  连接名，当设置为null时候，根据sql语句或表名确定
     * @param selectSql 查询的SQL
     * @return DataSet数据列表
     */
    public ResponseData<DataSet> queryForDataSet(String connName, String selectSql) {
        try {
            return responseData(daoFactory.queryForDataSet(connName, selectSql));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param selectSql  查询的SQL
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @return DataSet数据列表
     */
    public ResponseData<DataSet> queryForDataSet(String selectSql, int startIndex, int resultNum) {
        try {
            return responseData(daoFactory.queryForDataSet(selectSql, startIndex, resultNum));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param selectSql 查询的SQL
     * @param paramList 查询SQL的绑定参数
     * @return DataSet数据列表
     */
    public ResponseData<DataSet> queryForDataSet(String selectSql, Object[] paramList) {
        try {
            return responseData(daoFactory.queryForDataSet(selectSql, paramList));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param selectSql  查询的SQL
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @return DataSet数据列表
     */
    public ResponseData<DataSet> queryForDataSet(String connName, String selectSql, int startIndex, int resultNum) {
        try {
            return responseData(daoFactory.queryForDataSet(connName, selectSql, startIndex, resultNum));
        } catch (TransactionException e) {
            return responseError(e);
        }
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
    public ResponseData<DataSet> queryForDataSet(String connName, String selectSql, int startIndex, int resultNum, boolean autoCount) {
        try {
            return responseData(daoFactory.queryForDataSet(connName, selectSql, startIndex, resultNum, autoCount));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param selectSql 查询的SQL
     * @param paramList 查询SQL的绑定参数
     * @return DataSet数据列表
     */
    public ResponseData<DataSet> queryForDataSet(String connName, String selectSql, Object[] paramList) {
        try {
            return responseData(daoFactory.queryForDataSet(connName, selectSql, paramList));
        } catch (TransactionException e) {
            return responseError(e);
        }
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
    public ResponseData<DataSet> queryForDataSet(String connName, String selectSql, Object[] paramList, int startIndex, int resultNum) {
        try {
            return responseData(daoFactory.queryForDataSet(connName, selectSql, paramList, startIndex, resultNum));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param selectSql  查询的SQL
     * @param paramList  查询SQL的绑定参数
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @return DataSet数据列表
     */
    public ResponseData<DataSet> queryForDataSet(String selectSql, Object[] paramList, int startIndex, int resultNum) {
        try {
            return responseData(daoFactory.queryForDataSet(selectSql, paramList, startIndex, resultNum));
        } catch (TransactionException e) {
            return responseError(e);
        }
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
    public ResponseData<DataSet> queryForDataSet(String selectSql, Object[] paramList, int startIndex, int resultNum, boolean autoCount) {
        try {
            return responseData(daoFactory.queryForDataSet(selectSql, paramList, startIndex, resultNum, autoCount));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param selectSql  查询的SQL
     * @param startIndex 开始位置，默认为0
     * @param resultNum  结果集大小，默认为0，获取全部数据
     * @param autoCount  是否统计全部数据（用于分页算法），默认为false。
     * @return DataSet数据列表
     */
    public ResponseData<DataSet> queryForDataSet(String selectSql, int startIndex, int resultNum, boolean autoCount) {
        try {
            return responseData(daoFactory.queryForDataSet(selectSql, startIndex, resultNum, autoCount));
        } catch (TransactionException e) {
            return responseError(e);
        }
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
     */
    public ResponseData<DataSet> queryForDataSet(String connName, String selectSql, Object[] paramList, int startIndex, int resultNum, boolean autoCount) {
        try {
            return responseData(daoFactory.queryForDataSet(connName, selectSql, paramList, startIndex, resultNum, autoCount));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param connName   连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param queryParam 查询参数
     * @return DataSet数据列表
     */
    public ResponseData<DataSet> queryForDataSet(String connName, QueryParam queryParam) {
        try {
            return responseData(daoFactory.queryForDataSet(connName, queryParam));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 返回一个DataSet数据列表。 相比较DataList列表，这不是一个强类型列表，但是更加灵活.
     *
     * @param queryParam 查询参数
     * @return DataSet数据列表
     */
    public ResponseData<DataSet> queryForDataSet(QueryParam queryParam) {
        try {
            return responseData(daoFactory.queryForDataSet(queryParam));
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    // 其他方法

    /**
     * 执行一条SQL语句.
     *
     * @param sql 查询的SQL
     * @return 影响的行数
     */
    public ResponseData<Integer> executeCommand(String sql) {
        try {
            int effectedNum = daoFactory.executeCommand(sql);
            if (effectedNum < 1) {
                return responseData(null);
            } else {
                return responseData(effectedNum);
            }
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 执行一条SQL语句.
     *
     * @param connName 连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param sql      查询的SQL
     * @return 影响的行数
     */
    public ResponseData<Integer> executeCommand(String connName, String sql) {
        try {
            int effectedNum = daoFactory.executeCommand(connName, sql);
            if (effectedNum < 1) {
                return responseData(null);
            } else {
                return responseData(effectedNum);
            }
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 执行一条SQL语句.
     *
     * @param sql       查询的SQL
     * @param paramList 查询SQL的参数
     * @return 影响的行数
     */
    public ResponseData<Integer> executeCommand(String sql, Object[] paramList) {
        try {
            int effectedNum = daoFactory.executeCommand(sql, paramList);
            if (effectedNum < 1) {
                return responseData(null);
            } else {
                return responseData(effectedNum);
            }
        } catch (TransactionException e) {
            return responseError(e);
        }
    }


    /**
     * 执行一条SQL语句.
     *
     * @param connName  连接名，如设置为null，则根据sql语句或表名动态路由确定
     * @param sql       查询的SQL
     * @param paramList 查询SQL的参数
     * @return 影响的行数
     */
    public ResponseData<Integer> executeCommand(String connName, String sql, Object[] paramList) {
        try {
            int effectedNum = daoFactory.executeCommand(connName, sql, paramList);
            if (effectedNum < 1) {
                return responseData(null);
            } else {
                return responseData(effectedNum);
            }
        } catch (TransactionException e) {
            return responseError(e);
        }
    }

    /**
     * 解析QueryParam信息。
     *
     * @param queryParam 查询参数类。
     * @return
     * @throws TransactionException
     */
    public QueryParamResult parseQueryParam(QueryParam queryParam) {
        return daoFactory.parseQueryParam(queryParam);
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
        return daoFactory.parseQueryParam(cls, queryParam);
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
        return daoFactory.parseQueryParam(cls, tableName, queryParam);
    }


    /**
     * 把data封装成ResponseData。
     *
     * @param data
     * @param <T>
     * @return
     */
    private <T> ResponseData<T> responseData(T data) {
        if (data == null) {
            return ResponseData.errorCode(DaoResponseCode.DATA_NOT_FOUND_WARN);
        } else {
            return ResponseData.success(data);
        }
    }

    /**
     * 封装错误ResponseData。
     *
     * @param <T>
     * @return
     */
    private <T> ResponseData<T> responseError(TransactionException e) {
        // 记录错误日志
        logger.error(e.getMessage(), e);
        // 生产环境，只返回错误码，不返回错误信息，防止攻击者获取到敏感信息
        if (DaoConfigManager.isProdProfile()) {
            return ResponseData.errorCode(DaoResponseCode.TRANSACTION_ERROR);
        } else {
            return ResponseData.errorCode(DaoResponseCode.TRANSACTION_ERROR.getFullCode(), DaoResponseCode.TRANSACTION_ERROR.getLocalizedMessage() + e.getMessage());
        }
    }


}