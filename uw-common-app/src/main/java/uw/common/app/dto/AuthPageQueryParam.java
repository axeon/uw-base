package uw.common.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import uw.auth.service.AuthServiceHelper;
import uw.auth.service.constant.UserType;
import uw.auth.service.token.AuthTokenData;
import uw.dao.PageQueryParam;
import uw.dao.annotation.QueryMeta;

/**
 * 自带验证信息的分页查询参数类。
 * 自带了saasId, mchId, userId, userType属性。
 */
@Schema(title = "带验证的页面查询参数", description = "带验证的页面查询参数")
public class AuthPageQueryParam extends PageQueryParam {

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
     * 默认构造器。
     * 在web环境中自动带入saasId信息，非web环境中会抛异常。
     */
    public AuthPageQueryParam() {
        bindSaasId();
    }

    /**
     * 带saasId构造器。
     * saasId可以设置为null，用于非web环境中。
     *
     * @param saasId
     */
    public AuthPageQueryParam(Long saasId) {
        this.saasId = saasId;
    }

    /**
     * 当前QueryParam填入当前用户的userId。
     */
    public AuthPageQueryParam bindUserId() {
        AuthTokenData tokenData = getAuthToken();
        setUserId( tokenData.getUserId() );
        return this;
    }

    /**
     * 当前QueryParam填入当前用户的mchId。
     */
    public AuthPageQueryParam bindMchId() {
        AuthTokenData tokenData = getAuthToken();
        setMchId( tokenData.getMchId() );
        return this;
    }

    /**
     * 当前QueryParam填入当前用户的userType。
     */
    public AuthPageQueryParam bindUserType() {
        AuthTokenData tokenData = getAuthToken();
        setUserType( tokenData.getUserType() );
        return this;
    }

    /**
     * 获取saasId。
     * @return
     */
    public Long getSaasId() {
        return saasId;
    }

    /**
     * 设置saasId。
     * @param saasId
     */
    public void setSaasId(Long saasId) {
        this.saasId = saasId;
    }

    /**
     *  设置saasId。
     * @param saasId
     * @return
     */
    public AuthPageQueryParam saasId(Long saasId) {
        this.saasId = saasId;
        return this;
    }

    /**
     * 获取mchId。
     * @return
     */
    public Long getMchId() {
        return mchId;
    }

    /**
     * 设置mchId。
     * @param mchId
     */
    public void setMchId(Long mchId) {
        this.mchId = mchId;
    }

    /**
     * 设置mchId。
     * @param mchId
     * @return
     */
    public AuthPageQueryParam mchId(Long mchId) {
        this.mchId = mchId;
        return this;
    }

    /**
     * 获取userId。
     * @return
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置userId。
     * @param userId
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 设置userId。
     * @param userId
     * @return
     */
    public AuthPageQueryParam userId(Long userId) {
        this.userId = userId;
        return this;
    }

    /**
     * 获取userType。
     * @return
     */
    public Integer getUserType() {
        return userType;
    }

    /**
     * 设置userType。
     * @param userType
     */
    public void setUserType(Integer userType) {
        this.userType = userType;
    }

    /**
     * 设置userType。
     * @param userType
     * @return
     */
    public AuthPageQueryParam userType(Integer userType) {
        this.userType = userType;
        return this;
    }

    /**
     * 当前QueryParam填入当前用户的saasId
     */
    private AuthPageQueryParam bindSaasId() {
        AuthTokenData tokenData = getAuthToken();
        if (tokenData.getUserType() < UserType.RPC.getValue() || tokenData.getUserType() > UserType.ADMIN.getValue()) {
            setSaasId( tokenData.getSaasId() );
        }
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
            throw new UnsupportedOperationException( "AuthServiceHelper must be run in auth-service web environment!" );
        }
        return authToken;
    }

}
