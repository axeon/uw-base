package uw.log.es.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 组合了互联网应用常见列表所需数据的集合接口。 实现了iterator,Iterable.
 *
 * @param <T> 映射的类型
 * @author axeon
 */
@Schema(title = "DataList数据集", description = "组合了互联网应用常见列表所需数据的集合接口")
public class ESDataList<T> implements Iterable<T>, Serializable {

    /**
     * 开始的索引.
     */
    @JsonProperty
    @Schema(title = "请求的起始索引", description = "请求的起始索引")
    private int startIndex = 0;

    /**
     * 返回的结果集大小.
     */
    @JsonProperty
    @Schema(title = "请求的结果集大小", description = "请求的结果集大小")
    private int resultNum = 0;

    /**
     * List大小（实际返回的结果集大小）.
     */
    @JsonProperty
    @Schema(title = "结果集大小", description = "结果集大小")
    private int size = 0;

    /**
     * 整个表数据量大小.
     */
    @JsonProperty
    @Schema(title = "总数据集大小", description = "总数据集大小")
    private int sizeAll = 0;

    /**
     * 当前页.
     */
    @JsonProperty
    @Schema(title = "当前页", description = "当前页")
    private int page = 0;

    /**
     * 总页数.
     */
    @JsonProperty
    @Schema(title = "总页数", description = "总页数")
    private int pageCount = 0;

    /**
     * 返回的value object数组.
     *
     * @param <T>
     * 映射的类型
     */
    @JsonProperty
    @Schema(title = "结果集", description = "结果集")
    private ArrayList<T> results = null;


    /**
     * 构造函数.
     */
    public ESDataList() {
        super();
    }

    /**
     * DataList构造器.
     *
     * @param results    结果集
     * @param startIndex 开始位置
     * @param resultNum  每页大小
     * @param sizeAll    所有的数量
     */
    public ESDataList(ArrayList<T> results, int startIndex, int resultNum, int sizeAll) {
        this.startIndex = startIndex;
        this.results = results;
        this.resultNum = resultNum;
        this.size = this.results.size();
        this.sizeAll = sizeAll;

        if (this.results != null) {
            this.size = this.results.size();
        }
        if (this.sizeAll > 0 && this.resultNum > 0) {
            // 计算当前页
            this.page = (int) Math.ceil((double) startIndex / (double) resultNum);
            // 计算总页数
            this.pageCount = (int) Math.ceil((double) this.sizeAll / (double) resultNum);
        }
    }

    /**
     * 计算页面参数信息。
     */
    public void calcPages(int sizeAll) {
        this.sizeAll = sizeAll;
        if (this.sizeAll > 0 && this.resultNum > 0) {
            // 计算当前页
            this.page = (int) Math.ceil((double) startIndex / (double) resultNum);
            // 计算总页数
            this.pageCount = (int) Math.ceil((double) sizeAll / (double) resultNum);
        }
    }

    /**
     * 获得指定处的对象.
     *
     * @param index 位置
     * @return value object数组指定的对象
     */
    public T get(int index) {
        if (results != null) {
            return results.get(index);
        } else {
            return null;
        }
    }

    /**
     * 获取当前List大小.
     *
     * @return 当前List大小
     */
    public int size() {
        return this.size;
    }

    /**
     * 获取该表/视图所有的数据大小.
     *
     * @return 该表/视图所有的数据大小
     */
    public int sizeAll() {
        return this.sizeAll;
    }

    /**
     * 按照总记录数和每页条数计算出页数.
     *
     * @return 页数
     */
    public int pageCount() {
        return this.pageCount;
    }

    /**
     * 当前页.
     *
     * @return 页数
     */
    public int page() {
        return this.page;
    }


    /**
     * 在整个数据集中的开始索引位置.
     *
     * @return 开始位置
     */
    public int startIndex() {
        return this.startIndex;
    }

    /**
     * 返回结果集大小.
     *
     * @return int
     */
    public int resultNum() {
        return this.resultNum;
    }

    /**
     * 返回该结果集.
     *
     * @return 结果集
     */
    public ArrayList<T> results() {
        return this.results;
    }

    /**
     * 重新设定结果集合.
     *
     * @param objects objects集合
     */
    public void reset(ArrayList<T> objects) {
        this.results = objects;
        if (this.size != objects.size()) {
            this.size = objects.size();
        }
    }

    /**
     * 获得iterator列表.
     *
     * @return Iterator列表
     */
    @Override
    public Iterator<T> iterator() {
        return this.results.iterator();
    }


}
