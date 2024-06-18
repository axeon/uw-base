package uw.dao.util;

import uw.dao.annotation.ColumnMeta;
import uw.dao.annotation.TableMeta;
import uw.dao.vo.FieldMetaInfo;
import uw.dao.vo.TableMetaInfo;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EntityMeta工具类。
 */
public class EntityMetaUtils {

    /**
     * 实体信息反射缓存.
     */
    private static final Map<String, TableMetaInfo> entityMetaCache = new ConcurrentHashMap<>(1280);

    /**
     * 实体类支持的最大继承层级
     */
    static final int MAX_ENTITY_CLASS_EXTEND_LEVEL = 10;

    /**
     * 加载读取pojo的注解信息.
     *
     * @param entityCls 实体类类型
     * @return TableMetaInfo对象
     */
    public static TableMetaInfo loadEntityMetaInfo(Class<?> entityCls) {
        return entityMetaCache.computeIfAbsent(entityCls.getName(), (key) -> {
            TableMetaInfo emi = new TableMetaInfo();
            TableMeta tm = entityCls.getAnnotation(TableMeta.class);
            if (tm != null) {
                emi.setTableName(tm.tableName());
                emi.setTableType(tm.tableType());
                emi.setSql(tm.sql());
            }
            Class<?> clazz = entityCls;
            for (int i = 0; clazz != Object.class && i < MAX_ENTITY_CLASS_EXTEND_LEVEL; clazz = clazz.getSuperclass(), i++) {
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    ColumnMeta meta = field.getAnnotation(ColumnMeta.class);
                    if (meta != null) {
                        FieldMetaInfo fieldInfo = new FieldMetaInfo();
                        fieldInfo.setPropertyName(field.getName());
                        fieldInfo.setColumnName(meta.columnName());
                        fieldInfo.setPrimaryKey(meta.primaryKey());
                        fieldInfo.setField(field);
                        fieldInfo.setAutoIncrement(meta.autoIncrement());
                        if (fieldInfo.isPrimaryKey()) {
                            emi.addPkList(fieldInfo);
                        }
                        emi.addColumnMap(meta.columnName(), fieldInfo);
                    }
                }
            }
            return emi;
        });
    }
}
