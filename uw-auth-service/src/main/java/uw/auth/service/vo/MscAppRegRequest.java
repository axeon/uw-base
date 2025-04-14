package uw.auth.service.vo;

import uw.auth.service.annotation.MscPermDeclare;

import java.io.Serializable;
import java.util.List;

/**
 * app注册请求信息。
 */
public class MscAppRegRequest implements Serializable {

    /**
     * 应用服务名称
     */
    private String appName;

    /**
     * 应用显示名称
     */
    private String appLabel;

    /**
     * 应用版本
     */
    private String appVersion;

    /**
     * 应用权限声明
     */
    private List<PermVo> perms;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppLabel() {
        return appLabel;
    }

    public void setAppLabel(String appLabel) {
        this.appLabel = appLabel;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public List<PermVo> getPerms() {
        return perms;
    }

    public void setPerms(List<PermVo> perms) {
        this.perms = perms;
    }

    /**
     * PermsVo
     */
    public static class PermVo {
        /**
         * 权限名称。
         * @see MscPermDeclare
         */
        private String name;

        /**
         * 权限描述。
         * @see MscPermDeclare
         */
        private String desc;

        /**
         * 用户类型
         * @see MscPermDeclare
         */
        private int user;

        /**
         * 权限代码。
         * @see
         */
        private String code;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public int getUser() {
            return user;
        }

        public void setUser(int user) {
            this.user = user;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }
}
