package uw.common.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import uw.dao.QueryParam;
import uw.dao.annotation.QueryMeta;

import java.util.Map;

/**
 * 基于id查询参数。
 */
public class IdQueryParam extends QueryParam<IdQueryParam> {

    /**
     * ID。
     */
    @QueryMeta(expr = "id=?")
    @Schema(title="ID", description = "ID")
    private Long id;


    /**
     * 数组ID。
     */
    @QueryMeta(expr = "id in (?)")
    @Schema(title="ID数组", description = "ID数组，可同时匹配多个。")
    private Long[] ids;

    /**
     * 构造器。
     */
    public IdQueryParam() {
    }

    /**
     * 指定id构造器。
     *
     * @param id
     */
    public IdQueryParam(Long id) {
        this.id = id;
    }

    /**
     * 指定id数组构造器。
     *
     * @param ids
     */
    public IdQueryParam(Long[] ids) {
        this.ids = ids;
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
     * 获取ID。
     */
    public Long getId() {
        return this.id;
    }

    /**
     * 设置ID。
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 设置ID链式调用。
     */
    public IdQueryParam id(Long id) {
        setId(id);
        return this;
    }

    /**
     * 获取数组ID。
     */
    public Long[] getIds() {
        return this.ids;
    }

    /**
     * 设置数组ID。
     */
    public void setIds(Long[] ids) {
        this.ids = ids;
    }

    /**
     * 设置数组ID链式调用。
     */
    public IdQueryParam ids(Long[] ids) {
        setIds(ids);
        return this;
    }


}
