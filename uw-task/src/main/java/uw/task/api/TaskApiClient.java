package uw.task.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;
import uw.log.es.LogClient;
import uw.task.conf.TaskProperties;
import uw.task.entity.*;

import java.util.Arrays;
import java.util.List;

/**
 * 对应服务器端的API接口实现。
 *
 * @author axeon
 */
public class TaskApiClient {

    private static final Logger log = LoggerFactory.getLogger( TaskApiClient.class );

    /**
     * Task配置文件
     */
    private TaskProperties taskProperties;

    /**
     * Rest模板类
     */
    private RestTemplate restTemplate;

    /**
     * 日志客户端。
     */
    private LogClient logClient;

    public TaskApiClient(final TaskProperties taskProperties, final RestTemplate restTemplate, final LogClient logClient) {
        this.taskProperties = taskProperties;
        this.restTemplate = restTemplate;
        this.logClient = logClient;
    }


    /**
     * 更新当前主机状态，返回主机IP地址。
     *
     * @return 主机IP地址
     */
    public HostReportResponse reportHostInfo(TaskHostStats taskHostStats) {
        HostReportResponse reportResponse = null;
        try {
            reportResponse = restTemplate.postForObject( taskProperties.getTaskCenterHost() + "/rpc/task/host/report", taskHostStats, HostReportResponse.class );
        } catch (Exception e) {
            log.error( "TaskApiClient.updateHostStatus()服务端主机状态报告异常: {}", e.getMessage() );
        }
        return reportResponse;
    }

    /**
     * 初始化CronerConfig。
     *
     * @param config
     */
    public TaskCronerConfig initTaskCronerConfig(TaskCronerConfig config) {
        try {
            config = restTemplate.postForObject( taskProperties.getTaskCenterHost() + "/rpc/task/croner/update", config, TaskCronerConfig.class );
        } catch (Exception e) {
            log.error( "TaskApiClient.initTaskCronerConfig()上传定时任务配置到服务端异常: {}", e.getMessage() );
        }
        return config;
    }

    /**
     * 初始化RunnerConfig
     *
     * @param config
     */
    public TaskRunnerConfig initTaskRunnerConfig(TaskRunnerConfig config) {
        try {
            config = restTemplate.postForObject( taskProperties.getTaskCenterHost() + "/rpc/task/runner/update", config, TaskRunnerConfig.class );
        } catch (Exception e) {
            log.error( "TaskApiClient.initTaskRunnerConfig()上传队列任务配置到服务端异常: {}", e.getMessage() );
        }
        return config;

    }

    /**
     * 初始化联系人信息。
     *
     * @param contact
     */
    public void initTaskContact(TaskContact contact) {
        try {
            restTemplate.postForLocation( taskProperties.getTaskCenterHost() + "/rpc/task/contact/update", contact );
        } catch (Exception e) {
            log.error( "TaskApiClient.initTaskContact()上传联系人信息到服务端异常: {}", e.getMessage() );
        }
    }

    /**
     * 根据包名前缀获得TaskRunner配置列表。
     *
     * @param lastUpdateTime 最后更新时间
     * @return
     */
    public List<TaskRunnerConfig> getTaskRunnerConfigList(String runTarget, String taskProject, long lastUpdateTime) {
        List<TaskRunnerConfig> list = null;
        try {
            TaskRunnerConfig[] data = restTemplate.getForObject( taskProperties.getTaskCenterHost() + "/rpc/task/runner/list?runTarget={runTarget}&taskProject={taskProject" +
                    "}&lastUpdateTime={lastUpdateTime}", TaskRunnerConfig[].class, runTarget, taskProject, lastUpdateTime );
            list = Arrays.asList( data );
        } catch (Exception e) {
            log.error( "TaskApiClient.getTaskRunnerConfigList()服务端主机状态更新异常: {}", e.getMessage() );
        }
        return list;
    }

    /**
     * 根据包名前缀获得TaskCroner配置列表。
     *
     * @param taskProject
     * @param lastUpdateTime 最后更新时间
     * @return
     */
    public List<TaskCronerConfig> getTaskCronerConfigList(String runTarget, String taskProject, long lastUpdateTime) {
        List<TaskCronerConfig> list = null;
        try {
            TaskCronerConfig[] data = restTemplate.getForObject( taskProperties.getTaskCenterHost() + "/rpc/task/croner/list?runTarget={runTarget}&taskProject={taskProject" +
                    "}&lastUpdateTime={lastUpdateTime}", TaskCronerConfig[].class, runTarget, taskProject, lastUpdateTime );
            list = Arrays.asList( data );
        } catch (Exception e) {
            log.error( "TaskApiClient.getTaskCronerConfigList()服务端主机状态更新异常: {}", e.getMessage() );
        }

        return list;
    }

    /**
     * 发送Runner任务日志。
     *
     * @param log
     */
    public void sendTaskRunnerLog(TaskRunnerLog log) {
        logClient.log( log );
    }

    /**
     * 发送CronLog日志
     *
     * @param taskId        配置Id,方便更新下一次执行时间
     * @param taskCronerLog 日志对象
     */
    public void sendTaskCronerLog(long taskId, TaskCronerLog taskCronerLog) {
        logClient.log( taskCronerLog );
        //更新下次执行时间。
        long nextDate = taskCronerLog.getNextDate().getTime();
        try {
            restTemplate.put( taskProperties.getTaskCenterHost() + "/rpc/task/croner/tick?id={id}&nextDate={nextDate}", null, taskId, nextDate );
        } catch (Throwable e) {
            log.error( "TaskApiClient.cornerTick()服务端主机状态更新异常: {}", e.getMessage() );
        }
    }
}
