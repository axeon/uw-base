package uw.dao.sequence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import uw.common.util.SystemClock;

import java.util.concurrent.TimeUnit;

/**
 * 一个基于Redis优化的Sequence工厂类。
 * 此类可以和DaoSequenceFactory同步。
 */
public class FusionSequenceFactory {

    private static final Logger log = LoggerFactory.getLogger(FusionSequenceFactory.class);

    /**
     * redis key。
     */
    private static final String REDIS_SEQ_POOL = "uw-dao-seq-pool:";

    /**
     * redis前缀。
     */
    private static final String REDIS_SEQ = "uw-dao-seq:";

    /**
     * 重试次数。差不多超时50s左右。
     */
    private static final int MAX_RETRY_TIMES = 50;

    /**
     * 池子大小，每次从数据库中缓存1000个。
     */
    private static final int POOL_SIZE = 10000;

    /**
     * 池中最小容量。
     */
    private static final int POOL_MIN = 1000;

    /**
     * redis set 操作。
     */
    private static ValueOperations<String, Long> KV_OP;

    /**
     * redis set 操作。
     */
    private static SetOperations<String, Long> SET_OP;

    public FusionSequenceFactory(RedisTemplate<String, Long> daoRedisTemplate) {
        KV_OP = daoRedisTemplate.opsForValue();
        SET_OP = daoRedisTemplate.opsForSet();
    }

    /**
     * 通过SeqName获取当前主键ID。
     *
     * @return
     */
    public static long getCurrentId(Class<?> entityCls) {
        return getCurrentId(entityCls.getSimpleName());
    }

    /**
     * 通过SeqName获取主键ID。
     *
     * @return
     */
    public static long getSequenceId(Class<?> entityCls) {
        return getSequenceId(entityCls.getSimpleName());
    }


    /**
     * 通过SeqName获取当前主键ID。
     *
     * @return
     */
    public static long getCurrentId(String seqName) {
        for (int tryCount = 0; tryCount < MAX_RETRY_TIMES; tryCount++) {
            try {
                Long seqId = KV_OP.get(REDIS_SEQ + seqName);
                if (seqId == null) {
                    return 0L;
                } else {
                    return seqId;
                }
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
            log.warn("WARNING: FusionSequence[{}] failed to obtain getCurrentId. Trying {} times ...", seqName, tryCount);
            //休眠100ms，防止过多的无效冲击。
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
        throw new RuntimeException("FusionSequence[" + seqName + "] failed to obtain getCurrentId!!!");
    }

    /**
     * 通过SeqName获取主键ID。
     *
     * @return
     */
    public static long getSequenceId(String seqName) {
        for (int tryCount = 0; tryCount < MAX_RETRY_TIMES; tryCount++) {
            try {
                long seqId = KV_OP.increment(REDIS_SEQ + seqName, 1);
                //如果超过POOL_SIZE，需要同步到db seq库。
                if (seqId % POOL_SIZE == 1) {
                    //此时要从数据库中读取数值。
                    long dbSeqId = DaoSequenceFactory.allocateSequenceRange(seqName, POOL_SIZE);
                    if (seqId > dbSeqId) {
                        //当seq > daoSeq，需要同步到database。
                        log.warn("WARNING: FusionSequence[{}] redisSeq[{}] > dbSeq[{}]! 正在同步到database", seqName, seqId, dbSeqId);
                        long diff = seqId - dbSeqId;
                        //必须用allocateSequenceRange确保同步。
                        DaoSequenceFactory.allocateSequenceRange(seqName, diff + POOL_SIZE);
                    } else if (seqId < dbSeqId) {
                        //当seq <daoSeq，需要同步到redis。
                        log.warn("WARNING: FusionSequence[{}] redisSeq[{}] < dbSeq[{}]! 正在同步到redis", seqName, seqId, dbSeqId);
                        long diff = dbSeqId - seqId;
                        seqId = KV_OP.increment(REDIS_SEQ + seqName, diff);
                    }

                }
                return seqId;
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
            log.warn("WARNING: FusionSequence[{}] failed to obtain getSequenceId. Trying {} times ...", seqName, tryCount);
            //休眠100ms，防止过多的无效冲击。
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
        throw new RuntimeException("FusionSequence[" + seqName + "] failed to obtain getSequenceId!!!");
    }

    /**
     * 通过传入entityClass名，重置sequenceId.
     *
     * @param entityCls
     * @return
     */
    public static void resetSequenceId(Class<?> entityCls, long seqId) {
        resetSequenceId(entityCls.getSimpleName(), seqId);
    }

    /**
     * 重置sequenceId。
     * 1.重新设置redis。
     * 2.重新设置dao seq。
     *
     * @param seqName
     * @param seqId
     */
    public static synchronized void resetSequenceId(String seqName, long seqId) {
        int tryCount = 0;
        do {
            tryCount++;
            try {
                //重设seqId
                KV_OP.set(REDIS_SEQ + seqName, seqId);
                //先重设dao seq，有问题的话可以先抛异常，不会生效到redis。
                DaoSequenceFactory.resetSequenceId(seqName, seqId, 100);
                return;
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
            log.warn("WARNING: FusionSequence[{}] failed to resetSequenceId. Trying {} times...", seqName, tryCount);
            //休眠100ms，防止过多的无效冲击。
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        } while (tryCount < MAX_RETRY_TIMES);
        throw new RuntimeException("FusionSequence[" + seqName + "] failed to resetSequenceId!!!");
    }

    /**
     * 从池中随机分为一个id。
     * 对于系统中非常重要的ID，分配上希望更加慎重，同时ID也决定了数据的分布和分配。
     * 如果采用顺序性的Id分配，可能会导致如下问题：
     * 1.Id的规律性增长容易被竞争对手判断业务增长。
     * 2.Id的中断可能会浪费数据空间。
     * 3.规律性的Id在市场宣传中容易产生阶段性批量注册，导致大量无效数据，影响数据存储均衡。
     * <p>
     * 现在规划Id从10000开始，每批次1000个id（分布在10个库中，差不多一台物理机），id从这1000个id中随机获取，这样实际用户每个库中大约30-50个。
     * 1.在REDIS SET池中放置1000个ID。
     * 2.随机取出一个ID。
     * 3.如果REDIS SET池中余额少于100的时候，自动再放1000个ID。
     *
     * @return
     */
    public static long getRandomSequenceIdFromPool(Class<?> entityCls) {
        return getRandomSequenceIdFromPool(entityCls.getSimpleName());
    }

    /**
     * 从池中随机分为一个id。
     * 对于系统中非常重要的ID，分配上希望更加慎重，同时ID也决定了数据的分布和分配。
     * 如果采用顺序性的Id分配，可能会导致如下问题：
     * 1.Id的规律性增长容易被竞争对手判断业务增长。
     * 2.Id的中断可能会浪费数据空间。
     * 3.规律性的Id在市场宣传中容易产生阶段性批量注册，导致大量无效数据，影响数据存储均衡。
     * <p>
     * 现在规划Id从10000开始，每批次1000个id（分布在10个库中，差不多一台物理机），id从这1000个id中随机获取，这样实际用户每个库中大约30-50个。
     * 1.在REDIS SET池中放置1000个ID。
     * 2.随机取出一个ID。
     * 3.如果REDIS SET池中余额少于100的时候，自动再放1000个ID。
     *
     * @return
     */
    public static long getRandomSequenceIdFromPool(String seqName) {
        String redisKey = REDIS_SEQ_POOL + seqName;
        int tryCount = 0;
        do {
            tryCount++;
            long seqId;
            try {
                long size = SET_OP.size(redisKey);
                if (size > POOL_MIN) {
                    seqId = SET_OP.pop(redisKey);
                } else {
                    String redisLocker = redisKey + ":lock";
                    //加个一分钟的锁。
                    if (!KV_OP.setIfAbsent(redisLocker, SystemClock.now(), 10_000L, TimeUnit.MILLISECONDS)) {
                        continue;
                    }
                    //再次检查
                    size = SET_OP.size(redisKey);
                    if (size <= POOL_MIN) {
                        long saasId = DaoSequenceFactory.allocateSequenceRange(seqName, POOL_SIZE);
                        Long[] ids = new Long[1000];
                        for (int i = 0; i < 1000; i++) {
                            ids[i] = saasId + i;
                        }
                        SET_OP.add(redisKey, ids);
                    }
                    seqId = SET_OP.pop(redisKey);
                    //解锁
                    KV_OP.getAndDelete(redisLocker);
                }
                return seqId;
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
            log.warn("WARNING: FusionSequence[{}] failed to getRandomSequenceIdFromPool. Trying {} times ...", seqName, tryCount);
            //休眠100ms，防止过多的无效冲击。
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        } while (tryCount < MAX_RETRY_TIMES);
        log.error("ERROR: FusionSequence[{}] failed to getRandomSequenceIdFromPool. Trying {} times ...", seqName, tryCount);
        //如果执行到这里，直接报错吧。
        throw new RuntimeException("FusionSequence[" + seqName + "] failed to getRandomSequenceIdFromPool!!!");
    }

    /**
     * 回收一个seqId到池中，使其可以重新用于分配。
     * 常见于前台恶意注册，浪费了seqId。
     *
     * @param
     */
    public void restoreSequenceIdToPool(Class<?> entityCls, long seqId) {
        restoreSequenceIdToPool(entityCls.getSimpleName(), seqId);
    }

    /**
     * 回收一个seqId到池中，使其可以重新用于分配。
     * 常见于前台恶意注册，浪费了seqId。
     *
     * @param
     */
    public void restoreSequenceIdToPool(String seqName, long seqId) {
        SET_OP.add(REDIS_SEQ_POOL + seqName, seqId);
    }

}
