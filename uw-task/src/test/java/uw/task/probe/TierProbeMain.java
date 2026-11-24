package uw.task.probe;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.web.client.RestClient;
import uw.log.es.LogClient;
import uw.task.TaskData;
import uw.task.TaskFactory;
import uw.task.conf.UwTaskAutoConfiguration;
import uw.task.rabbit.TaskAmqpOps;

import java.util.HashMap;
import java.util.Map;

/**
 * dev 上"仅定时任务档"的实机验证（独立可执行程序，不依赖测试框架）：真实 Spring 上下文 + 真实 Redis，不配置 MQ。
 *
 * <p>用法：{@code java -cp "classes:test-classes:lib/*" uw.task.probe.TierProbeMain}，
 * Redis 连接经环境变量 {@code UW_PROBE_REDIS_HOST/PORT/PASSWORD/DATABASE}。验证项：</p>
 * <ul>
 *   <li>只配 Redis：uw-task 正常装配为仅定时任务档；单例定时任务经 Redis 选出 Leader 后每秒执行；</li>
 *   <li>同档下队列任务的本地执行可用；入队/延时提交直接抛"未配置 MQ"；队列查询按无数据处理；</li>
 *   <li>连 Redis 也不配：整个 uw-task 不装配，上下文照常启动。</li>
 * </ul>
 * <p>task-center 指向不可达地址：配置拉取失败后走本地默认配置降级启动，不会向 dev 的 task-center 写入任何数据。</p>
 */
public final class TierProbeMain {

    static final String RUNNER_CLASS = TierProbeRunner.class.getName();
    static int failed = 0;

    private TierProbeMain() {
    }

    /**
     * 入口。
     *
     * @param args 未使用
     * @throws Exception 任何运行异常
     */
    public static void main(String[] args) throws Exception {
        System.out.println("TIER-PROBE java=" + System.getProperty("java.version") + ", redis=" + redisProps().get("uw.task.redis.host"));
        // ① 只配 Redis（不配 MQ）
        AnnotationConfigApplicationContext cronOnly = start(redisProps());
        TaskFactory factory = cronOnly.getBean(TaskFactory.class);
        check("只配 Redis 时装配 TaskFactory", factory != null);
        check("无 TaskFactory 之外的 MQ 操作面（档位=仅定时任务）", cronOnly.getBeansOfType(TaskAmqpOps.class).isEmpty());

        // 等首个配置刷新周期完成（center 不可达 → 本地降级，把 runner 默认配置写入本地索引）
        Thread.sleep(3000);
        TaskData<String, String> local = new TaskData<>(RUNNER_CLASS, "local-exec");
        TaskData<String, String> ran = factory.runTask(local);
        check("队列任务本地执行: " + ran.getResultData(), "echo:local-exec".equals(ran.getResultData()));

        String sendError = messageOf(() -> factory.sendToQueue(new TaskData<>(RUNNER_CLASS, "q")));
        check("入队直接失败: " + sendError, TaskAmqpOps.MQ_NOT_CONFIGURED.equals(sendError));
        TaskData<String, String> delayed = new TaskData<>(RUNNER_CLASS, "d");
        delayed.setTaskDelay(5000);
        String delayError = messageOf(() -> factory.sendToQueue(delayed));
        check("延时提交直接失败: " + delayError, TaskAmqpOps.MQ_NOT_CONFIGURED.equals(delayError));
        check("队列查询按无数据处理", factory.getQueueInfo("any")[0] == 0 && factory.getQueueInfo("any")[1] == 0);

        int before = TierProbeCroner.RUNS.get();
        Thread.sleep(8000);
        int runs = TierProbeCroner.RUNS.get() - before;
        check("单例定时任务 8 秒内执行次数=" + runs + "（每秒一次，期望 ≥ 6）", runs >= 6);
        check("执行的是探针定时任务: taskParam=" + TierProbeCroner.lastParam, "tier-probe".equals(TierProbeCroner.lastParam));
        cronOnly.close();

        // ② Redis 也不配：整个 uw-task 不装配
        AnnotationConfigApplicationContext disabled = start(new HashMap<>());
        check("Redis 与 MQ 都不配时不装配 TaskFactory", disabled.getBeansOfType(TaskFactory.class).isEmpty());
        disabled.close();

        System.out.println("TIER-RESULT " + (failed == 0 ? "PASS" : "FAIL") + " (failed=" + failed + ")");
        System.exit(failed == 0 ? 0 : 1);
    }

    static Map<String, Object> redisProps() {
        Map<String, Object> props = new HashMap<>();
        props.put("uw.task.redis.host", env("UW_PROBE_REDIS_HOST", "127.0.0.1"));
        props.put("uw.task.redis.port", env("UW_PROBE_REDIS_PORT", "6379"));
        props.put("uw.task.redis.database", env("UW_PROBE_REDIS_DATABASE", "15"));
        String password = System.getenv("UW_PROBE_REDIS_PASSWORD");
        if (password != null) {
            props.put("uw.task.redis.password", password);
        }
        // task-center 指向不可达地址：拉取失败走本地默认配置降级，不向真实 task-center 写数据
        props.put("uw.task.task-center-host", "http://127.0.0.1:9");
        props.put("uw.task.enable-registry", "true");
        props.put("uw.task.task-project", "uw.task.probe");
        props.put("spring.application.name", "uw-task-tier-probe");
        props.put("project.version", "probe");
        props.put("server.port", "0");
        return props;
    }

    static AnnotationConfigApplicationContext start(Map<String, Object> props) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("tierProbe", props));
        context.registerBean("authRestClient", RestClient.class, () -> RestClient.create());
        // 探针任务的 logLevel 均为 NONE（不写 ES），LogClient 不需要真实的 LogService；容器内 Mockito 无法自附加 agent，故不用 mock
        context.registerBean("logClient", LogClient.class, TierProbeMain::noopLogClient);
        context.register(TierProbeConfig.class);
        context.refresh();
        return context;
    }

    /**
     * 不写 ES 的日志客户端：启动期会注册日志对象（需要 LogService），这里覆盖为空操作；探针任务 logLevel 均为 NONE，不会有日志外发。
     *
     * @return 日志客户端
     */
    static LogClient noopLogClient() {
        return new LogClient(null) {
            @Override
            public void regLogObjectWithIndexName(Class<?> logClass, String index) {
            }
        };
    }

    static void check(String what, boolean ok) {
        if (!ok) {
            failed++;
        }
        System.out.println("TIER-CHECK " + (ok ? "OK   " : "FAIL ") + what);
    }

    static String messageOf(Runnable action) {
        try {
            action.run();
            return "(no exception)";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    /**
     * 探针配置：显式注册探针任务 bean，并引入 uw-task 自动配置。<p>不加 {@code @Configuration}/{@code @Component}：
     * 集成测试的应用扫描整个 {@code uw.task} 包，带注解会被扫进去，把探针任务注册到 dev 的 task-center 并声明探针队列；
     * 仅由 {@link TierProbeMain#start} 显式 register（lite 模式，{@code @Bean} 方法照常生效）。</p>
     */
    @Import(UwTaskAutoConfiguration.class)
    static class TierProbeConfig {

        /**
         * 默认构造器。
         */
        public TierProbeConfig() {
        }

        /**
         * 探针定时任务（显式注册，不用 @Component，避免被集成测试的应用扫描到）。
         *
         * @return 定时任务
         */
        @Bean
        TierProbeCroner tierProbeCroner() {
            return new TierProbeCroner();
        }

        /**
         * 探针队列任务（显式注册）。
         *
         * @return 队列任务
         */
        @Bean
        TierProbeRunner tierProbeRunner() {
            return new TierProbeRunner();
        }
    }
}
