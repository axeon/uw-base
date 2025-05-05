package uw.task;

import uw.task.entity.TaskContact;
import uw.task.entity.TaskCronerConfig;
import uw.task.entity.TaskCronerLog;

/**
 * 定时任务类。 使用Cron表达式来运行定时任务。
 *
 * @author axeon
 */
public abstract class TaskCroner {

    /**
     * 运行任务。
     * 业务层面的异常请根据实际情况手动Throw TaskException:
     * 目前支持的异常:
     * 1. TaskPartnerException 任务合作方异常，此异常会引发任务重试。
     * 2. TaskDataException 任务数据异常，此异常不会引发任务重试。
     * ！！！其它未捕获异常一律认为是程序异常，不会引发任务重试。
     *
     */
    public abstract String runTask(TaskCronerLog taskCronerLog) throws Exception;

    /**
     * 初始化配置信息。
     */
    public abstract TaskCronerConfig initConfig();

    /**
     * 初始化联系人信息。
     *
     * @return
     */
    public abstract TaskContact initContact();

}
