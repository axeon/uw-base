package uw.httpclient.util;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

/**
 * SSL/TLS 上下文工具类。
 * <p>
 * 提供信任全部证书的 {@link SSLContext} / {@link SSLSocketFactory} / {@link X509TrustManager}，
 * 用于访问内部自签名 HTTPS 服务。每个 HttpInterface 实例使用独立的 Dispatcher，
 * 不会因配置改动影响全局或其他实例。
 * <p>
 * <b>安全警告</b>：信任全部证书会绕过证书校验，存在中间人攻击风险，
 * 仅适用于内网自签名 / 测试环境，<b>切勿用于生产公网环境</b>。
 */
public class SSLContextUtils {

    /**
     * 全部信任证书管理器。
     */
    private static final X509TrustManager TRUEST_ALL_MANAGER =
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    // Do nothing
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    // Do nothing
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[]{};
                }

            };
    /**
     * 全部信任Context。
     */
    private static final SSLContext TRUST_ALL_CONTEXT;

    /**
     * 全部信任的SSLContext初始化。
     */
    static {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{TRUEST_ALL_MANAGER}, new java.security.SecureRandom());
            TRUST_ALL_CONTEXT = ctx;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            // 初始化失败属于不可恢复的环境问题，直接抛出使类加载失败，避免后续getTruestAllSocketFactory()在null上NPE。
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * 获取全部信任的SSLContext。
     *
     * @return 全部信任的SSLContext。
     */
    public static SSLContext getAllTruestContext() {
        return TRUST_ALL_CONTEXT;
    }

    /**
     * 获取全部信任的SSLSocketFactory。
     *
     * @return 全部信任的SSLSocketFactory。
     */
    public static SSLSocketFactory getTruestAllSocketFactory() {
        return TRUST_ALL_CONTEXT.getSocketFactory();
    }

    /**
     * 获取全部信任的X509TrustManager。
     *
     * @return 全部信任的X509TrustManager。
     */
    public static X509TrustManager getTrustAllManager() {
        return TRUEST_ALL_MANAGER;
    }


}
