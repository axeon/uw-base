package uw.dao.vo;

import uw.dao.DataEntity;

import java.util.Set;

public class BaseDataEntity implements DataEntity {


    /**
     * 获得更改的字段列表。
     *
     * @return 字段列表
     */
    @Override
    public Set<String> GET_UPDATED_COLUMN() {
        return Set.of();
    }

    /**
     * 获得文本变更信息。
     *
     * @return String
     */
    @Override
    public String GET_UPDATED_INFO() {
        return "";
    }

    /**
     * 清理更新信息，同时清除更新列信息。
     */
    @Override
    public void CLEAR_UPDATED_INFO() {

    }
}
