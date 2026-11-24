package uw.task;

import uw.task.entity.TaskContact;
import uw.task.entity.TaskRunnerConfig;
import uw.task.exception.TaskDataException;
import uw.task.exception.TaskPartnerException;

/**
 * 队列任务基类，所有队列任务继承此类实现。
 * <p>TP/RD 必须与 {@link TaskData} 的泛型参数完全一致，否则运行时出错。</p>
 *
 * @param <TP> taskParam参数
 * @param <RD> ResultData返回结果
 * @author axeon
 */
public abstract class TaskRunner<TP, RD> {

    /**
     * 默认构造器。
     */
    public TaskRunner() {
    }

    /**
     * 执行任务。
     * 业务层面的异常请根据实际情况抛出以下受识别的异常（未捕获的异常框架按类型归入失败统计）：
     * 1. {@link TaskPartnerException} 合作方异常：状态 FAIL_PARTNER，按 {@code retryTimesByPartner} 自动重试。
     * 2. {@link TaskDataException} 数据异常：状态 FAIL_DATA，不自动重试。
     * 3. 其它未捕获异常：程序异常，状态 FAIL_PROGRAM，按 {@code retryTimesByProgram} 自动重试（默认 0=不重试）。
     *
     * @param taskData 数据
     * @return 指定的返回对象
     * @throws Exception 异常
     */
    public abstract RD runTask(TaskData<TP, RD> taskData) throws Exception;

    /**
     * 初始化默认配置（首次注册时上传到 task-center，center 不可达时作为本地降级配置）。
     *
     * @return 队列任务配置
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
