package uw.task.conf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.response.ResponseData;
import uw.common.util.SystemClock;
import uw.task.TaskCroner;
import uw.task.TaskRunner;
import uw.task.api.TaskApiClient;
import uw.task.TaskDelayer;
import uw.task.container.TaskDelayerContainer;
import uw.task.container.TaskCronerContainer;
import uw.task.container.TaskRunnerContainer;
import uw.task.entity.*;
import uw.task.util.TaskStatsService;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 任务服务注册器：负责与 task-center 交互，拉取/上传 runner、croner、delayer 三类任务的动态配置，
 * 并驱动各自容器的启动、热更新与暂停。
 *
 * <p>由 {@link TaskAutoConfiguration} 的调度线程池周期性调用（默认 60s）：
 * {@link #updateConfig} 编排三类配置的拉取与刷新，分别委托给三个独立方法——
 * {@link #updateTaskRunnerConfig}（基于 {@link #runnerLastUpdateTime} 增量拉取 runner 配置，首次上传本地默认配置并注册队列监听）、
 * {@link #updateTaskCronerConfig}（基于 {@link #cronerLastUpdateTime} 增量拉取 croner 配置，首次上传本地默认配置并配置调度）、
 * {@link #updateTaskDelayerConfig}（基于 {@link #delayerLastUpdateTime} 增量拉取 delayer 配置，首次上传本地默认配置并启动 poll handle）。
 * 三者各自维护独立的时间戳与 fail-fast，互不影响：runner/croner 拉取失败保留上次配置不更新，delayer 拉取失败仅 warn。
 * {@link #reportHostInfo} 上报 JVM/线程/统计/队列积压等信息。</p>
 *
 * @author axeon
 */
public class TaskServiceRegistrar {

    /**
     * 日志器。
     */
    private static final Logger log = LoggerFactory.getLogger(TaskServiceRegistrar.class);

    /**
     * 队列任务容器。
     */
    private final TaskRunnerContainer taskRunnerContainer;

    /**
     * 延迟任务容器。
     */
    private final TaskDelayerContainer taskDelayerContainer;

    /**
     * 定时任务容器。
     */
    private final TaskCronerContainer taskCronerContainer;

    /**
     * 任务配置。
     */
    private final TaskProperties taskProperties;

    /**
     * 任务中心 API 客户端。
     */
    private final TaskApiClient taskApiClient;

    /**
     * 任务meta信息管理器。
     */
    private final TaskMetaInfoManager taskMetaInfoManager;

    /**
     * runner 配置增量拉取时间戳：仅 runner 拉取成功时推进，与 croner/delayer 解耦。
     * 初始值必须=0，用于标识整体加载。
     */
    private volatile long runnerLastUpdateTime = 0;

    /**
     * croner 配置增量拉取时间戳：仅 croner 拉取成功时推进，与 runner/delayer 解耦。
     * 初始值必须=0，用于标识整体加载。
     */
    private volatile long cronerLastUpdateTime = 0;

    /**
     * 延迟任务增量拉取独立时间戳：仅 delayer 拉取成功时推进，与 runner/croner 的 lastUpdateTime 解耦，
     * 避免 runner/croner 成功推进后 delayer 拉取失败 导致 delayer 配置变更丢窗。
     */
    private volatile long delayerLastUpdateTime = 0;

    /**
     * runner 首次启动标志：首次拉取配置成功后上传本地默认配置。
     */
    private boolean runnerFirstRun = true;

    /**
     * croner 首次启动标志：首次拉取配置成功后上传本地默认配置。
     */
    private boolean cronerFirstRun = true;

    /**
     * 延迟任务配置首次上传标志。
     */
    private boolean delayerFirstRun = true;

    /**
     * 三类任务是否处于本地降级模式（center 不可用时用 initConfig 默认配置启动）。
     * 拉取失败+首次进入降级；拉取成功退出降级（center 配置覆盖/重建）。各自独立，互不影响。
     */
    private volatile boolean runnerDegraded = false;
    private volatile boolean cronerDegraded = false;
    private volatile boolean delayerDegraded = false;

    /**
     * 上次报告结果。
     */
    private HostReportResponse reportResponse;

    /**
     * 主机是否被服务端禁用（HostReportResponse.state=0）。禁用时停所有任务容器（保留底层线程池）、updateConfig 跳过；
     * 解除禁用(state=1)后清标志并重置增量时间戳，下次 updateConfig 全量拉取重新注册所有任务，自动恢复。
     */
    private volatile boolean hostDisabled = false;

    /**
     * 配置更新与主机禁用/启用的互斥锁：保护 updateConfig 与 reportHostInfo 的停机/恢复段，
     * 防止并发踩踏（suspendAll/destroyAll 与 registerQueueListener/configureTask 交错致被禁主机残留资源）。
     * 可重入：reportHostInfo 恢复段持锁后内嵌调 updateConfig（同线程重入），不阻塞自己。
     */
    private final ReentrantLock configLock = new ReentrantLock();

    /**
     * 默认构造器。
     *
     * @param taskProperties       任务配置
     * @param taskApiClient        任务中心 API 客户端
     * @param taskRunnerContainer  队列任务容器
     * @param taskCronerContainer  定时任务容器
     * @param taskDelayerContainer 延迟任务容器
     * @param taskMetaInfoManager  任务元信息管理器
     */
    public TaskServiceRegistrar(TaskProperties taskProperties, TaskApiClient taskApiClient, TaskRunnerContainer taskRunnerContainer, TaskCronerContainer taskCronerContainer,
                                TaskDelayerContainer taskDelayerContainer, TaskMetaInfoManager taskMetaInfoManager) {
        this.taskProperties = taskProperties;
        this.taskRunnerContainer = taskRunnerContainer;
        this.taskCronerContainer = taskCronerContainer;
        this.taskDelayerContainer = taskDelayerContainer;
        this.taskApiClient = taskApiClient;
        this.taskMetaInfoManager = taskMetaInfoManager;
    }

    /**
     * 是否开启任务注册（注册主机并执行任务的开关）。
     *
     * @return 开启返回 true
     */
    public boolean isEnableRegistry() {
        return taskProperties.isEnableRegistry();
    }


    /**
     * 注册并刷新当前主机的任务配置。
     * <p>由调度器每分钟调用：编排 runner/croner/delayer 三类任务配置的拉取与刷新。
     * 各类配置使用独立的时间戳与 fail-fast，互不影响。</p>
     */
    public void updateConfig() {
        configLock.lock();
        try {
            if (hostDisabled) {
                return;
            }
            updateTaskRunnerConfig();
            updateTaskCronerConfig();
            updateTaskDelayerConfig();
        } finally {
            configLock.unlock();
        }
    }

    /**
     * 拉取并应用 TaskRunner 配置增量。
     * <p>从 task-center 拉取 runner 配置增量，首次启动时上传本地默认配置，
     * 随后按 taskProject+runTarget 过滤、注册并启动 runner 队列监听。
     * 拉取失败时进入 fail-fast，保留上次配置不更新。</p>
     */
    private void updateTaskRunnerConfig() {
        long startUpdateTimeMills = SystemClock.now();
        // 优先拉取runnerConfig，因为是否注册任务，都要更新系统队列。
        ConcurrentHashMap<String, TaskRunnerConfig> updatedRunnerConfigMap = fetchRunnerConfig();
        if (updatedRunnerConfigMap == null) {
            // 注册主机 + 首次拉取失败（从未拿到配置）+ 未降级 → 本地降级启动（initConfig 默认配置注册 listener）
            if (taskProperties.isEnableRegistry() && runnerFirstRun && !runnerDegraded) {
                log.warn("!拉取TaskRunner配置失败，启用本地降级模式（initConfig 默认配置）！");
                startRunnersLocal();
                runnerDegraded = true;
            }
            log.warn("!拉取TaskRunner服务器配置失败, 进入Fail-Fast模式!");
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("发现服务器端拉取的[{}]个更新的TaskRunner配置...", updatedRunnerConfigMap.size());
        }
        //非后台任务主机，此时可以退出了。
        if (!taskProperties.isEnableRegistry()) {
            // 非注册主机也推进时间戳，避免每周期从 0 全量重拉（仍需 runner 配置用于 getFitQueue 路由）
            runnerLastUpdateTime = startUpdateTimeMills - 5000;
            return;
        }
        // 降级 → 恢复切换：清降级 listener（unacked 消息 RabbitMQ 重新入队不丢），主循环用 center 配置重建
        if (runnerDegraded) {
            log.warn("!TaskRunner center 恢复，从降级模式切换到 center 配置！");
            taskRunnerContainer.suspendAll();
            runnerDegraded = false;
        }
        // 第一次执行初始化操作
        if (runnerFirstRun) {
            runnerFirstRun = false;
            // 获取当前主机上所有的TaskRunner
            for (Entry<String, TaskRunner> kv : taskMetaInfoManager.getRunnerInstanceMap().entrySet()) {
                try {
                    // 拿到任务类名
                    TaskRunner<?, ?> taskRunner = kv.getValue();
                    String taskClass = kv.getKey();
                    TaskRunnerConfig localConfig = taskRunner.initConfig();
                    TaskContact contact = taskRunner.initContact();
                    if (localConfig == null || contact == null) {
                        log.warn("!TaskRunner: [{}]默认配置或联系人信息为空，无法启动！", taskClass);
                        continue;
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("正在初始化TaskRunner: [{}].", taskClass);
                    }
                    // 防止有人瞎胡搞，任务参数强制设死。
                    localConfig.setTaskClass(taskClass);
                    localConfig.setRunTarget(taskProperties.getRunTarget());
                    contact.setTaskClass(taskClass);
                    String configKey = taskMetaInfoManager.getRunnerConfigKey(localConfig);
                    TaskRunnerConfig serverConfig = updatedRunnerConfigMap.get(configKey);
                    // 上传配置
                    if (serverConfig == null) {
                        if (log.isDebugEnabled()) {
                            log.debug("TaskRunner: [{}]未找到服务器端配置，上传默认配置...", taskClass);
                        }
                        serverConfig = uploadRunnerInfo(localConfig, contact);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        // 改Runner配置，逐一循环并配置。
        for (TaskRunnerConfig taskRunnerConfig : taskMetaInfoManager.getRunnerConfigMap().values()) {
            //必须是本项目任务，否则跳过
            if (!taskRunnerConfig.getTaskClass().startsWith(taskProperties.getTaskProject())) {
                if (log.isDebugEnabled()) {
                    log.debug("因为TaskProject不匹配，跳过配置ID: [{}], RUNNER: [{}].", taskRunnerConfig.getId(), taskRunnerConfig.getTaskClass());
                }
                continue;
            }
            //必须是同一个runTarget项目，否则跳过
            if (!taskRunnerConfig.getRunTarget().equals(taskProperties.getRunTarget())) {
                if (log.isDebugEnabled()) {
                    log.debug("因为RunTarget不匹配，跳过配置ID: [{}], RUNNER: [{}].", taskRunnerConfig.getId(), taskRunnerConfig.getTaskClass());
                }
                continue;
            }
            if (log.isDebugEnabled()) {
                log.debug("正在加载ID: [{}], RUNNER: [{}].", taskRunnerConfig.getId(), taskRunnerConfig.getTaskClass());
            }
            if (taskMetaInfoManager.getRunnerInstance(taskRunnerConfig.getTaskClass()) != null) {
                // 启动任务
                taskRunnerContainer.registerQueueListener(taskRunnerConfig);
            } else {
                log.warn("!未找到匹配的任务类，未能启动配置ID: [{}], RUNNER: [{}].", taskRunnerConfig.getId(), taskRunnerConfig.getTaskClass());
            }
        }

        // 执行成功才会更新时间戳
        // 比对数据库的时候，因为数据库缺少ms值，所以减去5000，提高匹配度。
        runnerLastUpdateTime = startUpdateTimeMills - 5000;
    }

    /**
     * 拉取并应用 TaskCroner 配置增量。
     * <p>从 task-center 拉取 croner 配置增量，首次启动时上传本地默认配置，
     * 随后按 taskProject+runTarget 过滤、配置并启动 croner 调度。
     * 拉取失败时进入 fail-fast，保留上次配置不更新。</p>
     */
    private void updateTaskCronerConfig() {
        long startUpdateTimeMills = SystemClock.now();
        // 取得有变化的croner配置列表
        ConcurrentHashMap<String, TaskCronerConfig> updatedCronerConfigMap = fetchCronerConfig();
        if (updatedCronerConfigMap == null) {
            if (taskProperties.isEnableRegistry() && cronerFirstRun && !cronerDegraded) {
                log.warn("!拉取TaskCroner配置失败，启用本地降级模式（initConfig 默认配置）！");
                startCronersLocal();
                cronerDegraded = true;
            }
            log.warn("!拉取TaskCroner服务器配置失败, 进入Fail-Fast模式!");
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("发现服务器端拉取的[{}]个更新的TaskCroner配置...", updatedCronerConfigMap.size());
        }
        //非后台任务主机，此时可以退出了。
        if (!taskProperties.isEnableRegistry()) {
            // 非注册主机也推进时间戳，避免每周期从 0 全量重拉
            cronerLastUpdateTime = startUpdateTimeMills - 5000;
            return;
        }
        // 降级 → 恢复切换：Croner configKey 覆盖（主循环 configureTask 内 stopTask cancel(false) 让在途跑完 + schedule new），不清 map
        if (cronerDegraded) {
            log.warn("!TaskCroner center 恢复，从降级模式切换到 center 配置！");
            cronerDegraded = false;
        }
        // 第一次执行初始化操作
        if (cronerFirstRun) {
            cronerFirstRun = false;
            // 初始化croner
            for (Entry<String, TaskCroner> kv : taskMetaInfoManager.getCronerInstanceMap().entrySet()) {
                try {
                    // 拿到任务类名
                    TaskCroner taskCroner = kv.getValue();
                    String taskClass = taskCroner.getClass().getName();
                    if (log.isDebugEnabled()) {
                        log.debug("正在初始化TaskCroner: [{}].", taskClass);
                    }
                    TaskCronerConfig localConfig = taskCroner.initConfig();
                    TaskContact contact = taskCroner.initContact();
                    if (localConfig == null || contact == null) {
                        log.warn("!TaskCroner: [{}]默认配置或联系人信息为空，无法启动！", taskClass);
                        continue;
                    }
                    // 防止有人瞎胡搞，任务参数强制设死。
                    localConfig.setTaskClass(taskClass);
                    localConfig.setRunTarget(taskProperties.getRunTarget());
                    contact.setTaskClass(taskClass);
                    String configKey = taskMetaInfoManager.getCronerConfigKey(localConfig);
                    TaskCronerConfig serverConfig = updatedCronerConfigMap.get(configKey);
                    // 上传配置
                    if (serverConfig == null) {
                        if (log.isDebugEnabled()) {
                            log.debug("TaskCroner: [{}]未找到服务器端配置，上传默认配置...", taskClass);
                        }
                        serverConfig = uploadCronerInfo(localConfig, contact);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        // 此时执行更新操作
        for (TaskCronerConfig taskCronerConfig : taskMetaInfoManager.getCronerConfigMap().values()) {
            //必须是本项目任务，否则跳过
            if (!taskCronerConfig.getTaskClass().startsWith(taskProperties.getTaskProject())) {
                if (log.isDebugEnabled()) {
                    log.debug("因为TaskProject不匹配，跳过配置ID: [{}], CRONER:[{}], CRON:[{}].", taskCronerConfig.getId(), taskCronerConfig.getTaskClass(),
                            taskCronerConfig.getTaskCron());
                }
                continue;
            }
            //必须是同一个runTarget项目，否则跳过
            if (!taskCronerConfig.getRunTarget().equals(taskProperties.getRunTarget())) {
                if (log.isDebugEnabled()) {
                    log.debug("因为RunTarget不匹配，跳过配置ID: [{}], CRONER:[{}], CRON:[{}].", taskCronerConfig.getId(), taskCronerConfig.getTaskClass(),
                            taskCronerConfig.getTaskCron());
                }
                continue;
            }
            if (log.isDebugEnabled()) {
                log.debug("正在加载ID: [{}], CRONER:[{}], CRON:[{}].", taskCronerConfig.getId(), taskCronerConfig.getTaskClass(), taskCronerConfig.getTaskCron());
            }
            TaskCroner taskCroner = taskMetaInfoManager.getCronerInstance(taskCronerConfig.getTaskClass());
            if (taskCroner != null) {
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("正在配置ID: [{}], CRONER:[{}], CRON:[{}].", taskCronerConfig.getId(), taskCronerConfig.getTaskClass(), taskCronerConfig.getTaskCron());
                    }
                    boolean runFlag = taskCronerContainer.configureTask(taskCroner, taskCronerConfig);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            } else {
                log.warn("!未找到匹配的任务类，未能启动配置ID: [{}], CRONER:[{}], CRON:[{}].", taskCronerConfig.getId(), taskCronerConfig.getTaskClass(), taskCronerConfig.getTaskCron());
            }
        }

        // 执行成功才会更新时间戳
        // 比对数据库的时候，因为数据库缺少ms值，所以减去5000，提高匹配度。
        cronerLastUpdateTime = startUpdateTimeMills - 5000;
    }

    /**
     * 拉取并应用 TaskDelayer 配置增量。
     * <p>从 task-center 拉取 delayer 配置增量，首次启动时上传本地默认配置，
     * 随后按 taskProject+runTarget 过滤、配置并启动 delayer poll handle。
     * 独立于 runner/croner 的 fail-fast：delayer 拉取失败仅 warn，不影响主流程。</p>
     */
    private void updateTaskDelayerConfig() {
        long startUpdateTimeMills = SystemClock.now();
        // 取得有变化的delayer配置列表（增量已合并进 delayerConfigMap，state<1 已 remove + stopDelayer）
        ConcurrentHashMap<String, TaskDelayerConfig> updatedDelayerConfigMap = fetchDelayerConfig();
        if (updatedDelayerConfigMap == null) {
            // 注册主机 + 首次拉取失败（从未拿到配置）+ 未降级 → 本地降级启动（initConfig 默认配置）
            if (taskProperties.isEnableRegistry() && delayerFirstRun && !delayerDegraded) {
                log.warn("!拉取TaskDelayer配置失败，启用本地降级模式（initConfig 默认配置）！");
                startDelayersLocal();
                delayerDegraded = true;
            }
            log.warn("!拉取TaskDelayer服务器配置失败!");
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("发现服务器端拉取的[{}]个更新的TaskDelayer配置...", updatedDelayerConfigMap.size());
        }
        //非后台任务主机，此时可以退出了。
        if (!taskProperties.isEnableRegistry()) {
            // 非注册主机也推进时间戳，避免每周期从 0 全量重拉
            delayerLastUpdateTime = startUpdateTimeMills - 5000;
            return;
        }
        // 降级 → 恢复切换：Delayer configKey 覆盖（主循环 configureTaskDelayer 内 consumerNum 同则热更新、异则 stop old handle drain + new），不清 map
        if (delayerDegraded) {
            log.warn("!TaskDelayer center 恢复，从降级模式切换到 center 配置！");
            delayerDegraded = false;
        }
        // 第一次执行初始化操作
        if (delayerFirstRun) {
            delayerFirstRun = false;
            // 初始化delayer
            for (Map.Entry<String, TaskDelayer<?, ?>> entry : taskMetaInfoManager.getDelayerInstanceMap().entrySet()) {
                try {
                    TaskDelayer<?, ?> delayer = entry.getValue();
                    String taskClass = entry.getKey();
                    if (log.isDebugEnabled()) {
                        log.debug("正在初始化TaskDelayer: [{}].", taskClass);
                    }
                    TaskDelayerConfig localConfig = delayer.initConfig();
                    if (localConfig == null) {
                        log.warn("!TaskDelayer: [{}]默认配置为空，无法启动！", taskClass);
                        continue;
                    }
                    // 防止有人瞎胡搞，任务参数强制设死。
                    localConfig.setTaskClass(taskClass);
                    localConfig.setRunTarget(taskProperties.getRunTarget());
                    TaskContact contact = delayer.initContact();
                    if (contact != null) {
                        contact.setTaskClass(taskClass);
                    }
                    String configKey = taskMetaInfoManager.getDelayerConfigKey(localConfig);
                    TaskDelayerConfig serverConfig = updatedDelayerConfigMap.get(configKey);
                    // 上传配置：center 已有则跳过（对齐 runner/croner 条件首传；服务端按三元组幂等去重）
                    if (serverConfig == null) {
                        if (log.isDebugEnabled()) {
                            log.debug("TaskDelayer: [{}]未找到服务器端配置，上传默认配置...", taskClass);
                        }
                        uploadDelayerInfo(localConfig, contact);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        // 此时执行更新操作：遍历全量缓存（增量已由 fetchDelayerConfig 合并进 delayerConfigMap），过滤本项目+本 runTarget
        for (TaskDelayerConfig taskDelayerConfig : taskMetaInfoManager.getDelayerConfigMap().values()) {
            //必须是本项目任务，否则跳过
            if (!taskDelayerConfig.getTaskClass().startsWith(taskProperties.getTaskProject())) {
                if (log.isDebugEnabled()) {
                    log.debug("因为TaskProject不匹配，跳过配置ID: [{}], DELAYER: [{}].", taskDelayerConfig.getId(), taskDelayerConfig.getTaskClass());
                }
                continue;
            }
            //必须是同一个runTarget项目，否则跳过
            if (!taskDelayerConfig.getRunTarget().equals(taskProperties.getRunTarget())) {
                if (log.isDebugEnabled()) {
                    log.debug("因为RunTarget不匹配，跳过配置ID: [{}], DELAYER: [{}].", taskDelayerConfig.getId(), taskDelayerConfig.getTaskClass());
                }
                continue;
            }
            if (log.isDebugEnabled()) {
                log.debug("正在加载ID: [{}], DELAYER: [{}].", taskDelayerConfig.getId(), taskDelayerConfig.getTaskClass());
            }
            if (taskMetaInfoManager.getDelayerInstance(taskDelayerConfig.getTaskClass()) != null) {
                // 启动/热更新任务（configureTaskDelayer 幂等：consumerNum 不变则热更新，变则重建）
                taskDelayerContainer.configureTaskDelayer(taskDelayerConfig);
            } else {
                log.warn("!未找到匹配的任务类，未能启动配置ID: [{}], DELAYER: [{}].", taskDelayerConfig.getId(), taskDelayerConfig.getTaskClass());
            }
        }

        // 执行成功才会更新时间戳
        // 比对数据库的时候，因为数据库缺少ms值，所以减去5000，提高匹配度。
        delayerLastUpdateTime = startUpdateTimeMills - 5000;
    }

    /**
     * 应用关闭(@PreDestroy)时停所有任务，持 configLock 与在途 updateConfig/reportHostInfo 互斥，
     * 避免 destroyAll 的 clear/destroy 与并发的 registerQueueListener/configureTask 交错（listener 泄漏或写入已销毁容器）。
     */
    public void shutdownAllTasks() {
        configLock.lock();
        try {
            taskRunnerContainer.destroyAll();
            taskCronerContainer.destroyAll();
            taskDelayerContainer.destroyAll();
        } finally {
            configLock.unlock();
        }
    }

    /**
     * 本地降级启动所有 Croner（center 不可用时用 initConfig 默认配置调度）。
     * <p>center 恢复后 updateTaskCronerConfig 主循环用 center 配置覆盖（configKey 相同，
     * configureTask 内 stopTask cancel(false) 让在途跑完 + schedule new，不丢不中断）。</p>
     */
    private void startCronersLocal() {
        for (Map.Entry<String, TaskCroner> kv : taskMetaInfoManager.getCronerInstanceMap().entrySet()) {
            try {
                TaskCroner taskCroner = kv.getValue();
                String taskClass = kv.getKey();
                TaskCronerConfig localConfig = taskCroner.initConfig();
                if (localConfig == null) {
                    continue;
                }
                localConfig.setTaskClass(taskClass);
                localConfig.setRunTarget(taskProperties.getRunTarget());
                taskCronerContainer.configureTask(taskCroner, localConfig);
            } catch (Exception e) {
                log.error("Croner 本地降级启动失败[{}]: {}", kv.getKey(), e.getMessage(), e);
            }
        }
    }

    /**
     * 本地降级启动所有 Runner 的 MQ listener（center 不可用时用 initConfig 默认配置注册）。
     * <p>center 恢复后 updateTaskRunnerConfig 检测到 degraded → taskRunnerContainer.suspendAll() 清降级 listener
     *（unacked 消息 RabbitMQ 重新入队不丢）→ 主循环 taskRunnerContainer.registerQueueListener 用 center 配置重建。</p>
     */
    @SuppressWarnings("rawtypes")
    private void startRunnersLocal() {
        for (Map.Entry<String, TaskRunner> kv : taskMetaInfoManager.getRunnerInstanceMap().entrySet()) {
            try {
                TaskRunner taskRunner = kv.getValue();
                String taskClass = kv.getKey();
                TaskRunnerConfig localConfig = taskRunner.initConfig();
                if (localConfig == null) {
                    continue;
                }
                localConfig.setTaskClass(taskClass);
                localConfig.setRunTarget(taskProperties.getRunTarget());
                // 降级期间也必须填充 runnerConfigMap：否则同 JVM 业务调 sendToQueue→getFitQueue 找不到配置抛异常（与 Delayer 的 configureTaskDelayer 内 setDelayerConfig 对称）
                taskMetaInfoManager.setRunnerConfig(taskMetaInfoManager.getRunnerConfigKey(localConfig), localConfig);
                taskRunnerContainer.registerQueueListener(localConfig);
            } catch (Exception e) {
                log.error("Runner 本地降级启动失败[{}]: {}", kv.getKey(), e.getMessage(), e);
            }
        }
    }

    /**
     * 本地降级启动所有 Delayer（center 不可用时用 initConfig 默认配置启动 poll）。
     * <p>实现下沉到 {@link TaskDelayerContainer#start()}（遍历实例 initConfig + configureTaskDelayer），
     * 本方法仅为与 {@link #startRunnersLocal}/{@link #startCronersLocal} 命名对称。
     * center 恢复后 updateTaskDelayerConfig 检测到 degraded→false，主循环用 center 配置按 configKey 原地覆盖
     *（consumerNum 同则热更新、异则 stop old handle drain + 重建）。</p>
     */
    private void startDelayersLocal() {
        taskDelayerContainer.start();
    }

    /**
     * 上报主机状态。
     * <p>采集 JVM 内存/线程、任务统计、各队列积压与消费者数，提交到 task-center；
     * 返回值（含主机记录 id）缓存供下次上报定位记录。</p>
     *
     * @return 服务端响应；网络失败返回 null
     */
    public HostReportResponse reportHostInfo() {
        //最后提交主机状态报告。
        if (log.isDebugEnabled()) {
            log.debug("正在提交主机状态报告...");
        }
        TaskHostStats hostStats = new TaskHostStats();
        if (reportResponse != null && reportResponse.getId() > 0) {
            hostStats.setId(reportResponse.getId());
        }
        hostStats.setAppName(taskProperties.getAppName());
        hostStats.setAppVersion(taskProperties.getAppVersion());
        String appHost = taskProperties.getAppHost();
        if (appHost == null || appHost.isEmpty()) {
            // 未接入 nacos（appHost 配置为空）时用本机 IP 兜底，避免上报空 host
            try {
                appHost = java.net.InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                appHost = "";
            }
        }
        hostStats.setAppHost(appHost);
        hostStats.setAppPort(taskProperties.getAppPort());
        hostStats.setTaskProject(taskProperties.getTaskProject());
        hostStats.setRunTarget(taskProperties.getRunTarget());
        hostStats.setCronerNum(taskMetaInfoManager.getCronerInstanceMap().size());
        hostStats.setRunnerNum(taskMetaInfoManager.getRunnerInstanceMap().size());
        hostStats.setDelayerNum(taskMetaInfoManager.getDelayerInstanceMap().size());
        //设置内存和线程信息。
        Runtime runtime = Runtime.getRuntime();
        hostStats.setJvmMemMax(runtime.maxMemory());
        hostStats.setJvmMemTotal(runtime.totalMemory());
        hostStats.setJvmMemFree(runtime.freeMemory());
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        hostStats.setThreadActive(threadMXBean.getThreadCount());
        hostStats.setThreadDaemon(threadMXBean.getDaemonThreadCount());
        hostStats.setThreadPeak(threadMXBean.getPeakThreadCount());
        hostStats.setThreadStarted(threadMXBean.getTotalStartedThreadCount());
        //保存任务统计信息。
        hostStats.setTaskCronerStatsList(TaskStatsService.getCronerStats());
        hostStats.setTaskRunnerStatsList(TaskStatsService.getRunnerStats());
        java.util.List<TaskDelayerStats> delayerStatsList = TaskStatsService.getDelayerStats();
        // 填充 zset 积压数与执行线程数（被动查询，stats 本身只累计计数/耗时）
        taskDelayerContainer.fillDelayerStats(delayerStatsList);
        hostStats.setTaskDelayerStatsList(delayerStatsList);
        // 查询队列积压信息（queueName 反查 + queueDeclarePassive），委托 TaskRunnerContainer。
        taskRunnerContainer.fillQueueBacklog(hostStats.getTaskRunnerStatsList());
        ResponseData<HostReportResponse> reportResult = taskApiClient.reportHostInfo(hostStats);
        reportResponse = reportResult != null && reportResult.isSuccess() ? reportResult.getData() : null;
        // 主机禁用/启用处理：服务端 HostReportResponse.state=0 表示 OPS 禁用本机。
        // configLock 与 updateConfig 互斥：防止停机 suspendAll 与并发的 updateConfig 注册（registerQueueListener/configureTask）踩踏，
        // 导致被禁主机残留 listener/future/handle（clear 与 computeIfAbsent/put 交错）。
        configLock.lock();
        try {
            if (reportResponse != null) {
                if (reportResponse.getState() == 0 && !hostDisabled) {
                    hostDisabled = true;
                    log.warn("!本主机被服务端禁用(state=0)，停止所有任务容器（runner/croner/delayer），保留底层线程池以便恢复！");
                    // 三容器统一 suspendAll（禁用，保池）：runner destroy listener + 清 map；croner cancel future + 清 map；
                    // delayer stop handle + 清 map。三者均不清底层线程池/连接，解除禁用后可恢复。
                    taskRunnerContainer.suspendAll();
                    taskCronerContainer.suspendAll();
                    taskDelayerContainer.suspendAll();
                    // 重置降级/首次标志：禁用=全新停机，恢复时按全新启动（fetch 成功用 center 配置，fetch 失败则降级 start），
                    // 避免 degraded 残留导致解除禁用后 fetch 仍失败时降级条件(!degraded)不满足、任务不重启。
                    runnerDegraded = false;
                    cronerDegraded = false;
                    delayerDegraded = false;
                    runnerFirstRun = true;
                    cronerFirstRun = true;
                    delayerFirstRun = true;
                } else if (reportResponse.getState() != 0 && hostDisabled) {
                    hostDisabled = false;
                    log.warn("!本主机已解除禁用，重置增量时间戳并立即全量拉取重新注册所有任务！");
                    // 重置三类增量时间戳为 0，强制 updateConfig 全量拉取；各容器 map 已在禁用时清空，主循环会重建 listener/调度/handle
                    runnerLastUpdateTime = 0;
                    cronerLastUpdateTime = 0;
                    delayerLastUpdateTime = 0;
                    updateConfig();
                }
            }
        } finally {
            configLock.unlock();
        }
        //最后提交主机状态报告。
        if (log.isDebugEnabled()) {
            log.debug("完成提交主机状态报告...");
        }
        return reportResponse;

    }

    /**
     * 从 task-center 增量拉取 croner 配置（基于 cronerLastUpdateTime），同步写入本地 metaInfo 缓存。
     * <p>拉取失败时返回 null，触发 {@link #updateTaskCronerConfig} 的 fail-fast（保留上次配置不更新）。</p>
     *
     * @return 本次拉取到的配置 map（configKey -> config）；拉取失败时返回 null
     */
    private ConcurrentHashMap<String, TaskCronerConfig> fetchCronerConfig() {
        ConcurrentHashMap<String, TaskCronerConfig> map = new ConcurrentHashMap<>();
        ResponseData<List<TaskCronerConfig>> response = taskApiClient.getTaskCronerConfigList(taskProperties.getRunTarget(), taskProperties.getTaskProject(), cronerLastUpdateTime);
        List<TaskCronerConfig> list = response != null && response.isSuccess() ? response.getData() : null;
        if (list != null) {
            for (TaskCronerConfig config : list) {
                String key = taskMetaInfoManager.getCronerConfigKey(config);
                map.put(key, config);
                if (config.getState() < 1) {
                    taskMetaInfoManager.removeCronerConfig(key);
                    // 停掉孤儿 future：center 删除/禁用(state<1)后，已调度的 cron 不应再触发（否则持续跑已废止的业务，直到重启）
                    taskCronerContainer.stopTask(key);
                } else {
                    taskMetaInfoManager.setCronerConfig(key, config);
                }
            }
            return map;
        }
        return null;
    }

    /**
     * 从 task-center 增量拉取 runner 配置（基于 runnerLastUpdateTime），同步更新本地 metaInfo 缓存
     * （含 state&lt;1 的删除处理）。拉取失败时返回 null，触发 fail-fast。
     *
     * @return 本次拉取到的配置 map（configKey -> config）；拉取失败时返回 null
     */
    private ConcurrentHashMap<String, TaskRunnerConfig> fetchRunnerConfig() {
        ConcurrentHashMap<String, TaskRunnerConfig> map = new ConcurrentHashMap<>();
        ResponseData<List<TaskRunnerConfig>> response = taskApiClient.getTaskRunnerConfigList(taskProperties.getRunTarget(), taskProperties.getTaskProject(), runnerLastUpdateTime);
        List<TaskRunnerConfig> list = response != null && response.isSuccess() ? response.getData() : null;
        if (list != null) {
            for (TaskRunnerConfig config : list) {
                String key = taskMetaInfoManager.getRunnerConfigKey(config);
                if (config.getState() < 1) {
                    taskMetaInfoManager.removeRunnerConfig(key);
                    // 销毁残留 listener：center 删除/禁用(state<1)后，已注册的 MQ listener 不应继续占用连接/消费
                    taskRunnerContainer.registerQueueListener(config);
                } else {
                    taskMetaInfoManager.setRunnerConfig(key, config);
                }
                map.put(key, config);
            }
            return map;
        }
        return null;
    }

    /**
     * 从 task-center 增量拉取 delayer 配置（基于 delayerLastUpdateTime），同步更新本地 metaInfo 缓存
     *（含 state&lt;1 的删除处理：removeDelayerConfig + stopDelayer 停止孤儿 handle）。
     * 拉取失败时返回 null，触发降级/warn（delayer 独立于 runner/croner 的 fail-fast，仅 warn 不影响主流程）。
     *
     * @return 本次拉取到的配置 map（configKey -&gt; config）；拉取失败时返回 null
     */
    private ConcurrentHashMap<String, TaskDelayerConfig> fetchDelayerConfig() {
        ConcurrentHashMap<String, TaskDelayerConfig> map = new ConcurrentHashMap<>();
        ResponseData<List<TaskDelayerConfig>> response = taskApiClient.getTaskDelayerConfigList(taskProperties.getRunTarget(), taskProperties.getTaskProject(), delayerLastUpdateTime);
        List<TaskDelayerConfig> list = response != null && response.isSuccess() ? response.getData() : null;
        if (list != null) {
            for (TaskDelayerConfig config : list) {
                String key = taskMetaInfoManager.getDelayerConfigKey(config);
                if (config.getState() < 1) {
                    taskMetaInfoManager.removeDelayerConfig(key);
                    // 停掉孤儿 handle：center 删除/禁用(state<1)后，已 poll 的 handle 不应继续消费（对齐 fetchCronerConfig 的 stopTask）
                    taskDelayerContainer.stopDelayer(key);
                } else {
                    taskMetaInfoManager.setDelayerConfig(key, config);
                }
                map.put(key, config);
            }
            return map;
        }
        return null;
    }

    /**
     * 首次上传 runner 默认配置与联系人信息到 task-center，并将服务端返回的 config 写入本地缓存。
     *
     * @param config  本地默认配置
     * @param contact 联系人信息
     * @return 服务端确认后的配置（含分配的 id）
     */
    private TaskRunnerConfig uploadRunnerInfo(TaskRunnerConfig config, TaskContact contact) {
        ResponseData<TaskRunnerConfig> response = taskApiClient.initTaskRunnerConfig(config);
        if (response != null && response.isSuccess() && response.getData() != null) {
            config = response.getData();
        }
        taskApiClient.initTaskContact(contact);
        taskMetaInfoManager.setRunnerConfig(taskMetaInfoManager.getRunnerConfigKey(config), config);
        return config;
    }

    /**
     * 首次上传 croner 默认配置与联系人信息到 task-center，并将服务端返回的 config 写入本地缓存。
     *
     * @param config  本地默认配置
     * @param contact 联系人信息
     * @return 服务端确认后的配置（含分配的 id）
     */
    private TaskCronerConfig uploadCronerInfo(TaskCronerConfig config, TaskContact contact) {
        ResponseData<TaskCronerConfig> response = taskApiClient.initTaskCronerConfig(config);
        if (response != null && response.isSuccess() && response.getData() != null) {
            config = response.getData();
        }
        taskApiClient.initTaskContact(contact);
        taskMetaInfoManager.setCronerConfig(taskMetaInfoManager.getCronerConfigKey(config), config);
        return config;
    }

    /**
     * 首次上传 delayer 默认配置与联系人信息到 task-center，并将服务端返回的 config 写入本地缓存。
     * <p>与 {@link #uploadRunnerInfo}/{@link #uploadCronerInfo} 对称：只写缓存、不启动任务——
     * handle 启动由同周期主循环（遍历 delayerConfigMap）完成。submit() 校验的是 delayerConfigMap.containsKey，
     * setDelayerConfig 已满足，故首传与主循环之间不存在"submit 抛异常"窗口。</p>
     *
     * @param config  本地默认配置
     * @param contact 联系人信息（可空，delayer 不强制）
     * @return 服务端确认后的配置（含分配的 id）
     */
    private TaskDelayerConfig uploadDelayerInfo(TaskDelayerConfig config, TaskContact contact) {
        ResponseData<TaskDelayerConfig> response = taskApiClient.initTaskDelayerConfig(config);
        if (response != null && response.isSuccess() && response.getData() != null) {
            config = response.getData();
        }
        if (contact != null) {
            taskApiClient.initTaskContact(contact);
        }
        taskMetaInfoManager.setDelayerConfig(taskMetaInfoManager.getDelayerConfigKey(config), config);
        return config;
    }

}
