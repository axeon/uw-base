package uw.common.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import uw.auth.service.AuthServiceHelper;
import uw.auth.service.constant.UserType;
import uw.auth.service.token.AuthTokenData;
import uw.dao.QueryParam;
import uw.dao.annotation.QueryMeta;

import java.io.Serializable;
import java.util.Map;

/**
 * 带验证信息的查询参数类。
 * 自带了saasId, mchId, userId, userType属性。
 * 使用assign方法来对以上参数进行赋值。
 */
public class AuthIdQueryParam extends QueryParam<AuthIdQueryParam> {

    /**
     * ID。
     */
    @QueryMeta(expr = "id=?")
    @Schema(title="ID", description = "ID")
    private Serializable id;


    /**
     * 数组ID。
     */
    @QueryMeta(expr = "id in (?)")
    @Schema(title="ID数组", description = "ID数组，可同时匹配多个。")
    private Serializable[] ids;

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
     * 默认带id构造器。
     * 如果不在web环境下运行，将会抛异常。
     *
     * @param id
     */
    public AuthIdQueryParam(Serializable id) {
        this.id = id;
        bindSaasId();
    }

    /**
     * 指定saasId,id的构造器。
     *
     * @param saasId
     * @param id
     */
    public AuthIdQueryParam(Long saasId, Serializable id) {
        this.id = id;
        this.saasId = saasId;
    }

    /**
     * 默认带id构造器。
     * 如果不在web环境下运行，将会抛异常。
     *
     * @param ids
     */
    public AuthIdQueryParam(Serializable[] ids) {
        this.ids = ids;
        bindSaasId();
    }

    /**
     * 指定saasId,id的构造器。
     *
     * @param saasId
     * @param ids
     */
    public AuthIdQueryParam(Long saasId, Serializable[] ids) {
        this.ids = ids;
        this.saasId = saasId;
    }


    /**
     * 当前QueryParam填入当前用户的saasId。
     */
    public AuthIdQueryParam bindSaasId() {
        AuthTokenData tokenData = getAuthToken();
        if (tokenData.getUserType() < UserType.RPC.getValue() || tokenData.getUserType() > UserType.ADMIN.getValue()) {
            setSaasId(tokenData.getSaasId());
        }
        return this;
    }

    /**
     * 当前QueryParam填入当前用户的userId。
     */
    public AuthIdQueryParam bindUserId() {
        AuthTokenData tokenData = getAuthToken();
        setUserId(tokenData.getUserId());
        return this;
    }

    /**
     * 当前QueryParam填入当前用户的mchId。
     */
    public AuthIdQueryParam bindMchId() {
        AuthTokenData tokenData = getAuthToken();
        setMchId(tokenData.getMchId());
        return this;
    }

    /**
     * 当前QueryParam填入当前用户的userType。
     */
    public AuthIdQueryParam bindUserType() {
        AuthTokenData tokenData = getAuthToken();
        setUserType(tokenData.getUserType());
        return this;
    }

    /**
     * 获取id。
     *
     * @return
     */
    public Serializable getId() {
        return id;
    }

    /**
     * 设置id。
     *
     * @param id
     */
    public void setId(Serializable id) {
        this.id = id;
    }

    /**
     * 设置id。
     *
     * @param id
     * @return
     */
    public AuthIdQueryParam id(Serializable id) {
        this.id = id;
        return this;
    }

    /**
     * 获取数组ID。
     */
    public Serializable[] getIds() {
        return this.ids;
    }

    /**
     * 设置数组ID。
     */
    public void setIds(Serializable[] ids) {
        this.ids = ids;
    }

    /**
     * 设置数组ID链式调用。
     */
    public AuthIdQueryParam ids(Serializable[] ids) {
        setIds(ids);
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
    public AuthIdQueryParam saasId(Long saasId) {
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
     * @param mchId
     * @return
     */
    public AuthIdQueryParam mchId(Long mchId) {
        this.mchId = mchId;
        return this;
    }

    /**
     * 获取用户id。
     * @return
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置用户id。
     * @param userId
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 设置用户id。
     * @param userId
     * @return
     */
    public AuthIdQueryParam userId(Long userId) {
        this.userId = userId;
        return this;
    }

    /**
     * 获取用户类型。
     * @return
     */
    public Integer getUserType() {
        return userType;
    }

    /**
     * 设置用户类型。
     * @param userType
     */
    public void setUserType(Integer userType) {
        this.userType = userType;
    }

    /**
     * 设置用户类型。
     * @param userType
     * @return
     */
    public AuthIdQueryParam userType(Integer userType) {
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
