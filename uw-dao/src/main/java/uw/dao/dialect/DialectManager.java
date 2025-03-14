package uw.dao.dialect;

import org.apache.commons.lang3.StringUtils;

/**
 * 方言基类,对于当前的情况，可能只有分页需要处理.
 *
 * @author axeon
 */
public class DialectManager {

    /**
     * Mysql数据库方言
     */
    private static final Dialect MYSQL_DIALECT = new MySQLDialect();

    /**
     * Oracle数据库方言
     */
    private static final Dialect ORACLE_DIALECT = new OracleDialect();

    /**
     * SqlServer数据库方言
     */
    private static final Dialect SQLSERVER_DIALECT = new SqlServerDialect();

    /**
     * 得到Dialect对象.
     *
     * @param driverClassName 数据库驱动名称
     * @return Dialect对象
     */
    public static Dialect getDialectByDriverClassName(String driverClassName) {
        if (StringUtils.isNotBlank(driverClassName)) {
            if (driverClassName.contains("mysql")) {
                return MYSQL_DIALECT;
            } else if (driverClassName.contains("oracle")) {
                return ORACLE_DIALECT;
            } else if (driverClassName.contains("sqlserver")) {
                return SQLSERVER_DIALECT;
            }

        }
        return MYSQL_DIALECT;
    }
}
