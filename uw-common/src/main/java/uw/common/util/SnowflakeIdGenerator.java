package uw.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SnowflakeIdGenerator 是基于Snowflake算法的唯一ID生成器。
 * 生成的ID由时间戳、机器ID和序列号组成，保证在分布式环境中的全局唯一性。
 * 该实现支持高并发情况下的ID生成，避免了依赖中心化服务。
 * <p>
 * machineId分配策略（按优先级）：
 * 1. 环境变量 MACHINE_ID（K8s中可通过Pod Ordinal或Downward API注入）
 * 2. HOSTNAME的稳定hash（容器/Pod名称，同一实例重启后保持一致）
 * 3. 降级到UUID随机（仅当以上均不可用时）
 */
public class SnowflakeIdGenerator {

    private static final Logger log = LoggerFactory.getLogger(SnowflakeIdGenerator.class);

    // 配置
    private static final long START_TIMESTAMP = 1735660800_000L; // 自定义起始时间（2025年1月1日）
    private static final long MACHINE_ID_BITS = 10L; // 机器ID的位数
    private static final long SEQUENCE_BITS = 12L; // 序列号的位数

    // 生成的ID的各个部分的位数
    private static final long MACHINE_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;

    // 最大值
    private static final long MAX_MACHINE_ID = ~(-1L << MACHINE_ID_BITS);
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    // 静态实例
    private static final SnowflakeIdGenerator INSTANCE = new SnowflakeIdGenerator();

    private final long machineId; // 机器ID
    private long sequence = 0L; // 序列号
    private long lastTimestamp = -1L; // 上次生成ID的时间戳

    /**
     * 构造函数，通过策略确定machineId。
     * 优先使用环境变量，其次使用HOSTNAME的hash，最后降级到随机。
     */
    public SnowflakeIdGenerator() {
        this.machineId = resolveMachineId();
    }

    /**
     * 按优先级解析machineId：
     * 1. 环境变量 MACHINE_ID
     * 2. HOSTNAME 的稳定hash
     * 3. 随机降级
     */
    private static long resolveMachineId() {
        // 优先级1：环境变量 MACHINE_ID
        String envMachineId = System.getenv("MACHINE_ID");
        if (envMachineId != null && !envMachineId.isEmpty()) {
            try {
                long id = Long.parseLong(envMachineId) & MAX_MACHINE_ID;
                log.info("Snowflake machineId from env MACHINE_ID: {}", id);
                return id;
            } catch (NumberFormatException e) {
                log.warn("Invalid MACHINE_ID env: {}, falling back to hostname", envMachineId);
            }
        }

        // 优先级2：HOSTNAME的稳定hash（Pod名称在K8s StatefulSet中是有序且稳定的）
        String hostname = System.getenv("HOSTNAME");
        if (hostname != null && !hostname.isEmpty()) {
            long id = stableHash(hostname) & MAX_MACHINE_ID;
            log.info("Snowflake machineId from HOSTNAME hash: {} (hostname={})", id, hostname);
            return id;
        }

        // 优先级3：降级到随机（单实例开发/测试场景）
        long id = (System.currentTimeMillis() ^ Thread.currentThread().getId()) & MAX_MACHINE_ID;
        log.info("Snowflake machineId from fallback: {}", id);
        return id;
    }

    /**
     * 对字符串做稳定的hash，确保相同输入始终产生相同输出。
     */
    private static long stableHash(String value) {
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < value.length(); i++) {
            hash ^= value.charAt(i);
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    /**
     * 获取SnowflakeIdGenerator的默认实例
     *
     * @return SnowflakeIdGenerator的单例实例
     */
    public static SnowflakeIdGenerator getInstance() {
        return INSTANCE;
    }

    /**
     * 生成一个唯一的ID
     *
     * @return 唯一ID
     */
    public synchronized long generateId() {
        long timestamp = System.currentTimeMillis(); // 当前时间戳

        // 如果当前时间小于上次生成ID的时间戳，说明发生时钟回退，抛出异常
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("时钟回退异常");
        }

        // 如果是同一毫秒，生成的序列号递增
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) { // 如果序列号已经溢出，等待下一毫秒
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        // 生成最终ID
        return (timestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT |
                (machineId << MACHINE_ID_SHIFT) |
                sequence;
    }

    /**
     * 等待下一毫秒，确保ID的唯一性
     *
     * @param lastTimestamp 上次生成ID的时间戳
     * @return 下一毫秒的时间戳
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * 测试生成10个ID
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 获取默认实例
        SnowflakeIdGenerator idGenerator = SnowflakeIdGenerator.getInstance();

        // 生成10个ID
        for (int i = 0; i < 2000; i++) {
            System.out.println(idGenerator.generateId());
        }
    }
}
