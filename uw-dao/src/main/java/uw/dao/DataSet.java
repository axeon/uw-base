package uw.dao;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.function.Function;

/**
 * 存储并转化ResultSet对象数据.
 *
 * @author zhangjin
 */
@Schema(title = "DataSet结果集", description = "DataSet结果集")
public class DataSet implements Serializable {

    /**
     * 空数据集.
     */
    public static final DataSet EMPTY = new DataSet();

    /**
     * 当前索引位置.
     */
    @JsonIgnore
    private transient int currentIndex = -1;

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
    @Schema(title = "总数据大小", description = "总数据大小")
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
    private String[] cols;

    /**
     * 数据存放数组.
     */
    @JsonProperty
    @Schema(title = "数据集", description = "数据集")
    private ArrayList<Object[]> results;

    /**
     * 构造函数.
     */
    public DataSet() {
        super();
    }

    /**
     * 构造器.
     *
     * @param rs         结果集
     * @param startIndex 开始位置
     * @param resultNum  结果集大小
     * @param sizeAll    整个表数据量大小
     * @throws SQLException SQL异常
     */
    public DataSet(ResultSet rs, int startIndex, int resultNum, int sizeAll) throws SQLException {
        // 设置参数
        this.startIndex = startIndex;
        this.resultNum = resultNum;
        this.sizeAll = sizeAll;

        if (this.sizeAll > 0 && this.resultNum > 0) {
            // 计算当前页
            this.page = (int) Math.ceil((double) startIndex / (double) resultNum);
            // 计算总页数
            this.pageCount = (int) Math.ceil((double) sizeAll / (double) resultNum);
        }
        // 获取字段列表
        ResultSetMetaData rsm = rs.getMetaData();
        int colsCount = rsm.getColumnCount();
        cols = new String[colsCount];
        int[] colTypes = new int[colsCount];
        for (int i = 0; i < colsCount; i++) {
            cols[i] = rsm.getColumnLabel(i + 1).toLowerCase();
            colTypes[i] = rsm.getColumnType(i + 1);
        }
        // 开始赋值
        if (resultNum > 0) {
            this.results = new ArrayList<>(resultNum);
        } else {
            this.results = new ArrayList<>();
        }
        while (rs.next()) {
            this.size++;
            Object[] result = new Object[cols.length];
            for (int x = 0; x < cols.length; x++) {
                // 将对应列名的值放入二维数组中
                result[x] = rs.getObject(x + 1);
            }
            results.add(result);
        }
    }

    /**
     * 获取列名列表.
     *
     * @return 列名列表
     */
    public String[] getColumnNames() {
        return cols;
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
     * 到下一条记录，检查是否还有下一行数据.
     *
     * @return boolean
     */
    public boolean next() {
        currentIndex++;
        return results.size() > currentIndex;
    }

    /**
     * 到上一条记录，检查是否还有上一行数据.
     *
     * @return boolean
     */
    public boolean previous() {
        currentIndex--;
        return currentIndex > -1;
    }

    /**
     * remove当前行.
     */
    public void remove() {
        this.results.remove(currentIndex);
        this.size--;
        this.sizeAll--;
    }

    /**
     * 返回结果集数组.
     *
     * @return 结果集数组
     */
    public ArrayList<Object[]> results() {
        return results;
    }

    /**
     * 定位到指定的位置.
     *
     * @param index 位置
     */
    public void absolute(int index) {
        this.currentIndex = index - 1;
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
     * 事务异常
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
     * @return 结果集大小
     */
    public int resultNum() {
        return this.resultNum;
    }

    /**
     * 获取数组中指定位置的数据.
     *
     * @param colName 列名
     * @return 数组中指定位置的数据
     */
    public Object get(String colName) {
        return results.get(currentIndex)[getColumnPos(colName)];
    }

    /**
     * 返回值为boolean.
     *
     * @param colName 列名
     * @return int
     */
    public boolean getBoolean(String colName) {
        return getBoolean(getColumnPos(colName));
    }

    /**
     * 返回值为int.
     *
     * @param colName 列名
     * @return int
     */
    public int getInt(String colName) {
        return getInt(getColumnPos(colName));
    }

    /**
     * 返回值为long.
     *
     * @param colName 列名
     * @return long
     */
    public long getLong(String colName) {
        return getLong(getColumnPos(colName));
    }

    /**
     * 返回值为double.
     *
     * @param colName 列名
     * @return double
     */
    public double getDouble(String colName) {
        return getDouble(getColumnPos(colName));
    }

    /**
     * 返回值为float.
     *
     * @param colName 列名
     * @return float
     */
    public float getFloat(String colName) {
        return getFloat(getColumnPos(colName));
    }

    /**
     * 返回值为String.
     *
     * @param colName 列名
     * @return String
     */
    public String getString(String colName) {
        return getString(getColumnPos(colName));
    }

    /**
     * 返回值为BigInteger.
     *
     * @param colName 列名
     * @return BigInteger
     */
    public BigInteger getBigInteger(String colName) {
        return getBigInteger(getColumnPos(colName));
    }

    /**
     * 返回值为BigDecimal.
     *
     * @param colName 列名
     * @return BigDecimal
     */
    public BigDecimal getDecimal(String colName) {
        return getDecimal(getColumnPos(colName));
    }

    /**
     * 返回值为byte[].
     * @param colName
     * @return
     */
    public byte[] getBytes(String colName) {
        return getBytes(getColumnPos(colName));
    }

    /**
     * 返回值为Date.
     *
     * @param colName 列名
     * @return Date
     */
    public java.util.Date getDate(String colName) {
        return getDate(getColumnPos(colName));
    }

    /**
     * 获取数组中指定位置的数据.
     *
     * @param colIndex 列位置
     * @return 数组中指定位置的数据
     */
    public Object get(int colIndex) {
        return results.get(currentIndex)[--colIndex];
    }

    /**
     * 返回值为boolean.
     *
     * @param colIndex 列位置
     * @return int
     */
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

    /**
     * 返回值为int.
     *
     * @param colIndex 列位置
     * @return int
     */
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

    /**
     * 返回值为long.
     *
     * @param colIndex 列位置
     * @return long
     */
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

    /**
     * 返回值为double.
     *
     * @param colIndex 列位置
     * @return double
     */
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

    /**
     * 返回值为float.
     *
     * @param colIndex 列位置
     * @return float
     */
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

    /**
     * 返回值为String.
     *
     * @param colIndex 列位置
     * @return String
     */
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

    /**
     * 返回值为BigInteger.
     * @param colIndex
     * @return
     */
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

    /**
     * 返回值为BigDecimal.
     * @param colIndex
     * @return
     */
    public BigDecimal getDecimal(int colIndex){
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

    /**
     * 返回值为bytes.
     * @param colIndex
     * @return
     */
    public byte[] getBytes(int colIndex){
        Object data = get(colIndex);
        if (data == null) {
            return null;
        }
        return (byte[]) data;
    }

    /**
     * 返回值为Date.
     *
     * @param colIndex 列位置
     * @return Date
     */
    public java.util.Date getDate(int colIndex) {
        Object data = get(colIndex);
        if (data == null) {
            return null;
        }
        return (java.util.Date) data;
    }

    /**
     * 获取列名位置.
     *
     * @param colName 列名
     * @return 列名位置
     */
    public int getColumnPos(String colName) {
        int index = -1;
        int end = cols.length;
        for (int i = 0; i < end; i++) {
            if (colName.equalsIgnoreCase(cols[i])) {
                index = i;
                break;
            }
        }
        return index;
    }

    /**
     * map类型转换。
     *
     * @param function
     * @param <R>
     * @return
     */
    @JsonIgnore
    public <R> DataList<R> map(Function<DataSet, R> function) {
        ArrayList<R> list = new ArrayList<>();
        while (next()) {
            list.add(function.apply(this));
        }
        return new DataList<>(list, startIndex, resultNum, sizeAll);
    }

}
