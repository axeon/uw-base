package uw.dao.util;

import uw.dao.vo.FieldMetaInfo;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 * Dao反射工具类.
 *
 * @author zhangjin
 */
public class DaoReflectUtils {

    /**
     * 构造函数.
     */
    private DaoReflectUtils() {
    }

    /**
     * 在preparedStatement中动态set数值.
     *
     * @param pstmt    PreparedStatement
     * @param entity   Object 类的实例
     * @param fmi      列信息
     * @param sequence int 次序
     * @throws Exception 异常
     */
    public static Object DAOLiteSaveReflect(PreparedStatement pstmt, Object entity, FieldMetaInfo fmi, int sequence) throws IllegalAccessException, SQLException {
        Field fd = fmi.getField();
        Class<?> cls = fd.getType();
        Object value = fd.get(entity);
        // 包装类型为 null 时，setInt/setLong 等会因自动拆箱抛 NPE。
        // 统一用 setObject，JDBC 驱动会将 null 映射为对应列的 NULL；基本类型永不为 null，正常走对应 setter。
        if (value == null) {
            pstmt.setObject(sequence, null);
            return value;
        }
        switch (cls.getSimpleName()) {
            case "int", "Integer" -> pstmt.setInt(sequence, (Integer) value);
            case "String" -> pstmt.setString(sequence, (String) value);
            case "Date" -> pstmt.setTimestamp(sequence, DaoValueUtils.dateToTimestamp((Date) value));
            case "long", "Long" -> pstmt.setLong(sequence, (Long) value);
            case "double", "Double" -> pstmt.setDouble(sequence, (Double) value);
            case "float", "Float" -> pstmt.setFloat(sequence, (Float) value);
            case "short", "Short" -> pstmt.setShort(sequence, (Short) value);
            case "byte", "Byte" -> pstmt.setByte(sequence, (Byte) value);
            case "boolean", "Boolean" -> pstmt.setBoolean(sequence, (Boolean) value);
            default -> pstmt.setObject(sequence, value);
        }
        return value;
    }


    /**
     * 通用的反射更新方法.
     *
     * @param pstmt    PreparedStatement对象
     * @param sequence 序列
     * @param value    数值
     * @throws Exception 异常
     */
    public static void CommandUpdateReflect(PreparedStatement pstmt, int sequence, Object value) throws SQLException {
        if (value instanceof Date date) {
            pstmt.setTimestamp(sequence, DaoValueUtils.dateToTimestamp(date));
        } else {
            pstmt.setObject(sequence, value);
        }
    }

    /**
     * 动态载入.
     *
     * @param rs     结果集
     * @param entity 实体类
     * @param fmi    FieldMetaInfo对象
     * @throws Exception 实体类
     */
    public static void DAOLiteLoadReflect(ResultSet rs, Object entity, FieldMetaInfo fmi) throws SQLException, IllegalAccessException {
        Field fd = fmi.getField();
        Class<?> cls = fd.getType();
        String columnName = fmi.getColumnName();
        switch (cls.getSimpleName()) {
            case "int" -> fd.setInt(entity, rs.getInt(columnName));
            case "Integer" -> {
                int v = rs.getInt(columnName);
                fd.set(entity, rs.wasNull() ? null : v);
            }
            case "long" -> fd.setLong(entity, rs.getLong(columnName));
            case "Long" -> {
                long v = rs.getLong(columnName);
                fd.set(entity, rs.wasNull() ? null : v);
            }
            case "String" -> fd.set(entity, DaoValueUtils.nullToStr(rs.getString(columnName)));
            case "Date" -> fd.set(entity, rs.getTimestamp(columnName));
            case "double" -> fd.setDouble(entity, rs.getDouble(columnName));
            case "Double" -> {
                double v = rs.getDouble(columnName);
                fd.set(entity, rs.wasNull() ? null : v);
            }
            case "float" -> fd.setFloat(entity, rs.getFloat(columnName));
            case "Float" -> {
                float v = rs.getFloat(columnName);
                fd.set(entity, rs.wasNull() ? null : v);
            }
            case "short" -> fd.setShort(entity, rs.getShort(columnName));
            case "Short" -> {
                short v = rs.getShort(columnName);
                fd.set(entity, rs.wasNull() ? null : v);
            }
            case "byte" -> fd.setByte(entity, rs.getByte(columnName));
            case "Byte" -> {
                byte v = rs.getByte(columnName);
                fd.set(entity, rs.wasNull() ? null : v);
            }
            case "boolean" -> fd.setBoolean(entity, rs.getBoolean(columnName));
            case "Boolean" -> {
                boolean v = rs.getBoolean(columnName);
                fd.set(entity, rs.wasNull() ? null : v);
            }
            default -> fd.set(entity, rs.getObject(columnName));
        }
    }

}
