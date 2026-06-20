package uw.dao.dialect;

/**
 * PostgreSQL 方言。
 *
 * <p>利用 PostgreSQL 原生的 {@code LIMIT count OFFSET offset} 语法实现分页：在原 SQL 末尾追加
 * {@code limit ? offset ?}，两个占位符分别绑定 {@code resultNum}（行数）与 {@code startPos}（偏移）。</p>
 *
 * <p><b>注意参数顺序与 MySQL 相反</b>：MySQL 是 {@code limit offset, count}（参数 startPos、resultNum），
 * PostgreSQL 是 {@code limit ? offset ?}（参数 resultNum、startPos）。两者切勿混淆。</p>
 *
 * <p>对应驱动类名：{@code org.postgresql.Driver}（含 "postgresql"），由
 * {@link DialectManager} 自动选用。</p>
 *
 * @author axeon
 * @see Dialect#getPagedSQL(String, int, int)
 */
public class PostgreSQLDialect extends Dialect {

    /**
     * 生成 PostgreSQL 分页 SQL：{@code <sql> limit ? offset ?}。
     *
     * <p>占位符顺序：第一个 {@code ?} = {@code resultNum}（LIMIT 行数），第二个 {@code ?} = {@code startPos}（OFFSET 偏移）。</p>
     *
     * @param sql       原始查询 SQL（不含分页）
     * @param startPos  起始行偏移量（绑定到第二个 ?，OFFSET）
     * @param resultNum 本页最大返回行数（绑定到第一个 ?，LIMIT）
     * @return {@code [分页SQL, resultNum, startPos]}
     */
    @Override
    public Object[] getPagedSQL(String sql, int startPos, int resultNum) {
        return new Object[]{sql + " limit ? offset ?", resultNum, startPos};
    }

}
