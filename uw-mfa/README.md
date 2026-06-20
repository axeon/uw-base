[TOC]

# 项目说明

**uw-mfa**（Multi-Factor Authentication）是一个融合 **IP 限制 / Captcha / 设备验证码 / TOTP** 的多重认证基础库，对外通过 `MfaFusionHelper` 统一入口提供静态 API。

核心约定：
- **三态响应**：`success` / `warn`（需验证码提示）/ `error`（已屏蔽）。
- **白名单豁免**：白名单内 IP 全程豁免错误限制（不计入错误次数、不触发限制）。
- **一次性消费**：Captcha 答案与设备验证码校验时通过 `getAndDelete` 消费，防止重放。

# 基础配置

## Maven 引用

```xml
<dependency>
    <groupId>com.umtone</groupId>
    <artifactId>uw-mfa</artifactId>
    <version>${uw-mfa.version}</version>
</dependency>
```

> 自动装配：引入依赖后通过 `UwMfaAutoConfiguration` 自动注册 `mfaRedisTemplate`（独立 Redis 连接池）与 4 个 Helper Bean。

## 配置项

配置前缀：`uw.mfa`。

```yaml
uw:
  mfa:
    # ===== IP 限制 =====
    # IP白名单，支持CIDR格式，白名单IP全程豁免错误限制
    ip-white-list: "127.0.0.1,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16,::1/128,fe80::/10,FC00::/7"
    ip-limit-seconds: 600          # 错误计数过期时间（秒）
    ip-limit-warn-times: 3         # 警告阈值，达到后需验证码
    ip-limit-error-times: 10       # 屏蔽阈值，达到后拒绝请求

    # ===== Captcha =====
    captcha-strategies: "StringCaptchaStrategy,CalculateCaptchaStrategy,SlidePuzzleCaptchaStrategy,ClickWordCaptchaStrategy,RotatePuzzleCaptchaStrategy"
    captcha-expired-seconds: 180   # Captcha答案过期时间（秒）
    captcha-send-limit-seconds: 60 # 发送频率限制时间（秒）
    captcha-send-limit-times: 10   # 发送频率限制次数

    # ===== 设备验证码 =====
    device-code-expired-seconds: 300      # 验证码过期时间（秒）
    device-code-default-length: 6         # 默认验证码长度
    device-code-send-limit-seconds: 1800  # 发送频率限制时间（秒）
    device-code-send-limit-times: 10      # 发送频率限制次数
    device-code-verify-limit-seconds: 600 # 校验错误限制时间（秒）
    device-code-verify-error-times: 10    # 校验错误限制次数
    device-notify-subject: "设备验证码"
    device-notify-content: "设备验证码[$DEVICE_CODE$]，$EXPIRE_MINUTES$分钟后过期，如非本人操作，请忽略此信息。"
    device-notify-mobile-api: "http://saas-base/rpc/msg/sendSms"  # 短信发送API
    device-notify-email-api: "http://saas-base/rpc/msg/sendMail"  # 邮件发送API

    # ===== TOTP =====
    totp-algorithm: SHA1            # 算法：SHA1/SHA256/SHA512（默认SHA1兼容性最佳）
    totp-secret-length: 32          # 密钥长度（Base32字符数）
    totp-code-length: 6             # 验证码位数
    totp-time-period: 30            # 时间窗口（秒）
    totp-time-period-discrepancy: 2 # 允许的时间窗口偏移量
    totp-verify-limit-seconds: 600  # 校验错误限制时间（秒）
    totp-verify-error-times: 10     # 校验错误限制次数
    totp-gen-qr: true               # 是否生成二维码PNG
    totp-qr-size: 350               # 二维码尺寸（像素）
    totp-issuer: "uw-mfa"           # 默认签发人

    # ===== 独立Redis配置（继承Spring Boot Redis属性）=====
    redis:
      database: 0
      host: 127.0.0.1
      port: 6379
```

### 配置项一览表

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `ipWhiteList` | String | 本机+私网CIDR | IP白名单，逗号分隔，支持CIDR |
| `ipLimitSeconds` | long | 600 | 错误计数过期时间（秒） |
| `ipLimitWarnTimes` | long | 3 | 警告阈值次数 |
| `ipLimitErrorTimes` | long | 10 | 屏蔽阈值次数 |
| `captchaStrategies` | String | 全部5种 | 逗号分隔的策略类名 |
| `captchaExpiredSeconds` | long | 180 | Captcha答案过期时间（秒） |
| `captchaSendLimitSeconds` | long | 60 | Captcha发送频率限制时间（秒） |
| `captchaSendLimitTimes` | long | 10 | Captcha发送频率限制次数 |
| `deviceCodeExpiredSeconds` | long | 300 | 设备验证码过期时间（秒） |
| `deviceCodeDefaultLength` | int | 6 | 设备验证码默认长度 |
| `deviceCodeSendLimitSeconds` | long | 1800 | 设备码发送频率限制时间（秒） |
| `deviceCodeSendLimitTimes` | long | 10 | 设备码发送频率限制次数 |
| `deviceCodeVerifyLimitSeconds` | long | 600 | 设备码校验错误限制时间（秒） |
| `deviceCodeVerifyErrorTimes` | long | 10 | 设备码校验错误限制次数 |
| `deviceNotifySubject` | String | "设备验证码" | 邮件通知主题 |
| `deviceNotifyContent` | String | 含占位符 | 通知内容模板，支持 `$DEVICE_CODE$` / `$EXPIRE_MINUTES$` |
| `deviceNotifyMobileApi` | String | saas-base短信API | 短信发送RPC地址 |
| `deviceNotifyEmailApi` | String | saas-base邮件API | 邮件发送RPC地址 |
| `totpAlgorithm` | HmacAlgorithm | SHA1 | TOTP哈希算法 |
| `totpSecretLength` | int | 32 | 密钥长度（Base32字符数） |
| `totpCodeLength` | int | 6 | 验证码位数 |
| `totpTimePeriod` | int | 30 | 时间窗口（秒） |
| `totpTimePeriodDiscrepancy` | int | 2 | 允许的时间窗口偏移量 |
| `totpVerifyLimitSeconds` | long | 600 | TOTP校验错误限制时间（秒） |
| `totpVerifyErrorTimes` | long | 10 | TOTP校验错误限制次数 |
| `totpGenQr` | boolean | true | 是否生成二维码PNG |
| `totpQrSize` | int | 350 | 二维码尺寸（像素） |
| `totpIssuer` | String | "uw-mfa" | 默认签发人 |

# 使用参考

`MfaFusionHelper` 为对外统一入口，所有方法均为静态方法。

## IP 限制

| 方法 | 说明 |
|------|------|
| `checkIpWhiteList(ip)` | 检查IP是否在白名单 |
| `checkIpErrorLimit(ip)` | 检查IP错误限制，返回 success/warn/error |
| `incrementIpErrorTimes(ip, remark)` | 递增IP错误次数（白名单豁免） |
| `clearIpErrorLimit(ip)` | 清除IP错误限制 |
| `getIpErrorLimitList()` | 获取IP错误限制列表 |
| `countMfaInfo()` | 统计MFA限制信息条数 |

## Captcha

| 方法 | 说明 |
|------|------|
| `generateCaptcha(userIp, captchaId)` | 生成Captcha，warn状态才返回 |
| `verifyCaptcha(userIp, captchaId, captchaSign)` | 验证Captcha（一次性消费） |
| `getCaptchaSendLimitList()` | 获取发送限制列表 |
| `clearCaptchaSendLimit(ip)` | 清除发送限制 |

## 设备验证码

| 方法 | 说明 |
|------|------|
| `sendDeviceCode(userIp, saasId, deviceType, deviceId, captchaId, captchaSign)` | 发送设备验证码（默认长度模板） |
| `sendDeviceCode(..., codeLen)` | 指定验证码长度 |
| `sendDeviceCode(..., codeLen, notifySubject, notifyContent)` | 完整参数 |
| `verifyDeviceCode(deviceType, deviceId, deviceCode)` | 仅校验验证码 |
| `verifyDeviceCode(userIp, deviceType, deviceId, deviceCode)` | 校验验证码+IP |
| `verifyDeviceCode(userIp, ..., captchaId, captchaSign)` | 校验验证码+IP+Captcha |
| `getDeviceCodeSendLimitList()` / `getDeviceCodeVerifyLimitList()` | 获取限制列表 |
| `clearDeviceCodeSendLimit(ip)` / `clearDeviceCodeVerifyLimit(deviceId)` | 清除限制 |

## TOTP

| 方法 | 说明 |
|------|------|
| `issueTotpSecret(label)` | 生成TOTP密钥（默认签发人） |
| `issueTotpSecret(label, issuer, qrSize)` | 生成TOTP密钥（自定义） |
| `verifyTotpCode(userInfo, totpSecret, totpCode)` | 仅校验 |
| `verifyTotpCode(userIp, userInfo, totpSecret, totpCode)` | 校验+IP |
| `verifyTotpCode(userIp, ..., captchaId, captchaSign)` | 校验+IP+Captcha |
| `generateRecoveryCode(amount)` | 生成恢复码 |
| `getTotpVerifyLimitList()` / `clearTotpVerifyLimit(userInfo)` | 限制管理 |

## 完整示例

```java
public class LoginHelper {

    /**
     * 完整登录流程：IP检测 + Captcha + 密码校验。
     */
    public static ResponseData login(String username, String password, String ip,
                                     String captchaId, String captchaSign) {
        // 1. 检查IP限制
        ResponseData ipCheck = MfaFusionHelper.checkIpErrorLimit(ip);
        if (ipCheck.isError()) {
            return ipCheck; // 已屏蔽
        }
        // 2. warn状态需校验Captcha
        if (ipCheck.isWarn()) {
            ResponseData captchaCheck = MfaFusionHelper.verifyCaptcha(ip, captchaId, captchaSign);
            if (captchaCheck.isNotSuccess()) {
                return captchaCheck;
            }
        }
        // 3. 密码校验
        User user = UserHelper.verifyPassword(username, password);
        if (user == null) {
            MfaFusionHelper.incrementIpErrorTimes(ip, "密码错误");
            return ResponseData.errorMsg("用户名或密码错误");
        }
        // 4. 登录成功，清除IP限制
        MfaFusionHelper.clearIpErrorLimit(ip);
        return ResponseData.success(user);
    }

    /** 发送短信验证码（自动IP检测 + Captcha）。 */
    public static ResponseData sendSmsCode(String mobile, String ip,
                                           String captchaId, String captchaSign) {
        return MfaFusionHelper.sendDeviceCode(ip, 1001L,
                MfaDeviceType.MOBILE_CODE.getValue(), mobile, captchaId, captchaSign);
    }

    /** 校验短信验证码（同时验证IP）。 */
    public static ResponseData verifySmsCode(String mobile, String code, String ip) {
        return MfaFusionHelper.verifyDeviceCode(ip,
                MfaDeviceType.MOBILE_CODE.getValue(), mobile, code);
    }

    /** 生成TOTP密钥（绑定Google Authenticator）。 */
    public static ResponseData<TotpSecretData> bindTotp(Long userId) {
        return MfaFusionHelper.issueTotpSecret("user:" + userId, "MyApp", 300);
    }

    /** 校验TOTP验证码（同时验证IP）。 */
    public static ResponseData verifyTotp(Long userId, String totpSecret, String totpCode, String ip) {
        return MfaFusionHelper.verifyTotpCode(ip, "user:" + userId, totpSecret, totpCode);
    }
}
```

# Captcha 策略说明

内置 5 种 Captcha 策略，生成时随机选择：

| 策略类 | 说明 | 人类操作阈值 |
|--------|------|-------------|
| `StringCaptchaStrategy` | 4位字母数字字符串 | 500ms |
| `CalculateCaptchaStrategy` | 两位数加减法算式 | 1000ms |
| `SlidePuzzleCaptchaStrategy` | 滑动拼图（容差5px） | 1200ms |
| `ClickWordCaptchaStrategy` | 点选文字（容差FONT_SIZE） | 2000ms |
| `RotatePuzzleCaptchaStrategy` | 旋转拼图（容差10度） | 2000ms |

> **反机器人**：用户操作时间低于阈值时判定可疑，50% 概率放行（基于 `opTime % 2`）。

# MfaDeviceType 枚举

| 枚举 | 值 | 说明 |
|------|-----|------|
| `TOTP_RECOVERY_CODE` | 20 | TOTP恢复码登录 |
| `TOTP_CODE` | 21 | TOTP验证码登录 |
| `EMAIL_CODE` | 22 | Email验证码（设备码发送支持） |
| `MOBILE_CODE` | 23 | 手机短信验证码（设备码发送支持） |

> `sendDeviceCode` 仅支持 `MOBILE_CODE` 与 `EMAIL_CODE`，其他类型返回 `DEVICE_TYPE_ERROR`。

# 响应码（MfaResponseCode）

响应码前缀 `uw.mfa`，i18n 资源 `i18n/messages/uw_mfa`。

| 响应码 | 说明 |
|--------|------|
| `IP_AUTH_ERROR` | IP不在授权范围 |
| `IP_LIMIT_WARN` | IP验证失败达到警告阈值 |
| `IP_LIMIT_ERROR` | IP验证失败达到屏蔽阈值 |
| `CAPTCHA_FEE_ERROR` | Captcha欠费 |
| `CAPTCHA_SEND_LIMIT_ERROR` | Captcha发送超限 |
| `CAPTCHA_GENERATE_ERROR` | Captcha生成错误 |
| `CAPTCHA_LOST_ERROR` | Captcha信息丢失 |
| `CAPTCHA_VERIFY_ERROR` | Captcha校验错误 |
| `DEVICE_CODE_FEE_ERROR` | 设备码发送欠费 |
| `DEVICE_CODE_SEND_LIMIT_ERROR` | 设备码发送超限 |
| `DEVICE_CODE_VERIFY_LIMIT_ERROR` | 设备码校验超限 |
| `DEVICE_CODE_SEND_ERROR` | 设备码发送失败 |
| `DEVICE_CODE_LOST_ERROR` | 设备码信息丢失 |
| `DEVICE_CODE_VERIFY_ERROR` | 设备码校验错误 |
| `DEVICE_TYPE_ERROR` | 设备类型错误 |
| `TOTP_SECRET_GEN_ERROR` | TOTP密钥生成失败 |
| `TOTP_SECRET_MATCH_ERROR` | TOTP密钥不匹配 |
| `TOTP_SECRET_LOST_ERROR` | TOTP密钥丢失 |
| `TOTP_CODE_LOST_ERROR` | TOTP验证码丢失 |
| `TOTP_CODE_VERIFY_ERROR` | TOTP验证码校验错误 |
| `TOTP_VERIFY_LIMIT_ERROR` | TOTP校验超限 |
| `TOTP_RECOVERY_CODE_VERIFY_ERROR` | TOTP恢复码校验错误 |

# 注意事项

1. **`verifyCaptcha` 不可直接对外暴露**：否则会被重放/重试攻击，应由业务接口内部调用。
2. **设备码发送失败会清理验证码**：通知服务故障时已写入 Redis 的验证码会被删除，避免残留绕过发送限制。
3. **TOTP 默认 SHA1**：为兼容 Google Authenticator 等主流 App，默认算法为 SHA1，可按需切换 SHA256/SHA512。
4. **独立 Redis 连接**：`mfaRedisTemplate` 使用独立连接池与 database，与业务 Redis 隔离。
5. **校验错误限制是独立的**：设备码/TOTP 的校验错误限制基于 deviceId/userInfo，与 IP 限制相互独立。
