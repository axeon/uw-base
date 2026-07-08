package uw.task;

import uw.task.container.TaskDelayerContainer;
import uw.task.entity.TaskDelayerConfig;
import uw.task.entity.TaskContact;

/**
 * 延迟任务执行器，uw-task 与 {@link TaskRunner}/{@link TaskCroner} 对等的第三种任务类型。
 *
 * <p>实现类加 {@code @Component}，由 {@link TaskDelayerContainer} 扫描注册；
 * 任务到点被 poll 取出后，容器调用 {@link #run(TaskData)} 在 worker 线程内执行。
 * 三方法与 TaskRunner 对齐：{@link #initConfig()} 产出默认配置，{@link #initContact()} 产出报警联系人，
 * {@link #run(TaskData)} 执行业务逻辑。</p>
 *
 * <p>载体复用 {@link TaskData}（与 TaskRunner 一致）：taskParam/taskResult 通过泛型 TP/RD 指定具体类型，
 * taskDelay 为延迟毫秒数（zset score = queueDate + taskDelay），id 为唯一序列号（zset member 唯一性）。</p>
 *
 * <p>业务异常请按需抛出 {@link uw.task.exception.TaskPartnerException} /
 * {@link uw.task.exception.TaskDataException}（见"任务内异常处理"），框架据此映射状态并决定重试。</p>
 *
 * @param <TP> taskParam 参数类型
 * @param <RD> resultData 返回结果类型
 * @author axeon
 */
public abstract class TaskDelayer<TP, RD> {

    /**
     * 初始化延迟任务配置（首次注册时上传到 task-center，之后以服务端动态配置为准）。
     *
     * @return 延迟任务配置
     */
    public abstract TaskDelayerConfig initConfig();

    /**
     * 初始化联系人信息（首次注册时上传，用于报警通知）。
     *
     * @return 联系人信息
     */
    public abstract TaskContact initContact();

    /**
     * 运行延迟任务。
     *
     * @param task 任务数据（含 taskParam、taskDelay、ranTimes 等）
     * @throws Exception 业务异常（TaskPartnerException/TaskDataException 等）
     */
    public abstract void run(TaskData<TP, RD> task) throws Exception;
}
