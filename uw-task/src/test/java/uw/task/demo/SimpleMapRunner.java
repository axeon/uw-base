package uw.task.demo;

import com.google.common.collect.Maps;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import uw.task.TaskData;
import uw.task.converter.TaskMessageConverter;

import java.util.Map;

/**
 * 
 * @since 2018-05-30
 */
public class SimpleMapRunner extends SimpleBaseMapRunner {
    public static void main(String[] args) {
        TaskMessageConverter converter = new TaskMessageConverter();

        SimpleMapRunner runner = new SimpleMapRunner();
        TaskData<Map<String, String>, Map<String, String>> taskData = new TaskData<>();
        taskData.setTaskParam(Maps.newHashMap());
        taskData.setResultData(Maps.newHashMap());
        taskData.setTaskClass("uw.task.demo.SimpleMapRunner");


        MessageProperties properties = new MessageProperties();
        Message message = converter.toMessage(taskData, properties);

        TaskData<Map<String, String>, Map<String, String>> fromData = (TaskData<Map<String, String>, Map<String, String>>) converter.fromMessage(message);
        Map<String, String> param = fromData.getTaskParam();
        Map<String, String> data = fromData.getResultData();
    }
}
