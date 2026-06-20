package uw.dao.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.dao.DaoFactory;
import uw.common.data.PageRowSet;
import uw.dao.SequenceFactory;
import uw.dao.TransactionException;
import uw.dao.conf.DaoConfig;
import uw.dao.conf.DaoConfig.TableShardConfig;
import uw.dao.conf.DaoConfigManager;
import uw.dao.util.DaoStringUtils;
import uw.dao.util.ShardingTableUtils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 按日期分表的工具.
 *
 * @author axeon
 */
public class TableShardingTask implements Runnable {

    /**
     * 日志.
     */
    private static final Logger log = LoggerFactory.getLogger(TableShardingTask.class);
    /**
     * dao配置文件。
     */
    private static final DaoConfig daoConfig = DaoConfigManager.getConfig();
    /**
     * DAOFactory对象.
     */
    private final DaoFactory dao = DaoFactory.getInstance();
    /**
     * 链接内表列表.
     */
    private final HashMap<String, List<String>> tableListMap = new HashMap<>();

    /**
     * 自动建表工具。 每小时检查当天表和第二天的表。 设定一个3秒的延时，是担心同步问题.
     * <p>每轮执行前清空表列表缓存，使得上一轮（或其他途径）新建的分表能被正确识别为已存在，
     * 避免重复尝试 CREATE（无 IF NOT EXISTS）产生错误日志。</p>
     */
    @Override
    public void run() {
        // 清空上一轮的表列表缓存，确保本轮能看到期间新建的表
        tableListMap.clear();
        LocalDateTime now = LocalDateTime.now();
        Map<String, TableShardConfig> map = daoConfig.getTableShard();
        for (Map.Entry<String, TableShardConfig> kv : map.entrySet()) {
            String baseTableName = kv.getKey();
            TableShardConfig tc = kv.getValue();
            if (tc.isAutoGen()) {
                String createScript = getCreateScript(baseTableName);
                String currentTable = "", nextTable = "";
                if ("date".equalsIgnoreCase(tc.getShardType())) {
                    // 计算当前表和下一个表
                    nextTable = switch (tc.getShardRule()) {
                        case "month" -> {
                            currentTable = now.format(ShardingTableUtils.FORMATTER_MONTH);
                            yield now.plusMonths(1).format(ShardingTableUtils.FORMATTER_MONTH);
                        }
                        case "year" -> {
                            currentTable = now.format(ShardingTableUtils.FORMATTER_YEAR);
                            yield now.plusYears(1).format(ShardingTableUtils.FORMATTER_YEAR);
                        }
                        default -> {
                            currentTable = now.format(ShardingTableUtils.FORMATTER_DAY);
                            yield now.plusDays(1).format(ShardingTableUtils.FORMATTER_DAY);
                        }
                    };
                    currentTable = baseTableName + "_" + currentTable;
                    nextTable = baseTableName + "_" + nextTable;

                } else if ("id".equalsIgnoreCase(tc.getShardType())) {
                    long batchSize = Long.parseLong(tc.getShardRule());
                    long current = SequenceFactory.getCurrentId(DaoStringUtils.toUpperFirst(DaoStringUtils.toClearCase(baseTableName))) / batchSize;
                    currentTable = baseTableName + "_" + current;
                    nextTable = baseTableName + "_" + (current + 1);
                }
                //开始创建表（仅当基表存在、且能解析出建表脚本时）
                if (createScript != null) {
                    if (!checkTableExist(currentTable)) {
                        String currentDdl = rewriteCreateScript(createScript, baseTableName, currentTable);
                        if (currentDdl != null) {
                            exeCreateTable(currentTable, currentDdl);
                        }
                    }
                    if (!checkTableExist(nextTable)) {
                        String nextDdl = rewriteCreateScript(createScript, baseTableName, nextTable);
                        if (nextDdl != null) {
                            exeCreateTable(nextTable, nextDdl);
                        }
                    }
                }
            }
        }
    }

    /**
     * 检查表是否存在.
     *
     * @param tableName 表名
     * @return boolean
     */
    private boolean checkTableExist(String tableName) {
        String connName = dao.getConnectionName(tableName, "all");
        List<String> tablist = tableListMap.get(connName);
        if (tablist == null) {
            tablist = loadTableList(connName);
        }
        return tablist.contains(tableName);
    }

    /**
     * 载入当前连接列表.
     *
     * @param connName 连接名
     * @return 连接列列表
     */
    private List<String> loadTableList(String connName) {
        Connection conn = null;
        ResultSet rs = null;
        List<String> list = new ArrayList<String>();
        try {
            conn = dao.getConnection(connName);
            DatabaseMetaData metaData = conn.getMetaData();
            rs = metaData.getTables(null, null, null, new String[]{"TABLE"});
            while (rs.next()) {
                list.add(rs.getString("TABLE_NAME"));
            }
            rs.close();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return list;
    }

    /**
     * 获取建表sql.
     *
     * @param tableName 表名
     * @return 建表sql
     */
    private String getCreateScript(String tableName) {
        String script = null;
        if (!isValidTableName(tableName)) {
            log.warn("Skipping unexpected table name: [{}]", tableName);
            return null;
        }
        try {
            PageRowSet ds = dao.queryForRowSet(dao.getConnectionName(tableName, "all"), "show create table " + tableName);
            if (ds.next()) {
                script = ds.getString(1);
            }
        } catch (TransactionException e) {
            log.error(e.getMessage(), e);
        }
        return script;
    }

    /**
     * 建表.
     *
     * @param tableName    表名
     * @param createScript 创建脚本
     * @return int
     */
    private int exeCreateTable(String tableName, String createScript) {
        int effectedNum = 0;
        try {
            effectedNum = dao.execute(dao.getConnectionName(tableName, "all"), createScript);
        } catch (TransactionException e) {
            log.error(e.getMessage(), e);
        }
        return effectedNum;
    }

    private boolean isValidTableName(String tableName) {
        return tableName != null && tableName.matches("[a-zA-Z_][a-zA-Z0-9_]*");
    }

    /**
     * 将基表的 CREATE 脚本改写为分表脚本。
     * <p>仅替换 DDL 开头 {@code CREATE TABLE `baseTable`} 处的表名，避免全文 replace
     * 误伤索引名、约束名中与基表名同名的片段（如 {@code KEY `idx_baseTable_col`}）。</p>
     *
     * @param createScript 基表的 SHOW CREATE TABLE 输出
     * @param baseTable    基表名
     * @param targetTable  目标分表名
     * @return 改写后的建表脚本；若未匹配到表名定义则返回 null
     */
    private String rewriteCreateScript(String createScript, String baseTable, String targetTable) {
        if (createScript == null) {
            return null;
        }
        // 匹配 DDL 首部的表名定义，支持 `baseTable` / "baseTable" / baseTable 三种引号形式
        Pattern p = Pattern.compile("(CREATE\\s+(?:TEMPORARY\\s+)?TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?)(`?)(\\Q" + baseTable + "\\E)\\2",
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(createScript);
        if (!m.find()) {
            return null;
        }
        // quoteReplacement 防御：targetTable 虽经 isValidTableName 校验只含字母数字下划线，
        // 但 replaceFirst 的 replacement 参数会把 $/\ 当特殊字符，显式转义避免未来校验放宽时埋雷。
        return m.replaceFirst("$1$2" + Matcher.quoteReplacement(targetTable) + "$2");
    }

}
