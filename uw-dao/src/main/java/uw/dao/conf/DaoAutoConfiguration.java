package uw.dao.conf;

import io.lettuce.core.resource.ClientResources;
import jakarta.annotation.PreDestroy;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import uw.dao.conf.DaoConfig.ConnPoolConfig;
import uw.dao.conf.DaoConfig.TableShardConfig;
import uw.dao.connectionpool.ConnectionManager;
import uw.dao.sequence.FusionSequenceFactory;
import uw.dao.service.DaoService;

import java.util.Map;
import java.util.Map.Entry;

/**
 * spring启动配置文件.
 *
 * @author axeon
 */
@Configuration
@EnableConfigurationProperties({DaoConfig.class})
@AutoConfigureAfter({RedisAutoConfiguration.class})
public class DaoAutoConfiguration {

    /**
     * 日志.
     */
    private static final Logger log = LoggerFactory.getLogger( DaoAutoConfiguration.class );


    public DaoAutoConfiguration(DaoConfig daoConfig, final ClientResources clientResources) {
        init(daoConfig,clientResources);
    }

    /**
     * 配置初始化。
     * 初始化代码放入构造器，是考虑到初始化阶段可能有数据库操作。
     */
    public void init(DaoConfig daoConfig, final ClientResources clientResources) {

        log.info( "uw-dao start auto configuration..." );

        if (daoConfig == null) {
            log.warn( "uw-dao start failed, because the config missing!!! " );
            return;
        }

        //如果root配置没有，直接返回吧。
        ConnPoolConfig rootPoolConfig = daoConfig.getConnPool().getRoot();
        if (rootPoolConfig == null) {
            log.warn( "uw-dao not found root conn config!!!" );
            return;
        }

        Map<String, ConnPoolConfig> poolMap = daoConfig.getConnPool().getList();
        if (poolMap != null) {
            // 检查并填充DAOConfig默认值
            for (Entry<String, ConnPoolConfig> kv : poolMap.entrySet()) {
                ConnPoolConfig poolConfig = kv.getValue();
                if (poolConfig.getDriver() == null) {
                    poolConfig.setDriver( rootPoolConfig.getDriver() );
                }
                if (poolConfig.getTestSql() == null) {
                    poolConfig.setTestSql( rootPoolConfig.getTestSql() );
                }
                if (poolConfig.getMinConn() == 0) {
                    poolConfig.setMinConn( rootPoolConfig.getMinConn() );
                }
                if (poolConfig.getMaxConn() == 0) {
                    poolConfig.setMaxConn( rootPoolConfig.getMaxConn() );
                }
                if (poolConfig.getConnIdleTimeout() == 0) {
                    poolConfig.setConnIdleTimeout( rootPoolConfig.getConnIdleTimeout() );
                }
                if (poolConfig.getConnBusyTimeout() == 0) {
                    poolConfig.setConnBusyTimeout( rootPoolConfig.getConnBusyTimeout() );
                }
                if (poolConfig.getConnMaxAge() == 0) {
                    poolConfig.setConnMaxAge( rootPoolConfig.getConnMaxAge() );
                }
            }
        }
        // 给值
        DaoConfigManager.setConfig( daoConfig );
        // 启动连接池。
        ConnectionManager.start();
        if (daoConfig.getSqlStats().isEnable()) {
            // 加入统计日志表到sharding配置中。
            TableShardConfig config = new TableShardConfig();
            config.setShardType( "date" );
            config.setShardRule( "day" );
            config.setAutoGen( true );
            daoConfig.getTableShard().put( DaoService.STATS_BASE_TABLE, config );
        }
        DaoService.start();
        //处理Sequence问题。
        if (daoConfig.getRedis()!=null){
            log.info( "uw-dao FusionSequenceFactory init! " );
            RedisTemplate<String, Long> redisTemplate = new RedisTemplate<>();
            redisTemplate.setKeySerializer( new StringRedisSerializer() );
            redisTemplate.setValueSerializer(new GenericToStringSerializer(Long.class));
            redisTemplate.setConnectionFactory( redisConnectionFactory( daoConfig.getRedis(), clientResources ) );
            redisTemplate.setEnableDefaultSerializer( false );
            redisTemplate.afterPropertiesSet();
            new FusionSequenceFactory( redisTemplate );
        }
    }

    /**
     * Redis连接工厂
     *
     * @param redisProperties
     * @param clientResources
     * @return
     */
    private RedisConnectionFactory redisConnectionFactory(RedisProperties redisProperties, ClientResources clientResources) {
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
     * 关闭连接管理器,销毁全部连接池.
     */
    @PreDestroy
    public void destroy() {
        log.info( "uw-dao destroy configuration..." );
        DaoService.stop();
        ConnectionManager.stop();
    }

}
