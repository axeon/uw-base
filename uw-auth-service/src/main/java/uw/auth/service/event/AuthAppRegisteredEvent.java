package uw.auth.service.event;

/**
 * 应用向认证中心注册成功的事件：发布时应用的权限映射（权限 code → 权限 ID）与应用 ID 已经就绪。
 *
 * <p>由 {@code MscAppUpdateService} 在注册流程末尾、权限映射初始化之后发布。应用注册是异步执行的
 * （应用就绪后另起线程，失败会重试），所以启动期需要用到权限映射或应用 ID 的后续动作应监听本事件，
 * 不要在应用就绪事件里直接读取——那时注册多半尚未完成，读到的是空。</p>
 *
 * <p>每次注册成功都会发布（含失败后的补注册、配置热更新后的重新注册），监听方必须可重复执行。
 * 监听方同步执行在注册线程上；监听方抛出的异常会被发布方捕获并记日志，不影响注册流程。</p>
 *
 * @author axeon
 * @since 6.2.89
 */
public final class AuthAppRegisteredEvent {

    /**
     * 应用 ID（认证中心分配）。
     */
    private final long appId;

    /**
     * 注册状态，取值见 {@code MscAppRegResponse.STATE_*}。
     */
    private final int regState;

    /**
     * 构建事件。
     *
     * @param appId    应用 ID（认证中心分配）
     * @param regState 注册状态（见 {@code MscAppRegResponse.STATE_*}）
     */
    public AuthAppRegisteredEvent(long appId, int regState) {
        this.appId = appId;
        this.regState = regState;
    }

    /**
     * 获取应用 ID。
     *
     * @return 应用 ID
     */
    public long getAppId() {
        return appId;
    }

    /**
     * 获取注册状态。
     *
     * @return 注册状态（见 {@code MscAppRegResponse.STATE_*}）
     */
    public int getRegState() {
        return regState;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "AuthAppRegisteredEvent{appId=" + appId + ", regState=" + regState + '}';
    }
}
