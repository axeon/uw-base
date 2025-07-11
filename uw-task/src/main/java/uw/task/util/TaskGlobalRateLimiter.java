package uw.task.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Collections;

/**
 * 基于redis实现的限速管理器。
 *
 * @author axeon
 */
public class TaskGlobalRateLimiter {

    private static final Logger log = LoggerFactory.getLogger( TaskGlobalRateLimiter.class);

    private static final String REDIS_PREFIX = "uw-task-rate:";

    private final RedisTemplate<String, Long> redisTemplate;

    /**
     * LUA脚本。
     * 返回值为：可用请求数,等待毫秒数。
     */
    private static final RedisScript<Long> LUA_RATE_LIMIT = RedisScript.of(
            "local key = KEYS[1];\n" +
                    "local requests = tonumber(ARGV[1]);\n" +
                    "local millis = tonumber(ARGV[2])*1000;\n" +
                    "local permits = tonumber(ARGV[3]);\n" +
                    "local remainRequests=0;\n" +
                    "local waitMillis=0;\n" +
                    "local nowRate= redis.call('INCRBY', key,permits);\n" +
                    "if (nowRate==permits) then \n" +//如果是第一次执行，设置有效期保护。
                    "    redis.call('PEXPIRE',key,millis);\n" +
                    "end\n" +
                    "remainRequests = requests-nowRate;\n" +
                    "if (remainRequests<1) then \n" +//请求数超过限制
                    "    waitMillis = redis.call('PTTL',key);\n" +
                    "    if (waitMillis == -1) then \n" +
                    "        redis.call('PEXPIRE',key,millis);\n" +
                    "        waitMillis = millis;\n" +
                    "    end\n" +
                    "end \n" +
                    "return waitMillis", Long.class);


    public TaskGlobalRateLimiter(final RedisConnectionFactory redisConnectionFactory) {
        redisTemplate = new RedisTemplate<String, Long>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericToStringSerializer<Long>(Long.class));
        redisTemplate.setExposeConnection(true);
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.afterPropertiesSet();
    }

    /**
     * 尝试可否获取授权。
     *
     * @param permits 申请访问次数
     * @return 如果未超限则返回0，-1为不确定时间，其他为需要等待的毫秒数
     */
    public long tryAcquire(String name, int requests, int seconds, int permits) {
        if (requests == 0 || seconds == 0) {
            return 0;
        }
        Long waitLimit = redisTemplate.execute(LUA_RATE_LIMIT, Collections.singletonList(REDIS_PREFIX + name), requests, seconds, permits);
        if (waitLimit == null)
            return 0;
        else
            return waitLimit;
    }

}
