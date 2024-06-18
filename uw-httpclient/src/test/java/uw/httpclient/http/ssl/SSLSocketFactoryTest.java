package uw.httpclient.http.ssl;

import org.junit.Test;
import uw.httpclient.http.HttpConfig;
import uw.httpclient.http.HttpInterface;
import uw.httpclient.json.JsonInterfaceHelper;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * SSLSocketFactory test
 *
 * @since 2018-10-30
 */
public class SSLSocketFactoryTest {
    private static final HttpInterface httpInterface = buildHttpInterface();

    @Test
    public void testSSLSocket() throws Exception {
        System.out.println( httpInterface.getForData( "https://express.tt.usj.co.jp/app/interface_cmp/A19/ticket/info" ) );
    }

    private static HttpInterface buildHttpInterface() {
        try {
            X509TrustManager trustManager = new X509TrustManagerImpl( new File( "/home/axeon/express.cer" ) );
            SSLContext sslContext = SSLContext.getInstance( "TLS" );
            sslContext.init( null, new TrustManager[]{trustManager}, null );
            return new JsonInterfaceHelper( HttpConfig.builder().sslSocketFactory( sslContext.getSocketFactory() ).trustManager( trustManager )
                    /**
                     * 主机名称验证器
                     */.hostnameVerifier( new HostnameVerifier() {
                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    } ).build() );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class X509TrustManagerImpl implements X509TrustManager {
        private X509Certificate[] certificates;

        /**
         * 初始化证书
         *
         * @param certificateFiles
         */
        public X509TrustManagerImpl(File... certificateFiles) {
            try {
                this.certificates = new X509Certificate[certificateFiles.length];
                CertificateFactory certificateFactory = CertificateFactory.getInstance( "X.509" );
                int i = 0;
                for (File file : certificateFiles) {
                    certificates[i] = (X509Certificate) certificateFactory.generateCertificate( new FileInputStream( file ) );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return certificates;
        }
    }
}
