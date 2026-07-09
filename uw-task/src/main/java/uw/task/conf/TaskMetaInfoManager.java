package uw.task.conf;

import org.springframework.context.ApplicationContext;
import uw.task.TaskCroner;
import uw.task.TaskData;
import uw.task.TaskDelayer;
import uw.task.TaskRunner;
import uw.task.entity.TaskCronerConfig;
import uw.task.entity.TaskDelayerConfig;
import uw.task.entity.TaskRunnerConfig;

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
    private final Map<String, TaskRunner> runnerInstanceMap = new ConcurrentHashMap<>();

    /**
     * Cron任务实例缓存。
     */
    private final Map<String, TaskCroner> cronerInstanceMap = new ConcurrentHashMap<>();

    /**
     * Delayer任务实例缓存。
     */
    private final Map<String, TaskDelayer<?, ?>> delayerInstanceMap = new ConcurrentHashMap<>();

    /**
     * Runner任务配置缓存
     */
    private final ConcurrentHashMap<String, TaskRunnerConfig> runnerConfigMap = new ConcurrentHashMap<>();

    /**
     * Cron任务配置缓存。
     */
    private final ConcurrentHashMap<String, TaskCronerConfig> cronerConfigMap = new ConcurrentHashMap<>();

    /**
     * Delayer任务配置缓存。
     */
    private final ConcurrentHashMap<String, TaskDelayerConfig> delayerConfigMap = new ConcurrentHashMap<>();

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
     * @param runnerConfigKey 配置键（见 {@link #getRunnerConfigKey}）
     * @param config          队列任务配置
     */
    public void setRunnerConfig(String runnerConfigKey, TaskRunnerConfig config) {
        runnerConfigMap.put(runnerConfigKey, config);
    }

    /**
     * 移除队列任务配置。
     *
     * @param runnerConfigKey 配置键（见 {@link #getRunnerConfigKey}）
     */
    public void removeRunnerConfig(String runnerConfigKey) {
        runnerConfigMap.remove(runnerConfigKey);
    }

    /**
     * 设置定时任务配置。
     *
     * @param cronerConfigKey 配置键（见 {@link #getCronerConfigKey}）
     * @param config          定时任务配置
     */
    public void setCronerConfig(String cronerConfigKey, TaskCronerConfig config) {
        cronerConfigMap.put(cronerConfigKey, config);
    }

    /**
     * 移除定时任务配置。
     *
     * @param cronerConfigKey 配置键（见 {@link #getCronerConfigKey}）
     */
    public void removeCronerConfig(String cronerConfigKey) {
        cronerConfigMap.remove(cronerConfigKey);
    }

    /**
     * 获取队列任务实例 Map（key 为 taskClass 全限定名）。
     *
     * @return 队列任务实例 Map
     */
    public Map<String, TaskRunner> getRunnerInstanceMap() {
        return runnerInstanceMap;
    }

    /**
     * 获取定时任务实例 Map（key 为 taskClass 全限定名）。
     *
     * @return 定时任务实例 Map
     */
    public Map<String, TaskCroner> getCronerInstanceMap() {
        return cronerInstanceMap;
    }

    /**
     * 获取指定 taskClass 的队列任务实例。
     *
     * @param runnerCls taskClass 全限定名
     * @return 任务实例，无匹配时返回 null
     */
    public TaskRunner getRunnerInstance(String runnerCls) {
        return runnerInstanceMap.get(runnerCls);
    }

    /**
     * 获取指定 taskClass 的定时任务实例。
     *
     * @param cronerCls taskClass 全限定名
     * @return 任务实例，无匹配时返回 null
     */
    public TaskCroner getCronerInstance(String cronerCls) {
        return cronerInstanceMap.get(cronerCls);
    }

    /**
     * 获取延迟任务实例 Map（key 为 taskClass 全限定名）。
     *
     * @return 延迟任务实例 Map
     */
    public Map<String, TaskDelayer<?, ?>> getDelayerInstanceMap() {
        return delayerInstanceMap;
    }

    /**
     * 获取指定 taskClass 的延迟任务实例。
     *
     * @param delayerCls taskClass 全限定名
     * @return 任务实例，无匹配时返回 null
     */
    public TaskDelayer<?, ?> getDelayerInstance(String delayerCls) {
        return delayerInstanceMap.get(delayerCls);
    }

    /**
     * 设置延迟任务配置。
     *
     * @param delayerConfigKey 配置键（见 {@link #getConfigKey}）
     * @param config           延迟任务配置
     */
    public void setDelayerConfig(String delayerConfigKey, TaskDelayerConfig config) {
        delayerConfigMap.put(delayerConfigKey, config);
    }

    /**
     * 移除延迟任务配置。
     *
     * @param delayerConfigKey 配置键（见 {@link #getConfigKey}）
     */
    public void removeDelayerConfig(String delayerConfigKey) {
        delayerConfigMap.remove(delayerConfigKey);
    }

    /**
     * 获取延迟任务配置 Map（key 为配置键，见 {@link #getConfigKey}）。
     *
     * @return 延迟任务配置 Map
     */
    public ConcurrentHashMap<String, TaskDelayerConfig> getDelayerConfigMap() {
        return delayerConfigMap;
    }

    /**
     * 获取队列任务配置 Map（key 为配置键，见 {@link #getRunnerConfigKey}）。
     *
     * @return 队列任务配置 Map
     */
    public ConcurrentHashMap<String, TaskRunnerConfig> getRunnerConfigMap() {
        return runnerConfigMap;
    }

    /**
     * 获取定时任务配置 Map（key 为配置键，见 {@link #getCronerConfigKey}）。
     *
     * @return
     */
    public ConcurrentHashMap<String, TaskCronerConfig> getCronerConfigMap() {
        return cronerConfigMap;
    }

    /**
     * 初始化任务实例 Map。
     * <p>从 Spring 容器扫描所有 {@link TaskCroner} / {@link TaskRunner} Bean，按 taskClass 全限定名建立索引。</p>
     */
    public void init() {
        // 设置当前主机上所有的TaskRunner
        Map<String, TaskCroner> cronerMap = context.getBeansOfType(TaskCroner.class);
        for (Map.Entry<String, TaskCroner> kv : cronerMap.entrySet()) {
            // 拿到任务类名
            TaskCroner taskCroner = kv.getValue();
            String taskClass = taskCroner.getClass().getName();
            cronerInstanceMap.put(taskClass, taskCroner);
        }
        // 设置当前主机上所有的TaskRunner
        Map<String, TaskRunner> runnerMap = context.getBeansOfType(TaskRunner.class);
        for (Map.Entry<String, TaskRunner> kv : runnerMap.entrySet()) {
            // 拿到任务类名
            TaskRunner taskRunner = kv.getValue();
            String taskClass = taskRunner.getClass().getName();
            runnerInstanceMap.put(taskClass, taskRunner);
        }
        // 设置当前主机上所有的TaskDelayer
        Map<String, TaskDelayer> delayerMap = context.getBeansOfType(TaskDelayer.class);
        for (Map.Entry<String, TaskDelayer> kv : delayerMap.entrySet()) {
            TaskDelayer taskDelayer = kv.getValue();
            String taskClass = taskDelayer.getClass().getName();
            delayerInstanceMap.put(taskClass, taskDelayer);
        }
    }

    /**
     * 检查一个 runner 是否可以在本机运行（本机是否注册了该 taskClass 的实例）。
     *
     * @param taskData 任务数据
     * @return 本机存在该 runner 返回 true，否则 false
     */
    public boolean checkRunnerRunLocal(TaskData<?, ?> taskData) {
        return runnerInstanceMap.containsKey(taskData.getTaskClass());
    }

    /**
     * 客户端发送任务到队列时，通过此方法获取合适的队列。
     * <p>根据服务端配置匹配配置键（taskClass#taskTag$runTarget），无 taskTag 时宽松匹配；
     * 再按队列类型（任务级/组级/项目级）映射出最终队列名。延迟任务且 runType=GLOBAL 时返回 TTL 队列名。</p>
     *
     * @param data 任务数据
     * @return 匹配到的队列名
     * @throws RuntimeException 找不到任何匹配配置时抛出
     */
    public String getFitQueue(TaskData<?, ?> data) {
        String fitName = getRunnerConfigKeyByData(data);
        TaskRunnerConfig config = runnerConfigMap.get(fitName);
        if (config == null) {
            // 此时再检测没有taskTag匹配的情况。
            int pos1 = fitName.indexOf('#');
            int pos2 = fitName.lastIndexOf('$');
            if (pos1 >= 0 && pos2 > pos1) {
                String test = fitName.substring(0, pos1 + 1) + fitName.substring(pos2);
                config = runnerConfigMap.get(test);
                if (config != null) {
                    fitName = test;
                }
            }
        }
        if (config != null) {
            String queueName = getQueueNameByConfig(config);
            // 只有开启了延时队列并且发送队列异步进行才会返回TTL队列名称
            return config.getDelayType() == TaskRunnerConfig.TYPE_DELAY_ON && data.getRunType() == TaskData.RUN_TYPE_GLOBAL && data.getTaskDelay() > 0 ?
                    getTTLQueueName(queueName) : queueName;
        } else {
            throw new RuntimeException("No matching task config: taskClass = " + data.getTaskClass());
        }
    }

    /**
     * 任务执行端执行任务时，按 taskData 匹配合适的 runner 配置。
     * <p>匹配规则同 {@link #getFitQueue}：精确 taskTag 匹配优先，无则宽松匹配无 taskTag 的配置。</p>
     *
     * @param data 任务数据
     * @return 匹配到的 runner 配置
     * @throws RuntimeException 找不到任何匹配配置时抛出
     */
    public TaskRunnerConfig getRunnerConfigByData(TaskData<?, ?> data) {
        TaskRunnerConfig config = null;
        String fitName = getRunnerConfigKeyByData(data);
        config = runnerConfigMap.get(fitName);
        if (config != null) {
            return config;
        }
        // 此时再检测没有taskTag匹配的情况。
        int pos1 = fitName.indexOf('#');
        int pos2 = fitName.lastIndexOf('$');
        if (pos1 >= 0 && pos2 > pos1) {
            String test = fitName.substring(0, pos1 + 1) + fitName.substring(pos2);
            config = runnerConfigMap.get(test);
            if (config != null) {
                return config;
            }
        }
        throw new RuntimeException("No matching task config: taskClass = " + data.getTaskClass());
    }


    /**
     * 获取croner配置键。 使用 taskClass#taskParam$target 来配置。
     * <p>多实例场景下，同一个 TaskCroner 子类通过不同的 taskParam 区分（与 Runner 侧用 taskTag 对称），
     * 因此配置 key 以 taskParam 作为第三维度。此前误用 {@code config.getId()}，而本地默认配置在首次上传前
     * id 为 0，会导致多个多实例任务 key 碰撞、且无法命中服务端动态配置。</p>
     *
     * @param config 定时任务配置
     * @return 配置键 taskClass#taskParam$runTarget
     */
    public String getCronerConfigKey(TaskCronerConfig config) {
        return getConfigKey(config.getTaskClass(), config.getTaskParam(), config.getRunTarget());
    }

    /**
     * 获取 Runner 配置的 key：taskClass#taskTag$runTarget。
     *
     * @param config 队列任务配置
     * @return 配置键
     */
    public String getRunnerConfigKey(TaskRunnerConfig config) {
        return getConfigKey(config.getTaskClass(), config.getTaskTag(), config.getRunTarget());
    }

    /**
     * 获取 Delayer 配置的 key：taskClass#taskTag$runTarget。
     * <p>多实例维度与 Runner 一致（taskTag），区别于 Croner 的 taskParam。</p>
     *
     * @param config 延迟任务配置
     * @return 配置键
     */
    public String getDelayerConfigKey(TaskDelayerConfig config) {
        return getConfigKey(config.getTaskClass(), config.getTaskTag(), config.getRunTarget());
    }

    /**
     * 由 taskData 计算配置键：taskClass#taskTag$runTarget（与 {@link #getRunnerConfigKey} 同构）。
     *
     * @param data 任务数据
     * @return 配置键
     */
    public String getRunnerConfigKeyByData(TaskData<?, ?> data) {
        return getConfigKey(data.getTaskClass(), data.getTaskTag(), data.getRunTarget());
    }

    /**
     * 通用配置键：taskClass#taskTag$runTarget（null 维度当空串）。
     * <p>供以 taskTag 作为多实例维度的任务（Runner / Delayer）复用，避免各容器各自拼接。</p>
     *
     * @param taskClass 任务类全限定名
     * @param taskTag   任务标签（多实例区分，可空）
     * @param runTarget 运行目标（可空）
     * @return 配置键 taskClass#taskTag$runTarget
     */
    public String getConfigKey(String taskClass, String taskTag, String runTarget) {
        return taskClass + "#" + (taskTag == null ? "" : taskTag) + "$" + (runTarget == null ? "" : runTarget);
    }


    /**
     * 根据 runner 配置的队列类型，计算出实际队列名。
     *
     * @param config 队列任务配置
     * @return 队列名（任务级/组级/项目级，依 queueType 而定）
     */
    public String getQueueNameByConfig(TaskRunnerConfig config) {
        return switch (config.getQueueType()) {
            case TaskRunnerConfig.TYPE_QUEUE_TASK -> getRunnerConfigKey(config);
            case TaskRunnerConfig.TYPE_QUEUE_GROUP_PRIORITY ->
                    getGroupQueueName(config.getTaskClass(), "priority", config.getRunTarget());
            case TaskRunnerConfig.TYPE_QUEUE_GROUP ->
                    getGroupQueueName(config.getTaskClass(), "default", config.getRunTarget());
            case TaskRunnerConfig.TYPE_QUEUE_PROJECT_PRIORITY ->
                    taskProperties.getTaskProject() + "@priority$" + config.getRunTarget();
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
     * 是否是 TTL 队列名称（以 '*' 结尾）。
     *
     * @param queueName 队列名
     * @return 是 TTL 队列返回 true，否则 false
     */
    public boolean isTTLQueueName(String queueName) {
        return queueName.endsWith("*");
    }


    /**
     * 根据任务类所在的包，计算任务组队列名：{包名}@{type}${runTarget}。
     *
     * @param taskClass  任务类全限定名
     * @param type       队列类型标识（如 "default"、"priority"）
     * @param runTarget  运行目标
     * @return 任务组队列名；taskClass 无包名时原样返回
     */
    public String getGroupQueueName(String taskClass, String type, String runTarget) {
        if (taskClass != null) {
            int pos = taskClass.lastIndexOf('.');
            if (pos > -1) {
                return taskClass.substring(0, pos) + "@" + type + "$" + runTarget;
            }
        }
        return taskClass;
    }

}
