package uw.common.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import uw.auth.service.AuthServiceHelper;
import uw.auth.service.constant.UserType;
import uw.auth.service.token.AuthTokenData;
import uw.dao.QueryParam;
import uw.dao.annotation.QueryMeta;

import java.util.Map;

/**
 * 自带验证信息的查询参数类。
 * 自带了saasId, mchId, userId, userType属性。
 */
public class AuthIdStateQueryParam extends QueryParam<AuthIdStateQueryParam> {

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
     * 单一状态匹配。
     */
    @QueryMeta(expr = "state=?")
    @Schema(title = "状态", description = "状态，可同时匹配多个状态。")
    private Integer state;

    /**
     * 多状态匹配。
     */
    @QueryMeta(expr = "state in (?)")
    @Schema(title = "状态数组", description = "状态，可同时匹配多个状态。")
    private Integer[] states;

    /**
     * 大于等于状态值: -1: 删除 0: 冻结 1: 正常。
     */
    @QueryMeta(expr = "state>=?")
    @Schema(title = "大于等于状态值: -1: 删除 0: 冻结 1: 正常", description = "大于等于状态值: -1: 删除 0: 冻结 1: 正常")
    private Integer stateGte;

    /**
     * 小于等于状态值: -1: 删除 0: 冻结 1: 正常。
     */
    @QueryMeta(expr = "state<=?")
    @Schema(title = "小于等于状态值: -1: 删除 0: 冻结 1: 正常", description = "小于等于状态值: -1: 删除 0: 冻结 1: 正常")
    private Integer stateLte;

    /**
     * saasId。
     */
    @QueryMeta(expr = "saas_id=?")
    @Schema(title = "saasId", description = "saasId", hidden = true)
    private Long saasId;

    /**
     * 商户id。
     */
    @QueryMeta(expr = "mch_id=?")
    @Schema(title = "mchId", description = "mchId", hidden = true)
    private Long mchId;

    /**
     * 用户id。
     */
    @QueryMeta(expr = "user_id=?")
    @Schema(title = "userId", description = "userId", hidden = true)
    private Long userId;

    /**
     * 用户类型。
     */
    @QueryMeta(expr = "user_type=?")
    @Schema(title = "userType", description = "userType", hidden = true)
    private Integer userType;

    /**
     * 构造器。
     */
    public AuthIdStateQueryParam() {
    }

    /**
     * 指定id构造器。
     *
     * @param id
     */
    public AuthIdStateQueryParam(Long saasId, Long id) {
        this.saasId = saasId;
        this.id = id;
    }

    /**
     * 指定id数组构造器。
     *
     * @param ids
     */
    public AuthIdStateQueryParam(Long saasId, Long[] ids) {
        this.saasId = saasId;
        this.ids = ids;
    }

    /**
     * 指定id,state的构造器。
     * 如果不在web环境下运行，将会抛错。
     *
     * @param id
     * @param state
     */
    public AuthIdStateQueryParam(Long saasId, Long id, Integer state) {
        this.saasId = saasId;
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
    public AuthIdStateQueryParam(Long saasId, Long id, Integer... states) {
        this.saasId = saasId;
        this.id = id;
        this.states = states;
    }

    /**
     * 指定id,state的构造器。
     * 如果不在web环境下运行，将会抛错。
     *
     * @param ids
     * @param state
     */
    public AuthIdStateQueryParam(Long saasId, Long[] ids, Integer state) {
        this.saasId = saasId;
        this.ids = ids;
        this.state = state;
    }

    /**
     * 指定id,states的构造器。
     * 如果不在web环境下运行，将会抛错。
     *
     * @param ids
     * @param states
     */
    public AuthIdStateQueryParam(Long saasId, Long[] ids, Integer... states) {
        this.saasId = saasId;
        this.ids = ids;
        this.states = states;
    }

    /**
     * 指定id构造器。
     *
     * @param id
     */
    public AuthIdStateQueryParam(Long id) {
        this.id = id;
    }

    /**
     * 指定id数组构造器。
     *
     * @param ids
     */
    public AuthIdStateQueryParam(Long[] ids) {
        this.ids = ids;
    }

    /**
     * 指定id,state的构造器。
     * 如果不在web环境下运行，将会抛错。
     *
     * @param id
     * @param state
     */
    public AuthIdStateQueryParam(Long id, Integer state) {
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
    public AuthIdStateQueryParam(Long id, Integer... states) {
        this.id = id;
        this.states = states;
    }

    /**
     * 指定id,state的构造器。
     * 如果不在web环境下运行，将会抛错。
     *
     * @param ids
     * @param state
     */
    public AuthIdStateQueryParam(Long[] ids, Integer state) {
        this.ids = ids;
        this.state = state;
    }

    /**
     * 指定id,states的构造器。
     * 如果不在web环境下运行，将会抛错。
     *
     * @param ids
     * @param states
     */
    public AuthIdStateQueryParam(Long[] ids, Integer... states) {
        this.ids = ids;
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
     * 当前QueryParam填入当前用户的saasId。
     */
    public AuthIdStateQueryParam bindSaasId() {
        AuthTokenData tokenData = getAuthToken();
        if (tokenData.getUserType() < UserType.RPC.getValue() || tokenData.getUserType() > UserType.ADMIN.getValue()) {
            setSaasId(tokenData.getSaasId());
        }
        return this;
    }

    /**
     * 当前QueryParam填入当前用户的userId。
     */
    public AuthIdStateQueryParam bindUserId() {
        AuthTokenData tokenData = getAuthToken();
        setUserId(tokenData.getUserId());
        return this;
    }

    /**
     * 当前QueryParam填入当前用户的mchId。
     */
    public AuthIdStateQueryParam bindMchId() {
        AuthTokenData tokenData = getAuthToken();
        setMchId(tokenData.getMchId());
        return this;
    }

    /**
     * 当前QueryParam填入当前用户的userType。
     */
    public AuthIdStateQueryParam bindUserType() {
        AuthTokenData tokenData = getAuthToken();
        setUserType(tokenData.getUserType());
        return this;
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
    public AuthIdStateQueryParam id(Long id) {
        this.id = id;
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
    public AuthIdStateQueryParam ids(Long[] ids) {
        setIds(ids);
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
    public AuthIdStateQueryParam state(Integer state) {
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
    public AuthIdStateQueryParam states(Integer[] states) {
        this.states = states;
        return this;
    }

    /**
     * 获取大于等于-1: 删除; 0: 冻结; 1: 启用。
     */
    public Integer getStateGte(){
        return this.stateGte;
    }

    /**
     * 设置大于等于-1: 删除; 0: 冻结; 1: 启用。
     */
    public void setStateGte(Integer stateGte){
        this.stateGte = stateGte;
    }

    /**
     * 设置大于等于-1: 删除; 0: 冻结; 1: 启用链式调用。
     */
    public AuthIdStateQueryParam stateGte(Integer stateGte) {
        setStateGte(stateGte);
        return this;
    }

    /**
     * 获取小于等于-1: 删除; 0: 冻结; 1: 启用。
     */
    public Integer getStateLte(){
        return this.stateLte;
    }

    /**
     * 获取小于等于-1: 删除; 0: 冻结; 1: 启用。
     */
    public void setStateLte(Integer stateLte){
        this.stateLte = stateLte;
    }

    /**
     * 获取小于等于-1: 删除; 0: 冻结; 1: 启用链式调用。
     */
    public AuthIdStateQueryParam stateLte(Integer stateLte) {
        setStateLte(stateLte);
        return this;
    }

    /**
     * 获取saasId。
     *
     * @return
     */
    public Long getSaasId() {
        return saasId;
    }

    /**
     * 设置saasId。
     *
     * @param saasId
     */
    public void setSaasId(Long saasId) {
        this.saasId = saasId;
    }

    /**
     * 设置saasId。
     *
     * @param saasId
     * @return
     */
    public AuthIdStateQueryParam saasId(Long saasId) {
        this.saasId = saasId;
        return this;
    }

    /**
     * 获取商户id。
     *
     * @return
     */
    public Long getMchId() {
        return mchId;
    }

    /**
     * 设置商户id。
     *
     * @param mchId
     */
    public void setMchId(Long mchId) {
        this.mchId = mchId;
    }

    /**
     * 设置商户id。
     *
     * @param mchId
     * @return
     */
    public AuthIdStateQueryParam mchId(Long mchId) {
        this.mchId = mchId;
        return this;
    }

    /**
     * 获取用户id。
     *
     * @return
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置用户id。
     *
     * @param userId
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 设置用户id。
     *
     * @param userId
     * @return
     */
    public AuthIdStateQueryParam userId(Long userId) {
        this.userId = userId;
        return this;
    }

    /**
     * 获取用户类型。
     *
     * @return
     */
    public Integer getUserType() {
        return userType;
    }

    /**
     * 设置用户类型。
     *
     * @param userType
     */
    public void setUserType(Integer userType) {
        this.userType = userType;
    }

    /**
     * 设置用户类型。
     *
     * @param userType
     * @return
     */
    public AuthIdStateQueryParam userType(Integer userType) {
        this.userType = userType;
        return this;
    }

    /**
     * 获取AuthToken。
     *
     * @return
     */
    private AuthTokenData getAuthToken() {
        AuthTokenData authToken = AuthServiceHelper.getContextToken();
        if (authToken == null) {
            throw new UnsupportedOperationException("AuthServiceHelper must be run in auth-service web environment!");
        }
        return authToken;
    }


}
