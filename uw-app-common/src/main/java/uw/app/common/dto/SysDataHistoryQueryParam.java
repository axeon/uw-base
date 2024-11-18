package uw.app.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import uw.dao.annotation.QueryMeta;

import java.util.Date;

/**
 * 系统数据历史列表查询参数。
 */
@Schema(title = "系统数据历史列表查询参数", description = "系统数据历史列表查询参数")
public class SysDataHistoryQueryParam extends AuthPageQueryParam {

    /**
     * ID。
     */
    @QueryMeta(expr = "id=?")
    @Schema(title = "ID", description = "ID")
    private Long id;
    /**
     * 商户ID。
     */
    @QueryMeta(expr = "mch_id=?")
    @Schema(title = "商户ID", description = "商户ID")
    private Long mchId;
    /**
     * 用户ID。
     */
    @QueryMeta(expr = "user_id=?")
    @Schema(title = "用户ID", description = "用户ID")
    private Long userId;
    /**
     * 用户类型。
     */
    @QueryMeta(expr = "user_type=?")
    @Schema(title = "用户类型", description = "用户类型")
    private Integer userType;
    /**
     * 用户的组ID。
     */
    @QueryMeta(expr = "group_id=?")
    @Schema(title = "用户的组ID", description = "用户的组ID")
    private Long groupId;
    /**
     * 用户名称。
     */
    @QueryMeta(expr = "user_name like ?")
    @Schema(title = "用户名称", description = "用户名称")
    private String userName;
    /**
     * 用户昵称。
     */
    @QueryMeta(expr = "nick_name like ?")
    @Schema(title = "用户昵称", description = "用户昵称")
    private String nickName;
    /**
     * 真实名称。
     */
    @QueryMeta(expr = "real_name like ?")
    @Schema(title = "真实名称", description = "真实名称")
    private String realName;
    /**
     * 实体类。
     */
    @QueryMeta(expr = "entity_class like ?")
    @Schema(title = "实体类", description = "实体类")
    private String entityClass;
    /**
     * 实体ID。
     */
    @QueryMeta(expr = "entity_id like ?")
    @Schema(title = "实体ID", description = "实体ID")
    private String entityId;
    /**
     * 实体名。
     */
    @QueryMeta(expr = "entity_name like ?")
    @Schema(title = "实体名", description = "实体名")
    private String entityName;
    /**
     * 用户IP。
     */
    @QueryMeta(expr = "user_ip like ?")
    @Schema(title = "用户IP", description = "用户IP")
    private String userIp;
    /**
     * 创建日期范围。
     */
    @QueryMeta(expr = "create_date between ? and ?")
    @Schema(title = "创建日期范围", description = "创建日期范围")
    private Date[] createDateRange;

    public SysDataHistoryQueryParam() {
        super();
    }

    public SysDataHistoryQueryParam(Long saasId) {
        super( saasId );
    }

    /**
     * 获取ID。
     */
    public Long getId() {
        return this.id;
    }

    /**
     * 设置ID。
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取商户ID。
     */
    public Long getMchId() {
        return this.mchId;
    }

    /**
     * 设置商户ID。
     */
    public void setMchId(Long mchId) {
        this.mchId = mchId;
    }

    /**
     * 获取用户ID。
     */
    public Long getUserId() {
        return this.userId;
    }

    /**
     * 设置用户ID。
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获取用户类型。
     */
    public Integer getUserType() {
        return this.userType;
    }

    /**
     * 设置用户类型。
     */
    public void setUserType(Integer userType) {
        this.userType = userType;
    }

    /**
     * 获取用户的组ID。
     */
    public Long getGroupId() {
        return this.groupId;
    }

    /**
     * 设置用户的组ID。
     */
    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    /**
     * 获取用户名称。
     */
    public String getUserName() {
        return this.userName;
    }

    /**
     * 设置用户名称。
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * 获取用户昵称。
     */
    public String getNickName() {
        return this.nickName;
    }

    /**
     * 设置用户昵称。
     */
    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    /**
     * 获取真实名称。
     */
    public String getRealName() {
        return this.realName;
    }

    /**
     * 设置真实名称。
     */
    public void setRealName(String realName) {
        this.realName = realName;
    }

    /**
     * 获取实体类。
     */
    public String getEntityClass() {
        return this.entityClass;
    }

    /**
     * 设置实体类。
     */
    public void setEntityClass(String entityClass) {
        this.entityClass = entityClass;
    }

    /**
     * 设置实体类。
     */
    public void setEntityClass(Class entityClass) {
        this.entityClass = entityClass.getName();
    }

    /**
     * 获得实体ID。
     */
    public String getEntityId() {
        return this.entityId;
    }

    /**
     * 设置实体ID。
     */
    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    /**
     * 获取实体名。
     */
    public String getEntityName() {
        return this.entityName;
    }

    /**
     * 设置实体名。
     */
    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    /**
     * 获取用户IP。
     */
    public String getUserIp() {
        return this.userIp;
    }

    /**
     * 设置用户IP。
     */
    public void setUserIp(String userIp) {
        this.userIp = userIp;
    }

    /**
     * 获取创建日期范围。
     */
    public Date[] getCreateDateRange() {
        return this.createDateRange;
    }

    /**
     * 设置创建日期范围。
     */
    public void setCreateDateRange(Date[] createDateRange) {
        this.createDateRange = createDateRange;
    }

}