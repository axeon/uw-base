package uw.task;

import uw.task.entity.TaskContact;
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
     * 业务层面的异常请根据实际情况手动Throw TaskException:
     * 目前支持的异常:
     * 1. TaskPartnerException 任务合作方异常，此异常会引发任务重试。
     * 2. TaskDataException 任务数据异常，此异常不会引发任务重试。
     * ！！！其它未捕获异常一律认为是程序异常，不会引发任务重试。
     *
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
     *
     * @return TaskContact联系人信息
     */
    public abstract TaskContact initContact();
}