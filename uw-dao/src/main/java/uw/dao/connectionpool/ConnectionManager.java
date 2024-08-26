package uw.dao.connectionpool;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.dao.conf.DaoConfig.ConnPoolConfig;
import uw.dao.conf.DaoConfigManager;
import uw.dao.dialect.Dialect;
import uw.dao.dialect.DialectManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import static uw.dao.conf.DaoConfigManager.ROOT_CONN_NAME;

/**
 * 数据库连接管理器.
 *
 * @author axeon
 */
public final class ConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger( ConnectionManager.class );

    /**
     * 连接池方言
     */
    private static final Map<String, Dialect> SOURCE_DIALECT_MAP = new HashMap<String, Dialect>();

    /**
     * 数据源缓存表
     */
    private static final Map<String, HikariDataSource> DATA_SOURCE_MAP = new ConcurrentHashMap<String, HikariDataSource>();

    /**
     * 启动连接管理器.
     */
    public static void start() {
        List<String> poolList = DaoConfigManager.getConnPoolNameList();
        for (String poolName : poolList) {
            try {
                HikariDataSource dataSource = getDataSource( poolName );
            } catch (Exception e) {
                logger.error( "Initial ConnectionPool[{}] failed !!!", poolName );
            }
        }
    }

    /**
     * 关闭连接管理器.
     */
    public static void stop() {
        destroyAllConnectionPool();
    }

    /**
     * 获得一个connection，在poolList中排名第一的为默认连接.
     *
     * @return Connection
     * @throws SQLException SQL异常
     */
    public static Connection getConnection() throws SQLException {
        return getConnection( ROOT_CONN_NAME );
    }

    /**
     * 获得一个新连接.
     *
     * @param poolName 连接池名称
     * @return 指定的新连接
     * @throws SQLException SQL异常
     */
    public static Connection getConnection(String poolName) throws SQLException {
        HikariDataSource dataSource = null;
        try {
            dataSource = getDataSource( poolName );
        } catch (Exception e) {
            throw new SQLException( "ConnectionManager.getConnection() failed to init connPool[" + poolName + "]", e );
        }
        if (dataSource == null) {
            throw new SQLException( "ConnectionManager.getConnection() failed to init connPool[" + poolName + "]" );
        }
        return dataSource.getConnection();
    }

    /**
     * 获得一个连接的方言，在poolList中排名第一的为默认连接.
     *
     * @return
     * @throws SQLException
     */
    public static Dialect getDialect() throws SQLException {
        return SOURCE_DIALECT_MAP.get( ROOT_CONN_NAME );
    }

    /**
     * 获得一个连接的方言，在poolList中排名第一的为默认连接.
     *
     * @return
     */
    public static Dialect getDialect(String poolName) {
        return SOURCE_DIALECT_MAP.get( poolName );
    }

    /**
     * 检查一个连接池是否存在。
     *
     * @param poolName 连接池名字
     */
    public static boolean checkConnectionPool(String poolName) {
        HikariDataSource dataSource = DATA_SOURCE_MAP.get( poolName );
        if (dataSource == null) {
            return false;
        }
        if (!dataSource.isRunning()) {
            return false;
        }
        return true;
    }

    /**
     * 获得一个连接池。
     *
     * @param poolName 连接池名字
     */
    public static HikariDataSource getConnectionPool(String poolName) {
        return DATA_SOURCE_MAP.get( poolName );
    }

    /**
     * 初始化一个连接池.
     *
     * @param poolName 连接池名字
     */
    public static synchronized HikariDataSource initConnectionPool(String poolName, String aliasName, String driver, String url, String username, String password, String testSql
            , int connMin, int connMax, int connIdleTimeout, int connBusyTimeout, int connMaxAge) {
        HikariDataSource dataSource = DATA_SOURCE_MAP.computeIfAbsent( poolName, (key) -> {

            // HikariConfig
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setPoolName( poolName );
            // 数据库驱动
            hikariConfig.setDriverClassName( driver );
            // 服务器连接字符串
            hikariConfig.setJdbcUrl( url );
            // 登陆用户名
            hikariConfig.setUsername( username );
            // 登陆密码
            hikariConfig.setPassword( password );
            // 测试sql hikari不配置testSql,会直接使用Connection.isValid()检活
            if (StringUtils.isNotBlank( testSql )) {
                hikariConfig.setConnectionTestQuery( testSql );
            }
            // 最小连接数
            if (connMin < 0) {
                hikariConfig.setMinimumIdle( 0 );
            } else {
                hikariConfig.setMinimumIdle( connMin );
            }
            // 最大连接数
            if (connMax < 1) {
                hikariConfig.setMaximumPoolSize( 1 );
            } else {
                hikariConfig.setMaximumPoolSize( connMax );
            }
            // 空闲超时(秒钟)
            if (connIdleTimeout < 60) {
                // 最小一分钟
                hikariConfig.setIdleTimeout( 60 * 1000L );
            } else {
                hikariConfig.setIdleTimeout( connIdleTimeout * 1000L );
            }
            // 连接超时(秒钟)
            if (connBusyTimeout < 1800) {
                hikariConfig.setConnectionTimeout( 1800 * 1000L );
            } else {
                hikariConfig.setConnectionTimeout( connBusyTimeout * 1000L );
            }
            // 连接寿命(秒钟)
            if (connMaxAge < 1800) {
                // 最小60分钟
                hikariConfig.setMaxLifetime( 1800 * 1000L );
            } else {
                hikariConfig.setMaxLifetime( connMaxAge * 1000L );
            }
            //对于oracle，需要特殊处理。
            if (driver.contains( "OracleDriver" )) {
                hikariConfig.setDataSourceProperties( oracleProperties() );
            }
            //补充设置，防止出现数据库宕机卡死的问题。
            hikariConfig.setConnectionTimeout( 10_000L );
            hikariConfig.setValidationTimeout( 10_000L );
            hikariConfig.setInitializationFailTimeout( 10_000L );
            // 数据库方言
            HikariDataSource hikariDataSource = new HikariDataSource( hikariConfig );
            // 注册成功,初始化方言
            SOURCE_DIALECT_MAP.put( key, DialectManager.getDialectByDriverClassName( hikariConfig.getDriverClassName() ) );
            // 启动连接池
            return hikariDataSource;
        } );
        if (StringUtils.isNotBlank( aliasName ) && dataSource != null) {
            DATA_SOURCE_MAP.put( aliasName, dataSource );
        }
        return dataSource;
    }

    /**
     * oracle特殊设定。
     * @return
     */
    private static Properties oracleProperties() {
        Properties properties = new Properties();

        properties.put("oracle.net.CONNECT_TIMEOUT", 10000);
        properties.put("oracle.net.READ_TIMEOUT", 10000);
        properties.put("oracle.jdbc.ReadTimeout", 10000);

        return properties;
    }

    /**
     * 销毁一个连接池.
     *
     * @param poolName 连接池名字
     */
    public static synchronized void destroyConnectionPool(String poolName) {
        HikariDataSource cp = DATA_SOURCE_MAP.remove( poolName );
        if (cp != null && cp.isRunning()) {
            cp.close();
        }
    }

    /**
     * 销毁全部连接池.
     */
    private static synchronized void destroyAllConnectionPool() {
        for (String poolName : DATA_SOURCE_MAP.keySet()) {
            destroyConnectionPool( poolName );
        }
    }

    /**
     * 获得连接池.
     *
     * @param poolName 连接池名字
     * @return Connection
     */
    private static HikariDataSource getDataSource(String poolName) {
        if (StringUtils.isBlank( poolName )) {
            poolName = ROOT_CONN_NAME;
        }
        return DATA_SOURCE_MAP.computeIfAbsent( poolName, (key) -> {
            ConnPoolConfig config = DaoConfigManager.getConnPoolConfig( key );
            if (config == null) {
                return null;
            }
            // HikariConfig
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setPoolName( StringUtils.isBlank( key ) ? "root" : key );
            // 数据库驱动
            hikariConfig.setDriverClassName( config.getDriver() );
            // 服务器连接字符串
            hikariConfig.setJdbcUrl( config.getUrl() );
            // 登陆用户名
            hikariConfig.setUsername( config.getUsername() );
            // 登陆密码
            hikariConfig.setPassword( config.getPassword() );
            // 测试sql hikari不配置testSql,会直接使用Connection.isValid()检活
            if (!hikariConfig.getDriverClassName().contains( "mysql" )) {
                hikariConfig.setConnectionTestQuery( config.getTestSql() );
            }
            // 最小连接数
            if (config.getMinConn() < 0) {
                hikariConfig.setMinimumIdle( 0 );
            } else {
                hikariConfig.setMinimumIdle( config.getMinConn() );
            }
            // 最大连接数
            if (config.getMaxConn() < 1) {
                hikariConfig.setMaximumPoolSize( 1 );
            } else {
                hikariConfig.setMaximumPoolSize( config.getMaxConn() );
            }
            // 空闲超时(秒钟)
            if (config.getConnIdleTimeout() < 60) {
                // 最小一分钟
                hikariConfig.setIdleTimeout( 60 * 1000L );
            } else {
                hikariConfig.setIdleTimeout( config.getConnIdleTimeout() * 1000L );
            }
            // 连接超时(秒钟)
            if (config.getConnBusyTimeout() < 1800) {
                hikariConfig.setConnectionTimeout( 1800 * 1000L );
            } else {
                hikariConfig.setConnectionTimeout( config.getConnBusyTimeout() * 1000L );
            }
            // 连接寿命(秒钟)
            if (config.getConnMaxAge() < 1800) {
                // 最小60分钟
                hikariConfig.setMaxLifetime( 1800 * 1000L );
            } else {
                hikariConfig.setMaxLifetime( config.getConnMaxAge() * 1000L );
            }
            // 数据库方言
            HikariDataSource hikariDataSource = new HikariDataSource( hikariConfig );
            // 注册成功,初始化方言
            SOURCE_DIALECT_MAP.put( key, DialectManager.getDialectByDriverClassName( hikariConfig.getDriverClassName() ) );
            // 启动连接池
            return hikariDataSource;
        } );
    }
}
