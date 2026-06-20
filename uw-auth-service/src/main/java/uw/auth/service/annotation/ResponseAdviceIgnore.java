package uw.auth.service.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 跳过 {@code GlobalResponseAdvice} 包裹的标记注解。
 * <p>
 * 标注在 Controller 类或方法上后，返回值将<b>不会被</b>自动包裹为 {@code ResponseData}。
 * 典型场景：文件下载、SSE 流、纯文本/二进制输出等需要直接控制响应体的接口。
 *
 * @author axeon
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ResponseAdviceIgnore {

}
