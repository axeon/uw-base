# uw-oauth2-client

uw-oauth2-client是一个轻量级的OAuth2客户端库，提供多平台OAuth2通用第三方登录功能，兼容Google、Apple、GitHub、微信、及支付宝等主流平台，并支持通过添加Provider的方式无缝集成新的第三方登录平台。同时，还提供了扫码登录功能。

## 功能特性

- 支持OAuth2标准协议，兼容Google、Apple、GitHub等OAuth2平台。
- 支持微信、支付宝等国内主流平台的OAuth2登录。
- 支持通过添加Provider的方式无缝集成新的第三方登录平台。
- 提供扫码登录功能，包括二维码生成、扫码状态实时验证、扫码结果异步处理。
- 与现有token系统无缝对接，包括用户信息映射、token生成与管理。
- 完善的错误处理机制，定义清晰的错误码体系。
- 详细的日志记录，便于问题排查和系统监控。
- 模块化设计，代码结构清晰，便于后续维护和功能扩展。

## 技术栈

- Java 21+
- uw-httpclient（HTTP客户端）
- 不依赖Spring Security OAuth2框架

## 快速开始

### 1. 引入依赖

在项目的pom.xml文件中添加以下依赖：

```xml
<dependency>
    <groupId>com.umtone</groupId>
    <artifactId>uw-oauth2-client</artifactId>
</dependency>
```

### 2. 配置Provider

在application.yml或application.properties中配置第三方登录平台的Provider信息：

```yaml
uw:
  oauth2:
    client:
      # 重定向URL，用于接收授权结果。一般设置为前端网页。
      redirect-uri: http://localhost:8080/ui/oauth2/redirect
      # 扫码登录链接配置，设置为后端API，必须以“/”结尾。
      qrcode-uri: http://localhost:8080/oauth2/qrcode/
      # 第三方登录平台配置
      providers:
        google:
          clientId: your-google-client-id
          clientSecret: your-google-client-secret
        github:
          clientId: your-github-client-id
          clientSecret: your-github-client-secret
        apple:
          clientId: com.yourcompany.app
          extParam:
            teamId: your-team-id
            keyId: your-key-id
            p8Key: your-p8-key
        wechat:
          clientId: your-wechat-client-id
          clientSecret: your-wechat-client-secret
        alipay:
          clientId: your-alipay-client-id
          clientSecret: your-alipay-client-secret
```

### 3. 系统集成库表变动


#### 3.1 现有用户表（范例），不需要额外改造。

```sql
CREATE TABLE `msc_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) DEFAULT NULL COMMENT '密码（预留，第三方登录可能不需要）',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态（1：启用，0：禁用）',
  `create_date` datetime NOT NULL COMMENT '创建时间',
  `last_update` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

#### 3.2 新增第三方登录信息表（msc_oauth_info）

```sql
CREATE TABLE `msc_oauth_info` (
  `id` bigint NOT NULL COMMENT 'ID',
  `saas_id` bigint NOT NULL COMMENT '运营商Id',
  `user_id` bigint NOT NULL COMMENT '本地用户ID',
  `provider_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '三方平台标识（如：google、wechat、github、alipay、weibo、apple）',
  `open_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '三方平台用户ID',
  `union_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '三方平台统一ID',
  `username` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '用户名',
  `avatar` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '用户头像',
  `gender` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '性别',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'email地址',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '电话号码',
  `area` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '地区',
  `address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '地址',
  `raw_user_info` json DEFAULT NULL COMMENT '用户信息',
  `last_login_date` datetime(3) DEFAULT NULL COMMENT '最近登录时间',
  `last_login_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '更新时间',
  `logon_count` int DEFAULT '0' COMMENT '登录次数',
  `create_date` datetime(3) NOT NULL COMMENT '建立时间',
  `modify_date` datetime(3) DEFAULT NULL COMMENT '修改时间',
  `bind_date` datetime(3) DEFAULT NULL COMMENT '创建时间',
  `unbind_date` datetime(3) DEFAULT NULL COMMENT '修改日期',
  `state` int NOT NULL COMMENT '状态',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_provider_user` (`open_id`,`provider_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='MSC三方登录信息';

```

### 4. Java新增实体类

#### 4.1 MscOauthInfo类（忽略get/set）
```java

/**
 * MscOauthInfo实体类
 * MSC三方登录信息
 *
 * @author axeon
 */
@TableMeta(tableName="msc_oauth_info",tableType="table")
@Schema(title = "MSC三方登录信息", description = "MSC三方登录信息")
public class MscOauthInfo implements DataEntity,Serializable {


    /**
     * ID
     */
    @ColumnMeta(columnName = "id", dataType = "long", dataSize = 19, nullable = false, primaryKey = true)
    @Schema(title = "ID", description = "ID", maxLength = 19, nullable = false)
    private long id;

    /**
     * 运营商Id
     */
    @ColumnMeta(columnName = "saas_id", dataType = "long", dataSize = 19, nullable = false, primaryKey = true)
    @Schema(title = "运营商Id", description = "运营商Id", maxLength = 19, nullable = false)
    private long saasId;

    /**
     * 本地用户ID
     */
    @ColumnMeta(columnName = "user_id", dataType = "long", dataSize = 19, nullable = false)
    @Schema(title = "本地用户ID", description = "本地用户ID", maxLength = 19, nullable = false)
    private long userId;

    /**
     * 三方平台标识（如：google、wechat、github、alipay、weibo、apple）
     */
    @ColumnMeta(columnName = "provider_code", dataType = "String", dataSize = 20, nullable = false)
    @Schema(title = "三方平台标识（如：google、wechat、github、alipay、weibo、apple）", description = "三方平台标识（如：google、wechat、github、alipay、weibo、apple）", maxLength = 20, nullable = false)
    private String providerCode;

    /**
     * 三方平台用户ID
     */
    @ColumnMeta(columnName = "open_id", dataType = "String", dataSize = 100, nullable = false)
    @Schema(title = "三方平台用户ID", description = "三方平台用户ID", maxLength = 100, nullable = false)
    private String openId;

    /**
     * 用户名
     */
    @ColumnMeta(columnName = "username", dataType = "String", dataSize = 200, nullable = true)
    @Schema(title = "用户名", description = "用户名", maxLength = 200, nullable = true)
    private String username;

    /**
     *
     */
    @ColumnMeta(columnName = "avatar", dataType = "String", dataSize = 100, nullable = true)
    @Schema(title = "", description = "", maxLength = 100, nullable = true)
    private String avatar;

    /**
     * 性别
     */
    @ColumnMeta(columnName = "gender", dataType = "String", dataSize = 20, nullable = true)
    @Schema(title = "性别", description = "性别", maxLength = 20, nullable = true)
    private String gender;

    /**
     * email地址
     */
    @ColumnMeta(columnName = "email", dataType = "String", dataSize = 100, nullable = true)
    @Schema(title = "email地址", description = "email地址", maxLength = 100, nullable = true)
    private String email;

    /**
     * 电话号码
     */
    @ColumnMeta(columnName = "phone", dataType = "String", dataSize = 20, nullable = true)
    @Schema(title = "电话号码", description = "电话号码", maxLength = 20, nullable = true)
    private String phone;

    /**
     * 地区
     */
    @ColumnMeta(columnName = "area", dataType = "String", dataSize = 100, nullable = true)
    @Schema(title = "地区", description = "地区", maxLength = 100, nullable = true)
    private String area;

    /**
     * 地址
     */
    @ColumnMeta(columnName = "address", dataType = "String", dataSize = 200, nullable = true)
    @Schema(title = "地址", description = "地址", maxLength = 200, nullable = true)
    private String address;

    /**
     * 用户信息
     */
    @ColumnMeta(columnName = "raw_user_info", dataType = "String", dataSize = 1073741824, nullable = true)
    @Schema(title = "用户信息", description = "用户信息", maxLength = 1073741824, nullable = true)
    @JsonRawValue(value = false)
    private String rawUserInfo;

    /**
     * 最近登录时间
     */
    @ColumnMeta(columnName = "last_login_date", dataType = "java.util.Date", dataSize = 23, nullable = true)
    @Schema(title = "最近登录时间", description = "最近登录时间", maxLength = 23, nullable = true)
    private java.util.Date lastLoginDate;

    /**
     * 更新时间
     */
    @ColumnMeta(columnName = "last_login_ip", dataType = "String", dataSize = 50, nullable = true)
    @Schema(title = "更新时间", description = "更新时间", maxLength = 50, nullable = true)
    private String lastLoginIp;

    /**
     * 登录次数
     */
    @ColumnMeta(columnName = "logon_count", dataType = "int", dataSize = 10, nullable = true)
    @Schema(title = "登录次数", description = "登录次数", maxLength = 10, nullable = true)
    private int logonCount;

    /**
     * 建立时间
     */
    @ColumnMeta(columnName = "create_date", dataType = "java.util.Date", dataSize = 23, nullable = false)
    @Schema(title = "建立时间", description = "建立时间", maxLength = 23, nullable = false)
    private java.util.Date createDate;

    /**
     * 修改时间
     */
    @ColumnMeta(columnName = "modify_date", dataType = "java.util.Date", dataSize = 23, nullable = true)
    @Schema(title = "修改时间", description = "修改时间", maxLength = 23, nullable = true)
    private java.util.Date modifyDate;

    /**
     * 创建时间
     */
    @ColumnMeta(columnName = "bind_date", dataType = "java.util.Date", dataSize = 23, nullable = true)
    @Schema(title = "创建时间", description = "创建时间", maxLength = 23, nullable = true)
    private java.util.Date bindDate;

    /**
     * 修改日期
     */
    @ColumnMeta(columnName = "unbind_date", dataType = "java.util.Date", dataSize = 23, nullable = true)
    @Schema(title = "修改日期", description = "修改日期", maxLength = 23, nullable = true)
    private java.util.Date unbindDate;

    /**
     * 状态
     */
    @ColumnMeta(columnName = "state", dataType = "int", dataSize = 10, nullable = false)
    @Schema(title = "状态", description = "状态", maxLength = 10, nullable = false)
    private int state;
}
```
#### 4.2 OAuthTokenResponse 响应类（忽略get/set）
```java

/**
 * oauth登录Token
 */
@Schema(title = "oauth登录Token", description = "oauth登录Token")
public class OAuthTokenResponse {

    /**
     * oauth登录信息
     */
    @Schema(title = "oauth登录信息", description = "oauth登录信息")
    private MscOauthInfo mscOauthInfo;

    /**
     * token信息
     */
    @Schema(title = "token信息", description = "token信息")
    private TokenResponse tokenResponse;
}
```

### 5. OAuth2登录流程集成

#### 5.1 MscOAuth2Controller 控制器类（用于登录前操作）

```java

/**
 * oauth2接口。
 */
@Tag(name = "oauth2接口")
@RestController
@RequestMapping("/oauth2")
public class MscOAuth2Controller {

    /**
     * 授权码提供商代码列表。
     *
     * @return
     */
    @GetMapping("/providerCodes")
    @Operation(summary = "授权码提供商代码列表", description = "授权码提供商代码列表")
    public ResponseData<Set<String>> providerCodes() {
        return ResponseData.success(OAuth2ClientHelper.getConfigMap().keySet());
    }

    /**
     * 构造OAUTH URL
     *
     * @param providerCode
     * @return
     */
    @GetMapping("/buildAuthUrl")
    @Operation(summary = "构造OAUTH URL", description = "构造OAUTH URL")
    public ResponseData<String> buildAuthUrl(@Parameter(description = "授权码提供商") String providerCode) {
        return OAuth2ClientHelper.buildAuthUrl(providerCode, null);
    }


    /**
     * 构造OAUTH二维码
     *
     * @param providerCode
     * @return
     */
    @GetMapping("/buildQrCode")
    @Operation(summary = "构造OAUTH二维码", description = "构造OAUTH二维码")
    public ResponseData<String> buildQrCode(@Parameter(description = "授权码提供商") String providerCode) {
        return OAuth2ClientHelper.buildQrCode(providerCode);
    }

    /**
     * OAUTH二维码跳转链接。
     *
     * @param authStateId
     * @return
     */
    @GetMapping("/qrcode/{authStateId}")
    @Operation(summary = "OAUTH二维码跳转链接", description = "OAUTH二维码跳转链接")
    public void qrcodeJump(HttpServletResponse response, @Parameter(description = "授权码提供商") @PathVariable String authStateId) throws IOException {
        ResponseData<String> jumpResponse = OAuth2ClientHelper.buildAuthUrl(null, authStateId);
        if (jumpResponse.isNotSuccess()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, jumpResponse.getMsg());
        }
        response.sendRedirect(jumpResponse.getData());
    }


    /**
     * 检查验证状态。
     *
     * @param providerCode
     * @param authCode
     * @param authStateId
     * @return
     */
    @GetMapping("/checkState")
    @Operation(summary = "检查验证状态", description = "检查验证状态")
    public ResponseData<String> checkState(@Parameter(description = "授权码提供商") String providerCode, @Parameter(description = "授权码") String authCode, @Parameter(description = "授权码状态") String authStateId) {
        return ResponseData.success(OAuth2ClientHelper.getAuthState(authStateId).toString());
    }


    /**
     * 扫码后获取Token。
     * 扫码场景下可用，其它场景不可用。
     *
     * @param request
     * @param authStateId
     * @return
     */
    @GetMapping("/getToken")
    @Operation(summary = "扫码后获取Token", description = "扫码后获取Token")
    public ResponseData<OAuthTokenResponse> getToken(HttpServletRequest request, @Parameter(description = "授权码状态") String authStateId) {
        String userIp = AuthServiceHelper.getRemoteIp(request);
        //检测ip是否被封掉。
        ResponseData checkLimitResponse = MfaFusionHelper.checkIpErrorLimit(userIp);
        if (checkLimitResponse.isError()) {
            return checkLimitResponse;
        }
        ResponseData<OAuthTokenResponse> responseData = MscAuthHelper.oauthToken(userIp, authStateId);
        if (responseData.isError()) {
            MfaFusionHelper.incrementIpErrorTimes(userIp, responseData.getMsg());
        }
        return responseData;
    }

    /**
     * oauth登录操作。
     *
     * @param request
     * @param loginAgent
     * @param saasId
     * @param userType
     * @param providerCode
     * @param authCode
     * @param authStateId
     * @return
     */
    @PostMapping("/login")
    @Operation(summary = "登录操作", description = "登录操作")
    public ResponseData<OAuthTokenResponse> login(HttpServletRequest request, @Parameter(description = "登录代理") String loginAgent, @Parameter(description = "saasId") long saasId, @Parameter(description = "用户类型") int userType, @Parameter(description = "授权码提供商，不传的时候使用authState自动判定") String providerCode, @Parameter(description = "授权码") String authCode, @Parameter(description = "授权码状态") String authStateId, @Parameter(description = "扩展参数") Map<String, String> extParam) {
        // 检测登录代理。
        if (StringUtils.isBlank(loginAgent)) {
            return ResponseData.errorCode(AuthCenterResponseCode.LOGIN_AGENT_LOST_ERROR);
        }
        //校验用户类型是否合法。
        if (!UserType.checkTypeValid(userType)) {
            return ResponseData.errorCode(AuthCenterResponseCode.USER_TYPE_ERROR);
        }
        //针对saas和mch用户获取saasId信息。
        if (userType >= UserType.SAAS.getValue() && userType <= UserType.MCH.getValue()) {
            //如果saasId依旧没有数值，则查库。
            if (saasId < 0) {
                saasId = MscAccountHelper.getSaasIdByHost(HttpRequestTools.getDomainByUrl(request.getServerName()));
            }
            //如果saasId依旧没有数值，则返回错误。
            if (saasId < 0) {
                return ResponseData.errorCode(AuthCenterResponseCode.SAAS_ID_ERROR);
            }
        } else {
            //管理类型使用saasId=0。
            saasId = 0;
        }

        // 初始化mscLoginLog。
        MscLoginLog mscLoginLog = MscLoginLogHelper.initLoginLog(request, loginAgent, LoginType.OAUTH_LOGIN.getValue(), saasId, userType, "OAUTH_CODE");

        //执行登录操作。
        ResponseData<OAuthTokenResponse> loginResponse = MscAuthHelper.oauthLogin(mscLoginLog, providerCode, authCode, authStateId, extParam);
        if (loginResponse.isError()) {
            MfaFusionHelper.incrementIpErrorTimes(mscLoginLog.getUserIp(), loginResponse.getMsg());
        }
        //记录登录日志
        MscLoginLogHelper.finishOAuthLoginLog(mscLoginLog, loginResponse);

        return loginResponse;
    }

}

```
#### 5.2 MscUserController 控制器类(用于登录后的账号绑定、解绑、登录的关键操作)

```java
/**
 * 登录用户Controller。
 */
@RestController
@RequestMapping("/user")
@Tag(name = "已登录用户接口", description = "已登录用户接口")
public class MscUserController {
    /**
     * OAuth绑定
     *
     * @return
     */
    @Operation(summary = "OAuth绑定", description = "OAuth绑定")
    @MscPermDeclare(auth = AuthType.USER, log = ActionLog.CRIT)
    @PutMapping("/bindOAuth")
    public ResponseData bindOAuth(HttpServletRequest request, @Parameter(description = "oauth供应商") @RequestParam String providerCode, @Parameter(description = "openId") @RequestParam String openId) {
        AuthServiceHelper.logRef(MscUser.class, AuthServiceHelper.getUserId());
        String userIp = AuthServiceHelper.getRemoteIp(request);
        MscUser mscUser = MscAccountHelper.loadValidMscUser(AuthServiceHelper.getSaasId(), AuthServiceHelper.getUserType(), AuthServiceHelper.getMchId(), AuthServiceHelper.getUserId()).getData();
        if (Objects.isNull(mscUser)) {
            return ResponseData.errorCode(AuthCenterResponseCode.USER_NOT_FOUND_ERROR);
        }
        return MscAuthHelper.oauthBind(mscUser.getSaasId(), mscUser.getId(), providerCode, openId);
    }

    /**
     * OAuth解绑
     *
     * @return
     */
    @Operation(summary = "OAuth解绑", description = "OAuth解绑")
    @MscPermDeclare(auth = AuthType.USER, log = ActionLog.CRIT)
    @PutMapping("/unbindOAuth")
    public ResponseData unbindOAuth(HttpServletRequest request, @Parameter(description = "oauth供应商") @RequestParam String providerCode) {
        AuthServiceHelper.logRef(MscUser.class, AuthServiceHelper.getUserId());
        String userIp = AuthServiceHelper.getRemoteIp(request);
        MscUser mscUser = MscAccountHelper.loadValidMscUser(AuthServiceHelper.getSaasId(), AuthServiceHelper.getUserType(), AuthServiceHelper.getMchId(), AuthServiceHelper.getUserId()).getData();
        if (Objects.isNull(mscUser)) {
            return ResponseData.errorCode(AuthCenterResponseCode.USER_NOT_FOUND_ERROR);
        }
        return MscAuthHelper.oauthUnbind(mscUser.getSaasId(), mscUser.getId(), providerCode);
    }

    /**
     * OAuth绑定列表.
     *
     * @return
     */
    @GetMapping("/listOAuthInfo")
    @Operation(summary = "OAuth绑定列表", description = "OAuth绑定列表")
    @MscPermDeclare(auth = AuthType.USER, log = ActionLog.NONE)
    public ResponseData<DataList<MscOauthInfo>> listOAuthInfo() {
        //rpc以下用户，无权限。
        if (AuthServiceHelper.getUserType() <= UserType.RPC.getValue()) {
            return null;
        }
        MscOauthInfoQueryParam queryParam = new MscOauthInfoQueryParam();
        //钉死关键参数
        queryParam.setSaasId(AuthServiceHelper.getSaasId());
        queryParam.setUserId(AuthServiceHelper.getUserId());
        queryParam.setState(CommonState.ENABLED.getValue());
        return dao.list(MscOauthInfo.class, queryParam);
    }
}

```

#### 5.3 MscAuthHelper 帮助类(用于账号绑定、解绑、登录的帮助操作)

```java
/**
 * MscAuth帮助类。
 */
public class MscAuthHelper {

    /**
     * 绑定第三方登录账号。
     *
     * @param saasId
     * @param userId
     * @param providerCode
     * @param openId
     * @return
     */
    public static ResponseData<MscOauthInfo> oauthBind(long saasId, long userId, String providerCode, String openId) {
        // 检查是否已经绑定？
        ResponseData<DataList<MscOauthInfo>> checkResponse = dao.list(MscOauthInfo.class, "select * from msc_oauth_info where saas_id=? and provider_code=? and state=1", new Object[]{saasId, providerCode}, 0, 0, false);
        if (checkResponse.isNotSuccess()) {
            return checkResponse.raw();
        }
        DataList<MscOauthInfo> oauthInfoList = checkResponse.getData();
        if (oauthInfoList.stream().anyMatch(x -> x.getUserId() == userId)) {
            return ResponseData.errorCode(AuthCenterResponseCode.OAUTH_ACCOUNT_HAS_BIND_ERROR);
        }

        MscOauthInfo oauthInfo = oauthInfoList.stream().filter(x -> x.getOpenId().equals(openId) && x.getUserId() == 0).findFirst().orElse(null);
        if (oauthInfo == null) {
            return ResponseData.errorCode(AuthCenterResponseCode.OAUTH_ACCOUNT_NOT_FOUND_ERROR);
        }
        oauthInfo.setUserId(userId);
        oauthInfo.setBindDate(SystemClock.nowDate());
        oauthInfo.setState(1);
        return dao.save(oauthInfo);
    }

    /**
     * 解绑第三方登录账号。
     *
     * @param saasId
     * @param userId
     * @param providerCode
     */
    public static ResponseData<Integer> oauthUnbind(long saasId, long userId, String providerCode) {
        return dao.executeCommand("update msc_oauth_info set status=0,unbind_date=? where saas_id=? and user_id=? and provider_code=?", new Object[]{SystemClock.nowDate(), saasId, userId, providerCode});
    }

    /**
     * oauth登录。
     *
     * @param mscLoginLog
     * @param providerCode
     * @param authCode
     * @param authStateId
     * @return
     */
    public static ResponseData<OAuthTokenResponse> oauthLogin(MscLoginLog mscLoginLog, String providerCode, String authCode, String authStateId, Map<String, String> extParam) {
        // 获取访问令牌
        ResponseData<OAuth2Token> tokenResponseData = OAuth2ClientHelper.getToken(providerCode, authCode, authStateId, extParam);
        if (tokenResponseData.isNotSuccess()) {
            return tokenResponseData.raw();
        }
        OAuth2Token token = tokenResponseData.getData();
        // 用户ID
        String openId = token.getOpenId();
        // token转为用户信息（有idToken的情况下）
        OAuth2UserInfo oAuth2UserInfo = token.toUserInfo();
        //请求用户信息
        ResponseData<OAuth2UserInfo> userInfoResponseData = OAuth2ClientHelper.getUserInfo(providerCode, token);
        //如果用户信息接口不支持，则使用token参数。
        if (!OAuth2ClientResponseCode.NOT_SUPPORTED.getFullCode().equals(userInfoResponseData.getCode())) {
            if (userInfoResponseData.isNotSuccess()) {
                return userInfoResponseData.raw();
            }else{
                oAuth2UserInfo = userInfoResponseData.getData();
            }
        }
        openId = oAuth2UserInfo.getOpenId();
        // 再次检查openId。
        if (StringUtils.isBlank(openId)) {
            return ResponseData.warnCode(AuthCenterResponseCode.OAUTH_ACCOUNT_ID_ERROR);
        }
        // 查询oauth绑定信息
        ResponseData<MscOauthInfo> oauthInfoResponseData = dao.queryForSingleObject(MscOauthInfo.class, "select * from msc_oauth_info where saas_id=? and provider_code=? and open_id=? and state=1", new Object[]{mscLoginLog.getSaasId(), providerCode, openId});
        if (oauthInfoResponseData.isNotSuccess()) {
            return oauthInfoResponseData.raw();
        }
        MscOauthInfo oauthInfo = oauthInfoResponseData.getData();
        if (oauthInfo == null) {
            oauthInfo = new MscOauthInfo();
            oauthInfo.setSaasId(mscLoginLog.getSaasId());
            oauthInfo.setUserId(0);
            oauthInfo.setProviderCode(providerCode);
            oauthInfo.setOpenId(openId);
            oauthInfo.setCreateDate(SystemClock.nowDate());
            oauthInfo.setState(1);
        }
        // 更新用户信息,必须有数值才改变。
        if (StringUtils.isNotBlank(oAuth2UserInfo.getUnionId())) {
            oauthInfo.setUnionId(oAuth2UserInfo.getUnionId());
        }
        if (StringUtils.isNotBlank(oAuth2UserInfo.getUsername())) {
            oauthInfo.setUsername(oAuth2UserInfo.getUsername());
        }
        if (StringUtils.isNotBlank(oAuth2UserInfo.getEmail())) {
            oauthInfo.setEmail(oAuth2UserInfo.getEmail());
        }
        if (StringUtils.isNotBlank(oAuth2UserInfo.getPhone())) {
            oauthInfo.setPhone(oAuth2UserInfo.getPhone());
        }
        if (StringUtils.isNotBlank(oAuth2UserInfo.getAvatar())) {
            oauthInfo.setAvatar(oAuth2UserInfo.getAvatar());
        }
        if (StringUtils.isNotBlank(oAuth2UserInfo.getGender())) {
            oauthInfo.setGender(oAuth2UserInfo.getGender());
        }
        if (StringUtils.isNotBlank(oAuth2UserInfo.getArea())) {
            oauthInfo.setArea(oAuth2UserInfo.getArea());
        }
        if (StringUtils.isNotBlank(oAuth2UserInfo.getAddress())) {
            oauthInfo.setAddress(oAuth2UserInfo.getAddress());
        }
        if (oAuth2UserInfo.getRawParams() != null) {
            oauthInfo.setRawUserInfo(JsonUtils.toString(oAuth2UserInfo.getRawParams()));
        }
        // 如果有更新，则更新时间。
        if (oauthInfo.GET_UPDATED_INFO() != null && oauthInfo.getId() > 0) {
            oauthInfo.setModifyDate(SystemClock.nowDate());
        }
        oauthInfo.setLastLoginDate(SystemClock.nowDate());
        oauthInfo.setLastLoginIp(mscLoginLog.getUserIp());
        oauthInfo.setLogonCount(oauthInfo.getLogonCount() + 1);
        if (oauthInfo.getId() == 0) {
            oauthInfo.setId(dao.getSequenceId(MscOauthInfo.class));
            dao.save(oauthInfo);
        } else {
            dao.update(oauthInfo);
        }
        // 如果没有绑定，则返回OAUTH_NOT_BIND_ERROR警告，提示前端登录绑定。
        if (oauthInfo.getUserId() == 0) {
            return ResponseData.warn(new OAuthTokenResponse(oauthInfo, null), AuthCenterResponseCode.OAUTH_ACCOUNT_NOT_BIND_ERROR);
        }

        //拉取用户信息,组装token返回结果。
        MscUser mscUser = dao.load(MscUser.class, oauthInfo.getUserId()).getData();
        if (mscUser == null) {
            return ResponseData.errorCode(AuthCenterResponseCode.USER_NOT_FOUND_ERROR);
        }
        //更新用户登录IP.
        mscUser.setLastLogonIp(mscLoginLog.getUserIp());
        mscUser.setLastLogonType(mscLoginLog.getLoginType());
        //补充日志信息。
        mscLoginLog.setUserId(mscUser.getId());
        mscLoginLog.setSaasId(mscUser.getSaasId());
        mscLoginLog.setUserType(mscUser.getUserType());
        mscLoginLog.setGroupId(mscUser.getGroupId());
        mscLoginLog.setMchId(mscUser.getMchId());
        mscLoginLog.setUserName(mscUser.getUsername());
        mscLoginLog.setRealName(mscUser.getRealName());
        mscLoginLog.setNickName(mscUser.getNickName());

        //Token输出定义。
        TokenResponse tokenResponse = null;

        //检查双登问题。
        AuthTokenData doubleTokenData = checkDoubleLogin(mscLoginLog.getLoginAgent(), mscUser.getSaasId(), mscUser.getUserType(), mscUser.getId(), mscLoginLog.getUserIp(), true);

        //用户类型配置
        AuthCenterProperties.UserTypeConfig userTypeConfig = MscUserTypeConfigHelper.getUserTypeConfig(mscUser.getUserType());

        //检查初始密码状态。
        if (userTypeConfig.getPasswordConfig().checkInitReset(mscUser.getLastPasswdDate())) {
            return buildTempOAuthTokenResponse(mscLoginLog.getLoginType(), mscUser, oauthInfo, AuthCenterResponseCode.PASSWD_INIT_WARN);
        }

        //处理密码过期问题。
        if (userTypeConfig.getPasswordConfig().checkExpireReset(mscUser.getLastPasswdDate())) {
            return buildTempOAuthTokenResponse(mscLoginLog.getLoginType(), mscUser, oauthInfo, AuthCenterResponseCode.PASSWD_EXPIRE_WARN);
        }

        //处理MFA二次认证。
        int mfaTypeCount = userTypeConfig.getMfaTypes().size();
        MfaDeviceType currentMfaType = MfaDeviceType.valueOf(mscLoginLog.getLoginType());
        //先排除登录方式相同的情况，防止类似二次手机码登录，就没有意义了。
        if (currentMfaType != null && userTypeConfig.getMfaTypes().contains(currentMfaType)) {
            mfaTypeCount--;
        }
        if (mfaTypeCount > 0) {
            //如果没有MFA密钥，返回警告。前端引导进入签发页。
            if (userTypeConfig.getMfaTypes().contains(MfaDeviceType.TOTP_CODE) && StringUtils.isBlank(mscUser.getAuthSecret())) {
                return buildTempOAuthTokenResponse(mscLoginLog.getLoginType(), mscUser, oauthInfo, AuthCenterResponseCode.MFA_INIT_WARN);
            }
            return buildTempOAuthTokenResponse(mscLoginLog.getLoginType(), mscUser, oauthInfo, AuthCenterResponseCode.MFA_FORCE_WARN);
        }
        //此时说明一切OK，可以生成token。
        ResponseData<AuthTokenData> tokenDataResponse = buildAuthTokenData(mscLoginLog.getLoginType(), TokenType.COMMON, mscUser, mscUser.getLastLogonIp());
        if (tokenDataResponse.isNotSuccess()) {
            return tokenDataResponse.raw();
        }
        //生成完整token。
        tokenResponse = MscTokenService.genAllTokenResponse(mscUser, tokenDataResponse.getData());
        //设置双登提示。
        if (doubleTokenData != null) {
            tokenResponse.setLoginNotice(String.format(AuthCenterResponseCode.LOGIN_DOUBLE_WARN.getMessage(), doubleTokenData.getUserIp(), DateUtils.dateToString(new Date(doubleTokenData.getCreateAt()), DateUtils.DATE_TIME)));
        }
        OAuthTokenResponse authTokenResponse = new OAuthTokenResponse(oauthInfo, tokenResponse);

        // 获取授权id信息。
        OAuth2StateId stateIdInfo = OAuth2StateId.parse(authStateId);
        // 如果是qr扫码的，需要保存Token信息到redis中。
        if (OAuth2ClientAuthType.QRCODE.name().equalsIgnoreCase(stateIdInfo.getAuthType())) {
            GlobalCache.put(OAUTH_TOKEN_KEY, authStateId, authTokenResponse, 60 * 1000);
        } else {
            //删除验证状态。
            OAuth2ClientHelper.invalidateAuthState(authStateId);
        }
        //登录成功。
        return ResponseData.success(new OAuthTokenResponse(oauthInfo, tokenResponse), AuthCenterResponseCode.LOGIN_SUCCESS);
    }

    /**
     * 扫码情况下，通过authStateId获取Token信息。
     *
     * @param authStateId
     * @return
     */
    public static ResponseData<OAuthTokenResponse> oauthToken(String userIp, String authStateId) {
        //获取stateId信息，验证是否是扫码登录。
        OAuth2StateId stateIdInfo = OAuth2StateId.parse(authStateId);
        if (!OAuth2ClientAuthType.QRCODE.name().equalsIgnoreCase(stateIdInfo.getAuthType())) {
            return ResponseData.errorCode(AuthCenterResponseCode.OAUTH_TOKEN_NOT_FOUND_ERROR);
        }
        //获取Token信息。
        CacheValueWrapper<OAuthTokenResponse> oauthTokenResponse = GlobalCache.get(OAUTH_TOKEN_KEY, authStateId, OAuthTokenResponse.class);
        if (oauthTokenResponse == null) {
            return ResponseData.errorCode(AuthCenterResponseCode.OAUTH_TOKEN_NOT_FOUND_ERROR);
        }
        //删除Token信息。
        GlobalCache.invalidate(OAUTH_TOKEN_KEY, authStateId);
        //删除验证状态。
        OAuth2ClientHelper.invalidateAuthState(authStateId);
        return ResponseData.success(oauthTokenResponse.getValue());
    }



```

### 6. 自定义Provider

如果需要集成新的第三方登录平台，可以通过实现`OAuth2Provider`接口来创建自定义Provider：

```java
import uw.oauth2.client.conf.OAuth2ClientProperties;
import uw.oauth2.client.vo.OAuth2Token;
import uw.oauth2.client.vo.OAuth2UserInfo;
import uw.oauth2.client.provider.AbstractOAuth2Provider;
import uw.httpclient.UwHttpClient;

public class CustomOAuth2Provider extends AbstractOAuth2Provider {

    public CustomOAuth2Provider(String providerCode, OAuth2ClientProperties.ProviderConfig providerConfig, String redirectUri, String qrcodeUri) {
        super(providerCode, providerConfig, redirectUri, qrcodeUri);
    }

    @Override
    public OAuth2UserInfo getUserInfo(OAuth2Token token) {
        // 实现获取用户信息的逻辑
        // ...
    }

    @Override
    protected OAuth2Token parseTokenResponse(String responseBody) {
        // 实现解析令牌响应的逻辑
        // ...
    }

    @Override
    protected OAuth2UserInfo parseUserInfoResponse(String responseBody) {
        // 实现解析用户信息响应的逻辑
        // ...
    }
}
```

然后将自定义Provider注册到OAuth2Service中：

```java
OAuth2ClientHelper.registerProvider(new CustomOAuth2Provider("custom", config, redirectUri, qrcodeUri));
```

### 7. 前端集成说明

#### 7.1 页面鉴权流程
1. 调用`/oauth2/buildAuthUrl`接口，传入`providerCode`参数，返回授权页面URL，引导用户点击授权。
2. 用户执行授权，跳转到授权页面，完成授权操作。
3. 在跳转页面获取授权码`code`,`state`参数，并调用`/oauth2/login`接口，传入`authCode`,`authStateId`参数进行登录，其它参数放入extParam中传入。
3.1 如果登录成功，则返回登录成功信息，包含token信息，直接进行登录跳转。
3.2 如果登录失败，则返回登录失败信息，提示用户重新授权。
3.3 如果登录警告Code为`OAUTH_NOT_BIND_ERROR`，将OAuthUserInfo保存到LocalStorage中，引导客户登录后绑定。
3.3.1 客户登录成功后，将LocalStorage中的OAuthUserInfo信息传入`/user/bindOAuth`接口进行绑定。
3.3.2 如果返回值中的authType为"qrcode"，则直接返回给用户登录成功信息即可，不需要跳转。

#### 7.2 页面扫码流程
1. 调用`/oauth2/buildQrcode`接口，传入`providerCode`参数，渲染成二维码，引导用户扫码。
2. 用户执行授权，跳转到授权页面，完成授权操作，此时应注意用户手机端走的是7.1流程。
3. 前端程序间隔3S调用`/oauth2/checkState`接口，传入`authStateId`参数，监听扫码信息。
3.1 如果返回SCANNED，说明用户扫码成功，可在UI界面提示进度信息。
3.2 如果返回EXPIRED, 调用`/oauth2/buildQrcode`接口，重新生成二维码。
3.3 如果返回FAILD信息，说明用户扫码失败，可在UI界面提示错误信息,并提示客户重新生成二维码。
3.4 如果返回CONFIRMED，说明用户授权成功，调用`/oauth2/getToken`接口，获取token信息。
3.4.1 如果登录警告Code为`OAUTH_NOT_BIND_ERROR`，将OAuthUserInfo保存到LocalStorage中，引导客户登录后绑定。
3.4.2 如果登录成功，返回登录成功信息，包含token信息，直接进行登录跳转。

#### 7.3 绑定管理
1. 绑定列表：调用`/user/listOAuthInfo`接口，返回绑定信息列表，提供客户解绑操作按钮。
2. 解绑操作：调用`/user/unbindOAuth`接口，传入`providerCode`参数，执行解绑操作。
3. 新增绑定：用户点击页面授权后，引流用户鉴权成功后，调用`/user/bindOAuth`接口进行绑定。