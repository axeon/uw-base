package uw.task.util;

import uw.task.entity.TaskCronerStats;
import uw.task.entity.TaskRunnerStats;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.StampedLock;

public class TaskStatsService {


    /**
     * croner读写锁，用于提高性能。
     */
    private static StampedLock cronerStampedLock = new StampedLock();

    /**
     * croner数据存储map。
     */
    private static ConcurrentHashMap<Long, TaskCronerStats> cronerStatsMap = new ConcurrentHashMap<>();


    /**
     * runner读写锁，用于提高性能。
     */
    private static StampedLock runnerStampedLock = new StampedLock();

    /**
     * runner数据存储map。
     */
    private static ConcurrentHashMap<Long, TaskRunnerStats> runnerStatsMap = new ConcurrentHashMap<>();
    

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
        long stamp = cronerStampedLock.readLock();
        try {
            cronerStatsMap.compute(taskId, (k, v) -> {
                if (v != null) {
                    return v.addMetrics(numAll, numFailProgram, numFailConfig, numFailData, numFailPartner, timeWait, timeRun);
                } else {
                    return new TaskCronerStats(k).addMetrics(numAll, numFailProgram, numFailConfig, numFailData, numFailPartner, timeWait, timeRun);
                }
            });
        } finally {
            cronerStampedLock.unlockRead(stamp);
        }
    }

    /**
     * 获得CronerMetrics列表，同时清空map。
     *
     * @return
     */
    public static List<TaskCronerStats> getCronerStats() {
        long stamp = cronerStampedLock.writeLock();
        try {
            List<TaskCronerStats> list = new ArrayList<>(cronerStatsMap.size());
            list.addAll(cronerStatsMap.values());
            cronerStatsMap.clear();
            return list;
        } finally {
            cronerStampedLock.unlockWrite(stamp);
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
        long stamp = runnerStampedLock.readLock();
        try {
            runnerStatsMap.compute(taskId, (k, v) -> {
                if (v != null) {
                    return v.addMetrics(numAll, numFailProgram, numFailConfig, numFailData, numFailPartner, timeWaitQueue, timeWaitDelay, timeRun);
                } else {
                    return new TaskRunnerStats(k).addMetrics(numAll, numFailProgram, numFailConfig, numFailData, numFailPartner, timeWaitQueue, timeWaitDelay, timeRun);
                }
            });
        } finally {
            runnerStampedLock.unlockRead(stamp);
        }
    }

    /**
     * 获得RunnerMetrics列表，同时清空map。
     *
     * @return
     */
    public static List<TaskRunnerStats> getRunnerStats() {
        long stamp = runnerStampedLock.writeLock();
        try {
            List<TaskRunnerStats> list = new ArrayList<>(runnerStatsMap.size());
            list.addAll(runnerStatsMap.values());
            runnerStatsMap.clear();
            return list;
        } finally {
            runnerStampedLock.unlockWrite(stamp);
        }
    }

}
