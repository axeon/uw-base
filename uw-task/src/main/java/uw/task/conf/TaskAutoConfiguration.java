package uw.task.conf;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.lettuce.core.resource.ClientResources;
import jakarta.annotation.PreDestroy;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import uw.log.es.LogClient;
import uw.task.TaskFactory;
import uw.task.TaskListenerManager;
import uw.task.api.TaskApiClient;
import uw.task.container.TaskCronerContainer;
import uw.task.container.TaskRunnerContainer;
import uw.task.converter.TaskMessageConverter;
import uw.task.entity.TaskCronerLog;
import uw.task.entity.TaskRunnerLog;
import uw.task.util.TaskGlobalLocker;
import uw.task.util.TaskGlobalRateLimiter;
import uw.task.util.TaskLocalRateLimiter;
import uw.task.util.TaskSequenceManager;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 启动配置。
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties({TaskProperties.class})
@AutoConfigureAfter({RedisAutoConfiguration.class, RabbitAutoConfiguration.class})
public class TaskAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger( TaskAutoConfiguration.class );

    /**
     * 任务meta信息管理器。
     */
    TaskMetaInfoManager taskMetaInfoManager;

    /**
     * 是否已初始化配置，保证只初始化一次；
     */
    private AtomicBoolean initFlag = new AtomicBoolean( false );

    /**
     * 服务端任务配置。
     */
    private TaskServiceRegister serverConfig;

    /**
     * 定时任务容器。
     */
    private TaskCronerContainer taskCronerContainer;

    /**
     * 队列任务容器。
     */
    private TaskRunnerContainer taskRunnerContainer;

    /**
     * Leader选举器
     */
    private TaskGlobalLocker taskGlobalLocker;
    /**
     * 内部自持任务。
     */
    private ScheduledExecutorService scheduledExecutorService;

    /**
     * 注册日志对象
     *
     * @param logClient
     * @return
     */
    @Bean
    public CommandLineRunner configTaskLogClient(final LogClient logClient) {
        return args -> {
            // 登录日志查询
            logClient.regLogObjectWithIndexPattern( TaskRunnerLog.class, "yyyyMM" );
            // 操作日志
            logClient.regLogObjectWithIndexPattern( TaskCronerLog.class, "yyyyMM" );
        };
    }

    /**
     * ApplicationReadyEvent初始化完成或刷新后执行init方法
     */
    @EventListener(ApplicationReadyEvent.class)
    public void handleContextRefresh() {
        if (initFlag.compareAndSet( false, true )) {
            taskMetaInfoManager.init();
            //task自持任务
            if (serverConfig.isEnableRegistry()) {
                scheduledExecutorService = Executors.newScheduledThreadPool( 3, new ThreadFactoryBuilder().setDaemon( true ).setNameFormat( "uw-task-service-%d" ).build() );
                //主机报告任务
                scheduledExecutorService.scheduleAtFixedRate( () -> serverConfig.reportHostInfo(), 20, 180, TimeUnit.SECONDS );
                //选举任务
                scheduledExecutorService.scheduleAtFixedRate( () -> taskGlobalLocker.checkLock(), 0, 60, TimeUnit.SECONDS );
            } else {
                scheduledExecutorService = Executors.newScheduledThreadPool( 1, new ThreadFactoryBuilder().setDaemon( true ).setNameFormat( "uw-task-service-%d" ).build() );
            }
            //配置更新任务
            scheduledExecutorService.scheduleAtFixedRate( () -> serverConfig.updateConfig(), 0, 60, TimeUnit.SECONDS );
        }
    }

    @PreDestroy
    public void destroy() {
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
        }
        serverConfig.stopAllTaskRunner();
        taskCronerContainer.stopAllTaskCroner();
    }

    /**
     * 获得任务自定义的rabbitConnectionFactory
     *
     * @param rabbitProperties RabbitMQ配置
     * @return ConnectionFactory
     */
    private ConnectionFactory getTaskRabbitConnectionFactory(TaskProperties.RabbitProperties rabbitProperties) {
        PropertyMapper map = PropertyMapper.get();
        RabbitConnectionFactoryBean factoryBean = new RabbitConnectionFactoryBean();
        map.from( rabbitProperties::determineHost ).whenNonNull().to( factoryBean::setHost );
        map.from( rabbitProperties::determinePort ).to( factoryBean::setPort );
        map.from( rabbitProperties::determineUsername ).whenNonNull().to( factoryBean::setUsername );
        map.from( rabbitProperties::determinePassword ).whenNonNull().to( factoryBean::setPassword );
        map.from( rabbitProperties::determineVirtualHost ).whenNonNull().to( factoryBean::setVirtualHost );
        map.from( rabbitProperties::getRequestedHeartbeat ).whenNonNull().asInt( Duration::getSeconds ).to( factoryBean::setRequestedHeartbeat );
        RabbitProperties.Ssl ssl = rabbitProperties.getSsl();
        if (ssl.getEnabled() != null && ssl.getEnabled()) {
            factoryBean.setUseSSL( true );
            map.from( ssl::getAlgorithm ).whenNonNull().to( factoryBean::setSslAlgorithm );
            map.from( ssl::getKeyStoreType ).to( factoryBean::setKeyStoreType );
            map.from( ssl::getKeyStore ).to( factoryBean::setKeyStore );
            map.from( ssl::getKeyStorePassword ).to( factoryBean::setKeyStorePassphrase );
            map.from( ssl::getTrustStoreType ).to( factoryBean::setTrustStoreType );
            map.from( ssl::getTrustStore ).to( factoryBean::setTrustStore );
            map.from( ssl::getTrustStorePassword ).to( factoryBean::setTrustStorePassphrase );
        }
        map.from( rabbitProperties::getConnectionTimeout ).whenNonNull().asInt( Duration::toMillis ).to( factoryBean::setConnectionTimeout );
        try {
            factoryBean.afterPropertiesSet();
        } catch (Exception e) {
            log.error( "配置RabbitConnectionFactoryBean出错", e );
        }

        CachingConnectionFactory connFactory = null;
        try {
            connFactory = new CachingConnectionFactory( factoryBean.getObject() );
        } catch (Exception e) {
            log.error( "获取ConnectionFactory出错", e );
        }
        map.from( rabbitProperties::determineAddresses ).to( connFactory::setAddresses );
        // 消息确认
        // map.from(rabbitProperties::isPublisherConfirms).to(connFactory::setPublisherConfirms);
        map.from( rabbitProperties::getPublisherConfirmType ).to( connFactory::setPublisherConfirmType );
        map.from( rabbitProperties::isPublisherReturns ).to( connFactory::setPublisherReturns );
        RabbitProperties.Cache.Channel channel = rabbitProperties.getCache().getChannel();
        map.from( channel::getSize ).whenNonNull().to( connFactory::setChannelCacheSize );
        map.from( channel::getCheckoutTimeout ).whenNonNull().as( Duration::toMillis ).to( connFactory::setChannelCheckoutTimeout );
        RabbitProperties.Cache.Connection connection = rabbitProperties.getCache().getConnection();
        map.from( connection::getMode ).whenNonNull().to( connFactory::setCacheMode );
        map.from( connection::getSize ).whenNonNull().to( connFactory::setConnectionCacheSize );

        try {
            connFactory.afterPropertiesSet();
        } catch (Exception e) {
            log.error( "配置CachingConnectionFactory出错", e );
        }
        return connFactory;
    }

    /**
     * 自定义的redis链接工厂类。
     *
     * @param redisProperties
     * @param clientResources
     * @return
     */
    private RedisConnectionFactory getTaskRedisConnectionFactory(final TaskProperties.RedisProperties redisProperties, final ClientResources clientResources) {
        RedisProperties.Pool pool = redisProperties.getLettuce().getPool();
        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder;
        if (pool == null) {
            builder = LettuceClientConfiguration.builder();
        } else {
            GenericObjectPoolConfig config = new GenericObjectPoolConfig();
            config.setMaxTotal( pool.getMaxActive() );
            config.setMaxIdle( pool.getMaxIdle() );
            config.setMinIdle( pool.getMinIdle() );
            if (pool.getMaxWait() != null) {
                config.setMaxWait( pool.getMaxWait() );
            }
            builder = LettucePoolingClientConfiguration.builder().poolConfig( config );
        }

        if (redisProperties.getTimeout() != null) {
            builder.commandTimeout( redisProperties.getTimeout() );
        }
        if (redisProperties.getLettuce() != null) {
            RedisProperties.Lettuce lettuce = redisProperties.getLettuce();
            if (lettuce.getShutdownTimeout() != null && !lettuce.getShutdownTimeout().isZero()) {
                builder.shutdownTimeout( redisProperties.getLettuce().getShutdownTimeout() );
            }
        }
        builder.clientResources( clientResources );
        LettuceClientConfiguration config = builder.build();

        RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration();
        standaloneConfig.setHostName( redisProperties.getHost() );
        standaloneConfig.setPort( redisProperties.getPort() );
        standaloneConfig.setPassword( RedisPassword.of( redisProperties.getPassword() ) );
        standaloneConfig.setDatabase( redisProperties.getDatabase() );

        LettuceConnectionFactory factory = new LettuceConnectionFactory( standaloneConfig, config );
        factory.afterPropertiesSet();
        return factory;
    }

    /**
     * 转换器用TaskMessageConverter转换实体类对象。
     *
     * @param connectionFactory
     * @return RabbitTemplate
     */
    private RabbitTemplate getTaskRabbitTemplate(final ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate( connectionFactory );
        template.setMessageConverter( new TaskMessageConverter() );
        template.setReplyTimeout( 180000 );
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 任务监听管理器。
     *
     * @return
     */
    @Bean
    TaskListenerManager taskListenerManager() {
        return new TaskListenerManager();
    }

    /**
     * 声明 TaskFactory bean
     *
     * @param context
     * @param taskProperties
     * @param tokenRestTemplate
     * @param clientResources
     * @param logClient
     * @return
     */
    @Bean
    TaskFactory taskFactory(final ApplicationContext context, final TaskProperties taskProperties, final TaskListenerManager taskListenerManager,
                            @Qualifier("tokenRestTemplate") final RestTemplate tokenRestTemplate, final ClientResources clientResources, final LogClient logClient) {
        // task自定义的rabbit连接工厂
        ConnectionFactory taskRabbitConnectionFactory = getTaskRabbitConnectionFactory( taskProperties.getRabbitmq() );
        // task自定义的redis连接工厂
        RedisConnectionFactory taskRedisConnectionFactory = getTaskRedisConnectionFactory( taskProperties.getRedis(), clientResources );
        // 本地限速器。
        TaskLocalRateLimiter taskLocalRateLimiter = new TaskLocalRateLimiter();
        // 全局限速器
        TaskGlobalRateLimiter taskGlobalRateLimiter = new TaskGlobalRateLimiter( taskRedisConnectionFactory );
        // 全局sequence管理器
        TaskSequenceManager taskSequenceManager = new TaskSequenceManager( taskRedisConnectionFactory );
        // 任务交互API
        TaskApiClient taskApiClient = new TaskApiClient( taskProperties, tokenRestTemplate, logClient );
        // rabbit模板
        RabbitTemplate rabbitTemplate = getTaskRabbitTemplate( taskRabbitConnectionFactory );
        // rabbit管理器
        RabbitAdmin rabbitAdmin = new RabbitAdmin( taskRabbitConnectionFactory );
        // Leader选举器
        taskGlobalLocker = new TaskGlobalLocker( taskRedisConnectionFactory, taskProperties );
        // 任务信息管理器。
        taskMetaInfoManager = new TaskMetaInfoManager( context, taskProperties );
        // 队列任务容器。
        taskRunnerContainer = new TaskRunnerContainer( taskProperties, taskApiClient, taskLocalRateLimiter, taskGlobalRateLimiter, taskListenerManager, taskMetaInfoManager );
        // 定时任务容器。
        taskCronerContainer = new TaskCronerContainer( taskProperties, taskApiClient, taskSequenceManager, taskListenerManager, taskGlobalLocker );
        // 初始化TaskServerConfig
        serverConfig = new TaskServiceRegister( taskProperties, taskApiClient, taskRunnerContainer, taskCronerContainer, taskRabbitConnectionFactory, rabbitAdmin,
                taskMetaInfoManager );
        // 返回TaskScheduler
        TaskFactory taskFactory = new TaskFactory( taskProperties, rabbitTemplate, taskRunnerContainer, taskSequenceManager, taskMetaInfoManager );
        // taskRunnerContainer错误重试需要TaskScheduler
        taskRunnerContainer.setTaskFactory( taskFactory );
        return taskFactory;
    }
}
