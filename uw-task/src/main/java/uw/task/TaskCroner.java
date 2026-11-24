package uw.task;

import uw.task.entity.TaskContact;
import uw.task.entity.TaskCronerConfig;
import uw.task.entity.TaskCronerLog;
import uw.task.exception.TaskDataException;
import uw.task.exception.TaskPartnerException;

/**
 * 定时任务基类，使用 cron 表达式定时执行；所有定时任务继承此类实现。
 *
 * @author axeon
 */
public abstract class TaskCroner {

    /**
     * 默认构造器。
     */
    public TaskCroner() {
    }

    /**
     * 运行任务，返回值记入执行日志的 resultData（定时任务无自动重试）。
     * 业务层面的异常请根据实际情况抛出以下受识别的异常（未捕获的异常框架按类型归入失败统计）：
     * 1. {@link TaskPartnerException} 合作方异常：状态 FAIL_PARTNER。
     * 2. {@link TaskDataException} 数据异常：状态 FAIL_DATA。
     * 3. 其它未捕获异常：程序异常，状态 FAIL_PROGRAM。
     *
     * @param taskCronerLog 本轮执行日志对象（含任务配置与计划执行时间）
     * @return 执行结果描述
     * @throws Exception 任务执行异常
     */
    public abstract String runTask(TaskCronerLog taskCronerLog) throws Exception;

    /**
     * 初始化默认配置（首次注册时上传到 task-center，center 不可达时作为本地降级配置）。
     *
     * @return 定时任务配置
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
