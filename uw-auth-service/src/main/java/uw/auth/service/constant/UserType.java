package uw.auth.service.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户类型。
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@Schema(title = "用户类型", description = "用户类型")
public enum UserType {

    /**
     * 匿名用户类型
     * 未指定权限类型时候的默认值
     */
    ANONYMOUS( -1, "匿名用户" ),

    /**
     * C站用户类型
     * 用于登录C站
     */
    GUEST( 0, "C站用户" ),

    /**
     * RPC用户类型
     */
    RPC( 1, "RPC用户" ),

    /**
     * 超级管理员。
     */
    ROOT( 100, "超级管理员" ),

    /**
     * devops用户。
     * 用于登陆开发和运维管理工具。
     */
    OPS( 110, "开发运维" ),

    /**
     * 平台管理员。
     * 用于登录业务管理后台。
     */
    ADMIN( 200, "平台管理员" ),

    /**
     * SAAS用户。
     */
    SAAS( 300, "SAAS运营商" ),

    /**
     * SAAS商户。
     */
    MCH( 310, "SAAS商户" );


    /**
     * 参数值
     */
    private final int value;

    /**
     * 参数信息。
     */
    private final String label;

    UserType(int value, String label) {
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
        for (UserType type : UserType.values()) {
            if (value == type.value) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据数值匹配合适的UserType。
     *
     * @param value
     * @return
     */
    public static UserType findByValue(int value) {
        for (UserType type : UserType.values()) {
            if (value == type.value) {
                return type;
            }
        }
        return ANONYMOUS;
    }

    /**
     * 根据数值获得label。
     *
     * @param value
     * @return
     */
    public static String getLabel(int value) {
        return findByValue( value ).label;
    }


    /**
     * 获得有菜单的用户类型。
     *
     * @return
     */
    public static UserType[] getMscMenuUserTypes() {
        return new UserType[]{UserType.ROOT, UserType.OPS, UserType.ADMIN, UserType.SAAS, UserType.MCH};
    }

    /**
     * 获得ROOT可管理类型列表。
     *
     * @return
     */
    public static UserType[] getRootManageTypes() {
        return new UserType[]{UserType.ROOT, UserType.RPC, UserType.OPS, UserType.ADMIN};
    }

    /**
     * 获得ROOT可管理类型列表。
     *
     * @return
     */
    public static UserType[] getAdminManageTypes() {
        return new UserType[]{UserType.ADMIN};
    }

    /**
     * 获得SAASAdmin可管理类型列表。
     *
     * @return
     */
    public static UserType[] getSaasManageTypes() {
        return new UserType[]{UserType.SAAS, UserType.MCH};
    }

    /**
     * 是否是超级管理员管理类型。
     *
     * @param value
     * @return
     */
    public static boolean isRootManageType(int value) {
        for (UserType type : getRootManageTypes()) {
            if (value == type.value) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否是平台管理员管理类型。
     *
     * @param value
     * @return
     */
    public static boolean isAdminManageType(int value) {
        for (UserType type : getAdminManageTypes()) {
            if (value == type.value) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否是Saas管理员管理类型。
     *
     * @param value
     * @return
     */
    public static boolean isSaasManageType(int value) {
        for (UserType type : getSaasManageTypes()) {
            if (value == type.value) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否是商户类型。
     *
     * @param value
     * @return
     */
    public static boolean isMchType(int value) {
        return value == UserType.MCH.getValue();
    }

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

}
