package uw.dao.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uw.dao.vo.SqlExecuteStats;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DaoService统计列表有界化测试：触顶丢弃新统计，换出后恢复接收.
 */
class DaoServiceStatsTest {

    @BeforeEach
    void setUp() throws Exception {
        setStaticField("enableSqlStats", true);
        setStaticField("sqlCostMin", 100);
        resetStatsState();
    }

    @AfterEach
    void tearDown() throws Exception {
        resetStatsState();
        setStaticField("enableSqlStats", false);
    }

    /**
     * dataList触顶后丢弃新统计：getStatsList()换出的列表封顶于MAX_STATS_LIST_SIZE，
     * 换出后恢复接收（统计开关不因触顶失效）。
     */
    @Test
    void testLogStatsListIsBounded() throws Exception {
        int maxSize = (int) getStaticField("MAX_STATS_LIST_SIZE");
        for (int i = 0; i < maxSize + 50; i++) {
            DaoService.logStats(newStats(200));
        }
        ArrayList<SqlExecuteStats> list = DaoService.getStatsList();
        assertEquals(maxSize, list.size());
        // 换出后应恢复接收，且上一轮触顶丢弃不影响新一轮
        assertEquals(0, DaoService.getStatsList().size());
        DaoService.logStats(newStats(200));
        assertEquals(1, DaoService.getStatsList().size());
    }

    /**
     * 构造一条超过sqlCostMin阈值的统计（actionDate缺省，由logStats补齐为当前时间）.
     */
    private static SqlExecuteStats newStats(long allMillis) {
        return new SqlExecuteStats("testConn", 1, "select * from t where id=?", new Object[]{1}, 1, 1, 1, allMillis, null);
    }

    /**
     * 重置DaoService统计状态（dataList/dropped计数），隔离用例间静态状态.
     */
    private static void resetStatsState() throws Exception {
        Field dataListField = DaoService.class.getDeclaredField("dataList");
        dataListField.setAccessible(true);
        dataListField.set(null, new ArrayList<>());
        Field droppedField = DaoService.class.getDeclaredField("droppedStatsCount");
        droppedField.setAccessible(true);
        ((AtomicLong) droppedField.get(null)).set(0);
    }

    private static void setStaticField(String name, Object value) throws Exception {
        Field field = DaoService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    private static Object getStaticField(String name) throws Exception {
        Field field = DaoService.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

}
