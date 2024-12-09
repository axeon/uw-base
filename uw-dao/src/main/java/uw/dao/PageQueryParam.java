package uw.dao;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;


/**
 * 分页查询参数。
 *
 * @author axeon
 */
@Schema(title = "分页查询参数", description = "分页查询参数")
public class PageQueryParam extends QueryParam<PageQueryParam> {

    /**
     * 仅计算分页，不返回数据。
     */
    public static int REQUEST_COUNT = 0;

    /**
     * 仅返回数据，不计算分页。
     */
    public static int REQUEST_DATA = 1;

    /**
     * 同时返回数据和统计分页。
     */
    public static int REQUEST_ALL = 2;

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
     * 当前页码。
     */
    @JsonProperty("page")
    @Schema(name = "$pg", title = "当前页码", description = "当前页码", defaultValue = "1")
    private int PAGE = 1;

    /**
     * 每页条数。
     */
    @JsonProperty("resultNum")
    @Schema(name = "$rn", title = "每页条数", description = "每页条数", defaultValue = "10")
    private int RESULT_NUM = 10;

    /**
     * 起始位置，此数值和分页数2选一。
     */
    @JsonProperty("startIndex")
    @Schema(name = "$si", title = "起始位置", description = "起始位置", defaultValue = "0")
    private int START_INDEX = 0;

    /**
     * 请求类型。
     */
    @JsonProperty("requestType")
    @Schema(name = "$rt", title = "请求类型", description = "请求类型。0:仅分页信息, 1:仅数据, 2:全部", defaultValue = "2")
    private int REQUEST_TYPE = REQUEST_DATA;

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

    public PageQueryParam() {

    }

    /**
     * 是否自动count分页。
     *
     * @return
     */
    public boolean CHECK_AUTO_COUNT() {
        return REQUEST_TYPE == 2;
    }

    /**
     * 仅做count分页。
     *
     * @return
     */
    public boolean CHECK_ONLY_COUNT() {
        return REQUEST_TYPE == 0;
    }


    /**
     * 生成sort的sql，直接使用sortName生成sql。
     *
     * @return
     */
    public String GEN_SORT_SQL() {
        if (SORT_TYPE == SORT_NONE || StringUtils.isBlank( SORT_NAME )) {
            return StringUtils.EMPTY;
        }
        return " order by " + this.SORT_NAME + " " + (SORT_TYPE == SORT_ASC ? "asc" : "desc");
    }

    /**
     * 当前页码。
     * @return
     */
    public int PAGE() {
        if (PAGE < 1) {
            PAGE = 1;
        }
        return PAGE;
    }

    /**
     * 设置当前页码。
     * @param PAGE
     * @return
     */
    public PageQueryParam PAGE(int PAGE) {
        if (PAGE < 1) {
            PAGE = 1;
        }
        this.PAGE = PAGE;
        return this;

    }

    /**
     * 设置当前页码。
     * @param page
     */
    public void set$pg(int page) {
        PAGE( page );
    }

    /**
     * 每页条数。
     * @return
     */
    public int RESULT_NUM() {
        if (RESULT_NUM > 10000) {
            RESULT_NUM = 10000;
        } else if (RESULT_NUM < 1) {
            RESULT_NUM = 1;
        }
        return RESULT_NUM;
    }

    /**
     * 设置每页条数。
     * @param RESULT_NUM
     * @return
     */
    public PageQueryParam RESULT_NUM(int RESULT_NUM) {
        if (RESULT_NUM > 10000) {
            RESULT_NUM = 10000;
        } else if (RESULT_NUM < 1) {
            RESULT_NUM = 1;
        }
        this.RESULT_NUM = RESULT_NUM;
        return this;
    }

    /**
     * 设置每页条数。
     * @param resultNum
     */
    public void set$rn(int resultNum) {
        RESULT_NUM( resultNum );
    }

    /**
     * 起始位置，此数值和分页数2选一。
     * @return
     */
    public int START_INDEX() {
        if (START_INDEX < 0) {
            START_INDEX = 0;
        }
        //如果没有设置startIndex，并且有设置page值，则根据page计算startIndex。
        if (this.PAGE > 1 && START_INDEX == 0) {
            return (this.PAGE - 1) * this.RESULT_NUM;
        }
        return START_INDEX;
    }

    /**
     * 设置起始位置，此数值和分页数2选一。
     * @param START_INDEX
     * @return
     */
    public PageQueryParam START_INDEX(int START_INDEX) {
        if (START_INDEX < 0) {
            START_INDEX = 0;
        }
        //如果没有设置startIndex，并且有设置page值，则根据page计算startIndex。
        if (this.PAGE > 1 && START_INDEX == 0) {
            START_INDEX = (this.PAGE - 1) * this.RESULT_NUM;
        }
        this.START_INDEX = START_INDEX;
        return this;
    }

    /**
     * 设置起始位置，此数值和分页数2选一。
     * @param startIndex
     */
    public void set$si(int startIndex) {
        START_INDEX( startIndex );
    }

    /**
     * 请求类型。
     * @return
     */
    public int REQUEST_TYPE() {
        return REQUEST_TYPE;
    }

    /**
     * 设置请求类型。
     * @param REQUEST_TYPE
     * @return
     */
    public PageQueryParam REQUEST_TYPE(int REQUEST_TYPE) {
        this.REQUEST_TYPE = REQUEST_TYPE;
        return this;

    }

    /**
     * 设置请求类型。
     * @param requestType
     */
    public void set$rt(int requestType) {
        this.REQUEST_TYPE = requestType;
    }

    /**
     * 排序名称。
     * @return
     */
    public String SORT_NAME() {
        return SORT_NAME;
    }

    /**
     * 设置排序名称。
     * @param SORT_NAME
     * @return
     */
    public PageQueryParam SORT_NAME(String SORT_NAME) {
        this.SORT_NAME = SORT_NAME;
        return this;

    }

    /**
     * 设置排序名称。
     * @param sortName
     */
    public void set$sn(String sortName) {
        this.SORT_NAME = sortName;
    }

    /**
     * 排序类型。
     * @return
     */
    public int SORT_TYPE() {
        return SORT_TYPE;
    }

    /**
     * 设置排序类型。
     * @param SORT_TYPE
     * @return
     */
    public PageQueryParam SORT_TYPE(int SORT_TYPE) {
        this.SORT_TYPE = SORT_TYPE;
        return this;

    }

    /**
     * 设置排序类型。
     * @param sortType
     */
    public void set$st(int sortType) {
        this.SORT_TYPE = sortType;
    }
}
