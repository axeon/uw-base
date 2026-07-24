package uw.dao.util;

import uw.dao.conf.DaoConfig.TableShardConfig;
import uw.dao.conf.DaoConfigManager;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * 分表路由与跨分表查询工具。
 *
 * <p>支持两类分片策略（由 {@link TableShardConfig} 配置决定）：</p>
 * <ul>
 *   <li>按 id 取模分片：{@link #getTableNameById(String, long)}</li>
 *   <li>按日期分片（day/month/year）：{@link #getTableNameByDate(String, LocalDate)} /
 *       {@link #getTableNameByDate(String, Date)}</li>
 * </ul>
 *
 * <p>并提供建跨区间按天分表的 UNION ALL 内层查询拼装 {@link #unionAllShards}，
 * 用于报表类跨天查询，避免只按起始日期推单一表名而漏掉区间内其它分表数据。</p>
 *
 * @author axeon
 */
public class ShardingTableUtils {

    /**
     * DateTimeFormatter对象（包含年月日）.
     */
    public static final DateTimeFormatter FORMATTER_DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    /**
     * DateTimeFormatter对象（包含年月）.
     */
    public static final DateTimeFormatter FORMATTER_MONTH = DateTimeFormatter.ofPattern("yyyyMM");
    /**
     * DateTimeFormatter对象（包含年）.
     */
    public static final DateTimeFormatter FORMATTER_YEAR = DateTimeFormatter.ofPattern("yyyy");

    /**
     * ZoneId对象.
     */
    public static final ZoneId defaultZoneId = ZoneId.systemDefault();

    /**
     * 根据给定的ID给出表名.
     *
     * @param tableName 表名
     * @param id        id
     * @return 表名
     */
    public static String getTableNameById(String tableName, long id) {
        TableShardConfig config = DaoConfigManager.getTableShardingConfig(tableName);
        if (config != null && "id".equalsIgnoreCase(config.getShardType())) {
            long batchSize = Long.parseLong(config.getShardRule());
            return tableName + "_" + (id / batchSize);
        }
        return tableName;
    }

    /**
     * 根据给定的日期给出表名.
     *
     * @param tableName 表名
     * @param date      日期
     * @return 表名
     */
    public static String getTableNameByDate(String tableName, LocalDate date) {
        TableShardConfig config = DaoConfigManager.getTableShardingConfig(tableName);
        if (config != null && "date".equalsIgnoreCase(config.getShardType())) {
            switch (config.getShardRule()) {
                case "day":
                    return tableName + "_" + date.format(FORMATTER_DAY);
                case "month":
                    return tableName + "_" + date.format(FORMATTER_MONTH);
                case "year":
                    return tableName + "_" + date.format(FORMATTER_YEAR);
                default:
                    return tableName;
            }
        }
        return tableName;
    }

    /**
     * 根据给定的日期给出表名.
     *
     * @param tableName 表名
     * @param date      日期
     * @return 表名
     */
    public static String getTableNameByDate(String tableName, Date date) {
        // withNano=0是因为mysql精度太差。。。导致有时候四舍五入差了一秒
        return getTableNameByDate(tableName, date.toInstant().atZone(defaultZoneId).withNano(0).toLocalDate());
    }

    /**
     * 构建跨区间按天分表的 UNION ALL 内层查询：遍历 [startDate,endDate] 覆盖的所有分表，
     * 每表一段 "SELECT innerSelect FROM 分表 WHERE create_date&gt;=? AND create_date&lt;=? [extraCond]"，用 UNION ALL 连接。
     * <p>用于跨天报表查询，避免单表（仅按 startDate 推表名）漏掉 endDate 侧分表数据。
     * 每张分表都会追加 startDate/endDate + extraCondParams 到 params。</p>
     *
     * @param tableName       分表基础名（如 task_runner_stats）
     * @param startDate       区间开始
     * @param endDate         区间结束
     * @param innerSelect     内层 SELECT 列（不含 FROM），如 "LEFT(create_date,10) AS stats_date, num_all"
     * @param params          输出参数列表（每张分表追加 startDate,endDate + extraCondParams）
     * @param extraCond       额外条件 SQL 片段（如 "AND task_id=?"），null/空则无
     * @param extraCondParams extraCond 的绑定参数，每张分表重复追加
     * @return UNION ALL 拼接的内层 SQL（不含外层聚合）；区间为空时返回 1=0 空结果集
     */
    public static String unionAllShards(String tableName, Date startDate, Date endDate, String innerSelect,
                                        List<Object> params, String extraCond, Object... extraCondParams) {
        LocalDate startLocal = startDate.toInstant().atZone(defaultZoneId).toLocalDate();
        LocalDate endLocal = endDate.toInstant().atZone(defaultZoneId).toLocalDate();
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (LocalDate d = startLocal; !d.isAfter(endLocal); d = d.plusDays(1)) {
            if (!first) {
                sb.append(" UNION ALL ");
            }
            first = false;
            sb.append("SELECT ").append(innerSelect)
                    .append(" FROM ").append(getTableNameByDate(tableName, d))
                    .append(" WHERE create_date >= ? AND create_date <= ?");
            if (extraCond != null && !extraCond.isEmpty()) {
                sb.append(" ").append(extraCond);
            }
            params.add(startDate);
            params.add(endDate);
            if (extraCondParams != null) {
                for (Object p : extraCondParams) {
                    params.add(p);
                }
            }
        }
        if (first) {
            // 区间为空（startDate>endDate）兜底：返回空结果集，避免空 SQL
            sb.append("SELECT ").append(innerSelect).append(" FROM ").append(getTableNameByDate(tableName, startLocal))
                    .append(" WHERE 1=0");
        }
        return sb.toString();
    }

}
