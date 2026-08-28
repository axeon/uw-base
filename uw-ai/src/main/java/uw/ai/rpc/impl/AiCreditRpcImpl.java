package uw.ai.rpc.impl;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import uw.ai.conf.UwAiProperties;
import uw.ai.rpc.AiCreditRpc;
import uw.ai.vo.AiCreditBalanceVo;
import uw.ai.vo.AiCreditLimitVo;
import uw.ai.vo.AiCreditRechargeParam;
import uw.common.response.ResponseData;

/**
 * {@link AiCreditRpc} 实现：调用 AI 服务中心的 token 余额接口（RestClient）。
 */
public class AiCreditRpcImpl implements AiCreditRpc {

    /**
     * 带认证拦截器的RestClient。
     */
    private final RestClient authRestClient;

    /**
     * 属性配置器
     */
    private final UwAiProperties uwAiProperties;

    public AiCreditRpcImpl(UwAiProperties uwAiProperties, RestClient authRestClient) {
        this.authRestClient = authRestClient;
        this.uwAiProperties = uwAiProperties;
    }

    /**
     * 给 saas 充值积点。
     *
     * @param param 充值参数
     * @return 充值后的余额
     */
    @Override
    public ResponseData<AiCreditBalanceVo> recharge(AiCreditRechargeParam param) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("saasId", param.getSaasId());
        body.add("credits", param.getCredits());
        if (param.getRemark() != null) {
            body.add("remark", param.getRemark());
        }
        ResponseData<AiCreditBalanceVo> result = authRestClient.post()
                .uri(uwAiProperties.getAiCenterHost() + "/rpc/credit/recharge")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<ResponseData<AiCreditBalanceVo>>() {});
        if (result == null) {
            return ResponseData.errorMsg("AiCreditRpcImpl.recharge() returned null body");
        }
        return result;
    }

    /**
     * 查询 saas 余额。
     *
     * @param saasId 租户
     * @return 余额
     */
    @Override
    public ResponseData<AiCreditBalanceVo> getBalance(long saasId) {
        ResponseData<AiCreditBalanceVo> result = authRestClient.get()
                .uri(uwAiProperties.getAiCenterHost() + "/rpc/credit/getBalance?saasId=" + saasId)
                .retrieve()
                .body(new ParameterizedTypeReference<ResponseData<AiCreditBalanceVo>>() {});
        if (result == null) {
            return ResponseData.errorMsg("AiCreditRpcImpl.getBalance() returned null body");
        }
        return result;
    }

    /**
     * 查询员工限额。
     *
     * @param saasId 租户
     * @param userId 员工
     * @return 员工限额
     */
    @Override
    public ResponseData<AiCreditLimitVo> getUserLimit(long saasId, long userId) {
        ResponseData<AiCreditLimitVo> result = authRestClient.get()
                .uri(uwAiProperties.getAiCenterHost() + "/rpc/credit/getUserLimit?saasId=" + saasId + "&userId=" + userId)
                .retrieve()
                .body(new ParameterizedTypeReference<ResponseData<AiCreditLimitVo>>() {});
        if (result == null) {
            return ResponseData.errorMsg("AiCreditRpcImpl.getUserLimit() returned null body");
        }
        return result;
    }

    /**
     * 清零员工当前周期已用积点。
     *
     * @param saasId 租户
     * @param userId 员工
     * @return 重置后的限额
     */
    @Override
    public ResponseData<AiCreditLimitVo> resetUserLimit(long saasId, long userId) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("saasId", saasId);
        body.add("userId", userId);
        ResponseData<AiCreditLimitVo> result = authRestClient.post()
                .uri(uwAiProperties.getAiCenterHost() + "/rpc/credit/resetUserLimit")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<ResponseData<AiCreditLimitVo>>() {});
        if (result == null) {
            return ResponseData.errorMsg("AiCreditRpcImpl.resetUserLimit() returned null body");
        }
        return result;
    }

}
