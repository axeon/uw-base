package uw.webot.session;

import uw.webot.WebotSession;

import java.time.Duration;

/**
 * 会话管理器接口。
 * 定义会话创建、查询、更新、销毁的完整生命周期管理接口。
 * <p>
 * 注意：userId由业务方管理，业务方需要自行维护userId与sessionId的关联关系。
 * </p>
 */
public interface SessionService {

    /**
     * 设置会话。
     *
     * @param sessionId 会话ID
     * @param session   会话对象
     * @return 是否成功
     */
    void setSession(String sessionId, WebotSession session, Duration ttl);

    /**
     * 获取会话。
     *
     * @param sessionId 会话ID
     * @return 会话对象
     */
    WebotSession getSession(String sessionId);

    /**
     * 销毁会话。
     *
     * @param sessionId 会话ID
     * @return 是否成功
     */
    boolean invalidateSession(String sessionId);

    /**
     * 获取活跃会话数量。
     *
     * @return 会话数量
     */
    long getActiveSessionCount();

}
