package uw.common.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 分页行集数据容器。存储行列结构的二维数据，支持游标遍历和按列名访问。
 * 不依赖java.sql，由DAO层负责ResultSet到PageRowSet的转换。
 *
 * @author zhangjin
 */
@Schema(title = "分页行集", description = "分页行集数据容器")
public class PageRowSet implements Serializable {

    /**
     * 空的PageRowSet.
     */
    public static final PageRowSet EMPTY = new PageRowSet(new String[0], null, 0, 0, 0);

    /**
     * 当前索引位置.
     */
    @JsonIgnore
    private transient int currentIndex = -1;

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
     * 列名数组.
     */
    @JsonProperty
    @Schema(title = "列名数组", description = "列名数组")
    private String[] columnNames;

    /**
     * 数据存放数组.
     */
    @JsonProperty
    @Schema(title = "数据集", description = "数据集")
    private ArrayList<Object[]> list;

    // ===== 构造函数（不再接受ResultSet，由DAO层负责转换） =====

    /**
     * 构造函数.
     */
    public PageRowSet() {
    }

    /**
     * 构造器.
     *
     * @param columnNames 列名数组
     * @param list        数据列表
     * @param startIndex  开始位置
     * @param resultNum   每页大小
     * @param sizeAll     总数据量
     */
    public PageRowSet(String[] columnNames, List<Object[]> list, int startIndex, int resultNum, int sizeAll) {
        this.columnNames = columnNames;
        this.list = toArrayList(list);
        this.startIndex = startIndex;
        this.resultNum = resultNum;
        this.size = this.list.size();
        calcPages(sizeAll);
    }

    // ===== 静态工厂 =====

    /**
     * 获取空的PageRowSet.
     *
     * @return 空的PageRowSet
     */
    @JsonIgnore
    public static PageRowSet empty() {
        return EMPTY;
    }

    // ===== 列名访问 =====

    /**
     * 获取列名数组.
     *
     * @return 列名数组
     */
    @JsonIgnore
    public String[] getColumnNames() {
        return columnNames;
    }

    // ===== 分页计算 =====

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

    // ===== 游标遍历 =====

    /**
     * 到下一条记录，检查是否还有下一行数据.
     *
     * @return 是否有下一行
     */
    @JsonIgnore
    public boolean next() {
        if (list == null) {
            return false;
        }
        boolean flag = list.size() > currentIndex + 1;
        if (flag) {
            currentIndex++;
        }
        return flag;
    }

    /**
     * 到上一条记录，检查是否还有上一行数据.
     *
     * @return 是否有上一行
     */
    @JsonIgnore
    public boolean previous() {
        boolean flag = currentIndex > -1;
        if (flag) {
            currentIndex--;
        }
        return flag;
    }

    /**
     * remove当前行.
     */
    @JsonIgnore
    public void remove() {
        if (list == null) {
            return;
        }
        this.list.remove(currentIndex);
        this.size--;
        this.sizeAll--;
        currentIndex--;
    }

    /**
     * 定位到指定的位置.
     *
     * @param index 位置
     */
    @JsonIgnore
    public void absolute(int index) {
        this.currentIndex = index - 1;
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
     * 判断是否为空.
     *
     * @return 是否为空
     */
    @JsonIgnore
    public boolean isEmpty() {
        return this.size == 0;
    }

    /**
     * 判断是否非空.
     *
     * @return 是否非空
     */
    @JsonIgnore
    public boolean isNotEmpty() {
        return this.size > 0;
    }

    // ===== 数据获取 =====

    /**
     * 返回数据列表.
     *
     * @return 数据列表
     */
    @JsonIgnore
    public List<Object[]> list() {
        if (this.list == null) {
            return Collections.emptyList();
        }
        return this.list;
    }

    // ===== 按列名获取数据 =====

    /**
     * 获取当前行指定列名的数据.
     *
     * @param colName 列名
     * @return 数据值
     */
    @JsonIgnore
    public Object get(String colName) {
        if (list == null || currentIndex < 0 || currentIndex >= list.size()) {
            return null;
        }
        return list.get(currentIndex)[getColumnPos(colName)];
    }


    @JsonIgnore
    public boolean getBoolean(String colName) {
        return getBoolean(getColumnPos(colName));
    }

    @JsonIgnore
    public int getInt(String colName) {
        return getInt(getColumnPos(colName));
    }

    @JsonIgnore
    public long getLong(String colName) {
        return getLong(getColumnPos(colName));
    }

    @JsonIgnore
    public double getDouble(String colName) {
        return getDouble(getColumnPos(colName));
    }

    @JsonIgnore
    public float getFloat(String colName) {
        return getFloat(getColumnPos(colName));
    }

    @JsonIgnore
    public String getString(String colName) {
        return getString(getColumnPos(colName));
    }

    @JsonIgnore
    public BigInteger getBigInteger(String colName) {
        return getBigInteger(getColumnPos(colName));
    }

    @JsonIgnore
    public BigDecimal getBigDecimal(String colName) {
        return getBigDecimal(getColumnPos(colName));
    }

    @JsonIgnore
    public byte[] getBytes(String colName) {
        return getBytes(getColumnPos(colName));
    }

    @JsonIgnore
    public java.util.Date getDate(String colName) {
        return getDate(getColumnPos(colName));
    }

    // ===== 按列索引获取数据 =====

    /**
     * 获取当前行指定列索引的数据.
     *
     * @param colIndex 列索引
     * @return 数据值
     */
    @JsonIgnore
    public Object get(int colIndex) {
        if (list == null) {
            return null;
        }
        return list.get(currentIndex)[colIndex];
    }

    @JsonIgnore
    public boolean getBoolean(int colIndex) {
        Object data = get(colIndex);
        if (data == null) {
            return false;
        }
        if (data instanceof Boolean bool) {
            return bool;
        } else {
            return Boolean.parseBoolean(String.valueOf(data));
        }
    }

    @JsonIgnore
    public int getInt(int colIndex) {
        Object data = get(colIndex);
        if (data == null) {
            return 0;
        }
        if (data instanceof Integer num) {
            return num;
        } else {
            return Integer.parseInt(String.valueOf(data));
        }
    }

    @JsonIgnore
    public long getLong(int colIndex) {
        Object data = get(colIndex);
        if (data == null) {
            return 0;
        }
        if (data instanceof Long num) {
            return num;
        } else {
            return Long.parseLong(String.valueOf(data));
        }
    }

    @JsonIgnore
    public double getDouble(int colIndex) {
        Object data = get(colIndex);
        if (data == null) {
            return 0d;
        }
        if (data instanceof Double num) {
            return num;
        } else if (data instanceof Float num) {
            return num;
        } else {
            return Double.parseDouble(String.valueOf(data));
        }
    }

    @JsonIgnore
    public float getFloat(int colIndex) {
        Object data = get(colIndex);
        if (data == null) {
            return 0f;
        }
        if (data instanceof Float num) {
            return num;
        } else {
            return Float.parseFloat(String.valueOf(data));
        }
    }

    @JsonIgnore
    public String getString(int colIndex) {
        Object data = get(colIndex);
        if (data == null) {
            return StringUtils.EMPTY;
        }
        if (data instanceof String s) {
            return s;
        } else {
            return String.valueOf(data);
        }
    }

    @JsonIgnore
    public BigInteger getBigInteger(int colIndex) {
        Object data = get(colIndex);
        if (data == null) {
            return BigInteger.ZERO;
        }
        if (data instanceof BigInteger num) {
            return num;
        } else {
            return new BigInteger(String.valueOf(data));
        }
    }

    @JsonIgnore
    public BigDecimal getBigDecimal(int colIndex) {
        Object data = get(colIndex);
        if (data == null) {
            return BigDecimal.ZERO;
        }
        if (data instanceof BigDecimal num) {
            return num;
        } else {
            return new BigDecimal(String.valueOf(data));
        }
    }

    @JsonIgnore
    public byte[] getBytes(int colIndex) {
        Object data = get(colIndex);
        if (data == null) {
            return null;
        }
        return (byte[]) data;
    }

    @JsonIgnore
    public java.util.Date getDate(int colIndex) {
        Object data = get(colIndex);
        if (data == null) {
            return null;
        }
        return (java.util.Date) data;
    }

    // ===== 列名位置查找 =====

    /**
     * 获取列名位置.
     *
     * @param colName 列名
     * @return 列名位置
     */
    @JsonIgnore
    public int getColumnPos(String colName) {
        int index = -1;
        if (columnNames != null) {
            for (int i = 0; i < columnNames.length; i++) {
                if (colName.equalsIgnoreCase(columnNames[i])) {
                    index = i;
                    break;
                }
            }
        }
        return index;
    }

    // ===== 类型转换 =====

    /**
     * map类型转换，将PageRowSet转换为PageList.
     *
     * @param function 转换函数
     * @param <R>      目标类型
     * @return PageList
     */
    @JsonIgnore
    public <R> PageList<R> map(Function<PageRowSet, R> function) {
        ArrayList<R> resultList = new ArrayList<>();
        while (next()) {
            resultList.add(function.apply(this));
        }
        return new PageList<>(resultList, startIndex, resultNum, sizeAll);
    }

    /**
     * 将List转换为ArrayList.
     *
     * @param list 源List
     * @return ArrayList
     */
    private static ArrayList<Object[]> toArrayList(List<Object[]> list) {
        if (list == null) {
            return new ArrayList<>();
        }
        if (list instanceof ArrayList<Object[]> al) {
            return al;
        }
        return new ArrayList<>(list);
    }


}
