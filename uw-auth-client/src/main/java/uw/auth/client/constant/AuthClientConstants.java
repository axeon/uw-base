package uw.auth.client.constant;

import org.springframework.http.HttpStatus;

public class AuthClientConstants {

    /**
     * 200 http code。
     */
    public static final int HTTP_SUCCESS_CODE = HttpStatus.OK.value();

    /**
     * 401 http code。无Token。
     */
    public static final int HTTP_UNAUTHORIZED_CODE = HttpStatus.UNAUTHORIZED.value();

    /**
     * 403 http code。无权限。
     */
    public static final int HTTP_FORBIDDEN_CODE = HttpStatus.FORBIDDEN.value();

    /**
     * 498 http code。token过期。
     */
    public static final int HTTP_TOKEN_EXPIRED_CODE = 498;

    /**
     * 426 http code。token需要升级。
     */
    public static final int HTTP_UPGRADE_REQUIRED_CODE = HttpStatus.UPGRADE_REQUIRED.value();

    /**
     * 402 http code。token需要付费。
     */
    public static final int HTTP_PAYMENT_REQUIRED_CODE = HttpStatus.PAYMENT_REQUIRED.value();

    /**
     * 503 http code。服务不可用。
     */
    public static final int HTTP_SERVICE_UNAVAILABLE_CODE = HttpStatus.SERVICE_UNAVAILABLE.value();
}
