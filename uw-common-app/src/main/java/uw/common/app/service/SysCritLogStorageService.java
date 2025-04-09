package uw.common.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import uw.common.app.conf.CommonAppProperties;
import uw.common.app.entity.SysCritLog;
import uw.auth.service.log.AuthCriticalLogStorage;
import uw.auth.service.vo.MscActionLog;
import uw.dao.DaoFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 鉴权关键日志写数据库的服务。
 */
@Service
@Primary
public class SysCritLogStorageService implements AuthCriticalLogStorage {

    private static final Logger log = LoggerFactory.getLogger( SysCritLogStorageService.class );

    private static final DaoFactory dao = DaoFactory.getInstance();

    /**
     * 虚拟线程执行器。
     */
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * 是否记录CritLog。
     */
    private final boolean enableCritLog;

    public SysCritLogStorageService(CommonAppProperties uwAppBaseProperties) {
        enableCritLog = uwAppBaseProperties.isEnableCritLog();
    }

    @Override
    public void save(MscActionLog mscActionLog) {
        if (enableCritLog) {
            executor.submit( () -> {
                SysCritLog critLog = new SysCritLog();
                critLog.setId( dao.getSequenceId( SysCritLog.class ) );
                critLog.setSaasId( mscActionLog.getSaasId() );
                critLog.setMchId(mscActionLog.getMchId());
                critLog.setUserId(mscActionLog.getUserId());
                critLog.setUserType(mscActionLog.getUserType());
                critLog.setGroupId(mscActionLog.getGroupId());
                critLog.setUserName(mscActionLog.getUserName());
                critLog.setNickName(mscActionLog.getNickName());
                critLog.setRealName(mscActionLog.getRealName());
                critLog.setUserIp(mscActionLog.getUserIp());
                critLog.setApiUri(mscActionLog.getApiUri());
                critLog.setApiName(mscActionLog.getApiName());
                critLog.setBizType(mscActionLog.getBizType());
                critLog.setBizId(String.valueOf(mscActionLog.getBizId()));
                critLog.setBizLog(mscActionLog.getBizLog());
                critLog.setRequestDate(mscActionLog.getRequestDate());
                critLog.setRequestBody(mscActionLog.getRequestBody());
                critLog.setResponseState(mscActionLog.getResponseState());
                critLog.setResponseCode(mscActionLog.getResponseCode());
                critLog.setResponseMsg(mscActionLog.getResponseMsg());
                critLog.setResponseBody(mscActionLog.getResponseBody());
                critLog.setResponseMillis(mscActionLog.getResponseMillis());
                critLog.setStatusCode(mscActionLog.getStatusCode());
                critLog.setAppInfo(mscActionLog.getAppInfo());
                critLog.setAppHost(mscActionLog.getAppHost());
                try {
                    dao.save( critLog );
                } catch (Exception e) {
                    log.error( "系统关键日志保存数据库失败:{}", e.getMessage(), e );
                }
            } );
        }
    }
}
