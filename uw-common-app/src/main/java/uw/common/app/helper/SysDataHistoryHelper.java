package uw.common.app.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.app.entity.SysDataHistory;
import uw.auth.service.AuthServiceHelper;
import uw.common.util.JsonUtils;
import uw.dao.DaoFactory;
import uw.dao.DataEntity;
import uw.dao.TransactionException;
import uw.httpclient.exception.DataMapperException;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统公用的历史记录存储Helper。
 */
public class SysDataHistoryHelper {

    private static final Logger log = LoggerFactory.getLogger( SysDataHistoryHelper.class );

    private static final DaoFactory dao = DaoFactory.getInstance();

    /**
     * 保存历史记录。
     * @param dataEntity 实体数据
     */
    public static void saveHistory(DataEntity dataEntity) {
        saveHistory( dataEntity.ENTITY_ID(), dataEntity, dataEntity.ENTITY_NAME(), null);
    }

    /**
     * 保存历史记录。
     * @param dataEntity 实体数据
     * @param remark 备注信息
     */
    public static void saveHistory(DataEntity dataEntity, String remark) {
        saveHistory( dataEntity.ENTITY_ID(), dataEntity, dataEntity.ENTITY_NAME(), remark);
    }

    /**
     * 保存历史记录。
     * @param entityId 实体ID
     * @param dataEntity 实体数据
     * @param entityName 实体名
     * @param remark 备注信息
     */
    public static void saveHistory(Serializable entityId, Object dataEntity, String entityName, String remark) {
        SysDataHistory history = new SysDataHistory();
        history.setId( dao.getSequenceId( SysDataHistory.class ) );
        history.setEntityId( String.valueOf( entityId ) );
        history.setEntityClass( dataEntity.getClass().getName() );
        history.setEntityName( entityName );
        history.setRemark( remark );
        if (AuthServiceHelper.getContextToken() != null) {
            history.setSaasId( AuthServiceHelper.getSaasId() );
            history.setMchId( AuthServiceHelper.getMchId() );
            history.setUserId( AuthServiceHelper.getUserId() );
            history.setGroupId( AuthServiceHelper.getGroupId() );
            history.setUserType( AuthServiceHelper.getUserType() );
            history.setUserName( AuthServiceHelper.getUserName() );
            history.setNickName( AuthServiceHelper.getNickName() );
            history.setRealName( AuthServiceHelper.getRealName() );
            history.setUserIp( AuthServiceHelper.getRemoteIp() );
        } else {
            history.setSaasId( 0 );
            history.setMchId( 0 );
            history.setUserId( 0 );
            history.setGroupId( 0 );
            history.setUserType( -1 );
            history.setUserName( null );
            history.setNickName( null );
            history.setRealName( null );
            history.setUserIp( null );
        }
        history.setCreateDate( new Date() );
        try {
            history.setEntityData( JsonUtils.toString( dataEntity ) );
            if (dataEntity instanceof DataEntity de) {
                history.setEntityUpdateInfo( de.GET_UPDATED_INFO() );
            }
        } catch (DataMapperException e) {
            log.error( e.getMessage(), e );
        }
        history.setCreateDate( new Date() );
        try {
            dao.save( history );
        } catch (TransactionException e) {
            log.error( e.getMessage(), e );
        }
    }


}
