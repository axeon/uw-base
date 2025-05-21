package uw.dao;

import java.io.Serializable;
import java.util.Set;

/**
 * 用于dao的save/update操作。 如果需要做save/update操作，则entity必须要实现该接口.
 *
 * @author axeon
 */
public interface DataEntity {

    /**
     * 获取实体的表名。
     *
     * @return 表名
     */
    String ENTITY_TABLE();

    /**
     * 获取实体的名称。
     *
     * @return 实体名称
     */
    String ENTITY_NAME();

    /**
     * 获取实体的ID。
     *
     * @return 表名
     */
    Serializable ENTITY_ID();

    /**
     * 获取更新信息。
     *
     * @return String
     */
    DataUpdateInfo GET_UPDATED_INFO();

    /**
     * 清除更新信息。
     */
    void CLEAR_UPDATED_INFO();
}
