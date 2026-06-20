package uw.common.app.constant;

/**
 * 常用常量。
 * <p>
 * 集中维护分隔符、控制符、编码等高频字面量，避免在业务代码中散落魔法值。
 * 该类为工具常量类，不可实例化。
 * </p>
 */
public final class CommonConstants {

    /**
     * 私有构造器，禁止实例化。
     */
    private CommonConstants() {
    }

    /**
     * 空字符串
     */
    public static final String EMPTY = "";

    /**
     * 空格字符串
     */
    public static final String SPACE = " ";

    /**
     * 空格字符
     */
    public static final char EMPTY_CHAR = ' ';

    /**
     * null字符串
     */
    public static final String NULL = "null";

    /**
     * 空json字符串
     */
    public static final String EMPTY_JSON = "{}";

    /**
     * 冒号
     */
    public static final String COLON = ":";

    /**
     * 逗号
     */
    public static final String COMMA = ",";

    /**
     * 分号
     */
    public static final String SEMICOLON = ";";

    /**
     * 逗号字符
     */
    public static final char COMMA_CHAR = ',';

    /**
     * 星号
     */
    public static final char STAR = '*';

    /**
     * 小数点
     */
    public static final char DOT = '.';

    /**
     * 反斜杠
     */
    public static final char SLASH = '/';

    /**
     * 斜杠
     */
    public static final char BACK_SLASH = '\\';

    /**
     * 换行符
     */
    public static final char NEWLINE = '\n';

    /**
     * tab符
     */
    public static final char TAB = '\t';

    /**
     * IO Buffer 大小
     */
    public static final int IO_BUFFER_SIZE = 16384;


    /**
     * 请求头语言环境
     */
    public static final String ACCEPT_LANG = "Accept-Language";

    /**
     * 字符编码
     */
    public static final String UTF_8 = "UTF-8";


}
