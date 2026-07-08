package uw.task.util;

import uw.task.entity.TaskRunnerConfig;

/**
 * 限速 locker key 构造工具。
 */
public class TaskRateLimitHelper {

    /**
     * 构建 GLOBAL 限速的 locker key。
     * <p>限流 key 拼接规则：
     * <ul>
     *   <li>GLOBAL_TASK* 系列（TASK/TASK_TAG/TASK_HOST/TASK_TAG_HOST）：key 含 taskClass 前缀，
     *       即"按任务隔离"配额；</li>
     *   <li>GLOBAL_TAG / GLOBAL_HOST / GLOBAL_TAG_HOST：key 不含 taskClass，
     *       即"跨任务共享"配额——多个不同 task 只要 tag/host 相同即共享同一限流池。
     *       这是限流的常见场景（如多个任务都对接同一第三方接口，需共享 QPS 上限）。</li>
     * </ul>
     *
     * @param type          限速类型（RATE_LIMIT_GLOBAL_*）
     * @param taskClass     任务类名
     * @param rateLimitTag  限速 TAG
     * @param host          主机标识
     * @return locker key 字符串
     */
    public static String buildGlobalLocker(int type, String taskClass, String rateLimitTag, String host) {
        boolean taskScoped = type == TaskRunnerConfig.RATE_LIMIT_GLOBAL_TASK
                || type == TaskRunnerConfig.RATE_LIMIT_GLOBAL_TASK_TAG
                || type == TaskRunnerConfig.RATE_LIMIT_GLOBAL_TASK_HOST
                || type == TaskRunnerConfig.RATE_LIMIT_GLOBAL_TASK_TAG_HOST;
        StringBuilder locker = new StringBuilder(50);
        if (taskScoped) {
            locker.append(taskClass);
        }
        switch (type) {
            case TaskRunnerConfig.RATE_LIMIT_GLOBAL_TAG:
            case TaskRunnerConfig.RATE_LIMIT_GLOBAL_TASK_TAG:
                locker.append("$").append(rateLimitTag);
                break;
            case TaskRunnerConfig.RATE_LIMIT_GLOBAL_HOST:
            case TaskRunnerConfig.RATE_LIMIT_GLOBAL_TASK_HOST:
                locker.append("$@").append(host);
                break;
            case TaskRunnerConfig.RATE_LIMIT_GLOBAL_TASK:
                locker.append("$");
                break;
            case TaskRunnerConfig.RATE_LIMIT_GLOBAL_TAG_HOST:
            case TaskRunnerConfig.RATE_LIMIT_GLOBAL_TASK_TAG_HOST:
                locker.append("$").append(rateLimitTag).append("@").append(host);
                break;
            default:
                break;
        }
        return locker.toString();
    }
}
