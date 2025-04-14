package uw.auth.service.service;


import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PreDestroy;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;
import uw.auth.service.AuthServiceHelper;
import uw.auth.service.annotation.MscPermDeclare;
import uw.auth.service.conf.AuthServiceProperties;
import uw.auth.service.constant.AuthType;
import uw.auth.service.constant.UserType;
import uw.auth.service.filter.AuthServiceFilter;
import uw.auth.service.rpc.AuthAppRpc;
import uw.auth.service.token.InvalidTokenData;
import uw.auth.service.util.MscUtils;
import uw.auth.service.vo.AppRegRequest;
import uw.auth.service.vo.AppRegResponse;
import uw.auth.service.vo.MscAppReportRequest;
import uw.auth.service.vo.MscAppReportResponse;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * App更新服务,启动时发布自身
 *
 * @author axeon
 * @since 2017/11/29
 */
public class AppUpdateService {

    private static final Logger logger = LoggerFactory.getLogger(AppUpdateService.class);

    /**
     * 相关配置
     */
    private final AuthServiceProperties authServiceProperties;

    /**
     * 注册Rpc 接口
     */
    private final AuthAppRpc authAppRpc;

    /**
     * 用户权限接口服务
     */
    private final AuthPermService authPermService;

    /**
     * spring-mvc HandleMapping
     */
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    /**
     * 内部自持任务。
     */
    private ScheduledExecutorService scheduledExecutorService;
    /**
     * app主机ID。
     * 由center返回。
     */
    private long appHostId;

    /**
     * ctor
     *
     * @param authServiceProperties
     * @param authAppRpc
     * @param authPermService
     * @param requestMappingHandlerMapping
     */
    public AppUpdateService(final AuthServiceProperties authServiceProperties, final AuthAppRpc authAppRpc, final AuthPermService authPermService,
                            final RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.authServiceProperties = authServiceProperties;
        this.authAppRpc = authAppRpc;
        this.authPermService = authPermService;
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
    }

    /**
     * 匹配 / 在API URI出现的次数。
     *
     * @param apiUri
     * @return
     */
    private static int countLevel(String apiUri) {
        int count = 0;
        int index = 1;
        while ((index = apiUri.indexOf('/', index)) > -1) {
            index = index + 1;
            count++;
        }
        return count;
    }

    /**
     * 初始化服务。
     */
    public void init() {
        logger.info("uw-auth-service registry started!");
        boolean regSuccess = false;
        for (int i = 0; i < 20; i++) {
            if (regSuccess) {
                break;
            }
            try {
                registry();
                logger.info("uw-auth-service registry success!");
                regSuccess = true;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                logger.warn("uw-auth-service registry failed {} times! will retry after 5 seconds.", i);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                }
            }
        }
        if (!regSuccess) {
            logger.error("uw-auth-service will shutdown application!");
            System.exit(-1);
        }
        scheduledExecutorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setDaemon(true).build());
        scheduledExecutorService.scheduleAtFixedRate(() -> reportStatus(), 0, 1, TimeUnit.MINUTES);
    }

    /**
     * 注册APP服务,权限
     */
    public void registry() {
        //不管3721，先提交注册。
        AppRegRequest appRegRequest = new AppRegRequest();
        appRegRequest.setAppName(authServiceProperties.getAppName());
        appRegRequest.setAppLabel(authServiceProperties.getAppLabel());
        appRegRequest.setAppVersion(authServiceProperties.getAppVersion());
        AppRegResponse appRegResponse = authAppRpc.regApp(appRegRequest);
        if (appRegResponse.getState() == AppRegResponse.STATE_INIT) {
            //此时需要补充上传权限注册信息。
            logger.info("AuthService scaning@RequestMapping annotations in @Controller HandlerMethod ");
            List<AppRegRequest.PermVo> allVoList = new ArrayList<>(500);
            //扫描菜单。
            List<AppRegRequest.PermVo> menuVoList = new ArrayList<>(100);
            //扫描权限。
            List<AppRegRequest.PermVo> permVoList = new ArrayList<>(500);
            // 先扫Package,Class的菜单注解。
            if (StringUtils.isNotBlank(authServiceProperties.getAuthBasePackage())) {
                Reflections reflections =
                        new Reflections(new ConfigurationBuilder().setUrls(ClasspathHelper.forPackage(authServiceProperties.getAuthBasePackage())).setScanners(Scanners.TypesAnnotated, Scanners.SubTypes));
                Set<Class<?>> mscPermDeclareSet = reflections.getTypesAnnotatedWith(MscPermDeclare.class);
                for (Class<?> aClass : mscPermDeclareSet) {
                    MscPermDeclare mscPermDeclare = aClass.getAnnotation(MscPermDeclare.class);
                    RequestMapping requestMapping = aClass.getAnnotation(RequestMapping.class);
                    Tag tag = aClass.getAnnotation(Tag.class);
                    String permName = mscPermDeclare.name();
                    String permDesc = mscPermDeclare.description();
                    String permUri = mscPermDeclare.uri();

                    int userType = mscPermDeclare.user().getValue();
                    AuthType authType = mscPermDeclare.auth();

                    //只做root后用户的权限记录，其他不做。
                    if (userType < UserType.ROOT.getValue()) {
                        continue;
                    }

                    if (StringUtils.isBlank(permName) && tag != null) {
                        permName = tag.name();
                    }

                    if (StringUtils.isBlank(permDesc) && tag != null) {
                        permDesc = tag.description();
                    }
                    if (StringUtils.isBlank(permUri) && requestMapping != null) {
                        // 注解在Controller上,只取第一个
                        permUri = requestMapping.value()[0];
                    }
                    if (StringUtils.isBlank(permName) || StringUtils.isBlank(permUri)) {
                        logger.warn("AuthService scan warn: package/class [{}] annotation name or uri is blank!!!", aClass.getName());
                    }
                    AppRegRequest.PermVo permVo = new AppRegRequest.PermVo();
                    permVo.setName(permName);
                    permVo.setDesc(permDesc);
                    permVo.setUser(mscPermDeclare.user().getValue());
                    permVo.setCode(permUri);
                    menuVoList.add(permVo);
                }
            }
            // 扫描方法权限注解
            Map<RequestMappingInfo, HandlerMethod> map = requestMappingHandlerMapping.getHandlerMethods();
            if (!map.isEmpty()) {
                // 按照URI排序
                for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : map.entrySet()) {
                    HandlerMethod method = entry.getValue();
                    MscPermDeclare mscPermDeclare = method.getMethodAnnotation(MscPermDeclare.class);
                    Operation operation = method.getMethodAnnotation(Operation.class);
                    if (mscPermDeclare != null) {
                        String permName = mscPermDeclare.name();
                        String permDesc = mscPermDeclare.description();

                        int userType = mscPermDeclare.user().getValue();
                        AuthType authType = mscPermDeclare.auth();

                        //必须是authType=perm的，才给过，否则不用加入权限了。
                        if (authType != AuthType.PERM) {
                            continue;
                        }
                        //只做root后用户的权限记录，其他不做。
                        if (userType < UserType.ROOT.getValue()) {
                            continue;
                        }
                        //如果没有配置name和desc，则使用swagger注解。
                        if (StringUtils.isBlank(permName) && operation != null) {
                            permName = operation.summary();
                        }

                        if (StringUtils.isBlank(permDesc) && operation != null) {
                            permDesc = operation.description();
                        }
                        RequestMappingInfo info = entry.getKey();
                        Set<RequestMethod> requestMethods = info.getMethodsCondition().getMethods();
                        if (requestMethods.isEmpty()) {
                            logger.warn("http method: {} no explicit @RequestMapping Method mapping ", method.getMethod().toString());
                        } else {
                            // 映射多个方法或者多个URI
                            Set<PathPattern> patterns = info.getPathPatternsCondition().getPatterns();
                            for (PathPattern pattern : patterns) {
                                for (RequestMethod requestMethod : requestMethods) {
                                    AppRegRequest.PermVo permVo = new AppRegRequest.PermVo();
                                    String permPath = MscUtils.sanitizeUrl(pattern.getPatternString());
                                    //解决$PackageInfo$的问题。
                                    if (countLevel(permPath) < 3) {
                                        permVo.setCode(permPath);
                                    } else {
                                        permVo.setCode(permPath + ":" + requestMethodToString(requestMethod));
                                    }
                                    permVo.setName(permName);
                                    permVo.setDesc(permDesc);
                                    permVo.setUser(userType);
                                    permVoList.add(permVo);
                                }
                            }
                        }
                    }
                }
            }
            //过滤menu,保留perm的menu。
            for (AppRegRequest.PermVo menuVo : menuVoList) {
                for (AppRegRequest.PermVo permVo : permVoList) {
                    if (permVo.getCode().startsWith(menuVo.getCode())) {
                        allVoList.add(menuVo);
                        break;
                    }
                }
            }
            // 合并menu和perm
            allVoList.addAll(permVoList);
            appRegRequest.setPerms(allVoList);
            appRegResponse = authAppRpc.regApp(appRegRequest);
        }
        logger.info("AuthService RegApp: {}, version: {}, auth-center response state: {}, msg: {}", authServiceProperties.getAppName(), authServiceProperties.getAppVersion(),
                appRegResponse.getState(), appRegResponse.getMsg());
        if (appRegResponse.getState() == AppRegResponse.STATE_FAIL) {
            throw new RuntimeException("AuthService RegApp failed: " + appRegResponse.getMsg());
        }
        authServiceProperties.setAppId(appRegResponse.getAppId());
        authPermService.initAppPerm(appRegResponse.getAppId(), appRegResponse.getAppPerm(), appRegResponse.getState());
    }

    @PreDestroy
    public void destroy() {
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
        }
    }

    /**
     * 报告状态。
     */
    private void reportStatus() {
        try {
            MscAppReportRequest request = new MscAppReportRequest();
            request.setId(appHostId);
            request.setAppId(authServiceProperties.getAppId());
            request.setAppName(authServiceProperties.getAppName());
            request.setAppVersion(authServiceProperties.getAppVersion());
            request.setAppHost(authServiceProperties.getAppHost());
            request.setAppPort(authServiceProperties.getAppPort());
            request.setAccessCount(AuthServiceFilter.invokeCounter.longValue());
            //设置内存和线程信息。
            Runtime runtime = Runtime.getRuntime();
            request.setJvmMemMax(runtime.maxMemory());
            request.setJvmMemTotal(runtime.totalMemory());
            request.setJvmMemFree(runtime.freeMemory());
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            request.setThreadActive(threadMXBean.getThreadCount());
            request.setThreadDaemon(threadMXBean.getDaemonThreadCount());
            request.setThreadPeak(threadMXBean.getPeakThreadCount());
            request.setThreadStarted(threadMXBean.getTotalStartedThreadCount());
            request.setUserRootNum(AuthServiceHelper.getActiveUserNum(UserType.ROOT.getValue()));
            request.setUserRpcNum(AuthServiceHelper.getActiveUserNum(UserType.RPC.getValue()));
            request.setUserOpsNum(AuthServiceHelper.getActiveUserNum(UserType.OPS.getValue()));
            request.setUserAdminNum(AuthServiceHelper.getActiveUserNum(UserType.ADMIN.getValue()));
            request.setUserSaasNum(AuthServiceHelper.getActiveUserNum(UserType.SAAS.getValue()));
            request.setUserGuestNum(AuthServiceHelper.getActiveUserNum(UserType.GUEST.getValue()));
            MscAppReportResponse response = authAppRpc.reportStatus(request);
            //处理返回结果。
            if (response != null) {
                appHostId = response.getId();
                List<InvalidTokenData> list = response.getInvalidTokenDataList();
                if (list != null) {
                    for (InvalidTokenData invalidTokenData : list) {
                        AuthServiceHelper.invalidToken(invalidTokenData);
                    }
                }
            }
        } catch (Throwable e) {
            logger.error("reportStatus exception: {}", e.getMessage(), e);
        }
    }

    /**
     * RequestMethod enum toString
     *
     * @param requestMethod
     * @return
     */
    private String requestMethodToString(RequestMethod requestMethod) {
        return switch (requestMethod) {
            case GET -> "GET";
            case PUT -> "PUT";
            case HEAD -> "HEAD";
            case POST -> "POST";
            case PATCH -> "PATCH";
            case TRACE -> "TRACE";
            case DELETE -> "DELETE";
            case OPTIONS -> "OPTIONS";
        };
    }
}
