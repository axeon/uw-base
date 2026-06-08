package uw.task.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uw.common.dto.ResponseData;
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

    /**
     * Task配置文件
     */
    private final TaskProperties taskProperties;

    /**
     * Rest模板类
     */
    private final RestTemplate authRestTemplate;

    /**
     * 日志客户端。
     */
    private final LogClient logClient;

    public TaskApiClient(final TaskProperties taskProperties, final RestTemplate authRestTemplate, final LogClient logClient) {
        this.taskProperties = taskProperties;
        this.authRestTemplate = authRestTemplate;
        this.logClient = logClient;
    }


    /**
     * 更新当前主机状态，返回主机IP地址。
     *
     * @return 主机IP地址
     */
    public ResponseData<HostReportResponse> reportHostInfo(TaskHostStats taskHostStats) {
        try {
            ResponseEntity<ResponseData<HostReportResponse>> responseEntity = authRestTemplate.exchange(
                    taskProperties.getTaskCenterHost() + "/rpc/task/host/report",
                    HttpMethod.POST,
                    new HttpEntity<>(taskHostStats),
                    new ParameterizedTypeReference<ResponseData<HostReportResponse>>() {});
            return warnOnFailure(responseEntity.getBody(), "reportHostInfo");
        } catch (Exception e) {
            log.error("TaskApiClient.reportHostInfo()服务端主机状态报告异常: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 初始化CronerConfig。
     *
     * @param config
     */
    public ResponseData<TaskCronerConfig> initTaskCronerConfig(TaskCronerConfig config) {
        try {
            ResponseEntity<ResponseData<TaskCronerConfig>> responseEntity = authRestTemplate.exchange(
                    taskProperties.getTaskCenterHost() + "/rpc/task/croner/init",
                    HttpMethod.POST,
                    new HttpEntity<>(config),
                    new ParameterizedTypeReference<ResponseData<TaskCronerConfig>>() {});
            return warnOnFailure(responseEntity.getBody(), "initTaskCronerConfig");
        } catch (Exception e) {
            log.error("TaskApiClient.initTaskCronerConfig()上传定时任务配置到服务端异常: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 初始化RunnerConfig
     *
     * @param config
     */
    public ResponseData<TaskRunnerConfig> initTaskRunnerConfig(TaskRunnerConfig config) {
        try {
            ResponseEntity<ResponseData<TaskRunnerConfig>> responseEntity = authRestTemplate.exchange(
                    taskProperties.getTaskCenterHost() + "/rpc/task/runner/init",
                    HttpMethod.POST,
                    new HttpEntity<>(config),
                    new ParameterizedTypeReference<ResponseData<TaskRunnerConfig>>() {});
            return warnOnFailure(responseEntity.getBody(), "initTaskRunnerConfig");
        } catch (Exception e) {
            log.error("TaskApiClient.initTaskRunnerConfig()上传队列任务配置到服务端异常: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 初始化联系人信息。
     *
     * @param contact
     */
    public ResponseData<Void> initTaskContact(TaskContact contact) {
        try {
            ResponseEntity<ResponseData<Void>> responseEntity = authRestTemplate.exchange(
                    taskProperties.getTaskCenterHost() + "/rpc/task/contact/init",
                    HttpMethod.POST,
                    new HttpEntity<>(contact),
                    new ParameterizedTypeReference<ResponseData<Void>>() {});
            return warnOnFailure(responseEntity.getBody(), "initTaskContact");
        } catch (Exception e) {
            log.error("TaskApiClient.initTaskContact()上传联系人信息到服务端异常: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 根据包名前缀获取TaskRunner配置列表。
     *
     * @param lastUpdateTime 最后更新时间
     * @return
     */
    public ResponseData<List<TaskRunnerConfig>> getTaskRunnerConfigList(String runTarget, String taskProject, long lastUpdateTime) {
        try {
            ResponseEntity<ResponseData<List<TaskRunnerConfig>>> responseEntity = authRestTemplate.exchange(
                    taskProperties.getTaskCenterHost() + "/rpc/task/runner/list?runTarget={runTarget}&taskProject={taskProject}&lastUpdateTime={lastUpdateTime}",
                    HttpMethod.GET, null,
                    new ParameterizedTypeReference<ResponseData<List<TaskRunnerConfig>>>() {},
                    runTarget, taskProject, lastUpdateTime);
            return warnOnFailure(responseEntity.getBody(), "getTaskRunnerConfigList");
        } catch (Exception e) {
            log.error("TaskApiClient.getTaskRunnerConfigList()服务端主机状态更新异常: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 根据包名前缀获取TaskCroner配置列表。
     *
     * @param taskProject
     * @param lastUpdateTime 最后更新时间
     * @return
     */
    public ResponseData<List<TaskCronerConfig>> getTaskCronerConfigList(String runTarget, String taskProject, long lastUpdateTime) {
        try {
            ResponseEntity<ResponseData<List<TaskCronerConfig>>> responseEntity = authRestTemplate.exchange(
                    taskProperties.getTaskCenterHost() + "/rpc/task/croner/list?runTarget={runTarget}&taskProject={taskProject}&lastUpdateTime={lastUpdateTime}",
                    HttpMethod.GET, null,
                    new ParameterizedTypeReference<ResponseData<List<TaskCronerConfig>>>() {},
                    runTarget, taskProject, lastUpdateTime);
            return warnOnFailure(responseEntity.getBody(), "getTaskCronerConfigList");
        } catch (Exception e) {
            log.error("TaskApiClient.getTaskCronerConfigList()服务端主机状态更新异常: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 发送Runner任务日志。
     *
     * @param log
     */
    public void sendTaskRunnerLog(TaskRunnerLog log) {
        logClient.log(log);
    }

    /**
     * 发送CronLog日志
     *
     * @param taskId        配置Id,方便更新下一次执行时间
     * @param taskCronerLog 日志对象
     */
    public void sendTaskCronerLog(long taskId, TaskCronerLog taskCronerLog) {
        logClient.log(taskCronerLog);
        //更新下次执行时间。
        Date nextDate = taskCronerLog.getNextDate();
        if (nextDate == null) {
            return;
        }
        try {
            authRestTemplate.put(taskProperties.getTaskCenterHost() + "/rpc/task/croner/tick?id={id}&nextDate={nextDate}", null, taskId, nextDate.getTime());
        } catch (Throwable e) {
            log.error("TaskApiClient.sendTaskCronerLog()服务端主机状态更新异常: {}", e.getMessage());
        }
    }

    /**
     * 对非成功的ResponseData打印warn日志。
     *
     * @param response ResponseData响应
     * @param apiName  接口名称
     * @return 原始ResponseData
     */
    private <T> ResponseData<T> warnOnFailure(ResponseData<T> response, String apiName) {
        if (response != null && response.isNotSuccess()) {
            log.warn("TaskApiClient.{}()服务端返回非成功状态: state={}, code={}, msg={}", apiName, response.getState(), response.getCode(), response.getMsg());
        }
        return response;
    }
}
