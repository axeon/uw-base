package uw.task.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import uw.log.es.vo.LogBaseVo;
import uw.task.TaskData;
import uw.task.constant.TaskLogRecordType;

import java.io.Serializable;
import java.util.Date;

/**
 * 定时任务执行日志。
 *
 * <p>每次定时任务执行后由 {@code TaskCronerContainer} 构造并写入 ES。继承 {@link LogBaseVo}
 * 获取日志基础字段（如 logLevel）。resultData/taskParam 超过 {@link #logLimitSize} 时会被截断。
 * 序列化时忽略 refObject（运行期临时对象，不入库）。
 *
 * @author axeon
 */
@JsonIgnoreProperties({"refObject"})
public class TaskCronerLog extends LogBaseVo implements Serializable {

    /**
     * 日志 id（由框架发号器生成）。
     */
    private long id;

    /**
     * 对应的TaskCronerConfig的id
     */
    private long taskId;
    /**
     * 关联ID
     */
    private long refId;

    /**
     * 关联对象，此对象不会发送到服务器端。
     */
    private Object refObject;

    /**
     * 执行的类名
     */
    private String taskClass;

    /**
     * 执行参数，可能用于区分子任务
     */
    private String taskParam;

    /**
     * 运行类型。
     */
    private int runType;

    /**
     * 指定运行目标主机，可为空。
     */
    private String runTarget;

    /**
     * 配置信息
     */
    private String taskCron;

    /**
     * 计划执行时间
     */
    private Date scheduleDate;

    /**
     * 开始运行时间
     */
    private Date runDate;

    /**
     * 运行结束日期
     */
    private Date finishDate;

    /**
     * 下次执行时间
     */
    private Date nextDate;

    /**
     * 执行信息，用于存储任务完成信息。
     */
    private String resultData;

    /**
     * 执行状态
     */
    private int state;

    /**
     * 日志字符串字段大小限制: 0 表示无限制
     */
    private int logLimitSize;

    /**
     * 构造定时任务日志，指定日志级别与字段截断长度。
     *
     * @param logLevel     日志级别（见 {@link TaskLogRecordType}）
     * @param logLimitSize 日志字符串字段（resultData/taskParam）最大长度，0 表示不限制
     */
    public TaskCronerLog(int logLevel, int logLimitSize) {
        this.logLevel = logLevel;
        this.logLimitSize = logLimitSize;
    }

    /**
     * 获取日志 id（由框架发号器生成）。
     *
     * @return 日志 id（由框架发号器生成）
     */
    public long getId() {
        return id;
    }

    /**
     * 设置日志 id（由框架发号器生成）。
     *
     * @param id 日志 id（由框架发号器生成）
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * 获取关联ID。
     *
     * @return 关联ID
     */
    public long getRefId() {
        return refId;
    }

    /**
     * 设置关联ID。
     *
     * @param refId 关联ID
     */
    public void setRefId(long refId) {
        this.refId = refId;
    }

    /**
     * 获取关联对象，此对象不会发送到服务器端。
     *
     * @return 关联对象，此对象不会发送到服务器端
     */
    public Object getRefObject() {
        return refObject;
    }

    /**
     * 设置关联对象，此对象不会发送到服务器端。
     *
     * @param refObject 关联对象，此对象不会发送到服务器端
     */
    public void setRefObject(Object refObject) {
        this.refObject = refObject;
    }

    /**
     * 获取执行的类名。
     *
     * @return 执行的类名
     */
    public String getTaskClass() {
        return taskClass;
    }

    /**
     * 设置执行的类名。
     *
     * @param taskClass 执行的类名
     */
    public void setTaskClass(String taskClass) {
        this.taskClass = taskClass;
    }

    /**
     * 获取任务参数，并按 logLevel 决定是否输出、是否截断。
     * <p>记录规则：
     * <ul>
     *   <li>任务执行失败（state != STATE_SUCCESS）时始终输出参数，便于排查；</li>
     *   <li>任务成功时仅在 logLevel 为 {@link TaskCronerConfig#TASK_LOG_TYPE_RECORD_ALL}
     *       或 {@link TaskCronerConfig#TASK_LOG_TYPE_RECORD_TASK_PARAM} 时输出；</li>
     *   <li>logLimitSize > 0 且参数长度超限时，截断为前 logLimitSize 个字符。</li>
     * </ul>
     * 不满足输出条件或参数为空时返回 null。
     *
     * @return 按 logLevel 过滤并截断后的任务参数，不记录时返回 null
     */
    public String getTaskParam() {
        if (taskParam != null) {
            if (state != TaskData.STATE_SUCCESS || logLevel == TaskCronerConfig.TASK_LOG_TYPE_RECORD_ALL ||
                    logLevel == TaskCronerConfig.TASK_LOG_TYPE_RECORD_TASK_PARAM) {
                if (logLimitSize > 0 && taskParam.length() > logLimitSize) {
                    return taskParam.substring(0, logLimitSize);
                }
                return taskParam;
            }
        }
        return null;
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
     * 获取运行类型。
     *
     * @return 运行类型
     */
    public int getRunType() {
        return runType;
    }

    /**
     * 设置运行类型。
     *
     * @param runType 运行类型
     */
    public void setRunType(int runType) {
        this.runType = runType;
    }

    /**
     * 获取指定运行目标主机，可为空。
     *
     * @return 指定运行目标主机，可为空
     */
    public String getRunTarget() {
        return runTarget;
    }

    /**
     * 设置指定运行目标主机，可为空。
     *
     * @param runTarget 指定运行目标主机，可为空
     */
    public void setRunTarget(String runTarget) {
        this.runTarget = runTarget;
    }

    /**
     * 获取配置信息。
     *
     * @return 配置信息
     */
    public String getTaskCron() {
        return taskCron;
    }

    /**
     * 设置配置信息。
     *
     * @param taskCron 配置信息
     */
    public void setTaskCron(String taskCron) {
        this.taskCron = taskCron;
    }

    /**
     * 获取计划执行时间。
     *
     * @return 计划执行时间
     */
    public Date getScheduleDate() {
        return scheduleDate;
    }

    /**
     * 设置计划执行时间。
     *
     * @param scheduleDate 计划执行时间
     */
    public void setScheduleDate(Date scheduleDate) {
        this.scheduleDate = scheduleDate;
    }

    /**
     * 获取开始运行时间。
     *
     * @return 开始运行时间
     */
    public Date getRunDate() {
        return runDate;
    }

    /**
     * 设置开始运行时间。
     *
     * @param runDate 开始运行时间
     */
    public void setRunDate(Date runDate) {
        this.runDate = runDate;
    }

    /**
     * 获取运行结束日期。
     *
     * @return 运行结束日期
     */
    public Date getFinishDate() {
        return finishDate;
    }

    /**
     * 设置运行结束日期。
     *
     * @param finishDate 运行结束日期
     */
    public void setFinishDate(Date finishDate) {
        this.finishDate = finishDate;
    }

    /**
     * 获取下次执行时间。
     *
     * @return 下次执行时间
     */
    public Date getNextDate() {
        return nextDate;
    }

    /**
     * 设置下次执行时间。
     *
     * @param nextDate 下次执行时间
     */
    public void setNextDate(Date nextDate) {
        this.nextDate = nextDate;
    }

    /**
     * 获取执行结果数据，并按 logLevel 决定是否输出、是否截断。
     * <p>记录规则（与 {@link #getTaskParam} 对称，仅 logLevel 常量不同）：
     * <ul>
     *   <li>任务执行失败（state != STATE_SUCCESS）时始终输出结果（含异常堆栈），便于排查；</li>
     *   <li>任务成功时仅在 logLevel 为 {@link TaskCronerConfig#TASK_LOG_TYPE_RECORD_ALL}
     *       或 {@link TaskCronerConfig#TASK_LOG_TYPE_RECORD_RESULT_DATA} 时输出；</li>
     *   <li>logLimitSize > 0 且结果长度超限时，截断为前 logLimitSize 个字符。</li>
     * </ul>
     * 不满足输出条件或结果为空时返回 null。
     *
     * @return 按 logLevel 过滤并截断后的结果数据，不记录时返回 null
     */
    public String getResultData() {
        if (resultData != null) {
            if (state != TaskData.STATE_SUCCESS || logLevel == TaskCronerConfig.TASK_LOG_TYPE_RECORD_ALL ||
                    logLevel == TaskCronerConfig.TASK_LOG_TYPE_RECORD_RESULT_DATA) {
                if (logLimitSize > 0 && resultData.length() > logLimitSize) {
                    return resultData.substring(0, logLimitSize);
                }
                return resultData;
            }
        }
        return null;
    }

    /**
     * 设置执行信息，用于存储任务完成信息。
     *
     * @param resultData 执行信息，用于存储任务完成信息
     */
    public void setResultData(String resultData) {
        this.resultData = resultData;
    }

    /**
     * 获取执行状态。
     *
     * @return 执行状态
     */
    public int getState() {
        return state;
    }

    /**
     * 设置执行状态。
     *
     * @param state 执行状态
     */
    public void setState(int state) {
        this.state = state;
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
     * 获取对应的TaskCronerConfig的id。
     *
     * @return 对应的TaskCronerConfig的id
     */
    public long getTaskId() {
        return taskId;
    }

    /**
     * 设置对应的TaskCronerConfig的id。
     *
     * @param taskId 对应的TaskCronerConfig的id
     */
    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }
}
