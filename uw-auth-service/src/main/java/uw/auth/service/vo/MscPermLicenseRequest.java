package uw.auth.service.vo;

import java.util.Map;
import java.util.Set;

/**
 * 权限 License 更新请求 VO。
 * <p>
 * 用于 {@code AuthAppRpc.updatePermLicense}，将 licenseCode 与其对应的权限 ID 集合上报
 * auth-center，实现基于 License 的权限授权。
 *
 * @author axeon
 */
public class MscPermLicenseRequest {

    public MscPermLicenseRequest() {
    }

    public MscPermLicenseRequest(long appId, Map<String, Set<Integer>> licensePermMap) {
        this.appId = appId;
        this.licensePermMap = licensePermMap;
    }

    /**
     * appId。
     */
    private long appId;

    /**
     * licensePermMap。
     * key:licenseCode
     * value: permIdSet
     */
    private Map<String, Set<Integer>> licensePermMap;

    public long getAppId() {
        return appId;
    }

    public void setAppId(long appId) {
        this.appId = appId;
    }

    public Map<String, Set<Integer>> getLicensePermMap() {
        return licensePermMap;
    }

    public void setLicensePermMap(Map<String, Set<Integer>> licensePermMap) {
        this.licensePermMap = licensePermMap;
    }
}
