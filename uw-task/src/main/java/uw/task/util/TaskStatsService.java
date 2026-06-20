package uw.task.util;

import uw.task.entity.TaskCronerStats;
import uw.task.entity.TaskRunnerStats;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务运行统计聚合服务。
 *
 * <p>按 taskId 聚合 croner / runner 的执行计数与耗时，供主机状态报告周期性采集上报。</p>
 *
 * <h3>并发模型</h3>
 * 单读多写：{@code updateXxxStats} 由多个任务执行线程并发调用，{@code getXxxStats} 仅由主机状态上报
 * 的单一调度线程（每 180 秒一次）调用。
 * <ul>
 *   <li><b>写（update）</b>：{@link ConcurrentHashMap#compute} 保证同一 taskId 的累加串行化，
 *       叠加 {@link TaskCronerStats}/{@link TaskRunnerStats} 内部 {@code AtomicInteger} 字段，
 *       多线程累加无需外部锁即可保证原子性与可见性。</li>
 *   <li><b>读（get）</b>：{@code new ArrayList<>(map.values())} 拷贝弱一致快照后 {@code clear()}，
 *       两步非原子。<b>已知并接受的漂移</b>：若快照遍历完成到 clear 之间恰好有写线程写入新样本，
 *       该样本既未被本次快照采集、又被 clear 清除，会丢失（少上报一次单次执行计数）。
 *       监控场景下此概率极低且对曲线/告警无实质影响，故不引入外部锁——
 *       避免写端（高频任务执行路径）被全局锁串行化。</li>
 * </ul>
 * <p><b>复用约束</b>：本服务面向监控聚合，接受样本丢失。若未来复用到不允许丢计数的场景（对账、计费等），
 * 需改为加锁或"读时原子转移"语义。</p>
 */
public class TaskStatsService {

    /**
     * croner数据存储map。
     */
    private static final ConcurrentHashMap<Long, TaskCronerStats> CRONER_STATS_MAP = new ConcurrentHashMap<>();

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
        CRONER_STATS_MAP.compute(taskId, (k, v) -> {
            if (v != null) {
                return v.addMetrics(numAll, numFailProgram, numFailConfig, numFailData, numFailPartner, timeWait, timeRun);
            } else {
                return new TaskCronerStats(k).addMetrics(numAll, numFailProgram, numFailConfig, numFailData, numFailPartner, timeWait, timeRun);
            }
        });
    }

    /**
     * 获取CronerMetrics列表，同时清空map。
     *
     * @return
     */
    public static List<TaskCronerStats> getCronerStats() {
        List<TaskCronerStats> list = new ArrayList<>(CRONER_STATS_MAP.values());
        CRONER_STATS_MAP.clear();
        return list;
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
        RUNNER_STATS_MAP.compute(taskId, (k, v) -> {
            if (v != null) {
                return v.addMetrics(numAll, numFailProgram, numFailConfig, numFailData, numFailPartner, timeWaitQueue, timeWaitDelay, timeRun);
            } else {
                return new TaskRunnerStats(k).addMetrics(numAll, numFailProgram, numFailConfig, numFailData, numFailPartner, timeWaitQueue, timeWaitDelay, timeRun);
            }
        });
    }

    /**
     * 获取RunnerMetrics列表，同时清空map。
     *
     * @return
     */
    public static List<TaskRunnerStats> getRunnerStats() {
        List<TaskRunnerStats> list = new ArrayList<>(RUNNER_STATS_MAP.values());
        RUNNER_STATS_MAP.clear();
        return list;
    }

}
