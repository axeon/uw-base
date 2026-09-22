package uw.dao.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uw.dao.conf.DaoConfig;
import uw.dao.conf.DaoConfig.TableShardConfig;
import uw.dao.conf.DaoConfigManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ShardingTableUtils#unionAllShards} 分表名去重行为的固化测试。
 *
 * <p>核心不变式：按天迭代收集表名后每张分表只 UNION 一次且按时间先后保序——
 * month/year 分片规则与未配置分片的表在多天区间内均收敛到去重后的表集合。</p>
 *
 * <p>纯单元测试，不依赖 Spring/数据库：通过 {@link DaoConfigManager#setConfig} 注入
 * 分片配置，测试结束后恢复原配置，避免影响同模块其它测试。</p>
 *
 * @author axeon
 */
class ShardingTableUtilsTest {

    private static final String MONTH_TABLE = "test_month_stats";

    private static final String DAY_TABLE = "test_day_stats";

    /**
     * 未配置分片规则的表（不在 TableShard 配置中）。
     */
    private static final String PLAIN_TABLE = "test_plain_stats";

    /**
     * 原有配置，测试结束后恢复。
     */
    private DaoConfig originConfig;

    @BeforeEach
    void setUp() {
        originConfig = DaoConfigManager.getConfig();
        DaoConfig config = new DaoConfig();
        TableShardConfig monthShard = new TableShardConfig();
        monthShard.setShardType("date");
        monthShard.setShardRule("month");
        config.getTableShard().put(MONTH_TABLE, monthShard);
        TableShardConfig dayShard = new TableShardConfig();
        dayShard.setShardType("date");
        dayShard.setShardRule("day");
        config.getTableShard().put(DAY_TABLE, dayShard);
        DaoConfigManager.setConfig(config);
    }

    @AfterEach
    void tearDown() {
        DaoConfigManager.setConfig(originConfig);
    }

    /**
     * month 分片 + 跨3个月区间：跨月去重后每张月分表只 UNION 一次，且按时间先后保序。
     */
    @Test
    @DisplayName("month分片跨多月区间：月分表去重且保序")
    void testMonthShardCrossMonthsDedup() {
        Date startDate = date(2026, 7, 15);
        Date endDate = date(2026, 9, 3);
        List<Object> params = new ArrayList<>();
        String sql = ShardingTableUtils.unionAllShards(MONTH_TABLE, startDate, endDate, "create_date, num_all",
                params, "AND host_id=?", 100L);
        // 3个月分表各出现一次，去重后共2个 UNION ALL
        assertEquals(1, countMatches(sql, MONTH_TABLE + "_202607"));
        assertEquals(1, countMatches(sql, MONTH_TABLE + "_202608"));
        assertEquals(1, countMatches(sql, MONTH_TABLE + "_202609"));
        assertEquals(2, countMatches(sql, " UNION ALL "));
        // 保序：月份分表按时间先后出现
        assertTrue(sql.indexOf(MONTH_TABLE + "_202607") < sql.indexOf(MONTH_TABLE + "_202608"));
        assertTrue(sql.indexOf(MONTH_TABLE + "_202608") < sql.indexOf(MONTH_TABLE + "_202609"));
        // 每张分表追加 startDate,endDate + extraCondParams
        assertEquals(9, params.size());
        // 每段都带 extraCond
        assertEquals(3, countMatches(sql, "AND host_id=?"));
    }

    /**
     * month 分片 + 单月区间：同月内多天迭代去重为单表，无 UNION ALL。
     */
    @Test
    @DisplayName("month分片单月区间：去重为单表")
    void testMonthShardSingleMonthDedup() {
        Date startDate = date(2026, 8, 10);
        Date endDate = date(2026, 8, 28);
        List<Object> params = new ArrayList<>();
        String sql = ShardingTableUtils.unionAllShards(MONTH_TABLE, startDate, endDate, "create_date", params, null);
        assertEquals(1, countMatches(sql, MONTH_TABLE + "_202608"));
        assertFalse(sql.contains("UNION ALL"));
        assertEquals(2, params.size());
    }

    /**
     * day 分片回归：每天一张分表，去重不影响原有行为。
     */
    @Test
    @DisplayName("day分片区间：仍为每天一表不去重")
    void testDayShardKeepsOneTablePerDay() {
        Date startDate = date(2026, 8, 30);
        Date endDate = date(2026, 9, 2);
        List<Object> params = new ArrayList<>();
        String sql = ShardingTableUtils.unionAllShards(DAY_TABLE, startDate, endDate, "create_date", params, null);
        assertEquals(1, countMatches(sql, DAY_TABLE + "_20260830"));
        assertEquals(1, countMatches(sql, DAY_TABLE + "_20260831"));
        assertEquals(1, countMatches(sql, DAY_TABLE + "_20260901"));
        assertEquals(1, countMatches(sql, DAY_TABLE + "_20260902"));
        assertEquals(3, countMatches(sql, " UNION ALL "));
        assertEquals(8, params.size());
    }

    /**
     * month 分片 + 跨年区间：跨年去重后月分表按时间先后保序（覆盖 12 月->1 月的年边界）。
     */
    @Test
    @DisplayName("month分片跨年区间：月分表去重且跨年保序")
    void testMonthShardCrossYearDedup() {
        Date startDate = date(2025, 11, 20);
        Date endDate = date(2026, 2, 5);
        List<Object> params = new ArrayList<>();
        String sql = ShardingTableUtils.unionAllShards(MONTH_TABLE, startDate, endDate, "create_date", params, null);
        assertEquals(1, countMatches(sql, MONTH_TABLE + "_202511"));
        assertEquals(1, countMatches(sql, MONTH_TABLE + "_202512"));
        assertEquals(1, countMatches(sql, MONTH_TABLE + "_202601"));
        assertEquals(1, countMatches(sql, MONTH_TABLE + "_202602"));
        assertEquals(3, countMatches(sql, " UNION ALL "));
        assertTrue(sql.indexOf(MONTH_TABLE + "_202511") < sql.indexOf(MONTH_TABLE + "_202512"));
        assertTrue(sql.indexOf(MONTH_TABLE + "_202512") < sql.indexOf(MONTH_TABLE + "_202601"));
        assertTrue(sql.indexOf(MONTH_TABLE + "_202601") < sql.indexOf(MONTH_TABLE + "_202602"));
        assertEquals(8, params.size());
    }

    /**
     * 未配置分片规则的表：多天区间内表名恒定，去重收敛为单表、无 UNION ALL。
     */
    @Test
    @DisplayName("未配置分片表：多天区间收敛为单表")
    void testUnconfiguredTableDedupToSingle() {
        Date startDate = date(2026, 8, 10);
        Date endDate = date(2026, 8, 28);
        List<Object> params = new ArrayList<>();
        String sql = ShardingTableUtils.unionAllShards(PLAIN_TABLE, startDate, endDate, "create_date", params, null);
        assertEquals(1, countMatches(sql, " FROM " + PLAIN_TABLE + " "));
        assertFalse(sql.contains("UNION ALL"));
        assertEquals(2, params.size());
    }

    /**
     * 空区间兜底：startDate&gt;endDate 时返回 1=0 空结果集，不追加参数。
     */
    @Test
    @DisplayName("空区间：返回1=0空结果集")
    void testEmptyRangeFallback() {
        List<Object> params = new ArrayList<>();
        String sql = ShardingTableUtils.unionAllShards(MONTH_TABLE, date(2026, 9, 5), date(2026, 9, 1), "create_date", params, null);
        assertFalse(sql.contains(" UNION ALL "));
        assertTrue(sql.endsWith(" WHERE 1=0"));
        assertTrue(params.isEmpty());
    }

    private static Date date(int year, int month, int dayOfMonth) {
        return Date.from(LocalDate.of(year, month, dayOfMonth).atStartOfDay(ShardingTableUtils.defaultZoneId).toInstant());
    }

    private static int countMatches(String sql, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = sql.indexOf(sub, idx)) >= 0) {
            count++;
            idx += sub.length();
        }
        return count;
    }

}
