package uw.dao;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 查询参数的接口类。
 * 主要用于标识子类为查询参数。
 *
 * @author axeon
 */
public class QueryParam<P extends QueryParam<P>> implements Serializable {

    /**
     * like参数的最小长度。
     * 少于最小参数的，将转化为=查询。
     */
    @JsonIgnore
    private int LIKE_QUERY_PARAM_MIN_LEN = 5;

    /**
     * 是否开启LIKE查询。默认为true。
     */
    @JsonIgnore
    private boolean LIKE_QUERY_ENABLE = true;

    /**
     * select sql。
     * 如果有此属性，将不再从entity注解取值。
     * 网络禁止从参数传入！
     */
    @JsonIgnore
    private String SELECT_SQL;

    /**
     * 附加的where Sql。
     * 网络禁止从参数传入！
     */
    @JsonIgnore
    private StringBuilder EXT_WHERE_SQL;

    /**
     * 附加的参数组，使用and连接。
     * key: 带转义的sql，比如"and id=?"
     * value: 参数值。
     * 网络禁止从参数传入！
     */
    @JsonIgnore
    private Map<String, Object> EXT_PARAM_MAP;

    public QueryParam() {
    }

    /**
     * 携带select sql的构造器。
     *
     * @param SELECT_SQL
     */
    public QueryParam(String SELECT_SQL) {
        this.SELECT_SQL = SELECT_SQL;
    }

    @JsonIgnore
    public int GET_LIKE_QUERY_PARAM_MIN_LEN() {
        return LIKE_QUERY_PARAM_MIN_LEN;
    }

    @JsonIgnore
    public P SET_LIKE_QUERY_PARAM_MIN_LEN(int LIKE_QUERY_PARAM_MIN_LEN) {
        this.LIKE_QUERY_PARAM_MIN_LEN = LIKE_QUERY_PARAM_MIN_LEN;
        return (P) this;
    }

    @JsonIgnore
    public boolean GET_LIKE_QUERY_ENABLE() {
        return LIKE_QUERY_ENABLE;
    }

    @JsonIgnore
    public P SET_LIKE_QUERY_ENABLE(boolean LIKE_QUERY_ENABLE) {
        this.LIKE_QUERY_ENABLE = LIKE_QUERY_ENABLE;
        return (P) this;
    }

    /**
     * 获取附加的where sql。
     *
     * @return
     */
    @JsonIgnore
    public String GET_EXT_WHERE_SQL() {
        if (EXT_WHERE_SQL != null) {
            return EXT_WHERE_SQL.toString();
        } else {
            return null;
        }
    }

    /**
     * 清除附加的where sql。
     */
    @JsonIgnore
    public P CLEAR_EXT_WHERE_SQL() {
        this.EXT_WHERE_SQL = null;
        return (P) this;
    }

    /**
     * 增加附加的where sql。
     * 可以多次调用执行。
     * @param ADD_WHERE_SQL
     */
    @JsonIgnore
    public P ADD_EXT_WHERE_SQL(String ADD_WHERE_SQL) {
        if (this.EXT_WHERE_SQL == null) {
            this.EXT_WHERE_SQL = new StringBuilder(512);
        }
        this.EXT_WHERE_SQL.append(ADD_WHERE_SQL);
        return (P) this;
    }

    /**
     * 获取select * from tableName sql.
     *
     * @return
     */
    @JsonIgnore
    public String GET_SELECT_SQL() {
        return SELECT_SQL;
    }

    /**
     * 设置 select * from tableName sql.
     *
     * @param SELECT_SQL
     */
    @JsonIgnore
    public P SET_SELECT_SQL(String SELECT_SQL) {
        this.SELECT_SQL = SELECT_SQL;
        return (P) this;
    }

    /**
     * 设置额外的参数对。
     * 可以多次调用执行。
     * @return
     */
    @JsonIgnore
    public Map<String, Object> GET_EXT_PARAM_MAP() {
        return EXT_PARAM_MAP;
    }

    /**
     * 清除额外的参数对。
     */
    @JsonIgnore
    public P CLEAR_EXT_PARAM_MAP() {
        this.EXT_PARAM_MAP = null;
        return (P) this;
    }

    /**
     * 增加额外的参数。
     *
     * @param paramCond
     * @param paramValue
     */
    @JsonIgnore
    public P ADD_EXT_PARAM(String paramCond, Object paramValue) {
        if (EXT_PARAM_MAP == null) {
            EXT_PARAM_MAP = new LinkedHashMap<>();
        }
        this.EXT_PARAM_MAP.put(paramCond, paramValue);
        return (P) this;

    }
}
