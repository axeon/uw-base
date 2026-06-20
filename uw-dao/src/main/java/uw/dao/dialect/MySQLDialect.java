package uw.dao.dialect;

/**
 * MySQL 方言。
 *
 * <p>利用 MySQL 的 {@code LIMIT offset, count} 语法实现分页：在原 SQL 末尾追加
 * {@code limit ?,?}，两个占位符分别绑定 {@code startPos}（偏移）与 {@code resultNum}（行数）。</p>
 *
 * <p>对应驱动类名：{@code com.mysql.cj.jdbc.Driver} 等（含 "mysql"），由
 * {@link DialectManager} 自动选用。</p>
 *
 * @author axeon
 * @see Dialect#getPagedSQL(String, int, int)
 */
public class MySQLDialect extends Dialect {

    /**
     * 生成 MySQL 分页 SQL：{@code <sql> limit ?,?}。
     *
     * <p>占位符顺序：第一个 {@code ?} = {@code startPos}（偏移），第二个 {@code ?} = {@code resultNum}（行数）。</p>
     *
     * @param sql       原始查询 SQL（不含分页）
     * @param startPos  起始行偏移量（绑定到第一个 ?，LIMIT 的 offset）
     * @param resultNum 本页最大返回行数（绑定到第二个 ?，LIMIT 的 count）
     * @return {@code [分页SQL, startPos, resultNum]}
     */
    @Override
    public Object[] getPagedSQL(String sql, int startPos, int resultNum) {
        return new Object[]{sql + " limit ?,?", startPos, resultNum};
    }

}
