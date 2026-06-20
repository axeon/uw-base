package uw.httpclient.util;

import okhttp3.*;

import java.io.IOException;
import java.nio.charset.Charset;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

/**
 * HTTP Basic 认证器。
 * <p>
 * 实现 OkHttp {@link Authenticator}，在服务端返回 401 时自动附带
 * {@code Authorization: Basic <base64(user:pass)>} 头重试。
 * <p>
 * 已对无限重试做防护：若上一轮请求已带过 {@code Authorization} 头仍被 401，
 * 说明凭证无效，将返回 {@code null} 放弃重试，避免请求风暴。
 *
 * @since 2018-04-28
 */
public class HttpBasicAuthenticator implements Authenticator {

    /**
     * 用户名。
     */
    private final String username;

    /**
     * 密码。
     */
    private final String password;

    /**
     * 凭证编码所用字符集。
     */
    private final Charset charset;

    /**
     * 构造 Basic 认证器，使用指定字符集编码凭证。
     *
     * @param username 用户名。
     * @param password 密码。
     * @param charset  凭证编码字符集。
     */
    public HttpBasicAuthenticator(String username, String password, Charset charset) {
        this.username = username;
        this.password = password;
        this.charset = charset;
    }

    /**
     * 构造 Basic 认证器，默认使用 ISO-8859-1 编码（RFC 7617 推荐）。
     *
     * @param username 用户名。
     * @param password 密码。
     */
    public HttpBasicAuthenticator(String username, String password) {
        this(username, password, ISO_8859_1);
    }

    /**
     * 在收到 401 时附加 Basic 认证头并重试。
     * <p>
     * 若原请求已携带 {@code Authorization} 头，说明凭证已被拒，返回 {@code null} 放弃重试。
     *
     * @param route    当前路由信息，可能为 null。
     * @param response 触发认证的 401 响应。
     * @return 附加了 Authorization 头的新请求；放弃重试时返回 null。
     * @throws IOException 当构造凭证时发生 IO 错误。
     */
    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        // 若上一轮已带过Authorization仍被401，说明凭证无效，放弃重试以避免死循环。
        if (response.request().header("Authorization") != null) {
            return null;
        }
        String credential = Credentials.basic(username, password, charset);
        return response.request().newBuilder().header("Authorization", credential).build();
    }
}
