package uw.dao.dialect;

/**
 * SQL Server 方言。
 *
 * <p>SQL Server 2012 之前不支持 {@code OFFSET ... FETCH}，分页依赖 {@code ROW_NUMBER() OVER(order by ...)}
 * 窗口函数：将原 SQL 包成子查询，按 {@code RowId between ? and ?} 取 {@code [startPos+1, startPos+resultNum]} 区间。</p>
 *
 * <p>{@code OVER(order by ...)} 子句必须有 order by，本方言按以下策略处理：</p>
 * <ul>
 *   <li>原 SQL <b>含</b> {@code order by}：复用其排序表达式，保证分页结果与原排序一致；</li>
 *   <li>原 SQL <b>不含</b> {@code order by}：用 {@code order by 1}（按第一列）兜底，结果顺序未定义但可分页。</li>
 * </ul>
 *
 * <p>对应驱动类名：{@code com.microsoft.sqlserver.jdbc.SQLServerDriver}（含 "sqlserver"），由
 * {@link DialectManager} 自动选用。</p>
 *
 * @author axeon
 * @see Dialect#getPagedSQL(String, int, int)
 */
public class SqlServerDialect extends Dialect {

    /**
     * 生成 SQL Server 分页 SQL。
     *
     * <p>占位符顺序：第一个 {@code ?} = {@code startPos}（RowId 下界），第二个 {@code ?} = {@code startPos + resultNum}（RowId 上界）。
     * 即 {@code RowId between startPos and startPos+resultNum}，取 {@code [startPos+1, startPos+resultNum]} 行。</p>
     *
     * <p>当原 SQL 含 {@code order by} 时，将其从原 SQL 切出并移入 {@code OVER(...)}，保证分页沿用原排序；
     * 否则用 {@code order by 1} 兜底。</p>
     *
     * @param sql       原始查询 SQL（不含分页）
     * @param startPos  起始行偏移量（绑定到第一个 ?，RowId 下界，基于 0：实际行号 startPos+1）
     * @param resultNum 本页最大返回行数（与 startPos 相加后绑定到第二个 ?，RowId 上界）
     * @return {@code [分页SQL, startPos, startPos + resultNum]}
     */
    @Override
    public Object[] getPagedSQL(String sql, int startPos, int resultNum) {
        int i = sql.toLowerCase().indexOf("order by");
        if (i <= 0) {
            return new Object[]{
                    "select * from (select sub.*, ROW_NUMBER() OVER(order by 1) AS RowId from (" + sql + ") sub) as b where RowId between ? and ?", startPos,
                    startPos + resultNum};
        }
        return new Object[]{
                "select * from (select sub.*, ROW_NUMBER() OVER(" + sql.substring(i - 1) + ") AS RowId from (" + sql.substring(0, i) + ") sub) as b where RowId between ? and ?", startPos,
                startPos + resultNum};
    }

}
