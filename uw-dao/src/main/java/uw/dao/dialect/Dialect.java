package uw.dao.dialect;

/**
 * 数据库方言基类。
 *
 * <p>方言用于屏蔽不同数据库在 SQL 语法上的差异。当前框架中，CRUD 与原生 SQL 操作均已用标准 SQL
 * 编写，唯一需要方言处理的是<b>分页</b>——各数据库的分页语法（MySQL 的 {@code LIMIT}、Oracle 的
 * {@code ROWNUM}、SQL Server 的 {@code ROW_NUMBER}、PostgreSQL 的 {@code LIMIT/OFFSET}）各不相同。</p>
 *
 * <p>具体方言由 {@link DialectManager} 按 JDBC 驱动类名自动选择，业务代码无需感知。
 * 新增数据库支持时：实现 {@link Dialect}，在 {@link DialectManager#getDialectByDriverClassName}
 * 中按驱动类名关键字注册即可。</p>
 *
 * @author axeon
 * @see DialectManager
 * @see MySQLDialect
 * @see OracleDialect
 * @see SqlServerDialect
 * @see PostgreSQLDialect
 */
public class Dialect {

    /**
     * 将普通查询 SQL 改写为分页查询 SQL。
     *
     * <p><b>返回值约定</b>：返回长度为 3 的 {@code Object[]}：</p>
     * <ul>
     *   <li>{@code [0]} String：改写后的分页 SQL，末尾追加该数据库的分页语法（含 {@code ?} 占位符）；</li>
     *   <li>{@code [1]} Integer：绑定到分页语法中<b>第一个</b> {@code ?} 的参数值；</li>
     *   <li>{@code [2]} Integer：绑定到分页语法中<b>第二个</b> {@code ?} 的参数值。</li>
     * </ul>
     *
     * <p><b>注意参数顺序因数据库而异</b>：占位符的填充顺序由 SQL 中 {@code ?} 出现的先后决定。
     * 例如 MySQL 为 {@code limit ?,?}（参数 startPos、resultNum），
     * 而 PostgreSQL 为 {@code limit ? offset ?}（参数 resultNum、startPos）。
     * 框架执行层按 {@code [1]→第一个?、[2]→第二个?} 顺序绑定，因此各方言须自行保证数组顺序与 SQL 占位符顺序一致。</p>
     *
     * <p>基类默认返回 {@code null}（不支持分页），由 {@link DialectManager} 兜底返回具体方言，故实际不会命中。</p>
     *
     * @param sql       原始查询 SQL（不含分页）
     * @param startPos  起始行偏移量（从 0 开始）
     * @param resultNum 本页最大返回行数
     * @return {@code [分页SQL, 第一个绑定参数, 第二个绑定参数]}；基类默认返回 {@code null}
     */
    public Object[] getPagedSQL(String sql, int startPos, int resultNum) {
        return null;
    }

}
