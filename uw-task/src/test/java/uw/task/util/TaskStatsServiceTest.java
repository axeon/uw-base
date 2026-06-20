package uw.task.util;

import org.junit.jupiter.api.Test;
import uw.task.entity.TaskCronerStats;
import uw.task.entity.TaskRunnerStats;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link TaskStatsService} 的纯单元测试，验证累加、采集清空、taskId 隔离。
 *
 * <p>该服务为静态聚合器、纯内存、无外部依赖，适合直接单测。</p>
 *
 * @author axeon
 */
class TaskStatsServiceTest {

    @Test
    void updateRunnerStats_accumulatesByTaskId() {
        // 预清空，避免与其他测试串扰（静态 map）
        TaskStatsService.getRunnerStats();

        TaskStatsService.updateRunnerStats(100L, 10, 1, 2, 0, 3, 100, 200, 300);
        TaskStatsService.updateRunnerStats(100L, 5, 0, 1, 0, 1, 50, 60, 70);

        List<TaskRunnerStats> list = TaskStatsService.getRunnerStats();
        assertEquals(1, list.size());
        TaskRunnerStats stats = list.get(0);
        assertEquals(100L, stats.getTaskId());
        assertEquals(15, stats.getNumAll());
        assertEquals(1, stats.getNumFailProgram());
        assertEquals(3, stats.getNumFailConfig());
        assertEquals(1, stats.getNumFailPartner());
        assertEquals(150, stats.getTimeWaitQueue());
        assertEquals(260, stats.getTimeWaitDelay());
        assertEquals(370, stats.getTimeRun());
    }

    @Test
    void getRunnerStats_clearsMapAfterRead() {
        TaskStatsService.getRunnerStats();
        TaskStatsService.updateRunnerStats(200L, 1, 0, 0, 0, 0, 0, 0, 0);

        assertEquals(1, TaskStatsService.getRunnerStats().size());
        // 二次采集应为空（已被清空）
        assertTrue(TaskStatsService.getRunnerStats().isEmpty());
    }

    @Test
    void runnerStats_isolatesByTaskId() {
        TaskStatsService.getRunnerStats();
        TaskStatsService.updateRunnerStats(1L, 1, 0, 0, 0, 0, 0, 0, 0);
        TaskStatsService.updateRunnerStats(2L, 1, 0, 0, 0, 0, 0, 0, 0);

        List<TaskRunnerStats> list = TaskStatsService.getRunnerStats();
        assertEquals(2, list.size());
        assertTrue(list.stream().anyMatch(s -> s.getTaskId() == 1L));
        assertTrue(list.stream().anyMatch(s -> s.getTaskId() == 2L));
    }

    @Test
    void cronerStats_accumulatesAndClears() {
        TaskStatsService.getCronerStats();
        TaskStatsService.updateCronerStats(300L, 8, 1, 1, 1, 1, 40, 50);

        List<TaskCronerStats> list = TaskStatsService.getCronerStats();
        assertEquals(1, list.size());
        TaskCronerStats stats = list.get(0);
        assertEquals(300L, stats.getTaskId());
        assertEquals(8, stats.getNumAll());
        assertEquals(1, stats.getNumFailProgram());

        assertTrue(TaskStatsService.getCronerStats().isEmpty());
    }

}
