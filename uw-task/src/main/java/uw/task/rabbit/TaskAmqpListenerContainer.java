package uw.task.rabbit;

import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;

/**
 * 协议无关的队列监听容器句柄：包装 091 的 SimpleMessageListenerContainer 或 1.0 的 Rabbit100Ops.ElasticConsumerGroup，
 * 向 TaskRunnerContainer 暴露统一的生命周期与并发调整面。
 *
 * @author axeon
 */
public final class TaskAmqpListenerContainer {

    /**
     * 091 协议容器（二选一，另一个为 null）。
     */
    private final SimpleMessageListenerContainer container091;

    /**
     * 100 协议弹性消费者组（Rabbit100Ops 自管消费者通道，支持 pause/unpause 动态伸缩）。
     */
    private final Rabbit100Ops.ElasticConsumerGroup elasticGroup;

    /**
     * 包装 091 监听容器。
     *
     * @param c spring-rabbit 的监听容器
     */
    private TaskAmqpListenerContainer(SimpleMessageListenerContainer c) {
        this.container091 = c;
        this.elasticGroup = null;
    }

    /**
     * 包装 1.0 弹性消费者组。
     *
     * @param elasticGroup 弹性消费者组
     */
    private TaskAmqpListenerContainer(Rabbit100Ops.ElasticConsumerGroup elasticGroup) {
        this.container091 = null;
        this.elasticGroup = elasticGroup;
    }

    /**
     * 包装 091 协议监听容器。
     *
     * @param container spring-rabbit 的 SimpleMessageListenerContainer
     * @return 协议无关句柄
     */
    public static TaskAmqpListenerContainer of(SimpleMessageListenerContainer container) {
        return new TaskAmqpListenerContainer(container);
    }

    /**
     * 包装 100 协议弹性消费者组（Rabbit100Ops.ElasticConsumerGroup）。
     *
     * @param elasticGroup 弹性消费者组
     * @return 协议无关句柄
     */
    public static TaskAmqpListenerContainer of(Rabbit100Ops.ElasticConsumerGroup elasticGroup) {
        return new TaskAmqpListenerContainer(elasticGroup);
    }

    /**
     * 在线调整消费并发与预取。
     * <p>091：set 系列即时生效；1.0：经 ElasticConsumerGroup.resize（pause/unpause+按需新建）即时生效，
     * prefetch 不可热改（credit 侧已发放，入参被忽略）。</p>
     *
     * @param consumerNum 消费并发
     * @param prefetchNum 预取数
     */
    public void tune(int consumerNum, int prefetchNum) {
        if (container091 != null) {
            container091.setMaxConcurrentConsumers(consumerNum);
            container091.setConcurrentConsumers(Rabbit091Ops.minConsumers(consumerNum));
            container091.setPrefetchCount(prefetchNum);
        } else {
            // 1.0 弹性组：resize 即时生效（pause/unpause + 按需新建），prefetch 不支持热改（credit 侧已发放）
            elasticGroup.resize(consumerNum);
        }
    }

    /**
     * 销毁容器（091 走 destroy() 按 Spring 生命周期顺序关闭；1.0 关闭全部消费者，在途任务跑完后自然结束）。
     */
    public void destroy() {
        if (container091 != null) {
            container091.destroy();
        } else {
            elasticGroup.shutdown();
        }
    }
}
