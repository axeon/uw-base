package uw.dao.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Set;

/**
 * dao配置表.
 *
 * @author axeon
 */
@ConfigurationProperties(prefix = "uw.dao")
public class DaoConfig {

    /**
     * 连接池配置.
     */
    private ConnPool connPool = new ConnPool();

    /**
     * 连接路由配置.
     */
    private ConnRoute connRoute = new ConnRoute();

    /**
     * 表分片配置.
     */
    private LinkedHashMap<String, TableShardConfig> tableShard = new LinkedHashMap<String, TableShardConfig>();

    /**
     * sql统计配置.
     */
    private SqlStatsConfig sqlStats = new SqlStatsConfig();

    /**
     * Redis配置
     */
    private RedisProperties redis = null;
    /**
     * 生产环境命名列表
     */
    private Set<String> prodProfiles = Set.of("prod", "stab", "prd", "stb");

    /**
     * 是否生产环境.
     */
    private boolean isProdProfile = false;

    public ConnPool getConnPool() {
        return connPool;
    }

    public void setConnPool(ConnPool connPool) {
        this.connPool = connPool;
    }

    public ConnRoute getConnRoute() {
        return connRoute;
    }

    public void setConnRoute(ConnRoute connRoute) {
        this.connRoute = connRoute;
    }

    public LinkedHashMap<String, TableShardConfig> getTableShard() {
        return tableShard;
    }

    public void setTableShard(LinkedHashMap<String, TableShardConfig> tableShard) {
        this.tableShard = tableShard;
    }

    public SqlStatsConfig getSqlStats() {
        return sqlStats;
    }

    public void setSqlStats(SqlStatsConfig sqlStats) {
        this.sqlStats = sqlStats;
    }

    public RedisProperties getRedis() {
        return redis;
    }

    public void setRedis(RedisProperties redis) {
        this.redis = redis;
    }

    public Set<String> getProdProfiles() {
        return prodProfiles;
    }

    public void setProdProfiles(Set<String> prodProfiles) {
        this.prodProfiles = prodProfiles;
    }

    public boolean isProdProfile() {
        return isProdProfile;
    }

    public void setProdProfile(boolean prodProfile) {
        isProdProfile = prodProfile;
    }

    public static class RedisProperties extends org.springframework.boot.autoconfigure.data.redis.RedisProperties {
    }

    /**
     * 连接池配置.
     */
    public static class ConnPool {
        /**
         * 默认的连接池.
         */
        private ConnPoolConfig root;

        /**
         * 连接路由表.
         */
        private LinkedHashMap<String, ConnPoolConfig> list;

        public ConnPoolConfig getRoot() {
            return root;
        }

        public void setRoot(ConnPoolConfig root) {
            this.root = root;
        }

        public LinkedHashMap<String, ConnPoolConfig> getList() {
            return list;
        }

        public void setList(LinkedHashMap<String, ConnPoolConfig> list) {
            this.list = list;
        }
    }

    /**
     * 连接路由配置.
     */
    public static class ConnRoute {
        /**
         * 默认的链接路由.
         */
        private ConnRouteConfig root;

        /**
         * 连接路由表.
         */
        private LinkedHashMap<String, ConnRouteConfig> list;

        public ConnRouteConfig getRoot() {
            return root;
        }

        public void setRoot(ConnRouteConfig root) {
            this.root = root;
        }

        public LinkedHashMap<String, ConnRouteConfig> getList() {
            return list;
        }

        public void setList(LinkedHashMap<String, ConnRouteConfig> list) {
            this.list = list;
        }
    }

    /**
     * 连接池配置.
     *
     * @author axeon
     */
    public static class ConnPoolConfig {

        /**
         * 数据库驱动.
         */
        private String driver;

        /**
         * 服务器连接地址.
         */
        private String url;

        /**
         * 登录用户名.
         */
        private String username;

        /**
         * 登录密码.
         */
        private String password;

        /**
         * 测试sql，用于测试连接是否可用.
         */
        private String testSql;

        /**
         * 最小连接数.
         */
        private int minConn = 1;

        /**
         * 最大连接数.
         */
        private int maxConn = 1;

        /**
         * 连接闲时超时秒数.
         */
        private int connIdleTimeout = 300;

        /**
         * 连接忙时超时秒数.
         */
        private int connBusyTimeout = 1800;

        /**
         * 连接最大寿命秒数.
         */
        private int connMaxAge = 3600;

        public String getDriver() {
            return driver;
        }

        public void setDriver(String driver) {
            this.driver = driver;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getTestSql() {
            return testSql;
        }

        public void setTestSql(String testSql) {
            this.testSql = testSql;
        }

        public int getMinConn() {
            return minConn;
        }

        public void setMinConn(int minConn) {
            this.minConn = minConn;
        }

        public int getMaxConn() {
            return maxConn;
        }

        public void setMaxConn(int maxConn) {
            this.maxConn = maxConn;
        }

        public int getConnIdleTimeout() {
            return connIdleTimeout;
        }

        public void setConnIdleTimeout(int connIdleTimeout) {
            this.connIdleTimeout = connIdleTimeout;
        }

        public int getConnBusyTimeout() {
            return connBusyTimeout;
        }

        public void setConnBusyTimeout(int connBusyTimeout) {
            this.connBusyTimeout = connBusyTimeout;
        }

        public int getConnMaxAge() {
            return connMaxAge;
        }

        public void setConnMaxAge(int connMaxAge) {
            this.connMaxAge = connMaxAge;
        }
    }

    /**
     * 链接路由配置.
     *
     * @author axeon
     */
    public static class ConnRouteConfig {

        /**
         * 写入节点位置。
         * 不需要配置，程序运行期使用。
         */
        private transient int writePos;

        /**
         * 读取节点位置。
         * 不需要配置，程序运行期使用。
         */
        private transient int readPos;

        /**
         * 写连接.
         */
        private String[] writePools;

        /**
         * 读连接.
         */
        private String[] readPools;

        /**
         * 获取合适的写入连接池。
         *
         * @return
         */
        public String getFitWritePool() {
            return writePools[writePos++ % writePools.length];
        }

        /**
         * @return
         */
        public String getFitReadPool() {
            return readPools[readPos++ % readPools.length];
        }

        public String[] getWritePools() {
            return writePools;
        }

        public void setWritePools(String[] writePools) {
            this.writePools = writePools;
        }

        public String[] getReadPools() {
            return readPools;
        }

        public void setReadPools(String[] readPools) {
            this.readPools = readPools;
        }
    }

    /**
     * 多表配置.
     *
     * @author axeon
     */
    public static class TableShardConfig {
        /**
         * 分片类型。 当前仅支持date类型.
         */
        private String shardType;

        /**
         * 分片规则。当前仅支持day,month,year类型.
         */
        private String shardRule;

        /**
         * 是否自动建表.
         */
        private boolean autoGen = true;

        public String getShardType() {
            return shardType;
        }

        public void setShardType(String shardType) {
            this.shardType = shardType;
        }

        public String getShardRule() {
            return shardRule;
        }

        public void setShardRule(String shardRule) {
            this.shardRule = shardRule;
        }

        public boolean isAutoGen() {
            return autoGen;
        }

        public void setAutoGen(boolean autoGen) {
            this.autoGen = autoGen;
        }
    }

    /**
     * 统计sql执行信息，包括参数，返回信息，执行时间等,表名为dao_sql_stats开头，此表被自动配置为按日分表。.
     *
     * @author axeon
     */
    public static class SqlStatsConfig {

        /**
         * 是否统计，默认是false.
         */
        private boolean enable = false;

        /**
         * sql执行最小毫秒数。
         */
        private int sqlCostMin = 100;

        /**
         * 保存时间，默认是100天.
         */
        private int dataKeepDays = 100;

        public boolean isEnable() {
            return enable;
        }

        public void setEnable(boolean enable) {
            this.enable = enable;
        }

        public int getSqlCostMin() {
            return sqlCostMin;
        }

        public void setSqlCostMin(int sqlCostMin) {
            this.sqlCostMin = sqlCostMin;
        }

        public int getDataKeepDays() {
            return dataKeepDays;
        }

        public void setDataKeepDays(int dataKeepDays) {
            this.dataKeepDays = dataKeepDays;
        }
    }

}
