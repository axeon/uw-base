package uw.auth.service.rpc;

import uw.auth.service.vo.*;
import uw.common.dto.ResponseData;

/**
 * auth-server向auth-center 交互信息。
 *
 * @author axeon
 */
public interface AuthAppRpc {

    /**
     * 注册当前App。
     * 注册流程分两步走，第一步直接不发送权限信息，只发送版本信息，如果需要升级，则发送第二次信息。
     *
     * @param appRegRequest
     * @return
     */
    MscAppRegResponse regApp(MscAppRegRequest appRegRequest);

    /**
     * 报告状态，同时拉取非法TokenData。
     *
     * @return
     */
    MscAppReportResponse reportStatus(MscAppReportRequest mscAppReportRequest);

    /**
     * 更新mscPerm授权状态。
     *
     * @return
     */
    ResponseData updatePermLicense(MscPermLicenseRequest mscPermLicenseRequest);

}
