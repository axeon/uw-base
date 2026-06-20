package uw.task.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import uw.common.response.ResponseData;
import uw.log.es.LogClient;
import uw.task.conf.TaskProperties;
import uw.task.entity.*;

import java.util.Date;
import java.util.List;

/**
 * 任务中心（task-center）服务端 API 客户端。
 *
 * <p>封装与 uw-task-center 的全部交互：主机状态上报、任务配置初始化与拉取、
 * 任务日志/统计写入、定时任务心跳等。HTTP 调用委托给 {@link RestClient}（带鉴权），
 * 日志写入委托给 {@link LogClient}（ES）。所有方法对网络异常做了兜底：
 * 调用失败时记录日志并返回 {@code null}，由上层据返回值判断是否进入 fail-fast。</p>
 *
 * @author axeon
 */
public class TaskApiClient {

    /**
     * 日志器。
     */
    private static final Logger log = LoggerFactory.getLogger(TaskApiClient.class);

    /**
     * 任务配置（提供 task-center 地址等）。
     */
    private final TaskProperties taskProperties;

    /**
     * 带鉴权的 HTTP 客户端，用于调用 task-center 的 RPC 接口。
     */
    private final RestClient authRestClient;

    /**
     * 日志客户端，用于写入任务执行日志到 ES。
     */
    private final LogClient logClient;

    /**
     * @param taskProperties 任务配置
     * @param authRestClient 带鉴权的 HTTP 客户端
     * @param logClient      日志客户端
     */
    public TaskApiClient(final TaskProperties taskProperties, final RestClient authRestClient, final LogClient logClient) {
        this.taskProperties = taskProperties;
        this.authRestClient = authRestClient;
        this.logClient = logClient;
    }


    /**
     * 上报主机状态（含 JVM、线程、任务统计与队列信息）。
     *
     * @param taskHostStats 主机状态数据
     * @return 服务端响应（含主机记录 id）；网络失败返回 null
     */
    public ResponseData<HostReportResponse> reportHostInfo(TaskHostStats taskHostStats) {
        try {
            ResponseData<HostReportResponse> result = authRestClient.post()
                    .uri(taskProperties.getTaskCenterHost() + "/rpc/task/host/report")
                    .body(taskHostStats)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ResponseData<HostReportResponse>>() {});
            return warnOnFailure(result, "reportHostInfo");
        } catch (Exception e) {
            log.error("TaskApiClient.reportHostInfo()服务端主机状态报告异常: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 初始化定时任务配置到服务端（首次注册时上传默认配置，服务端按三元组幂等去重）。
     *
     * @param config 定时任务配置
     * @return 服务端响应（含已分配的 id）；网络失败返回 null
     */
    public ResponseData<TaskCronerConfig> initTaskCronerConfig(TaskCronerConfig config) {
        try {
            ResponseData<TaskCronerConfig> result = authRestClient.post()
                    .uri(taskProperties.getTaskCenterHost() + "/rpc/task/croner/init")
                    .body(config)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ResponseData<TaskCronerConfig>>() {});
            return warnOnFailure(result, "initTaskCronerConfig");
        } catch (Exception e) {
            log.error("TaskApiClient.initTaskCronerConfig()上传定时任务配置到服务端异常: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 初始化队列任务配置到服务端（首次注册时上传默认配置，服务端按三元组幂等去重）。
     *
     * @param config 队列任务配置
     * @return 服务端响应（含已分配的 id）；网络失败返回 null
     */
    public ResponseData<TaskRunnerConfig> initTaskRunnerConfig(TaskRunnerConfig config) {
        try {
            ResponseData<TaskRunnerConfig> result = authRestClient.post()
                    .uri(taskProperties.getTaskCenterHost() + "/rpc/task/runner/init")
                    .body(config)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ResponseData<TaskRunnerConfig>>() {});
            return warnOnFailure(result, "initTaskRunnerConfig");
        } catch (Exception e) {
            log.error("TaskApiClient.initTaskRunnerConfig()上传队列任务配置到服务端异常: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 上传任务报警联系人信息到服务端。
     *
     * @param contact 联系人信息
     * @return 服务端响应；网络失败返回 null
     */
    public ResponseData<Void> initTaskContact(TaskContact contact) {
        try {
            ResponseData<Void> result = authRestClient.post()
                    .uri(taskProperties.getTaskCenterHost() + "/rpc/task/contact/init")
                    .body(contact)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ResponseData<Void>>() {});
            return warnOnFailure(result, "initTaskContact");
        } catch (Exception e) {
            log.error("TaskApiClient.initTaskContact()上传联系人信息到服务端异常: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 拉取指定 runTarget 与 taskProject 下、lastUpdateTime 之后变更的队列任务配置列表。
     *
     * @param runTarget     运行目标
     * @param taskProject   任务项目（包名前缀）
     * @param lastUpdateTime 上次更新时间戳（毫秒），仅拉取此时间之后的增量
     * @return 服务端响应（配置列表）；网络失败返回 null
     */
    public ResponseData<List<TaskRunnerConfig>> getTaskRunnerConfigList(String runTarget, String taskProject, long lastUpdateTime) {
        try {
            ResponseData<List<TaskRunnerConfig>> result = authRestClient.get()
                    .uri(taskProperties.getTaskCenterHost() + "/rpc/task/runner/list?runTarget={runTarget}&taskProject={taskProject}&lastUpdateTime={lastUpdateTime}",
                            runTarget, taskProject, lastUpdateTime)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ResponseData<List<TaskRunnerConfig>>>() {});
            return warnOnFailure(result, "getTaskRunnerConfigList");
        } catch (Exception e) {
            log.error("TaskApiClient.getTaskRunnerConfigList()服务端主机状态更新异常: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 拉取指定 runTarget 与 taskProject 下、lastUpdateTime 之后变更的定时任务配置列表。
     *
     * @param runTarget     运行目标
     * @param taskProject   任务项目（包名前缀）
     * @param lastUpdateTime 上次更新时间戳（毫秒），仅拉取此时间之后的增量
     * @return 服务端响应（配置列表）；网络失败返回 null
     */
    public ResponseData<List<TaskCronerConfig>> getTaskCronerConfigList(String runTarget, String taskProject, long lastUpdateTime) {
        try {
            ResponseData<List<TaskCronerConfig>> result = authRestClient.get()
                    .uri(taskProperties.getTaskCenterHost() + "/rpc/task/croner/list?runTarget={runTarget}&taskProject={taskProject}&lastUpdateTime={lastUpdateTime}",
                            runTarget, taskProject, lastUpdateTime)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ResponseData<List<TaskCronerConfig>>>() {});
            return warnOnFailure(result, "getTaskCronerConfigList");
        } catch (Exception e) {
            log.error("TaskApiClient.getTaskCronerConfigList()服务端主机状态更新异常: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 异步写入队列任务执行日志到 ES。
     *
     * @param log 任务执行日志
     */
    public void sendTaskRunnerLog(TaskRunnerLog log) {
        logClient.log(log);
    }

    /**
     * 写入定时任务执行日志，并在存在下次执行时间时向服务端发送心跳（更新 next_run_date）。
     *
     * @param taskId        任务配置 id
     * @param taskCronerLog 任务执行日志
     */
    public void sendTaskCronerLog(long taskId, TaskCronerLog taskCronerLog) {
        logClient.log(taskCronerLog);
        Date nextDate = taskCronerLog.getNextDate();
        if (nextDate == null) {
            return;
        }
        try {
            authRestClient.put()
                    .uri(taskProperties.getTaskCenterHost() + "/rpc/task/croner/tick?id={id}&nextDate={nextDate}", taskId, nextDate.getTime())
                    .retrieve()
                    .toBodilessEntity();
        } catch (Throwable e) {
            log.error("TaskApiClient.sendTaskCronerLog()服务端主机状态更新异常: {}", e.getMessage());
        }
    }

    /**
     * 当服务端返回非成功状态时记录 warn 日志，便于排查；原样返回响应供上层判断。
     *
     * @param response 服务端响应
     * @param apiName  调用方方法名（仅用于日志）
     * @param <T>      响应数据类型
     * @return 原响应对象
     */
    private <T> ResponseData<T> warnOnFailure(ResponseData<T> response, String apiName) {
        if (response != null && response.isNotSuccess()) {
            log.warn("TaskApiClient.{}()服务端返回非成功状态: state={}, code={}, msg={}", apiName, response.getState(), response.getCode(), response.getMsg());
        }
        return response;
    }
}
