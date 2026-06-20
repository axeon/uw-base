package uw.auth.service.annotation;

import uw.auth.service.constant.ActionLog;
import uw.auth.service.constant.AuthType;
import uw.auth.service.constant.UserType;

import java.lang.annotation.*;

/**
 * 权限声明注解。
 * <p>
 * 标注在 Controller 类或方法上，声明该接口所需的 {@link UserType 用户类型}、{@link AuthType 授权级别}
 * 与 {@link ActionLog 日志级别}。{@code AuthServiceFilter} 在请求进入时解析本注解，
 * 据此完成 Token 校验、权限判定与操作日志记录；应用启动时 {@code MscAppUpdateService}
 * 会扫描本注解并将权限信息注册到 auth-center。
 * <p>
 * <b>注意</b>：权限控制基于「精确请求 URI + 请求方法」匹配，<b>不支持路径变量</b>。
 * 带 {@code {id}} 等路径变量的接口不应使用 {@link AuthType#PERM}/{@link AuthType#SUDO}，
 * 否则权限无法命中。
 *
 * @author axeon
 * @see UserType
 * @see AuthType
 * @see ActionLog
 */
@Target({ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MscPermDeclare {

    /**
     * 权限名称，用于菜单/权限列表展示。为空时回退取 {@code @Operation.summary()}。
     *
     * @return 权限名称
     */
    String name() default "";

    /**
     * 权限描述，用于菜单/权限列表补充说明。为空时回退取 {@code @Tag.description()} 或 {@code @Operation.description()}。
     *
     * @return 权限描述
     */
    String description() default "";

    /**
     * 自定义权限标识，通常是 URI 前缀（如一级菜单）。
     * <p>
     * 一般方法级权限无需显式指定，框架会自动使用「请求 URI + 请求方法」作为权限 code。
     * 仅类级菜单声明时需要，用于在 auth-center 注册一级菜单路径。
     *
     * @return 权限标识
     */
    String uri() default "";

    /**
     * 接口允许访问的 {@link UserType 用户类型}。<b>单值</b>，必须精确匹配。
     * 默认 {@link UserType#ANY} 表示任意用户类型（需配合 {@link AuthType#NONE}）。
     *
     * @return 用户类型
     */
    UserType user() default UserType.ANY;

    /**
     * 授权级别，决定 Token 校验与权限集合校验的严格程度。
     *
     * @return 授权类型
     */
    AuthType auth() default AuthType.USER;

    /**
     * 操作日志记录级别，默认 {@link ActionLog#NONE} 不记录。
     * <p>
     * {@link ActionLog#CRIT} 或 {@link AuthType#SUDO} 接口会同时写入 ES 与
     * {@code AuthCriticalLogStorage}（数据库）。
     *
     * @return 日志级别
     */
    ActionLog log() default ActionLog.NONE;

}
