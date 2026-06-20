package uw.auth.service.rpc;

import uw.auth.service.vo.*;
import uw.common.response.ResponseData;

/**
 * 服务端向 auth-center 的应用注册与上报接口。
 * <p>
 * 封装应用注册、状态上报、License 更新等 RPC 调用，由 {@code MscAppUpdateService} 在
 * 启动与周期上报时调用。默认实现 {@code AuthAppRpcImpl}。
 *
 * @author axeon
 */
public interface AuthAppRpc {

    /**
     * 注册当前应用。
     * <p>
     * 注册分两步：第一步只发送版本信息；若 auth-center 返回 {@link MscAppRegResponse#STATE_INIT}，
     * 则补充扫描出的权限信息后再次调用上报。
     *
     * @param appRegRequest 应用注册请求
     * @return 应用注册响应，含 appId 与权限 ID 映射
     */
    MscAppRegResponse regApp(MscAppRegRequest appRegRequest);

    /**
     * 上报应用主机运行状态，同时拉取需要失效的 Token 列表。
     *
     * @param mscAppReportRequest 状态上报请求
     * @return 状态上报响应，含应用主机 ID 与非法 Token 列表
     */
    MscAppReportResponse reportStatus(MscAppReportRequest mscAppReportRequest);

    /**
     * 更新应用的权限 License 授权状态。
     *
     * @param mscPermLicenseRequest 权限 License 请求
     * @return 更新结果
     */
    ResponseData updatePermLicense(MscPermLicenseRequest mscPermLicenseRequest);

}
