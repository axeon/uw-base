package uw.common.app.dto;

import uw.dao.QueryParam;
import uw.dao.annotation.QueryMeta;

import java.util.Map;

/**
 * 基于id查询参数。
 */
public class IdQueryParam extends QueryParam<IdQueryParam> {

    /**
     * id匹配。
     */
    @QueryMeta(expr = "id=?")
    private Long id;

    /**
     * 指定id构造器。
     *
     * @param id
     */
    public IdQueryParam(Long id) {
        this.id = id;
    }

    /**
     * 允许的排序属性。
     * key:排序名 value:排序字段
     *
     * @return
     */
    @Override
    public Map<String, String> ALLOWED_SORT_PROPERTY() {
        return Map.of("id", "id");
    }

    /**
     * 获取id。
     *
     * @return
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置id。
     *
     * @param id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 设置id。
     *
     * @param id
     * @return
     */
    public IdQueryParam id(Long id) {
        this.id = id;
        return this;
    }

}
