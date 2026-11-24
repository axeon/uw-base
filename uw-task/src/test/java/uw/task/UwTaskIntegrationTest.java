package uw.task;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uw.task.conf.TaskMetaInfoManager;
import uw.task.demo.EchoRunner;
import uw.task.entity.TaskRunnerConfig;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * uw-task 端到端集成测试。
 * <p>
 * 在 application.yml <b>关闭 publisher-confirm-type</b> 下跑，全过即证明 uw-task 的启动/RPC/投递不依赖 confirm。
 * </p>
 *
 * @author axeon
 */
@SpringBootTest(classes = UwTaskTestApplication.class)
class UwTaskIntegrationTest {

    @Autowired
    EchoRunner echoRunner;

    /** 手动注册 EchoRunner config（绕过 task-center 不可达）。 */
    @BeforeEach
    void registerEchoRunnerConfig() throws Exception {
        TaskMetaInfoManager tmm = metaInfoManager();
        TaskRunnerConfig config = echoRunner.initConfig();
        config.setRunTarget("default");
        tmm.setRunnerConfig(tmm.getRunnerConfigKey(config), config);
    }

    private TaskMetaInfoManager metaInfoManager() throws Exception {
        Field f = TaskFactory.class.getDeclaredField("taskMetaInfoManager");
        f.setAccessible(true);
        return (TaskMetaInfoManager) f.get(TaskFactory.getInstance());
    }

    // ===== confirm 是否必须（关 confirm 下跑）=====

    @Test
    void contextLoads() {
        assertNotNull(TaskFactory.getInstance());
    }

    @Test
    void runTask_localAutoRpc_shouldReturnEcho() {
        TaskData<String, String> td = TaskData.<String, String>builder(EchoRunner.class, "hello-rpc").build();
        TaskData<String, String> result = TaskFactory.getInstance().runTask(td);
        assertNotNull(result);
        assertEquals("echo:hello-rpc", result.getResultData());
        assertEquals(TaskData.STATE_SUCCESS, result.getState());
    }

    @Test
    void sendToQueue_publishOk_withoutConfirm() {
        TaskData<String, String> td = TaskData.<String, String>builder(EchoRunner.class, "hello-queue").build();
        assertDoesNotThrow(() -> TaskFactory.getInstance().sendToQueue(td));
    }
}
