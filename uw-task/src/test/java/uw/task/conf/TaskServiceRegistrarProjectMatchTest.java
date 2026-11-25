package uw.task.conf;

import org.junit.jupiter.api.Test;
import uw.task.container.TaskCronerContainer;
import uw.task.container.TaskDelayTransferContainer;
import uw.task.container.TaskRunnerContainer;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 任务类是否属于本项目的判定（纯逻辑，不连任何外部服务）：按包名前缀匹配且带点号边界。
 */
class TaskServiceRegistrarProjectMatchTest {

    /**
     * 前缀相同但不是同一个包的任务类不能被匹配到。
     *
     * @throws Exception 反射调用失败
     */
    @Test
    void projectPrefixRespectsPackageBoundary() throws Exception {
        UwTaskProperties props = new UwTaskProperties();
        props.setTaskProject("saas.mall");
        TaskServiceRegistrar registrar = new TaskServiceRegistrar(props, null, (TaskRunnerContainer) null, (TaskCronerContainer) null, null, (TaskDelayTransferContainer) null);
        Method method = TaskServiceRegistrar.class.getDeclaredMethod("belongsToProject", String.class);
        method.setAccessible(true);
        assertThat(method.invoke(registrar, "saas.mall.task.OrderRunner")).isEqualTo(true);
        assertThat(method.invoke(registrar, "saas.mall")).isEqualTo(true);
        assertThat(method.invoke(registrar, "saas.mall2.task.OrderRunner")).isEqualTo(false);
        assertThat(method.invoke(registrar, "saas.base.task.MailRunner")).isEqualTo(false);
    }
}
