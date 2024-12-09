package uw.httpclient.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

public class SSLContextUtils {

    private static final Logger logger = LoggerFactory.getLogger( SSLContextUtils.class );
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
    private static SSLContext TRUST_ALL_CONTEXT = null;

    /**
     * 全部信任的SSLContext初始化。
     */
    static {
        try {
            TRUST_ALL_CONTEXT = SSLContext.getInstance( "TLS" );
            TRUST_ALL_CONTEXT.init( null, new TrustManager[]{TRUEST_ALL_MANAGER}, new java.security.SecureRandom() );
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.error( "全部信任SSL证书环境初始化失败！{}", e.getMessage(), e );
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
     * @return
     */
    public static X509TrustManager getTrustAllManager(){
        return TRUEST_ALL_MANAGER;
    }


}
