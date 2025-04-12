package uw.common.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import uw.dao.QueryParam;
import uw.dao.annotation.QueryMeta;

import java.util.Map;

/**
 * 自带验证信息的查询参数类。
 * 自带了saasId, mchId, userId, userType属性。
 */
public class IdStateQueryParam extends QueryParam<IdStateQueryParam> {

    /**
     * id匹配。
     */
    @QueryMeta(expr = "id=?")
    private Long id;

    /**
     * 单一状态匹配。
     */
    @QueryMeta(expr = "state=?")
    private Integer state;

    /**
     * 多状态匹配。
     */
    @QueryMeta(expr = "state in (?)")
    @Schema(title = "状态", description = "状态，可同时匹配多个状态。")
    private Integer[] states;

    /**
     * 指定id,state的构造器。
     * 如果不在web环境下运行，将会抛错。
     *
     * @param id
     * @param state
     */
    public IdStateQueryParam(Long id, Integer state) {
        this.id = id;
        this.state = state;
    }

    /**
     * 指定id,states的构造器。
     * 如果不在web环境下运行，将会抛错。
     *
     * @param id
     * @param states
     */
    public IdStateQueryParam(Long id, Integer... states) {
        this.id = id;
        this.states = states;
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
    public IdStateQueryParam id(Long id) {
        this.id = id;
        return this;
    }

    /**
     * 获取状态。
     *
     * @return
     */
    public Integer getState() {
        return state;
    }

    /**
     * 设置状态。
     *
     * @param state
     */
    public void setState(Integer state) {
        this.state = state;
    }

    /**
     * 设置state。
     *
     * @param state
     * @return
     */
    public IdStateQueryParam state(Integer state) {
        this.state = state;
        return this;
    }

    /**
     * 获取状态。
     *
     * @return
     */
    public Integer[] getStates() {
        return states;
    }

    /**
     * 设置状态。
     *
     * @param states
     */
    public void setStates(Integer[] states) {
        this.states = states;
    }

    /**
     * 设置状态。
     *
     * @param states
     */
    public IdStateQueryParam states(Integer[] states) {
        this.states = states;
        return this;
    }

}
