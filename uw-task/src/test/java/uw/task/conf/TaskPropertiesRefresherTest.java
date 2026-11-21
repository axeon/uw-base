package uw.task.conf;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * UwTaskAutoConfiguration.TaskPropertiesRefresher 原地重绑单测（其余模块的同构 Refresher 行为一致）。
 */
class TaskPropertiesRefresherTest {

    /**
     * 可变环境（模拟 nacos 推送后的新配置源）。
     */
    private StandardEnvironment environment;

    /**
     * 被测组件。
     */
    private UwTaskAutoConfiguration.TaskPropertiesRefresher refresher;

    /**
     * 属性 bean（原地重绑目标）。
     */
    private UwTaskProperties properties;

    @BeforeEach
    void setUp() {
        environment = new StandardEnvironment();
        properties = new UwTaskProperties();
        refresher = new UwTaskAutoConfiguration.TaskPropertiesRefresher(properties);
        refresher.setEnvironment(environment);
    }

    private void publish(Map<String, Object> source, Set<String> keys) {
        environment.getPropertySources().addFirst(new MapPropertySource("nacos-push", source));
        refresher.onEnvironmentChange(new EnvironmentChangeEvent(keys));
    }

    /**
     * uw.task 前缀变更：同实例字段更新（静态持有方生效的关键语义）。
     */
    @Test
    void uwTaskPrefixChangeRebindsInPlace() {
        properties.setTaskCenterHost("http://old");
        Map<String, Object> source = new HashMap<>();
        source.put("uw.task.task-center-host", "http://new-center");
        source.put("uw.task.task-queue-retry-delay", "9000");
        publish(source, Set.of("uw.task.task-center-host"));
        assertEquals("http://new-center", properties.getTaskCenterHost(), "center 地址应热更新");
        assertEquals(9000L, properties.getTaskQueueRetryDelay(), "重试延时应热更新");
    }

    /**
     * 非 uw.task 前缀的变更事件：不触碰属性。
     */
    @Test
    void foreignPrefixIgnored() {
        properties.setTaskCenterHost("http://old");
        Map<String, Object> source = new HashMap<>();
        source.put("uw.task.task-center-host", "http://new-center");
        publish(source, Set.of("uw.other.key"));
        assertEquals("http://old", properties.getTaskCenterHost(), "非本模块前缀不应触发重绑");
    }
}
