package uw.logback.es.util;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;

/**
 * 打印logback的异常输出。
 */
public class ThrowableProxyUtils {

    private static final String JSON_LINE_SEPARATOR = "\\n";

    /**
     * 堆栈输出长度限制。
     */
    public static int MaxDepthPerThrowable = 30;

    /**
     * 需要忽略的堆栈输出。
     */
    public static String[] ExcludeThrowableKeys = new String[]{"org.spring", "org.apache", "java.base", "jakarta", "com.mysql"};

    /**
     * 输出异常到buffer。
     *
     * @param buf
     * @param tp
     * @return
     */
    public static void writeThrowable(okio.Buffer buf, IThrowableProxy tp) {
        recursiveAppend( buf, null, 0, tp );
    }

    private static void recursiveAppend(okio.Buffer buf, String prefix, int indent, IThrowableProxy tp) {
        if (tp == null) return;
        subjoinFirstLine( buf, prefix, indent, tp );
        buf.writeUtf8( JSON_LINE_SEPARATOR );
        subjoinSTEPArray( buf, indent, tp );
        IThrowableProxy[] suppressed = tp.getSuppressed();
        if (suppressed != null) {
            for (IThrowableProxy current : suppressed) {
                recursiveAppend( buf, "Suppressed: ", indent + ThrowableProxyUtil.SUPPRESSED_EXCEPTION_INDENT, current );
            }
        }
        recursiveAppend( buf, "Caused by: ", indent, tp.getCause() );
    }

    /**
     * 打印首行。
     *
     * @param buf
     * @param prefix
     * @param indent
     * @param tp
     */
    private static void subjoinFirstLine(okio.Buffer buf, String prefix, int indent, IThrowableProxy tp) {
        buf.writeUtf8( indentJsonTab( indent ) );
        if (prefix != null) {
            buf.writeUtf8( prefix );
        }
        if (tp.isCyclic()) {
            buf.writeUtf8( "[CIRCULAR REFERENCE: " ).writeUtf8( tp.getClassName() ).writeUtf8( ": " ).writeUtf8( JsonEncoderUtils.escapeJSON( tp.getMessage() ) ).writeUtf8( "]" );
        } else {
            buf.writeUtf8( tp.getClassName() ).writeUtf8( ": " ).writeUtf8( JsonEncoderUtils.escapeJSON( tp.getMessage() ) );
        }
    }

    /**
     * 打印堆栈输出。
     *
     * @param buf
     * @param indent
     * @param tp
     */
    private static void subjoinSTEPArray(okio.Buffer buf, int indent, IThrowableProxy tp) {
        StackTraceElementProxy[] stepArray = tp.getStackTraceElementProxyArray();
        int commonFrames = tp.getCommonFrames();
        boolean unrestrictedPrinting = MaxDepthPerThrowable > stepArray.length;
        int maxIndex = (unrestrictedPrinting) ? stepArray.length : MaxDepthPerThrowable;
        if (commonFrames > 0 && unrestrictedPrinting) {
            maxIndex -= commonFrames;
        }
        int ignoredCount = 0;

        for (int i = 0; i < maxIndex; i++) {
            StackTraceElementProxy element = stepArray[i];
            if (!isIgnoredStackTraceLine( element.toString() )) {
                buf.writeUtf8( indentJsonTab( indent ) );
                buf.writeUtf8( element.getSTEAsString() );
                if (ignoredCount > 0) {
                    printIgnoredCount( buf, ignoredCount );
                }
                ignoredCount = 0;
                buf.writeUtf8( JSON_LINE_SEPARATOR );
            } else {
                ++ignoredCount;
                if (maxIndex < stepArray.length) {
                    ++maxIndex;
                }
            }
        }

        if (ignoredCount > 0) {
            printIgnoredCount( buf, ignoredCount );
        }

        if (commonFrames > 0 && unrestrictedPrinting) {
            buf.writeUtf8( indentJsonTab( indent ) );
            buf.writeUtf8( "... " ).writeUtf8( String.valueOf( tp.getCommonFrames() ) ).writeUtf8( " common frames omitted" );
            buf.writeUtf8( JSON_LINE_SEPARATOR );
        }
    }

    /**
     * 打印指定长度的缩紧。
     *
     * @param indent
     * @return
     */
    private static String indentJsonTab(int indent) {
        return switch (indent) {
            case 1 -> "\\t";
            case 2 -> "\\t\\t";
            case 3 -> "\\t\\t\\t";
            case 4 -> "\\t\\t\\t\\t";
            case 5 -> "\\t\\t\\t\\t\\t";
            case 6 -> "\\t\\t\\t\\t\\t\\t";
            case 7 -> "\\t\\t\\t\\t\\t\\t\\t";
            case 8 -> "\\t\\t\\t\\t\\t\\t\\t\\t";
            case 9 -> "\\t\\t\\t\\t\\t\\t\\t\\t\\t";
            default -> "";
        };
    }


    /**
     * 打印出已忽略输出的行数。
     *
     * @param buf
     * @param ignoredCount
     */
    private static void printIgnoredCount(okio.Buffer buf, int ignoredCount) {
        buf.writeUtf8( " [" ).writeUtf8( String.valueOf( ignoredCount ) ).writeUtf8( " skipped]" );
    }

    /**
     * 是否是需要忽略的堆栈输出。
     *
     * @param line
     * @return
     */
    private static boolean isIgnoredStackTraceLine(String line) {
        if (ExcludeThrowableKeys != null) {
            for (String ignoredStackTraceLine : ExcludeThrowableKeys) {
                if (line.contains( ignoredStackTraceLine )) {
                    return true;
                }
            }
        }
        return false;
    }

}
