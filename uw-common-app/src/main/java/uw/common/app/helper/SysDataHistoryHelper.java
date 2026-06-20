package uw.common.app.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.auth.service.AuthServiceHelper;
import uw.common.app.entity.SysDataHistory;
import uw.common.util.JsonUtils;
import uw.common.util.SystemClock;
import uw.dao.DaoFactory;
import uw.dao.DataEntity;

import java.io.Serializable;

/**
 * 系统公用的数据历史记录 Helper。
 * <p>
 * 将实体快照（含变更差异）落库到 {@code sys_data_history}，用于数据回滚与审计。
 * 自动从 {@link AuthServiceHelper} 获取当前用户上下文（非 web 环境下用户相关字段填默认值）。
 * 历史记录为辅助功能：落库失败不阻断主业务，仅记录 WARN 日志；{@link Error} 级异常会重新抛出。
 * </p>
 * 使用示例：
 * <pre>{@code
 * SysDataHistoryHelper.saveHistory(user, "更新前");
 * dao.update(user);
 * }</pre>
 */
public class SysDataHistoryHelper {

    private static final Logger log = LoggerFactory.getLogger(SysDataHistoryHelper.class);

    private static final DaoFactory dao = DaoFactory.getInstance();

    /**
     * 保存实体历史记录（自动提取实体 ID 与名称，无备注）。
     *
     * @param dataEntity 实体数据，需实现 {@link DataEntity}
     */
    public static void saveHistory(DataEntity dataEntity) {
        saveHistory(dataEntity.ENTITY_ID(), dataEntity, dataEntity.ENTITY_NAME(), null);
    }

    /**
     * 保存实体历史记录（自动提取实体 ID 与名称，附带备注）。
     *
     * @param dataEntity 实体数据，需实现 {@link DataEntity}
     * @param remark     备注信息
     */
    public static void saveHistory(DataEntity dataEntity, String remark) {
        saveHistory(dataEntity.ENTITY_ID(), dataEntity, dataEntity.ENTITY_NAME(), remark);
    }

    /**
     * 保存历史记录（完整参数）。
     * <p>
     * 记录内容包括：操作人上下文、实体数据（JSON 快照）、实体更新差异（若 dataEntity 为 {@link DataEntity}
     * 且存在更新信息）。落库成功后会清除原实体的更新信息。
     * </p>
     *
     * @param entityId   实体ID
     * @param dataEntity 实体数据（序列化为 JSON 快照）
     * @param entityName 实体名（用于展示）
     * @param remark     备注信息
     */
    public static void saveHistory(Serializable entityId, Object dataEntity, String entityName, String remark) {
        try {
            SysDataHistory history = new SysDataHistory();
            history.setId(dao.getSequenceId(SysDataHistory.class));
            history.setEntityId(String.valueOf(entityId));
            history.setEntityClass(dataEntity.getClass().getName());
            history.setEntityName(entityName);
            history.setRemark(remark);
            if (AuthServiceHelper.getContextToken() != null) {
                history.setSaasId(AuthServiceHelper.getSaasId());
                history.setMchId(AuthServiceHelper.getMchId());
                history.setUserId(AuthServiceHelper.getUserId());
                history.setGroupId(AuthServiceHelper.getGroupId());
                history.setUserType(AuthServiceHelper.getUserType());
                history.setUserName(AuthServiceHelper.getUserName());
                history.setNickName(AuthServiceHelper.getNickName());
                history.setRealName(AuthServiceHelper.getRealName());
                history.setUserIp(AuthServiceHelper.getRemoteIp());
            } else {
                history.setSaasId(0);
                history.setMchId(0);
                history.setUserId(0);
                history.setGroupId(0);
                history.setUserType(-1);
                history.setUserName(null);
                history.setNickName(null);
                history.setRealName(null);
                history.setUserIp(null);
            }
            history.setCreateDate(SystemClock.nowDate());
            history.setEntityData(JsonUtils.toString(dataEntity));
            if (dataEntity instanceof DataEntity de) {
                if (de.GET_UPDATED_INFO() != null) {
                    history.setEntityUpdateInfo(JsonUtils.toString(de.GET_UPDATED_INFO().getUpdatedMap()));
                }
            }
            // 历史记录写入数据库后再清理原实体的更新信息，避免 save 失败时副作用泄漏给调用方对象。
            dao.save(history);
            if (dataEntity instanceof DataEntity de) {
                de.CLEAR_UPDATED_INFO();
            }
        } catch (Error e) {
            // OOM/StackOverflow等不可恢复错误，记录后重新抛出
            log.error("Critical error saving history: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            // 历史记录为辅助功能，失败不阻断主业务，但需明确告警以便排查数据历史缺失。
            log.warn("系统数据历史保存失败，entityClass=[{}] entityId=[{}]：{}",
                    dataEntity.getClass().getName(), entityId, e.getMessage(), e);
        }
    }


}
