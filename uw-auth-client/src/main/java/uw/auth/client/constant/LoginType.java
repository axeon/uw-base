package uw.auth.client.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

/**
 * 登录类型
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@Schema(title = "登录类型", description = "登录类型")
public enum LoginType {

    /**
     * 重复登录
     */
    LOGIN_DOUBLE( -2, "重复登录" ),

    /**
     * 踢出用户
     */
    KICK_OUT( -4, "踢出用户" ),

    /**
     * 退出登录。
     */
    LOGOUT( -1, "退出登录" ),

    /**
     * TOKEN刷新。
     */
    REFRESH_TOKEN( 0, "TOKEN刷新" ),

    /**
     * 用户名密码登录。
     */
    USER_PASS( 1, "用户名密码登录" ),

    /**
     * Email密码登录。
     */
    EMAIL_PASS( 2, "Email密码登录" ),

    /**
     * 手机号密码登录。
     */
    MOBILE_PASS( 3, "手机号密码登录" ),

    /**
     * TOTP恢复码登录
     */
    TOTP_RECOVERY_CODE( 20, "TOTP恢复码登录"),

    /**
     * TOTP验证码登录
     */
    TOTP_CODE( 21, "TOTP验证码登录" ),

    /**
     * Email验证码登录
     */
    EMAIL_CODE( 22, "Email验证码登录" ),

    /**
     * 手机验证码登录
     */
    MOBILE_CODE( 23, "手机验证码登录" ),

    /**
     * 微信登录。
     */
    OPEN_WECHAT( 31, "微信平台登录" ),

    /**
     * 抖音登录。
     */
    OPEN_DOUYIN( 32, "抖音平台登录" ),

    /**
     * 小红书平台登录。
     */
    OPEN_REDNOTE( 33, "小红书平台登录" ),

    /**
     * 快手平台登录。
     */
    OPEN_KUAISHOU( 34, "快手平台登录" ),

    /**
     * 阿里平台登录。
     */
    OPEN_ALI( 35, "阿里平台登录" ),

    /**
     * 拼多多平台登录。
     */
    OPEN_PDD( 36, "拼多多平台登录" ),

    /**
     * 其它开放平台。
     */
    OPEN_OTHER( 99, "其它开放平台" );


    /**
     * 参数值
     */
    private final int value;

    /**
     * 参数信息。
     */
    private final String label;

    LoginType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 检查类型值是否合法。
     *
     * @param value
     * @return
     */
    public static boolean checkTypeValid(int value) {
        for (LoginType type : LoginType.values()) {
            if (value == type.value) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据值获取枚举。
     *
     * @param value
     * @return
     */
    public static LoginType valueOf(int value) {
        for (LoginType type : LoginType.values()) {
            if (value == type.value) {
                return type;
            }
        }
        return null;
    }

    /**
     * 所有登录类型。
     *
     * @return
     */
    public static Set<LoginType> types() {
        return Set.of(values());
    }

    /**
     * 输入登录类型。
     * 包括密码和验证码登录。
     *
     * @return
     */
    public static Set<LoginType> inputTypes() {
        return Set.of(USER_PASS, EMAIL_PASS, MOBILE_PASS, EMAIL_CODE, MOBILE_CODE);
    }

    /**
     * 检查是否是输入登录类型。
     * 包括密码和验证码登录。
     *
     * @param value
     * @return
     */
    public static boolean isInputType(int value) {
        if (value >= USER_PASS.getValue() && value <= MOBILE_CODE.getValue()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 检查是否是开放平台登录类型。
     * @return
     */
    public static Set<LoginType> openTypes() {
        return Set.of(OPEN_WECHAT, OPEN_DOUYIN, OPEN_REDNOTE, OPEN_KUAISHOU, OPEN_ALI, OPEN_PDD, OPEN_OTHER);
    }

    /**
     * 检查是否是开放平台登录类型。
     * @param value
     * @return
     */
    public static boolean isOpenType(int value) {
        if (value >= OPEN_WECHAT.getValue() && value <= OPEN_OTHER.getValue()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 用户名密码登录类型。
     *
     * @return
     */
    public static Set<LoginType> userPassTypes() {
        return Set.of(USER_PASS);
    }

    /**
     * 是否是用户名密码类型。
     *
     * @param value
     * @return
     */
    public static boolean isUserPassType(int value) {
        if (value == USER_PASS.getValue()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 密码登录类型。
     * @return
     */
    public static Set<LoginType> passTypes() {
        return Set.of(USER_PASS, EMAIL_PASS, MOBILE_PASS);
    }

    /**
     * 是否是密码类型。
     *
     * @param value
     * @return
     */
    public static boolean isPassType(int value) {
        if (value >= USER_PASS.getValue() && value <= MOBILE_PASS.getValue()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 验证码登录类型。
     * @return
     */
    public static Set<LoginType> codeTypes() {
        return Set.of(EMAIL_CODE, MOBILE_CODE);
    }

    /**
     * 是否是验证码类型。
     *
     * @param value
     * @return
     */
    public static boolean isCodeType(int value) {
        if (value >= EMAIL_CODE.getValue() && value <= MOBILE_CODE.getValue()) {
            return true;
        } else {
            return false;
        }
    }

    public String getLabel() {
        return label;
    }

    public int getValue() {
        return value;
    }
}
