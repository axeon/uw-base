package uw.auth.service.annotation;

import uw.auth.service.constant.RateLimitTarget;

import java.lang.annotation.*;

/**
 * Msc限速注解。
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimitDeclare {

    /**
     * 限定的请求数。
     *
     * @return
     */
    int requests() default 1;

    /**
     * 限定的秒数。
     *
     * @return
     */
    int seconds() default 1;

    /**
     * 限速类型
     *
     * @return
     */
    RateLimitTarget target() default RateLimitTarget.NONE;

}
