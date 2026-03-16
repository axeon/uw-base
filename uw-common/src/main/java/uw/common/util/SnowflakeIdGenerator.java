package uw.common.util;

import java.util.UUID;

/**
 * SnowflakeIdGenerator 是基于Snowflake算法的唯一ID生成器。
 * 生成的ID由时间戳、机器ID（通过UUID生成）和序列号组成，保证在分布式环境中的全局唯一性。
 * 该实现支持高并发情况下的ID生成，避免了依赖中心化服务。
 * 为了适配k8s和docker环境，使用了UUID替换了IP。
 */
public class SnowflakeIdGenerator {

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

    private long machineId; // 机器ID
    private long sequence = 0L; // 序列号
    private long lastTimestamp = -1L; // 上次生成ID的时间戳

    /**
     * 私有构造函数，通过UUID生成机器ID
     * 将UUID的高64位作为机器ID，并确保其位数在最大机器ID范围内。
     */
    public SnowflakeIdGenerator() {
        // 使用UUID生成机器ID，这里取UUID的低10位作为机器ID
        this.machineId = UUID.randomUUID().getMostSignificantBits() & MAX_MACHINE_ID;
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
