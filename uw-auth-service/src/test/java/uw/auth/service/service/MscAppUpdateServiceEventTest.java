package uw.auth.service.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import uw.auth.service.conf.UwAuthServiceProperties;
import uw.auth.service.event.AuthAppRegisteredEvent;
import uw.auth.service.rpc.AuthAppRpc;
import uw.auth.service.vo.MscAppRegRequest;
import uw.auth.service.vo.MscAppRegResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 应用注册成功事件的发布时机（不起 Spring 容器，task-center 用桩替换）：
 * 事件在权限映射与应用 ID 初始化之后发布；监听方异常不影响注册流程；未经容器管理时不发布。
 */
class MscAppUpdateServiceEventTest {

    private static final long APP_ID = 7L;

    /**
     * 注册成功后发布事件，且发布时权限映射与应用 ID 已可读（启动期监听该事件的后续动作拿到的不再是空）。
     */
    @Test
    void publishesEventAfterPermMapReady() {
        AuthAppRpc authAppRpc = mock(AuthAppRpc.class);
        when(authAppRpc.regApp(any(MscAppRegRequest.class))).thenReturn(registryResponse());
        UwAuthServiceProperties properties = new UwAuthServiceProperties();
        MscAuthPermService authPermService = new MscAuthPermService();
        MscAppUpdateService service = new MscAppUpdateService(null, null, properties, authAppRpc, authPermService);

        List<AuthAppRegisteredEvent> events = new ArrayList<>();
        List<Map<String, Integer>> permMapAtPublish = new ArrayList<>();
        List<Long> appIdAtPublish = new ArrayList<>();
        service.setApplicationEventPublisher(event -> {
            events.add((AuthAppRegisteredEvent) event);
            permMapAtPublish.add(authPermService.getAppPermMap());
            appIdAtPublish.add(properties.getAppId());
        });

        service.registry();

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getAppId()).isEqualTo(APP_ID);
        assertThat(events.get(0).getRegState()).isEqualTo(MscAppRegResponse.STATE_REGISTRY);
        assertThat(permMapAtPublish.get(0)).as("发布时权限映射已初始化").containsEntry("/demo/ping:GET", 3);
        assertThat(appIdAtPublish.get(0)).as("发布时应用 ID 已写入配置").isEqualTo(APP_ID);
    }

    /**
     * 监听方抛异常：注册流程照常完成，异常不冒泡（否则会被当作注册失败而重试）。
     */
    @Test
    void listenerFailureDoesNotBreakRegistration() {
        AuthAppRpc authAppRpc = mock(AuthAppRpc.class);
        when(authAppRpc.regApp(any(MscAppRegRequest.class))).thenReturn(registryResponse());
        UwAuthServiceProperties properties = new UwAuthServiceProperties();
        MscAppUpdateService service = new MscAppUpdateService(null, null, properties, authAppRpc, new MscAuthPermService());
        service.setApplicationEventPublisher(event -> {
            throw new IllegalStateException("listener boom");
        });

        assertThatCode(service::registry).doesNotThrowAnyException();
        assertThat(properties.getAppId()).isEqualTo(APP_ID);
    }

    /**
     * 未经 Spring 容器管理（没有注入事件发布器）：不发布，也不报错。
     */
    @Test
    void noPublisherNoEvent() {
        AuthAppRpc authAppRpc = mock(AuthAppRpc.class);
        when(authAppRpc.regApp(any(MscAppRegRequest.class))).thenReturn(registryResponse());
        MscAppUpdateService service = new MscAppUpdateService(null, null, new UwAuthServiceProperties(), authAppRpc, new MscAuthPermService());

        assertThatCode(service::registry).doesNotThrowAnyException();
        verify(authAppRpc).regApp(any(MscAppRegRequest.class));
    }

    /**
     * 认证中心对"已注册"应用的应答：不再上传权限，直接带应用 ID 与权限映射返回。
     *
     * @return 注册应答
     */
    static MscAppRegResponse registryResponse() {
        Map<String, Integer> appPerm = new LinkedHashMap<>();
        appPerm.put("/demo/ping:GET", 3);
        MscAppRegResponse response = new MscAppRegResponse();
        response.setAppId(APP_ID);
        response.setState(MscAppRegResponse.STATE_REGISTRY);
        response.setAppPerm(appPerm);
        return response;
    }
}
