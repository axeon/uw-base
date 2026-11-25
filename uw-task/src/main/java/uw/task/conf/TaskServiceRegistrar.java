package uw.task.conf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.response.ResponseData;
import uw.common.util.SystemClock;
import uw.task.TaskCroner;
import uw.task.TaskRunner;
import uw.task.api.TaskApiClient;
import uw.task.container.TaskCronerContainer;
import uw.task.container.TaskDelayTransferContainer;
import uw.task.container.TaskRunnerContainer;
import uw.task.entity.*;
import uw.task.util.TaskConfigValidator;
import uw.task.util.TaskStatsService;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 任务服务注册器：负责与 task-center 交互，拉取/上传 TaskRunner、TaskCroner 两类任务的动态配置，
 * 并驱动各自容器的启动、热更新与暂停。
 *
 * <p>由 {@link UwTaskAutoConfiguration} 的调度线程池周期性调用：
 * {@link #updateConfig}（60s）编排两类配置的增量拉取与刷新，
 * {@link #reportHostInfo}（180s）上报 JVM/线程/统计/队列积压等信息并处理主机禁用/启用。</p>
 *
 * <p><b>降级与恢复（center 不可达时）</b></p>
 * <p>两类任务各自独立判定，核心机制：<b>拉取失败时 {@code xxxLastUpdateTime} 不推进</b>（直接 return，
 * 不更新时间戳），保证 center 恢复后能按断联前的时间点补拉增量，不丢配置变更窗口。</p>
 * <ul>
 *   <li><b>启动降级</b>：仅首次拉取失败（{@code xxxFirstRun && !xxxDegraded}）触发，用 {@code initConfig()} 本地默认配置
 *       启动。此时 {@code xxxLastUpdateTime} 仍为初始 0，且降级分支<b>不</b>把 {@code xxxFirstRun} 置 false。center 恢复后
 *       fetch 用 {@code lastUpdateTime=0} 拉全量，firstRun 块重新执行（上传缺失配置 + register/configure），
 *       按 configKey 覆盖本地降级 config，无残留。</li>
 *   <li><b>运行中断联</b>：非首次拉取失败不触发降级（firstRun 已 false），直接 fail-fast return 保留上次配置，
 *       {@code xxxLastUpdateTime} 冻在断联前。center 恢复后按断联前时间点拉增量，补齐断联期间的配置变更。</li>
 * </ul>
 * <p>一旦拉取成功就不再因后续失败重新降级——运行中故障靠 fail-fast 保留配置，避免降级/恢复反复抖动。</p>
 *
 * <p><b>主循环增量驱动</b></p>
 * <p>只遍历本次 fetch 的增量（updatedXxxConfigMap），不遍历全量 configMap：配置无变化时 fetch 返回空、主循环不遍历。
 * firstRun 上传的 config（center 没返回过、不在增量里）在 firstRun 分支直接 register/configure 启动。</p>
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
     * 定时任务容器。
     */
    private final TaskCronerContainer taskCronerContainer;
    /**
     * 延时搬运器（上报 zset 积压）。
     */
    private final TaskDelayTransferContainer taskDelayTransferContainer;

    /**
     * 任务配置。
     */
    private final UwTaskProperties taskProperties;

    /**
     * 任务中心 API 客户端。
     */
    private final TaskApiClient taskApiClient;

    /**
     * 任务meta信息管理器。
     */
    private final TaskMetaInfoManager taskMetaInfoManager;

    /**
     * 初始值必须=0，用于标识整体加载。
     */
    private volatile long runnerLastUpdateTime = 0;

    /**
     * 初始值必须=0，用于标识整体加载。
     */
    private volatile long cronerLastUpdateTime = 0;

    /**
     * runner 首次启动标志：首次拉取配置成功后上传本地默认配置。
     */
    private boolean runnerFirstRun = true;

    /**
     * croner 首次启动标志：首次拉取配置成功后上传本地默认配置。
     */
    private boolean cronerFirstRun = true;

    /**
     * 两类任务是否处于本地降级模式（center 不可用时用 initConfig 默认配置启动）。
     * 拉取失败+首次进入降级；拉取成功退出降级（center 配置覆盖/重建）。各自独立，互不影响。
     */
    private volatile boolean runnerDegraded = false;
    /**
     * croner 是否处于本地降级模式（语义同 runnerDegraded）。
     */
    private volatile boolean cronerDegraded = false;
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
     * 默认配置上传 task-center 失败、待重试的队列任务类名（先用本地默认配置启动，下一周期起重试上传）。
     */
    private final Set<String> runnerUploadPending = ConcurrentHashMap.newKeySet();

    /**
     * 默认配置上传 task-center 失败、待重试的定时任务类名（语义同 runnerUploadPending）。
     */
    private final Set<String> cronerUploadPending = ConcurrentHashMap.newKeySet();

    /**
     * 配置更新与主机禁用/启用的互斥锁：保护 updateConfig 与 reportHostInfo 的停机/恢复段，
     * 防止并发踩踏（suspendAll 与 registerQueueListener/configureTask 交错致被禁主机残留资源）。
     * 可重入：reportHostInfo 恢复段持锁后内嵌调 updateConfig（同线程重入），不阻塞自己。
     */
    private final ReentrantLock configLock = new ReentrantLock();

    /**
     * 构建任务服务注册器。
     *
     * @param taskProperties       任务配置
     * @param taskApiClient        任务中心 API 客户端
     * @param taskRunnerContainer  队列任务容器
     * @param taskCronerContainer  定时任务容器
     * @param taskMetaInfoManager  任务元信息管理器
     * @param taskDelayTransferContainer 延时搬运器（上报 zset 积压）
     */
    public TaskServiceRegistrar(UwTaskProperties taskProperties, TaskApiClient taskApiClient, TaskRunnerContainer taskRunnerContainer, TaskCronerContainer taskCronerContainer,
                                TaskMetaInfoManager taskMetaInfoManager, TaskDelayTransferContainer taskDelayTransferContainer) {
        this.taskProperties = taskProperties;
        this.taskRunnerContainer = taskRunnerContainer;
        this.taskCronerContainer = taskCronerContainer;
        this.taskDelayTransferContainer = taskDelayTransferContainer;
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
     * 注册并刷新当前主机的任务配置（持 configLock；主机被禁用时跳过）。
     * <p>各类配置使用独立的时间戳与 fail-fast，互不影响。</p>
     */
    public void updateConfig() {
        configLock.lock();
        try {
            if (hostDisabled) {
                return;
            }
            updateTaskRunnerConfig();
            updateTaskCronerConfig();
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
                log.warn("拉取TaskRunner配置失败，启用本地降级模式（initConfig 默认配置）。");
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
            log.warn("TaskRunner center 恢复，从降级模式切换到 center 配置。");
            taskRunnerContainer.suspendAll();
            runnerDegraded = false;
        }
        // 首次启动时上传失败的默认配置：每周期重试，成功后用 center 配置（含分配的 id）覆盖本地的 id=0 配置
        retryPendingRunnerUploads();
        // 第一次执行初始化操作
        if (runnerFirstRun) {
            runnerFirstRun = false;
            // 获取当前主机上所有的TaskRunner
            for (Entry<String, TaskRunner<?, ?>> kv : taskMetaInfoManager.getRunnerInstanceMap().entrySet()) {
                try {
                    // 拿到任务类名
                    TaskRunner<?, ?> taskRunner = kv.getValue();
                    String taskClass = kv.getKey();
                    // 只处理 task-project 前缀下的任务（与服务端配置过滤、降级启动同口径）
                    if (!belongsToProject(taskClass)) {
                        if (log.isDebugEnabled()) {
                            log.debug("TaskProject 不匹配，跳过本机任务: [{}].", taskClass);
                        }
                        continue;
                    }
                    TaskRunnerConfig localConfig = taskRunner.initConfig();
                    TaskContact contact = taskRunner.initContact();
                    if (localConfig == null || contact == null) {
                        log.warn("TaskRunner: [{}]默认配置或联系人信息为空，无法启动。", taskClass);
                        continue;
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("正在初始化TaskRunner: [{}].", taskClass);
                    }
                    // 上传前先规整取值（center 保存的默认配置即合法值）；taskClass 与 runTarget 由框架设死（防止调用方误填）
                    buildLocalRunnerConfig(taskClass, localConfig, contact);
                    String configKey = taskMetaInfoManager.getRunnerConfigKey(localConfig);
                    TaskRunnerConfig serverConfig = updatedRunnerConfigMap.get(configKey);
                    // 上传配置
                    if (serverConfig == null) {
                        if (log.isDebugEnabled()) {
                            log.debug("TaskRunner: [{}]未找到服务器端配置，上传默认配置...", taskClass);
                        }
                        serverConfig = uploadRunnerInfo(localConfig, contact);
                        if (serverConfig == null) {
                            // 上传失败：先以本地默认配置（id=0）启动监听保证任务可执行，并登记为待重试
                            runnerUploadPending.add(taskClass);
                            serverConfig = localConfig;
                            taskMetaInfoManager.setRunnerConfig(taskMetaInfoManager.getRunnerConfigKey(localConfig), localConfig);
                        }
                        // 首次上传的 config 不在 fetch 增量(updatedRunnerConfigMap)里，这里直接 register 启动 listener；
                        // 后续由下方主循环按 fetch 增量驱动
                        taskRunnerContainer.registerQueueListener(serverConfig);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        // 只处理本次 fetch 增量（updatedRunnerConfigMap），不遍历全量 runnerConfigMap：
        // 配置无变化时 fetch 返回空，避免每 60s 对全量 config 调 registerQueueListener 白跑（含声明与并发调整）。
        // firstRun 上传的 config 已在上方 firstRun 分支直接 register 启动，不依赖此循环。
        for (TaskRunnerConfig taskRunnerConfig : updatedRunnerConfigMap.values()) {
            //必须是本项目任务，否则跳过
            if (!belongsToProject(taskRunnerConfig.getTaskClass())) {
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
                log.warn("拉取TaskCroner配置失败，启用本地降级模式（initConfig 默认配置）。");
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
        // 降级 → 恢复切换：Croner configKey 覆盖（主循环 configureTask 内 stop cancel(false) 让在途跑完 + schedule new），不清 map
        if (cronerDegraded) {
            log.warn("TaskCroner center 恢复，从降级模式切换到 center 配置。");
            cronerDegraded = false;
        }
        // 首次启动时上传失败的默认配置：每周期重试，成功后用 center 配置覆盖本地的 id=0 配置
        retryPendingCronerUploads();
        // 第一次执行初始化操作
        if (cronerFirstRun) {
            cronerFirstRun = false;
            // 初始化croner
            for (Entry<String, TaskCroner> kv : taskMetaInfoManager.getCronerInstanceMap().entrySet()) {
                try {
                    // 拿到任务类名
                    TaskCroner taskCroner = kv.getValue();
                    String taskClass = taskCroner.getClass().getName();
                    // 只处理 task-project 前缀下的任务（与服务端配置过滤、降级启动同口径）
                    if (!belongsToProject(taskClass)) {
                        if (log.isDebugEnabled()) {
                            log.debug("TaskProject 不匹配，跳过本机任务: [{}].", taskClass);
                        }
                        continue;
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("正在初始化TaskCroner: [{}].", taskClass);
                    }
                    TaskCronerConfig localConfig = taskCroner.initConfig();
                    TaskContact contact = taskCroner.initContact();
                    if (localConfig == null || contact == null) {
                        log.warn("TaskCroner: [{}]默认配置或联系人信息为空，无法启动。", taskClass);
                        continue;
                    }
                    // 上传前先规整取值（center 保存的默认配置即合法值）；taskClass 与 runTarget 由框架设死（防止调用方误填）
                    buildLocalCronerConfig(taskClass, localConfig, contact);
                    String configKey = taskMetaInfoManager.getCronerConfigKey(localConfig);
                    TaskCronerConfig serverConfig = updatedCronerConfigMap.get(configKey);
                    // 上传配置
                    if (serverConfig == null) {
                        if (log.isDebugEnabled()) {
                            log.debug("TaskCroner: [{}]未找到服务器端配置，上传默认配置...", taskClass);
                        }
                        serverConfig = uploadCronerInfo(localConfig, contact);
                        if (serverConfig == null) {
                            // 上传失败：先以本地默认配置（id=0）启动调度，并登记为待重试
                            cronerUploadPending.add(taskClass);
                            serverConfig = localConfig;
                            taskMetaInfoManager.setCronerConfig(taskMetaInfoManager.getCronerConfigKey(localConfig), localConfig);
                        }
                        // 首次上传的 config 不在 fetch 增量(updatedCronerConfigMap)里，这里直接 configure 启动调度；
                        // 后续由下方主循环按 fetch 增量驱动
                        taskCronerContainer.configureTask(taskCroner, serverConfig);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        // 只处理本次 fetch 增量（updatedCronerConfigMap），不遍历全量 cronerConfigMap：
        // 配置无变化时 fetch 返回空，避免每 60s 对全量 croner 调 configureTask（stop cancel + reschedule）浪费与调度抖动。
        // firstRun 上传的 config 已在上方 firstRun 分支直接 configure 启动，不依赖此循环。
        for (TaskCronerConfig taskCronerConfig : updatedCronerConfigMap.values()) {
            //必须是本项目任务，否则跳过
            if (!belongsToProject(taskCronerConfig.getTaskClass())) {
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
                    taskCronerContainer.configureTask(taskCroner, taskCronerConfig);
                } catch (Exception e) {
                    // 带业务上下文（configId/taskClass/cron），便于排查是哪个 croner 配置失败
                    log.error("配置CRONER异常, ID: [{}], taskClass: [{}], CRON: [{}]: {}", taskCronerConfig.getId(), taskCronerConfig.getTaskClass(), taskCronerConfig.getTaskCron(), e.getMessage(), e);
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
     * 应用关闭时停所有任务，持 configLock 与在途 updateConfig/reportHostInfo 互斥，
     * 避免 shutdown 的 clear/destroy 与并发的 registerQueueListener/configureTask 交错（listener 泄漏或写入已销毁容器）。
     */
    public void shutdown() {
        configLock.lock();
        try {
            taskRunnerContainer.shutdown();
            taskCronerContainer.shutdown();
        } finally {
            configLock.unlock();
        }
    }

    /**
     * 本地降级启动所有 Croner（center 不可用时用 initConfig 默认配置调度）。
     * <p>center 恢复后 updateTaskCronerConfig 主循环用 center 配置覆盖（configKey 相同，
     * configureTask 内 stop cancel(false) 让在途跑完 + schedule new，不丢不中断）。</p>
     */
    private void startCronersLocal() {
        for (Entry<String, TaskCroner> kv : taskMetaInfoManager.getCronerInstanceMap().entrySet()) {
            try {
                TaskCroner taskCroner = kv.getValue();
                String taskClass = kv.getKey();
                // 只处理 task-project 前缀下的任务（与服务端配置过滤、降级启动同口径）
                if (!belongsToProject(taskClass)) {
                    if (log.isDebugEnabled()) {
                        log.debug("TaskProject 不匹配，跳过本机任务: [{}].", taskClass);
                    }
                    continue;
                }
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
    private void startRunnersLocal() {
        for (Entry<String, TaskRunner<?, ?>> kv : taskMetaInfoManager.getRunnerInstanceMap().entrySet()) {
            try {
                TaskRunner<?, ?> taskRunner = kv.getValue();
                String taskClass = kv.getKey();
                // 只处理 task-project 前缀下的任务（与服务端配置过滤、降级启动同口径）
                if (!belongsToProject(taskClass)) {
                    if (log.isDebugEnabled()) {
                        log.debug("TaskProject 不匹配，跳过本机任务: [{}].", taskClass);
                    }
                    continue;
                }
                TaskRunnerConfig localConfig = taskRunner.initConfig();
                if (localConfig == null) {
                    continue;
                }
                localConfig.setTaskClass(taskClass);
                localConfig.setRunTarget(taskProperties.getRunTarget());
                taskMetaInfoManager.setRunnerConfig(taskMetaInfoManager.getRunnerConfigKey(localConfig), localConfig);
                taskRunnerContainer.registerQueueListener(localConfig);
            } catch (Exception e) {
                log.error("Runner 本地降级启动失败[{}]: {}", kv.getKey(), e.getMessage(), e);
            }
        }
    }

    /**
     * 上报主机状态。
     * <p>采集 JVM 内存/线程、任务统计、各队列积压与消费者数、延时 zset 积压，提交到 task-center；
     * 返回值（含主机记录 id）缓存供下次上报定位记录。随后按响应的 state 处理主机禁用/启用
     * （state=0 停两容器；恢复为非 0 时重置增量时间戳并立即全量重拉）。</p>
     *
     * @return 服务端响应；网络失败返回 null
     */
    public HostReportResponse reportHostInfo() {
        if (log.isDebugEnabled()) {
            log.debug("开始采集并提交主机状态报告...");
        }
        TaskHostStats hostStats = new TaskHostStats();
        if (reportResponse != null && reportResponse.getId() > 0) {
            hostStats.setId(reportResponse.getId());
        }
        hostStats.setAppName(taskProperties.getAppName());
        hostStats.setAppVersion(taskProperties.getAppVersion());
        String appHost = taskProperties.getAppHost();
        if (appHost == null || appHost.isEmpty() || "unknown".equals(appHost)) {
            // 未接入 nacos（appHost 为空或是占位值 unknown）时用本机 IP 兜底，避免上报无效 host
            try {
                appHost = InetAddress.getLocalHost().getHostAddress();
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
        // 查询队列积压信息，委托 TaskRunnerContainer。
        taskRunnerContainer.fillQueueBacklog(hostStats.getTaskRunnerStatsList());
        // 延时积压（project 级 zset 待搬运数，与 MQ 侧队列积压互补）
        hostStats.setDelayBacklog(taskDelayTransferContainer.getBacklogSize());
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
                    log.warn("本主机被服务端禁用(state=0)，停止所有任务容器（runner/croner），保留底层线程池以便恢复。");
                    // 三容器统一 suspendAll（禁用，保池）：runner destroy listener + 清 map；croner cancel future + 清 map；
                    taskRunnerContainer.suspendAll();
                    taskCronerContainer.suspendAll();
                    // 重置降级/首次标志：禁用=全新停机，恢复时按全新启动（fetch 成功用 center 配置，fetch 失败则降级 start），
                    // 避免 degraded 残留导致解除禁用后 fetch 仍失败时降级条件(!degraded)不满足、任务不重启。
                    runnerDegraded = false;
                    cronerDegraded = false;
                    runnerFirstRun = true;
                    cronerFirstRun = true;
                    // 恢复时按全新启动重新上传默认配置，旧的待重试记录作废
                    runnerUploadPending.clear();
                    cronerUploadPending.clear();
                } else if (reportResponse.getState() != 0 && hostDisabled) {
                    hostDisabled = false;
                    log.warn("本主机已解除禁用，重置增量时间戳并立即全量拉取重新注册所有任务。");
                    // 重置两类增量时间戳为 0，强制 updateConfig 全量拉取；各容器 map 已在禁用时清空，主循环会重建 listener/调度/handle
                    runnerLastUpdateTime = 0;
                    cronerLastUpdateTime = 0;
                    updateConfig();
                }
            }
        } finally {
            configLock.unlock();
        }
        if (log.isDebugEnabled()) {
            log.debug("完成提交主机状态报告...");
        }
        return reportResponse;
    }

    /**
     * 任务类是否属于本项目的 task-project 前缀（首次上传、本地降级启动、服务端配置过滤三处同口径）。
     *
     * @param taskClass 任务类全限定名
     * @return taskClass 等于该前缀或以"前缀."开头返回 true
     */
    private boolean belongsToProject(String taskClass) {
        String project = taskProperties.getTaskProject();
        // 带点号边界：project 是包名前缀，saas.mall 不能匹配到 saas.mall2 下的任务
        return taskClass.equals(project) || taskClass.startsWith(project + ".");
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
                    taskCronerContainer.stop(key);
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
     * 首次上传 runner 默认配置与联系人信息到 task-center，并将服务端返回的 config 写入本地缓存。
     *
     * @param config  本地默认配置（已规整）
     * @param contact 联系人信息
     * @return 服务端确认后的配置（含分配的 id）；上传失败返回 null
     */
    private TaskRunnerConfig uploadRunnerInfo(TaskRunnerConfig config, TaskContact contact) {
        ResponseData<TaskRunnerConfig> response = taskApiClient.initTaskRunnerConfig(config);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            return null;
        }
        TaskRunnerConfig serverConfig = response.getData();
        taskApiClient.initTaskContact(contact);
        taskMetaInfoManager.setRunnerConfig(taskMetaInfoManager.getRunnerConfigKey(serverConfig), serverConfig);
        return serverConfig;
    }

    /**
     * 重试上传首次启动时上传失败的 runner 默认配置：成功的用 center 配置覆盖本地的 id=0 配置（监听已存在，只按新配置调整并发）；
     * 仍失败的保留到下一周期。
     */
    private void retryPendingRunnerUploads() {
        for (Iterator<String> it = runnerUploadPending.iterator(); it.hasNext(); ) {
            String taskClass = it.next();
            TaskRunner<?, ?> taskRunner = taskMetaInfoManager.getRunnerInstance(taskClass);
            if (taskRunner == null) {
                it.remove();
                continue;
            }
            try {
                TaskRunnerConfig localConfig = taskRunner.initConfig();
                TaskContact contact = taskRunner.initContact();
                TaskRunnerConfig serverConfig = localConfig == null || contact == null ? null : uploadRunnerInfo(buildLocalRunnerConfig(taskClass, localConfig, contact), contact);
                if (serverConfig != null) {
                    it.remove();
                    taskRunnerContainer.registerQueueListener(serverConfig);
                    log.info("TaskRunner: [{}]默认配置补传 task-center 成功，已切换为 center 配置(id={})。", taskClass, serverConfig.getId());
                } else {
                    log.warn("TaskRunner: [{}]默认配置补传 task-center 失败，下一周期继续重试（当前仍以本地默认配置运行）。", taskClass);
                }
            } catch (Exception e) {
                log.warn("TaskRunner: [{}]默认配置补传 task-center 异常，下一周期继续重试: {}", taskClass, e.getMessage());
            }
        }
    }

    /**
     * 构造待上传的本地默认配置：规整取值，taskClass 与 runTarget 由框架设死（防止调用方误填）。
     *
     * @param taskClass   任务类全限定名
     * @param localConfig initConfig 返回的默认配置
     * @param contact     联系人信息（taskClass 同步设死）
     * @return 规整后的本地默认配置
     */
    private TaskRunnerConfig buildLocalRunnerConfig(String taskClass, TaskRunnerConfig localConfig, TaskContact contact) {
        TaskConfigValidator.normalize(localConfig);
        localConfig.setTaskClass(taskClass);
        localConfig.setRunTarget(taskProperties.getRunTarget());
        contact.setTaskClass(taskClass);
        return localConfig;
    }

    /**
     * 首次上传 croner 默认配置与联系人信息到 task-center，并将服务端返回的 config 写入本地缓存。
     *
     * @param config  本地默认配置（已规整）
     * @param contact 联系人信息
     * @return 服务端确认后的配置（含分配的 id）；上传失败返回 null
     */
    private TaskCronerConfig uploadCronerInfo(TaskCronerConfig config, TaskContact contact) {
        ResponseData<TaskCronerConfig> response = taskApiClient.initTaskCronerConfig(config);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            return null;
        }
        TaskCronerConfig serverConfig = response.getData();
        taskApiClient.initTaskContact(contact);
        taskMetaInfoManager.setCronerConfig(taskMetaInfoManager.getCronerConfigKey(serverConfig), serverConfig);
        return serverConfig;
    }

    /**
     * 重试上传首次启动时上传失败的 croner 默认配置：成功的用 center 配置重新调度（覆盖本地的 id=0 配置）；
     * 仍失败的保留到下一周期。
     */
    private void retryPendingCronerUploads() {
        for (Iterator<String> it = cronerUploadPending.iterator(); it.hasNext(); ) {
            String taskClass = it.next();
            TaskCroner taskCroner = taskMetaInfoManager.getCronerInstance(taskClass);
            if (taskCroner == null) {
                it.remove();
                continue;
            }
            try {
                TaskCronerConfig localConfig = taskCroner.initConfig();
                TaskContact contact = taskCroner.initContact();
                TaskCronerConfig serverConfig = localConfig == null || contact == null ? null : uploadCronerInfo(buildLocalCronerConfig(taskClass, localConfig, contact), contact);
                if (serverConfig != null) {
                    it.remove();
                    taskCronerContainer.configureTask(taskCroner, serverConfig);
                    log.info("TaskCroner: [{}]默认配置补传 task-center 成功，已切换为 center 配置(id={})。", taskClass, serverConfig.getId());
                } else {
                    log.warn("TaskCroner: [{}]默认配置补传 task-center 失败，下一周期继续重试（当前仍以本地默认配置运行）。", taskClass);
                }
            } catch (Exception e) {
                log.warn("TaskCroner: [{}]默认配置补传 task-center 异常，下一周期继续重试: {}", taskClass, e.getMessage());
            }
        }
    }

    /**
     * 构造待上传的本地默认配置：规整取值，taskClass 与 runTarget 由框架设死（防止调用方误填）。
     *
     * @param taskClass   任务类全限定名
     * @param localConfig initConfig 返回的默认配置
     * @param contact     联系人信息（taskClass 同步设死）
     * @return 规整后的本地默认配置
     */
    private TaskCronerConfig buildLocalCronerConfig(String taskClass, TaskCronerConfig localConfig, TaskContact contact) {
        TaskConfigValidator.normalize(localConfig);
        localConfig.setTaskClass(taskClass);
        localConfig.setRunTarget(taskProperties.getRunTarget());
        contact.setTaskClass(taskClass);
        return localConfig;
    }
}
