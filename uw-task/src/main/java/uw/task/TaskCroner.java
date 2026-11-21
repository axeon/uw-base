package uw.task;

import uw.task.entity.TaskContact;
import uw.task.exception.TaskDataException;
import uw.task.exception.TaskPartnerException;
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
     * 业务层面的异常请根据实际情况手动抛出以下受识别的异常：
     * 1. {@link TaskPartnerException} 任务合作方异常，此异常会引发任务重试。
     * 2. {@link TaskDataException} 任务数据异常，此异常不会引发任务重试。
     * ！！！其它未捕获异常一律认为是程序异常，不会引发任务重试。
     *
     */
    public abstract String runTask(TaskCronerLog taskCronerLog) throws Exception;

    /**
     * 初始化配置信息。
     */
    public abstract TaskCronerConfig initConfig();

    /**
     * 初始化联系人信息（首次注册时上传到服务端，用于报警通知）。
     * <p>默认返回空联系人（无报警需求的任务无需覆写）；空联系人不影响本地降级启动，
     * 仅意味着报警通知无接收方。</p>
     *
     * @return 联系人信息
     */
    public TaskContact initContact() {
        return new TaskContact();
    }

}
