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
     * 通过SeqName获取当前ID。
     *
     * @return
     */
    public static long getCurrentId(String seqName) {
        if (DaoConfigManager.getConfig().getRedis() != null) {
            return FusionSequenceFactory.getCurrentId( seqName );
        } else {
            return DaoSequenceFactory.getCurrentId( seqName );
        }
    }

    /**
     * 通过传入entityClass名，获取当前ID.
     *
     * @param cls
     * @return
     */
    public static long getCurrentId(Class<?> cls) {
        if (DaoConfigManager.getConfig().getRedis() != null) {
            return FusionSequenceFactory.getCurrentId( cls.getSimpleName() );
        } else {
            return DaoSequenceFactory.getCurrentId( cls.getSimpleName() );
        }
    }

    /**
     * 通过SeqName获取主键ID。
     *
     * @return
     */
    public static long getSequenceId(String seqName) {
        if (DaoConfigManager.getConfig().getRedis() != null) {
            return FusionSequenceFactory.getSequenceId( seqName );
        } else {
            return DaoSequenceFactory.getSequenceId( seqName );
        }
    }

    /**
     * 通过传入entityClass名，来获取主键ID.
     *
     * @param cls
     * @return
     */
    public static long getSequenceId(Class<?> cls) {
        if (DaoConfigManager.getConfig().getRedis() != null) {
            return FusionSequenceFactory.getSequenceId( cls.getSimpleName() );
        } else {
            return DaoSequenceFactory.getSequenceId( cls.getSimpleName() );
        }
    }

    /**
     * 重置SequenceId。
     *
     */
    public static void resetSequenceId(String seqName, long seqId) {
        if (DaoConfigManager.getConfig().getRedis() != null) {
            FusionSequenceFactory.resetSequenceId( seqName, seqId );
        } else {
            DaoSequenceFactory.resetSequenceId( seqName, seqId, 1 );
        }
    }

    /**
     * 重置SequenceId。
     *
     */
    public static void resetSequenceId(Class<?> cls, long seqId) {
        if (DaoConfigManager.getConfig().getRedis() != null) {
            FusionSequenceFactory.resetSequenceId( cls.getSimpleName(), seqId );
        } else {
            DaoSequenceFactory.resetSequenceId( cls.getSimpleName(), seqId, 1 );
        }
    }

}