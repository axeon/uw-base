package uw.task.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import uw.task.conf.TaskProperties;

import java.util.concurrent.TimeUnit;

/**
 * 使用Reids的setnx+expire来选举Leader，来在多个运行实例中选举出一个运行全局任务的实例。
 *
 * @author axeon
 */
public class TaskGlobalLocker {

    private static final Logger log = LoggerFactory.getLogger(TaskGlobalLocker.class);

    /**
     * REDIS前缀
     */
    private static final String REDIS_TAG = "uw-task-locker:";

    /**
     * 默认锁定时间。
     */
    private static final long LOCK_MILLIS = 90_000L;

    /**
     * redis模板
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 锁名。
     */
    private final String lockerName;

    /**
     * task配置
     */
    private final String lockerData;

    /**
     * 是否拿到锁。
     */
    private volatile boolean flag;

    public TaskGlobalLocker(final RedisConnectionFactory redisConnectionFactory, TaskProperties taskProperties) {
        this.stringRedisTemplate = new StringRedisTemplate(redisConnectionFactory);
        this.stringRedisTemplate.afterPropertiesSet();
        this.lockerName = REDIS_TAG + taskProperties.getTaskProject();
        this.lockerData = taskProperties.getAppName() + ":" + taskProperties.getAppVersion() + "@" + taskProperties.getAppHost() + ":" + taskProperties.getAppPort();
    }

    /**
     * 是否得到锁。
     *
     * @return
     */
    public boolean isLeader() {
        return flag;
    }

    /**
     * 返回当前是否是Leader.
     * 使用setIfAbsent原子操作避免GET+SET的TOCTOU竞态条件。
     *
     * @return the isLeader
     */
    public boolean checkLeader() {
        // 先尝试 setIfAbsent，如果key不存在则原子性地获取锁
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(lockerName, lockerData, LOCK_MILLIS, TimeUnit.MILLISECONDS);
        if (Boolean.TRUE.equals(acquired)) {
            flag = true;
            return true;
        }
        // key已存在，检查是否是自己持有的
        String data = stringRedisTemplate.opsForValue().get(lockerName);
        if (data != null && data.equals(lockerData)) {
            flag = stringRedisTemplate.expire(lockerName, LOCK_MILLIS, TimeUnit.MILLISECONDS);
        } else {
            flag = false;
        }
        return flag;
    }

}
