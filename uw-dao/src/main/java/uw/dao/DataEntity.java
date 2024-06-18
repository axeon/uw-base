package uw.dao;

import java.util.Set;

/**
 * 用于dao的save/update操作。 如果需要做save/update操作，则entity必须要实现该接口.
 *
 * @author axeon
 */
public interface DataEntity {

    /**
     * 获得更改的字段列表。
     *
     * @return 字段列表
     */
    Set<String> GET_UPDATED_COLUMN();

    /**
     * 获得文本变更信息。
     *
     * @return String
     */
    String GET_UPDATED_INFO();

    /**
     * 清理更新信息，同时清除更新列信息。
     */
    void CLEAR_UPDATED_INFO();
}
