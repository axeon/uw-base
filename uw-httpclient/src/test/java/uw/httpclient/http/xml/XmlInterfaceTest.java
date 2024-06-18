package uw.httpclient.http.xml;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * XmlInterfaceHelper测试类。
 *
 * @since 2017/12/13
 */
public class XmlInterfaceTest {


    @Test
    public void testGet() throws Exception {

    }

    @Test
    public void testPostFormData() throws Exception {
        Map<String, String> loginParam = new HashMap<>();
        loginParam.put( "username", "test" );
        loginParam.put( "password", "test" );
    }

    @Test
    public void testPostRequestBody() throws Exception {

    }

}
