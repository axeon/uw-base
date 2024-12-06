package uw.task.conf;

import com.rabbitmq.client.AMQP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import uw.task.TaskCroner;
import uw.task.TaskRunner;
import uw.task.api.TaskApiClient;
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
 * 任务服务注册器。
 *
 * @param
 * @author axeon
 */
public class TaskServiceRegister {

    private static final Logger log = LoggerFactory.getLogger( TaskServiceRegister.class );

    private final ConnectionFactory taskConnectionFactory;

    private final TaskRunnerContainer taskRunnerContainer;

    private final TaskCronerContainer taskCronerContainer;

    private final TaskProperties taskProperties;

    private final TaskApiClient taskApiClient;

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
     * 上次更新配置时间，初始值必须=0，用于标识整体加载。
     */
    private long lastUpdateTime = 0;

    /**
     * 从服务器端拉取数据是否成功。
     */
    private boolean updateFlag = true;

    /**
     * 是否首次启动
     */
    private boolean isFirstRun = true;

    /**
     * 上次报告结果。
     */
    private HostReportResponse reportResponse;

    /**
     * 默认构造器
     *
     * @param taskProperties
     * @param taskApiClient
     * @param taskRunnerContainer
     * @param taskCronerContainer
     * @param taskRabbitConnectionFactory
     * @param rabbitAdmin
     */
    public TaskServiceRegister(TaskProperties taskProperties, TaskApiClient taskApiClient, TaskRunnerContainer taskRunnerContainer, TaskCronerContainer taskCronerContainer,
                               ConnectionFactory taskRabbitConnectionFactory,
                               RabbitAdmin rabbitAdmin, TaskMetaInfoManager taskMetaInfoManager) {
        this.taskProperties = taskProperties;
        this.taskConnectionFactory = taskRabbitConnectionFactory;
        this.taskRunnerContainer = taskRunnerContainer;
        this.taskCronerContainer = taskCronerContainer;
        this.taskApiClient = taskApiClient;
        this.rabbitAdmin = rabbitAdmin;
        this.taskMetaInfoManager = taskMetaInfoManager;
    }

    /**
     * 是否开启任务注册。
     *
     * @return
     */
    public boolean isEnableRegistry() {
        return taskProperties.isEnableRegistry();
    }


    /**
     * 注册当前所有的服务。 每隔1分钟刷新一次。
     */
    public void updateConfig() {
        long startUpdateTimeMills = System.currentTimeMillis();
        // 优先拉取runnerConfig，因为是否注册任务，都要更新系统队列。
        ConcurrentHashMap<String, TaskRunnerConfig> updatedRunnerConfigMap = updateRunnerConfig();
        if (!updateFlag) {
            log.warn( "!拉取TaskRunner服务器配置失败, 进入Fail-Fast模式!" );
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug( "发现服务器端拉取的[{}]个更新的TaskRunner配置...", updatedRunnerConfigMap.size() );
        }
        // 取得有变化的croner配置列表
        ConcurrentHashMap<String, TaskCronerConfig> updatedCronerConfigMap = updateCronerConfig();
        if (!updateFlag) {
            log.warn( "!拉取TaskCroner服务器配置失败, 进入Fail-Fast模式!" );
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug( "发现服务器端拉取的[{}]个更新的TaskCroner配置...", updatedCronerConfigMap.size() );
        }
        //非后台任务主机，此时可以退出了。
        if (!taskProperties.isEnableRegistry()) {
            return;
        }
        // 第一次执行初始化操作
        if (isFirstRun) {
            isFirstRun = false;
            // 初始化croner和runner
            for (Entry<String, TaskCroner> kv : taskMetaInfoManager.getCronerInstanceMap().entrySet()) {
                try {
                    // 拿到任务类名
                    TaskCroner taskCroner = kv.getValue();
                    String taskClass = taskCroner.getClass().getName();
                    if (log.isDebugEnabled()) {
                        log.debug( "正在初始化TaskCroner: [{}].", taskClass );
                    }
                    TaskCronerConfig localConfig = taskCroner.initConfig();
                    TaskContact contact = taskCroner.initContact();
                    if (localConfig == null || contact == null) {
                        log.warn( "!TaskCroner: [{}]默认配置或联系人信息为空，无法启动！", taskClass );
                        continue;
                    }
                    // 防止有人瞎胡搞，任务参数强制设死。
                    localConfig.setTaskClass( taskClass );
                    localConfig.setRunTarget( taskProperties.getRunTarget() );
                    contact.setTaskClass( taskClass );
                    String configKey = taskMetaInfoManager.getCronerConfigKey( localConfig );
                    TaskCronerConfig serverConfig = updatedCronerConfigMap.get( configKey );
                    // 上传配置
                    if (serverConfig == null) {
                        if (log.isDebugEnabled()) {
                            log.debug( "TaskCroner: [{}]未找到服务器端配置，上传默认配置...", taskClass );
                        }
                        serverConfig = uploadCronerInfo( localConfig, contact );
                    }
                } catch (Exception e) {
                    log.error( e.getMessage(), e );
                }
            }

            // 获得当前主机上所有的TaskRunner
            for (Entry<String, TaskRunner> kv : taskMetaInfoManager.getRunnerInstanceMap().entrySet()) {
                try {
                    // 拿到任务类名
                    TaskRunner<?, ?> taskRunner = kv.getValue();
                    String taskClass = kv.getKey();
                    TaskRunnerConfig localConfig = taskRunner.initConfig();
                    TaskContact contact = taskRunner.initContact();
                    if (localConfig == null || contact == null) {
                        log.warn( "!TaskRunner: [{}]默认配置或联系人信息为空，无法启动！", taskClass );
                        continue;
                    }
                    if (log.isDebugEnabled()) {
                        log.debug( "正在初始化TaskRunner: [{}].", taskClass );
                    }
                    // 防止有人瞎胡搞，任务参数强制设死。
                    localConfig.setTaskClass( taskClass );
                    localConfig.setRunTarget( taskProperties.getRunTarget() );
                    contact.setTaskClass( taskClass );
                    String configKey = taskMetaInfoManager.getRunnerConfigKey( localConfig );
                    TaskRunnerConfig serverConfig = updatedRunnerConfigMap.get( configKey );
                    // 上传配置
                    if (serverConfig == null) {
                        if (log.isDebugEnabled()) {
                            log.debug( "TaskRunner: [{}]未找到服务器端配置，上传默认配置...", taskClass );
                        }
                        serverConfig = uploadRunnerInfo( localConfig, contact );
                    }
                } catch (Exception e) {
                    log.error( e.getMessage(), e );
                }
            }
            // 当未连接服务器的时候，必须用configMap。
            updatedCronerConfigMap = taskMetaInfoManager.getCronerConfigMap();
            updatedRunnerConfigMap = taskMetaInfoManager.getRunnerConfigMap();
        }

        // 此时执行更新操作
        if (updatedCronerConfigMap != null) {
            for (TaskCronerConfig taskCronerConfig : updatedCronerConfigMap.values()) {
                //必须是本项目任务，否则跳过
                if (!taskCronerConfig.getTaskClass().startsWith( taskProperties.getTaskProject() )) {
                    if (log.isDebugEnabled()) {
                        log.debug( "因为TaskProject不匹配，跳过配置ID: [{}], CRONER:[{}], CRON:[{}].", taskCronerConfig.getId(), taskCronerConfig.getTaskClass(),
                                taskCronerConfig.getTaskCron() );
                    }
                    continue;
                }
                //必须是同一个runTarget项目，否则跳过
                if (!taskCronerConfig.getRunTarget().equals( taskProperties.getRunTarget() )) {
                    if (log.isDebugEnabled()) {
                        log.debug( "因为RunTarget不匹配，跳过配置ID: [{}], CRONER:[{}], CRON:[{}].", taskCronerConfig.getId(), taskCronerConfig.getTaskClass(),
                                taskCronerConfig.getTaskCron() );
                    }
                    continue;
                }
                if (log.isDebugEnabled()) {
                    log.debug( "正在加载ID: [{}], CRONER:[{}], CRON:[{}].", taskCronerConfig.getId(), taskCronerConfig.getTaskClass(), taskCronerConfig.getTaskCron() );
                }
                TaskCroner taskCroner = taskMetaInfoManager.getCronerInstance( taskCronerConfig.getTaskClass() );
                if (taskCroner != null) {
                    try {
                        if (log.isDebugEnabled()) {
                            log.debug( "正在配置ID: [{}], CRONER:[{}], CRON:[{}].", taskCronerConfig.getId(), taskCronerConfig.getTaskClass(), taskCronerConfig.getTaskCron() );
                        }
                        boolean runFlag = taskCronerContainer.configureTask( taskCroner, taskCronerConfig );
                    } catch (Exception e) {
                        log.error( e.getMessage(), e );
                    }
                } else {
                    log.warn( "!未找到匹配的任务类，未能启动配置ID: [{}], CRONER:[{}], CRON:[{}].", taskCronerConfig.getId(), taskCronerConfig.getTaskClass(), taskCronerConfig.getTaskCron() );
                }
            }
        }
        // 改Runner配置，逐一循环并配置。
        if (updatedRunnerConfigMap != null) {
            for (TaskRunnerConfig taskRunnerConfig : updatedRunnerConfigMap.values()) {
                //必须是本项目任务，否则跳过
                if (!taskRunnerConfig.getTaskClass().startsWith( taskProperties.getTaskProject() )) {
                    if (log.isDebugEnabled()) {
                        log.debug( "因为TaskProject不匹配，跳过配置ID: [{}], RUNNER: [{}].", taskRunnerConfig.getId(), taskRunnerConfig.getTaskClass() );
                    }
                    continue;
                }
                //必须是同一个runTarget项目，否则跳过
                if (!taskRunnerConfig.getRunTarget().equals( taskProperties.getRunTarget() )) {
                    if (log.isDebugEnabled()) {
                        log.debug( "因为RunTarget不匹配，跳过配置ID: [{}], RUNNER: [{}].", taskRunnerConfig.getId(), taskRunnerConfig.getTaskClass() );
                    }
                    continue;
                }
                if (log.isDebugEnabled()) {
                    log.debug( "正在加载ID: [{}], RUNNER: [{}].", taskRunnerConfig.getId(), taskRunnerConfig.getTaskClass() );
                }
                if (taskMetaInfoManager.getRunnerInstance( taskRunnerConfig.getTaskClass() ) != null) {
                    // 启动任务
                    registerQueueListener( taskRunnerConfig );
                } else {
                    log.warn( "!未找到匹配的任务类，未能启动配置ID: [{}], RUNNER: [{}].", taskRunnerConfig.getId(), taskRunnerConfig.getTaskClass() );
                }
            }
        }

        // 执行成功才会更新时间戳
        if (updateFlag) {
            // 比对数据库的时候，因为数据库缺少ms值，所以减去5000，提高匹配度。
            lastUpdateTime = startUpdateTimeMills - 5000;
        }

    }

    /**
     * 停止所有的任务。
     */
    public void stopAllTaskRunner() {
        log.info( "All TaskRunner Destroy...." );
        for (SimpleMessageListenerContainer container : queueListenerMap.values()) {
            container.shutdown();
            container.stop();
        }
    }

    /**
     * 报告主机状态。
     *
     * @return
     */
    public HostReportResponse reportHostInfo() {
        //最后提交主机状态报告。
        if (log.isDebugEnabled()) {
            log.debug( "正在提交主机状态报告..." );
        }
        TaskHostStats hostStats = new TaskHostStats();
        if (reportResponse != null && reportResponse.getId() > 0) {
            hostStats.setId( reportResponse.getId() );
        }
        hostStats.setAppName( taskProperties.getAppName() );
        hostStats.setAppVersion( taskProperties.getAppVersion() );
        hostStats.setAppHost( taskProperties.getAppHost() );
        hostStats.setAppPort( taskProperties.getAppPort() );
        hostStats.setTaskProject( taskProperties.getTaskProject() );
        hostStats.setRunTarget( taskProperties.getRunTarget() );
        hostStats.setCronerNum( taskMetaInfoManager.getCronerInstanceMap().size() );
        hostStats.setRunnerNum( taskMetaInfoManager.getRunnerInstanceMap().size() );
        //设置内存和线程信息。
        Runtime runtime = Runtime.getRuntime();
        hostStats.setJvmMemMax( runtime.maxMemory() );
        hostStats.setJvmMemTotal( runtime.totalMemory() );
        hostStats.setJvmMemFree( runtime.freeMemory() );
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        hostStats.setThreadActive( threadMXBean.getThreadCount() );
        hostStats.setThreadDaemon( threadMXBean.getDaemonThreadCount() );
        hostStats.setThreadPeak( threadMXBean.getPeakThreadCount() );
        hostStats.setThreadStarted( threadMXBean.getTotalStartedThreadCount() );
        //保存任务统计信息。
        hostStats.setTaskCronerStatsList( TaskStatsService.getCronerStats() );
        hostStats.setTaskRunnerStatsList( TaskStatsService.getRunnerStats() );
        //查询队列信息。
        List<TaskRunnerStats> taskRunnerStatsList = hostStats.getTaskRunnerStatsList();
        //查询缓存，应对公用队列的情况，防止重复查询。key:队列名。
        Map<String, AMQP.Queue.DeclareOk> queueInfoMap = new HashMap<>();
        for (TaskRunnerStats stats : taskRunnerStatsList) {
            for (TaskRunnerConfig taskRunnerConfig : taskMetaInfoManager.getRunnerConfigMap().values()) {
                if (stats.getTaskId() == taskRunnerConfig.getId()) {
                    String queueName = taskMetaInfoManager.getQueueNameByConfig( taskRunnerConfig );
                    AMQP.Queue.DeclareOk declareOk = queueInfoMap.computeIfAbsent( queueName,
                            (Key) -> rabbitAdmin.getRabbitTemplate().execute( channel -> channel.queueDeclarePassive( queueName ) ) );
                    stats.setQueueSize( declareOk.getMessageCount() );
                    stats.setConsumerNum( declareOk.getConsumerCount() );
                    break;
                }
            }
        }
        reportResponse = taskApiClient.reportHostInfo( hostStats );
        //最后提交主机状态报告。
        if (log.isDebugEnabled()) {
            log.debug( "完成提交主机状态报告..." );
        }
        return reportResponse;

    }

    /**
     * 更新所有Croner的配置
     *
     * @return
     */
    private ConcurrentHashMap<String, TaskCronerConfig> updateCronerConfig() {
        ConcurrentHashMap<String, TaskCronerConfig> map = new ConcurrentHashMap<>();
        List<TaskCronerConfig> list = taskApiClient.getTaskCronerConfigList( taskProperties.getRunTarget(), taskProperties.getTaskProject(), lastUpdateTime );
        if (list != null) {
            for (TaskCronerConfig config : list) {
                String key = taskMetaInfoManager.getCronerConfigKey( config );
                map.put( key, config );
                taskMetaInfoManager.getCronerConfigMap().put( key, config );
            }
            updateFlag = true;
        } else {
            updateFlag = false;
        }
        return map;
    }

    /**
     * 更新所有Runner的配置
     *
     * @return
     */
    private ConcurrentHashMap<String, TaskRunnerConfig> updateRunnerConfig() {
        ConcurrentHashMap<String, TaskRunnerConfig> map = new ConcurrentHashMap<>();
        List<TaskRunnerConfig> list = taskApiClient.getTaskRunnerConfigList( taskProperties.getRunTarget(), taskProperties.getTaskProject(), lastUpdateTime );
        if (list != null) {
            for (TaskRunnerConfig config : list) {
                String key = taskMetaInfoManager.getRunnerConfigKey( config );
                if (config.getState() < 1) {
                    taskMetaInfoManager.removeRunnerConfig( key );
                } else {
                    taskMetaInfoManager.setRunnerConfig( key, config );
                }
                map.put( key, config );
            }
            updateFlag = true;
        } else {
            updateFlag = false;
        }
        return map;
    }

    /**
     * 上传Runner信息。
     *
     * @param config
     * @param contact
     */
    private TaskRunnerConfig uploadRunnerInfo(TaskRunnerConfig config, TaskContact contact) {
        config = taskApiClient.initTaskRunnerConfig( config );
        taskApiClient.initTaskContact( contact );
        taskMetaInfoManager.setRunnerConfig( taskMetaInfoManager.getRunnerConfigKey( config ), config );
        return config;
    }

    /**
     * 上传Croner信息。
     *
     * @param config
     * @param contact
     */
    private TaskCronerConfig uploadCronerInfo(TaskCronerConfig config, TaskContact contact) {
        config = taskApiClient.initTaskCronerConfig( config );
        taskApiClient.initTaskContact( contact );
        taskMetaInfoManager.setCronerConfig( taskMetaInfoManager.getCronerConfigKey( config ), config );
        return config;
    }

    /**
     * 注册队列监听。
     *
     * @param runnerConfig
     */
    private void registerQueueListener(TaskRunnerConfig runnerConfig) {
        String queueName = taskMetaInfoManager.getQueueNameByConfig( runnerConfig );
        SimpleMessageListenerContainer sysContainer = queueListenerMap.computeIfAbsent( queueName, key -> {
            if (runnerConfig.getState() != 1) {
                log.warn( "!TaskRunner: [{}]状态为暂停，不进行注册。。。", queueName );
                return null;
            }
            if (log.isDebugEnabled()) {
                log.debug( "TaskRunner: [{}]正在注册并启动监听...", queueName );
            }
            try {
                Queue queue = QueueBuilder.durable( queueName ).build();
                // 声明处理
                declareManage( queue );
                // 启动任务监听器
                SimpleMessageListenerContainer container = bindMessageListenerContainer( runnerConfig, queueName );
                if (runnerConfig.getDelayType() == TaskRunnerConfig.TYPE_DELAY_ON) {
                    // 建立TTL队列，需要设置名称
                    Queue ttlQueue =
                            QueueBuilder.durable( taskMetaInfoManager.getTTLQueueName( queueName ) ).withArgument( "x-dead-letter-exchange", queueName ).withArgument( "x"
                                    + "-dead-letter-routing-key", queueName ).build();
                    // 声明处理
                    declareManage( ttlQueue );
                }
                return container;
            } catch (Exception e) {
                log.error( "任务队列创建失败! {}", e.getMessage(), e );
            }
            return null;
        } );
        if (sysContainer != null) {
            if (runnerConfig.getState() == 1) {
                try {
                    //只有runner的queueType才可以在线设置。对于配置了死信队列，此处消费者配置为配置死信队列消费
                    if (runnerConfig.getQueueType() == TaskRunnerConfig.TYPE_QUEUE_TASK) {
                        sysContainer.setMaxConcurrentConsumers( runnerConfig.getConsumerNum() );
                        sysContainer.setConcurrentConsumers( (int) Math.ceil( runnerConfig.getConsumerNum() * 0.1f ) );
                        sysContainer.setPrefetchCount( runnerConfig.getPrefetchNum() );
                    }
                } catch (Exception e) {
                    log.error( e.getMessage(), e );
                }
            } else {
                queueListenerMap.remove( queueName );
                sysContainer.shutdown();
                sysContainer.stop();
            }
        }
    }

    /**
     * 声明管理
     *
     * @param queue 队列
     */
    private void declareManage(Queue queue) {
        // 交换机
        Exchange exchange = ExchangeBuilder.directExchange( queue.getName() ).durable( true ).build();
        // 队列和交换机绑定
        Binding binding = BindingBuilder.bind( queue ).to( exchange ).with( queue.getName() ).noargs();
        // 定义队列
        rabbitAdmin.declareQueue( queue );
        // 定义交换机
        rabbitAdmin.declareExchange( exchange );
        // 队列和交换机绑定
        rabbitAdmin.declareBinding( binding );
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
        container.setAutoStartup( false );
        container.setTaskExecutor( new SimpleAsyncTaskExecutor( queueName ) );
        // 提高启动consumer速度。
        container.setStartConsumerMinInterval( 100 );
        container.setConsecutiveActiveTrigger( 1 );
        container.setStopConsumerMinInterval( 20000 );
        container.setConsecutiveIdleTrigger( 3 );
        switch (runnerConfig.getQueueType()) {
            case TaskRunnerConfig.TYPE_QUEUE_TASK:
                container.setMaxConcurrentConsumers( runnerConfig.getConsumerNum() );
                container.setConcurrentConsumers( (int) Math.ceil( runnerConfig.getConsumerNum() * 0.1f ) );
                container.setPrefetchCount( runnerConfig.getPrefetchNum() );
                break;
            case TaskRunnerConfig.TYPE_QUEUE_GROUP_PRIORITY:
                container.setMaxConcurrentConsumers( taskProperties.getQueueGroupPriorityThreadMaxNum() );
                container.setConcurrentConsumers( taskProperties.getQueueGroupPriorityThreadMinNum() );
                container.setPrefetchCount( taskProperties.getQueueGroupPriorityPrefetchNum() );
                break;
            case TaskRunnerConfig.TYPE_QUEUE_GROUP:
                container.setMaxConcurrentConsumers( taskProperties.getQueueGroupDefaultThreadMaxNum() );
                container.setConcurrentConsumers( taskProperties.getQueueGroupDefaultThreadMinNum() );
                container.setPrefetchCount( taskProperties.getQueueGroupDefaultPrefetchNum() );
                break;
            case TaskRunnerConfig.TYPE_QUEUE_PROJECT_PRIORITY:
                container.setMaxConcurrentConsumers( taskProperties.getQueueProjectPriorityThreadMaxNum() );
                container.setConcurrentConsumers( taskProperties.getQueueProjectPriorityThreadMinNum() );
                container.setPrefetchCount( taskProperties.getQueueProjectPriorityPrefetchNum() );
                break;
            default:
                container.setMaxConcurrentConsumers( taskProperties.getQueueProjectDefaultThreadMaxNum() );
                container.setConcurrentConsumers( taskProperties.getQueueProjectDefaultThreadMinNum() );
                container.setPrefetchCount( taskProperties.getQueueProjectDefaultPrefetchNum() );
        }
        container.setConnectionFactory( taskConnectionFactory );
        container.setAcknowledgeMode( AcknowledgeMode.AUTO );
        container.setQueueNames( queueName );
        MessageListenerAdapter listenerAdapter = new MessageListenerAdapter( taskRunnerContainer, "process" );
        listenerAdapter.setMessageConverter( new TaskMessageConverter() );
        container.setMessageListener( listenerAdapter );
        // 如果发生错误不再重新排队，我们自己catch异常处理
        container.setDefaultRequeueRejected( false );
        container.setAutoStartup( true );
        container.afterPropertiesSet();
        container.start();
        return container;
    }

}
