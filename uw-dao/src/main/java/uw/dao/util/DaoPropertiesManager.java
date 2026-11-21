package uw.dao.util;

import org.apache.commons.lang3.StringUtils;
import uw.dao.conf.UwDaoProperties;
import uw.dao.conf.UwDaoProperties.ConnPoolConfig;
import uw.dao.conf.UwDaoProperties.ConnRoute;
import uw.dao.conf.UwDaoProperties.ConnRouteConfig;
import uw.dao.conf.UwDaoProperties.TableShardConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * uw.dao 属性静态管理器：持有 {@link UwDaoProperties}（Spring 装配注入或测试手工初始化），
 * 供 DAO 底层非 Spring 管理的静态工具（连接路由/分表/SQL 组装）访问；内建路由解析缓存，
 * 配置更新（{@link #updateProperties}）清缓存即时生效。
 *
 * <p>261120 整改：由 conf.DaoPropertiesManager 改名移入 util（内部支撑设施，非公开 API 门面）；
 * 支撑 nacos 热更新（EnvironmentChangeEvent 重绑后同步，见 UwDaoAutoConfiguration）——
 * 查表类配置（connRoute/tableShard/prodProfiles）即时生效，连接池参数变更需重启（监听侧记 WARN 提示）。</p>
 *
 * @author axeon
 */
public final class DaoPropertiesManager {

    /**
     * 默认的root连接名
     */
    public static final String ROOT_CONN_NAME = "$ROOT_CONN$";
    /**
     * 链接路由Map.
     */
    private static final Map<String, String> routeMap = new ConcurrentHashMap<>();
    /**
     * 当前生效的 uw.dao 属性（volatile：热更新线程写入，静态工具线程并发读取）。
     */
    private static volatile UwDaoProperties properties;


    /**
     * @return 当前生效的 uw.dao 属性
     */
    public static UwDaoProperties getProperties() {
        return properties;
    }

    /**
     * 更新生效属性（Spring 装配注入 / nacos 热更新重绑后同步 / 测试初始化）。
     * <p>清空路由缓存使查表类配置（路由/分表）立即生效；连接池参数变更不重建已建池（需重启，
     * 调用方负责提示）。合法调用方：UwDaoAutoConfiguration 装配与热更新监听、initTestConn。</p>
     *
     * @param newProperties 新属性（不可为 null）
     */
    public static void updateProperties(UwDaoProperties newProperties) {
        DaoPropertiesManager.properties = newProperties;
        // 清空路由缓存，使新配置（路由/分片变更）立即生效。
        routeMap.clear();
    }

    /**
     * @return the isProdProfile
     */
    public static boolean isProdProfile() {
        return properties != null && properties.isProdProfile();
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
        DaoPropertiesManager.properties = new UwDaoProperties();
        ConnPoolConfig connPoolConfig = new ConnPoolConfig();
        properties.getConnPool().setRoot(connPoolConfig);
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
        if (properties.getConnPool().getRoot() != null) {
            list.add(ROOT_CONN_NAME);
        }
        if (properties.getConnPool().getList() != null) {
            list.addAll(properties.getConnPool().getList().keySet());
        }
        return list;
    }

    /**
     * 检查是否只有root连接。
     *
     * @return 连接池配置列表
     */
    public static boolean checkOnlyRootPool() {
        return properties.getConnPool().getRoot() != null && properties.getConnPool().getList() == null;
    }


    /**
     * 获取表分片配置.
     *
     * @param tableName 表名
     * @return 表分片配置
     */
    public static TableShardConfig getTableShardingConfig(String tableName) {
        return properties.getTableShard().get(tableName);
    }

    /**
     * 获取连接池配置.
     *
     * @param poolName
     * @return 连接池配置
     */
    public static ConnPoolConfig getConnPoolConfig(String poolName) {
        if (StringUtils.isBlank(poolName) || poolName.equals(ROOT_CONN_NAME)) {
            return properties.getConnPool().getRoot();
        }
        if (properties.getConnPool().getList() != null) {
            return properties.getConnPool().getList().get(poolName);
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
            ConnRoute connRoute = properties.getConnRoute();
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
