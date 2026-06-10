package uw.common.util;

/**
 * 异常工具类。
 * 用于过滤和优化堆栈信息。
 */
public class ExceptionUtils {

    private static final String[] FILTERED_PREFIXES = {
            "java.base", "org.spring", "org.apache", "jakarta", "com.mysql", "okhttp", "com.fasterxml", "uw.auth.service.filter"
    };

    private static boolean isFiltered(String className) {
        for (String prefix : FILTERED_PREFIXES) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 打印异常信息,屏蔽掉框架自身的堆栈输出。
     *
     * @param e 需要打印的异常信息
     * @return
     */
    public static String exceptionToString(Throwable e) {
        StringBuilder sb = new StringBuilder(256);
        Throwable current = e;
        while (current != null) {
            if (current != e) {
                sb.append("Caused by: ");
            }
            sb.append(current.toString()).append("\n");
            for (StackTraceElement traceElement : current.getStackTrace()) {
                if (isFiltered(traceElement.getClassName())) {
                    continue;
                }
                sb.append("\tat ").append(traceElement).append("\n");
            }
            current = current.getCause();
        }
        return sb.toString();
    }

}
