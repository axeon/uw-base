package uw.httpclient.http.json;

import org.junit.Test;
import uw.httpclient.http.HttpConfig;
import uw.httpclient.http.HttpData;
import uw.httpclient.http.HttpInterface;
import uw.httpclient.http.json.vo.TestVo;
import uw.httpclient.json.JsonInterfaceHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * JsonInterfaceHelper测试类。
 *
 * @since 2018-03-01
 */
public class JsonInterfaceTest {

    private static final HttpInterface jsonHelper =
            new JsonInterfaceHelper( HttpConfig.builder().connectTimeout( 30000 ).readTimeout( 30000 ).writeTimeout( 30000 ).keepAliveTimeout( 60000 ).maxRequestsPerHost( 256 ).maxRequests( 25600 ).retryOnConnectionFailure( false ).build() );

    @Test
    public void testGetData() throws Exception {
        HttpData data = jsonHelper.getForData( "https://www.baidu.com" );
        System.out.println( data );
    }

    @Test
    public void testGetErrorData() throws Exception {
        try {
            HttpData data = jsonHelper.getForData( "https://www.baidu.com1" );
            System.out.println( data );
        } catch (Exception e) {
        }
    }

    @Test
    public void testGetDataWithQueryParam() throws Exception {
        Map<String, String> queryParam = new HashMap<>();
        queryParam.put( "test", "123" );
        HttpData data = jsonHelper.getForData( "https://www.baidu.com", queryParam );
        System.out.println( data );
    }

    @Test
    public void testPostDataWithFormData() throws Exception {
        Map<String, String> formData = new HashMap<>();
        formData.put( "test", "123" );
        HttpData data = jsonHelper.postFormForData( "https://www.baidu.com", formData );
        System.out.println( data );
    }

    @Test
    public void testPostDataWithRequestBody() throws Exception {
        TestVo vo = new TestVo();
        vo.setName( "test" );
        vo.setAge( 18 );
        vo.setAddress( "planet" );
        Map<String, String> headers = new HashMap<>();
        headers.put( "username", "test" );
        headers.put( "password", "test" );
        HttpData data = jsonHelper.postBodyForData( "http://192.168.88.21:80/123", headers, vo );
        System.out.println( data );
    }

    @Test
    public void testPostFormData() throws Exception {
        Map<String, String> loginParam = new HashMap<>();
        loginParam.put( "username", "test" );
        loginParam.put( "password", "test" );
    }

    @Test
    public void testPostFormFile() throws Exception {
        Map<String, String> loginParam = new HashMap<>();
        loginParam.put( "username", "test" );
        loginParam.put( "password", "test" );

    }


    @Test
    public void testGetImage() throws Exception {
        HttpData data = jsonHelper.getForData( "https://www.baidu.com/img/flexible/logo/pc/result.png" );
        System.out.println( data );
    }

}
