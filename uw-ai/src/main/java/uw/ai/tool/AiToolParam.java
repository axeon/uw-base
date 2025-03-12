package uw.ai.tool;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ai Tool Param.
 * 工具参数基类，主要保存了鉴权信息。
 */
@Schema(title = "AiTool参数基类", description = "AiTool参数基类")
public class AiToolParam {

    /**
     * SaasId
     */
    @Schema(title = "saasId", description = "saasId")
    private long saasId;

    /**
     * UserId
     */
    @Schema(title = "userId", description = "userId")
    private long userId;

    /**
     * UserType
     */
    @Schema(title = "userType", description = "userType")
    private int userType;

    /**
     * UserInfo
     */
    @Schema(title = "userInfo", description = "userInfo")
    private String userInfo;

    public long getSaasId() {
        return saasId;
    }

    public void setSaasId(long saasId) {
        this.saasId = saasId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public int getUserType() {
        return userType;
    }

    public void setUserType(int userType) {
        this.userType = userType;
    }

    public String getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(String userInfo) {
        this.userInfo = userInfo;
    }
}
