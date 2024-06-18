package uw.dao.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用来生成where开始的查询字段信息的注解。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface QueryMeta {

    /**
     * sql中需要包含字段，表达式，预算符，占位信息。
     * 比如"col=?,col like ?,col in (?)"。
     *
     * 特殊情况支持：
     * 1.无占位符情况。 expr="cols>0", value="1"。只要value有数值，即生效。
     * 2.多占位符支持。 expr="(col1 like ? or col2 like ? or col3 like ?)", value="value"。
     * 3.in支持。 expr="col in (?)",value = Integer[]/List。
     * 4.运算符嵌入数值。 expr="col ?", value=">0"/"<=100"/"!=99"。
     *
     * @return
     */
    String expr() default "";

    /**
     * sql查询时的运算符，and ,or ,in
     *
     * @return
     */
    String op() default "and";

}
