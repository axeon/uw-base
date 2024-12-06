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

    public TaskContact() {
        super();
    }


    public TaskContact(String contactName) {
        this.contactName = contactName;
    }

    /**
     * 构建联系人信息。
     *
     * @param contactName
     * @param mobile
     * @param email
     * @param wechat
     * @param im
     * @param notifyUrl
     * @param remark
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

    private TaskContact(Builder builder) {
        setTaskClass( builder.taskClass );
        setContactName( builder.contactName );
        setMobile( builder.mobile );
        setEmail( builder.email );
        setWechat( builder.wechat );
        setIm( builder.im );
        setNotifyUrl( builder.notifyUrl );
        setRemark( builder.remark );
    }

    /**
     * 默认的builder模式。
     *
     * @return
     */
    public static Builder builder() {
        return new Builder();
    }


    /**
     * builder模式，带联系人姓名。
     *
     * @param contactName
     * @return
     */
    public static Builder builder(String contactName) {
        return new Builder().contactName( contactName );
    }

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
     * @return the taskClass
     */
    public String getTaskClass() {
        return taskClass;
    }

    /**
     * @param taskClass the taskClass to set
     */
    public void setTaskClass(String taskClass) {
        this.taskClass = taskClass;
    }

    /**
     * @return the contactName
     */
    public String getContactName() {
        return contactName;
    }

    /**
     * @param contactName the contactName to set
     */
    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    /**
     * @return the mobile
     */
    public String getMobile() {
        return mobile;
    }

    /**
     * @param mobile the mobile to set
     */
    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    public String getWechat() {
        return wechat;
    }

    public void setWechat(String wechat) {
        this.wechat = wechat;
    }

    public String getIm() {
        return im;
    }

    public void setIm(String im) {
        this.im = im;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    /**
     * @return the remark
     */
    public String getRemark() {
        return remark;
    }

    /**
     * @param remark the remark to set
     */
    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * {@code TaskContact} builder static inner class.
     */
    public static final class Builder {
        private String taskClass;
        private String contactName;
        private String mobile;
        private String email;
        private String wechat;
        private String im;
        private String notifyUrl;
        private String remark;

        private Builder() {
        }

        /**
         * builder模式，带taskClass参数。
         *
         * @param taskClass
         * @return
         */
        public static Builder builder(String taskClass) {
            return new Builder().taskClass( taskClass );
        }

        /**
         * builder模式，带taskClass参数。
         *
         * @param taskClass
         * @return
         */
        public static Builder builder(Class taskClass) {
            return new Builder().taskClass( taskClass.getName() );
        }

        /**
         * Sets the {@code taskClass} and returns a reference to this Builder enabling method chaining.
         *
         * @param taskClass the {@code taskClass} to set
         * @return a reference to this Builder
         */
        public Builder taskClass(String taskClass) {
            this.taskClass = taskClass;
            return this;
        }

        /**
         * Sets the {@code contactName} and returns a reference to this Builder enabling method chaining.
         *
         * @param contactName the {@code contactName} to set
         * @return a reference to this Builder
         */
        public Builder contactName(String contactName) {
            this.contactName = contactName;
            return this;
        }

        /**
         * Sets the {@code mobile} and returns a reference to this Builder enabling method chaining.
         *
         * @param mobile the {@code mobile} to set
         * @return a reference to this Builder
         */
        public Builder mobile(String mobile) {
            this.mobile = mobile;
            return this;
        }

        /**
         * Sets the {@code email} and returns a reference to this Builder enabling method chaining.
         *
         * @param email the {@code email} to set
         * @return a reference to this Builder
         */
        public Builder email(String email) {
            this.email = email;
            return this;
        }

        /**
         * Sets the {@code wechat} and returns a reference to this Builder enabling method chaining.
         *
         * @param wechat the {@code wechat} to set
         * @return a reference to this Builder
         */
        public Builder wechat(String wechat) {
            this.wechat = wechat;
            return this;
        }

        /**
         * Sets the {@code im} and returns a reference to this Builder enabling method chaining.
         *
         * @param im the {@code im} to set
         * @return a reference to this Builder
         */
        public Builder im(String im) {
            this.im = im;
            return this;
        }

        /**
         * Sets the {@code notifyUrl} and returns a reference to this Builder enabling method chaining.
         *
         * @param notifyUrl the {@code notifyUrl} to set
         * @return a reference to this Builder
         */
        public Builder notifyUrl(String notifyUrl) {
            this.notifyUrl = notifyUrl;
            return this;
        }

        /**
         * Sets the {@code remark} and returns a reference to this Builder enabling method chaining.
         *
         * @param remark the {@code remark} to set
         * @return a reference to this Builder
         */
        public Builder remark(String remark) {
            this.remark = remark;
            return this;
        }

        /**
         * Returns a {@code TaskContact} built from the parameters previously set.
         *
         * @return a {@code TaskContact} built with parameters of this {@code TaskContact.Builder}
         */
        public TaskContact build() {
            return new TaskContact( this );
        }
    }
}
