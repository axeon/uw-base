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
 * 对应服务器端的API接口实现。
 *
 * @author axeon
 */
public class TaskApiClient {

    private static final Logger log = LoggerFactory.getLogger(TaskApiClient.class);

    private final TaskProperties taskProperties;
    private final RestClient authRestClient;
    private final LogClient logClient;

    public TaskApiClient(final TaskProperties taskProperties, final RestClient authRestClient, final LogClient logClient) {
        this.taskProperties = taskProperties;
        this.authRestClient = authRestClient;
        this.logClient = logClient;
    }


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

    public void sendTaskRunnerLog(TaskRunnerLog log) {
        logClient.log(log);
    }

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

    private <T> ResponseData<T> warnOnFailure(ResponseData<T> response, String apiName) {
        if (response != null && response.isNotSuccess()) {
            log.warn("TaskApiClient.{}()服务端返回非成功状态: state={}, code={}, msg={}", apiName, response.getState(), response.getCode(), response.getMsg());
        }
        return response;
    }
}
