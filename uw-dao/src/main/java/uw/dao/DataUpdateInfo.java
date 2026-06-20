package uw.dao;

import io.swagger.v3.oas.annotations.media.Schema;
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
    private final LinkedHashMap<String, UpdateInfo> updatedMap = new LinkedHashMap<>();

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
     * @param dataUpdateInfo 待检查的更新信息对象（可为 null）
     * @return true 表示存在字段变更记录
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
     * @param fieldName 字段名
     * @param oldValue  旧值
     * @param newValue  新值
     * @param isForce   是否强制添加更新信息（true 时即使新旧值相同也记录）
     * @return 当前对象（链式调用）
     */
    public DataUpdateInfo addUpdateInfo(String fieldName, Object oldValue, Object newValue, boolean isForce) {
        if (isForce || !Objects.equals(oldValue, newValue)) {
            updatedMap.put(fieldName, new UpdateInfo(oldValue, newValue));
        }
        return this;
    }

    /**
     * 获取更新Map。
     *
     * @return 字段名到新旧值映射（保持插入顺序）
     */
    public LinkedHashMap<String, UpdateInfo> getUpdatedMap() {
        return updatedMap;
    }

    /**
     * 获取更新字段集合。
     *
     * @return 已变更的字段名集合
     */
    public Set<String> getUpdateFieldSet() {
        return updatedMap.keySet();
    }

    /**
     * 转为JSON字符串。
     *
     * @return 更新信息的 JSON 表示
     */
    @Override
    public String toString() {
        return JsonUtils.toString(updatedMap);
    }

    /**
     * 更新信息。
     */
    @Schema(title = "更新信息", description = "更新信息")
    public static class UpdateInfo {
        /**
         * 旧值。
         */
        @Schema(title = "旧值", description = "旧值")
        private Object oldValue;
        /**
         * 新值。
         */
        @Schema(title = "新值", description = "新值")
        private Object newValue;

        public UpdateInfo(Object oldValue, Object newValue) {
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public UpdateInfo() {
        }

        public Object getOldValue() {
            return oldValue;
        }

        public void setOldValue(Object oldValue) {
            this.oldValue = oldValue;
        }

        public Object getNewValue() {
            return newValue;
        }

        public void setNewValue(Object newValue) {
            this.newValue = newValue;
        }
    }
}
