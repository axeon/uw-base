package uw.task.entity;

import org.junit.jupiter.api.Test;
import uw.task.util.TaskConfigValidator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 任务配置取值规整（不连 MQ/Redis）：非法取值回退默认值，合法取值原样保留，重复调用幂等。
 */
class TaskConfigNormalizeTest {

    /**
     * runner：枚举类越界与数值越界的全部字段都回退默认值。
     */
    @Test
    void runnerIllegalValues_fallBackToDefaults() {
        TaskRunnerConfig config = new TaskRunnerConfig();
        config.setQueueType(5);
        config.setConsumerNum(0);
        config.setPrefetchNum(-3);
        config.setRateLimitType(99);
        config.setRateLimitValue(0);
        config.setRateLimitTime(-1);
        config.setRateLimitWait(-30);
        config.setRetryTimesByOverrated(-1);
        config.setRetryTimesByPartner(-1);
        config.setRetryTimesByProgram(-1);
        config.setRunTarget("  ");
        config.setAlertFailRate(-1);
        config.setAlertQueueOversize(-1);
        config.setAlertWaitTimeout(-1);
        config.setLogLevel(9);
        config.setLogLimitSize(-5);
        TaskConfigValidator.normalize(config);
        assertThat(config.getQueueType()).isEqualTo(TaskRunnerConfig.QUEUE_TYPE_QUORUM);
        assertThat(config.getConsumerNum()).isEqualTo(TaskRunnerConfig.DEFAULT_CONSUMER_NUM);
        assertThat(config.getPrefetchNum()).isEqualTo(TaskRunnerConfig.DEFAULT_PREFETCH_NUM);
        assertThat(config.getRateLimitType()).isEqualTo(TaskRunnerConfig.RATE_LIMIT_NONE);
        assertThat(config.getRateLimitValue()).isEqualTo(TaskRunnerConfig.DEFAULT_RATE_LIMIT_VALUE);
        assertThat(config.getRateLimitTime()).isEqualTo(TaskRunnerConfig.DEFAULT_RATE_LIMIT_TIME);
        assertThat(config.getRateLimitWait()).isEqualTo(TaskRunnerConfig.DEFAULT_RATE_LIMIT_WAIT);
        assertThat(config.getRetryTimesByOverrated()).isZero();
        assertThat(config.getRetryTimesByPartner()).isZero();
        assertThat(config.getRetryTimesByProgram()).isZero();
        assertThat(config.getRunTarget()).isEqualTo(TaskRunnerConfig.DEFAULT_RUN_TARGET);
        assertThat(config.getAlertFailRate()).isZero();
        assertThat(config.getAlertQueueOversize()).isZero();
        assertThat(config.getAlertWaitTimeout()).isZero();
        assertThat(config.getLogLevel()).isEqualTo(TaskRunnerConfig.TASK_LOG_TYPE_RECORD);
        assertThat(config.getLogLimitSize()).isZero();
    }

    /**
     * runner：合法取值（含边界值）原样保留；规整过的配置再规整不再变化。
     */
    @Test
    void runnerLegalValues_kept() {
        TaskRunnerConfig config = TaskRunnerConfig.builder().queueType(TaskRunnerConfig.QUEUE_TYPE_CLASSIC).consumerNum(1).prefetchNum(1)
                .rateLimitType(TaskRunnerConfig.RATE_LIMIT_GLOBAL_TASK_TAG_HOST).rateLimitValue(1).rateLimitTime(1).rateLimitWait(0)
                .retryTimesByPartner(3).logLevel(TaskRunnerConfig.TASK_LOG_TYPE_NONE).build();
        assertThat(config.getQueueType()).isZero();
        assertThat(config.getConsumerNum()).isEqualTo(1);
        assertThat(config.getPrefetchNum()).isEqualTo(1);
        assertThat(config.getRateLimitType()).isEqualTo(TaskRunnerConfig.RATE_LIMIT_GLOBAL_TASK_TAG_HOST);
        assertThat(config.getRetryTimesByPartner()).isEqualTo(3);
        assertThat(config.getLogLevel()).isEqualTo(TaskRunnerConfig.TASK_LOG_TYPE_NONE);
        TaskRunnerConfig again = TaskRunnerConfig.builder(config).queueType(7).build();
        assertThat(again.getQueueType()).isEqualTo(7);
        assertThat(TaskConfigValidator.normalize(again)).containsExactly("queueType=7→1");
        assertThat(again.getQueueType()).isEqualTo(TaskRunnerConfig.QUEUE_TYPE_QUORUM);
        assertThat(again.getConsumerNum()).isEqualTo(1);
        assertThat(TaskConfigValidator.normalize(again)).as("重复规整不再有修正").isEmpty();
    }

    /**
     * croner：runType 越界、阈值为负、runTarget 空白、logLevel 越界都回退默认值。
     */
    @Test
    void cronerIllegalValues_fallBackToDefaults() {
        TaskCronerConfig config = new TaskCronerConfig();
        config.setRunType(7);
        config.setRunTarget(null);
        config.setAlertFailRate(-1);
        config.setAlertRunTimeout(-1);
        config.setLogLevel(4);
        config.setLogLimitSize(-1);
        TaskConfigValidator.normalize(config);
        assertThat(config.getRunType()).isEqualTo(TaskCronerConfig.RUN_TYPE_SINGLETON);
        assertThat(config.getRunTarget()).isEqualTo(TaskCronerConfig.DEFAULT_RUN_TARGET);
        assertThat(config.getAlertFailRate()).isZero();
        assertThat(config.getAlertRunTimeout()).isZero();
        assertThat(config.getLogLevel()).isEqualTo(TaskCronerConfig.TASK_LOG_TYPE_RECORD);
        assertThat(config.getLogLimitSize()).isZero();
    }

    /**
     * cron 表达式不归规整管：非法表达式由调度容器拒绝注册，这里不能悄悄换成默认表达式（会让任务按别的节奏跑起来）。
     */
    @Test
    void cronerInvalidCron_notTouched() {
        TaskCronerConfig config = new TaskCronerConfig();
        config.setTaskCron("not-a-cron");
        TaskConfigValidator.normalize(config);
        assertThat(config.getTaskCron()).isEqualTo("not-a-cron");
    }

    /**
     * 只校验不修改（供 task-center 保存时拒绝非法输入）：报告全部不合法项，原配置保持不变；全合法返回空列表。
     */
    @Test
    void validate_reportsWithoutModifying() {
        TaskRunnerConfig runner = new TaskRunnerConfig();
        runner.setQueueType(5);
        runner.setPrefetchNum(0);
        runner.setRunTarget("");
        assertThat(TaskConfigValidator.validate(runner)).containsExactly("queueType=5→1", "prefetchNum=0→10", "runTarget=→default");
        assertThat(runner.getQueueType()).isEqualTo(5);
        assertThat(runner.getPrefetchNum()).isZero();
        TaskCronerConfig croner = new TaskCronerConfig();
        croner.setRunType(3);
        croner.setLogLevel(-2);
        assertThat(TaskConfigValidator.validate(croner)).containsExactly("runType=3→1", "logLevel=-2→0");
        assertThat(croner.getRunType()).isEqualTo(3);
        assertThat(TaskConfigValidator.validate(new TaskCronerConfig())).isEmpty();
    }

    /**
     * 配置进入框架的唯一入口（setRunnerConfig / setCronerConfig）会就地规整：经 TaskMetaInfoManager 写入的配置（模拟 task-center 下发或 uw-dao 直接写字段加载的实体）同样被纠正。
     */
    @Test
    void metaInfoManager_normalizesOnPut() {
        TaskRunnerConfig runner = new TaskRunnerConfig();
        runner.setTaskClass("com.demo.NeedsFixRunner");
        runner.setQueueType(5);
        TaskCronerConfig croner = new TaskCronerConfig();
        croner.setTaskClass("com.demo.NeedsFixCroner");
        croner.setRunType(42);
        uw.task.conf.TaskMetaInfoManager meta = new uw.task.conf.TaskMetaInfoManager(null, new uw.task.conf.UwTaskProperties());
        meta.setRunnerConfig(meta.getRunnerConfigKey(runner), runner);
        meta.setCronerConfig(meta.getCronerConfigKey(croner), croner);
        assertThat(meta.getRunnerConfigMap().get(meta.getRunnerConfigKey(runner)).getQueueType()).isEqualTo(TaskRunnerConfig.QUEUE_TYPE_QUORUM);
        assertThat(meta.getCronerConfigMap().get(meta.getCronerConfigKey(croner)).getRunType()).isEqualTo(TaskCronerConfig.RUN_TYPE_SINGLETON);
    }
}
