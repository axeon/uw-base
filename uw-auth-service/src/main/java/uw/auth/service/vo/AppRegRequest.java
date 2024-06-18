package uw.auth.service.vo;

import uw.auth.service.annotation.MscPermDeclare;

import java.io.Serializable;
import java.util.List;

/**
 * app注册请求信息。
 */
public class AppRegRequest implements Serializable {

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
     * 用户成功登录重定向地址
     */
    private String redirectUrl;

    /**
     * 回调地址
     */
    private String callbackUrl;

    /**
     * 应用权限声明
     */
    private List<PermVo> perms;

    /**
     * PermsVo
     */
    public static class PermVo {
        /**
         * @see MscPermDeclare
         */
        private String name;

        /**
         * @see MscPermDeclare
         */
        private String desc;

        /**
         * @see MscPermDeclare
         */
        private int type;

        /**
         * @see MscPermDeclare
         */
        private int level;

        /**
         * @see org.springframework.web.bind.annotation.RequestMapping
         */
        private String uri;

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

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }
    }

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

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public List<PermVo> getPerms() {
        return perms;
    }

    public void setPerms(List<PermVo> perms) {
        this.perms = perms;
    }
}
