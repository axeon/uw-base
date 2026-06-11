package uw.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Date;

/**
 * AI API连接配置VO。
 */
@Schema(title = "AI API连接配置VO", description = "AI API连接配置信息，供外部服务查询")
public class AiApiConfigVo {

    /**
     * 主键ID
     */
    @Schema(title = "主键ID", description = "API配置ID")
    private long id;

    /**
     * 配置代码
     */
    @Schema(title = "配置代码", description = "API配置代码")
    private String apiCode;

    /**
     * 配置名称
     */
    @Schema(title = "配置名称", description = "API配置名称，如 阿里DashScope生产账号")
    private String apiName;

    /**
     * 配置描述
     */
    @Schema(title = "配置描述", description = "API配置描述")
    private String apiDesc;

    /**
     * API地址
     */
    @Schema(title = "API地址", description = "API连接地址")
    private String apiUrl;

    /**
     * 状态
     */
    @Schema(title = "状态", description = "状态")
    private int state;

    /**
     * 创建时间
     */
    @Schema(title = "创建时间", description = "创建时间")
    private Date createDate;

    public AiApiConfigVo() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getApiCode() {
        return apiCode;
    }

    public void setApiCode(String apiCode) {
        this.apiCode = apiCode;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getApiDesc() {
        return apiDesc;
    }

    public void setApiDesc(String apiDesc) {
        this.apiDesc = apiDesc;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }
}
