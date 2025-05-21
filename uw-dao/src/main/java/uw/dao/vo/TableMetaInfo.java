package uw.dao.vo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * 实体信息.
 *
 * @author axeon
 */
public class TableMetaInfo {

    /**
     * 加载标志.
     */
    public static final String LOADED_FLAG = "_IS_LOADED";
    /**
     * 日志对象.
     */
    private static final Logger logger = LoggerFactory.getLogger(TableMetaInfo.class);
    /**
     * 主键列表.
     */
    private final ArrayList<FieldMetaInfo> pkList = new ArrayList<>();
    /**
     * 列名列表. key=fieldName.
     */
    private final LinkedHashMap<String, FieldMetaInfo> fieldInfoMap = new LinkedHashMap<>();

    /**
     * 列名列表. key=columnName.
     */
    private final LinkedHashMap<String, FieldMetaInfo> columnInfoMap = new LinkedHashMap<>();

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

    /**
     * @return the tableType
     * @return
     */
    public String getTableType() {
        return tableType;
    }

    /**
     * @param tableType the tableType to set
     */
    public void setTableType(String tableType) {
        this.tableType = tableType;
    }

    /**
     * @return the sql
     */
    public String getSql() {
        return sql;
    }

    /**
     * @param sql the sql to set
     */
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
     * 获取fieldInfoMap.
     *
     * @return the fieldMetaInfoMap
     */
    public LinkedHashMap<String, FieldMetaInfo> getFieldInfoMap() {
        return fieldInfoMap;
    }

    /**
     * 获取columnInfoMap.
     *
     * @return the columnInfoMap
     */
    public LinkedHashMap<String, FieldMetaInfo> getColumnInfoMap() {
        return columnInfoMap;
    }

    /**
     * 根据fieldName获取FieldMetaInfo.
     *
     * @param fieldName 属性名
     * @return FieldMetaInfo集合
     */
    public FieldMetaInfo getInfoByFieldName(String fieldName) {
        return fieldInfoMap.get(fieldName);
    }

    /**
     * 根据fieldName获取FieldMetaInfo.
     *
     * @param columnName 属性名
     * @return FieldMetaInfo集合
     */
    public FieldMetaInfo getInfoByColumnName(String columnName) {
        return columnInfoMap.get(columnName);
    }

    /**
     * 向fieldMap中加入FieldMetaInfo信息.
     *
     * @param fieldName 属性名
     * @param fmi        FieldMetaInfo对象
     */
    public void addFieldInfo(String fieldName, FieldMetaInfo fmi) {
        this.fieldInfoMap.put(fieldName, fmi);
    }

    /**
     * 向columnInfoMap中加入FieldMetaInfo信息.
     *
     * @param columnName 属性名
     * @param fmi         FieldMetaInfo对象
     */
    public void addColumnInfo(String columnName, FieldMetaInfo fmi) {
        this.columnInfoMap.put(columnName, fmi);
    }

    /**
     * 根据fieldName获取FieldMetaInfo.
     *
     * @param fieldNames 列名
     * @return FieldMetaInfo集合
     */
    public List<FieldMetaInfo> buildFieldMetaInfoList(Set<String> fieldNames) {
        List<FieldMetaInfo> fieldInfoList = new ArrayList<>();
        for (String fieldName : fieldNames) {
            FieldMetaInfo fieldMetaInfo = fieldInfoMap.get(fieldName);
            fieldInfoList.add(fieldMetaInfo);
        }
        return fieldInfoList;
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
