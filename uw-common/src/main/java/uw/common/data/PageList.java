package uw.common.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * 分页列表数据容器。组合了分页信息与泛型列表数据，实现Iterable接口。
 *
 * @param <T> 数据类型
 * @author axeon
 */
@Schema(title = "分页列表", description = "分页列表数据容器")
public class PageList<T> implements Iterable<T>, Serializable {

    /**
     * 空的PageList.
     */
    @SuppressWarnings("unchecked")
    public static final PageList<?> EMPTY = new PageList<>(java.util.Collections.emptyList(), 0, 0, 0);

    /**
     * 开始的索引.
     */
    @JsonProperty
    @Schema(title = "起始索引", description = "请求的起始索引")
    private int startIndex = 0;

    /**
     * 返回的结果集大小.
     */
    @JsonProperty
    @Schema(title = "每页大小", description = "请求的结果集大小")
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
    @Schema(title = "总数据量", description = "总数据量")
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
     * 数据列表.
     */
    @JsonProperty
    @Schema(title = "数据列表", description = "数据列表")
    private ArrayList<T> list;

    /**
     * 构造函数.
     */
    public PageList() {
        super();
    }

    /**
     * PageList构造器.
     *
     * @param list       数据列表
     * @param startIndex 开始位置
     * @param resultNum  每页大小
     * @param sizeAll    总数据量
     */
    public PageList(List<T> list, int startIndex, int resultNum, int sizeAll) {
        this.list = toArrayList(list);
        this.startIndex = startIndex;
        this.resultNum = resultNum;
        this.size = this.list.size();
        calcPages(sizeAll);
    }

    /**
     * 获取空的PageList.
     *
     * @param <T> 数据类型
     * @return 空的PageList
     */
    @JsonIgnore
    public static <T> PageList<T> empty() {
        return (PageList<T>) EMPTY;
    }

    /**
     * 计算页面参数信息。
     *
     * @param sizeAll 总数据量
     */
    @JsonIgnore
    public void calcPages(int sizeAll) {
        this.sizeAll = sizeAll;
        if (this.sizeAll > 0 && this.resultNum > 0) {
            this.page = (int) Math.ceil((float) startIndex / (float) resultNum);
            this.pageCount = (int) Math.ceil((float) sizeAll / (float) resultNum);
        }
    }

    // ===== 列表访问 =====

    /**
     * 获取指定位置的对象.
     *
     * @param index 位置
     * @return 指定位置的对象，如果list为null则返回null
     */
    @JsonIgnore
    public T get(int index) {
        if (list == null) {
            return null;
        }
        if (index < 0 || index >= this.size) {
            return null;
        }
        return list.get(index);
    }

    /**
     * 获取第一个元素.
     *
     * @return 第一个元素
     */
    @JsonIgnore
    public T getFirst() {
        return get(0);
    }

    /**
     * 获取最后一个元素.
     *
     * @return 最后一个元素
     */
    @JsonIgnore
    public T getLast() {
        return get(this.size - 1);
    }

    // ===== 大小信息 =====

    /**
     * 获取当前List大小.
     *
     * @return 当前List大小
     */
    @JsonIgnore
    public int size() {
        return this.size;
    }

    /**
     * 获取总数据量.
     *
     * @return 总数据量
     */
    @JsonIgnore
    public int sizeAll() {
        return this.sizeAll;
    }

    /**
     * 获取总页数.
     *
     * @return 总页数
     */
    @JsonIgnore
    public int pageCount() {
        return this.pageCount;
    }

    /**
     * 获取当前页.
     *
     * @return 当前页
     */
    @JsonIgnore
    public int page() {
        return this.page;
    }

    /**
     * 获取起始索引.
     *
     * @return 起始索引
     */
    @JsonIgnore
    public int startIndex() {
        return this.startIndex;
    }

    /**
     * 获取每页大小.
     *
     * @return 每页大小
     */
    @JsonIgnore
    public int resultNum() {
        return this.resultNum;
    }

    // ===== 空判断 =====

    /**
     * 判断列表是否为空.
     *
     * @return 是否为空
     */
    @JsonIgnore
    public boolean isEmpty() {
        return this.size == 0;
    }

    /**
     * 判断列表是否非空.
     *
     * @return 是否非空
     */
    @JsonIgnore
    public boolean isNotEmpty() {
        return this.size > 0;
    }

    // ===== 查询 =====

    /**
     * 判断是否包含指定元素.
     *
     * @param o 元素
     * @return 是否包含
     */
    @JsonIgnore
    public boolean contains(Object o) {
        if (this.list == null) {
            return false;
        }
        return this.list.contains(o);
    }

    /**
     * 查找指定元素的索引.
     *
     * @param o 元素
     * @return 索引，如果不存在返回-1
     */
    @JsonIgnore
    public int indexOf(Object o) {
        if (this.list == null) {
            return -1;
        }
        return this.list.indexOf(o);
    }

    // ===== 数据获取 =====

    /**
     * 返回数据列表.
     *
     * @return 数据列表
     */
    @JsonIgnore
    public List<T> list() {
        if (this.list == null) {
            return java.util.Collections.emptyList();
        }
        return this.list;
    }

    /**
     * 重新设定数据列表.
     *
     * @param list 数据列表
     */
    @JsonIgnore
    public void reset(ArrayList<T> list) {
        this.list = toArrayList(list);
        this.size = this.list.size();
    }

    // ===== Iterable/Stream =====

    /**
     * 获取iterator.
     *
     * @return Iterator
     */
    @Override
    @JsonIgnore
    public Iterator<T> iterator() {
        if (this.list == null) {
            return java.util.Collections.emptyIterator();
        }
        return this.list.iterator();
    }

    /**
     * 获取stream.
     *
     * @return Stream
     */
    @JsonIgnore
    public Stream<T> stream() {
        if (this.list == null) {
            return Stream.empty();
        }
        return this.list.stream();
    }


    /**
     * 将list转为ArrayList.
     *
     * @param list list
     * @param <T>  数据类型
     * @return ArrayList
     */
    private static <T> ArrayList<T> toArrayList(List<T> list) {
        if (list == null) {
            return new ArrayList<>();
        }
        if (list instanceof ArrayList<T> al) {
            return al;
        }
        return new ArrayList<>(list);
    }

}