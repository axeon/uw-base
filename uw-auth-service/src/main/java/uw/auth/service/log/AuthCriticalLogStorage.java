package uw.auth.service.log;

import uw.auth.service.vo.MscActionLog;

/**
 * 关键（CRIT）操作日志存储接口。
 * <p>
 * 当接口标注 {@link uw.auth.service.constant.ActionLog#CRIT} 或为
 * {@link uw.auth.service.constant.AuthType#SUDO} 时，{@code AuthServiceFilter} 在写入 ES 的
 * 同时调用本接口将日志持久化到数据库等关键存储。各业务服务应自行实现本接口；
 * 未提供实现时使用 {@link uw.auth.service.log.impl.AuthCriticalLogNoneStorage} 空实现。
 *
 * @author axeon
 */
public interface AuthCriticalLogStorage {

    /**
     * 保存一条关键操作日志。
     *
     * @param mscActionLog 操作日志对象
     */
    void save(MscActionLog mscActionLog);

}
