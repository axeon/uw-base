package uw.task.conf;

import com.rabbitmq.client.AMQP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.response.ResponseData;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import uw.common.util.SystemClock;
import uw.task.TaskCroner;
import uw.task.TaskRunner;
import uw.task.api.TaskApiClient;
import uw.task.TaskDelayer;
import uw.task.container.TaskDelayerContainer;
import uw.task.container.TaskCronerContainer;
import uw.task.container.TaskRunnerContainer;
import uw.task.converter.TaskMessageConverter;
import uw.task.entity.*;
import uw.task.util.TaskStatsService;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务服务注册器：负责与 task-center 交互，拉取/上传 runner、croner、delayer 三类任务的动态配置，
 * 并驱动各自容器的启动、热更新与暂停。
 *
 * <p>由 {@link TaskAutoConfiguration} 的调度线程池周期性调用（默认 60s）：
 * {@link #updateConfig} 编排三类配置的拉取与刷新，分别委托给三个独立方法——
 * {@link #updateTaskRunnerConfig}（基于 {@link #runnerLastUpdateTime} 增量拉取 runner 配置，首次上传本地默认配置并注册队列监听）、
 * {@link #updateTaskCronerConfig}（基于 {@link #cronerLastUpdateTime} 增量拉取 croner 配置，首次上传本地默认配置并配置调度）、
 * {@link #updateTaskDelayerConfig}（基于 {@link #delayerLastUpdateTime} 独立拉取 delay 配置）。
 * 三者各自维护独立的时间戳与 fail-fast，互不影响：runner/croner 拉取失败保留上次配置不更新，delay 拉取失败仅 warn。
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
     * 任务专用的 RabbitMQ 连接工厂（用于队列监听容器）。
     */
    private final ConnectionFactory taskConnectionFactory;

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
     * RabbitMQ 管理器（声明队列/交换机/绑定）。
     */
    private final RabbitAdmin rabbitAdmin;

    /**
     * 任务meta信息管理器。
     */
    private final TaskMetaInfoManager taskMetaInfoManager;

    /**
     * container缓存。key=队列名,value=container。
     */
    private final ConcurrentHashMap<String, SimpleMessageListenerContainer> queueListenerMap = new ConcurrentHashMap<>();

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
     * 延迟任务增量拉取独立时间戳：仅 delay 拉取成功时推进，与 runner/croner 的 lastUpdateTime 解耦，
     * 避免 runner/croner 成功推进后 delay 拉取失败 导致 delay 配置变更丢窗。
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
     * 上次报告结果。
     */
    private HostReportResponse reportResponse;

    /**
     * 默认构造器。
     *
     * @param taskProperties         任务配置
     * @param taskApiClient          任务中心 API 客户端
     * @param taskRunnerContainer    队列任务容器
     * @param taskCronerContainer    定时任务容器
     * @param taskRabbitConnectionFactory 任务专用 RabbitMQ 连接工厂
     * @param rabbitAdmin            RabbitMQ 管理器
     * @param taskMetaInfoManager    任务元信息管理器
     */
    public TaskServiceRegistrar(TaskProperties taskProperties, TaskApiClient taskApiClient, TaskRunnerContainer taskRunnerContainer, TaskCronerContainer taskCronerContainer,
                                TaskDelayerContainer taskDelayerContainer,
                                ConnectionFactory taskRabbitConnectionFactory,
                                RabbitAdmin rabbitAdmin, TaskMetaInfoManager taskMetaInfoManager) {
        this.taskProperties = taskProperties;
        this.taskConnectionFactory = taskRabbitConnectionFactory;
        this.taskRunnerContainer = taskRunnerContainer;
        this.taskCronerContainer = taskCronerContainer;
        this.taskDelayerContainer = taskDelayerContainer;
        this.taskApiClient = taskApiClient;
        this.rabbitAdmin = rabbitAdmin;
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
        updateTaskRunnerConfig();
        updateTaskCronerConfig();
        updateTaskDelayerConfig();
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
        ConcurrentHashMap<String, TaskRunnerConfig> updatedRunnerConfigMap = updateRunnerConfig();
        if (updatedRunnerConfigMap == null) {
            log.warn("!拉取TaskRunner服务器配置失败, 进入Fail-Fast模式!");
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("发现服务器端拉取的[{}]个更新的TaskRunner配置...", updatedRunnerConfigMap.size());
        }
        //非后台任务主机，此时可以退出了。
        if (!taskProperties.isEnableRegistry()) {
            return;
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
                registerQueueListener(taskRunnerConfig);
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
        ConcurrentHashMap<String, TaskCronerConfig> updatedCronerConfigMap = updateCronerConfig();
        if (updatedCronerConfigMap == null) {
            log.warn("!拉取TaskCroner服务器配置失败, 进入Fail-Fast模式!");
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("发现服务器端拉取的[{}]个更新的TaskCroner配置...", updatedCronerConfigMap.size());
        }
        //非后台任务主机，此时可以退出了。
        if (!taskProperties.isEnableRegistry()) {
            return;
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
     * 更新延迟任务配置：拉取服务端配置、首次上传本地默认配置、过滤后启动。
     * <p>独立于 runner/croner 的 fail-fast：delay 拉取失败仅 warn，不影响主流程。</p>
     */
    private void updateTaskDelayerConfig() {
        if (taskDelayerContainer == null) {
            return;
        }
        long startUpdateTimeMills = SystemClock.now();
        ResponseData<List<TaskDelayerConfig>> response = taskApiClient.getTaskDelayerConfigList(taskProperties.getRunTarget(), taskProperties.getTaskProject(), delayerLastUpdateTime);
        List<TaskDelayerConfig> serverConfigs = response != null && response.isSuccess() ? response.getData() : null;
        if (serverConfigs == null) {
            log.warn("!拉取DelayTask服务器配置失败!");
            return;
        }
        // 非后台任务主机，此时可以退出了。
        if (!taskProperties.isEnableRegistry()) {
            return;
        }
        // 首次启动：上传本地 delayer 的默认配置与联系人
        if (delayerFirstRun) {
            delayerFirstRun = false;
            for (Map.Entry<String, TaskDelayer<?, ?>> entry : taskMetaInfoManager.getDelayerInstanceMap().entrySet()) {
                try {
                    TaskDelayer<?, ?> runner = entry.getValue();
                    TaskDelayerConfig localConfig = runner.initConfig();
                    if (localConfig == null) {
                        continue;
                    }
                    localConfig.setTaskClass(entry.getKey());
                    localConfig.setRunTarget(taskProperties.getRunTarget());
                    taskApiClient.initTaskDelayerConfig(localConfig);
                    TaskContact contact = runner.initContact();
                    if (contact != null) {
                        contact.setTaskClass(entry.getKey());
                        taskApiClient.initTaskContact(contact);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        // 过滤本项目+本 runTarget 的配置，交由容器启动（服务端动态配置覆盖本地默认）
        for (TaskDelayerConfig config : serverConfigs) {
            if (!config.getTaskClass().startsWith(taskProperties.getTaskProject())) {
                continue;
            }
            if (!config.getRunTarget().equals(taskProperties.getRunTarget())) {
                continue;
            }
            taskDelayerContainer.configureTaskDelayer(config);
        }
        // 仅 delay 拉取成功才推进 delay 专属时间戳（与 runner/croner 解耦；失败不推进则下周期重查同窗口，不丢窗）
        delayerLastUpdateTime = startUpdateTimeMills - 5000;
    }

    /**
     * 停止所有的任务。
     */
    public void stopAllTaskRunner() {
        log.info("All TaskRunner Destroy....");
        for (SimpleMessageListenerContainer container : queueListenerMap.values()) {
            // shutdown() 是彻底关闭、stop() 是优雅停止；先 shutdown 后 stop 状态会异常。
            // SimpleMessageListenerContainer 的 destroy() 按 Spring 生命周期顺序处理关闭，最为稳妥。
            container.destroy();
        }
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
        hostStats.setAppHost(taskProperties.getAppHost());
        hostStats.setAppPort(taskProperties.getAppPort());
        hostStats.setTaskProject(taskProperties.getTaskProject());
        hostStats.setRunTarget(taskProperties.getRunTarget());
        hostStats.setCronerNum(taskMetaInfoManager.getCronerInstanceMap().size());
        hostStats.setRunnerNum(taskMetaInfoManager.getRunnerInstanceMap().size());
        hostStats.setDelayNum(taskDelayerContainer != null ? taskMetaInfoManager.getDelayerInstanceMap().size() : 0);
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
        java.util.List<TaskDelayerStats> delayStatsList = TaskStatsService.getDelayerStats();
        // 填充 zset 积压数与执行线程数（被动查询，stats 本身只累计计数/耗时）
        if (taskDelayerContainer != null) {
            taskDelayerContainer.fillDelayerStats(delayStatsList);
        }
        hostStats.setTaskDelayStatsList(delayStatsList);
        //查询队列信息。
        List<TaskRunnerStats> taskRunnerStatsList = hostStats.getTaskRunnerStatsList();
        // 预先构建 taskId -> queueName 索引，避免对每个 stats 遍历全部 config（O(n*m)）。
        Map<Long, String> queueNameByTaskId = new HashMap<>(taskMetaInfoManager.getRunnerConfigMap().size());
        for (TaskRunnerConfig taskRunnerConfig : taskMetaInfoManager.getRunnerConfigMap().values()) {
            queueNameByTaskId.put(taskRunnerConfig.getId(), taskMetaInfoManager.getQueueNameByConfig(taskRunnerConfig));
        }
        //查询缓存，应对公用队列的情况，防止重复查询。key:队列名。
        Map<String, AMQP.Queue.DeclareOk> queueInfoMap = new HashMap<>();
        for (TaskRunnerStats stats : taskRunnerStatsList) {
            String queueName = queueNameByTaskId.get(stats.getTaskId());
            if (queueName == null) {
                continue;
            }
            AMQP.Queue.DeclareOk declareOk = queueInfoMap.computeIfAbsent(queueName,
                    (Key) -> rabbitAdmin.getRabbitTemplate().execute(channel -> channel.queueDeclarePassive(queueName)));
            stats.setQueueSize(declareOk.getMessageCount());
            stats.setConsumerNum(declareOk.getConsumerCount());
        }
        ResponseData<HostReportResponse> reportResult = taskApiClient.reportHostInfo(hostStats);
        reportResponse = reportResult != null && reportResult.isSuccess() ? reportResult.getData() : null;
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
    private ConcurrentHashMap<String, TaskCronerConfig> updateCronerConfig() {
        ConcurrentHashMap<String, TaskCronerConfig> map = new ConcurrentHashMap<>();
        ResponseData<List<TaskCronerConfig>> response = taskApiClient.getTaskCronerConfigList(taskProperties.getRunTarget(), taskProperties.getTaskProject(), cronerLastUpdateTime);
        List<TaskCronerConfig> list = response != null && response.isSuccess() ? response.getData() : null;
        if (list != null) {
            for (TaskCronerConfig config : list) {
                String key = taskMetaInfoManager.getCronerConfigKey(config);
                map.put(key, config);
                if (config.getState() < 1) {
                    taskMetaInfoManager.removeCronerConfig(key);
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
    private ConcurrentHashMap<String, TaskRunnerConfig> updateRunnerConfig() {
        ConcurrentHashMap<String, TaskRunnerConfig> map = new ConcurrentHashMap<>();
        ResponseData<List<TaskRunnerConfig>> response = taskApiClient.getTaskRunnerConfigList(taskProperties.getRunTarget(), taskProperties.getTaskProject(), runnerLastUpdateTime);
        List<TaskRunnerConfig> list = response != null && response.isSuccess() ? response.getData() : null;
        if (list != null) {
            for (TaskRunnerConfig config : list) {
                String key = taskMetaInfoManager.getRunnerConfigKey(config);
                if (config.getState() < 1) {
                    taskMetaInfoManager.removeRunnerConfig(key);
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
     * 注册/更新 RabbitMQ 队列监听容器：首次按配置声明队列/交换机/绑定并启动监听，
     * 后续周期按 state/consumerNum/prefetchNum 动态调整或销毁。
     *
     * @param runnerConfig runner 配置信息
     */
    private void registerQueueListener(TaskRunnerConfig runnerConfig) {
        String queueName = taskMetaInfoManager.getQueueNameByConfig(runnerConfig);
        SimpleMessageListenerContainer sysContainer = queueListenerMap.computeIfAbsent(queueName, key -> {
            if (runnerConfig.getState() != 1) {
                log.warn("!TaskRunner: [{}]状态为暂停，不进行注册。。。", queueName);
                return null;
            }
            if (log.isDebugEnabled()) {
                log.debug("TaskRunner: [{}]正在注册并启动监听...", queueName);
            }
            try {
                Queue queue = QueueBuilder.durable(queueName).build();
                // 声明处理
                declareManage(queue);
                // 启动任务监听器
                SimpleMessageListenerContainer container = bindMessageListenerContainer(runnerConfig, queueName);
                if (runnerConfig.getDelayType() == TaskRunnerConfig.TYPE_DELAY_ON) {
                    // 建立TTL队列，需要设置名称
                    Queue ttlQueue =
                            QueueBuilder.durable(taskMetaInfoManager.getTTLQueueName(queueName))
                                    .withArgument("x-dead-letter-exchange", queueName)
                                    .withArgument("x-dead-letter-routing-key", queueName)
                                    .build();
                    // 声明处理
                    declareManage(ttlQueue);
                }
                return container;
            } catch (Exception e) {
                // 此处每分钟（updateConfig 周期）会重试，为避免大堆栈刷屏仅打 message；
                // 配置持续失败（如 MQ 不可用）会在后续周期自动重试恢复。
                log.warn("任务队列创建失败, 将持续重试: queue=[{}], msg={}", queueName, e.getMessage());
            }
            return null;
        });
        if (sysContainer != null) {
            if (runnerConfig.getState() == 1) {
                try {
                    //只有runner的queueType才可以在线设置。对于配置了死信队列，此处消费者配置为配置死信队列消费
                    if (runnerConfig.getQueueType() == TaskRunnerConfig.TYPE_QUEUE_TASK) {
                        sysContainer.setMaxConcurrentConsumers(runnerConfig.getConsumerNum());
                        sysContainer.setConcurrentConsumers((int) Math.ceil(runnerConfig.getConsumerNum() * 0.1f));
                        sysContainer.setPrefetchCount(runnerConfig.getPrefetchNum());
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            } else {
                queueListenerMap.remove(queueName);
                sysContainer.destroy();
            }
        }
    }

    /**
     * 通过 RabbitAdmin 声明队列、直连交换机及其绑定关系（三者同名，routing key = 队列名）。
     *
     * @param queue 队列定义
     */
    private void declareManage(Queue queue) {
        // 交换机
        Exchange exchange = ExchangeBuilder.directExchange(queue.getName()).durable(true).build();
        // 队列和交换机绑定
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(queue.getName()).noargs();
        // 定义队列
        rabbitAdmin.declareQueue(queue);
        // 定义交换机
        rabbitAdmin.declareExchange(exchange);
        // 队列和交换机绑定
        rabbitAdmin.declareBinding(binding);
    }

    /**
     * 绑定队列监听器容器
     *
     * @param runnerConfig runner配置信息
     * @param queueName    队列名称
     * @return SimpleMessageListenerContainer 监听器容器
     */
    private SimpleMessageListenerContainer bindMessageListenerContainer(TaskRunnerConfig runnerConfig, String queueName) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setAutoStartup(false);
        // 消费者线程虚拟化：basicConsume 长连接等待 + process 内的 IO/限速 sleep 都 park 在虚拟线程上，不占平台载体
        SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor(queueName);
        taskExecutor.setVirtualThreads(true);
        container.setTaskExecutor(taskExecutor);
        // 提高启动consumer速度。
        container.setStartConsumerMinInterval(100);
        container.setConsecutiveActiveTrigger(1);
        container.setStopConsumerMinInterval(20000);
        container.setConsecutiveIdleTrigger(3);
        switch (runnerConfig.getQueueType()) {
            case TaskRunnerConfig.TYPE_QUEUE_TASK:
                container.setMaxConcurrentConsumers(runnerConfig.getConsumerNum());
                container.setConcurrentConsumers((int) Math.ceil(runnerConfig.getConsumerNum() * 0.1f));
                container.setPrefetchCount(runnerConfig.getPrefetchNum());
                break;
            case TaskRunnerConfig.TYPE_QUEUE_GROUP_PRIORITY:
                container.setMaxConcurrentConsumers(taskProperties.getQueueGroupPriorityThreadMaxNum());
                container.setConcurrentConsumers(taskProperties.getQueueGroupPriorityThreadMinNum());
                container.setPrefetchCount(taskProperties.getQueueGroupPriorityPrefetchNum());
                break;
            case TaskRunnerConfig.TYPE_QUEUE_GROUP:
                container.setMaxConcurrentConsumers(taskProperties.getQueueGroupDefaultThreadMaxNum());
                container.setConcurrentConsumers(taskProperties.getQueueGroupDefaultThreadMinNum());
                container.setPrefetchCount(taskProperties.getQueueGroupDefaultPrefetchNum());
                break;
            case TaskRunnerConfig.TYPE_QUEUE_PROJECT_PRIORITY:
                container.setMaxConcurrentConsumers(taskProperties.getQueueProjectPriorityThreadMaxNum());
                container.setConcurrentConsumers(taskProperties.getQueueProjectPriorityThreadMinNum());
                container.setPrefetchCount(taskProperties.getQueueProjectPriorityPrefetchNum());
                break;
            default:
                container.setMaxConcurrentConsumers(taskProperties.getQueueProjectDefaultThreadMaxNum());
                container.setConcurrentConsumers(taskProperties.getQueueProjectDefaultThreadMinNum());
                container.setPrefetchCount(taskProperties.getQueueProjectDefaultPrefetchNum());
        }
        container.setConnectionFactory(taskConnectionFactory);
        container.setAcknowledgeMode(AcknowledgeMode.AUTO);
        container.setQueueNames(queueName);
        MessageListenerAdapter listenerAdapter = new MessageListenerAdapter(taskRunnerContainer, "process");
        listenerAdapter.setMessageConverter(new TaskMessageConverter());
        container.setMessageListener(listenerAdapter);
        // 如果发生错误不再重新排队，我们自己catch异常处理
        container.setDefaultRequeueRejected(false);
        container.setAutoStartup(true);
        container.afterPropertiesSet();
        container.start();
        return container;
    }

}
