package uw.httpclient.util;

import okhttp3.MediaType;

/**
 * 常用的 HTTP MediaType 常量集合。
 * <p>
 * 覆盖表单、JSON、XML 在 UTF-8 与 GBK 编码下的 MediaType，以及通用二进制流类型，
 * 供 {@code uw.httpclient.http.HttpInterface} 及业务代码复用，避免重复 {@code MediaType.parse}。
 */
public class MediaTypes {

    /**
     * application/x-www-form-urlencoded; charset=utf-8，UTF-8 编码的表单提交。
     */
    public final static MediaType FORM_UTF8 = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");

    /**
     * application/x-www-form-urlencoded; charset=gbk，GBK 编码的表单提交。
     */
    public final static MediaType FORM_GBK = MediaType.parse("application/x-www-form-urlencoded; charset=gbk");

    /**
     * application/json; charset=utf-8，UTF-8 编码的 JSON 请求/响应。
     */
    public final static MediaType JSON_UTF8 = MediaType.parse("application/json; charset=utf-8");

    /**
     * application/json; charset=gbk，GBK 编码的 JSON 请求/响应。
     */
    public final static MediaType JSON_GBK = MediaType.parse("application/json; charset=gbk");

    /**
     * application/xml; charset=utf-8，UTF-8 编码的 XML 请求/响应。
     */
    public final static MediaType XML_UTF8 = MediaType.parse("application/xml; charset=utf-8");

    /**
     * application/xml; charset=gbk，GBK 编码的 XML 请求/响应。
     */
    public final static MediaType XML_GBK = MediaType.parse("application/xml; charset=gbk");

    /**
     * application/octet-stream，通用二进制流（用于文件上传的默认文件类型）。
     */
    public final static MediaType OCTET_STREAM = MediaType.parse("application/octet-stream");
}
