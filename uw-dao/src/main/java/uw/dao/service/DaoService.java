package uw.dao.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.dao.DaoFactory;
import uw.dao.TransactionException;
import uw.dao.conf.DaoConfig;
import uw.dao.conf.DaoConfigManager;
import uw.dao.vo.SqlExecuteStats;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 性能计数器,将性能数据输出到mysql中.
 *
 * @author axeon
 */
public class DaoService {
    /**
     * 存储的表名.
     */
    public static final String STATS_BASE_TABLE = "dao_sql_stats";

    /**
     * 日志.
     */
    private static final Logger logger = LoggerFactory.getLogger(DaoService.class);

    /**
     * 定时任务执行。
     */
    private static ScheduledExecutorService executorService = null;

    /**
     * DAOFactory对象.
     */
    private static final DaoFactory dao = DaoFactory.getInstance();

    /**
     * 是否已经启动.
     */
    private static final AtomicBoolean isStarted = new AtomicBoolean(false);

    /**
     * 数据集合.
     */
    private static ArrayList<SqlExecuteStats> dataList = new ArrayList<>();

    /**
     * pageLog的写入锁.
     */
    private static final ReentrantLock locker = new ReentrantLock();

    /**
     * 可以记录的sqlCostMin最小时间。
     */
    private static int sqlCostMin = 100;

    /**
     * 是否开启sql统计。
     */
    private static boolean enableSqlStats = false;

    /**
     * 是否开启table sharding。
     */
    private static boolean enableTableShard = false;

    /**
     * 开始任务.
     */
    public static void start() {
        if (isStarted.compareAndSet(false, true)) {
            //获取配置。
            DaoConfig config = DaoConfigManager.getConfig();
            enableSqlStats = config.getSqlStats().isEnable();
            enableTableShard = config.getTableShard().size() > 0;
            sqlCostMin = DaoConfigManager.getConfig().getSqlStats().getSqlCostMin();
            //检测是否需要启动后台服务。
            if (enableSqlStats || enableTableShard) {
                executorService = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r);
                        t.setName("uw-dao.service");
                        t.setDaemon(true);
                        return t;
                    }
                });
                if (enableSqlStats) {
                    checkForCreatesStatsTable();
                    executorService.scheduleAtFixedRate(new StatsLogWriteTask(), 1, 30, TimeUnit.SECONDS);
                    executorService.scheduleAtFixedRate(new StatsCleanDataTask(), 3600, 86400, TimeUnit.SECONDS);
                }
                if (enableTableShard) {
                    executorService.scheduleAtFixedRate(new TableShardingTask(), 5, 3600, TimeUnit.SECONDS);
                }
            }
        }
    }

    /**
     * 停止任务.
     */
    public static void stop() {
        if (isStarted.compareAndSet(true, false)) {
            if (executorService != null) {
                executorService.shutdown();
            }
        }
    }

    /**
     * 是否已启动.
     *
     * @return boolean
     */
    public static boolean isStarted() {
        return isStarted.get();
    }

    /**
     * 记录性能参数.
     *
     * @param ses 用于统计sql执行的性能数据
     */
    public static void logStats(SqlExecuteStats ses) {
        if (enableSqlStats && ses.getAllTime() >= sqlCostMin) {
            ses.initActionDate();
            locker.lock();
            try {
                dataList.add(ses);
            } finally {
                locker.unlock();
            }
        }
    }

    /**
     * 获取sql执行列表，并重新构造列表.
     *
     * @return 列表
     */
    static ArrayList<SqlExecuteStats> getStatsList() {
        ArrayList<SqlExecuteStats> list = null;
        locker.lock();
        try {
            list = dataList;
            dataList = new ArrayList<>();
        } finally {
            locker.unlock();
        }
        return list;
    }

    /**
     * 检查是否应该新建表.
     */
    private static void checkForCreatesStatsTable() {
        String sql = "create table if not exists " + STATS_BASE_TABLE + " (\n"
                + "id bigint(20) NOT NULL AUTO_INCREMENT,\n" + "conn_name varchar(100) DEFAULT NULL,\n" + "conn_id int(11) DEFAULT NULL,\n"
                + "sql_info varchar(1000) DEFAULT NULL,\n" + "sql_param varchar(1000) DEFAULT NULL,\n"
                + "row_num int(11) DEFAULT NULL,\n" + "conn_time int(11) DEFAULT NULL,\n" + "db_time int(11) DEFAULT NULL,\n"
                + "all_time int(11) DEFAULT NULL,\n" + "exception varchar(500) DEFAULT NULL,\n"
                + "exe_date datetime DEFAULT NULL,\n" + "PRIMARY KEY (id)\n" + ") ENGINE=INNODB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPRESSED";
        try {
            dao.executeCommand(dao.getConnectionName(STATS_BASE_TABLE, "all"), sql);
            logger.info("init table: {}", STATS_BASE_TABLE);
        } catch (TransactionException e) {
            logger.error(e.getMessage(), e);
        }
    }

}
