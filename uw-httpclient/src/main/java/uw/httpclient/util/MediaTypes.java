package uw.httpclient.util;

import okhttp3.MediaType;

/**
 * 常用的MediaType常量。
 */
public class MediaTypes {

    public final static MediaType FORM_UTF8 = MediaType.parse( "application/x-www-form-urlencoded; charset=utf-8" );
    public final static MediaType FORM_GBK = MediaType.parse( "application/x-www-form-urlencoded; charset=gbk" );
    public final static MediaType JSON_UTF8 = MediaType.parse( "application/json; charset=utf-8" );
    public final static MediaType JSON_GBK = MediaType.parse( "application/json; charset=gbk" );
    public final static MediaType XML_UTF8 = MediaType.parse( "application/xml; charset=utf-8" );
    public final static MediaType XML_GBK = MediaType.parse( "application/xml; charset=gbk" );
}
