package uw.app.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import uw.auth.service.AuthServiceHelper;
import uw.auth.service.constant.UserType;
import uw.dao.PageQueryParam;
import uw.dao.annotation.QueryMeta;

/**
 * 带验证信息的查询参数类。
 * 自带了saasId, mchId, userId, userType属性。
 * 使用assign方法来对以上参数进行赋值。
 */
@Schema(title = "带验证的页面查询参数", description = "带验证的页面查询参数")
public class AuthPageQueryParam extends PageQueryParam {

    @QueryMeta(expr = "saas_id=?")
    @Schema(title="saasId", description = "saasId",hidden = true)
    private Long saasId;

    @QueryMeta(expr = "mch_id=?")
    @Schema(title="mchId", description = "mchId",hidden = true)
    private Long mchId;

    @QueryMeta(expr = "user_id=?")
    @Schema(title="userId", description = "userId",hidden = true)
    private Long userId;

    @QueryMeta(expr = "user_type=?")
    @Schema(title="userType", description = "userType",hidden = true)
    private Integer userType;

    public AuthPageQueryParam() {
        bindSaasId();
    }
    public AuthPageQueryParam(boolean ignoreException) {
        if (ignoreException) {
            try {
                bindSaasId();
            } catch (Exception e){
                // 捕获不处理
            }
        } else {
            bindSaasId();
        }
    }

    /**
     * 当前QueryParam填入当前用户的saasId
     */
    private AuthPageQueryParam bindSaasId() {
        int userType = AuthServiceHelper.getUserType();
        if (userType < UserType.RPC.getValue() || userType > UserType.ADMIN.getValue()){
            setSaasId(AuthServiceHelper.getSaasId());
        }
        return this;
    }

    /**
     * 当前QueryParam填入当前用户的userId。
     */
    public AuthPageQueryParam bindUserId() {
        setSaasId(AuthServiceHelper.getSaasId());
        setUserId(AuthServiceHelper.getUserId());
        return this;
    }

    /**
     * 当前QueryParam填入当前用户的mchId。
     */
    public AuthPageQueryParam bindMchId() {
        setSaasId(AuthServiceHelper.getSaasId());
        setMchId(AuthServiceHelper.getMchId());
        return this;
    }

    /**
     * 当前QueryParam填入当前用户的userType。
     */
    public AuthPageQueryParam bindUserType() {
        setUserType(AuthServiceHelper.getUserType());
        return this;
    }


    /**
     * 当前QueryParam填入当前用户的saasId、userId、mchId、userType
     */
    public AuthPageQueryParam bindAuthInfo() {
        setSaasId(AuthServiceHelper.getSaasId());
        setMchId(AuthServiceHelper.getMchId());
        setUserId(AuthServiceHelper.getUserId());
        setUserType(AuthServiceHelper.getUserType());
        return this;
    }

    /**
     * 当前QueryParam根据参数控制填入当前用户的saasId、userId、mchId、userType
     * @param bindSaasId 是否填入saasId
     * @param bindUserId 是否填入userId
     * @param bindMchId 是否填入mchId
     * @param bindUserType 是否填入userType
     */
    public AuthPageQueryParam bindAuthInfo(boolean bindSaasId, boolean bindUserId, boolean bindMchId, boolean bindUserType) {
        if (bindSaasId) {
            setSaasId(AuthServiceHelper.getSaasId());
        }
        if (bindMchId) {
            setMchId(AuthServiceHelper.getMchId());
        }
        if (bindUserId) {
            setUserId(AuthServiceHelper.getUserId());
        }
        if (bindUserType) {
            setUserType(AuthServiceHelper.getUserType());
        }
        return this;
    }

    public Long getSaasId() {
        return saasId;
    }

    public void setSaasId(Long saasId) {
        this.saasId = saasId;
    }

    public Long getMchId() {
        return mchId;
    }

    public void setMchId(Long mchId) {
        this.mchId = mchId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getUserType() {
        return userType;
    }

    public void setUserType(Integer userType) {
        this.userType = userType;
    }
}
