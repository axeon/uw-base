package uw.dao.dialect;

import org.apache.commons.lang3.StringUtils;

/**
 * 数据库方言管理器。
 *
 * <p>按 JDBC 驱动类名（{@code driver}）自动选择对应的 {@link Dialect}，使框架无需为不同数据库编写不同代码。
 * 方言实例均为无状态单例，安全共享。</p>
 *
 * <p><b>驱动识别规则</b>：对驱动类名做关键字包含匹配（大小写敏感，因驱动类名本身规范），匹配优先级如下：</p>
 * <ol>
 *   <li>含 {@code "mysql"}    → {@link MySQLDialect}（驱动如 {@code com.mysql.cj.jdbc.Driver}）</li>
 *   <li>含 {@code "oracle"}   → {@link OracleDialect}（驱动如 {@code oracle.jdbc.driver.OracleDriver}）</li>
 *   <li>含 {@code "sqlserver"}→ {@link SqlServerDialect}（驱动如 {@code com.microsoft.sqlserver.jdbc.SQLServerDriver}）</li>
 *   <li>含 {@code "postgresql"}→ {@link PostgreSQLDialect}（驱动如 {@code org.postgresql.Driver}）</li>
 *   <li>其他 / 空 / 未匹配     → {@link MySQLDialect}（兜底，因 MySQL 分页语法适用面最广）</li>
 * </ol>
 *
 * <p>新增数据库支持：实现 {@link Dialect}，在本类的 {@link #getDialectByDriverClassName(String)}
 * 按驱动类名关键字增加匹配分支与单例字段即可。</p>
 *
 * @author axeon
 */
public class DialectManager {

    /**
     * MySQL 方言单例（无状态，安全共享）。
     */
    private static final Dialect MYSQL_DIALECT = new MySQLDialect();

    /**
     * Oracle 方言单例（无状态，安全共享）。
     */
    private static final Dialect ORACLE_DIALECT = new OracleDialect();

    /**
     * SQL Server 方言单例（无状态，安全共享）。
     */
    private static final Dialect SQLSERVER_DIALECT = new SqlServerDialect();

    /**
     * PostgreSQL 方言单例（无状态，安全共享）。
     */
    private static final Dialect POSTGRESQL_DIALECT = new PostgreSQLDialect();

    /**
     * 根据 JDBC 驱动类名获取对应的方言。
     *
     * <p>匹配规则：按 mysql → oracle → sqlserver → postgresql 的顺序对驱动类名做包含匹配，
     * 命中即返回对应方言；驱动类名为空或均未命中时返回 {@link MySQLDialect} 作为兜底。</p>
     *
     * @param driverClassName JDBC 驱动类名（如 {@code com.mysql.cj.jdbc.Driver}），可为 null/空
     * @return 对应的 {@link Dialect}；未匹配时返回 MySQL 方言
     */
    public static Dialect getDialectByDriverClassName(String driverClassName) {
        if (StringUtils.isNotBlank(driverClassName)) {
            if (driverClassName.contains("mysql")) {
                return MYSQL_DIALECT;
            } else if (driverClassName.contains("oracle")) {
                return ORACLE_DIALECT;
            } else if (driverClassName.contains("sqlserver")) {
                return SQLSERVER_DIALECT;
            } else if (driverClassName.contains("postgresql")) {
                return POSTGRESQL_DIALECT;
            }

        }
        return MYSQL_DIALECT;
    }
}
