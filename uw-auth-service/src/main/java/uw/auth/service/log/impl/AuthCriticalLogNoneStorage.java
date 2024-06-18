package uw.auth.service.log.impl;

import uw.auth.service.log.AuthCriticalLogStorage;
import uw.auth.service.vo.MscActionLog;

/**
 * 关键日志存储接口默认实现。
 *
 */
public class AuthCriticalLogNoneStorage implements AuthCriticalLogStorage {

    @Override
    public void save(MscActionLog mscActionLog) {
        // 默认bean实现 不操作 只是防止bean注入时为null 在各个服务自实现AuthCriticalLogStorage
    }
}
