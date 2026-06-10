package uw.dao.util;

import uw.dao.conf.DaoConfigManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static uw.dao.conf.DaoConfigManager.ROOT_CONN_NAME;

/**
 * SQL工具类，根据SQL语句提取目标表名并路由到对应的数据库连接。
 *
 * @author axeon
 */
public class SQLUtils {

    // 匹配FROM后第一个表名，遇到空格/括号/逗号/分号停止，跳过子查询
    private static final Pattern SELECT_TABLE_PATTERN = Pattern.compile("\\bfrom\\s+([^\\s(,;]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern UPDATE_TABLE_PATTERN = Pattern.compile("\\bupdate\\s+([^\\s(,;]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DELETE_TABLE_PATTERN = Pattern.compile("\\bfrom\\s+([^\\s(,;]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern INSERT_TABLE_PATTERN = Pattern.compile("\\binto\\s+([^\\s(,;]+)", Pattern.CASE_INSENSITIVE);
    // 兼容 CREATE TABLE [IF NOT EXISTS] tableName
    private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile("\\btable\\s+(?:if\\s+not\\s+exists\\s+)?([^\\s(,;]+)", Pattern.CASE_INSENSITIVE);

    /**
     * 使用正则从SQL中提取表名，并去除反引号和双引号包裹。
     */
    private static String extractTableName(String sql, Pattern pattern) {
        Matcher m = pattern.matcher(sql);
        if (m.find()) {
            return m.group(1).replace("`", "").replace("\"", "");
        }
        return "";
    }

    /**
     * 从SQL语句中识别操作类型和目标表名，路由到对应的数据库连接。
     * <p>
     * 支持SELECT/UPDATE/DELETE/INSERT/REPLACE/MERGE/CREATE TABLE语句，
     * 仅对SQL前128字符做类型判断以减少toLowerCase开销。
     *
     * @param sql SQL语句
     * @return 路由后的数据库连接名称
     */
    public static String getConnNameFromSQL(String sql) {
        if (DaoConfigManager.checkOnlyRootPool()) {
            return ROOT_CONN_NAME;
        }
        sql = sql.trim();
        String lower = sql.length() < 128 ? sql.toLowerCase() : sql.substring(0, 128).toLowerCase();
        String table = "", access = "write";

        if (lower.startsWith("select")) {
            table = extractTableName(sql, SELECT_TABLE_PATTERN);
            access = "read";
        } else if (lower.startsWith("update")) {
            table = extractTableName(sql, UPDATE_TABLE_PATTERN);
        } else if (lower.startsWith("delete")) {
            table = extractTableName(sql, DELETE_TABLE_PATTERN);
        } else if (lower.startsWith("insert") || lower.startsWith("replace") || lower.startsWith("merge")) {
            table = extractTableName(sql, INSERT_TABLE_PATTERN);
        } else if (lower.startsWith("create")) {
            table = extractTableName(sql, CREATE_TABLE_PATTERN);
        }
        return DaoConfigManager.getRouteMapping(table, access);
    }

}
