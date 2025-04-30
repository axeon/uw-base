package uw.dao;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
    public static final int SORT_NONE = 0;
    /**
     * 排顺序。
     */
    public static final int SORT_ASC = 1;
    /**
     * 排倒序。
     */
    public static final int SORT_DESC = 2;
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
    private StringBuilder EXT_COND_SQL;
    /**
     * 附加的where条件参数组，使用and连接。
     * key: 带转义的sql expr，比如"and id=?"
     * value: 参数值。
     * 网络禁止从参数传入！
     */
    @JsonIgnore
    private Map<String, Object> EXT_COND_MAP;
    /**
     * 排序名称。
     */
    @JsonProperty("sortName")
    @Schema(name = "$sn", title = "排序名称", description = "排序名称")
    private List<String> SORT_NAME;
    /**
     * 排序类型。
     */
    @JsonProperty("sortType")
    @Schema(name = "$st", title = "排序类型", description = "排序类型。0:不排序, 1:顺序, 2:倒序", defaultValue = "0")
    private List<Integer> SORT_TYPE;

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
    public String EXT_COND_SQL() {
        if (EXT_COND_SQL != null) {
            return EXT_COND_SQL.toString();
        } else {
            return null;
        }
    }

    /**
     * 清除附加的where sql。
     */
    @JsonIgnore
    public P CLEAR_EXT_COND_SQL() {
        this.EXT_COND_SQL = null;
        return (P) this;
    }

    /**
     * 增加附加的where条件sql。
     * 可以多次调用执行。
     *
     * @param ADD_COND_SQL
     */
    @JsonIgnore
    public P ADD_EXT_COND_SQL(String ADD_COND_SQL) {
        if (StringUtils.isBlank(ADD_COND_SQL)) {
            return (P) this;
        }
        if (this.EXT_COND_SQL == null) {
            this.EXT_COND_SQL = new StringBuilder(128);
        }
        if (!this.EXT_COND_SQL.isEmpty()) {
            this.EXT_COND_SQL.append(" and ");
        }
        this.EXT_COND_SQL.append(ADD_COND_SQL);
        return (P) this;
    }

    /**
     * 设置额外的参数对。
     * 可以多次调用执行。
     *
     * @return
     */
    @JsonIgnore
    public Map<String, Object> EXT_COND_MAP() {
        return EXT_COND_MAP;
    }

    /**
     * 清除额外的参数对。
     */
    @JsonIgnore
    public P CLEAR_EXT_COND_MAP() {
        this.EXT_COND_MAP = null;
        return (P) this;
    }

    /**
     * 增加额外的where条件。
     *
     * @param condExpr
     * @param condValue
     */
    @JsonIgnore
    public P ADD_EXT_COND(String condExpr, Object condValue) {
        if (EXT_COND_MAP == null) {
            EXT_COND_MAP = new LinkedHashMap<>();
        }
        this.EXT_COND_MAP.put(condExpr, condValue);
        return (P) this;
    }

    /**
     * 增加额外的where参数。
     * 对于参数，默认使用=?来强制匹配。
     *
     * @param paramExpr
     * @param paramValue
     */
    @JsonIgnore
    public P ADD_EXT_COND_PARAM(String paramExpr, Object paramValue) {
        if (EXT_COND_MAP == null) {
            EXT_COND_MAP = new LinkedHashMap<>();
        }
        this.EXT_COND_MAP.put(paramExpr + "=?", paramValue);
        return (P) this;
    }

    /**
     * 排序名称。
     *
     * @return
     */
    public List<String> SORT_NAME() {
        return SORT_NAME;
    }

    /**
     * 设置排序名称。
     *
     * @param SORT_NAME
     * @return
     */
    public P SORT_NAME(String... SORT_NAME) {
        this.SORT_NAME = new ArrayList<>(List.of(SORT_NAME));
        return (P) this;
    }

    /**
     * 设置排序名称。
     *
     * @param sortName
     */
    public void set$sn(String... sortName) {
        this.SORT_NAME = new ArrayList<>(List.of(sortName));
    }

    /**
     * 排序类型。
     *
     * @return
     */
    public List<Integer> SORT_TYPE() {
        return SORT_TYPE;
    }

    /**
     * 设置排序类型。
     *
     * @param SORT_TYPE
     * @return
     */
    public P SORT_TYPE(Integer... SORT_TYPE) {
        this.SORT_TYPE = new ArrayList<>(List.of(SORT_TYPE));
        return (P) this;
    }

    /**
     * 设置排序类型。
     *
     * @param sortType
     */
    public void set$st(Integer... sortType) {
        this.SORT_TYPE = new ArrayList<>(List.of(sortType));
    }

    /**
     * 增加排序。
     *
     * @param sortName
     * @param sortType
     */
    public P ADD_SORT(String sortName, int sortType) {
        if (this.SORT_NAME == null) {
            this.SORT_NAME = new ArrayList<>(3);
            this.SORT_TYPE = new ArrayList<>(3);
        }
        this.SORT_NAME.add(sortName);
        this.SORT_TYPE.add(sortType);
        return (P) this;
    }

    /**
     * 生成sort的sql。
     *
     * @return
     */
    public String GEN_SORT_SQL() {
        if (SORT_NAME != null && SORT_NAME.size() > 0) {
            // 是否有排序字段。
            boolean hasSort = false;
            StringBuilder sb = new StringBuilder(32);
            sb.append(" order by");
            for (int i = 0; i < SORT_NAME.size(); i++) {
                String sortColumns = ALLOWED_SORT_PROPERTY().get(SORT_NAME.get(i));
                if (StringUtils.isBlank(sortColumns)) {
                    continue;
                }
                hasSort = true;
                sb.append(" ").append(sortColumns);
                if (SORT_TYPE != null && i < SORT_TYPE.size()) {
                    if (SORT_TYPE.get(i) != null && SORT_TYPE.get(i) == SORT_DESC) {
                        sb.append(" desc");
                    } else {
                        sb.append(" asc");
                    }
                }
                sb.append(",");
            }
            if (hasSort) {
                //去掉最后一个逗号
                sb.deleteCharAt(sb.length() - 1);
                return sb.toString();
            }
        }
        return StringUtils.EMPTY;
    }

}
