package uw.dao;

import org.apache.commons.lang3.tuple.Pair;
import uw.common.util.JsonUtils;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Set;

/**
 * 数据更新信息。
 */
public class DataUpdateInfo {

    /**
     * 更新Map。
     * key: 更新字段。
     * Value: left:oldValue; right:newValue;
     */
    private final LinkedHashMap<String, Pair<?, ?>> updatedMap = new LinkedHashMap<>();

    /**
     * 添加更新信息。
     *
     * @param updateInfo
     * @param fieldName
     * @param oldValue
     * @param newValue
     * @param isForce    是否强制添加更新信息。
     */
    public static DataUpdateInfo addUpdateInfo(DataUpdateInfo updateInfo, String fieldName, Object oldValue, Object newValue, boolean isForce) {
        if (updateInfo == null) {
            updateInfo = new DataUpdateInfo();
        }
        return updateInfo.addUpdateInfo(fieldName, oldValue, newValue, isForce);
    }

    /**
     * 检查是否包含更新信息。
     *
     * @param dataUpdateInfo
     * @return
     */
    public static boolean hasUpdateInfo(DataUpdateInfo dataUpdateInfo) {
        if (dataUpdateInfo == null) {
            return false;
        }
        return !dataUpdateInfo.updatedMap.isEmpty();
    }

    /**
     * 添加更新信息。
     *
     * @param fieldName
     * @param oldValue
     * @param newValue
     * @param isForce   是否强制添加更新信息。
     */
    public DataUpdateInfo addUpdateInfo(String fieldName, Object oldValue, Object newValue, boolean isForce) {
        if (isForce || !Objects.equals(oldValue, newValue)) {
            updatedMap.put(fieldName, Pair.of(oldValue, newValue));
        }
        return this;
    }

    /**
     * 获取更新Map。
     *
     * @return
     */
    public LinkedHashMap<String, Pair<?, ?>> getUpdatedMap() {
        return updatedMap;
    }

    /**
     * 获取更新字段集合。
     *
     * @return
     */
    public Set<String> getUpdateFieldSet() {
        return updatedMap.keySet();
    }

    /**
     * 转为JSON字符串。
     * @return
     */
    @Override
    public String toString() {
        return JsonUtils.toString(updatedMap);
    }

}
