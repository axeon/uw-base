package uw.auth.service.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

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
     * Email验证码登录
     */
    EMAIL_CODE( 22, "Email验证码登录" ),

    /**
     * 手机号验证码登录
     */
    MOBILE_CODE( 23, "手机号验证码登录" ),

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
     * 检查是否是输入登录类型。
     * 包括密码和验证码登录。
     *
     * @param value
     * @return
     */
    public static boolean isInputLoginType(int value) {
        if (value >= USER_PASS.getValue() && value <= MOBILE_CODE.getValue()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 是否是用户名密码类型。
     *
     * @param value
     * @return
     */
    public static boolean isAccountPassLoginType(int value) {
        if (value == USER_PASS.getValue()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 是否是密码类型。
     *
     * @param value
     * @return
     */
    public static boolean isPassLoginType(int value) {
        if (value >= USER_PASS.getValue() && value <= MOBILE_PASS.getValue()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 是否是验证码类型。
     *
     * @param value
     * @return
     */
    public static boolean isCodeLoginType(int value) {
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
