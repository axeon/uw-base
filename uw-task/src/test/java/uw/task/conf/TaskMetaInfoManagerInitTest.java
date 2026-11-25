package uw.task.conf;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.support.GenericApplicationContext;
import uw.task.TaskCroner;
import uw.task.TaskData;
import uw.task.TaskRunner;
import uw.task.entity.TaskContact;
import uw.task.entity.TaskCronerConfig;
import uw.task.entity.TaskCronerLog;
import uw.task.entity.TaskRunnerConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TaskMetaInfoManager#init()} 的索引口径：按任务类的真实类名建索引，被 CGLIB 代理的 bean
 * （业务方加了 @Transactional/@Async 等）不能用代理类名（带 $$SpringCGLIB$$ 后缀）。
 */
class TaskMetaInfoManagerInitTest {

    /**
     * 普通任务 bean：按自身类名索引。
     */
    @Test
    void indexesPlainBeansByClassName() {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean(PlainRunner.class);
            context.registerBean(PlainCroner.class);
            context.refresh();
            TaskMetaInfoManager manager = new TaskMetaInfoManager(context, new UwTaskProperties());
            manager.init();
            assertThat(manager.getRunnerInstance(PlainRunner.class.getName())).isInstanceOf(PlainRunner.class);
            assertThat(manager.getCronerInstance(PlainCroner.class.getName())).isInstanceOf(PlainCroner.class);
            assertThat(manager.checkRunnerRunLocal(TaskDataOf(PlainRunner.class))).isTrue();
        }
    }

    /**
     * 被 CGLIB 代理的任务 bean：仍按真实类名索引（代理类名带 $$SpringCGLIB$$ 后缀）。
     */
    @Test
    void indexesProxiedBeansByUserClassName() {
        PlainRunner runner = new PlainRunner();
        PlainCroner croner = new PlainCroner();
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            ProxyFactory runnerFactory = new ProxyFactory(runner);
            runnerFactory.setProxyTargetClass(true);
            ProxyFactory cronerFactory = new ProxyFactory(croner);
            cronerFactory.setProxyTargetClass(true);
            Object runnerProxy = runnerFactory.getProxy();
            Object cronerProxy = cronerFactory.getProxy();
            assertThat(runnerProxy.getClass().getName()).contains("$$");
            context.registerBean("proxiedRunner", TaskRunner.class, () -> (TaskRunner<?, ?>) runnerProxy);
            context.registerBean("proxiedCroner", TaskCroner.class, () -> (TaskCroner) cronerProxy);
            context.refresh();
            TaskMetaInfoManager manager = new TaskMetaInfoManager(context, new UwTaskProperties());
            manager.init();
            assertThat(manager.getRunnerInstance(PlainRunner.class.getName())).isSameAs(runnerProxy);
            assertThat(manager.getCronerInstance(PlainCroner.class.getName())).isSameAs(cronerProxy);
        }
    }

    static TaskData<?, ?> TaskDataOf(Class<?> cls) {
        TaskData<Object, Object> data = TaskData.builder().build();
        data.setTaskClass(cls.getName());
        return data;
    }

    /**
     * 测试用队列任务。
     */
    static class PlainRunner extends TaskRunner<Object, Object> {

        @Override
        public Object runTask(TaskData<Object, Object> taskData) {
            return null;
        }

        @Override
        public TaskRunnerConfig initConfig() {
            return new TaskRunnerConfig();
        }
    }

    /**
     * 测试用定时任务。
     */
    static class PlainCroner extends TaskCroner {

        @Override
        public String runTask(TaskCronerLog taskCronerLog) {
            return "ok";
        }

        @Override
        public TaskCronerConfig initConfig() {
            return new TaskCronerConfig();
        }

        @Override
        public TaskContact initContact() {
            return new TaskContact();
        }
    }
}
