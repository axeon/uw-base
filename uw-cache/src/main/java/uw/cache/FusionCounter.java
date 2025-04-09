package uw.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.cache.util.RedisKeyUtils;
import uw.common.util.SystemClock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

/**
 * 基于AtomicLong和redis的复合计数器。
 */
public class FusionCounter {

    private static final Logger log = LoggerFactory.getLogger(FusionCounter.class);

    /**
     * 默认同步全局时间间隔为1分钟。
     */
    private static final long DEFAULT_SYNC_GLOBAL_MILLIS = 60_000L;

    /**
     * key是缓存名，value是config实例。
     */
    private static final ConcurrentMap<String, LocalCounter.Config> configMap = new ConcurrentHashMap<>();

    /**
     * key是缓存名，value是Cache实例。
     */
    private static final ConcurrentMap<String, LocalCounter> counterMap = new ConcurrentHashMap<>();

    /**
     * 虚拟线程执行器。
     */
    private static final ExecutorService executorService = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("uw-counter").factory());

    /**
     * 配置计数器。
     *
     * @param entityType       entity类型(主要用于构造counterType)。
     * @param syncGlobalMillis 同步全局间隔毫秒数
     */
    public static void config(final Class entityType, final long syncGlobalMillis) {
        config(entityType.getSimpleName(), syncGlobalMillis);
    }


    /**
     * 初始化计数器。
     *
     * @param counterType      计数器类型。
     * @param syncGlobalMillis 同步全局间隔毫秒数
     */
    public static void config(final String counterType, final long syncGlobalMillis) {
        configMap.put(counterType, new LocalCounter.Config(counterType, syncGlobalMillis, 0));
    }

    /**
     * 初始化计数器。
     *
     * @param entityType       entity类型(主要用于构造counterType)。
     * @param syncGlobalMillis 同步全局间隔毫秒数
     * @param writeBackMillis  回写数据库间隔毫秒数
     * @param callbackConsumer 回写函数，可以在此函数中写入数据库。
     */
    public static void config(final Class entityType, final long syncGlobalMillis, final long writeBackMillis, final BiConsumer<Object, Long> callbackConsumer) {
        config(entityType.getSimpleName(), syncGlobalMillis, writeBackMillis, callbackConsumer);
    }

    /**
     * 初始化计数器。
     *
     * @param counterType       计数器类型。
     * @param syncGlobalMillis  同步全局间隔毫秒数
     * @param writeBackMillis   回写数据库间隔毫秒数
     * @param writeBackConsumer 回写函数，可以在此函数中写入数据库。
     */
    public static void config(final String counterType, final long syncGlobalMillis, final long writeBackMillis, final BiConsumer<Object, Long> writeBackConsumer) {
        configMap.put(counterType, new LocalCounter.Config(counterType, syncGlobalMillis, writeBackMillis, writeBackConsumer));
    }

    /**
     * 增加计数。
     *
     * @param entityType entity类型(主要用于构造counterType)。
     * @param counterId  计数器ID
     */
    public static long increment(Class entityType, Object counterId) {
        return increment(entityType.getSimpleName(), counterId);
    }

    /**
     * 增加计数。
     *
     * @param counterType 计数器类型。
     * @param counterId   计数器ID
     */
    public static long increment(String counterType, Object counterId) {
        return increment(counterType, counterId, 1);
    }

    /**
     * 增加计数。
     *
     * @param entityType   entity类型(主要用于构造counterType)。
     * @param counterId    计数器ID
     * @param incrementNum 增加的计数。
     */
    public static long increment(Class entityType, Object counterId, long incrementNum) {
        return increment(entityType.getSimpleName(), counterId, incrementNum);
    }

    /**
     * 增加计数。
     *
     * @param counterType  计数器类型。
     * @param counterId    计数器ID
     * @param incrementNum 增加的计数。
     */
    public static long increment(String counterType, Object counterId, long incrementNum) {
        LocalCounter localCounter = counterMap.computeIfAbsent(counterType + RedisKeyUtils.KEY_SPLITTER + counterId,
                key -> new LocalCounter(configMap.computeIfAbsent(counterType, config -> new LocalCounter.Config(counterType, DEFAULT_SYNC_GLOBAL_MILLIS, 0)), counterId,
                        0));
        return localCounter.increment(incrementNum);
    }

    /**
     * 减少计数。
     *
     * @param entityType entity类型(主要用于构造counterType)。
     * @param counterId  计数器ID
     */
    public static long decrement(Class entityType, Object counterId) {
        return decrement(entityType.getSimpleName(), counterId);
    }

    /**
     * 减少计数。
     *
     * @param counterType 计数器类型。
     * @param counterId   计数器ID
     */
    public static long decrement(String counterType, Object counterId) {
        return decrement(counterType, counterId, 1);
    }

    /**
     * 减少计数。
     *
     * @param entityType   entity类型(主要用于构造counterType)。
     * @param counterId    计数器ID
     * @param incrementNum 增加的计数。
     */
    public static long decrement(Class entityType, Object counterId, long incrementNum) {
        return decrement(entityType.getSimpleName(), counterId, incrementNum);
    }

    /**
     * 减少计数。
     *
     * @param counterType  计数器类型。
     * @param counterId    计数器ID
     * @param incrementNum 增加的计数。
     */
    public static long decrement(String counterType, Object counterId, long incrementNum) {
        LocalCounter localCounter = counterMap.computeIfAbsent(counterType + RedisKeyUtils.KEY_SPLITTER + counterId,
                key -> new LocalCounter(configMap.computeIfAbsent(counterType, config -> new LocalCounter.Config(counterType, DEFAULT_SYNC_GLOBAL_MILLIS, 0)), counterId,
                        0));
        return localCounter.decrement(incrementNum);
    }


    /**
     * 一般用于计数器初始设置。
     * 设置初始值后，将会自动同步redis数值。
     *
     * @param entityType entity类型(主要用于构造counterType)。
     * @param counterId  计数器ID
     * @param initNum    初始值。
     */
    public static LocalCounter init(Class entityType, Object counterId, long initNum) {
        return init(entityType.getSimpleName(), counterId, initNum);
    }

    /**
     * 设置计数器数值。
     * 设置初始值后，将会自动同步redis数值。
     *
     * @param counterType 计数器类型。
     * @param counterId   计数器ID
     * @return 计数数值。
     */
    public static LocalCounter init(String counterType, Object counterId, long initNum) {
        return counterMap.computeIfAbsent(counterType + RedisKeyUtils.KEY_SPLITTER + counterId, key -> new LocalCounter(configMap.computeIfAbsent(counterType,
                config -> new LocalCounter.Config(counterType, DEFAULT_SYNC_GLOBAL_MILLIS, 0)), counterId, initNum));
    }

    /**
     * 获得计数器数值。
     *
     * @param entityType entity类型(主要用于构造counterType)。
     * @param counterId  计数器ID
     * @return 计数数值。
     */
    public static long get(Class entityType, Object counterId) {
        return get(entityType.getSimpleName(), counterId);
    }

    /**
     * 获得计数器数值。
     *
     * @param counterType 计数器类型。
     * @param counterId   计数器ID
     * @return 计数数值。
     */
    public static long get(String counterType, Object counterId) {
        return get(counterType, counterId, false);
    }

    /**
     * 获得计数器数值。
     *
     * @param entityType entity类型(主要用于构造counterType)。
     * @param counterId  计数器ID
     * @param forceSync  强制同步。
     * @return 计数数值。
     */
    public static long get(Class entityType, Object counterId, boolean forceSync) {
        return get(entityType.getSimpleName(), counterId, forceSync);
    }

    /**
     * 获得计数器数值。
     *
     * @param counterType 计数器类型。
     * @param counterId   计数器ID
     * @param forceSync   强制同步。
     * @return 计数数值。
     */
    public static long get(String counterType, Object counterId, boolean forceSync) {
        LocalCounter localCounter = counterMap.computeIfAbsent(counterType + RedisKeyUtils.KEY_SPLITTER + counterId,
                key -> new LocalCounter(configMap.computeIfAbsent(counterType, config -> new LocalCounter.Config(counterType, DEFAULT_SYNC_GLOBAL_MILLIS, 0)), counterId,
                        0));
        if (forceSync) {
            localCounter.sync(true);
        }
        return localCounter.get();
    }

    /**
     * 删除计数器。
     *
     * @param entityType entity类型(主要用于构造counterType)。
     * @param counterId  计数器ID
     * @return 是否成功
     */
    public static boolean delete(Class entityType, Object counterId) {
        return delete(entityType.getSimpleName(), counterId);
    }

    /**
     * 删除计数器。
     *
     * @param counterType 计数器类型。
     * @param counterId   计数器ID
     * @return 是否成功
     */
    public static boolean delete(String counterType, Object counterId) {
        LocalCounter localCounter = counterMap.remove(counterType + RedisKeyUtils.KEY_SPLITTER + counterId);
        if (localCounter != null) {
            localCounter.sync(true);
        }
        return GlobalCounter.delete(counterType, counterId);
    }


    /**
     * 获取数值后删除计数器。
     *
     * @param entityType entity类型(主要用于构造counterType)。
     * @param counterId  计数器ID
     * @return 计数数值
     */
    public static long getAndDelete(Class entityType, Object counterId) {
        return getAndDelete(entityType.getSimpleName(), counterId);
    }

    /**
     * 获取数值后删除计数器。
     *
     * @param counterType 计数器类型。
     * @param counterId   计数器ID
     * @return 计数数值
     */
    public static long getAndDelete(String counterType, Object counterId) {
        LocalCounter localCounter = counterMap.remove(counterType + RedisKeyUtils.KEY_SPLITTER + counterId);
        if (localCounter != null) {
            localCounter.sync(true);
        }
        return GlobalCounter.getAndDelete(counterType, counterId);
    }

    /**
     * 本地计数器。
     */
    private static class LocalCounter {

        /**
         * 计数器配置。
         */
        private Config config;

        /**
         * 计数器ID。
         */
        private Object counterId;

        /**
         * 上次同步时间。
         */
        private long lastSyncTime = 0;

        /**
         * 上次同步数值。
         */
        private long lastSyncNum = 0;

        /**
         * 递增计数器
         */
        private AtomicLong counter = new AtomicLong();

        public LocalCounter(Config config, Object counterId, long initNum) {
            this.config = config;
            this.counterId = counterId;
            init(initNum);
        }

        /**
         * 获得当前计数。
         *
         * @return
         */
        public long get() {
            return lastSyncNum + counter.get();
        }

        /**
         * increment
         *
         * @param incrementNum
         */
        public long increment(long incrementNum) {
            long num = counter.addAndGet(incrementNum);
            sync(false);
            return lastSyncNum + num;
        }

        /**
         * decrement
         *
         * @param incrementNum
         */
        public long decrement(long incrementNum) {
            long num = counter.addAndGet(-incrementNum);
            sync(false);
            return lastSyncNum + num;
        }

        /**
         * 检查同步。
         */
        public void sync(boolean forceSync) {
            long now = SystemClock.now();
            if (forceSync || now > lastSyncTime + config.syncGlobalMillis) {
                long num = 0;
                //此处代码用于控制线程并发
                if ((num = counter.getAndSet(0)) != 0) {
                    lastSyncNum = GlobalCounter.increment(config.counterType, counterId, num);
                    //异步执行回写函数
                    if (config.writeBackConsumer != null && now > lastSyncTime + config.syncGlobalMillis) {
                        executorService.submit(() -> config.writeBackConsumer.accept(counterId, lastSyncNum));
                    }
                    //设置同步时间
                    lastSyncTime = now;
                }
            }
        }

        /**
         * 初始化操作,初始化本地num。
         */
        private void init(long initNum) {
            if (initNum != 0 && GlobalCounter.setIfAbsent(config.counterType, counterId, initNum)) {
                lastSyncNum = initNum;
            } else {
                lastSyncNum = GlobalCounter.get(config.counterType, counterId);
            }
            lastSyncTime = SystemClock.now();
        }

        private static class Config {
            /**
             * 计数器类型。
             */
            private String counterType;

            /**
             * 同步redis时间间隔。
             */
            private long syncGlobalMillis;

            /**
             * 回写数据库时间间隔。
             */
            private long writeBackMillis;

            /**
             * 回写库表的方法。
             */
            private BiConsumer<Object, Long> writeBackConsumer;

            public Config(String counterType, long syncGlobalMillis, long writeBackMillis) {
                this.counterType = counterType;
                this.syncGlobalMillis = syncGlobalMillis;
                this.writeBackMillis = writeBackMillis;
            }

            public Config(String counterType, long syncGlobalMillis, long writeBackMillis, BiConsumer<Object, Long> writeBackConsumer) {
                this.counterType = counterType;
                this.syncGlobalMillis = syncGlobalMillis;
                this.writeBackMillis = writeBackMillis;
                this.writeBackConsumer = writeBackConsumer;
            }
        }

    }

}
