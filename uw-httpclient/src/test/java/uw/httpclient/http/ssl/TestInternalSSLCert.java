package uw.httpclient.http.ssl;

import uw.httpclient.http.HttpConfig;
import uw.httpclient.http.HttpData;
import uw.httpclient.http.HttpInterface;
import uw.httpclient.json.JsonInterfaceHelper;
import uw.httpclient.util.SSLContextUtils;

public class TestInternalSSLCert {

    private static final HttpInterface JSON_HTTP_INTERFACE = new JsonInterfaceHelper( HttpConfig.builder()
            .connectTimeout( 30000 )
            .readTimeout( 30000 )
            .writeTimeout( 30000 )
            .trustManager( SSLContextUtils.getTrustAllManager() ).sslSocketFactory( SSLContextUtils.getTruestAllSocketFactory())
            .hostnameVerifier( (host, session) -> true )
            .build() );


    public static void main(String[] args) {
        HttpData httpData = JSON_HTTP_INTERFACE.getForData( "https://es-bjo2r42o-internal.private.tencentelasticsearch.com:9200" );

        System.out.println( httpData );

    }
}
