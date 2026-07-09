package uw.task.log;

import org.junit.jupiter.api.Test;
import uw.common.util.KryoUtils;
import uw.task.TaskData;
import uw.task.entity.TaskRunnerConfig;
import uw.task.entity.TaskRunnerLog;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * TaskRunnerLog 字段输出与序列化测试。
 *
 * @since 2018-05-31
 */
public class TaskLogSerSpec {

    @Test
    public void testLogSpecByte() throws Exception {
        TaskData<String, String> taskData = new TaskData<>();
        taskData.setTaskParam("hello");
        taskData.setResultData("hello");

        TaskRunnerLog log = new TaskRunnerLog(taskData);
        log.setLogLevel(TaskRunnerConfig.TASK_LOG_TYPE_RECORD_ALL);
        // RECORD_ALL 级别应输出 taskParam/resultData
        assertNotNull(log.getTaskParam(), "RECORD_ALL 级别应输出 taskParam");
        assertNotNull(log.getResultData(), "RECORD_ALL 级别应输出 resultData");
        // round-trip 序列化验证（不抛异常 + 反序列化非 null）
        byte[] bytes = KryoUtils.serialize(log);
        assertNotNull(KryoUtils.deserialize(bytes, TaskRunnerLog.class), "反序列化结果不应为 null");
    }
}
