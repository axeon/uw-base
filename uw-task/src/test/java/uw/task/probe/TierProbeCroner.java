package uw.task.probe;

import uw.task.TaskCroner;
import uw.task.entity.TaskContact;
import uw.task.entity.TaskCronerConfig;
import uw.task.entity.TaskCronerLog;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * dev 档位验证用的定时任务：每秒触发一次的单例任务（仅 Leader 实例执行），只计数。
 * <p>类在 {@code uw.task.probe} 包下，与探针的 task-project 前缀一致。不加 {@code @Component}：
 * 集成测试的应用扫描整个 {@code uw.task} 包，注解会被扫进去并注册到 dev 的 task-center；由探针配置类显式注册为 bean。</p>
 */
public class TierProbeCroner extends TaskCroner {

    /**
     * 实际执行次数。
     */
    public static final AtomicInteger RUNS = new AtomicInteger();

    /**
     * 最近一次执行的任务参数（用于确认是本任务在跑）。
     */
    public static volatile String lastParam;

    @Override
    public String runTask(TaskCronerLog taskCronerLog) {
        lastParam = taskCronerLog.getTaskParam();
        return "runs=" + RUNS.incrementAndGet();
    }

    @Override
    public TaskCronerConfig initConfig() {
        TaskCronerConfig config = new TaskCronerConfig();
        config.setTaskName("uw-task 档位验证定时任务");
        config.setTaskCron("*/1 * * * * *");
        config.setTaskParam("tier-probe");
        config.setRunType(TaskCronerConfig.RUN_TYPE_SINGLETON);
        config.setLogLevel(TaskCronerConfig.TASK_LOG_TYPE_NONE);
        return config;
    }

    @Override
    public TaskContact initContact() {
        return new TaskContact();
    }
}
