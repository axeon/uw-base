package uw.auth.service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;

public class MscUtils {

    private static final Logger log = LoggerFactory.getLogger( MscUtils.class );

    /**
     * to sanitizeUrl url
     *
     * @param path
     * @return
     */
    public static String sanitizeUrl(final String path) {
        String sanitized = path;
        while (true) {
            int index = sanitized.indexOf( "//" );
            if (index < 0) {
                break;
            } else {
                sanitized = sanitized.substring( 0, index ) + sanitized.substring( index + 1 );
            }
        }
        return sanitized;
    }

    /**
     * 打印异常信息,屏蔽掉spring自己的堆栈输出。
     *
     * @param e 需要打印的异常信息
     * @return
     */
    public static String exceptionToString(Throwable e) {
        StringBuilder sb = new StringBuilder( 256 );
        sb.append( e.toString() ).append( "\n" );

        StackTraceElement[] trace = e.getStackTrace();
        for (StackTraceElement traceElement : trace) {
            if (traceElement.getClassName().startsWith( "org.spring" )) {
                continue;
            }
            if (traceElement.getClassName().startsWith( "java.base." )) {
                continue;
            }
            if (traceElement.getClassName().startsWith( "sun." )) {
                continue;
            }
            if (traceElement.getClassName().startsWith( "jakarta." )) {
                continue;
            }
            if (traceElement.getClassName().startsWith( "org.apache" )) {
                continue;
            }
            if (traceElement.getClassName().startsWith( "com.mysql" )) {
                continue;
            }
            sb.append( "\tat " ).append( traceElement ).append( "\n" );
        }
        Throwable ourCause = e.getCause();
        if (ourCause != null) {
            sb.append( "Caused by: " ).append( ourCause.toString() ).append( "\n" );
            trace = ourCause.getStackTrace();
            for (StackTraceElement traceElement : trace) {
                if (traceElement.getClassName().startsWith( "org.spring" )) {
                    continue;
                }
                if (traceElement.getClassName().startsWith( "java.base." )) {
                    continue;
                }
                if (traceElement.getClassName().startsWith( "sun." )) {
                    continue;
                }
                if (traceElement.getClassName().startsWith( "jakarta." )) {
                    continue;
                }
                if (traceElement.getClassName().startsWith( "org.apache" )) {
                    continue;
                }
                if (traceElement.getClassName().startsWith( "com.mysql" )) {
                    continue;
                }
                sb.append( "\tat " ).append( traceElement ).append( "\n" );
            }
        }
        return sb.toString();
    }

}
