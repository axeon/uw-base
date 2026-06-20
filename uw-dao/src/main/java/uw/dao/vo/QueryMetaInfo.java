package uw.dao.vo;

import java.lang.reflect.Field;

/**
 * 查询条件的meta信息.
 *
 * @author axeon
 */
public class QueryMetaInfo {

    /**
     * Java 属性名（预留字段，当前解析逻辑未直接使用）。
     */
    private String propertyName;

    /**
     * 查询 SQL 表达式，来自 {@link uw.dao.annotation.QueryMeta#expr()}，如 {@code "user_name like ?"}。
     */
    private String queryExpr;

    /**
     * 属性反射句柄，用于读取查询参数对象上对应字段的值。
     */
    private Field field;


    /**
     * 获取 Java 属性名。
     *
     * @return 属性名
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * 设置 Java 属性名。
     *
     * @param propertyName 属性名
     */
    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    /**
     * 获取查询 SQL 表达式。
     *
     * @return 查询表达式
     */
    public String getQueryExpr() {
        return queryExpr;
    }

    /**
     * 设置查询 SQL 表达式。
     *
     * @param queryExpr 查询表达式
     */
    public void setQueryExpr(String queryExpr) {
        this.queryExpr = queryExpr;
    }

    /**
     * 获取属性反射句柄。
     *
     * @return Field 对象
     */
    public Field getField() {
        return field;
    }

    /**
     * 设置属性反射句柄。
     *
     * @param field Field 对象
     */
    public void setField(Field field) {
        this.field = field;
    }
}