package uw.auth.service.vo;

import uw.auth.service.token.InvalidTokenData;

import java.util.List;

/**
 * 应用状态上报响应 VO。
 * <p>
 * auth-center 对 {@code reportStatus} 的响应，返回应用主机 ID 及需要本地失效的 Token 列表，
 * {@code MscAppUpdateService} 据此下发踢人指令。
 *
 * @author axeon
 */
public class MscAppReportResponse {

    /**
     * 成功标识: 1: 成功,0,待补充 -1: 失败
     */
    private int state;

    /**
     * id
     */
    private long id;

    /**
     * 非法token列表。
     */
    private List<InvalidTokenData> invalidTokenDataList;

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<InvalidTokenData> getInvalidTokenDataList() {
        return invalidTokenDataList;
    }

    public void setInvalidTokenDataList(List<InvalidTokenData> invalidTokenDataList) {
        this.invalidTokenDataList = invalidTokenDataList;
    }
}
