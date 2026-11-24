package uw.task.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.util.JsonUtils;
import uw.log.es.vo.LogBaseVo;
import uw.task.TaskData;

import java.io.Serializable;
import java.util.Date;

/**
 * 队列任务执行日志。
 *
 * <p>每次队列任务执行后由 {@code TaskRunnerContainer} 构造并写入 ES。内部持有 {@link TaskData} 引用，
 * 各 getter 委托 taskData 取值（因 taskParam/resultData 泛型类型无法直接平铺为字段，故用委托方式）。
 * 序列化时忽略 taskData 本身（由各 getter 暴露具体字段），resultData 超过 logLimitSize 时截断。</p>
 *
 * @author axeon
 */
@JsonIgnoreProperties({"taskData"})
public class TaskRunnerLog extends LogBaseVo implements Serializable {

    /**
     * 日志器。
     */
    private static final Logger logger = LoggerFactory.getLogger(TaskRunnerLog.class);

    /**
     * 关联的任务数据对象（日志各字段委托自此对象取值）。
     */
    private TaskData taskData;

    /**
     * 日志字符串字段（resultData 等）最大长度，0 表示不限制。
     */
    private int logLimitSize;

    /**
     * 对应的 TaskRunnerConfig 的 id。
     */
    private long taskId;

    /**
     * 默认构造器。
     */
    public TaskRunnerLog() {
    }

    /**
     * 由任务数据构建日志对象（各字段委托自此对象取值）。
     *
     * @param taskData 关联的任务数据对象
     */
    public TaskRunnerLog(TaskData taskData) {
        this.taskData = taskData;
    }

    /**
     * 获取关联的任务数据对象（日志各字段委托自此对象取值）。
     *
     * @return 关联的任务数据对象（日志各字段委托自此对象取值）
     */
    public TaskData getTaskData() {
        return taskData;
    }

    /**
     * 返回任务 id（框架自动生成）。
     *
     * @return 任务 id（框架自动生成）
     */
    public long getId() {
        return taskData.getId();
    }

    /**
     * 返回关联 id。
     *
     * @return 关联 id
     */
    public long getRefId() {
        return taskData.getRefId();
    }

    /**
     * 返回关联子 id。
     *
     * @return 关联子 id
     */
    public long getRefSubId() {
        return taskData.getRefSubId();
    }

    /**
     * 返回关联 TAG。
     *
     * @return 关联 TAG
     */
    public String getRefTag() {
        return taskData.getRefTag();
    }

    /**
     * 返回限速 TAG。
     *
     * @return 限速 TAG
     */
    public String getRateLimitTag() {
        return taskData.getRateLimitTag();
    }

    /**
     * 返回任务类全限定名。
     *
     * @return 任务类全限定名
     */
    public String getTaskClass() {
        return taskData.getTaskClass();
    }

    /**
     * 返回任务标签。
     *
     * @return 任务标签
     */
    public String getTaskTag() {
        return taskData.getTaskTag();
    }

    /**
     * 返回延时毫秒数。
     *
     * @return 延时毫秒数
     */
    public long getTaskDelay() {
        return taskData.getTaskDelay();
    }

    /**
     * 返回执行参数。
     *
     * @return 执行参数
     */
    public String getTaskParam() {
        Object value = taskData.getTaskParam();
        if (value != null) {
            // 报错了
            if (taskData.getState() != TaskData.STATE_SUCCESS || logLevel == TaskRunnerConfig.TASK_LOG_TYPE_RECORD_ALL ||
                    logLevel == TaskRunnerConfig.TASK_LOG_TYPE_RECORD_TASK_PARAM) {
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
     * 返回运行类型。
     *
     * @return 运行类型
     */
    public int getRunType() {
        return taskData.getRunType();
    }

    /**
     * 返回重试类型。
     *
     * @return 重试类型
     */
    public int getRetryType() {
        return taskData.getRetryType();
    }

    /**
     * 返回运行目标。
     *
     * @return 运行目标
     */
    public String getRunTarget() {
        return taskData.getRunTarget();
    }

    /**
     * 返回入队时间。
     *
     * @return 入队时间
     */
    public Date getQueueDate() {
        return taskData.getQueueDate();
    }

    /**
     * 返回开始消费时间。
     *
     * @return 开始消费时间
     */
    public Date getConsumeDate() {
        return taskData.getConsumeDate();
    }

    /**
     * 返回开始运行时间。
     *
     * @return 开始运行时间
     */
    public Date getRunDate() {
        return taskData.getRunDate();
    }

    /**
     * 返回运行结束时间。
     *
     * @return 运行结束时间
     */
    public Date getFinishDate() {
        return taskData.getFinishDate();
    }

    /**
     * 返回执行结果。
     *
     * @return 执行结果
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
     * 返回出错信息。
     *
     * @return 出错信息
     */
    public String getErrorInfo() {
        return taskData.getErrorInfo();
    }

    /**
     * 返回已执行次数。
     *
     * @return 已执行次数
     */
    public int getRanTimes() {
        return taskData.getRanTimes();
    }

    /**
     * 返回执行状态。
     *
     * @return 执行状态
     */
    public int getState() {
        return taskData.getState();
    }

    /**
     * 获取日志字符串字段（resultData 等）最大长度，0 表示不限制。
     *
     * @return 日志字符串字段（resultData 等）最大长度，0 表示不限制
     */
    public int getLogLimitSize() {
        return logLimitSize;
    }

    /**
     * 设置日志字符串字段（resultData 等）最大长度，0 表示不限制。
     *
     * @param logLimitSize 日志字符串字段（resultData 等）最大长度，0 表示不限制
     */
    public void setLogLimitSize(int logLimitSize) {
        this.logLimitSize = logLimitSize;
    }

    /**
     * 获取对应的 TaskRunnerConfig 的 id。
     *
     * @return 对应的 TaskRunnerConfig 的 id
     */
    public long getTaskId() {
        return taskId;
    }

    /**
     * 设置对应的 TaskRunnerConfig 的 id。
     *
     * @param taskId 对应的 TaskRunnerConfig 的 id
     */
    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }
}
