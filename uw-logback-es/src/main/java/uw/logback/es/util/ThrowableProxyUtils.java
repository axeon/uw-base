package uw.logback.es.util;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;

/**
 * 将 Logback 的异常代理（{@link IThrowableProxy}）格式化为压缩后的堆栈文本，写入 okio buffer。
 * <p>
 * 与默认堆栈输出的差异：
 * <ul>
 *   <li>受 {@link #MaxDepthPerThrowable} 限制单帧数量；</li>
 *   <li>匹配 {@link #ExcludeThrowableKeys} 前缀的堆栈帧会被折叠为 "[N skipped]"；</li>
 *   <li>换行/缩进采用字面 {@code \n}/{@code \t} 文本，以便嵌入 JSON 字符串值。</li>
 * </ul>
 * <p>
 * 注意：{@code MaxDepthPerThrowable} 与 {@code ExcludeThrowableKeys} 为静态共享变量，
 * 多个 appender 实例配置时会互相覆盖，最后一个启动的实例生效。
 */
public class ThrowableProxyUtils {

    /**
     * JSON 文本中的换行占位（字面两个字符 {@code \} {@code n}），ES 解析后还原为换行。
     */
    private static final String JSON_LINE_SEPARATOR = "\\n";

    /**
     * 堆栈输出长度限制（帧数）。由 appender 在启动时设置。
     */
    public static volatile int MaxDepthPerThrowable = 30;

    /**
     * 需要折叠的堆栈类名前缀数组。由 appender 在启动时设置。
     */
    public static volatile String[] ExcludeThrowableKeys = new String[]{"java.base", "org.spring", "org.apache", "jakarta", "com.mysql", "okhttp", "com.fasterxml", "uw.auth.service.filter"};

    /**
     * 把异常代理格式化后写入 buffer，包含 cause 链与 suppressed 异常。
     *
     * @param buf 目标 buffer
     * @param tp  异常代理，可为 null（将无输出）
     */
    public static void writeThrowable(okio.Buffer buf, IThrowableProxy tp) {
        recursiveAppend(buf, null, 0, tp);
    }

    /**
     * 递归追加：首行 + 堆栈帧 + suppressed + cause。
     *
     * @param buf    目标 buffer
     * @param prefix 行前缀（如 "Caused by: "、"Suppressed: "），顶层为 null
     * @param indent 缩进层级
     * @param tp     异常代理
     */
    private static void recursiveAppend(okio.Buffer buf, String prefix, int indent, IThrowableProxy tp) {
        if (tp == null) return;
        subjoinFirstLine(buf, prefix, indent, tp);
        buf.writeUtf8(JSON_LINE_SEPARATOR);
        subjoinSTEPArray(buf, indent, tp);
        IThrowableProxy[] suppressed = tp.getSuppressed();
        if (suppressed != null) {
            for (IThrowableProxy current : suppressed) {
                recursiveAppend(buf, "Suppressed: ", indent + ThrowableProxyUtil.SUPPRESSED_EXCEPTION_INDENT, current);
            }
        }
        recursiveAppend(buf, "Caused by: ", indent, tp.getCause());
    }

    /**
     * 写入异常首行：缩进 + 可选前缀 + 类名 + message。
     *
     * @param buf    目标 buffer
     * @param prefix 行前缀，为 null 则无
     * @param indent 缩进层级
     * @param tp     异常代理
     */
    private static void subjoinFirstLine(okio.Buffer buf, String prefix, int indent, IThrowableProxy tp) {
        buf.writeUtf8(indentJsonTab(indent));
        if (prefix != null) {
            buf.writeUtf8(prefix);
        }
        if (tp.isCyclic()) {
            buf.writeUtf8("[CIRCULAR REFERENCE: ").writeUtf8(tp.getClassName()).writeUtf8(": ").writeUtf8(JsonEncoderUtils.escapeJSON(tp.getMessage())).writeUtf8("]");
        } else {
            buf.writeUtf8(tp.getClassName()).writeUtf8(": ").writeUtf8(JsonEncoderUtils.escapeJSON(tp.getMessage()));
        }
    }

    /**
     * 写入堆栈帧数组，匹配 {@link #ExcludeThrowableKeys} 的帧被折叠为 "[N skipped]"。
     * <p>
     * 为保证折叠帧不挤占有效深度，忽略帧出现时会尝试扩展 maxIndex（受限于 2 倍最大深度）。
     *
     * @param buf    目标 buffer
     * @param indent 缩进层级
     * @param tp     异常代理
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
            StackTraceElementProxy elementProxy = stepArray[i];
            String steStr = elementProxy.getStackTraceElement().toString();
            if (!isIgnoredStackTraceLine(steStr)) {
                buf.writeUtf8(indentJsonTab(indent));
                buf.writeUtf8("at ");
                buf.writeUtf8(steStr);
                if (ignoredCount > 0) {
                    printIgnoredCount(buf, ignoredCount);
                }
                ignoredCount = 0;
                buf.writeUtf8(JSON_LINE_SEPARATOR);
            } else {
                ++ignoredCount;
                if (maxIndex < stepArray.length && maxIndex < MaxDepthPerThrowable * 2) {
                    ++maxIndex;
                }
            }
        }

        if (ignoredCount > 0) {
            printIgnoredCount(buf, ignoredCount);
        }

        if (commonFrames > 0 && unrestrictedPrinting) {
            buf.writeUtf8(indentJsonTab(indent));
            buf.writeUtf8("... ").writeUtf8(String.valueOf(tp.getCommonFrames())).writeUtf8(" common frames omitted");
            buf.writeUtf8(JSON_LINE_SEPARATOR);
        }
    }

    /**
     * 根据缩进层级返回对应的字面制表符占位串（{@code \t}），超过 9 层不缩进。
     *
     * @param indent 缩进层级
     * @return 制表符占位串
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
     * 写入折叠计数标记，如 " [3 skipped]"。
     *
     * @param buf          目标 buffer
     * @param ignoredCount 被跳过的帧数
     */
    private static void printIgnoredCount(okio.Buffer buf, int ignoredCount) {
        buf.writeUtf8(" [").writeUtf8(String.valueOf(ignoredCount)).writeUtf8(" skipped]");
    }

    /**
     * 判断堆栈帧文本是否命中 {@link #ExcludeThrowableKeys} 任一前缀。
     *
     * @param line 堆栈帧文本
     * @return true 表示该帧应被折叠跳过
     */
    private static boolean isIgnoredStackTraceLine(String line) {
        if (ExcludeThrowableKeys != null) {
            for (String ignoredStackTraceLine : ExcludeThrowableKeys) {
                if (line.startsWith(ignoredStackTraceLine)) {
                    return true;
                }
            }
        }
        return false;
    }

}
