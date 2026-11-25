package uw.task.rabbit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;

/**
 * 任务队列声明（两种协议共用）：正常声明；现有同名队列的形态与目标不一致时做有条件迁移。
 *
 * <p>RabbitMQ 不允许改队列形态，同名队列声明会报参数不一致（inequivalent arg x-queue-type），监听永远起不来。
 * 处理原则是"只在确认无数据风险时才动队列"：</p>
 * <ul>
 *   <li>仅当报错原因是队列形态不一致才处理，其他失败原样抛出；</li>
 *   <li>仅当队列已确认无积压且无消费者（无在途未确认消息）才删除并按新形态重建；</li>
 *   <li>队列非空时不删，打 ERROR 说明处理办法，等消息消费完后下一次声明自动完成迁移。</li>
 * </ul>
 * <p>不依赖 Leader 身份：启动初期 Leader 尚未选出，迁移不能因此卡住。多实例同时启动时各实例都会走这一判断，
 * 此时队列为空，先后重建的结果一致。</p>
 *
 * <p><b>已知取舍（用户裁决：两个方向都自动迁移）</b>：判空用的是待投递消息数，未到期的原生延迟消息不计入。
 * classic → quorum 方向没有延迟消息，是安全的；quorum → classic 方向上，队列里尚未到期的延迟消息会随旧队列一起删除。</p>
 *
 * @author axeon
 */
final class AmqpQueueDeclarer {

    /**
     * 日志器。
     */
    private static final Logger log = LoggerFactory.getLogger(AmqpQueueDeclarer.class);

    /**
     * 工具类，禁止实例化。
     */
    private AmqpQueueDeclarer() {
    }

    /**
     * 声明持久化队列及同名直连 exchange、绑定；形态不一致时按上述规则迁移。
     *
     * @param ops       协议操作面（提供判空与删除能力）
     * @param admin     该协议的 AmqpAdmin
     * @param queueName 队列名
     * @param quorum    是否 quorum 形态
     */
    static void declare(TaskAmqpOps ops, AmqpAdmin admin, String queueName, boolean quorum) {
        try {
            TaskAmqpOps.declareTopology(admin, queueName, quorum);
        } catch (Exception e) {
            if (!isQueueTypeMismatch(e)) {
                throw e;
            }
            String target = quorum ? "quorum" : "classic";
            if (!ops.isEmptyWithoutConsumers(queueName)) {
                log.error("任务队列形态与配置不一致，且队列非空或仍有消费者，无法自动迁移: queue={}, 目标形态={}——请先把该队列的消息消费完，之后会自动删除并重建（每分钟重试）", queueName, target);
                throw e;
            }
            ops.deleteQueue(queueName);
            TaskAmqpOps.declareTopology(admin, queueName, quorum);
            log.warn("任务队列形态已变更，原空队列已删除并按新形态重建: queue={}, 新形态={}", queueName, target);
        }
    }

    /**
     * 声明失败是否因为现有队列的形态与目标不一致。两种协议的报错文案不同：091 是
     * "PRECONDITION_FAILED - inequivalent arg 'x-queue-type'"，1.0 是"Unexpected response code: 409 … inequivalent arg 'x-queue-type'"，
     * 共同点是同时出现"inequivalent arg"与"x-queue-type"。
     *
     * @param e 声明异常
     * @return 是队列形态不一致返回 true
     */
    static boolean isQueueTypeMismatch(Exception e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String message = t.getMessage();
            if (message != null && message.contains("inequivalent arg") && message.contains("x-queue-type")) {
                return true;
            }
        }
        return false;
    }
}
