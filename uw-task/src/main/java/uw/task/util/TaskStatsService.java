package uw.task.util;

import uw.task.entity.TaskCronerStats;
import uw.task.entity.TaskRunnerStats;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.StampedLock;

public class TaskStatsService {


    /**
     * croner读写锁，用于提高性能。
     */
    private static final StampedLock CRONER_STAMPED_LOCK = new StampedLock();

    /**
     * croner数据存储map。
     */
    private static final ConcurrentHashMap<Long, TaskCronerStats> CRONER_STATS_MAP = new ConcurrentHashMap<>();


    /**
     * runner读写锁，用于提高性能。
     */
    private static final StampedLock RUNNER_STAMPED_LOCK = new StampedLock();

    /**
     * runner数据存储map。
     */
    private static final ConcurrentHashMap<Long, TaskRunnerStats> RUNNER_STATS_MAP = new ConcurrentHashMap<>();
    

    /**
     * 更新cronerMetrics。
     *
     * @param taskId
     * @param numAll
     * @param numFailProgram
     * @param numFailConfig
     * @param numFailData
     * @param numFailPartner
     * @param timeWait
     * @param timeRun
     */
    public static void updateCronerStats(long taskId, int numAll, int numFailProgram, int numFailConfig, int numFailData, int numFailPartner, int timeWait, int timeRun) {
        long stamp = CRONER_STAMPED_LOCK.readLock();
        try {
            CRONER_STATS_MAP.compute(taskId, (k, v) -> {
                if (v != null) {
                    return v.addMetrics(numAll, numFailProgram, numFailConfig, numFailData, numFailPartner, timeWait, timeRun);
                } else {
                    return new TaskCronerStats(k).addMetrics(numAll, numFailProgram, numFailConfig, numFailData, numFailPartner, timeWait, timeRun);
                }
            });
        } finally {
            CRONER_STAMPED_LOCK.unlockRead(stamp);
        }
    }

    /**
     * 获得CronerMetrics列表，同时清空map。
     *
     * @return
     */
    public static List<TaskCronerStats> getCronerStats() {
        long stamp = CRONER_STAMPED_LOCK.writeLock();
        try {
            List<TaskCronerStats> list = new ArrayList<>( CRONER_STATS_MAP.size());
            list.addAll( CRONER_STATS_MAP.values());
            CRONER_STATS_MAP.clear();
            return list;
        } finally {
            CRONER_STAMPED_LOCK.unlockWrite(stamp);
        }
    }


    /**
     * 更新RunnerMetrics.
     *
     * @param taskId
     * @param numAll
     * @param numFailProgram
     * @param numFailConfig
     * @param numFailData
     * @param numFailPartner
     * @param timeWaitQueue
     * @param timeWaitDelay
     * @param timeRun
     */
    public static void updateRunnerStats(long taskId, int numAll, int numFailProgram, int numFailConfig, int numFailData, int numFailPartner, int timeWaitQueue, int timeWaitDelay, int timeRun) {
        long stamp = RUNNER_STAMPED_LOCK.readLock();
        try {
            RUNNER_STATS_MAP.compute(taskId, (k, v) -> {
                if (v != null) {
                    return v.addMetrics(numAll, numFailProgram, numFailConfig, numFailData, numFailPartner, timeWaitQueue, timeWaitDelay, timeRun);
                } else {
                    return new TaskRunnerStats(k).addMetrics(numAll, numFailProgram, numFailConfig, numFailData, numFailPartner, timeWaitQueue, timeWaitDelay, timeRun);
                }
            });
        } finally {
            RUNNER_STAMPED_LOCK.unlockRead(stamp);
        }
    }

    /**
     * 获得RunnerMetrics列表，同时清空map。
     *
     * @return
     */
    public static List<TaskRunnerStats> getRunnerStats() {
        long stamp = RUNNER_STAMPED_LOCK.writeLock();
        try {
            List<TaskRunnerStats> list = new ArrayList<>( RUNNER_STATS_MAP.size());
            list.addAll( RUNNER_STATS_MAP.values());
            RUNNER_STATS_MAP.clear();
            return list;
        } finally {
            RUNNER_STAMPED_LOCK.unlockWrite(stamp);
        }
    }

}
