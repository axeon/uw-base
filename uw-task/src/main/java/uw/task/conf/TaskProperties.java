package uw.task.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 任务配置类。
 *
 * @author axeon
 */
@ConfigurationProperties(prefix = "uw.task")
public class TaskProperties {

    /**
     * 应用名称
     */
    @Value("${spring.application.name}")
    private String appName;

    /**
     * 应用版本
     */
    @Value("${project.version}")
    private String appVersion;

    /**
     * app主机
     */
    @Value("${spring.cloud.nacos.discovery.ip}")
    private String appHost;

    /**
     * app端口
     */
    @Value("${server.port}")
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
     * croner线程数，默认在5个，建议按照实际croner任务数量*70%。
     */
    private int cronerThreadNum = 5;

    /**
     * RPC最小线程数,用于执行RPC调用，如不使用rpc，建议设置为1，否则按照最大并发量*10%设置。
     */
    private int taskRpcThreadMinNum = 1;

    /**
     * RPC最大线程数,用于执行RPC调用，超过此线程数，将会导致阻塞。
     */
    private int taskRpcThreadMaxNum = 60;

    /**
     * 本地队列任务运行最小线程数，用于运行本地队列任务，减少资源消耗。
     */
    private int taskLocalThreadMinNum = 1;

    /**
     * 本地队列任务运行最大线程数，用于运行本地队列任务，减少资源消耗。
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
     * 项目队列最小线程数量
     */
    private int queueProjectDefaultThreadMinNum = 1;

    /**
     * 全局队列最大线程数量
     */
    private int queueProjectDefaultThreadMaxNum = 60;

    /**
     * 全局队列预取数量
     */
    private int queueProjectDefaultPrefetchNum = 1;

    /**
     * 全局队列最小线程数量
     */
    private int queueProjectPriorityThreadMinNum = 1;

    /**
     * 全局队列最大线程数量
     */
    private int queueProjectPriorityThreadMaxNum = 60;

    /**
     * 全局队列预取数量
     */
    private int queueProjectPriorityPrefetchNum = 1;

    /**
     * 组队列最小线程数量
     */
    private int queueGroupDefaultThreadMinNum = 1;

    /**
     * 组队列最大线程数量
     */
    private int queueGroupDefaultThreadMaxNum = 20;

    /**
     * 组队列预取数量
     */
    private int queueGroupDefaultPrefetchNum = 1;

    /**
     * 组队列最小线程数量
     */
    private int queueGroupPriorityThreadMinNum = 1;

    /**
     * 组队列最大线程数量
     */
    private int queueGroupPriorityThreadMaxNum = 20;

    /**
     * 组队列预取数量
     */
    private int queueGroupPriorityPrefetchNum = 1;

    /**
     * Redis配置
     */
    private RedisProperties redis = new RedisProperties();

    /**
     * Rabbit MQ配置
     */
    private RabbitProperties rabbitmq = new RabbitProperties();

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

    public int getCronerThreadNum() {
        return cronerThreadNum;
    }

    public void setCronerThreadNum(int cronerThreadNum) {
        this.cronerThreadNum = cronerThreadNum;
    }

    public int getTaskRpcThreadMinNum() {
        return taskRpcThreadMinNum;
    }

    public void setTaskRpcThreadMinNum(int taskRpcThreadMinNum) {
        this.taskRpcThreadMinNum = taskRpcThreadMinNum;
    }

    public int getTaskRpcThreadMaxNum() {
        return taskRpcThreadMaxNum;
    }

    public void setTaskRpcThreadMaxNum(int taskRpcThreadMaxNum) {
        this.taskRpcThreadMaxNum = taskRpcThreadMaxNum;
    }

    public int getTaskLocalThreadMinNum() {
        return taskLocalThreadMinNum;
    }

    public void setTaskLocalThreadMinNum(int taskLocalThreadMinNum) {
        this.taskLocalThreadMinNum = taskLocalThreadMinNum;
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

    public int getQueueProjectDefaultThreadMinNum() {
        return queueProjectDefaultThreadMinNum;
    }

    public void setQueueProjectDefaultThreadMinNum(int queueProjectDefaultThreadMinNum) {
        this.queueProjectDefaultThreadMinNum = queueProjectDefaultThreadMinNum;
    }

    public int getQueueProjectDefaultThreadMaxNum() {
        return queueProjectDefaultThreadMaxNum;
    }

    public void setQueueProjectDefaultThreadMaxNum(int queueProjectDefaultThreadMaxNum) {
        this.queueProjectDefaultThreadMaxNum = queueProjectDefaultThreadMaxNum;
    }

    public int getQueueProjectDefaultPrefetchNum() {
        return queueProjectDefaultPrefetchNum;
    }

    public void setQueueProjectDefaultPrefetchNum(int queueProjectDefaultPrefetchNum) {
        this.queueProjectDefaultPrefetchNum = queueProjectDefaultPrefetchNum;
    }

    public int getQueueProjectPriorityThreadMinNum() {
        return queueProjectPriorityThreadMinNum;
    }

    public void setQueueProjectPriorityThreadMinNum(int queueProjectPriorityThreadMinNum) {
        this.queueProjectPriorityThreadMinNum = queueProjectPriorityThreadMinNum;
    }

    public int getQueueProjectPriorityThreadMaxNum() {
        return queueProjectPriorityThreadMaxNum;
    }

    public void setQueueProjectPriorityThreadMaxNum(int queueProjectPriorityThreadMaxNum) {
        this.queueProjectPriorityThreadMaxNum = queueProjectPriorityThreadMaxNum;
    }

    public int getQueueProjectPriorityPrefetchNum() {
        return queueProjectPriorityPrefetchNum;
    }

    public void setQueueProjectPriorityPrefetchNum(int queueProjectPriorityPrefetchNum) {
        this.queueProjectPriorityPrefetchNum = queueProjectPriorityPrefetchNum;
    }

    public int getQueueGroupDefaultThreadMinNum() {
        return queueGroupDefaultThreadMinNum;
    }

    public void setQueueGroupDefaultThreadMinNum(int queueGroupDefaultThreadMinNum) {
        this.queueGroupDefaultThreadMinNum = queueGroupDefaultThreadMinNum;
    }

    public int getQueueGroupDefaultThreadMaxNum() {
        return queueGroupDefaultThreadMaxNum;
    }

    public void setQueueGroupDefaultThreadMaxNum(int queueGroupDefaultThreadMaxNum) {
        this.queueGroupDefaultThreadMaxNum = queueGroupDefaultThreadMaxNum;
    }

    public int getQueueGroupDefaultPrefetchNum() {
        return queueGroupDefaultPrefetchNum;
    }

    public void setQueueGroupDefaultPrefetchNum(int queueGroupDefaultPrefetchNum) {
        this.queueGroupDefaultPrefetchNum = queueGroupDefaultPrefetchNum;
    }

    public int getQueueGroupPriorityThreadMinNum() {
        return queueGroupPriorityThreadMinNum;
    }

    public void setQueueGroupPriorityThreadMinNum(int queueGroupPriorityThreadMinNum) {
        this.queueGroupPriorityThreadMinNum = queueGroupPriorityThreadMinNum;
    }

    public int getQueueGroupPriorityThreadMaxNum() {
        return queueGroupPriorityThreadMaxNum;
    }

    public void setQueueGroupPriorityThreadMaxNum(int queueGroupPriorityThreadMaxNum) {
        this.queueGroupPriorityThreadMaxNum = queueGroupPriorityThreadMaxNum;
    }

    public int getQueueGroupPriorityPrefetchNum() {
        return queueGroupPriorityPrefetchNum;
    }

    public void setQueueGroupPriorityPrefetchNum(int queueGroupPriorityPrefetchNum) {
        this.queueGroupPriorityPrefetchNum = queueGroupPriorityPrefetchNum;
    }

    public RedisProperties getRedis() {
        return redis;
    }

    public void setRedis(RedisProperties redis) {
        this.redis = redis;
    }

    public RabbitProperties getRabbitmq() {
        return rabbitmq;
    }

    public void setRabbitmq(RabbitProperties rabbitmq) {
        this.rabbitmq = rabbitmq;
    }

    public static class RedisProperties extends org.springframework.boot.autoconfigure.data.redis.RedisProperties {
    }

    public static class RabbitProperties extends org.springframework.boot.autoconfigure.amqp.RabbitProperties {
    }
}
