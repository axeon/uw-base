package uw.task.log;

import org.junit.jupiter.api.Test;
import uw.task.TaskData;
import uw.task.entity.TaskRunnerLog;

/**
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
    }
}
