package uw.task.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import uw.log.es.vo.LogBaseVo;
import uw.task.TaskData;

import java.io.Serializable;
import java.util.Date;

/**
 * 定时任务执行日志。
 *
 * <p>每次定时任务执行后由 {@code TaskCronerContainer} 构造并写入 ES。继承 {@link LogBaseVo}
 * 获取日志基础字段（如 logLevel）。resultData/taskParam 超过 {@link #logLimitSize} 时会被截断。
 * 序列化时忽略 refObject（运行期临时对象，不入库）。</p>
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
     * @param logLevel     日志级别（见 {@link uw.task.constant.TaskLogRecordType}）
     * @param logLimitSize 日志字符串字段（resultData/taskParam）最大长度，0 表示不限制
     */
    public TaskCronerLog(int logLevel, int logLimitSize) {
        this.logLevel = logLevel;
        this.logLimitSize = logLimitSize;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getRefId() {
        return refId;
    }

    public void setRefId(long refId) {
        this.refId = refId;
    }

    public Object getRefObject() {
        return refObject;
    }

    public void setRefObject(Object refObject) {
        this.refObject = refObject;
    }

    public String getTaskClass() {
        return taskClass;
    }

    public void setTaskClass(String taskClass) {
        this.taskClass = taskClass;
    }

    public String getTaskParam() {
        if (taskParam != null) {
            if (state != TaskData.STATE_SUCCESS || logLevel == TaskCronerConfig.TASK_LOG_TYPE_RECORD_ALL ||
                    logLevel == TaskCronerConfig.TASK_LOG_TYPE_RECORD_RESULT_DATA) {
                if (logLimitSize > 0 && taskParam.length() > logLimitSize) {
                    return taskParam.substring(0, logLimitSize);
                }
                return taskParam;
            }
        }
        return null;
    }

    public void setTaskParam(String taskParam) {
        this.taskParam = taskParam;
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

    public String getTaskCron() {
        return taskCron;
    }

    public void setTaskCron(String taskCron) {
        this.taskCron = taskCron;
    }

    public Date getScheduleDate() {
        return scheduleDate;
    }

    public void setScheduleDate(Date scheduleDate) {
        this.scheduleDate = scheduleDate;
    }

    public Date getRunDate() {
        return runDate;
    }

    public void setRunDate(Date runDate) {
        this.runDate = runDate;
    }

    public Date getFinishDate() {
        return finishDate;
    }

    public void setFinishDate(Date finishDate) {
        this.finishDate = finishDate;
    }

    public Date getNextDate() {
        return nextDate;
    }

    public void setNextDate(Date nextDate) {
        this.nextDate = nextDate;
    }

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

    public void setResultData(String resultData) {
        this.resultData = resultData;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getLogLimitSize() {
        return logLimitSize;
    }

    public void setLogLimitSize(int logLimitSize) {
        this.logLimitSize = logLimitSize;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }
}
