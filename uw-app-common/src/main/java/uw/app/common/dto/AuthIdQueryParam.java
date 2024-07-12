package uw.app.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import uw.auth.service.AuthServiceHelper;
import uw.auth.service.constant.UserType;
import uw.auth.service.token.AuthTokenData;
import uw.dao.QueryParam;
import uw.dao.annotation.QueryMeta;

/**
 * 带验证信息的查询参数类。
 * 自带了saasId, mchId, userId, userType属性。
 * 使用assign方法来对以上参数进行赋值。
 */
public class AuthIdQueryParam extends QueryParam {

    /**
     * id。
     */
    @QueryMeta(expr = "id=?")
    private Long id;

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
     * 默认带id构造器。
     * 如果不在web环境下运行，将会抛异常。
     *
     * @param id
     */
    public AuthIdQueryParam(Long id) {
        this.id = id;
        bindSaasId();
    }

    /**
     * 指定saasId,id的构造器。
     *
     * @param saasId
     * @param id
     */
    public AuthIdQueryParam(Long saasId, Long id) {
        this.id = id;
        this.saasId = saasId;
    }

    /**
     * 当前QueryParam填入当前用户的userId。
     */
    public AuthIdQueryParam bindUserId() {
        AuthTokenData tokenData = getAuthToken();
        setUserId( tokenData.getUserId() );
        return this;
    }

    /**
     * 当前QueryParam填入当前用户的mchId。
     */
    public AuthIdQueryParam bindMchId() {
        AuthTokenData tokenData = getAuthToken();
        setMchId( tokenData.getMchId() );
        return this;
    }

    /**
     * 当前QueryParam填入当前用户的userType。
     */
    public AuthIdQueryParam bindUserType() {
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

    public Long getSaasId() {
        return saasId;
    }

    public void setSaasId(Long saasId) {
        this.saasId = saasId;
    }

    public AuthIdQueryParam saasId(Long saasId) {
        this.saasId = saasId;
        return this;
    }

    public Long getMchId() {
        return mchId;
    }

    public void setMchId(Long mchId) {
        this.mchId = mchId;
    }

    public AuthIdQueryParam mchId(Long mchId) {
        this.mchId = mchId;
        return this;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public AuthIdQueryParam userId(Long userId) {
        this.userId = userId;
        return this;
    }

    public Integer getUserType() {
        return userType;
    }

    public void setUserType(Integer userType) {
        this.userType = userType;
    }

    public AuthIdQueryParam userType(Integer userType) {
        this.userType = userType;
        return this;
    }

    /**
     * 当前QueryParam填入当前用户的saasId。
     */
    private AuthIdQueryParam bindSaasId() {
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
            throw new UnsupportedOperationException( "AuthServiceHelper must be run in web environment!" );
        }
        return authToken;
    }

}
