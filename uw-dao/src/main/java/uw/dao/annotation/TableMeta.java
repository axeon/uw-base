package uw.dao.annotation;

import java.lang.annotation.*;

/**
 * 表的属性.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface TableMeta {
    /**
     * 表名.
     *
     * @return 表名
     */
    public String tableName() default "";

    /**
     * 表类型.
     *
     * @return 表类型
     */
    public String tableType() default "";

    /**
     * where之前的查询sql信息。
     * 如果不设置，则默认为select * from tableName().
     *
     * @return
     */
    String sql() default "";

}
