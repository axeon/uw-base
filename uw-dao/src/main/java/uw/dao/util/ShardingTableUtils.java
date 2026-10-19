package uw.dao.util;

import uw.common.data.PageList;
import uw.common.data.PageRowSet;
import uw.common.dto.PageQueryParam;
import uw.common.response.ResponseData;
import uw.common.util.DateTools;
import uw.dao.DaoManager;
import uw.dao.vo.QueryParamResult;
import uw.dao.conf.DaoConfig.TableShardConfig;
import uw.dao.conf.DaoConfigManager;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
 * <p>并提供建跨区间日期分表的 UNION ALL 内层查询拼装 {@link #unionAllShards}，
 * 用于报表类跨区间查询，避免只按起始日期推单一表名而漏掉区间内其它分表数据；
 * 拼装前对迭代取到的分表名做保序去重，兼容 day/month/year 任一分片规则。</p>
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
     * 构建跨区间日期分表的 UNION ALL 内层查询：遍历 [startDate,endDate] 闭区间覆盖的所有分表，
     * 每表一段 "SELECT innerSelect FROM 分表 WHERE create_date&gt;=? AND create_date&lt;=? [extraCond]"，用 UNION ALL 连接。
     * <p>用于跨区间报表查询，避免单表（仅按 startDate 推表名）漏掉 endDate 侧分表数据。
     * 每张分表都会追加 startDate/endDate + extraCondParams 到 params。</p>
     * <p>分表名按天迭代收集后经 LinkedHashSet 保序去重：每张分表只 UNION 一次且按时间先后排列，
     * 兼容 day/month/year 任一分片规则及未配置分片的表（多天区间收敛为单表）。</p>
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
        // 按天迭代（DateTools.getDateList）收集分表名，保序去重保证每表只 UNION 一次
        //（month/year 分片与未分片表多天命中同一表）
        Set<String> shardTables = new LinkedHashSet<>();
        for (Date day : DateTools.getDateList(startDate, endDate)) {
            shardTables.add(getTableNameByDate(tableName, day));
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String shardTable : shardTables) {
            if (!first) {
                sb.append(" UNION ALL ");
            }
            first = false;
            sb.append("SELECT ").append(innerSelect)
                    .append(" FROM ").append(shardTable)
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
        if (shardTables.isEmpty()) {
            // 区间为空（startDate>endDate）兜底：返回空结果集，避免空 SQL
            sb.append("SELECT ").append(innerSelect).append(" FROM ").append(tableName)
                    .append(" WHERE 1=0");
        }
        return sb.toString();
    }
    // ==================== 分表存在性探测与 UNION 拼装（探测/拼装解耦） ====================

    /**
     * 基名现存分表集（<b>一次 {@code show tables like '基名\_%'} 整体拉取</b>，绝不逐表探测）。
     * <p>
     * 分表由 {@code TableShardingTask} 只预建"当期+下期"，跨月窗口引用历史分表时可能不存在
     * （in-place 升级首月/迁移换库/保留策略清理后）——盲拼 UNION 会整条 1146（Table doesn't
     * exist）。探测走 show tables 而非 information_schema（后者受统计缓存欺骗）。
     *
     * @param tableName 分表基名
     * @return 现存分表名集合（探测失败返回 null——调用方按"全窗口直拼"处理，缺表时报可见 1146）
     */
    public static Set<String> getExistingShards(String tableName) {
        PageRowSet rs = DaoManager.getInstance()
                .queryForRowSet("show tables like '" + tableName + "\\_%'").getData();
        if (rs == null) {
            return null;
        }
        Set<String> existing = new LinkedHashSet<>();
        while (rs.next()) {
            existing.add(rs.getString(0));
        }
        return existing;
    }

    /**
     * 窗口内<b>实际存在</b>的分表名数组：按分表规则推导窗口分表名（{@link #getTableNameByDate}
     * 保序去重，兼容 day/month/year 任一规则），再经 {@link #getExistingShards} 一次探测过滤——
     * 缺失分表直接剔除，返回可安全 UNION 的表名清单。
     * <p>
     * <b>当期窗口免探测短路</b>：窗口分表全部收敛为当期分表且该表启用自动预建时（TableShardingTask
     * 预建"当期+下期"，当期表必然存在），直接返回窗口全集，省一次元数据往返（管理端常态查询
     * 即此形态）。未配置分片（窗口收敛为基表名）同样免探测直返。
     *
     * @param tableName 分表基名
     * @param startDate 窗口开始
     * @param endDate   窗口结束
     * @return 实际存在的窗口分表名（保序；探测失败按全窗口直返——缺表时将报可见 1146）
     */
    public static String[] getExistingShards(String tableName, Date startDate, Date endDate) {
        Set<String> windowTables = new LinkedHashSet<>();
        for (Date day : DateTools.getDateList(startDate, endDate)) {
            windowTables.add(getTableNameByDate(tableName, day));
        }
        //未配置分片（全部日期收敛为基表）：探测无意义（like '基名\_%' 不匹配基表自身），直返
        if (windowTables.size() == 1 && windowTables.contains(tableName)) {
            return windowTables.toArray(new String[0]);
        }
        //当期窗口免探测：窗口全落当期分表且启用自动预建（当期表必然存在）
        if (windowTables.size() == 1) {
            String currentShard = getTableNameByDate(tableName, LocalDate.now());
            TableShardConfig config = DaoConfigManager.getTableShardingConfig(tableName);
            if (windowTables.contains(currentShard) && (config == null || config.isAutoGen())) {
                return windowTables.toArray(new String[0]);
            }
        }
        Set<String> existing = getExistingShards(tableName);
        if (existing == null) {
            //探测失败：全窗口直返（宁可缺表时可见 1146，不静默丢一段数据）
            return windowTables.toArray(new String[0]);
        }
        List<String> kept = new ArrayList<>(windowTables.size());
        for (String shardTable : windowTables) {
            if (existing.contains(shardTable)) {
                kept.add(shardTable);
            }
        }
        return kept.toArray(new String[0]);
    }

    /**
     * 按表名清单拼 UNION ALL 内层查询（纯拼装，不做探测/存在性判断——表名清单经
     * {@link #getExistingShards(String, Date, Date)} 过滤后传入）。
     * <p>
     * 每张分表统一用<b>全窗口</b>过滤 create_date：分表只存自身日期段的数据（路由按写入时刻
     * 定表），全窗口谓词命中的行集与逐表 clamp 收敛窗口<b>等价</b>——无需按表覆盖期分段。
     *
     * @param tables          分表名清单（不可为空数组——空窗口由调用方短路返回空结果）
     * @param startDate       窗口开始
     * @param endDate         窗口结束
     * @param innerSelect     内层 SELECT 列（不含 FROM）
     * @param params          输出参数列表（每张表追加 startDate,endDate + extraCondParams）
     * @param extraCond       额外条件 SQL 片段（null/空则无）
     * @param extraCondParams extraCond 的绑定参数（每张表重复追加）
     * @return UNION ALL 拼接的内层 SQL
     */
    public static String unionShards(String[] tables, Date startDate, Date endDate, String innerSelect,
                                     List<Object> params, String extraCond, Object[] extraCondParams) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tables.length; i++) {
            if (i > 0) {
                sb.append(" UNION ALL ");
            }
            sb.append("SELECT ").append(innerSelect)
                    .append(" FROM ").append(tables[i])
                    .append(" WHERE create_date >= ? AND create_date <= ?");
            if (extraCond != null && !extraCond.isEmpty()) {
                sb.append(" ").append(extraCond);
            }
            params.add(startDate);
            params.add(endDate);
            if (extraCondParams != null) {
                params.addAll(Arrays.asList(extraCondParams));
            }
        }
        return sb.toString();
    }

    /**
     * 跨分表 UNION 分页查询执行管线（存在性过滤 + 动态过滤/排序白名单/分页 + 仅计数短路）。
     * <p>
     * <b>SELECT_SQL 占位符位序</b>：分表占位符参数在 SQL 文本中位于最前，必须置于 QueryParam 条件
     * 参数之前（{@code shardParams + condParams} 拼接顺序不可调换），否则 PreparedStatement 位置
     * 绑定错位=查询结果错误。窗口内无存在分表（早于库最早分表）返回空分页。
     *
     * @param entityClass   结果实体类型
     * @param tableName     分表基名
     * @param selectColumns 内层显式列投影（UNION ALL 各臂列集恒定一致）
     * @param queryParam    分页查询参数（SELECT_SQL/动态条件/排序白名单/分页开关）
     * @param startDate     窗口开始
     * @param endDate       窗口结束
     * @param <T>           实体类型
     * @return 统计分页列表
     */
    public static <T> ResponseData<PageList<T>> listShardUnion(Class<T> entityClass, String tableName,
                                                               String selectColumns, PageQueryParam queryParam,
                                                               Date startDate, Date endDate) {
        String[] tables = getExistingShards(tableName, startDate, endDate);
        if (tables.length == 0) {
            //窗口早于库最早分表：空结果（含全量统计的空 PageList）
            return ResponseData.success(new PageList<>(new ArrayList<>(), queryParam.START_INDEX(), queryParam.RESULT_NUM(), 0));
        }
        DaoManager dao = DaoManager.getInstance();
        List<Object> shardParams = new ArrayList<>();
        String unionSql = unionShards(tables, startDate, endDate, selectColumns, shardParams, null, null);
        queryParam.SELECT_SQL("select * from (" + unionSql + ") u_shard_stats");
        QueryParamResult parsed = QueryParamUtils.parseQueryParam(queryParam);
        Object[] condParams = parsed.getParamList();
        Object[] allParams = new Object[shardParams.size() + condParams.length];
        for (int i = 0; i < shardParams.size(); i++) {
            allParams[i] = shardParams.get(i);
        }
        System.arraycopy(condParams, 0, allParams, shardParams.size(), condParams.length);
        if (queryParam.CHECK_ONLY_COUNT()) {
            String countSql = "select count(1) from (" + parsed.getSql() + ") must_alias";
            ResponseData<Integer> countData = dao.queryForValue(Integer.class, countSql, allParams);
            if (countData.isSuccess()) {
                int allSize = countData.getData() == null ? 0 : countData.getData();
                return ResponseData.success(new PageList<>(null, queryParam.START_INDEX(), queryParam.RESULT_NUM(), allSize));
            }
            return ResponseData.errorMsg("shard union count failed: " + countData.getMsg());
        }
        return dao.list(entityClass, parsed.getSql().toString(), allParams, queryParam.START_INDEX(), queryParam.RESULT_NUM(), queryParam.CHECK_AUTO_COUNT());
    }

}
