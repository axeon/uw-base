package uw.auth.service.constant;

/**
 * 常量表。
 */
public class AuthConstants {

    /**
     * 用户名与saas_id分隔符
     */
    public static final String ACCOUNT_SPLITTER = "@";

    /**
     * Token Header name
     */
    public static final String TOKEN_HEADER_PARAM = "Authorization";

    /**
     * Token Bearer
     */
    public static final String TOKEN_HEADER_PREFIX = "Bearer ";

    /**
     * 401 http code。
     */
    public static final String HTTP_UNAUTHORIZED_CODE = "401";

    /**
     * 200 http code。
     */
    public static final String HTTP_SUCCESS_CODE = "200";

    /**
     * 获取用户名和SaasId
     *
     * @param loginId
     * @return
     */
    public static Object[] parseUserAndSaasId(String loginId) {
        String user;
        long saasId = -1;
        int pos = loginId.lastIndexOf(AuthConstants.ACCOUNT_SPLITTER);
        if (pos>-1){
            user = loginId.substring(0,pos);
            try {
                saasId = Long.parseLong(loginId.substring(pos+1));
            } catch (Exception e) {
                user = loginId;
            }
        }else{
            user = loginId;
        }
        return new Object[]{user, saasId};
    }


}
