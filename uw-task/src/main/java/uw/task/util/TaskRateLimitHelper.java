package uw.task.util;

import uw.common.util.SystemClock;
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

    /**
     * 构建 LOCAL 限速的 locker key。
     * <ul>
     *   <li>{@code RATE_LIMIT_LOCAL}：空串（全进程共享配额）；</li>
     *   <li>{@code RATE_LIMIT_LOCAL_TASK}：taskClass（按任务隔离）；</li>
     *   <li>{@code RATE_LIMIT_LOCAL_TASK_TAG}：taskClass$rateLimitTag（按任务+TAG 隔离）。</li>
     * </ul>
     *
     * @param type         限速类型（RATE_LIMIT_LOCAL*）
     * @param taskClass    任务类名
     * @param rateLimitTag 限速 TAG
     * @return locker key 字符串
     */
    public static String buildLocalLocker(int type, String taskClass, String rateLimitTag) {
        if (type == TaskRunnerConfig.RATE_LIMIT_LOCAL_TASK) {
            return taskClass;
        } else if (type == TaskRunnerConfig.RATE_LIMIT_LOCAL_TASK_TAG) {
            return taskClass + "$" + rateLimitTag;
        } else {
            // RATE_LIMIT_LOCAL 或兜底：全进程共享
            return "";
        }
    }

    /**
     * GLOBAL 限速等待：在 waitSec 内循环 tryAcquire，命中(返回 0)即返回 true，否则 sleep 到下个窗口后重试，
     * 超时返回 false。统一 TaskRunner / TaskDelayer 的 GLOBAL 限速等待循环。
     * <p>rateLimitWait 为 int（秒），乘 1000L 转毫秒用 long 字面量避免 int 溢出。</p>
     *
     * @param limiter 全局限速器
     * @param locker  限流 key（由 {@link #buildGlobalLocker} 构造）
     * @param value   限速值
     * @param time    限速窗口（秒）
     * @param waitSec 最大等待秒数（rateLimitWait）
     * @return true=获得配额；false=等待超时未通过
     */
    public static boolean acquireGlobalWithWait(TaskGlobalRateLimiter limiter, String locker, int value, int time, int waitSec) {
        long end = SystemClock.now() + waitSec * 1000L;
        while (SystemClock.now() <= end) {
            long waitMillis = limiter.tryAcquire(locker, value, time, 1);
            if (waitMillis == 0) {
                return true;
            }
            try {
                Thread.sleep(waitMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
