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
        if (cls == int.class) {
            pstmt.setInt(sequence, (Integer) value);
        } else if (cls == String.class) {
            pstmt.setObject(sequence, value);
        } else if (cls == Date.class) {
            pstmt.setTimestamp(sequence, DaoValueUtils.dateToTimestamp((Date) value));
        } else if (cls == long.class) {
            pstmt.setLong(sequence, (Long) value);
        } else if (cls == double.class) {
            pstmt.setDouble(sequence, (Double) value);
        } else if (cls == float.class) {
            pstmt.setFloat(sequence, (Float) value);
        } else if (cls == short.class) {
            pstmt.setShort(sequence, (Short) value);
        } else if (cls == byte.class) {
            pstmt.setByte(sequence, (Byte) value);
        } else if (cls == boolean.class) {
            pstmt.setBoolean(sequence, (Boolean) value);
        } else {
            pstmt.setObject(sequence, value);
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
        if (cls == int.class) {
            fd.setInt(entity, rs.getInt(fmi.getColumnName()));
        } else if (cls == long.class) {
            fd.setLong(entity, rs.getLong(fmi.getColumnName()));
        } else if (cls == String.class) {
            fd.set(entity, DaoValueUtils.nullToStr(rs.getString(fmi.getColumnName())));
        } else if (cls == Date.class) {
            fd.set(entity, rs.getTimestamp(fmi.getColumnName()));
        } else if (cls == double.class) {
            fd.setDouble(entity, rs.getDouble(fmi.getColumnName()));
        } else if (cls == float.class) {
            fd.setFloat(entity, rs.getFloat(fmi.getColumnName()));
        } else if (cls == short.class) {
            fd.setShort(entity, rs.getShort(fmi.getColumnName()));
        } else if (cls == byte.class) {
            fd.setByte(entity, rs.getByte(fmi.getColumnName()));
        } else if (cls == boolean.class) {
            fd.setBoolean(entity, rs.getBoolean(fmi.getColumnName()));
        } else {
            fd.set(entity, rs.getObject(fmi.getColumnName()));
        }

    }

}
