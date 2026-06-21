package uw.notify.client.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * Web 通知消息体。
 * <p>
 * 描述一条通知的目标（用户/运营商）与内容（{@link NotifyBody}）。
 * {@code userId=0} 表示对该运营商下所有用户广播；{@code saasId=0} 表示对所有运营商广播。
 *
 * @author axeon
 */
@Schema(title = "通知消息", description = "通知消息")
public class WebNotifyMsg implements Serializable {

    /**
     * 用户 ID；{@code 0} 表示对该运营商下所有用户广播。
     */
    @Schema(title = "用户ID", description = "用户ID，0表示广播")
    private long userId;

    /**
     * 运营商编号；{@code 0} 表示对所有运营商广播。
     */
    @Schema(title = "运营商编号", description = "运营商编号，0表示所有运营商")
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
     * @param userId     用户 ID（0 表示广播）
     * @param saasId     运营商编号（0 表示所有运营商）
     * @param notifyBody 通知内容
     */
    public WebNotifyMsg(long userId, long saasId, NotifyBody notifyBody) {
        this.userId = userId;
        this.saasId = saasId;
        this.notifyBody = notifyBody;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getSaasId() {
        return saasId;
    }

    public void setSaasId(long saasId) {
        this.saasId = saasId;
    }

    public NotifyBody getNotifyBody() {
        return notifyBody;
    }

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

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }
    }


}
