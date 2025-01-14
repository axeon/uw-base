package uw.app.common.dto;

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
     * 指定saasId,id,state的构造器。
     *
     * @param saasId
     * @param id
     * @param state
     */
    public AuthIdStateQueryParam(Long saasId, Long id, Integer state) {
        this.saasId = saasId;
        this.id = id;
        this.state = state;
    }

    /**
     * 指定saasId,id,states的构造器。
     *
     * @param saasId
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
     * @param id
     * @param state
     */
    public AuthIdStateQueryParam(Long id, Integer state) {
        this.id = id;
        this.state = state;
        bindSaasId();
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
        bindSaasId();
    }

    /**
     * 允许的排序属性。
     * key:排序名 value:排序字段
     *
     * @return
     */
    @Override
    public Map<String, String> ALLOWED_SORT_PROPERTY() {
        return Map.of( "id", "id" );
    }

    /**
     * 当前QueryParam填入当前用户的userId。
     */
    public AuthIdStateQueryParam bindUserId() {
        AuthTokenData tokenData = getAuthToken();
        setUserId( tokenData.getUserId() );
        return this;
    }

    /**
     * 当前QueryParam填入当前用户的mchId。
     */
    public AuthIdStateQueryParam bindMchId() {
        AuthTokenData tokenData = getAuthToken();
        setMchId( tokenData.getMchId() );
        return this;
    }

    /**
     * 当前QueryParam填入当前用户的userType。
     */
    public AuthIdStateQueryParam bindUserType() {
        AuthTokenData tokenData = getAuthToken();
        setUserType( tokenData.getUserType() );
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
     * 当前QueryParam填入当前用户的saasId。
     */
    private AuthIdStateQueryParam bindSaasId() {
        AuthTokenData tokenData = getAuthToken();
        if (tokenData.getUserType() < UserType.RPC.getValue() || tokenData.getUserType() > UserType.ADMIN.getValue()) {
            setSaasId( tokenData.getSaasId() );
        }
        return this;
    }

    /**
     * 获得AuthToken。
     *
     * @return
     */
    private AuthTokenData getAuthToken() {
        AuthTokenData authToken = AuthServiceHelper.getContextToken();
        if (authToken == null) {
            throw new UnsupportedOperationException( "AuthServiceHelper must be run in auth-service web environment!" );
        }
        return authToken;
    }


}
