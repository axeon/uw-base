package uw.dao;

import uw.dao.conf.DaoConfigManager;
import uw.dao.sequence.DaoSequenceFactory;
import uw.dao.sequence.FusionSequenceFactory;

/**
 * Sequence工厂类。
 * 通过配置文件动态决定使用 DaoSequenceFactory，还是 FusionSequenceFactory。
 * DaoSequenceFactory 和还是 FusionSequenceFactory 差异在于：
 * 1.FusionSequenceFactory 可以获取连续的Sequence数值，DaoSequenceFactory 集群环境下是不连续的。
 * 2.FusionSequenceFactory 默认配置下性能是 DaoSequenceFactory 的100倍。
 * 3.DaoSequenceFactory的incrementNum=100的时候和FusionSequenceFactory性能平衡点，超过100则性能大于FusionSequenceFactory。
 *
 */
public class SequenceFactory {


    /**
     * 通过SeqName获取当前ID（最近一次已发的号，不消耗序列；未初始化返回 0）。
     *
     * @param seqName 序列名
     * @return 当前已发号；未初始化返回 0
     */
    public static long getCurrentId(String seqName) {
        if (useFusion()) {
            return FusionSequenceFactory.getCurrentId(seqName);
        } else {
            return DaoSequenceFactory.getCurrentId(seqName);
        }
    }

    /**
     * 通过传入entityClass名，获取当前ID.
     *
     * @param cls 实体类（序列名取类简单名）
     * @return 当前已发号；未初始化返回 0
     */
    public static long getCurrentId(Class<?> cls) {
        if (useFusion()) {
            return FusionSequenceFactory.getCurrentId(cls.getSimpleName());
        } else {
            return DaoSequenceFactory.getCurrentId(cls.getSimpleName());
        }
    }

    /**
     * 通过SeqName获取主键ID。
     *
     * @param seqName 序列名
     * @return 下一个ID
     */
    public static long getSequenceId(String seqName) {
        if (useFusion()) {
            return FusionSequenceFactory.getSequenceId(seqName);
        } else {
            return DaoSequenceFactory.getSequenceId(seqName);
        }
    }

    /**
     * 通过传入entityClass名，来获取主键ID.
     *
     * @param cls 实体类（序列名取类简单名）
     * @return 下一个ID
     */
    public static long getSequenceId(Class<?> cls) {
        if (useFusion()) {
            return FusionSequenceFactory.getSequenceId(cls.getSimpleName());
        } else {
            return DaoSequenceFactory.getSequenceId(cls.getSimpleName());
        }
    }

    /**
     * 重置SequenceId。
     *
     */
    public static void resetSequenceId(String seqName, long seqId) {
        if (useFusion()) {
            FusionSequenceFactory.resetSequenceId(seqName, seqId);
        } else {
            DaoSequenceFactory.resetSequenceId(seqName, seqId, 1);
        }
    }

    /**
     * 重置SequenceId。
     *
     */
    public static void resetSequenceId(Class<?> cls, long seqId) {
        if (useFusion()) {
            FusionSequenceFactory.resetSequenceId(cls.getSimpleName(), seqId);
        } else {
            DaoSequenceFactory.resetSequenceId(cls.getSimpleName(), seqId, 1);
        }
    }


    /**
     * 判断是否启用 Fusion（Redis）发号器。
     * <p>条件：配置了 Redis 且 Fusion 已成功初始化（注入了 Redis 操作句柄）。
     * 当配置了 Redis 但初始化失败（Redis 不可用、构造异常）时，自动降级到 DaoSequenceFactory，
     * 保证全局发号不因 Redis 故障而瘫痪。</p>
     *
     * @return true 表示使用 FusionSequenceFactory
     */
    private static boolean useFusion() {
        return DaoConfigManager.getConfig() != null
                && DaoConfigManager.getConfig().getRedis() != null
                && FusionSequenceFactory.isAvailable();
    }

}