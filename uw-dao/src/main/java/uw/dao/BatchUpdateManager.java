package uw.dao;

import java.util.List;
import java.util.Map;

/**
 * 批量更新的管理类.
 */
public interface BatchUpdateManager {

    /**
     * 设置批量更新的数量.
     *
     * @param size 批量更新的数量
     */
    void setBatchSize(int size);

    /**
     * 获得批量更新的数量.
     *
     * @return 获得批量更新的数量
     */
    int getBatchSize();

    /**
     * 获得Batch的sql列表.
     *
     * @return Batch的sql列表
     */
    List<String> getBatchList();

    /**
     * 提交该事务.
     *
     * @return 执行结果
     * @throws TransactionException 事务异常
     */
    Map<String, List<Integer>> submit() throws TransactionException;

}
