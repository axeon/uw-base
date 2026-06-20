package uw.task.conf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import uw.task.TaskData;
import uw.task.entity.TaskCronerConfig;
import uw.task.entity.TaskRunnerConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * {@link TaskMetaInfoManager} 的纯逻辑单元测试，覆盖配置 key 计算、队列名映射、TTL 队列名、多实例 key。
 *
 * <p>不调用 init()（该方法依赖 ApplicationContext.getBeansOfType），仅测试无副作用的计算方法。</p>
 *
 * @author axeon
 */
class TaskMetaInfoManagerTest {

    private TaskMetaInfoManager manager;

    @BeforeEach
    void setUp() {
        TaskProperties props = new TaskProperties();
        props.setTaskProject("com.demo.task");
        // ApplicationContext 仅被构造器存引用，不调用 init() 即不触发 getBeansOfType
        manager = new TaskMetaInfoManager(mock(ApplicationContext.class), props);
    }

    @Test
    void getRunnerConfigKey_withTag() {
        TaskRunnerConfig config = baseRunnerConfig();
        config.setTaskClass("com.demo.task.Foo");
        config.setTaskTag("t1");
        config.setRunTarget("prod");
        assertEquals("com.demo.task.Foo#t1$prod", manager.getRunnerConfigKey(config));
    }

    @Test
    void getRunnerConfigKey_withoutTag() {
        TaskRunnerConfig config = baseRunnerConfig();
        config.setTaskClass("com.demo.task.Foo");
        config.setTaskTag("");
        config.setRunTarget("prod");
        // 无 tag 时 # 与 $ 之间为空
        assertEquals("com.demo.task.Foo#$prod", manager.getRunnerConfigKey(config));
    }

    @Test
    void getRunnerConfigKeyByData_matchesConfigKey() {
        TaskData<Object, Object> data = TaskData.builder().build();
        data.setTaskClass("com.demo.task.Bar");
        data.setTaskTag("v2");
        data.setRunTarget("prod");
        TaskRunnerConfig config = baseRunnerConfig();
        config.setTaskClass("com.demo.task.Bar");
        config.setTaskTag("v2");
        config.setRunTarget("prod");
        assertEquals(manager.getRunnerConfigKey(config), manager.getRunnerConfigKeyByData(data));
    }

    @Test
    void getQueueNameByConfig_variants() {
        TaskRunnerConfig config = baseRunnerConfig();
        config.setTaskClass("com.demo.task.svc.Foo");
        config.setTaskTag("t1");
        config.setRunTarget("prod");

        // 任务级队列：直接用 config key
        config.setQueueType(TaskRunnerConfig.TYPE_QUEUE_TASK);
        assertEquals("com.demo.task.svc.Foo#t1$prod", manager.getQueueNameByConfig(config));

        // 任务组默认队列：取包名 + @default$target
        config.setQueueType(TaskRunnerConfig.TYPE_QUEUE_GROUP);
        assertEquals("com.demo.task.svc@default$prod", manager.getQueueNameByConfig(config));

        // 任务组优先级队列
        config.setQueueType(TaskRunnerConfig.TYPE_QUEUE_GROUP_PRIORITY);
        assertEquals("com.demo.task.svc@priority$prod", manager.getQueueNameByConfig(config));

        // 项目优先级队列
        config.setQueueType(TaskRunnerConfig.TYPE_QUEUE_PROJECT_PRIORITY);
        assertEquals("com.demo.task@priority$prod", manager.getQueueNameByConfig(config));

        // 项目默认队列
        config.setQueueType(TaskRunnerConfig.TYPE_QUEUE_PROJECT);
        assertEquals("com.demo.task$prod", manager.getQueueNameByConfig(config));
    }

    @Test
    void ttlQueueName_namingAndDetection() {
        assertEquals("foo*", manager.getTTLQueueName("foo"));
        assertTrue(manager.isTTLQueueName("foo*"));
        assertFalse(manager.isTTLQueueName("foo"));
    }

    @Test
    void getFitQueue_returnsTtlName_whenDelayOnAndGlobalAndDelayed() {
        TaskRunnerConfig config = baseRunnerConfig();
        config.setTaskClass("com.demo.task.Foo");
        config.setTaskTag("t1");
        config.setRunTarget("prod");
        config.setQueueType(TaskRunnerConfig.TYPE_QUEUE_TASK);
        config.setDelayType(TaskRunnerConfig.TYPE_DELAY_ON);
        manager.setRunnerConfig(manager.getRunnerConfigKey(config), config);

        TaskData<Object, Object> data = TaskData.builder().build();
        data.setTaskClass("com.demo.task.Foo");
        data.setTaskTag("t1");
        data.setRunTarget("prod");
        data.setRunType(TaskData.RUN_TYPE_GLOBAL);
        data.setTaskDelay(5000);

        assertEquals("com.demo.task.Foo#t1$prod*", manager.getFitQueue(data));
    }

    @Test
    void getFitQueue_throws_whenNoConfigFound() {
        TaskData<Object, Object> data = TaskData.builder().build();
        data.setTaskClass("com.demo.task.Missing");
        data.setRunTarget("prod");
        assertThrows(RuntimeException.class, () -> manager.getFitQueue(data));
    }

    @Test
    void getCronerConfigKey_multiInstanceUsesTaskParam() {
        // 多实例 croner 的 config key 以 taskParam 作为区分维度（与 Runner 侧 taskTag 对称）。
        TaskCronerConfig config = new TaskCronerConfig();
        config.setTaskClass("com.demo.task.CronA");
        config.setRunTarget("prod");

        // 单实例：无 taskParam，# 与 $ 之间为空
        config.setTaskParam("");
        assertEquals("com.demo.task.CronA#$prod", manager.getCronerConfigKey(config));

        // 多实例：有 taskParam 时拼入 taskParam（而非 id）
        config.setTaskParam("tenant-xyz");
        config.setId(42L);
        assertEquals("com.demo.task.CronA#tenant-xyz$prod", manager.getCronerConfigKey(config));
    }

    private TaskRunnerConfig baseRunnerConfig() {
        TaskRunnerConfig config = new TaskRunnerConfig();
        config.setState(1);
        return config;
    }

}
