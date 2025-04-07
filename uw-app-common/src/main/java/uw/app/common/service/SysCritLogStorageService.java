package uw.app.common.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import uw.app.common.conf.AppCommonProperties;
import uw.app.common.entity.SysCritLog;
import uw.auth.service.log.AuthCriticalLogStorage;
import uw.auth.service.vo.MscActionLog;
import uw.dao.DaoFactory;

import java.util.Date;
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
    private boolean enableCritLog;

    public SysCritLogStorageService(AppCommonProperties uwAppBaseProperties) {
        enableCritLog = uwAppBaseProperties.isEnableCritLog();
    }

    @Override
    public void save(MscActionLog mscActionLog) {
        if (enableCritLog) {
            executor.submit( () -> {
                SysCritLog critLog = new SysCritLog();
                critLog.setId( dao.getSequenceId( SysCritLog.class ) );
                critLog.setAppInfo( mscActionLog.getAppInfo() );
                critLog.setAppHost( mscActionLog.getAppHost() );
                critLog.setSaasId( mscActionLog.getSaasId() );
                critLog.setMchId( mscActionLog.getMchId() );
                critLog.setUserId( mscActionLog.getUserId() );
                critLog.setGroupId( mscActionLog.getGroupId() );
                critLog.setUserName( mscActionLog.getUserName() );
                critLog.setUserType( mscActionLog.getUserType() );
                critLog.setRefType( mscActionLog.getRefType() );
                critLog.setRefId( String.valueOf( mscActionLog.getRefId() ) );
                critLog.setRequestBody( mscActionLog.getRequestBody() );
                critLog.setResponseBody( mscActionLog.getResponseBody() );
                critLog.setResponseMillis( mscActionLog.getResponseMillis() );
                critLog.setApiUri( mscActionLog.getApiUri() );
                critLog.setApiName( mscActionLog.getApiName() );
                critLog.setUserIp( mscActionLog.getUserIp() );
                critLog.setAppHost( mscActionLog.getAppHost() );
                critLog.setAppInfo( mscActionLog.getAppInfo() );
                critLog.setRequestDate( new Date() );
                try {
                    dao.save( critLog );
                } catch (Exception e) {
                    log.error( "系统关键日志保存数据库失败:{}", e.getMessage(), e );
                }
            } );
        }
    }
}
