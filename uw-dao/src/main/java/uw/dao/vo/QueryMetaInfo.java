package uw.dao.vo;

import java.lang.reflect.Field;

/**
 * 查询条件的meta信息.
 *
 * @author axeon
 */
public class QueryMetaInfo {

    /**
     * java属性名.
     */
    private String propertyName;

    /**
     * 查询sql表达式.
     */
    private String queryExpr;


    /**
     * 查询sql运算符.
     */
    private String queryOp;

    /**
     * 属性反射句柄.
     */
    private Field field;


    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getQueryExpr() {
        return queryExpr;
    }

    public void setQueryExpr(String queryExpr) {
        this.queryExpr = queryExpr;
    }

    public String getQueryOp() {
        return queryOp;
    }

    public void setQueryOp(String queryOp) {
        this.queryOp = queryOp;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }
}