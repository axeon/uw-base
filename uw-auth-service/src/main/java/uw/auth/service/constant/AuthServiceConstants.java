package uw.auth.service.constant;

import org.springframework.http.HttpStatus;

/**
 * 常量表。
 */
public class AuthServiceConstants {

    /**
     * access token的分隔符。
     */
    public static final char TOKEN_ACCESS_TYPE_SEPARATOR = '$';

    /**
     * refresh token的分隔符。
     */
    public static final char TOKEN_REFRESH_TYPE_SEPARATOR = '#';

    /**
     * access token的用户信息分隔符。
     */
    public static final char TOKEN_USER_SEPARATOR = '!';

    /**
     * access token的SAAS分隔符。
     */
    public static final char TOKEN_SAAS_SEPARATOR = '@';

    /**
     * Token Header name
     */
    public static final String TOKEN_HEADER_PARAM = "Authorization";

    /**
     * Token Bearer
     */
    public static final String TOKEN_HEADER_PREFIX = "Bearer ";

    /**
     * 200 http code。
     */
    public static final String HTTP_SUCCESS_CODE = String.valueOf(HttpStatus.OK.value());

    /**
     * 401 http code。无Token。
     */
    public static final String HTTP_UNAUTHORIZED_CODE = String.valueOf(HttpStatus.UNAUTHORIZED.value());

    /**
     * 403 http code。无权限。
     */
    public static final String HTTP_FORBIDDEN_CODE = String.valueOf(HttpStatus.FORBIDDEN.value());

    /**
     * 498 http code。token过期。
     */
    public static final String HTTP_TOKEN_EXPIRED_CODE = String.valueOf(498);

    /**
     * 426 http code。token需要升级。
     */
    public static final String HTTP_UPGRADE_REQUIRED_CODE = String.valueOf(HttpStatus.UPGRADE_REQUIRED.value());

    /**
     * 402 http code。token需要付费。
     */
    public static final String HTTP_PAYMENT_REQUIRED_CODE = String.valueOf(HttpStatus.PAYMENT_REQUIRED.value());

    /**
     * 503 http code。服务不可用。
     */
    public static final String HTTP_SERVICE_UNAVAILABLE_CODE = String.valueOf(HttpStatus.SERVICE_UNAVAILABLE.value());

    /**
     * 获取用户名和SaasId
     *
     * @param loginId
     * @return
     */
    public static Object[] parseUserAndSaasId(String loginId) {
        String user;
        long saasId = -1;
        int pos = loginId.lastIndexOf(AuthServiceConstants.TOKEN_SAAS_SEPARATOR);
        if (pos > -1) {
            user = loginId.substring(0, pos);
            try {
                saasId = Long.parseLong(loginId.substring(pos + 1));
            } catch (Exception e) {
                user = loginId;
            }
        } else {
            user = loginId;
        }
        return new Object[]{user, saasId};
    }


}
