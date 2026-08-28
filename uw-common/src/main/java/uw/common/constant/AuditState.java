package uw.common.constant;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 全局审批状态枚举（审批状态机统一值域）。
 * <p>
 * 统一约定审批维度状态值：0 草稿 / 1 已申请待审批 / -1 已撤回 / 2 审批通过 / -2 审批拒绝 / -3 已撤销 / -4 已过期。
 * 审批维与数据状态维（{@link CommonState}）正交：audit_state 表达单据审批生命周期，
 * state 表达数据行生命周期（启用/禁用/软删）。
 * 死单（终态）查询约定：audit_state &lt; 0。
 * </p>
 * 语义约定：
 * <ul>
 *   <li>CANCEL(-1)：审批前申请方主动撤回（撤的是"申请"）；草稿的终结是 delete，不走 CANCEL。</li>
 *   <li>REJECT(-2)：审批环节的负向结果。</li>
 *   <li>REVOKE(-3)：APPROVE 后人工撤销（撤的是"通过"），仅授权类业务启用。</li>
 *   <li>EXPIRE(-4)：时效自然耗尽（待审超时/生效到期），系统终结，非人工动作。</li>
 * </ul>
 * 序列化为 JSON 时以对象形态输出（包含 value 与 label 字段）。
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@Schema(title = "全局审批状态", description = "全局审批状态")
public enum AuditState {

    /** 草稿（已创建未提交）。 */
    INIT(0, "草稿"),
    /** 已申请待审批。 */
    APPLY(1, "已申请待审批"),
    /** 已撤回（审批前，申请方主动）。 */
    CANCEL(-1, "已撤回"),
    /** 审批通过。 */
    APPROVE(2, "审批通过"),
    /** 审批拒绝。 */
    REJECT(-2, "审批拒绝"),
    /** 已撤销（审批通过后人工撤销，仅授权类业务启用）。 */
    REVOKE(-3, "已撤销"),
    /** 已过期（时效自然耗尽：待审超时/生效到期，系统终结）。 */
    EXPIRE(-4, "已过期");

    /**
     * 状态值
     */
    private final int value;

    /**
     * 状态描述
     */
    private final String label;

    AuditState(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 根据状态值解析枚举。
     * <p>
     * 若传入值匹配不上任何枚举常量，将返回 {@link #REJECT}（保守策略：不进待审、不进生效）。
     * </p>
     *
     * @param value 状态值
     * @return 匹配的枚举常量；匹配不上时返回 {@link #REJECT}
     */
    public static AuditState valueOf(int value) {
        for (AuditState state : values()) {
            if (state.getValue() == value) {
                return state;
            }
        }
        return REJECT;
    }

    /**
     * 获取状态值。
     *
     * @return 状态值（0/1/-1/2/-2/-3/-4）
     */
    public int getValue() {
        return value;
    }

    /**
     * 获取状态描述。
     *
     * @return 状态描述文本
     */
    public String getLabel() {
        return label;
    }
}
