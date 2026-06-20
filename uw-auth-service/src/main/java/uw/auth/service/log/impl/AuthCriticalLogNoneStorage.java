package uw.auth.service.log.impl;

import uw.auth.service.log.AuthCriticalLogStorage;
import uw.auth.service.vo.MscActionLog;

/**
 * {@link uw.auth.service.log.AuthCriticalLogStorage} 的默认空实现。
 * <p>
 * 不执行任何存储操作，仅保证 Bean 注入时不为 null。各业务服务应自行实现
 * {@code AuthCriticalLogStorage} 以覆盖此默认实现。
 *
 * @author axeon
 */
public class AuthCriticalLogNoneStorage implements AuthCriticalLogStorage {

    @Override
    public void save(MscActionLog mscActionLog) {
        // 默认bean实现 不操作 只是防止bean注入时为null 在各个服务自实现AuthCriticalLogStorage
    }
}
