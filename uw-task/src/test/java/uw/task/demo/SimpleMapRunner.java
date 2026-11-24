package uw.task.demo;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import uw.task.TaskData;
import uw.task.converter.TaskMessageConverter;
import uw.task.demo.SimpleMapRunner;

/**
 *
 * @since 2018-05-30
 */
public class SimpleMapRunner extends SimpleBaseMapRunner {
    public static void main(String[] args) {
        TaskMessageConverter converter = new TaskMessageConverter();

        SimpleMapRunner runner = new SimpleMapRunner();
        TaskData<Map<String, String>, Map<String, String>> taskData = new TaskData<>();
        taskData.setTaskParam(new HashMap<>());
        taskData.setResultData(new HashMap<>());
        taskData.setTaskClass("uw.task.demo.SimpleMapRunner");

        MessageProperties properties = new MessageProperties();
        Message message = converter.toMessage(taskData, properties);

        TaskData<Map<String, String>, Map<String, String>> fromData = (TaskData<Map<String, String>, Map<String, String>>) converter.fromMessage(message);
        Map<String, String> param = fromData.getTaskParam();
        Map<String, String> data = fromData.getResultData();
    }
}
