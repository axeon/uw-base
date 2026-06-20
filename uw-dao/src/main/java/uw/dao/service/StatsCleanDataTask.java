package uw.dao.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.dao.DaoFactory;
import uw.dao.TransactionException;
import uw.dao.conf.DaoConfigManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;

/**
 * 数据清理任务.
 *
 * @author axeon
 */
public class StatsCleanDataTask implements Runnable {

    /**
     * 日志.
     */
    private static final Logger logger = LoggerFactory.getLogger(StatsCleanDataTask.class);

    /**
     * DAOFactory对象.
     */
    private final DaoFactory dao = DaoFactory.getInstance();

    /**
     * 统计分表后缀日期格式（yyyyMMdd），与分表命名规则一致。
     * yyyyMMdd 为定长8位数字串，字典序与时间顺序一致，可直接用 String.compareTo 比较早晚。
     */
    private static final DateTimeFormatter SUFFIX_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 获取当前的表Set.
     *
     * @return HashSet对象
     */
    private HashSet<String> getCurrentTableSet() {
        HashSet<String> set = new HashSet<>();
        List<String> list = null;
        try {
            list = dao.queryForValueList(dao.getConnectionName(DaoService.STATS_BASE_TABLE, "all"), String.class,
                    "show tables");
            if (list != null) {
                for (String s : list) {
                    if (s.startsWith(DaoService.STATS_BASE_TABLE + "_")) {
                        set.add(s);
                    }
                }
            }
        } catch (TransactionException e) {
            logger.error(e.getMessage());
        }
        return set;
    }

    /**
     * 定时清理过期统计分表。
     * <p>判定方式：解析表名后缀 yyyyMMdd 为日期，与 {@code 今天 - dataKeepDays} 比较，
     * 早于该截止日的分表才删除。避免按"表数量偏移"判定时，因某些天无数据未建表
     * 导致保留天数不足、误删仍需保留的表。</p>
     */
    @Override
    public void run() {
        logger.info("StatsInfo Clean Task is run start!");
        HashSet<String> tableSet = getCurrentTableSet();
        // 默认保留100天数据
        int keepDays = 100;
        try {
            keepDays = DaoConfigManager.getConfig().getSqlStats().getDataKeepDays();
        } catch (Throwable ignored) {
        }
        LocalDate cutoffDate = LocalDate.now().minusDays(keepDays);
        String cutoffSuffix = cutoffDate.format(SUFFIX_FORMATTER);
        for (String tableName : tableSet) {
            // 安全校验：只允许处理以 STATS_BASE_TABLE 开头且后缀为合法日期的分表
            if (!isValidStatsTable(tableName)) {
                logger.warn("Skipping unexpected table name: [{}]", tableName);
                continue;
            }
            String suffix = tableName.substring(DaoService.STATS_BASE_TABLE.length() + 1);
            // 后缀日期 < 截止日期（字典序与 yyyyMMdd 顺序一致）才删除
            if (suffix.compareTo(cutoffSuffix) >= 0) {
                continue;
            }
            try {
                dao.execute("DROP TABLE IF EXISTS " + tableName);
                logger.info("DROP TABLE IF EXISTS [{}].", tableName);
            } catch (TransactionException e) {
                logger.error(e.getMessage());
            }
        }
        logger.info("StatsInfo Clean Task is run end!");

    }

    /**
     * 校验表名是否为合法的统计分片表。
     * 合法表名格式：以 STATS_BASE_TABLE 开头 + "_" + 日期后缀(yyyyMMdd)，仅包含字母数字和下划线。
     */
    private boolean isValidStatsTable(String tableName) {
        if (tableName == null || !tableName.startsWith(DaoService.STATS_BASE_TABLE + "_")) {
            return false;
        }
        String suffix = tableName.substring(DaoService.STATS_BASE_TABLE.length() + 1);
        // 日期后缀应为纯数字（yyyyMMdd）
        return !suffix.isEmpty() && suffix.matches("\\d+");
    }

}
