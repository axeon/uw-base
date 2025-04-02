package uw.task.conf;

import org.springframework.context.ApplicationContext;
import uw.task.TaskCroner;
import uw.task.TaskData;
import uw.task.TaskRunner;
import uw.task.entity.TaskCronerConfig;
import uw.task.entity.TaskRunnerConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Task元信息管理器
 *
 * @author axeon
 */
public class TaskMetaInfoManager {

    /**
     * Runner任务实例缓存。
     */
    private final Map<String, TaskRunner> runnerInstanceMap = new HashMap<>();

    /**
     * Cron任务实例缓存。
     */
    private final Map<String, TaskCroner> cronerInstanceMap = new HashMap<>();

    /**
     * Runner任务配置缓存
     */
    private final ConcurrentHashMap<String, TaskRunnerConfig> runnerConfigMap = new ConcurrentHashMap<>();

    /**
     * Cron任务配置缓存。
     */
    private final ConcurrentHashMap<String, TaskCronerConfig> cronerConfigMap = new ConcurrentHashMap<>();

    /**
     * 任务配置。
     */
    private final TaskProperties taskProperties;

    /**
     * 应用上下文。
     */
    private final ApplicationContext context;

    public TaskMetaInfoManager(ApplicationContext context, TaskProperties taskProperties) {
        this.context = context;
        this.taskProperties = taskProperties;
    }

    /**
     * 设置队列任务配置。
     *
     * @param runnerConfigKey
     * @param config
     */
    public void setRunnerConfig(String runnerConfigKey, TaskRunnerConfig config) {
        runnerConfigMap.put( runnerConfigKey, config );
    }

    /**
     * 移除队列任务配置。
     *
     * @param runnerConfigKey
     */
    public void removeRunnerConfig(String runnerConfigKey) {
        runnerConfigMap.remove( runnerConfigKey );
    }

    /**
     * 设置定时任务配置。
     *
     * @param cronerConfigKey
     * @param config
     */
    public void setCronerConfig(String cronerConfigKey, TaskCronerConfig config) {
        cronerConfigMap.put( cronerConfigKey, config );
    }

    /**
     * 获得队列任务实例Map。
     *
     * @return
     */
    public Map<String, TaskRunner> getRunnerInstanceMap() {
        return runnerInstanceMap;
    }

    /**
     * 获得定时任务实例Map。
     *
     * @return
     */
    public Map<String, TaskCroner> getCronerInstanceMap() {
        return cronerInstanceMap;
    }

    /**
     * 获得队列任务实例。
     *
     * @param runnerCls
     * @return
     */
    public TaskRunner getRunnerInstance(String runnerCls) {
        return runnerInstanceMap.get( runnerCls );
    }

    /**
     * 获得定时任务实例。
     *
     * @param cronerCls
     * @return
     */
    public TaskCroner getCronerInstance(String cronerCls) {
        return cronerInstanceMap.get( cronerCls );
    }

    /**
     * 获得队列任务配置Map。
     *
     * @return
     */
    public ConcurrentHashMap<String, TaskRunnerConfig> getRunnerConfigMap() {
        return runnerConfigMap;
    }

    /**
     * 获得定时任务配置Map。
     *
     * @return
     */
    public ConcurrentHashMap<String, TaskCronerConfig> getCronerConfigMap() {
        return cronerConfigMap;
    }

    /**
     * 初始化任务实例Map。
     */
    public void init() {
        // 设置当前主机上所有的TaskCroner
        Map<String, TaskCroner> cronerMap = context.getBeansOfType( TaskCroner.class );
        for (Map.Entry<String, TaskCroner> kv : cronerMap.entrySet()) {
            // 拿到任务类名
            TaskCroner taskCroner = kv.getValue();
            String taskClass = taskCroner.getClass().getName();
            cronerInstanceMap.put( taskClass, taskCroner );
        }
        // 设置当前主机上所有的TaskCroner
        Map<String, TaskRunner> runnerMap = context.getBeansOfType( TaskRunner.class );
        for (Map.Entry<String, TaskRunner> kv : runnerMap.entrySet()) {
            // 拿到任务类名
            TaskRunner taskRunner = kv.getValue();
            String taskClass = taskRunner.getClass().getName();
            runnerInstanceMap.put( taskClass, taskRunner );
        }
    }

    /**
     * 检查一个runner是否可以在本地运行。
     *
     * @param taskData
     * @return
     */
    public boolean checkRunnerRunLocal(TaskData<?, ?> taskData) {
        return runnerInstanceMap.containsKey( taskData.getTaskClass() );
    }

    /**
     * 客户端发送任务到队列时，通过此方法获得合适的队列。
     * 根据服务器端Queue列表，匹配合适的key。
     * 队列格式为：taskClass#taskTag$runTarget
     *
     * @return
     */
    public String getFitQueue(TaskData<?, ?> data) {
        String fitName = getRunnerConfigKeyByData( data );
        TaskRunnerConfig config = runnerConfigMap.get( fitName );
        if (config == null) {
            // 此时再检测没有taskTag匹配的情况。
            int pos1 = fitName.indexOf( '#' );
            int pos2 = fitName.lastIndexOf( '$' );
            String test = fitName.substring( 0, pos1 + 1 ) + fitName.substring( pos2 );
            config = runnerConfigMap.get( test );
            if (config != null) {
                fitName = test;
            }
        }
        if (config != null) {
            String queueName = switch (config.getQueueType()) {
                case TaskRunnerConfig.TYPE_QUEUE_TASK -> fitName;
                case TaskRunnerConfig.TYPE_QUEUE_GROUP_PRIORITY -> getGroupQueueName( config.getTaskClass(), "priority", config.getRunTarget() );
                case TaskRunnerConfig.TYPE_QUEUE_GROUP -> getGroupQueueName( config.getTaskClass(), "default", config.getRunTarget() );
                case TaskRunnerConfig.TYPE_QUEUE_PROJECT_PRIORITY -> taskProperties.getTaskProject() + "@priority$" + config.getRunTarget();
                default -> taskProperties.getTaskProject() + "$" + config.getRunTarget();
            };
            // 只有开启了延时队列并且发送队列异步进行才会返回TTL队列名称
            return config.getDelayType() == TaskRunnerConfig.TYPE_DELAY_ON && data.getRunType() == TaskData.RUN_TYPE_GLOBAL && data.getTaskDelay() > 0 ?
                    getTTLQueueName( queueName ) : queueName;
        } else {
            throw new RuntimeException( "找不到合适的任务配置: taskClass = " + data.getTaskClass() );
        }
    }

    /**
     * 任务执行端执行任务时，调用此方法来获得合适的队列。
     *
     * @param data
     * @return
     */
    public TaskRunnerConfig getRunnerConfigByData(TaskData<?, ?> data) {
        TaskRunnerConfig config = null;
        String fitName = getRunnerConfigKeyByData( data );
        config = runnerConfigMap.get( fitName );
        if (config != null) {
            return config;
        }
        // 此时再检测没有taskTag匹配的情况。
        int pos1 = fitName.indexOf( '#' );
        int pos2 = fitName.lastIndexOf( '$' );
        String test = fitName.substring( 0, pos1 + 1 ) + fitName.substring( pos2 );
        config = runnerConfigMap.get( test );
        if (config != null) {
            return config;
        }
        throw new RuntimeException( "找不到任务配置: taskClass = " + data.getTaskClass() );
    }


    /**
     * 获得croner配置键。 使用taskClass#Id$target来配置
     *
     * @return
     */
    public String getCronerConfigKey(TaskCronerConfig config) {
        StringBuilder sb = new StringBuilder( 168 );
        sb.append( config.getTaskClass() ).append( "#" );
        if (config.getTaskParam() != null && !config.getTaskParam().isEmpty()) {
            sb.append( config.getId() );
        }
        sb.append( "$" );
        if (config.getRunTarget() != null && !config.getRunTarget().isEmpty()) {
            sb.append( config.getRunTarget() );
        }
        return sb.toString();
    }

    /**
     * 获得Runner配置的key。
     *
     * @return
     */
    public String getRunnerConfigKey(TaskRunnerConfig config) {
        StringBuilder sb = new StringBuilder( 168 );
        sb.append( config.getTaskClass() ).append( "#" );
        if (config.getTaskTag() != null && !config.getTaskTag().isEmpty()) {
            sb.append( config.getTaskTag() );
        }
        sb.append( "$" );
        if (config.getRunTarget() != null && !config.getRunTarget().isEmpty()) {
            sb.append( config.getRunTarget() );
        }
        return sb.toString();
    }

    /**
     * 获得taskData的key。
     *
     * @return
     */
    public String getRunnerConfigKeyByData(TaskData<?, ?> data) {
        StringBuilder sb = new StringBuilder( 168 );
        sb.append( data.getTaskClass() ).append( "#" );
        if (data.getTaskTag() != null && !data.getTaskTag().isEmpty()) {
            sb.append( data.getTaskTag() );
        }
        sb.append( "$" );
        if (data.getRunTarget() != null && !data.getRunTarget().isEmpty()) {
            sb.append( data.getRunTarget() );
        }
        return sb.toString();
    }


    /**
     * 根据runner配置，计算出队列名。
     *
     * @return
     */
    public String getQueueNameByConfig(TaskRunnerConfig config) {
        return switch (config.getQueueType()) {
            case TaskRunnerConfig.TYPE_QUEUE_TASK -> getRunnerConfigKey( config );
            case TaskRunnerConfig.TYPE_QUEUE_GROUP_PRIORITY -> getGroupQueueName( config.getTaskClass(), "priority", config.getRunTarget() );
            case TaskRunnerConfig.TYPE_QUEUE_GROUP -> getGroupQueueName( config.getTaskClass(), "default", config.getRunTarget() );
            case TaskRunnerConfig.TYPE_QUEUE_PROJECT_PRIORITY -> taskProperties.getTaskProject() + "@priority$" + config.getRunTarget();
            default -> taskProperties.getTaskProject() + "$" + config.getRunTarget();
        };
    }

    /**
     * 获取ttl队列名称
     *
     * @param queueName 队列名
     * @return ttl队列名
     */
    public String getTTLQueueName(String queueName) {
        return queueName + '*';
    }

    /**
     * 是否是ttl队列名称
     *
     * @param queueName 队列名
     * @return ttl队列名
     */
    public boolean isTTLQueueName(String queueName) {
        return queueName.endsWith( "*" );
    }


    /**
     * 根据TaskData获得包下队列名。
     *
     * @return
     */
    public String getGroupQueueName(String taskClass, String type, String runTarget) {
        if (taskClass != null) {
            int pos = taskClass.lastIndexOf( '.' );
            if (pos > -1) {
                return taskClass.substring( 0, pos ) + "@" + type + "$" + runTarget;
            }
        }
        return taskClass;
    }

}
