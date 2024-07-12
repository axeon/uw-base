package uw.app.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import uw.auth.service.AuthServiceHelper;
import uw.auth.service.constant.UserType;
import uw.auth.service.token.AuthTokenData;
import uw.dao.QueryParam;
import uw.dao.annotation.QueryMeta;

/**
 * 自带验证信息的查询参数类。
 * 自带了saasId, mchId, userId, userType属性。
 */
public class AuthIdStateQueryParam extends QueryParam {

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
     * @param id
     * @param states
     */
    public AuthIdStateQueryParam(Long id, Integer... states) {
        this.id = id;
        this.states = states;
        bindSaasId();
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


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public Integer[] getStates() {
        return states;
    }

    public void setStates(Integer[] states) {
        this.states = states;
    }

    public Long getSaasId() {
        return saasId;
    }

    public void setSaasId(Long saasId) {
        this.saasId = saasId;
    }

    public AuthIdStateQueryParam saasId(Long saasId) {
        this.saasId = saasId;
        return this;
    }

    public Long getMchId() {
        return mchId;
    }

    public void setMchId(Long mchId) {
        this.mchId = mchId;
    }

    public AuthIdStateQueryParam mchId(Long mchId) {
        this.mchId = mchId;
        return this;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public AuthIdStateQueryParam userId(Long userId) {
        this.userId = userId;
        return this;
    }

    public Integer getUserType() {
        return userType;
    }

    public void setUserType(Integer userType) {
        this.userType = userType;
    }

    public AuthIdStateQueryParam userType(Integer userType) {
        this.userType = userType;
        return this;
    }

    /**
     * 当前QueryParam填入当前用户的saasId
     */
    private AuthIdStateQueryParam bindSaasId() {
        AuthTokenData tokenData = getAuthToken();
        if (userType < UserType.RPC.getValue() || userType > UserType.ADMIN.getValue()) {
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
            throw new UnsupportedOperationException( "AuthServiceHelper must be run in web environment!" );
        }
        return authToken;
    }


}
