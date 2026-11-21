package uw.task.conf;

import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.rabbitmq.autoconfigure.RabbitProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 任务配置类。
 *
 * @author axeon
 */
@ConfigurationProperties(prefix = "uw.task")
public class UwTaskProperties {

    /**
     * 应用名称
     */
    @Value("${spring.application.name:unknown}")
    private String appName;

    /**
     * 应用版本
     */
    @Value("${project.version:unknown}")
    private String appVersion;

    /**
     * app主机
     */
    @Value("${spring.cloud.nacos.discovery.ip:unknown}")
    private String appHost;

    /**
     * app端口
     */
    @Value("${server.port:0}")
    private int appPort;

    /**
     * 是否启用uw-task服务注册和主机注册，默认不启用。
     */
    private boolean enableRegistry = false;

    /**
     * 任务服务器
     */
    private String taskCenterHost = "http://uw-task-center";

    /**
     * 任务名，必须设置为基础包名，只扫描指定目录下任务。
     */
    private String taskProject;

    /**
     * 运行目标，重要参数！非运行目标不会执行。默认为“default”。
     */
    private String runTarget = "default";

    /**
     * runTaskAsync 在途执行并发上限（虚拟线程池背压闸门）：远程模式每个 future 等待 RPC 回复（最长 180s），
     * 超过此并发时提交线程按 CallerRuns 策略自行执行。虚拟线程化后此值为纯并发上限，与线程成本无关。
     */
    private int taskRpcThreadMaxNum = 60;

    /**
     * runQueue 本地队列任务执行并发上限（虚拟线程池背压闸门）：超过此并发先入有界缓冲队列，
     * 缓冲也满则溢出到 MQ 队列由远端消费（SendToQueuePolicy）。虚拟线程化后此值为纯并发上限。
     */
    private int taskLocalThreadMaxNum = 60;

    /**
     * 队列任务重试延时毫秒数，默认2秒
     */
    private long taskQueueRetryDelay = 2000;

    /**
     * rpc任务重试延时毫秒数，默认100毫秒
     */
    private long taskRpcRetryDelay = 100;

    /**
     * Rabbit MQ配置
     */
    private RabbitProperties rabbitmq = new RabbitProperties();

    /**
     * uw-task 私有 Redis（DelayTask zset/序列发号/Leader 选举/全局限速共用，与 uw-kv 隔离）。
     * 未配置的项回退 spring.data.redis 同名项。
     */
    private final DataRedisProperties redis = new DataRedisProperties();

    public String getAppName() {
        return appName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getAppHost() {
        return appHost;
    }

    public int getAppPort() {
        return appPort;
    }

    public boolean isEnableRegistry() {
        return enableRegistry;
    }

    public void setEnableRegistry(boolean enableRegistry) {
        this.enableRegistry = enableRegistry;
    }

    public String getTaskCenterHost() {
        return taskCenterHost;
    }

    public void setTaskCenterHost(String taskCenterHost) {
        this.taskCenterHost = taskCenterHost;
    }

    public String getTaskProject() {
        return taskProject;
    }

    public void setTaskProject(String taskProject) {
        this.taskProject = taskProject;
    }

    public String getRunTarget() {
        return runTarget;
    }

    public void setRunTarget(String runTarget) {
        this.runTarget = runTarget;
    }

    public int getTaskRpcThreadMaxNum() {
        return taskRpcThreadMaxNum;
    }

    public void setTaskRpcThreadMaxNum(int taskRpcThreadMaxNum) {
        this.taskRpcThreadMaxNum = taskRpcThreadMaxNum;
    }

    public int getTaskLocalThreadMaxNum() {
        return taskLocalThreadMaxNum;
    }

    public void setTaskLocalThreadMaxNum(int taskLocalThreadMaxNum) {
        this.taskLocalThreadMaxNum = taskLocalThreadMaxNum;
    }

    public long getTaskQueueRetryDelay() {
        return taskQueueRetryDelay;
    }

    public void setTaskQueueRetryDelay(long taskQueueRetryDelay) {
        this.taskQueueRetryDelay = taskQueueRetryDelay;
    }

    public long getTaskRpcRetryDelay() {
        return taskRpcRetryDelay;
    }

    public void setTaskRpcRetryDelay(long taskRpcRetryDelay) {
        this.taskRpcRetryDelay = taskRpcRetryDelay;
    }

    public RabbitProperties getRabbitmq() {
        return rabbitmq;
    }

    /**
     * uw-task 私有 Redis 配置（DelayTask zset/序列发号/Leader 选举/全局限速共用）。
     */
    public DataRedisProperties getRedis() {
        return redis;
    }

    public void setRabbitmq(RabbitProperties rabbitmq) {
        this.rabbitmq = rabbitmq;
    }

}
