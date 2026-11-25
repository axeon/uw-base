package uw.task.entity;

import uw.task.TaskCroner;

import java.io.Serializable;

/**
 * 定时任务配置。
 *
 * <p>由 {@link TaskCroner#initConfig()} 产出默认值，首次注册时上传到 task-center，
 * 之后以服务端下发的动态配置为准。支持多实例：同一 taskClass 通过不同 taskParam 区分。
 * 关键字段：runType（单例/到处运行）、taskCron（cron 表达式）、各类 alertFailRate（报警阈值）。</p>
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
     * 默认运行目标。
     */
    public static final String DEFAULT_RUN_TARGET = "default";

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
    private String runTarget = DEFAULT_RUN_TARGET;

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
     * 配置失败率告警阈值（百分比）。
     */
    private int alertFailConfigRate;

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
     * 日志类型，取 {@link #TASK_LOG_TYPE_NONE} 等常量（-1 不记录/0 基础/1 含参数/2 含返回/3 全部）
     */
    private int logLevel = TASK_LOG_TYPE_RECORD;

    /**
     * 日志字符串字段大小限制: 0 表示无限制
     */
    private int logLimitSize = 0;

    /**
     * 默认构造器。
     */
    public TaskCronerConfig() {
    }

    /**
     * 带任务名称的构造器。
     *
     * @param taskName 任务名称
     */
    public TaskCronerConfig(String taskName) {
        this.taskName = taskName;
    }

    /**
     * 带任务名称与 cron 表达式的构造器。
     *
     * @param taskName 任务名称
     * @param taskCron cron 表达式
     */
    public TaskCronerConfig(String taskName, String taskCron) {
        this.taskName = taskName;
        this.taskCron = taskCron;
    }

    /**
     * 由 Builder 构建实例。
     * @param builder 已设置好字段的 Builder
     */
    private TaskCronerConfig(Builder builder) {
        setId(builder.id);
        setTaskClass(builder.taskClass);
        setTaskParam(builder.taskParam);
        setTaskName(builder.taskName);
        setTaskDesc(builder.taskDesc);
        setTaskCron(builder.taskCron);
        setRunType(builder.runType);
        setRunTarget(builder.runTarget);
        setAlertFailRate(builder.alertFailRate);
        setAlertFailPartnerRate(builder.alertFailPartnerRate);
        setAlertFailProgramRate(builder.alertFailProgramRate);
        setAlertFailConfigRate(builder.alertFailConfigRate);
        setAlertFailDataRate(builder.alertFailDataRate);
        setAlertWaitTimeout(builder.alertWaitTimeout);
        setAlertRunTimeout(builder.alertRunTimeout);
        setState(builder.state);
        setLogLevel(builder.logLevel);
        setLogLimitSize(builder.logLimitSize);
    }

    /**
     * builder 模式，带 taskName 参数。
     *
     * @param taskName 任务名称
     * @return 新的 Builder
     */
    public static Builder builder(String taskName) {
        return new Builder().taskName(taskName);
    }

    /**
     * builder 模式，带 taskName 和 taskCron 参数。
     *
     * @param taskName 任务名称
     * @param taskCron cron 表达式
     * @return 新的 Builder
     */
    public static Builder builder(String taskName, String taskCron) {
        return new Builder().taskName(taskName).taskCron(taskCron);
    }

    /**
     * 创建空 Builder。
     *
     * @return 空白 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 基于已有配置复制出一个 Builder（用于局部修改后重建）。
     *
     * @param copy 源配置
     * @return 复制了源配置字段的 Builder
     */
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
        builder.alertFailConfigRate = copy.getAlertFailConfigRate();
        builder.alertFailDataRate = copy.getAlertFailDataRate();
        builder.alertWaitTimeout = copy.getAlertWaitTimeout();
        builder.alertRunTimeout = copy.getAlertRunTimeout();
        builder.state = copy.getState();
        builder.logLevel = copy.getLogLevel();
        builder.logLimitSize = copy.getLogLimitSize();
        return builder;
    }

    /**
     * 获取任务配置 id。
     *
     * @return 任务配置 id
     */
    public long getId() {
        return id;
    }

    /**
     * 设置任务配置 id。
     *
     * @param id 任务配置 id
     */
    public void setId(long id) {
        this.id = id;
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
     * 获取执行参数，可能用于区分子任务。
     *
     * @return 执行参数，可能用于区分子任务
     */
    public String getTaskParam() {
        return taskParam;
    }

    /**
     * 设置执行参数，可能用于区分子任务。
     *
     * @param taskParam 执行参数，可能用于区分子任务
     */
    public void setTaskParam(String taskParam) {
        this.taskParam = taskParam;
    }

    /**
     * 获取任务名称。
     *
     * @return 任务名称
     */
    public String getTaskName() {
        return taskName;
    }

    /**
     * 设置任务名称。
     *
     * @param taskName 任务名称
     */
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    /**
     * 获取任务描述。
     *
     * @return 任务描述
     */
    public String getTaskDesc() {
        return taskDesc;
    }

    /**
     * 设置任务描述。
     *
     * @param taskDesc 任务描述
     */
    public void setTaskDesc(String taskDesc) {
        this.taskDesc = taskDesc;
    }

    /**
     * 获取cron表达式，默认5秒一次。
     *
     * @return cron表达式，默认5秒一次
     */
    public String getTaskCron() {
        return taskCron;
    }

    /**
     * 设置cron表达式，默认5秒一次。
     *
     * @param taskCron cron表达式，默认5秒一次
     */
    public void setTaskCron(String taskCron) {
        this.taskCron = taskCron;
    }

    /**
     * 获取0随意执行，1全局唯一执行。
     *
     * @return 0随意执行，1全局唯一执行
     */
    public int getRunType() {
        return runType;
    }

    /**
     * 设置0随意执行，1全局唯一执行。
     *
     * @param runType 0随意执行，1全局唯一执行
     */
    public void setRunType(int runType) {
        this.runType = runType;
    }

    /**
     * 获取运行目标，默认不指定。
     *
     * @return 运行目标，默认不指定
     */
    public String getRunTarget() {
        return runTarget;
    }

    /**
     * 设置运行目标，默认不指定。
     *
     * @param runTarget 运行目标，默认不指定
     */
    public void setRunTarget(String runTarget) {
        this.runTarget = runTarget;
    }

    /**
     * 获取失败率。
     *
     * @return 失败率
     */
    public int getAlertFailRate() {
        return alertFailRate;
    }

    /**
     * 设置失败率。
     *
     * @param alertFailRate 失败率
     */
    public void setAlertFailRate(int alertFailRate) {
        this.alertFailRate = alertFailRate;
    }

    /**
     * 获取接口失败率。
     *
     * @return 接口失败率
     */
    public int getAlertFailPartnerRate() {
        return alertFailPartnerRate;
    }

    /**
     * 设置接口失败率。
     *
     * @param alertFailPartnerRate 接口失败率
     */
    public void setAlertFailPartnerRate(int alertFailPartnerRate) {
        this.alertFailPartnerRate = alertFailPartnerRate;
    }

    /**
     * 获取程序失败率。
     *
     * @return 程序失败率
     */
    public int getAlertFailProgramRate() {
        return alertFailProgramRate;
    }

    /**
     * 设置程序失败率。
     *
     * @param alertFailProgramRate 程序失败率
     */
    public void setAlertFailProgramRate(int alertFailProgramRate) {
        this.alertFailProgramRate = alertFailProgramRate;
    }

    /**
     * 获取配置失败率告警阈值（百分比）。
     *
     * @return 配置失败率告警阈值（百分比）
     */
    public int getAlertFailConfigRate() {
        return alertFailConfigRate;
    }

    /**
     * 设置配置失败率告警阈值（百分比）。
     *
     * @param alertFailConfigRate 配置失败率告警阈值（百分比）
     */
    public void setAlertFailConfigRate(int alertFailConfigRate) {
        this.alertFailConfigRate = alertFailConfigRate;
    }

    /**
     * 获取数据失败率。
     *
     * @return 数据失败率
     */
    public int getAlertFailDataRate() {
        return alertFailDataRate;
    }

    /**
     * 设置数据失败率。
     *
     * @param alertFailDataRate 数据失败率
     */
    public void setAlertFailDataRate(int alertFailDataRate) {
        this.alertFailDataRate = alertFailDataRate;
    }

    /**
     * 获取等待超时。
     *
     * @return 等待超时
     */
    public int getAlertWaitTimeout() {
        return alertWaitTimeout;
    }

    /**
     * 设置等待超时。
     *
     * @param alertWaitTimeout 等待超时
     */
    public void setAlertWaitTimeout(int alertWaitTimeout) {
        this.alertWaitTimeout = alertWaitTimeout;
    }

    /**
     * 获取运行超时。
     *
     * @return 运行超时
     */
    public int getAlertRunTimeout() {
        return alertRunTimeout;
    }

    /**
     * 设置运行超时。
     *
     * @param alertRunTimeout 运行超时
     */
    public void setAlertRunTimeout(int alertRunTimeout) {
        this.alertRunTimeout = alertRunTimeout;
    }

    /**
     * 获取状态值。
     *
     * @return 状态值
     */
    public int getState() {
        return state;
    }

    /**
     * 设置状态值。
     *
     * @param state 状态值
     */
    public void setState(int state) {
        this.state = state;
    }

    /**
     * 获取日志类型，取 {@link #TASK_LOG_TYPE_NONE} 等常量（-1 不记录/0 基础/1 含参数/2 含返回/3 全部）。
     *
     * @return 日志类型，取 {@link #TASK_LOG_TYPE_NONE} 等常量（-1 不记录/0 基础/1 含参数/2 含返回/3 全部）
     */
    public int getLogLevel() {
        return logLevel;
    }

    /**
     * 设置日志类型，取 {@link #TASK_LOG_TYPE_NONE} 等常量（-1 不记录/0 基础/1 含参数/2 含返回/3 全部）。
     *
     * @param logLevel 日志类型，取 {@link #TASK_LOG_TYPE_NONE} 等常量（-1 不记录/0 基础/1 含参数/2 含返回/3 全部）
     */
    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    /**
     * 获取日志字符串字段大小限制: 0 表示无限制。
     *
     * @return 日志字符串字段大小限制: 0 表示无限制
     */
    public int getLogLimitSize() {
        return logLimitSize;
    }

    /**
     * 设置日志字符串字段大小限制: 0 表示无限制。
     *
     * @param logLimitSize 日志字符串字段大小限制: 0 表示无限制
     */
    public void setLogLimitSize(int logLimitSize) {
        this.logLimitSize = logLimitSize;
    }

    /**
     * {@code TaskCronerConfig} builder static inner class.
     */
    public static final class Builder {

        /**
         * 任务配置 id（服务端分配）。
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
        private String runTarget = DEFAULT_RUN_TARGET;

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
         * 配置失败率告警阈值（百分比）。
         */
        private int alertFailConfigRate;

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
         * 日志类型，取 {@link #TASK_LOG_TYPE_NONE} 等常量（-1 不记录/0 基础/1 含参数/2 含返回/3 全部）
         */
        private int logLevel = TASK_LOG_TYPE_RECORD;

        /**
         * 日志字符串字段大小限制: 0 表示无限制
         */
        private int logLimitSize = 0;

        /**
         * Builder 构造器（仅经 builder(...) 工厂方法创建）。
         */
        private Builder() {
        }

        /**
         * builder模式，带taskClass参数。
         *
         * @param taskClass 执行类信息
         * @return 新的 Builder
         */
        public static Builder builder(String taskClass) {
            return new Builder().taskClass(taskClass);
        }

        /**
         * builder模式，带taskClass参数。
         *
         * @param taskClass 执行类信息
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
         * 设置执行参数，可能用于区分子任务。
         *
         * @param taskParam 执行参数，可能用于区分子任务
         * @return this（链式调用）
         */
        public Builder taskParam(String taskParam) {
            this.taskParam = taskParam;
            return this;
        }

        /**
         * 设置任务名称。
         *
         * @param taskName 任务名称
         * @return this（链式调用）
         */
        public Builder taskName(String taskName) {
            this.taskName = taskName;
            return this;
        }

        /**
         * 设置任务描述。
         *
         * @param taskDesc 任务描述
         * @return this（链式调用）
         */
        public Builder taskDesc(String taskDesc) {
            this.taskDesc = taskDesc;
            return this;
        }

        /**
         * 设置cron表达式，默认5秒一次。
         *
         * @param taskCron cron表达式，默认5秒一次
         * @return this（链式调用）
         */
        public Builder taskCron(String taskCron) {
            this.taskCron = taskCron;
            return this;
        }

        /**
         * 设置0随意执行，1全局唯一执行。
         *
         * @param runType 0随意执行，1全局唯一执行
         * @return this（链式调用）
         */
        public Builder runType(int runType) {
            this.runType = runType;
            return this;
        }

        /**
         * 设置运行目标，默认不指定。
         *
         * @param runTarget 运行目标，默认不指定
         * @return this（链式调用）
         */
        public Builder runTarget(String runTarget) {
            this.runTarget = runTarget;
            return this;
        }

        /**
         * 设置失败率。
         *
         * @param alertFailRate 失败率
         * @return this（链式调用）
         */
        public Builder alertFailRate(int alertFailRate) {
            this.alertFailRate = alertFailRate;
            return this;
        }

        /**
         * 设置接口失败率。
         *
         * @param alertFailPartnerRate 接口失败率
         * @return this（链式调用）
         */
        public Builder alertFailPartnerRate(int alertFailPartnerRate) {
            this.alertFailPartnerRate = alertFailPartnerRate;
            return this;
        }

        /**
         * 设置程序失败率。
         *
         * @param alertFailProgramRate 程序失败率
         * @return this（链式调用）
         */
        public Builder alertFailProgramRate(int alertFailProgramRate) {
            this.alertFailProgramRate = alertFailProgramRate;
            return this;
        }

        /**
         * 设置配置失败率告警阈值（百分比）。
         *
         * @param alertFailConfigRate 配置失败率告警阈值（百分比）
         * @return this（链式调用）
         */
        public Builder alertFailConfigRate(int alertFailConfigRate) {
            this.alertFailConfigRate = alertFailConfigRate;
            return this;
        }

        /**
         * 设置数据失败率。
         *
         * @param alertFailDataRate 数据失败率
         * @return this（链式调用）
         */
        public Builder alertFailDataRate(int alertFailDataRate) {
            this.alertFailDataRate = alertFailDataRate;
            return this;
        }

        /**
         * 设置等待超时。
         *
         * @param alertWaitTimeout 等待超时
         * @return this（链式调用）
         */
        public Builder alertWaitTimeout(int alertWaitTimeout) {
            this.alertWaitTimeout = alertWaitTimeout;
            return this;
        }

        /**
         * 设置运行超时。
         *
         * @param alertRunTimeout 运行超时
         * @return this（链式调用）
         */
        public Builder alertRunTimeout(int alertRunTimeout) {
            this.alertRunTimeout = alertRunTimeout;
            return this;
        }

        /**
         * 设置状态值。
         *
         * @param state 状态值
         * @return this（链式调用）
         */
        public Builder state(int state) {
            this.state = state;
            return this;
        }

        /**
         * 设置日志类型，取 {@link #TASK_LOG_TYPE_NONE} 等常量（-1 不记录/0 基础/1 含参数/2 含返回/3 全部）。
         *
         * @param logLevel 日志类型，取 {@link #TASK_LOG_TYPE_NONE} 等常量（-1 不记录/0 基础/1 含参数/2 含返回/3 全部）
         * @return this（链式调用）
         */
        public Builder logLevel(int logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        /**
         * 设置日志字符串字段大小限制: 0 表示无限制。
         *
         * @param logLimitSize 日志字符串字段大小限制: 0 表示无限制
         * @return this（链式调用）
         */
        public Builder logLimitSize(int logLimitSize) {
            this.logLimitSize = logLimitSize;
            return this;
        }

        /**
         * 按已设置的参数构建实例。
         *
         * @return 构建出的对象
         */
        public TaskCronerConfig build() {
            return new TaskCronerConfig(this);
        }
    }
}
