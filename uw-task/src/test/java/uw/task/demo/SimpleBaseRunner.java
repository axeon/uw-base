package uw.task.demo;

import uw.task.TaskData;
import uw.task.TaskRunner;
import uw.task.entity.TaskContact;
import uw.task.entity.TaskRunnerConfig;

/**
 * 
 * @since 2018-05-30
 */
public abstract class SimpleBaseRunner extends TaskRunner<String, String> {

    @Override
    public String runTask(TaskData<String, String> taskData) throws Exception {
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
