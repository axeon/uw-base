package uw.dao;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;

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
     * 不排序。
     */
    public static int SORT_NONE = 0;
    /**
     * 排顺序。
     */
    public static int SORT_ASC = 1;
    /**
     * 排倒序。
     */
    public static int SORT_DESC = 2;
    /**
     * like参数的最小长度。
     * 少于最小参数的，将转化为=查询。
     */
    @JsonIgnore
    private int LIKE_QUERY_PARAM_MIN_LEN = 3;
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
    /**
     * 排序名称。
     */
    @JsonProperty("sortName")
    @Schema(name = "$sn", title = "排序名称", description = "排序名称")
    private String SORT_NAME;
    /**
     * 排序类型。
     */
    @JsonProperty("sortType")
    @Schema(name = "$st", title = "排序类型", description = "排序类型。0:不排序, 1:顺序, 2:倒序", defaultValue = "0")
    private int SORT_TYPE = SORT_NONE;

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

    /**
     * 允许的排序属性。
     * key:排序名 value:排序字段
     *
     * @return
     */
    public Map<String, String> ALLOWED_SORT_PROPERTY() {
        return null;
    }

    /**
     * 获取like参数的最小长度。
     *
     * @return
     */
    @JsonIgnore
    public int LIKE_QUERY_PARAM_MIN_LEN() {
        return LIKE_QUERY_PARAM_MIN_LEN;
    }

    /**
     * 设置like参数的最小长度。
     *
     * @param LIKE_QUERY_PARAM_MIN_LEN
     * @return
     */
    @JsonIgnore
    public P LIKE_QUERY_PARAM_MIN_LEN(int LIKE_QUERY_PARAM_MIN_LEN) {
        this.LIKE_QUERY_PARAM_MIN_LEN = LIKE_QUERY_PARAM_MIN_LEN;
        return (P) this;
    }

    /**
     * 是否开启like查询。
     *
     * @return
     */
    @JsonIgnore
    public boolean LIKE_QUERY_ENABLE() {
        return LIKE_QUERY_ENABLE;
    }

    /**
     * 设置是否开启like查询。
     *
     * @param LIKE_QUERY_ENABLE
     * @return
     */
    @JsonIgnore
    public P LIKE_QUERY_ENABLE(boolean LIKE_QUERY_ENABLE) {
        this.LIKE_QUERY_ENABLE = LIKE_QUERY_ENABLE;
        return (P) this;
    }

    /**
     * 获取select * from tableName sql.
     *
     * @return
     */
    @JsonIgnore
    public String SELECT_SQL() {
        return SELECT_SQL;
    }

    /**
     * 设置 select * from tableName sql.
     *
     * @param SELECT_SQL
     */
    @JsonIgnore
    public P SELECT_SQL(String SELECT_SQL) {
        this.SELECT_SQL = SELECT_SQL;
        return (P) this;
    }

    /**
     * 获取附加的where sql。
     *
     * @return
     */
    @JsonIgnore
    public String EXT_WHERE_SQL() {
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
     *
     * @param ADD_WHERE_SQL
     */
    @JsonIgnore
    public P ADD_EXT_WHERE_SQL(String ADD_WHERE_SQL) {
        if (this.EXT_WHERE_SQL == null) {
            this.EXT_WHERE_SQL = new StringBuilder( 512 );
        }
        this.EXT_WHERE_SQL.append( ADD_WHERE_SQL );
        return (P) this;
    }


    /**
     * 设置额外的参数对。
     * 可以多次调用执行。
     *
     * @return
     */
    @JsonIgnore
    public Map<String, Object> EXT_PARAM_MAP() {
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
        this.EXT_PARAM_MAP.put( paramCond, paramValue );
        return (P) this;
    }

    /**
     * 生成sort的sql。
     *
     * @return
     */
    public String GEN_SORT_SQL() {
        Map<String, String> allowedSortProperty = ALLOWED_SORT_PROPERTY();
        if (allowedSortProperty == null || StringUtils.isBlank( SORT_NAME )) {
            return StringUtils.EMPTY;
        }
        String sortColumns = allowedSortProperty.get( SORT_NAME );
        if (StringUtils.isBlank( sortColumns )) {
            return StringUtils.EMPTY;
        }
        //此处要对SORT_NAME处理，此处是唯一潜在注入点。
        return " order by " + sortColumns + " " + (SORT_TYPE == SORT_NONE ? StringUtils.EMPTY : SORT_TYPE == SORT_ASC ? "asc" : "desc");
    }

    /**
     * 排序名称。
     *
     * @return
     */
    public String SORT_NAME() {
        return SORT_NAME;
    }

    /**
     * 设置排序名称。
     *
     * @param SORT_NAME
     * @return
     */
    public QueryParam SORT_NAME(String SORT_NAME) {
        this.SORT_NAME = SORT_NAME;
        return this;
    }

    /**
     * 设置排序名称。
     *
     * @param sortName
     */
    public void set$sn(String sortName) {
        this.SORT_NAME = sortName;
    }

    /**
     * 排序类型。
     *
     * @return
     */
    public int SORT_TYPE() {
        return SORT_TYPE;
    }

    /**
     * 设置排序类型。
     *
     * @param SORT_TYPE
     * @return
     */
    public QueryParam SORT_TYPE(int SORT_TYPE) {
        this.SORT_TYPE = SORT_TYPE;
        return this;
    }

    /**
     * 设置排序类型。
     *
     * @param sortType
     */
    public void set$st(int sortType) {
        this.SORT_TYPE = sortType;
    }


}
