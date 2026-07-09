package uw.task.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.util.JsonUtils;
import uw.log.es.vo.LogBaseVo;
import uw.task.TaskData;

import java.util.Date;

/**
 * 延迟任务执行日志。
 *
 * <p>每次延迟任务执行后由 {@code TaskDelayerContainer} 构造并写入 ES（索引 uw.task.delayer.log）。
 * 载体复用 {@link TaskData}（与 {@link TaskRunnerLog} 一致委托模式）：各 getter 委托 taskData 取值，
 * taskParam/resultData 按 logLevel 决定是否序列化、超 logLimitSize 截断。triggerDate/delayMillis
 * 为延迟任务特有的触发时刻与实际延迟时长（runDate - (queueDate+taskDelay)），用于监控延迟精度。</p>
 *
 * @author axeon
 */
@JsonIgnoreProperties({"taskData"})
public class TaskDelayerLog extends LogBaseVo implements java.io.Serializable {

    private static final Logger logger = LoggerFactory.getLogger(TaskDelayerLog.class);

    /**
     * 关联的任务数据对象（各字段委托自此对象取值）。
     */
    private TaskData taskData;

    /**
     * 日志字符串字段（taskParam/resultData）最大长度，0 表示不限制。
     */
    private int logLimitSize;

    /**
     * 对应的 TaskDelayerConfig 的 id。
     */
    private long taskId;

    public TaskDelayerLog() {
    }

    public TaskDelayerLog(TaskData taskData) {
        this.taskData = taskData;
    }

    public TaskData getTaskData() {
        return taskData;
    }

    public long getId() {
        return taskData.getId();
    }

    public long getRefId() {
        return taskData.getRefId();
    }

    public long getRefSubId() {
        return taskData.getRefSubId();
    }

    public String getRefTag() {
        return taskData.getRefTag();
    }

    public String getTaskClass() {
        return taskData.getTaskClass();
    }

    public String getTaskTag() {
        return taskData.getTaskTag();
    }

    public String getRateLimitTag() {
        return taskData.getRateLimitTag();
    }

    public String getRunTarget() {
        return taskData.getRunTarget();
    }

    public long getTaskDelay() {
        return taskData.getTaskDelay();
    }

    public Date getQueueDate() {
        return taskData.getQueueDate();
    }

    public Date getConsumeDate() {
        return taskData.getConsumeDate();
    }

    public Date getRunDate() {
        return taskData.getRunDate();
    }

    public Date getFinishDate() {
        return taskData.getFinishDate();
    }

    /**
     * 触发执行时刻（同 runDate，单独暴露便于监控）。
     */
    public Date getTriggerDate() {
        return taskData.getRunDate();
    }

    /**
     * 实际延迟时长（runDate - 预期到期(queueDate+taskDelay)），正值表示晚于预期到期执行。
     */
    public long getDelayMillis() {
        if (taskData.getRunDate() != null && taskData.getQueueDate() != null) {
            return taskData.getRunDate().getTime() - (taskData.getQueueDate().getTime() + taskData.getTaskDelay());
        }
        return 0;
    }

    public int getRanTimes() {
        return taskData.getRanTimes();
    }

    public int getRetryType() {
        return taskData.getRetryType();
    }

    public int getState() {
        return taskData.getState();
    }

    public String getErrorInfo() {
        return taskData.getErrorInfo();
    }

    /**
     * @return 序列化后的 taskParam，按 logLevel 决定是否输出，超 logLimitSize 截断
     */
    public String getTaskParam() {
        Object value = taskData.getTaskParam();
        if (value != null) {
            if (taskData.getState() != TaskData.STATE_SUCCESS || logLevel == TaskDelayerConfig.TASK_LOG_TYPE_RECORD_ALL
                    || logLevel == TaskDelayerConfig.TASK_LOG_TYPE_RECORD_TASK_PARAM) {
                String data = null;
                try {
                    data = JsonUtils.toString(value);
                } catch (Exception e) {
                    data = e.getMessage();
                    logger.error(e.getMessage(), e);
                }
                if (data != null) {
                    if (logLimitSize > 0 && data.length() > logLimitSize) {
                        data = data.substring(0, logLimitSize);
                    }
                    return data;
                }
            }
        }
        return null;
    }

    /**
     * @return 序列化后的 resultData，按 logLevel 决定是否输出，超 logLimitSize 截断
     */
    public String getResultData() {
        Object value = taskData.getResultData();
        if (value != null) {
            if (taskData.getState() != TaskData.STATE_SUCCESS || logLevel == TaskDelayerConfig.TASK_LOG_TYPE_RECORD_ALL
                    || logLevel == TaskDelayerConfig.TASK_LOG_TYPE_RECORD_RESULT_DATA) {
                String data = null;
                try {
                    data = JsonUtils.toString(value);
                } catch (Exception e) {
                    data = e.getMessage();
                    logger.error(e.getMessage(), e);
                }
                if (data != null) {
                    if (logLimitSize > 0 && data.length() > logLimitSize) {
                        data = data.substring(0, logLimitSize);
                    }
                    return data;
                }
            }
        }
        return null;
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
