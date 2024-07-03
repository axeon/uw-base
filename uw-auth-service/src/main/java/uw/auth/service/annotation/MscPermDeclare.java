package uw.auth.service.annotation;

import uw.auth.service.constant.ActionLog;
import uw.auth.service.constant.AuthType;
import uw.auth.service.constant.UserType;

import java.lang.annotation.*;

/**
 * Msc权限声明注解
 */
@Target({ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MscPermDeclare {
    /**
     * 权限名称
     *
     * @return
     */
    String name() default "";

    /**
     * 权限名称描述
     *
     * @return
     */
    String description() default "";

    /**
     * 权限描述 通常是URI
     *
     * @return
     */
    String uri() default "";

    /**
     * 权限关联的Code
     *
     * @return
     */
    UserType user() default UserType.ANONYMOUS;

    /**
     * 验证类型
     *
     * @return
     */
    AuthType auth() default AuthType.PERM;

    /**
     * 记录日志类型,默认不记录
     *
     * @return
     */
    ActionLog log() default ActionLog.REQUEST;

}
