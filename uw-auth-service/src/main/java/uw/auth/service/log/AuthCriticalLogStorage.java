package uw.auth.service.log;

import uw.auth.service.vo.MscActionLog;

/**
 * 关键日志存储接口。
 *
 */
public interface AuthCriticalLogStorage {

    void save(MscActionLog mscActionLog);

}
