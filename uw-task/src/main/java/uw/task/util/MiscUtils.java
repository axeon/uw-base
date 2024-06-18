package uw.task.util;

public class MiscUtils {


    /**
     * 打印异常信息，屏蔽掉spring自己的堆栈输出。
     *
     * @param e 需要打印的异常信息
     * @return
     */
    public static String exceptionToString(Throwable e) {
        StringBuilder sb = new StringBuilder(256);
        sb.append(e.toString()).append("\n");

        StackTraceElement[] trace = e.getStackTrace();
        for (StackTraceElement traceElement : trace) {
            if (traceElement.getClassName().startsWith("org.spring")) {
                continue;
            }
            if (traceElement.getClassName().startsWith("java.base.")) {
                continue;
            }
            if (traceElement.getClassName().startsWith("sun.")) {
                continue;
            }
            if (traceElement.getClassName().startsWith("jakarta.")) {
                continue;
            }
            if (traceElement.getClassName().startsWith("org.apache")) {
                continue;
            }
            if (traceElement.getClassName().startsWith("com.mysql")) {
                continue;
            }
            sb.append("\tat ").append(traceElement).append("\n");
        }
        Throwable ourCause = e.getCause();
        if (ourCause != null) {
            sb.append("Caused by: ").append(ourCause.toString()).append("\n");
            trace = ourCause.getStackTrace();
            for (StackTraceElement traceElement : trace) {
                if (traceElement.getClassName().startsWith("org.spring")) {
                    continue;
                }
                if (traceElement.getClassName().startsWith("java.base.")) {
                    continue;
                }
                if (traceElement.getClassName().startsWith("sun.")) {
                    continue;
                }
                if (traceElement.getClassName().startsWith("jakarta.")) {
                    continue;
                }
                if (traceElement.getClassName().startsWith("org.apache")) {
                    continue;
                }
                if (traceElement.getClassName().startsWith("com.mysql")) {
                    continue;
                }
                sb.append("\tat ").append(traceElement).append("\n");
            }
        }
        return sb.toString();
    }


    public static void main(String[] args) {
        String data = MiscUtils.exceptionToString(new Exception("test"));
        System.out.println(data);
    }
}
