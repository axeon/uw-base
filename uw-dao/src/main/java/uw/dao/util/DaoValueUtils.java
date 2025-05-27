package uw.dao.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.util.Arrays;

/**
 * dao的数值工具类.
 */
public class DaoValueUtils {

    private static final FastDateFormat dateFormat = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");

    /**
     * 从日期类型转换为java.sql.Timestamp.
     *
     * @param date 日期时间
     * @return Timestamp对象
     */
    public static java.sql.Timestamp dateToTimestamp(java.util.Date date) {
        if (date != null) {
            return new java.sql.Timestamp(date.getTime());
        } else {
            return null;
        }
    }

    /**
     * 获取真正的列名。 在sql里面可能会使用一些函数并使用as进行区分.
     *
     * @param columnName 列名
     * @return 列名
     */
    public static String getTrueColumnName(String columnName) {
        int p = columnName.indexOf(" as ");
        if (p > -1) {
            return columnName.substring(p + 4).trim();
        } else {
            return columnName;
        }
    }

    /**
     * 把null转换为空字符.
     *
     * @param str 字符串
     * @return String
     */
    public static String nullToStr(String str) {
        if (str == null) {
            return StringUtils.EMPTY;
        }
        return str;
    }

    /**
     * 合并sql和参数。
     *
     * @param sql
     * @param paramList
     * @return
     */
    public static String combineSqlAndParam(String sql, Object[] paramList) {
        //如果参数列表为空，直接返回sql。
        if (paramList == null) {
            return sql;
        }
        String[] sqlParts = sql.split("\\?");
        //如果参数长度小于sql分段，说明有问题，分离合并后返回。
        if (paramList.length < sqlParts.length - 1) {
            return sql + "#" + Arrays.toString(paramList);
        }
        StringBuilder sb = new StringBuilder(sql.length() * 2);
        for (int i = 0; i < paramList.length; i++) {
            sb.append(sqlParts[i]);
            Object param = paramList[i];
            if (param instanceof String) {
                sb.append("'");
                sb.append(paramList[i]);
                sb.append("'");
            } else if (param instanceof java.util.Date) {
                sb.append("'");
                sb.append(dateFormat.format(paramList[i]));
                sb.append("'");
            } else {
                sb.append(paramList[i]);
            }
        }
        if (sqlParts.length > paramList.length) {
            sb.append(sqlParts[sqlParts.length - 1]);
        }
        return sb.toString();
    }

}
