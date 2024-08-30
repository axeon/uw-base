package uw.notify.client.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * WebNotifyMsg。
 *
 * @author axeon
 */
@Schema(title = "通知消息", description = "通知消息")
public class WebNotifyMsg implements Serializable {

    /**
     * 用户ID
     */
    @Schema(title = "用户ID", description = "用户ID")
    private long userId;

    /**
     * 运营商编号
     */
    @Schema(title = "运营商编号", description = "运营商编号")
    private long saasId;

    /**
     * 通知内容。
     */
    @Schema(title = "通知内容", description = "通知内容")
    private NotifyBody notifyBody;

    public WebNotifyMsg() {
    }

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

    @Schema(title = "通知内容", description = "通知内容")
    public static class NotifyBody implements Serializable {

        /**
         * 通知类型
         */
        @Schema(title = "通知类型", description = "通知类型")
        private String type;

        /**
         * 消息标题
         */
        @Schema(title = "消息标题", description = "消息标题")
        private String subject;

        /**
         * 消息内容
         */
        @Schema(title = "消息内容", description = "消息内容")
        private String content;

        /**
         * 消息数据
         */
        @Schema(title = "消息数据", description = "消息数据")
        private Object data;

        public NotifyBody() {
        }

        public NotifyBody(String type, Object data) {
            this.type = type;
            this.data = data;
        }

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