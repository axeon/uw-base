package uw.log.es.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Date;

/**
 * 用于作为支付回调接口记录日志用
 *
 * @author Liph
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayCallBackLog extends LogBaseVo {
    /**
     * 记录日志所涉及的充值渠道
     */
    private long serviceId;

    /**
     * 运营商编号
     */
    private long saasId;

    /**
     * 记录的日志类型
     */
    private String type;

    /**
     * 日志内容
     */
    private String content;

    /**
     * 支付第三方回调数据
     */
    private String responseData;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 状态 -1=失败  1=成功
     */
    private int status = -1;

    public long getServiceId() {
        return serviceId;
    }

    public void setServiceId(long serviceId) {
        this.serviceId = serviceId;
    }

    public long getSaasId() {
        return saasId;
    }

    public void setSaasId(long saasId) {
        this.saasId = saasId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getResponseData() {
        return responseData;
    }

    public void setResponseData(String responseData) {
        this.responseData = responseData;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }
}
