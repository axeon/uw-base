package uw.dao.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.dao.DaoFactory;
import uw.dao.TransactionException;
import uw.dao.conf.DaoConfigManager;

import java.util.ArrayList;
import java.util.Collections;
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
     * 获取当前的表Set.
     *
     * @return HashSet对象
     */
    private HashSet<String> getCurrentTableSet() {
        HashSet<String> set = new HashSet<>();
        List<String> list = null;
        try {
            list = dao.queryForSingleList(dao.getConnectionName(DaoService.STATS_BASE_TABLE, "all"), String.class,
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
     * 每天凌晨3点半清理一下数据表.
     */
    @Override
    public void run() {
        logger.info("StatsInfo Clean Task is run start!");
        HashSet<String> tableSet = getCurrentTableSet();
        ArrayList<String> list = new ArrayList<>(tableSet);
        // 自然顺序排序
        Collections.sort(list);
        // 保留100天数据，假设
        int start = 100;
        try {
            start = DaoConfigManager.getConfig().getSqlStats().getDataKeepDays();
        } catch (Throwable ignored) {
        }
        // 循环删除过期数据
        for (int i = start; i < list.size(); i++) {
            try {
                dao.executeCommand("DROP TABLE IF EXISTS " + list.get(i));
                logger.info( "DROP TABLE IF EXISTS [{}].", list.get( i ) );
            } catch (TransactionException e) {
                logger.error(e.getMessage());
            }
        }
        logger.info("StatsInfo Clean Task is run end!");

    }

}
