package uw.auth.service.vo;

import java.util.Map;
import java.util.Set;

/**
 * mscPerm设置licenseCode的方法体。
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
