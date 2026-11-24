package uw.task.entity;

import java.io.Serializable;

/**
 * taskAlertContact实体类。
 *
 * @author axeon
 * @version $Revision: 1.00 $ $Date: 2017-05-06 13:38:17
 */
public class TaskContact implements Serializable {

    /**
     * 执行类信息
     */
    private String taskClass;

    /**
     * 联系人
     */
    private String contactName;

    /**
     * 联系电话
     */
    private String mobile;

    /**
     * 联系email
     */
    private String email;

    /**
     * 联系微信
     */
    private String wechat;

    /**
     * 备用im
     */
    private String im;

    /**
     * 通知链接，如钉钉，微信等
     */
    private String notifyUrl;

    /**
     * 联系人备注
     */
    private String remark;

    /**
     * 默认构造器。
     */
    public TaskContact() {
        super();
    }

    /**
     * 带联系人姓名的构造器。
     *
     * @param contactName 联系人姓名
     */
    public TaskContact(String contactName) {
        this.contactName = contactName;
    }

    /**
     * 构建联系人信息。
     *
     * @param contactName 联系人
     * @param mobile 联系电话
     * @param email 联系email
     * @param wechat 联系微信
     * @param im 备用im
     * @param notifyUrl 通知链接，如钉钉，微信等
     * @param remark 联系人备注
     */
    public TaskContact(String contactName, String mobile, String email, String wechat, String im, String notifyUrl, String remark) {
        this.contactName = contactName;
        this.mobile = mobile;
        this.email = email;
        this.wechat = wechat;
        this.im = im;
        this.notifyUrl = notifyUrl;
        this.remark = remark;
    }

    /**
     * 由 Builder 构建实例。
     * @param builder 已设置好字段的 Builder
     */
    private TaskContact(Builder builder) {
        setTaskClass(builder.taskClass);
        setContactName(builder.contactName);
        setMobile(builder.mobile);
        setEmail(builder.email);
        setWechat(builder.wechat);
        setIm(builder.im);
        setNotifyUrl(builder.notifyUrl);
        setRemark(builder.remark);
    }

    /**
     * 默认的builder模式。
     *
     * @return 新的 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * builder模式，带联系人姓名。
     *
     * @param contactName 联系人
     * @return 新的 Builder
     */
    public static Builder builder(String contactName) {
        return new Builder().contactName(contactName);
    }

    /**
     * 基于已有联系人复制出一个 Builder（用于局部修改后重建）。
     *
     * @param copy 源联系人
     * @return 复制了源字段的 Builder
     */
    public static Builder builder(TaskContact copy) {
        Builder builder = new Builder();
        builder.taskClass = copy.getTaskClass();
        builder.contactName = copy.getContactName();
        builder.mobile = copy.getMobile();
        builder.email = copy.getEmail();
        builder.wechat = copy.getWechat();
        builder.im = copy.getIm();
        builder.notifyUrl = copy.getNotifyUrl();
        builder.remark = copy.getRemark();
        return builder;
    }

    /**
     * 获取执行类信息。
     *
     * @return 执行类信息
     */
    public String getTaskClass() {
        return taskClass;
    }

    /**
     * 设置执行类信息。
     *
     * @param taskClass 执行类信息
     */
    public void setTaskClass(String taskClass) {
        this.taskClass = taskClass;
    }

    /**
     * 获取联系人。
     *
     * @return 联系人
     */
    public String getContactName() {
        return contactName;
    }

    /**
     * 设置联系人。
     *
     * @param contactName 联系人
     */
    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    /**
     * 获取联系电话。
     *
     * @return 联系电话
     */
    public String getMobile() {
        return mobile;
    }

    /**
     * 设置联系电话。
     *
     * @param mobile 联系电话
     */
    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    /**
     * 获取联系email。
     *
     * @return 联系email
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置联系email。
     *
     * @param email 联系email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 获取联系微信。
     *
     * @return 联系微信
     */
    public String getWechat() {
        return wechat;
    }

    /**
     * 设置联系微信。
     *
     * @param wechat 联系微信
     */
    public void setWechat(String wechat) {
        this.wechat = wechat;
    }

    /**
     * 获取备用im。
     *
     * @return 备用im
     */
    public String getIm() {
        return im;
    }

    /**
     * 设置备用im。
     *
     * @param im 备用im
     */
    public void setIm(String im) {
        this.im = im;
    }

    /**
     * 获取通知链接，如钉钉，微信等。
     *
     * @return 通知链接，如钉钉，微信等
     */
    public String getNotifyUrl() {
        return notifyUrl;
    }

    /**
     * 设置通知链接，如钉钉，微信等。
     *
     * @param notifyUrl 通知链接，如钉钉，微信等
     */
    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    /**
     * 获取联系人备注。
     *
     * @return 联系人备注
     */
    public String getRemark() {
        return remark;
    }

    /**
     * 设置联系人备注。
     *
     * @param remark 联系人备注
     */
    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * {@code TaskContact} builder static inner class.
     */
    public static final class Builder {
        /**
         * 执行类信息。
         */
        private String taskClass;
        /**
         * 联系人。
         */
        private String contactName;
        /**
         * 联系电话。
         */
        private String mobile;
        /**
         * 联系email。
         */
        private String email;
        /**
         * 联系微信。
         */
        private String wechat;
        /**
         * 备用im。
         */
        private String im;
        /**
         * 通知链接，如钉钉，微信等。
         */
        private String notifyUrl;
        /**
         * 联系人备注。
         */
        private String remark;

        /**
         * Builder 构造器（仅经 builder(...) 工厂方法创建）。
         */
        private Builder() {
        }

        /**
         * builder模式，带taskClass参数。
         *
         * @param taskClass 任务类全限定名
         * @return 新的 Builder
         */
        public static Builder builder(String taskClass) {
            return new Builder().taskClass(taskClass);
        }

        /**
         * builder模式，带taskClass参数。
         *
         * @param taskClass 任务类全限定名
         * @return 新的 Builder
         */
        public static Builder builder(Class taskClass) {
            return new Builder().taskClass(taskClass.getName());
        }

        /**
         * 设置执行类信息。
         *
         * @param taskClass 执行类信息
         * @return this（链式调用）
         */
        public Builder taskClass(String taskClass) {
            this.taskClass = taskClass;
            return this;
        }

        /**
         * 设置联系人。
         *
         * @param contactName 联系人
         * @return this（链式调用）
         */
        public Builder contactName(String contactName) {
            this.contactName = contactName;
            return this;
        }

        /**
         * 设置联系电话。
         *
         * @param mobile 联系电话
         * @return this（链式调用）
         */
        public Builder mobile(String mobile) {
            this.mobile = mobile;
            return this;
        }

        /**
         * 设置联系email。
         *
         * @param email 联系email
         * @return this（链式调用）
         */
        public Builder email(String email) {
            this.email = email;
            return this;
        }

        /**
         * 设置联系微信。
         *
         * @param wechat 联系微信
         * @return this（链式调用）
         */
        public Builder wechat(String wechat) {
            this.wechat = wechat;
            return this;
        }

        /**
         * 设置备用im。
         *
         * @param im 备用im
         * @return this（链式调用）
         */
        public Builder im(String im) {
            this.im = im;
            return this;
        }

        /**
         * 设置通知链接，如钉钉，微信等。
         *
         * @param notifyUrl 通知链接，如钉钉，微信等
         * @return this（链式调用）
         */
        public Builder notifyUrl(String notifyUrl) {
            this.notifyUrl = notifyUrl;
            return this;
        }

        /**
         * 设置联系人备注。
         *
         * @param remark 联系人备注
         * @return this（链式调用）
         */
        public Builder remark(String remark) {
            this.remark = remark;
            return this;
        }

        /**
         * 按已设置的参数构建实例。
         *
         * @return 构建出的对象
         */
        public TaskContact build() {
            return new TaskContact(this);
        }
    }
}
