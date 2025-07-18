package uw.task.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.util.JsonUtils;
import uw.log.es.vo.LogBaseVo;
import uw.task.TaskData;

import java.util.Date;

/**
 * 专门用于发送日志给log-es。
 * 因为task参数的问题。
 *
 * @author axeon
 */
@JsonIgnoreProperties({"taskData"})
public class TaskRunnerLog extends LogBaseVo {

    private static final Logger logger = LoggerFactory.getLogger(TaskRunnerLog.class);

    private TaskData taskData;

    /**
     * logLimitSize。
     */
    private int logLimitSize;

    /**
     * 对应的TaskCronerConfig的id
     */
    private long taskId;

    public TaskRunnerLog() {
    }

    public TaskRunnerLog(TaskData taskData) {
        this.taskData = taskData;
    }

    public TaskData getTaskData() {
        return taskData;
    }

    /**
     * @return the id
     */
    public long getId() {
        return taskData.getId();
    }

    /**
     * @return the refId
     */
    public long getRefId() {
        return taskData.getRefId();
    }

    /**
     * @return the refSubId
     */
    public long getRefSubId() {
        return taskData.getRefSubId();
    }

    /**
     * @return the refTag
     */
    public String getRefTag() {
        return taskData.getRefTag();
    }

    /**
     * @return the rateLimitTag
     */
    public String getRateLimitTag() {
        return taskData.getRateLimitTag();
    }

    /**
     * @return the taskClass
     */
    public String getTaskClass() {
        return taskData.getTaskClass();
    }

    /**
     * @return the taskTag
     */
    public String getTaskTag() {
        return taskData.getTaskTag();
    }

    public long getTaskDelay() {
        return taskData.getTaskDelay();
    }

    /**
     * @return the taskParam
     */
    public String getTaskParam() {
        Object value = taskData.getTaskParam();
        if (value != null) {
            // 报错了
            if (taskData.getState() != TaskData.STATE_SUCCESS || logLevel == TaskRunnerConfig.TASK_LOG_TYPE_RECORD_ALL ||
                    logLevel == TaskRunnerConfig.TASK_LOG_TYPE_RECORD_TASK_PARAM) {
                String data = null;
                try {
                    data = JsonUtils.toString( value );
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
     * @return the runType
     */
    public int getRunType() {
        return taskData.getRunType();
    }

    /**
     * @return the retryType
     */
    public int getRetryType() {
        return taskData.getRetryType();
    }

    /**
     * @return the runTarget
     */
    public String getRunTarget() {
        return taskData.getRunTarget();
    }

    /**
     * @return the queueDate
     */
    public Date getQueueDate() {
        return taskData.getQueueDate();
    }

    /**
     * @return the consumeDate
     */
    public Date getConsumeDate() {
        return taskData.getConsumeDate();
    }

    /**
     * @return the runDate
     */
    public Date getRunDate() {
        return taskData.getRunDate();
    }

    /**
     * @return the finishDate
     */
    public Date getFinishDate() {
        return taskData.getFinishDate();
    }

    /**
     * @return the resultData
     */
    public String getResultData() {
        Object value = taskData.getResultData();
        if (value != null) {
            // 报错了
            if (taskData.getState() != TaskData.STATE_SUCCESS || logLevel == TaskRunnerConfig.TASK_LOG_TYPE_RECORD_ALL ||
                    logLevel == TaskRunnerConfig.TASK_LOG_TYPE_RECORD_RESULT_DATA) {
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
     * @return the errorInfo
     */
    public String getErrorInfo() {
        return taskData.getErrorInfo();
    }

    /**
     * @return the ranTimes
     */
    public int getRanTimes() {
        return taskData.getRanTimes();
    }

    /**
     * @return the status
     */
    public int getState() {
        return taskData.getState();
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
