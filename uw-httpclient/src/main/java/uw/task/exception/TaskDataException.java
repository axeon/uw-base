package uw.task.exception;

/**
 * 数据异常，用于抛出各种数据错误的信息。
 * <p>
 * 作为任务执行过程中数据相关错误（解析失败、格式不符、约束冲突等）的统一运行时异常基类。
 * <p>
 * <b>关于本类的存在</b>：uw-httpclient 的异常体系
 * （{@code uw.httpclient.exception.DataMapperException}、{@code HttpRequestException}）
 * 分别继承自 {@code TaskDataException} 与 {@code TaskPartnerException}，
 * 以便在 uw-task 任务调度框架内被统一识别和分类处理。但 uw-httpclient 作为底层基础库
 * <b>不希望强依赖 uw-task</b>（避免循环依赖与体积膨胀），因此在本地保留了同包名
 * （{@code uw.task.exception}）的桥接类。当本库运行在 uw-task 环境时，类加载器会优先加载
 * uw-task 提供的同名类，异常分类语义自然生效；独立运行时则回退到本类，仍可作为普通
 * RuntimeException 正常使用。
 *
 * @author axeon
 */
public class TaskDataException extends RuntimeException {

    /**
     * 构造一个不带消息和原因的数据异常。
     */
    public TaskDataException() {
        super();
    }

    /**
     * 构造一个带消息的数据异常。
     *
     * @param msg 异常消息。
     */
    public TaskDataException(String msg) {
        super(msg);
    }

    /**
     * 构造一个带原因的数据异常。
     *
     * @param nestedThrowable 原始异常。
     */
    public TaskDataException(Throwable nestedThrowable) {
        super(nestedThrowable);
    }

    /**
     * 构造一个带消息和原因的数据异常。
     *
     * @param msg             异常消息。
     * @param nestedThrowable 原始异常。
     */
    public TaskDataException(String msg, Throwable nestedThrowable) {
        super(msg, nestedThrowable);
    }

}