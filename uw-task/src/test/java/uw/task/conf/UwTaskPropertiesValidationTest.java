package uw.task.conf;

import org.junit.jupiter.api.Test;

import java.util.Map;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import org.springframework.mock.env.MockEnvironment;
import uw.task.conf.UwTaskProperties.RuntimeMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 启动参数校验与运行档位：Redis 是必需项，MQ 可选（只配 Redis 为 {@link RuntimeMode#CRONER_ONLY}）；
 * 可选参数不合法时回退默认值。
 */
class UwTaskPropertiesValidationTest {

    static final String REDIS_MISSING = "uw.task.redis.host";

    /**
     * 必填项（Redis、执行主机的 task-project）齐备时不报缺失，配不配 MQ 都可以。
     */
    @Test
    void noMissingWhenRequiredSettingsPresent() {
        assertNull(missing(base()));
        assertNull(missing(base().withProperty("uw.task.rabbitmq.host", "127.0.0.1")));
        assertNull(missing(base().withProperty("uw.task.rabbitmq.addresses", "n1:5672,n2:5672")
                .withProperty("uw.task.enable-registry", "true").withProperty("uw.task.task-project", "com.demo")));
    }

    /**
     * 未配置 Redis 报缺失（空白串视同未配置；显式配置 localhost 算已配置）；MQ 已配不能替代 Redis。
     */
    @Test
    void missingWhenRedisAbsent() {
        assertTrue(missing(new MockEnvironment()).contains(REDIS_MISSING));
        assertTrue(missing(base().withProperty("uw.task.redis.host", "  ")).contains(REDIS_MISSING));
        assertTrue(missing(new MockEnvironment().withProperty("uw.task.rabbitmq.host", "127.0.0.1")).contains(REDIS_MISSING));
        assertNull(missing(base().withProperty("uw.task.redis.host", "localhost")));
    }

    /**
     * 执行主机未配置 task-project 报缺失；仅作调用方（enable-registry=false 或未配置）时不要求。
     */
    @Test
    void registryHostRequiresTaskProject() {
        MockEnvironment registry = base().withProperty("uw.task.enable-registry", "true");
        assertTrue(missing(registry).contains("uw.task.task-project"));
        assertTrue(missing(registry.withProperty("uw.task.task-project", " ")).contains("uw.task.task-project"));
        assertNull(missing(registry.withProperty("uw.task.task-project", "com.demo.task")));
        assertNull(missing(base().withProperty("uw.task.enable-registry", "false")));
    }

    /**
     * 必填项检测走 Binder 宽松绑定：环境变量形式的属性名同样生效。
     */
    @Test
    void relaxedBindingFromEnvironmentVariableStyle() {
        // 环境变量风格的键只在 SystemEnvironmentPropertySource 下才做宽松映射，普通属性源不认
        SystemEnvironmentPropertySource envVars = new SystemEnvironmentPropertySource("testEnv", Map.of(
                "UW_TASK_REDIS_HOST", "127.0.0.2", "UW_TASK_ENABLEREGISTRY", "true", "UW_TASK_TASKPROJECT", "com.demo"));
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addFirst(envVars);
        assertNull(missing(environment));
        assertEquals(RuntimeMode.CRONER_ONLY, mode(environment));
    }

    /**
     * 运行档位：没配 Redis 为 DISABLED（只配 MQ 也是）；只配 Redis 为 CRONER_ONLY；Redis 与 MQ（host 或 addresses）齐备为 FULL。
     */
    @Test
    void runtimeModeByConfiguredDependencies() {
        assertEquals(RuntimeMode.DISABLED, mode(new MockEnvironment()));
        assertEquals(RuntimeMode.DISABLED, mode(new MockEnvironment().withProperty("uw.task.rabbitmq.host", "127.0.0.1")));
        assertEquals(RuntimeMode.DISABLED, mode(base().withProperty("uw.task.enable-registry", "true")));
        assertEquals(RuntimeMode.CRONER_ONLY, mode(base()));
        assertEquals(RuntimeMode.CRONER_ONLY, mode(base().withProperty("uw.task.enable-registry", "true").withProperty("uw.task.task-project", "com.demo")));
        assertEquals(RuntimeMode.FULL, mode(base().withProperty("uw.task.rabbitmq.host", "127.0.0.1")));
        assertEquals(RuntimeMode.FULL, mode(base().withProperty("uw.task.rabbitmq.addresses", "n1:5672,n2:5672")));
    }

    /**
     * 可选参数不合法：getter 回退默认值；合法值（含边界 0 延时）原样返回；logInvalidSettings 不抛异常。
     */
    @Test
    void invalidOptionalValuesFallBackToDefaults() {
        UwTaskProperties props = new UwTaskProperties();
        props.setTaskRpcThreadMaxNum(0);
        props.setTaskLocalThreadMaxNum(-5);
        props.setTaskQueueRetryDelay(-1);
        props.setTaskRpcRetryDelay(-100);
        props.setRunTarget("");
        props.setTaskCenterHost(null);
        assertEquals(UwTaskProperties.DEFAULT_TASK_RPC_THREAD_MAX_NUM, props.getTaskRpcThreadMaxNum());
        assertEquals(UwTaskProperties.DEFAULT_TASK_LOCAL_THREAD_MAX_NUM, props.getTaskLocalThreadMaxNum());
        assertEquals(UwTaskProperties.DEFAULT_TASK_QUEUE_RETRY_DELAY, props.getTaskQueueRetryDelay());
        assertEquals(UwTaskProperties.DEFAULT_TASK_RPC_RETRY_DELAY, props.getTaskRpcRetryDelay());
        assertEquals(UwTaskProperties.DEFAULT_RUN_TARGET, props.getRunTarget());
        assertEquals(UwTaskProperties.DEFAULT_TASK_CENTER_HOST, props.getTaskCenterHost());
        props.setTaskRpcThreadMaxNum(3);
        props.setTaskQueueRetryDelay(0);
        assertEquals(3, props.getTaskRpcThreadMaxNum());
        assertEquals(0, props.getTaskQueueRetryDelay());
        props.logInvalidSettings();
    }

    /**
     * 只配置了 Redis 的环境（未配置 MQ）。
     *
     * @return MockEnvironment
     */
    static MockEnvironment base() {
        return new MockEnvironment().withProperty("uw.task.redis.host", "127.0.0.1");
    }

    /**
     * 按给定环境绑定属性对象后执行必填项检测（与装配处的用法一致）。
     *
     * @param environment 环境
     * @return 缺失的必填项说明；齐备返回 null
     */
    static String missing(Environment environment) {
        return bind(environment).missingRequiredSetting(environment);
    }

    /**
     * 按给定环境绑定属性对象后判定运行档位。
     *
     * @param environment 环境
     * @return 运行档位
     */
    static RuntimeMode mode(Environment environment) {
        return bind(environment).resolveRuntimeMode(environment);
    }

    /**
     * 按 uw.task 前缀把环境绑定到一个新的属性对象。
     *
     * @param environment 环境
     * @return 已绑定的属性对象
     */
    static UwTaskProperties bind(Environment environment) {
        return Binder.get(environment).bind("uw.task", Bindable.of(UwTaskProperties.class)).orElseGet(UwTaskProperties::new);
    }
}
