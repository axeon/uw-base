package uw.task.demo;

import java.util.Map;

import uw.task.TaskData;
import uw.task.TaskRunner;
import uw.task.entity.TaskContact;
import uw.task.entity.TaskRunnerConfig;

/**
 *
 * @since 2018-05-30
 */
public abstract class SimpleBaseMapRunner extends TaskRunner<Map<String, String>, Map<String, String>> {
    @Override
    public Map<String, String> runTask(TaskData<Map<String, String>, Map<String, String>> taskData) throws Exception {
        return null;
    }

    @Override
    public TaskRunnerConfig initConfig() {
        return null;
    }

    @Override
    public TaskContact initContact() {
        return null;
    }
}
