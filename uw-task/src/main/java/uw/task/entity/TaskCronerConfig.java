package uw.task.entity;

import java.io.Serializable;

/**
 * taskCronerConfig实体类。
 *
 * @author axeon
 * @version $Revision: 1.00 $ $Date: 2017-05-03 16:51:06
 */
public class TaskCronerConfig implements Serializable {

    /**
     * 直接运行模式。
     */
    public static final int RUN_TYPE_ANYWAY = 0;
    /**
     * 运行在全局单例模式下。
     */
    public static final int RUN_TYPE_SINGLETON = 1;
    /**
     * 什么都不记录
     */
    public static final int TASK_LOG_TYPE_NONE = -1;
    /**
     * 记录日志
     */
    public static final int TASK_LOG_TYPE_RECORD = 0;
    /**
     * 记录日志,含请求参数
     */
    public static final int TASK_LOG_TYPE_RECORD_TASK_PARAM = 1;
    /**
     * 记录日志,含返回参数
     */
    public static final int TASK_LOG_TYPE_RECORD_RESULT_DATA = 2;
    /**
     * 记录全部日志
     */
    public static final int TASK_LOG_TYPE_RECORD_ALL = 3;

    /**
     * id。
     */
    private long id;

    /**
     * 执行类信息
     */
    private String taskClass;

    /**
     * 执行参数，可能用于区分子任务
     */
    private String taskParam;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务描述
     */
    private String taskDesc;

    /**
     * cron表达式，默认5秒一次。
     */
    private String taskCron = "*/5 * * * * ?";

    /**
     * 0随意执行，1全局唯一执行
     */
    private int runType = RUN_TYPE_SINGLETON;

    /**
     * 运行目标，默认不指定
     */
    private String runTarget = "default";

    /**
     * 失败率
     */
    private int alertFailRate;

    /**
     * 接口失败率
     */
    private int alertFailPartnerRate;

    /**
     * 程序失败率
     */
    private int alertFailProgramRate;

    /**
     * 数据失败率
     */
    private int alertFailDataRate;

    /**
     * 等待超时
     */
    private int alertWaitTimeout;

    /**
     * 运行超时
     */
    private int alertRunTimeout;

    /**
     * 状态值
     */
    private int state = 1;

    /**
     * 详见 TaskLogObjectAsStringSerializer 日志类型说明
     */
    private int logLevel = TASK_LOG_TYPE_RECORD;

    /**
     * 日志字符串字段大小限制: 0 表示无限制
     */
    private int logLimitSize = 0;

    public TaskCronerConfig() {
    }

    public TaskCronerConfig(String taskName) {
        this.taskName = taskName;
    }

    public TaskCronerConfig(String taskName, String taskCron) {
        this.taskName = taskName;
        this.taskCron = taskCron;
    }

    private TaskCronerConfig(Builder builder) {
        setId( builder.id );
        setTaskClass( builder.taskClass );
        setTaskParam( builder.taskParam );
        setTaskName( builder.taskName );
        setTaskDesc( builder.taskDesc );
        setTaskCron( builder.taskCron );
        setRunType( builder.runType );
        setRunTarget( builder.runTarget );
        setAlertFailRate( builder.alertFailRate );
        setAlertFailPartnerRate( builder.alertFailPartnerRate );
        setAlertFailProgramRate( builder.alertFailProgramRate );
        setAlertFailDataRate( builder.alertFailDataRate );
        setAlertWaitTimeout( builder.alertWaitTimeout );
        setAlertRunTimeout( builder.alertRunTimeout );
        setState( builder.state );
        setLogLevel( builder.logLevel );
        setLogLimitSize( builder.logLimitSize );
    }


    /**
     * builder模式，带taskName参数。
     *
     * @param taskName
     * @return
     */
    public static Builder builder(String taskName) {
        return new Builder().taskName( taskName );
    }

    /**
     * builder模式，带taskName和taskCron参数。
     *
     * @param taskName
     * @return
     */
    public static Builder builder(String taskName, String taskCron) {
        return new Builder().taskName( taskName ).taskCron( taskCron );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(TaskCronerConfig copy) {
        Builder builder = new Builder();
        builder.id = copy.getId();
        builder.taskClass = copy.getTaskClass();
        builder.taskParam = copy.getTaskParam();
        builder.taskName = copy.getTaskName();
        builder.taskDesc = copy.getTaskDesc();
        builder.taskCron = copy.getTaskCron();
        builder.runType = copy.getRunType();
        builder.runTarget = copy.getRunTarget();
        builder.alertFailRate = copy.getAlertFailRate();
        builder.alertFailPartnerRate = copy.getAlertFailPartnerRate();
        builder.alertFailProgramRate = copy.getAlertFailProgramRate();
        builder.alertFailDataRate = copy.getAlertFailDataRate();
        builder.alertWaitTimeout = copy.getAlertWaitTimeout();
        builder.alertRunTimeout = copy.getAlertRunTimeout();
        builder.state = copy.getState();
        builder.logLevel = copy.getLogLevel();
        builder.logLimitSize = copy.getLogLimitSize();
        return builder;
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTaskClass() {
        return taskClass;
    }

    public void setTaskClass(String taskClass) {
        this.taskClass = taskClass;
    }

    public String getTaskParam() {
        return taskParam;
    }

    public void setTaskParam(String taskParam) {
        this.taskParam = taskParam;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskDesc() {
        return taskDesc;
    }

    public void setTaskDesc(String taskDesc) {
        this.taskDesc = taskDesc;
    }

    public String getTaskCron() {
        return taskCron;
    }

    public void setTaskCron(String taskCron) {
        this.taskCron = taskCron;
    }

    public int getRunType() {
        return runType;
    }

    public void setRunType(int runType) {
        this.runType = runType;
    }

    public String getRunTarget() {
        return runTarget;
    }

    public void setRunTarget(String runTarget) {
        this.runTarget = runTarget;
    }

    public int getAlertFailRate() {
        return alertFailRate;
    }

    public void setAlertFailRate(int alertFailRate) {
        this.alertFailRate = alertFailRate;
    }

    public int getAlertFailPartnerRate() {
        return alertFailPartnerRate;
    }

    public void setAlertFailPartnerRate(int alertFailPartnerRate) {
        this.alertFailPartnerRate = alertFailPartnerRate;
    }

    public int getAlertFailProgramRate() {
        return alertFailProgramRate;
    }

    public void setAlertFailProgramRate(int alertFailProgramRate) {
        this.alertFailProgramRate = alertFailProgramRate;
    }

    public int getAlertFailDataRate() {
        return alertFailDataRate;
    }

    public void setAlertFailDataRate(int alertFailDataRate) {
        this.alertFailDataRate = alertFailDataRate;
    }

    public int getAlertWaitTimeout() {
        return alertWaitTimeout;
    }

    public void setAlertWaitTimeout(int alertWaitTimeout) {
        this.alertWaitTimeout = alertWaitTimeout;
    }

    public int getAlertRunTimeout() {
        return alertRunTimeout;
    }

    public void setAlertRunTimeout(int alertRunTimeout) {
        this.alertRunTimeout = alertRunTimeout;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    public int getLogLimitSize() {
        return logLimitSize;
    }

    public void setLogLimitSize(int logLimitSize) {
        this.logLimitSize = logLimitSize;
    }

    /**
     * {@code TaskCronerConfig} builder static inner class.
     */
    public static final class Builder {

        private long id;

        /**
         * 执行类信息
         */
        private String taskClass;

        /**
         * 执行参数，可能用于区分子任务
         */
        private String taskParam;

        /**
         * 任务名称
         */
        private String taskName;

        /**
         * 任务描述
         */
        private String taskDesc;

        /**
         * cron表达式，默认5秒一次。
         */
        private String taskCron = "*/5 * * * * ?";

        /**
         * 0随意执行，1全局唯一执行
         */
        private int runType = RUN_TYPE_SINGLETON;

        /**
         * 运行目标，默认不指定
         */
        private String runTarget = "default";

        /**
         * 失败率
         */
        private int alertFailRate;

        /**
         * 接口失败率
         */
        private int alertFailPartnerRate;

        /**
         * 程序失败率
         */
        private int alertFailProgramRate;

        /**
         * 数据失败率
         */
        private int alertFailDataRate;

        /**
         * 等待超时
         */
        private int alertWaitTimeout;

        /**
         * 运行超时
         */
        private int alertRunTimeout;

        /**
         * 状态值
         */
        private int state = 1;

        /**
         * 详见 TaskLogObjectAsStringSerializer 日志类型说明
         */
        private int logLevel = TASK_LOG_TYPE_RECORD;

        /**
         * 日志字符串字段大小限制: 0 表示无限制
         */
        private int logLimitSize = 0;

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
         * Sets the {@code taskParam} and returns a reference to this Builder enabling method chaining.
         *
         * @param taskParam the {@code taskParam} to set
         * @return a reference to this Builder
         */
        public Builder taskParam(String taskParam) {
            this.taskParam = taskParam;
            return this;
        }

        /**
         * Sets the {@code taskName} and returns a reference to this Builder enabling method chaining.
         *
         * @param taskName the {@code taskName} to set
         * @return a reference to this Builder
         */
        public Builder taskName(String taskName) {
            this.taskName = taskName;
            return this;
        }

        /**
         * Sets the {@code taskDesc} and returns a reference to this Builder enabling method chaining.
         *
         * @param taskDesc the {@code taskDesc} to set
         * @return a reference to this Builder
         */
        public Builder taskDesc(String taskDesc) {
            this.taskDesc = taskDesc;
            return this;
        }

        /**
         * Sets the {@code taskCron} and returns a reference to this Builder enabling method chaining.
         *
         * @param taskCron the {@code taskCron} to set
         * @return a reference to this Builder
         */
        public Builder taskCron(String taskCron) {
            this.taskCron = taskCron;
            return this;
        }

        /**
         * Sets the {@code runType} and returns a reference to this Builder enabling method chaining.
         *
         * @param runType the {@code runType} to set
         * @return a reference to this Builder
         */
        public Builder runType(int runType) {
            this.runType = runType;
            return this;
        }

        /**
         * Sets the {@code runTarget} and returns a reference to this Builder enabling method chaining.
         *
         * @param runTarget the {@code runTarget} to set
         * @return a reference to this Builder
         */
        public Builder runTarget(String runTarget) {
            this.runTarget = runTarget;
            return this;
        }

        /**
         * Sets the {@code alertFailRate} and returns a reference to this Builder enabling method chaining.
         *
         * @param alertFailRate the {@code alertFailRate} to set
         * @return a reference to this Builder
         */
        public Builder alertFailRate(int alertFailRate) {
            this.alertFailRate = alertFailRate;
            return this;
        }

        /**
         * Sets the {@code alertFailPartnerRate} and returns a reference to this Builder enabling method chaining.
         *
         * @param alertFailPartnerRate the {@code alertFailPartnerRate} to set
         * @return a reference to this Builder
         */
        public Builder alertFailPartnerRate(int alertFailPartnerRate) {
            this.alertFailPartnerRate = alertFailPartnerRate;
            return this;
        }

        /**
         * Sets the {@code alertFailProgramRate} and returns a reference to this Builder enabling method chaining.
         *
         * @param alertFailProgramRate the {@code alertFailProgramRate} to set
         * @return a reference to this Builder
         */
        public Builder alertFailProgramRate(int alertFailProgramRate) {
            this.alertFailProgramRate = alertFailProgramRate;
            return this;
        }

        /**
         * Sets the {@code alertFailDataRate} and returns a reference to this Builder enabling method chaining.
         *
         * @param alertFailDataRate the {@code alertFailDataRate} to set
         * @return a reference to this Builder
         */
        public Builder alertFailDataRate(int alertFailDataRate) {
            this.alertFailDataRate = alertFailDataRate;
            return this;
        }

        /**
         * Sets the {@code alertWaitTimeout} and returns a reference to this Builder enabling method chaining.
         *
         * @param alertWaitTimeout the {@code alertWaitTimeout} to set
         * @return a reference to this Builder
         */
        public Builder alertWaitTimeout(int alertWaitTimeout) {
            this.alertWaitTimeout = alertWaitTimeout;
            return this;
        }

        /**
         * Sets the {@code alertRunTimeout} and returns a reference to this Builder enabling method chaining.
         *
         * @param alertRunTimeout the {@code alertRunTimeout} to set
         * @return a reference to this Builder
         */
        public Builder alertRunTimeout(int alertRunTimeout) {
            this.alertRunTimeout = alertRunTimeout;
            return this;
        }

        /**
         * Sets the {@code state} and returns a reference to this Builder enabling method chaining.
         *
         * @param state the {@code state} to set
         * @return a reference to this Builder
         */
        public Builder state(int state) {
            this.state = state;
            return this;
        }

        /**
         * Sets the {@code logLevel} and returns a reference to this Builder enabling method chaining.
         *
         * @param logLevel the {@code logLevel} to set
         * @return a reference to this Builder
         */
        public Builder logLevel(int logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        /**
         * Sets the {@code logLimitSize} and returns a reference to this Builder enabling method chaining.
         *
         * @param logLimitSize the {@code logLimitSize} to set
         * @return a reference to this Builder
         */
        public Builder logLimitSize(int logLimitSize) {
            this.logLimitSize = logLimitSize;
            return this;
        }

        /**
         * Returns a {@code TaskCronerConfig} built from the parameters previously set.
         *
         * @return a {@code TaskCronerConfig} built with parameters of this {@code TaskCronerConfig.Builder}
         */
        public TaskCronerConfig build() {
            return new TaskCronerConfig( this );
        }
    }
}
