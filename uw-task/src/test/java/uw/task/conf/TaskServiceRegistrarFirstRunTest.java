package uw.task.conf;

import org.junit.jupiter.api.Test;
import uw.common.response.ResponseData;
import uw.task.TaskData;
import uw.task.TaskListenerManager;
import uw.task.TaskRunner;
import uw.task.api.TaskApiClient;
import uw.task.container.TaskCronerContainer;
import uw.task.container.TaskDelayTransferContainer;
import uw.task.container.TaskRunnerContainer;
import uw.task.entity.TaskContact;
import uw.task.entity.TaskCronerConfig;
import uw.task.entity.TaskRunnerConfig;
import uw.task.util.TaskLocalRateLimiter;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 登记器首次启动链路（不连 MQ/Redis/task-center，task-center 用桩替换）：
 * <ul>
 *   <li>首次拉取配置失败先短暂重试，重试成功不降级；重试耗尽才降级；</li>
 *   <li>默认配置上传 task-center 失败时先以本地默认配置（id=0）启动，下一周期重试上传，成功后换成 center 配置。</li>
 * </ul>
 */
class TaskServiceRegistrarFirstRunTest {

    /**
     * 第一次拉取失败、重试成功：共拉取 2 次。
     *
     * @throws Exception 反射调用失败
     */
    @Test
    void firstFetchFailure_isRetriedBeforeDegrading() throws Exception {
        StubClient client = new StubClient(props());
        client.failFirstFetch = true;
        TaskServiceRegistrar registrar = registrar(client);
        updateRunnerConfig(registrar);
        assertThat(client.runnerFetch).isEqualTo(2);
    }

    /**
     * 一直拉取失败：首次 + 3 次重试后才降级（间隔 1+2+3 秒）。
     *
     * @throws Exception 反射调用失败
     */
    @Test
    void firstFetchAlwaysFailing_degradesAfterRetries() throws Exception {
        StubClient client = new StubClient(props());
        client.failAllFetch = true;
        TaskServiceRegistrar registrar = registrar(client);
        updateRunnerConfig(registrar);
        assertThat(client.runnerFetch).isEqualTo(4);
    }

    /**
     * 单次拉取本身就很慢（如连接超时，超过时间预算）：不再重试，首次失败后直接降级，共拉取 2 次（runner 与 croner 各 1 次）。
     *
     * @throws Exception 反射调用失败
     */
    @Test
    void slowFirstFetch_isNotRetried() throws Exception {
        StubClient client = new StubClient(props());
        client.failAllFetch = true;
        client.fetchDelayMillis = FIRST_FETCH_RETRY_BUDGET_MILLIS + 200L;
        TaskServiceRegistrar registrar = registrar(client);
        updateRunnerConfig(registrar);
        assertThat(client.runnerFetch).isEqualTo(1);
    }

    /**
     * 上传默认配置失败：先以 id=0 的本地配置启动；下一周期补传成功后配置换成 center 返回的 id，之后不再上传。
     */
    @Test
    void uploadFailure_startsLocalThenSwitchesToCenterConfig() {
        UwTaskProperties props = props();
        StubClient client = new StubClient(props);
        TaskMetaInfoManager meta = new TaskMetaInfoManager(null, props);
        meta.getRunnerInstanceMap().put(DemoRunner.class.getName(), new DemoRunner());
        TaskCronerContainer croner = new TaskCronerContainer(props, client, meta, new TaskListenerManager());
        try {
            TaskRunnerContainer runner = new TaskRunnerContainer(props, client, meta, new TaskLocalRateLimiter(), new TaskListenerManager(), null, null);
            TaskServiceRegistrar registrar = new TaskServiceRegistrar(props, client, runner, croner, meta, new TaskDelayTransferContainer(null, props.getTaskProject()));

            registrar.updateConfig();
            assertThat(client.runnerInit).isEqualTo(1);
            assertThat(storedConfig(meta).getId()).as("上传失败时以本地默认配置(id=0)启动").isZero();

            registrar.updateConfig();
            assertThat(client.runnerInit).isEqualTo(2);
            assertThat(storedConfig(meta).getId()).as("补传成功后换成 center 配置").isEqualTo(99);

            registrar.updateConfig();
            assertThat(client.runnerInit).as("补传成功后不再重复上传").isEqualTo(2);
        } finally {
            croner.shutdown();
        }
    }

    /**
     * 取本地缓存里唯一的 runner 配置（测试只有一个任务类）。
     *
     * @param meta 元信息管理器
     * @return 缓存的配置
     */
    static TaskRunnerConfig storedConfig(TaskMetaInfoManager meta) {
        assertThat(meta.getRunnerConfigMap()).hasSize(1);
        return meta.getRunnerConfigMap().values().iterator().next();
    }

    static UwTaskProperties props() {
        UwTaskProperties props = new UwTaskProperties();
        props.setEnableRegistry(true);
        props.setTaskProject("uw.task.conf");
        return props;
    }

    static TaskServiceRegistrar registrar(TaskApiClient client) {
        UwTaskProperties props = props();
        TaskMetaInfoManager meta = new TaskMetaInfoManager(null, props);
        return new TaskServiceRegistrar(props, client, new TaskRunnerContainer(props, client, meta, new TaskLocalRateLimiter(), new TaskListenerManager(), null, null),
                null, meta, null);
    }

    static void updateRunnerConfig(TaskServiceRegistrar registrar) throws Exception {
        Method method = TaskServiceRegistrar.class.getDeclaredMethod("updateTaskRunnerConfig");
        method.setAccessible(true);
        method.invoke(registrar);
    }

    /**
     * 重试的总时长预算（毫秒），与登记器保持一致。
     */
    static final long FIRST_FETCH_RETRY_BUDGET_MILLIS = 8000L;

    /**
     * 静默休眠。
     *
     * @param millis 休眠毫秒
     */
    static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * task-center 的桩：按开关控制拉取/上传成败，并计数。
     */
    static class StubClient extends TaskApiClient {

        int runnerFetch;
        int runnerInit;
        boolean failFirstFetch;
        boolean failAllFetch;

        /**
         * 每次拉取前的人为耗时（毫秒），模拟连接超时等慢失败。
         */
        long fetchDelayMillis;

        StubClient(UwTaskProperties props) {
            super(props, null, null);
        }

        @Override
        public ResponseData<List<TaskRunnerConfig>> getTaskRunnerConfigList(String runTarget, String taskProject, long lastUpdateTime) {
            runnerFetch++;
            sleep(fetchDelayMillis);
            return failAllFetch || (failFirstFetch && runnerFetch == 1) ? null : ResponseData.success(List.of());
        }

        @Override
        public ResponseData<List<TaskCronerConfig>> getTaskCronerConfigList(String runTarget, String taskProject, long lastUpdateTime) {
            return ResponseData.success(List.of());
        }

        @Override
        public ResponseData<TaskRunnerConfig> initTaskRunnerConfig(TaskRunnerConfig config) {
            runnerInit++;
            if (runnerInit == 1) {
                return ResponseData.errorMsg("runner config register failed.");
            }
            config.setId(99);
            return ResponseData.success(config);
        }

        @Override
        public ResponseData<Void> initTaskContact(TaskContact contact) {
            return ResponseData.success();
        }
    }

    /**
     * 测试用队列任务（类在本测试类里，包名 uw.task.conf 落在测试用的 task-project 之下）。
     */
    static class DemoRunner extends TaskRunner<Object, Object> {

        @Override
        public Object runTask(TaskData<Object, Object> taskData) {
            return null;
        }

        @Override
        public TaskRunnerConfig initConfig() {
            return new TaskRunnerConfig();
        }
    }
}
