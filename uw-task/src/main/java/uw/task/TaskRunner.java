package uw.task;

import uw.task.entity.TaskContact;
import uw.task.exception.TaskDataException;
import uw.task.exception.TaskPartnerException;
import uw.task.entity.TaskRunnerConfig;

/**
 * 任务执行器。 所有的任务都通过实现此接口实现.
 * TaskParam和ResultData可通过泛型参数制定具体类型。
 * TP,TD应和TaskData的泛型参数完全一致，否则会导致运行时出错。
 *
 * @param <TP> taskParam参数
 * @param <RD> ResultData返回结果
 * @author axeon
 */
public abstract class TaskRunner<TP, RD> {

    /**
     * 执行任务。
     * 业务层面的异常请根据实际情况手动抛出以下受识别的异常：
     * 1. {@link TaskPartnerException} 任务合作方异常，此异常会引发任务重试。
     * 2. {@link TaskDataException} 任务数据异常，此异常不会引发任务重试。
     * ！！！其它未捕获异常一律认为是程序异常，不会引发任务重试。
     *
     * @param taskData 数据
     * @return 指定的返回对象
     * @throws Exception 异常
     */
    public abstract RD runTask(TaskData<TP, RD> taskData) throws Exception;

    /**
     * 初始化配置信息
     *
     * @return TaskRunnerConfig配置
     */
    public abstract TaskRunnerConfig initConfig();

    /**
     * 初始化联系人信息
     * <p>默认返回空联系人（无报警需求的任务无需覆写）；空联系人不影响本地降级启动，
     * 仅意味着报警通知无接收方。</p>
     *
     * @return TaskContact联系人信息
     */
    public TaskContact initContact() {
        return new TaskContact();
    }
}