package uw.ai.rpc;

import uw.ai.vo.AiCreditBalanceVo;
import uw.ai.vo.AiCreditLimitVo;
import uw.ai.vo.AiCreditRechargeParam;
import uw.common.response.ResponseData;

/**
 * AI积点 RPC 接口（供计费系统/门户等服务充值、余额查询与员工限额查询/清零）。
 */
public interface AiCreditRpc {

    /**
     * 给 saas 充值积点（tokens 可为负=回收）。
     *
     * @param param 充值参数（saasId + tokens + remark）
     * @return 充值后的余额
     */
    ResponseData<AiCreditBalanceVo> recharge(AiCreditRechargeParam param);

    /**
     * 查询 saas 余额。
     *
     * @param saasId 租户
     * @return 余额（无余额行时 grant/used/remaining 均为 0）
     */
    ResponseData<AiCreditBalanceVo> getBalance(long saasId);

    /**
     * 查询员工限额（周期已过时按重置后口径返回：已用归 0、周期起点推进）。
     *
     * @param saasId 租户
     * @param userId 员工
     * @return 员工限额（无行=未开通 AI 授权，data 为空）
     */
    ResponseData<AiCreditLimitVo> getUserLimit(long saasId, long userId);

    /**
     * 清零员工当前周期已用积点（立即重置：used=0、周期起点推进到当前周期起点）。
     * 用于周期中途手动放宽（如临时活动），周期自然翻转仍自动进行。
     *
     * @param saasId 租户
     * @param userId 员工
     * @return 重置后的限额（限额行不存在或已停用时返回错误）
     */
    ResponseData<AiCreditLimitVo> resetUserLimit(long saasId, long userId);

}
