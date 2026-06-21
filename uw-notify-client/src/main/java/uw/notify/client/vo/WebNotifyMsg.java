package uw.notify.client.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * Web 通知消息体。
 * <p>
 * 描述一条通知的目标用户与内容（{@link NotifyBody}）。
 * <p>
 * <b>当前实现约束（重要）：</b>
 * <ul>
 *     <li>{@code userId} 必须为全局唯一且 &gt; 0 的有效用户 ID，notify-center 仅支持向单个在线用户定向投递；</li>
 *     <li>不支持按 {@code userId} 或 {@code saasId} 维度的广播，传入 {@code userId<=0} 会被 notify-center 直接拒绝；</li>
 *     <li>{@code saasId} 当前仅作为消息溯源字段参与记录，不参与投递路由
 *     （SSE 连接池以全局唯一的 {@code userId} 为 key，故不会跨运营商串号）。</li>
 * </ul>
 *
 * @author axeon
 */
@Schema(title = "通知消息", description = "通知消息")
public class WebNotifyMsg implements Serializable {

    /**
     * 目标用户 ID（全局唯一，必须 &gt; 0）。
     * <p>
     * notify-center 仅支持向单个在线用户定向投递；{@code userId<=0} 会被拒绝。
     */
    @Schema(title = "用户ID", description = "目标用户ID，必须大于0")
    private long userId;

    /**
     * 运营商编号。
     * <p>
     * 当前仅作为消息溯源字段记录，不参与投递路由（投递仅依据全局唯一的 {@code userId}）。
     */
    @Schema(title = "运营商编号", description = "运营商编号，仅作溯源，不参与投递")
    private long saasId;

    /**
     * 通知内容。
     */
    @Schema(title = "通知内容", description = "通知内容")
    private NotifyBody notifyBody;

    /**
     * 默认构造方法（用于反序列化）。
     */
    public WebNotifyMsg() {
    }

    /**
     * 通过用户、运营商与通知内容构造消息体。
     *
     * @param userId     目标用户 ID（必须 &gt; 0）
     * @param saasId     运营商编号（仅作溯源）
     * @param notifyBody 通知内容
     */
    public WebNotifyMsg(long userId, long saasId, NotifyBody notifyBody) {
        this.userId = userId;
        this.saasId = saasId;
        this.notifyBody = notifyBody;
    }

    /**
     * @return 目标用户 ID（必须 &gt; 0）
     */
    public long getUserId() {
        return userId;
    }

    /**
     * @param userId 目标用户 ID（必须 &gt; 0）
     */
    public void setUserId(long userId) {
        this.userId = userId;
    }

    /**
     * @return 运营商编号（仅作溯源）
     */
    public long getSaasId() {
        return saasId;
    }

    /**
     * @param saasId 运营商编号（仅作溯源）
     */
    public void setSaasId(long saasId) {
        this.saasId = saasId;
    }

    /**
     * @return 通知内容
     */
    public NotifyBody getNotifyBody() {
        return notifyBody;
    }

    /**
     * @param notifyBody 通知内容
     */
    public void setNotifyBody(NotifyBody notifyBody) {
        this.notifyBody = notifyBody;
    }

    /**
     * 通知内容，包含类型、标题、正文与附加数据。
     */
    @Schema(title = "通知内容", description = "通知内容")
    public static class NotifyBody implements Serializable {

        /**
         * 通知类型（业务自定义，如 {@code "SYSTEM"}、{@code "ORDER"} 等）。
         */
        @Schema(title = "通知类型", description = "通知类型")
        private String type;

        /**
         * 消息标题。
         */
        @Schema(title = "消息标题", description = "消息标题")
        private String subject;

        /**
         * 消息内容（正文）。
         */
        @Schema(title = "消息内容", description = "消息内容")
        private String content;

        /**
         * 消息附加数据，类型任意，由前端按 {@code type} 约定解析。
         */
        @Schema(title = "消息数据", description = "消息数据")
        private Object data;

        /**
         * 默认构造方法（用于反序列化）。
         */
        public NotifyBody() {
        }

        /**
         * 通过通知类型与附加数据构造消息体（不含标题与正文）。
         *
         * @param type 通知类型
         * @param data 附加数据
         */
        public NotifyBody(String type, Object data) {
            this.type = type;
            this.data = data;
        }

        /**
         * 通过通知类型、标题、正文与附加数据构造完整消息体。
         *
         * @param type    通知类型
         * @param subject 消息标题
         * @param content 消息正文
         * @param data    附加数据
         */
        public NotifyBody(String type, String subject, String content, Object data) {
            this.type = type;
            this.subject = subject;
            this.content = content;
            this.data = data;
        }

        /**
         * @return 通知类型
         */
        public String getType() {
            return type;
        }

        /**
         * @param type 通知类型
         */
        public void setType(String type) {
            this.type = type;
        }

        /**
         * @return 消息标题
         */
        public String getSubject() {
            return subject;
        }

        /**
         * @param subject 消息标题
         */
        public void setSubject(String subject) {
            this.subject = subject;
        }

        /**
         * @return 消息正文
         */
        public String getContent() {
            return content;
        }

        /**
         * @param content 消息正文
         */
        public void setContent(String content) {
            this.content = content;
        }

        /**
         * @return 消息附加数据
         */
        public Object getData() {
            return data;
        }

        /**
         * @param data 消息附加数据
         */
        public void setData(Object data) {
            this.data = data;
        }
    }


}
