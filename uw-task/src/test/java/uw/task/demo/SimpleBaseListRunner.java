package uw.task.demo;

import java.util.List;

import uw.task.TaskData;
import uw.task.TaskRunner;
import uw.task.entity.TaskContact;
import uw.task.entity.TaskRunnerConfig;

/**
 *
 * @since 2018-05-30
 */
public abstract class SimpleBaseListRunner extends TaskRunner<List<String>, List<String>> {
    @Override
    public List<String> runTask(TaskData<List<String>, List<String>> taskData) throws Exception {
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
