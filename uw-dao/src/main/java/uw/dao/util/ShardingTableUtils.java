package uw.dao.util;

import uw.dao.conf.DaoConfig.TableShardConfig;
import uw.dao.conf.DaoConfigManager;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 按日期分表的工具.
 *
 * @author axeon
 */
public class ShardingTableUtils {

    /**
     * DateTimeFormatter对象（包含年月日）.
     */
    public static final DateTimeFormatter FORMATTER_DAY = DateTimeFormatter.ofPattern( "yyyyMMdd" );
    /**
     * DateTimeFormatter对象（包含年月）.
     */
    public static final DateTimeFormatter FORMATTER_MONTH = DateTimeFormatter.ofPattern( "yyyyMM" );
    /**
     * DateTimeFormatter对象（包含年）.
     */
    public static final DateTimeFormatter FORMATTER_YEAR = DateTimeFormatter.ofPattern( "yyyy" );

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
        TableShardConfig config = DaoConfigManager.getTableShardingConfig( tableName );
        if (config != null && "id".equalsIgnoreCase( config.getShardType() )) {
            long batchSize = Long.parseLong( config.getShardRule() );
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
        TableShardConfig config = DaoConfigManager.getTableShardingConfig( tableName );
        if (config != null && "date".equalsIgnoreCase( config.getShardType() )) {
            switch (config.getShardRule()) {
                case "day":
                    return tableName + "_" + date.format( FORMATTER_DAY );
                case "month":
                    return tableName + "_" + date.format( FORMATTER_MONTH );
                case "year":
                    return tableName + "_" + date.format( FORMATTER_YEAR );
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
        return getTableNameByDate( tableName, date.toInstant().atZone( defaultZoneId ).withNano( 0 ).toLocalDate() );
    }

}
