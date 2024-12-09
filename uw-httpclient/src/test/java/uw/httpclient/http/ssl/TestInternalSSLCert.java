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
            .trustManager( SSLContextUtils.getTrustAllManager() )
            .sslSocketFactory( SSLContextUtils.getTruestAllSocketFactory())
            .hostnameVerifier( (host, session) -> true )
            .build() );


    public static void main(String[] args) {
        HttpData httpData = JSON_HTTP_INTERFACE.getForData( "https://openservice.open.uat.ctripqa.com/openservice/serviceproxy" +
                ".ashx?icode=560950401e1b4a4db166a3c8cbb46abb&aid=161&uuid=9dca5de6-5d25-4b13-91e6-bed5c339f06e&sid=368&token=961466acf0c94af3839d012da311a03c" );
        System.out.println( httpData );

    }
}
