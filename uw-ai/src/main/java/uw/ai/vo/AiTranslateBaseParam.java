package uw.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import uw.auth.service.AuthServiceHelper;

import java.util.List;

/**
 * 翻译列表基类。
 */
@Schema(title = "翻译列表基类", description = "翻译列表基类")
public abstract class AiTranslateBaseParam {

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
    /**
     * 配置ID。
     */
    @Schema(title = "配置ID", description = "配置ID")
    private long configId;

    /**
     * 系统提示。
     */
    @Schema(title = "系统提示", description = "系统提示")
    private String systemPrompt;

    /**
     * 目标语言。
     */
    @Schema(title = "目标语言列表", description = "目标语言列表")
    private List<String> langList;

    public AiTranslateBaseParam() {
    }

    /**
     * 绑定授权信息。
     */
    public void bindAuthInfo(){
        this.saasId = AuthServiceHelper.getSaasId();
        this.userId = AuthServiceHelper.getUserId();
        this.userType = AuthServiceHelper.getUserType();
        this.userInfo = AuthServiceHelper.getUserName();
    }

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

    public long getConfigId() {
        return configId;
    }

    public void setConfigId(long configId) {
        this.configId = configId;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public List<String> getLangList() {
        return langList;
    }

    public void setLangList(List<String> langList) {
        this.langList = langList;
    }
}
