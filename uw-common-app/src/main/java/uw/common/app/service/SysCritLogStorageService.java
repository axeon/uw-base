package uw.common.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PreDestroy;
import uw.auth.service.log.AuthCriticalLogStorage;
import uw.auth.service.vo.MscActionLog;
import uw.common.app.conf.CommonAppProperties;
import uw.common.app.entity.SysCritLog;
import uw.dao.DaoFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 鉴权关键日志写数据库的服务。
 * <p>
 * Bean 由 {@link uw.common.app.conf.CommonAppAutoConfiguration} 统一注册（@Primary），
 * 此处不再标注 @Service，避免与 AutoConfig 重复注册同名 bean。
 */
public class SysCritLogStorageService implements AuthCriticalLogStorage {

    private static final Logger log = LoggerFactory.getLogger(SysCritLogStorageService.class);

    private static final DaoFactory dao = DaoFactory.getInstance();

    /**
     * 关闭时等待未完成任务落库的最长时间（秒）。
     */
    private static final long SHUTDOWN_AWAIT_SECONDS = 10;

    /**
     * 虚拟线程执行器。
     */
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * 是否记录 CritLog（取自 {@link CommonAppProperties#isEnableCritLog()}）。
     */
    private final boolean enableCritLog;

    /**
     * 构造关键日志存储服务。
     *
     * @param uwAppBaseProperties 通用应用配置
     */
    public SysCritLogStorageService(CommonAppProperties uwAppBaseProperties) {
        enableCritLog = uwAppBaseProperties.isEnableCritLog();
    }

    /**
     * 异步保存一条关键操作日志到数据库。
     * <p>
     * 当 {@link #enableCritLog} 为 true 时，将 {@link MscActionLog} 转换为 {@link SysCritLog}
     * 并提交到虚拟线程执行器落库；落库异常仅记录错误日志，不影响主流程。
     * </p>
     *
     * @param mscActionLog 鉴权框架产生的关键操作日志
     */
    @Override
    public void save(MscActionLog mscActionLog) {
        if (enableCritLog) {
            executor.submit(() -> {
                SysCritLog critLog = new SysCritLog();
                critLog.setId(dao.getSequenceId(SysCritLog.class));
                critLog.setSaasId(mscActionLog.getSaasId());
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
                    dao.save(critLog);
                } catch (Exception e) {
                    log.error("系统关键日志保存数据库失败:{}", e.getMessage(), e);
                }
            });
        }
    }

    /**
     * 销毁任务执行器，等待未完成的日志落库。
     */
    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_AWAIT_SECONDS, TimeUnit.SECONDS)) {
                long pending = executor.shutdownNow().size();
                log.warn("系统关键日志关闭超时，仍有 {} 个任务未完成被强制中断。", pending);
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}
