package uw.dao.vo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * 实体信息.
 *
 * @author axeon
 */
public class TableMetaInfo {

    /**
     * 日志对象.
     */
    private static final Logger logger = LoggerFactory.getLogger(TableMetaInfo.class);
    /**
     * 加载标志.
     */
    public static final String LOADED_FLAG = "_IS_LOADED";
    /**
     * 主键列表.
     */
    private final ArrayList<FieldMetaInfo> pkList = new ArrayList<FieldMetaInfo>();
    /**
     * 列名列表. key=column.
     */
    private final LinkedHashMap<String, FieldMetaInfo> columnMap = new LinkedHashMap<String, FieldMetaInfo>();
    /**
     * 表名.
     */
    private String tableName;
    /**
     * 表类型，table/view
     */
    private String tableType;
    /**
     * 查询sql。
     */
    private String sql;
    /**
     * 加载标志字段.
     */
    private Field loadFlagField;

    /**
     * @return the tableName
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * @param tableName the tableName to set
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }


    public String getTableType() {
        return tableType;
    }

    public void setTableType(String tableType) {
        this.tableType = tableType;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    /**
     * @return the pklist
     */
    public ArrayList<FieldMetaInfo> getPkList() {
        return pkList;
    }

    /**
     * @param fi the pklist to set
     */
    public void addPkList(FieldMetaInfo fi) {
        this.pkList.add(fi);
    }

    /**
     * @return the columnMap
     */
    public LinkedHashMap<String, FieldMetaInfo> getColumnMap() {
        return columnMap;
    }

    /**
     * 根据columnName获取FieldMetaInfo.
     *
     * @param columnName 列名
     * @return FieldMetaInfo集合
     */
    public FieldMetaInfo getFieldMetaInfo(String columnName) {
        return columnMap.get(columnName);
    }

    /**
     * 向ColumnMap中加入FieldInfo信息.
     *
     * @param columnName 列名
     * @param fi         FieldMetaInfo对象
     */
    public void addColumnMap(String columnName, FieldMetaInfo fi) {
        this.columnMap.put(columnName, fi);
    }

    /**
     * 获取加载标志字段.
     *
     * @return
     */
    public Field getLoadFlagField() {
        return loadFlagField;
    }

    /**
     * 设置加载标志字段.
     *
     * @param loadFlagField
     */
    public void setLoadFlagField(Field loadFlagField) {
        this.loadFlagField = loadFlagField;
    }

    /**
     * 设置加载标志.
     *
     * @param object
     */
    public void setLoadFlag(Object object) {
        if (this.loadFlagField != null) {
            try {
                this.loadFlagField.setBoolean(object, true);
            } catch (IllegalAccessException e) {
                logger.error("tableName[{}] setLoadFlag Error! {}", tableName, e.getMessage(), e);
            }
        }
    }
}
