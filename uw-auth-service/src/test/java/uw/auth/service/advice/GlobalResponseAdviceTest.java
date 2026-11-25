package uw.auth.service.advice;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import uw.common.response.ResponseData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 全局响应处理器对 uw-dao 查无数据的处理：响应体原样透传，仅把 HTTP 状态码改为 404；服务间 RPC 接口（路径含 /rpc/）
 * 与其余响应不动状态码。不起 Spring 容器，服务端请求/响应用桩替换。
 */
class GlobalResponseAdviceTest {

    /**
     * 查无数据的 warn：状态码改为 404，响应体是同一个对象（内容不变）。
     *
     * @throws Exception 构造返回类型描述失败
     */
    @Test
    void daoDataNotFound_returns404WithBodyUnchanged() throws Exception {
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        ResponseData<Object> body = ResponseData.warnCode("uw.dao.data.not.found.warn");

        Object written = advice().beforeBodyWrite(body, responseDataReturn(), null, null, request("/saas/saas/load"), response);

        assertThat(written).isSameAs(body);
        verify(response).setStatusCode(HttpStatus.NOT_FOUND);
    }

    /**
     * 其他 warn、以及非 warn 状态带着同一个码：都不改状态码。
     *
     * @throws Exception 构造返回类型描述失败
     */
    @Test
    void otherResponses_keepStatusCode() throws Exception {
        assertStatusUntouched(ResponseData.warnCode("some.other.warn"));
        assertStatusUntouched(ResponseData.errorCode("uw.dao.data.not.found.warn"));
        assertStatusUntouched(ResponseData.success("ok"));
    }

    static void assertStatusUntouched(ResponseData<Object> body) throws Exception {
        assertStatusUntouched(body, "/saas/saas/load");
    }

    /**
     * 指定请求路径下返回给定响应体：状态码不被改写。
     *
     * @param body 响应体
     * @param path 请求路径
     * @throws Exception 构造返回类型描述失败
     */
    static void assertStatusUntouched(ResponseData<Object> body, String path) throws Exception {
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        Object written = advice().beforeBodyWrite(body, responseDataReturn(), null, null, request(path), response);
        assertThat(written).isSameAs(body);
        verify(response, never()).setStatusCode(any());
    }

    /**
     * 服务间 RPC 接口：调用方对 4xx 抛异常、拿不到 ResponseData，查无数据必须保持 200 + warn。
     *
     * @throws Exception 构造返回类型描述失败
     */
    @Test
    void rpcRequests_keep200ForDataNotFound() throws Exception {
        assertStatusUntouched(ResponseData.warnCode("uw.dao.data.not.found.warn"), "/rpc/service/loadUser");
        assertStatusUntouched(ResponseData.warnCode("uw.dao.data.not.found.warn"), "/uw-auth-center/rpc/service/loadUser");
    }

    /**
     * 路径只是以 rpc 开头、并非 /rpc/ 片段的接口，仍按普通接口处理。
     *
     * @throws Exception 构造返回类型描述失败
     */
    @Test
    void pathMerelyPrefixedWithRpc_isNotRpc() throws Exception {
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        ResponseData<Object> body = ResponseData.warnCode("uw.dao.data.not.found.warn");
        advice().beforeBodyWrite(body, responseDataReturn(), null, null, request("/rpcadmin/load"), response);
        verify(response).setStatusCode(HttpStatus.NOT_FOUND);
    }

    /**
     * 指定路径的服务端请求桩。
     *
     * @param path 请求路径
     * @return 服务端请求
     */
    static ServerHttpRequest request(String path) {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getURI()).thenReturn(URI.create(path));
        return request;
    }

    static GlobalResponseAdvice advice() {
        return new GlobalResponseAdvice();
    }

    /**
     * 一个返回类型为 ResponseData 的方法描述（String 类型返回值会被另行转成 JSON 字符串，这里避开）。
     *
     * @return 返回类型描述
     * @throws Exception 反射失败
     */
    static MethodParameter responseDataReturn() throws Exception {
        return new MethodParameter(Sample.class.getDeclaredMethod("handle"), -1);
    }

    /**
     * 提供返回类型描述用的样例控制器方法。
     */
    static class Sample {

        /**
         * 样例方法。
         *
         * @return 响应数据
         */
        public ResponseData<Object> handle() {
            return null;
        }
    }
}
