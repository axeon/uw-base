package uw.dao.conf;

import org.apache.commons.lang3.StringUtils;
import uw.dao.conf.DaoConfig.ConnPoolConfig;
import uw.dao.conf.DaoConfig.ConnRoute;
import uw.dao.conf.DaoConfig.ConnRouteConfig;
import uw.dao.conf.DaoConfig.TableShardConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DAO配置管理器.
 *
 * @author axeon
 */
public final class DaoConfigManager {

    /**
     * 默认的root连接名
     */
    public static final String ROOT_CONN_NAME = "$ROOT_CONN$";
    /**
     * 链接路由Map.
     */
    private static final Map<String, String> routeMap = new ConcurrentHashMap<>();
    /**
     * DAO配置表.
     */
    private static DaoConfig config;


    /**
     * @return the config
     */
    public static DaoConfig getConfig() {
        return config;
    }

    /**
     * @param config the config to set
     */
    public static void setConfig(DaoConfig config) {
        DaoConfigManager.config = config;
    }

    /**
     * @return the isProdProfile
     */
    public static boolean isProdProfile() {
        return config.isProdProfile();
    }

    /**
     * 初始化一个测试用连接。
     *
     * @param driver
     * @param url
     * @param username
     * @param password
     * @param maxConn
     */
    public static void initTestConn(String driver, String url, String username, String password, int maxConn) {
        DaoConfigManager.config = new DaoConfig();
        ConnPoolConfig connPoolConfig = new ConnPoolConfig();
        config.getConnPool().setRoot(connPoolConfig);
        connPoolConfig.setDriver(driver);
        connPoolConfig.setUrl(url);
        connPoolConfig.setUsername(username);
        connPoolConfig.setPassword(password);
        connPoolConfig.setMaxConn(maxConn);
    }


    /**
     * 获取连接池配置列表.
     *
     * @return 连接池配置列表
     */
    public static List<String> getConnPoolNameList() {
        ArrayList<String> list = new ArrayList<>();
        if (config.getConnPool().getRoot() != null) {
            list.add(ROOT_CONN_NAME);
        }
        if (config.getConnPool().getList() != null) {
            list.addAll(config.getConnPool().getList().keySet());
        }
        return list;
    }

    /**
     * 检查是否只有root连接。
     *
     * @return 连接池配置列表
     */
    public static boolean checkOnlyRootPool() {
        return config.getConnPool().getRoot() != null && config.getConnPool().getList() == null;
    }


    /**
     * 获取表分片配置.
     *
     * @param tableName 表名
     * @return 表分片配置
     */
    public static TableShardConfig getTableShardingConfig(String tableName) {
        return config.getTableShard().get(tableName);
    }

    /**
     * 获取连接池配置.
     *
     * @param poolName
     * @return 连接池配置
     */
    public static ConnPoolConfig getConnPoolConfig(String poolName) {
        if (StringUtils.isBlank(poolName) || poolName.equals(ROOT_CONN_NAME)) {
            return config.getConnPool().getRoot();
        }
        if (config.getConnPool().getList() != null) {
            return config.getConnPool().getList().get(poolName);
        } else {
            return null;
        }
    }

    /**
     * 获取路由映射信息.
     *
     * @param table  表名
     * @param access 权限
     * @return 路由映射信息
     */
    public static String getRouteMapping(String table, String access) {
        String tableAccess = table + ":" + access;
        String connPoolName = routeMap.get(tableAccess);
        if (connPoolName != null) {
            return connPoolName;
        }
        return routeMap.computeIfAbsent(tableAccess, (key) -> {
            String poolName = null;
            ConnRoute connRoute = config.getConnRoute();
            if (connRoute != null) {
                Map<String, ConnRouteConfig> map = connRoute.getList();
                // 先尝试匹配列表.
                if (map != null) {
                    for (Entry<String, ConnRouteConfig> kv : map.entrySet()) {
                        if (table.startsWith(kv.getKey())) {
                            ConnRouteConfig route = kv.getValue();
                            poolName = getPoolNameByAccess(route, access);
                            break;
                        }
                    }
                }
                // 如果匹配不到，那么就直接从根配置获取.
                if (poolName == null) {
                    ConnRouteConfig route = connRoute.getRoot();
                    if (route != null) {
                        poolName = getPoolNameByAccess(route, access);
                    }
                }
            }
            // 如果还是没有找到，说明根本就没配置路由，直接返回默认链接.
            if (poolName == null) {
                poolName = ROOT_CONN_NAME;
            }
            return poolName;
        });
    }

    /**
     * 通过route和access获取连接池名称.
     *
     * @param route  路由
     * @param access 权限
     * @return poolName
     */
    private static String getPoolNameByAccess(ConnRouteConfig route, String access) {
        String poolName = null;
        if ("read".equalsIgnoreCase(access)) {
            poolName = route.getFitReadPool();
        }
        if (StringUtils.isBlank(poolName)) {
            poolName = route.getFitWritePool();
        }
        return poolName;
    }

}
