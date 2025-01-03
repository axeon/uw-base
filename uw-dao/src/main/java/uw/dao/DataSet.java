package uw.dao;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

/**
 * 存储并转化ResultSet对象数据.
 *
 * @author zhangjin
 */
@Schema(title = "DataSet结果集", description = "DataSet结果集")
public class DataSet implements Serializable, Cloneable {

    /**
     * 当前索引位置.
     */
    @JsonIgnore
    private transient int currentIndex = -1;

    /**
     * 开始的索引.
     */
    @JsonProperty
    @Schema(title = "请求的起始索引",description = "请求的起始索引")
    private int startIndex = 0;

    /**
     * 返回的结果集大小.
     */
    @JsonProperty
    @Schema(title = "请求的结果集大小",description ="请求的结果集大小")
    private int resultNum = 0;

    /**
     * List大小（实际返回的结果集大小）.
     */
    @JsonProperty
    @Schema(title = "结果集大小",description = "结果集大小")
    private int size = 0;

    /**
     * 整个表数据量大小.
     */
    @JsonProperty
    @Schema(title = "总数据大小",description = "总数据大小")
    private int sizeAll = 0;

    /**
     * 当前页.
     */
    @JsonProperty
    @Schema(title = "当前页",description = "当前页")
    private int page = 0;

    /**
     * 总页数.
     */
    @JsonProperty
    @Schema(title = "总页数",description = "总页数")
    private int pageCount = 0;


    /**
     * 列名数组.
     */
    @JsonProperty
    @Schema(title = "列名数组",description = "列名数组")
    private String[] cols;

    /**
     * 数据存放数组.
     */
    @JsonProperty
    @Schema(title = "数据集",description = "数据集")
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
        // 获得字段列表
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
            this.results = new ArrayList<Object[]>(resultNum);
        } else {
            this.results = new ArrayList<Object[]>();
        }
        while (rs.next()) {
            this.size++;
            Object[] result = new Object[cols.length];
            for (int x = 0; x < cols.length; x++) {
                // 将对应列名的值放入二维数组中
                switch (colTypes[x]) {
                    case Types.NUMERIC:
                        result[x] = rs.getBigDecimal(x + 1);
                        break;
                    case Types.VARCHAR:
                        result[x] = rs.getString(x + 1);
                        break;
                    case Types.CLOB:
                        result[x] = rs.getString(x + 1);
                        break;
                    case Types.DATE:
                        result[x] = rs.getTimestamp(x + 1);
                        break;
                    case Types.TIME:
                        result[x] = rs.getTimestamp(x + 1);
                        break;
                    case Types.TIMESTAMP:
                        result[x] = rs.getTimestamp(x + 1);
                        break;
                    case Types.BIGINT:
                        result[x] = rs.getLong(x + 1);
                        break;
                    case Types.INTEGER:
                        result[x] = rs.getInt(x + 1);
                        break;
                    case Types.SMALLINT:
                        result[x] = rs.getInt(x + 1);
                        break;
                    case Types.TINYINT:
                        result[x] = rs.getInt(x + 1);
                        break;
                    case Types.FLOAT:
                        result[x] = rs.getFloat(x + 1);
                        break;
                    case Types.DOUBLE:
                        result[x] = rs.getDouble(x + 1);
                        break;
                    case Types.BIT:
                        result[x] = rs.getInt(x + 1);
                        break;

                    default:
                        result[x] = rs.getObject(x + 1);
                }
            }
            results.add(result);
        }
    }

    /**
     * 获得列名列表.
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
     * 获得数组中指定位置的数据.
     *
     * @param colName 列名
     * @return 数组中指定位置的数据
     */
    public Object get(String colName) {
        return ((Object[]) results.get(currentIndex))[getColumnPos(colName)];
    }

    /**
     * 返回值为int.
     *
     * @param colName 列名
     * @return int
     */
    public int getInt(String colName) {
        Object data = get(colName);
        if (data == null) {
            return 0;
        } else {
            return Integer.parseInt(String.valueOf(data));
        }
    }

    /**
     * 返回值为long.
     *
     * @param colName 列名
     * @return long
     */
    public long getLong(String colName) {
        Object data = get(colName);
        if (data == null) {
            return 0;
        } else {
            return Long.parseLong(String.valueOf(data));
        }
    }

    /**
     * 返回值为double.
     *
     * @param colName 列名
     * @return double
     */
    public double getDouble(String colName) {
        Object data = get(colName);
        if (data == null) {
            return 0;
        } else {
            return Double.parseDouble(String.valueOf(data));
        }
    }

    /**
     * 返回值为float.
     *
     * @param colName 列名
     * @return float
     */
    public float getFloat(String colName) {
        Object data = get(colName);
        if (data == null) {
            return 0;
        } else {
            return Float.parseFloat(String.valueOf(data));
        }
    }

    /**
     * 返回值为String.
     *
     * @param colName 列名
     * @return String
     */
    public String getString(String colName) {
        Object data = get(colName);
        if (data == null) {
            return "";
        } else {
            return String.valueOf(data);
        }
    }

    /**
     * 返回值为Date.
     *
     * @param colName 列名
     * @return Date
     */
    public java.util.Date getDate(String colName) {
        return (java.util.Date) get(colName);
    }

    /**
     * 获得数组中指定位置的数据.
     *
     * @param colIndex 列位置
     * @return 数组中指定位置的数据
     */
    public Object get(int colIndex) {
        return ((Object[]) results.get(currentIndex))[--colIndex];
    }

    /**
     * 返回值为int.
     *
     * @param colIndex 列位置
     * @return int
     */
    public int getInt(int colIndex) {
        return (Integer) get(colIndex);
    }

    /**
     * 返回值为long.
     *
     * @param colIndex 列位置
     * @return long
     */
    public long getLong(int colIndex) {
        return (Long) get(colIndex);
    }

    /**
     * 返回值为double.
     *
     * @param colIndex 列位置
     * @return double
     */
    public double getDouble(int colIndex) {
        return (Double) get(colIndex);
    }

    /**
     * 返回值为float.
     *
     * @param colIndex 列位置
     * @return float
     */
    public float getFloat(int colIndex) {
        return (Float) get(colIndex);
    }

    /**
     * 返回值为String.
     *
     * @param colIndex 列位置
     * @return String
     */
    public String getString(int colIndex) {
        String s = String.valueOf(get(colIndex));
        if (s.equals("null")) {
            s = "";
        }
        return s;
    }

    /**
     * 返回值为Date.
     *
     * @param colIndex 列位置
     * @return Date
     */
    public java.util.Date getDate(int colIndex) {
        return (java.util.Date) get(colIndex);
    }

    /**
     * 获得列名位置.
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
}
