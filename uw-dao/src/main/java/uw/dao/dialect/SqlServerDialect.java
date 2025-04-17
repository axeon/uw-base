package uw.dao.dialect;

/**
 * SqlServer方言.
 */
public class SqlServerDialect extends Dialect {

    /**
     * 获取分页sql.
     *
     * @param sql       执行sql
     * @param startPos  起始位置
     * @param resultNum 结果集大小
     * @return
     */
    @Override
    public Object[] getPagedSQL(String sql, int startPos, int resultNum) {
        int i = sql.toLowerCase().indexOf("order by");
        return new Object[]{
                "select * from (select sub.*, ROW_NUMBER() OVER(" + sql.substring(i - 1) + ") AS RowId from (" + sql.substring(0, i) + ") sub) as b where RowId between ? and ?", startPos,
                startPos + resultNum};
    }

}
