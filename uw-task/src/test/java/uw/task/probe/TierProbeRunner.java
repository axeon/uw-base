package uw.task.probe;

import uw.task.TaskData;
import uw.task.TaskRunner;
import uw.task.entity.TaskContact;
import uw.task.entity.TaskRunnerConfig;

/**
 * dev 档位验证用的队列任务：原样回显参数，用于验证"仅定时任务档"下队列任务的本地执行。
 */
public class TierProbeRunner extends TaskRunner<String, String> {

    @Override
    public String runTask(TaskData<String, String> taskData) {
        return "echo:" + taskData.getTaskParam();
    }

    @Override
    public TaskRunnerConfig initConfig() {
        TaskRunnerConfig config = new TaskRunnerConfig();
        config.setTaskName("uw-task 档位验证队列任务");
        config.setLogLevel(TaskRunnerConfig.TASK_LOG_TYPE_NONE);
        return config;
    }

    @Override
    public TaskContact initContact() {
        return new TaskContact();
    }
}
