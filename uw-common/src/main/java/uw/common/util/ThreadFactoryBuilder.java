package uw.common.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 线程工厂构建器：静态入口 {@link #of(String)} + 链式配置 + {@link Builder#build()}，替代 guava ThreadFactoryBuilder。
 * <p>
 * nameFormat 必填（%d 从 0 递增）；默认非 daemon 线程（与 JDK/guava 默认一致），需后台线程时链式 {@link Builder#daemon()}。
 * 可选 {@link Builder#uncaughtExceptionHandler} 挂兜底处理（如记 error 日志，避免线程裸死静默）。
 * </p>
 */
public final class ThreadFactoryBuilder {

    private ThreadFactoryBuilder() {
    }

    /**
     * 以线程名格式（含 %d 占位）创建构建器。
     *
     * @param nameFormat 线程名格式，如 "uw-task-service-%d"
     */
    public static Builder of(String nameFormat) {
        return new Builder(nameFormat);
    }

    public static final class Builder {

        /**
         * 线程名格式（含 %d 占位，从 0 递增）。
         */
        private final String nameFormat;

        /**
         * 是否 daemon 线程（默认非 daemon，与 JDK 一致）。
         */
        private boolean daemon = false;

        /**
         * 线程优先级（null=继承默认）。
         */
        private Integer priority = null;

        /**
         * 附加线程配置（如 setContextClassLoader），在命名/daemon/优先级设置之后执行；null=无。
         */
        private Consumer<Thread> configurer = null;

        /**
         * 未捕获异常处理器（建议挂 error 日志兜底）；null=无。
         */
        private Thread.UncaughtExceptionHandler uncaughtExceptionHandler = null;

        private Builder(String nameFormat) {
            this.nameFormat = nameFormat;
        }

        /**
         * 设为 daemon 线程。
         */
        public Builder daemon() {
            this.daemon = true;
            return this;
        }

        /**
         * 设置线程优先级（不设置则继承默认）。
         */
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * 附加线程配置（如 setContextClassLoader），在命名与 daemon 设置之后执行。
         */
        public Builder configure(Consumer<Thread> configurer) {
            this.configurer = configurer;
            return this;
        }

        /**
         * 设置未捕获异常处理器（建议挂 error 日志兜底）。
         */
        public Builder uncaughtExceptionHandler(Thread.UncaughtExceptionHandler handler) {
            this.uncaughtExceptionHandler = handler;
            return this;
        }

        /**
         * 构建线程工厂（不可变快照：捕获当前配置，后续 Builder 变更不影响已构建实例）。
         *
         * @return 线程工厂：按 nameFormat 命名（%d 从 0 递增）并应用 daemon/优先级/异常处理器/附加配置
         */
        public ThreadFactory build() {
            String fmt = nameFormat;
            boolean isDaemon = daemon;
            Integer prio = priority;
            Consumer<Thread> cfg = configurer;
            Thread.UncaughtExceptionHandler handler = uncaughtExceptionHandler;
            return new ThreadFactory() {
                /**
                 * 线程编号发生器（%d 占位取值，从 0 递增）。
                 */
                private final AtomicLong index = new AtomicLong();

                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable, String.format(fmt, index.getAndIncrement()));
                    thread.setDaemon(isDaemon);
                    if (prio != null) {
                        thread.setPriority(prio);
                    }
                    if (handler != null) {
                        thread.setUncaughtExceptionHandler(handler);
                    }
                    if (cfg != null) {
                        cfg.accept(thread);
                    }
                    return thread;
                }
            };
        }
    }
}
