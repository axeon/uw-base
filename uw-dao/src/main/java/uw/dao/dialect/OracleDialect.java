package uw.dao.dialect;

/**
 * Oracle 方言。
 *
 * <p>Oracle 12c 之前不支持 {@code LIMIT}，分页依赖 {@code ROWNUM} 伪列。本方言采用两层子查询包装：
 * 内层用 {@code rownum <= ?} 截取前 N 行（避免全表扫描），外层用 {@code rnum > ?} 裁掉前 offset 行，
 * 得到 {@code [startPos+1, startPos+resultNum]} 区间的结果。</p>
 *
 * <p>对应驱动类名：{@code oracle.jdbc.driver.OracleDriver}（含 "oracle"），由
 * {@link DialectManager} 自动选用。</p>
 *
 * @author axeon
 * @see Dialect#getPagedSQL(String, int, int)
 */
public class OracleDialect extends Dialect {

    /**
     * 生成 Oracle 分页 SQL：
     * {@code select * from (select sub.*, rownum rnum from ( <sql> ) sub where rownum <= ?) where rnum > ?}。
     *
     * <p>占位符顺序：第一个 {@code ?} = {@code startPos + resultNum}（rownum 上界，截断前 N 行），
     * 第二个 {@code ?} = {@code startPos}（rnum 下界，裁掉前 offset 行）。
     * 与 MySQL 的 {@code (startPos, resultNum)} 顺序不同，注意区分。</p>
     *
     * @param sql       原始查询 SQL（不含分页）
     * @param startPos  起始行偏移量（绑定到第二个 ?，rnum 下界）
     * @param resultNum 本页最大返回行数（与 startPos 相加后绑定到第一个 ?，rownum 上界）
     * @return {@code [分页SQL, startPos + resultNum, startPos]}
     */
    @Override
    public Object[] getPagedSQL(String sql, int startPos, int resultNum) {
        return new Object[]{
                "select * from (select sub.*, rownum rnum from ( " + sql + " ) sub where rownum <= ?) where rnum > ?",
                startPos + resultNum, startPos};
    }

}
