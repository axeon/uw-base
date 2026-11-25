package uw.task.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.task.entity.TaskCronerConfig;
import uw.task.entity.TaskRunnerConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务配置的校验与规整（TaskRunnerConfig / TaskCronerConfig 共用一套规则）。
 *
 * <p>{@link #validate} 只报告不合法项，供 task-center 保存配置时拒绝非法输入；{@link #normalize} 把不合法项改成默认值并打一条 WARN，
 * 供任务执行侧在配置进入框架时调用。规则只认取值本身，不区分来源（initConfig 手填、task-center 下发、uw-dao 直接写字段加载的实体）。</p>
 *
 * @author axeon
 */
public final class TaskConfigValidator {

    /**
     * 日志器。
     */
    private static final Logger log = LoggerFactory.getLogger(TaskConfigValidator.class);

    /**
     * 工具类，禁止实例化。
     */
    private TaskConfigValidator() {
    }

    /**
     * 规整队列任务配置：非法取值就地改为默认值，有修正时打一条 WARN。
     *
     * @param config 队列任务配置
     * @return 被修正的字段说明（字段=原值→默认值）；全部合法返回空列表
     */
    public static List<String> normalize(TaskRunnerConfig config) {
        return warn("TaskRunnerConfig", config.getTaskClass(), fixRunner(config, true));
    }

    /**
     * 规整定时任务配置：非法取值就地改为默认值，有修正时打一条 WARN。
     *
     * @param config 定时任务配置
     * @return 被修正的字段说明（字段=原值→默认值）；全部合法返回空列表
     */
    public static List<String> normalize(TaskCronerConfig config) {
        return warn("TaskCronerConfig", config.getTaskClass(), fixCroner(config, true));
    }

    /**
     * 校验队列任务配置：只报告不合法项，不修改配置。
     *
     * @param config 队列任务配置
     * @return 不合法项（字段=原值→默认值）；全部合法返回空列表
     */
    public static List<String> validate(TaskRunnerConfig config) {
        return fixRunner(TaskRunnerConfig.builder(config).build(), false);
    }

    /**
     * 校验定时任务配置：只报告不合法项，不修改配置。
     *
     * @param config 定时任务配置
     * @return 不合法项（字段=原值→默认值）；全部合法返回空列表
     */
    public static List<String> validate(TaskCronerConfig config) {
        return fixCroner(TaskCronerConfig.builder(config).build(), false);
    }

    /**
     * 队列任务配置的规则。不在 setter 里做：uw-dao 读实体直接写字段、不经 setter。
     * id、taskClass、state 不是可调参数，不处理。
     *
     * @param c     配置
     * @param apply true=就地修正；false=只报告
     * @return 修正记录
     */
    private static List<String> fixRunner(TaskRunnerConfig c, boolean apply) {
        List<String> fixes = new ArrayList<>();
        c.setQueueType(range(c.getQueueType(), TaskRunnerConfig.QUEUE_TYPE_CLASSIC, TaskRunnerConfig.QUEUE_TYPE_QUORUM, TaskRunnerConfig.QUEUE_TYPE_QUORUM, "queueType", apply, fixes));
        c.setConsumerNum(min(c.getConsumerNum(), 1, TaskRunnerConfig.DEFAULT_CONSUMER_NUM, "consumerNum", apply, fixes));
        c.setPrefetchNum(min(c.getPrefetchNum(), 1, TaskRunnerConfig.DEFAULT_PREFETCH_NUM, "prefetchNum", apply, fixes));
        c.setRateLimitType(range(c.getRateLimitType(), TaskRunnerConfig.RATE_LIMIT_NONE, TaskRunnerConfig.RATE_LIMIT_GLOBAL_TASK_TAG_HOST, TaskRunnerConfig.RATE_LIMIT_NONE, "rateLimitType", apply, fixes));
        c.setRateLimitValue(min(c.getRateLimitValue(), 1, TaskRunnerConfig.DEFAULT_RATE_LIMIT_VALUE, "rateLimitValue", apply, fixes));
        c.setRateLimitTime(min(c.getRateLimitTime(), 1, TaskRunnerConfig.DEFAULT_RATE_LIMIT_TIME, "rateLimitTime", apply, fixes));
        c.setRateLimitWait(min(c.getRateLimitWait(), 0, TaskRunnerConfig.DEFAULT_RATE_LIMIT_WAIT, "rateLimitWait", apply, fixes));
        c.setRetryTimesByOverrated(min(c.getRetryTimesByOverrated(), 0, 0, "retryTimesByOverrated", apply, fixes));
        c.setRetryTimesByPartner(min(c.getRetryTimesByPartner(), 0, 0, "retryTimesByPartner", apply, fixes));
        c.setRetryTimesByProgram(min(c.getRetryTimesByProgram(), 0, 0, "retryTimesByProgram", apply, fixes));
        c.setRunTarget(nonBlank(c.getRunTarget(), TaskRunnerConfig.DEFAULT_RUN_TARGET, "runTarget", apply, fixes));
        c.setAlertFailRate(min(c.getAlertFailRate(), 0, 0, "alertFailRate", apply, fixes));
        c.setAlertFailPartnerRate(min(c.getAlertFailPartnerRate(), 0, 0, "alertFailPartnerRate", apply, fixes));
        c.setAlertFailProgramRate(min(c.getAlertFailProgramRate(), 0, 0, "alertFailProgramRate", apply, fixes));
        c.setAlertFailConfigRate(min(c.getAlertFailConfigRate(), 0, 0, "alertFailConfigRate", apply, fixes));
        c.setAlertFailDataRate(min(c.getAlertFailDataRate(), 0, 0, "alertFailDataRate", apply, fixes));
        c.setAlertQueueOversize(min(c.getAlertQueueOversize(), 0, 0, "alertQueueOversize", apply, fixes));
        c.setAlertQueueTimeout(min(c.getAlertQueueTimeout(), 0, 0, "alertQueueTimeout", apply, fixes));
        c.setAlertWaitTimeout(min(c.getAlertWaitTimeout(), 0, 0, "alertWaitTimeout", apply, fixes));
        c.setAlertRunTimeout(min(c.getAlertRunTimeout(), 0, 0, "alertRunTimeout", apply, fixes));
        c.setLogLevel(range(c.getLogLevel(), TaskRunnerConfig.TASK_LOG_TYPE_NONE, TaskRunnerConfig.TASK_LOG_TYPE_RECORD_ALL, TaskRunnerConfig.TASK_LOG_TYPE_RECORD, "logLevel", apply, fixes));
        c.setLogLimitSize(min(c.getLogLimitSize(), 0, 0, "logLimitSize", apply, fixes));
        return fixes;
    }

    /**
     * 定时任务配置的规则。cron 表达式不在此处理：非法表达式换成默认值会让任务按别的节奏跑，仍由调度容器拒绝注册。
     *
     * @param c     配置
     * @param apply true=就地修正；false=只报告
     * @return 修正记录
     */
    private static List<String> fixCroner(TaskCronerConfig c, boolean apply) {
        List<String> fixes = new ArrayList<>();
        c.setRunType(range(c.getRunType(), TaskCronerConfig.RUN_TYPE_ANYWAY, TaskCronerConfig.RUN_TYPE_SINGLETON, TaskCronerConfig.RUN_TYPE_SINGLETON, "runType", apply, fixes));
        c.setRunTarget(nonBlank(c.getRunTarget(), TaskCronerConfig.DEFAULT_RUN_TARGET, "runTarget", apply, fixes));
        c.setAlertFailRate(min(c.getAlertFailRate(), 0, 0, "alertFailRate", apply, fixes));
        c.setAlertFailPartnerRate(min(c.getAlertFailPartnerRate(), 0, 0, "alertFailPartnerRate", apply, fixes));
        c.setAlertFailProgramRate(min(c.getAlertFailProgramRate(), 0, 0, "alertFailProgramRate", apply, fixes));
        c.setAlertFailConfigRate(min(c.getAlertFailConfigRate(), 0, 0, "alertFailConfigRate", apply, fixes));
        c.setAlertFailDataRate(min(c.getAlertFailDataRate(), 0, 0, "alertFailDataRate", apply, fixes));
        c.setAlertWaitTimeout(min(c.getAlertWaitTimeout(), 0, 0, "alertWaitTimeout", apply, fixes));
        c.setAlertRunTimeout(min(c.getAlertRunTimeout(), 0, 0, "alertRunTimeout", apply, fixes));
        c.setLogLevel(range(c.getLogLevel(), TaskCronerConfig.TASK_LOG_TYPE_NONE, TaskCronerConfig.TASK_LOG_TYPE_RECORD_ALL, TaskCronerConfig.TASK_LOG_TYPE_RECORD, "logLevel", apply, fixes));
        c.setLogLimitSize(min(c.getLogLimitSize(), 0, 0, "logLimitSize", apply, fixes));
        return fixes;
    }

    /**
     * 不小于下限则原样保留，否则记一条并按 apply 决定回退默认值还是保留原值。
     *
     * @param value 取值
     * @param min   下限（含）
     * @param def   默认值
     * @param field 字段名
     * @param apply true=回退默认值；false=保留原值（只报告）
     * @param fixes 修正记录
     * @return 处理后的取值
     */
    private static int min(int value, int min, int def, String field, boolean apply, List<String> fixes) {
        if (value >= min) {
            return value;
        }
        fixes.add(field + "=" + value + "→" + def);
        return apply ? def : value;
    }

    /**
     * 在闭区间内则原样保留，否则记一条并按 apply 决定回退默认值还是保留原值。
     *
     * @param value 取值
     * @param min   下限（含）
     * @param max   上限（含）
     * @param def   默认值
     * @param field 字段名
     * @param apply true=回退默认值；false=保留原值（只报告）
     * @param fixes 修正记录
     * @return 处理后的取值
     */
    private static int range(int value, int min, int max, int def, String field, boolean apply, List<String> fixes) {
        if (value >= min && value <= max) {
            return value;
        }
        fixes.add(field + "=" + value + "→" + def);
        return apply ? def : value;
    }

    /**
     * 非空白则原样保留（去首尾空白），否则记一条并按 apply 决定回退默认值还是保留原值。
     *
     * @param value 取值
     * @param def   默认值
     * @param field 字段名
     * @param apply true=回退默认值；false=保留原值（只报告）
     * @param fixes 修正记录
     * @return 处理后的取值
     */
    private static String nonBlank(String value, String def, String field, boolean apply, List<String> fixes) {
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        fixes.add(field + "=" + value + "→" + def);
        return apply ? def : value;
    }

    /**
     * 有修正时汇总打一条 WARN。
     *
     * @param type      配置类型名
     * @param taskClass 任务类名
     * @param fixes     修正记录
     * @return 修正记录
     */
    private static List<String> warn(String type, String taskClass, List<String> fixes) {
        if (!fixes.isEmpty()) {
            log.warn("任务配置含非法取值，已回退默认值: type={}, taskClass={}, {}", type, taskClass, fixes);
        }
        return fixes;
    }
}
