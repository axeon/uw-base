package uw.dao.conf;

import io.lettuce.core.resource.ClientResources;
import jakarta.annotation.PreDestroy;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import uw.common.util.RedisConnectionFactoryBuilder;
import uw.dao.conf.UwDaoProperties.ConnPoolConfig;
import uw.dao.conf.UwDaoProperties.TableShardConfig;
import uw.dao.connectionpool.ConnectionManager;
import uw.dao.sequence.FusionSequenceFactory;
import uw.dao.util.DaoPropertiesManager;
import uw.dao.service.DaoService;

import java.util.Map;
import java.util.Map.Entry;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import java.util.Objects;

/**
 * spring启动配置文件.
 *
 * @author axeon
 */
@Configuration
@EnableConfigurationProperties({UwDaoProperties.class})
@AutoConfigureAfter({DataRedisAutoConfiguration.class})
public class UwDaoAutoConfiguration {

    /**
     * 日志.
     */
    private static final Logger log = LoggerFactory.getLogger(UwDaoAutoConfiguration.class);


    public UwDaoAutoConfiguration(ApplicationContext context, UwDaoProperties daoConfig, final ClientResources clientResources) {
        init(context, daoConfig, clientResources);
    }

    /**
     * 配置初始化。
     * 初始化代码放入构造器，是考虑到初始化阶段可能有数据库操作。
     */
    public void init(ApplicationContext context, UwDaoProperties daoConfig, final ClientResources clientResources) {

        log.info("uw-dao start auto configuration...");

        if (daoConfig == null) {
            log.warn("uw-dao start failed, because the config missing!!! ");
            return;
        }
        // 检测环境
        String[] activeProfiles = context.getEnvironment().getActiveProfiles();
        for (String profile : activeProfiles) {
            log.info("uw-dao detecting profile: {}", profile);
            if (StringUtils.isNotBlank(profile)) {
                profile = profile.trim().toLowerCase();
                for (String prodProfile : daoConfig.getProdProfiles()) {
                    if (profile.startsWith(prodProfile)) {
                        daoConfig.setProdProfile(true);
                        log.info("uw-dao detected prod profile: {}", profile);
                        break;
                    }
                }
                if (daoConfig.isProdProfile()) {
                    break;
                }
            }
        }

        //如果root配置没有，直接返回吧。
        ConnPoolConfig rootPoolConfig = daoConfig.getConnPool().getRoot();
        if (rootPoolConfig == null) {
            log.warn("uw-dao not found root conn config!!!");
            return;
        }

        Map<String, ConnPoolConfig> poolMap = daoConfig.getConnPool().getList();
        if (poolMap != null) {
            // 检查并填充DAOConfig默认值
            for (Entry<String, ConnPoolConfig> kv : poolMap.entrySet()) {
                ConnPoolConfig poolConfig = kv.getValue();
                if (poolConfig.getDriver() == null) {
                    poolConfig.setDriver(rootPoolConfig.getDriver());
                }
                if (poolConfig.getTestSql() == null) {
                    poolConfig.setTestSql(rootPoolConfig.getTestSql());
                }
                if (poolConfig.getMinConn() == 0) {
                    poolConfig.setMinConn(rootPoolConfig.getMinConn());
                }
                if (poolConfig.getMaxConn() == 0) {
                    poolConfig.setMaxConn(rootPoolConfig.getMaxConn());
                }
                if (poolConfig.getConnIdleTimeout() == 0) {
                    poolConfig.setConnIdleTimeout(rootPoolConfig.getConnIdleTimeout());
                }
                if (poolConfig.getConnBusyTimeout() == 0) {
                    poolConfig.setConnBusyTimeout(rootPoolConfig.getConnBusyTimeout());
                }
                if (poolConfig.getConnMaxAge() == 0) {
                    poolConfig.setConnMaxAge(rootPoolConfig.getConnMaxAge());
                }
            }
        }
        // 给值
        DaoPropertiesManager.updateProperties(daoConfig);
        // 启动连接池。
        ConnectionManager.start();
        if (daoConfig.getSqlStats().isEnable()) {
            // 加入统计日志表到sharding配置中。
            TableShardConfig config = new TableShardConfig();
            config.setShardType("date");
            config.setShardRule("day");
            config.setAutoGen(true);
            daoConfig.getTableShard().put(DaoService.STATS_BASE_TABLE, config);
        }
        DaoService.start();
        //处理Sequence问题：发号池用 dao 专属 Redis 连接（uw.dao.redis.*，独立故障域——DB 发号是关键路径，
        if (daoConfig.getRedis() != null) {
            log.info("uw-dao FusionSequenceFactory init! ");
            RedisTemplate<String, Long> redisTemplate = new RedisTemplate<>();
            redisTemplate.setKeySerializer(new StringRedisSerializer());
            redisTemplate.setValueSerializer(new GenericToStringSerializer(Long.class));
            redisTemplate.setConnectionFactory(RedisConnectionFactoryBuilder.build(daoConfig.getRedis(), clientResources));
            redisTemplate.setEnableDefaultSerializer(false);
            redisTemplate.afterPropertiesSet();
            new FusionSequenceFactory(redisTemplate);
        }
    }

    /**
     * 关闭连接管理器,销毁全部连接池.
     */
    @PreDestroy
    public void destroy() {
        log.info("uw-dao destroy configuration...");
        DaoService.stop();
        ConnectionManager.stop();
    }

    /**
     * uw.dao 属性 nacos 热更新监听器（classpath 有 spring-cloud-context 才装配；本地/纯测试环境行为不变）。
     *
     * @param daoConfig uw.dao 属性 bean
     * @return DaoPropertiesRefresher
     */
    @Bean
    @ConditionalOnClass(EnvironmentChangeEvent.class)
    public UwDaoPropertiesRefresher daoPropertiesRefresher(UwDaoProperties daoConfig) {
        return new UwDaoPropertiesRefresher(daoConfig);
    }

    /**
     * uw.dao 属性 nacos 热更新监听（EnvironmentChangeEvent 原地重绑 + 清路由缓存 + 连接池差异告警，
     * 详见方法级与原独立实现的完整语义：查表配置即时生效、连接池参数需重启）。
     */
    static class UwDaoPropertiesRefresher implements EnvironmentAware {

        /**
         * 日志器。
         */
        private static final Logger log = LoggerFactory.getLogger(UwDaoPropertiesRefresher.class);

        /**
         * uw.dao 属性 bean（Spring 管理的单例，原地重绑目标）。
         */
        private final UwDaoProperties daoProperties;

        /**
         * Spring 环境（重绑配置源）。
         */
        private Environment environment;

        /**
         * 启动时已生效的连接池参数快照（热更差异检测：变更需重启）。
         */
        private String connPoolSnapshot;

        /**
         * @param daoProperties uw.dao 属性 bean
         */
        public UwDaoPropertiesRefresher(UwDaoProperties daoProperties) {
            this.daoProperties = daoProperties;
            this.connPoolSnapshot = snapshotConnPool(daoProperties);
        }

        /**
         * 注入 Spring 环境。
         *
         * @param environment Spring 环境
         */
        @Override
        public void setEnvironment(Environment environment) {
            this.environment = environment;
        }

        /**
         * 环境变更（nacos 推送等）：原地重绑属性 → 差异检测（连接池参数变更 WARN）→ 同步静态层。
         *
         * @param event 环境变更事件（含变更的 key 集合）
         */
        @EventListener(EnvironmentChangeEvent.class)
        public void onEnvironmentChange(EnvironmentChangeEvent event) {
            // 只关心 uw.dao.* 前缀的变更（事件可能由其他配置源触发）
            boolean daoRelated = event.getKeys() == null || event.getKeys().stream().anyMatch(k -> k.startsWith("uw.dao"));
            if (!daoRelated) {
                return;
            }
            try {
                Binder.get(environment).bind("uw.dao", Bindable.ofInstance(daoProperties));
            } catch (Exception e) {
                // 配置错误：保持旧配置不半更新（bind 原地覆盖，失败时可能部分字段已写入——记录并继续同步当前态）
                log.error("uw.dao 热更新重绑失败, 保持当前生效配置: {}", e.getMessage(), e);
            }
            // 连接池参数变更检测（不重建已建池，需重启）
            String newSnapshot = snapshotConnPool(daoProperties);
            if (!Objects.equals(connPoolSnapshot, newSnapshot)) {
                log.warn("uw.dao.connPool 连接池参数发生变更：已建连接池不重建（需重启生效），新参数将在重启后应用");
                connPoolSnapshot = newSnapshot;
            }
            // 同步静态层（清路由缓存，路由/分片即时生效）
            DaoPropertiesManager.updateProperties(daoProperties);
            log.info("uw.dao 属性已热更新同步（路由/分片/环境标记即时生效，连接池参数需重启）");
        }

        /**
         * 连接池参数快照（差异检测用，内容散列无关紧要——只求可比较）。
         *
         * @param properties 属性
         * @return 快照字符串
         */
        private static String snapshotConnPool(UwDaoProperties properties) {
            StringBuilder sb = new StringBuilder();
            ConnPoolConfig root = properties.getConnPool().getRoot();
            if (root != null) {
                sb.append("root=").append(Objects.toString(root.getUrl())).append('/')
                        .append(root.getMaxConn()).append('/').append(root.getConnIdleTimeout()).append(';');
            }
            if (properties.getConnPool().getList() != null) {
                for (Map.Entry<String, ConnPoolConfig> kv : properties.getConnPool().getList().entrySet()) {
                    ConnPoolConfig c = kv.getValue();
                    sb.append(kv.getKey()).append('=').append(Objects.toString(c.getUrl())).append('/')
                            .append(c.getMaxConn()).append('/').append(c.getConnIdleTimeout()).append(';');
                }
            }
            return sb.toString();
        }
    }
}
