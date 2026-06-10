# uw-auth-service

## 目录

- [1. 项目概述](#1-项目概述)
    - [1.1 简介](#11-简介)
    - [1.2 核心特性](#12-核心特性)
- [2. 技术栈](#2-技术栈)
- [3. 安装与配置](#3-安装与配置)
    - [3.1 Maven依赖](#31-maven依赖)
    - [3.2 基础配置](#32-基础配置)
- [4. 核心功能详解](#4-核心功能详解)
    - [4.1 权限声明注解](#41-权限声明注解)
    - [4.2 用户类型](#42-用户类型)
    - [4.3 授权类型](#43-授权类型)
    - [4.4 操作日志类型](#44-操作日志类型)
    - [4.5 Token认证流程](#45-token认证流程)
    - [4.6 全局响应处理](#46-全局响应处理)
    - [4.7 全局异常处理](#47-全局异常处理)
- [5. API使用指南](#5-api使用指南)
    - [5.1 声明接口权限](#51-声明接口权限)
    - [5.2 获取当前用户信息](#52-获取当前用户信息)
    - [5.3 生成Guest Token](#53-生成guest-token)
    - [5.4 记录操作日志](#54-记录操作日志)
- [6. 核心类说明](#6-核心类说明)
- [7. 最佳实践](#7-最佳实践)
- [8. 常见问题](#8-常见问题)

---

## 1. 项目概述

### 1.1 简介

`uw-auth-service` 是 UW Base 的认证授权服务模块，为 Web 应用提供统一的认证、授权和日志记录功能。该模块作为服务端的认证组件，通过与
uw-auth-center 认证中心交互，实现 Token 验证、权限检查、操作日志记录等功能。

### 1.2 核心特性

| 特性          | 说明                                           |
|-------------|----------------------------------------------|
| **权限注解**    | 通过 `@MscPermDeclare` 注解声明接口权限                |
| **多用户类型**   | 支持 RPC、ROOT、OPS、ADMIN、SAAS、MCH、GUEST 等多种用户类型 |
| **分级授权**    | 支持 NONE、TEMP、USER、PERM、SUDO 五级授权类型           |
| **Token缓存** | 使用 Caffeine 缓存用户 Token，减少认证中心调用              |
| **全局响应包装**  | 自动将响应包装为 `ResponseData` 格式                   |
| **全局异常处理**  | 统一处理认证异常，返回标准错误码                             |
| **操作日志记录**  | 支持 BASE、REQUEST、RESPONSE、ALL、CRIT 多级日志记录     |
| **IP白名单**   | 支持 RPC/Agent 路径的 IP 白名单保护                    |

---

## 2. 技术栈

| 技术              | 版本  | 用途                  |
|-----------------|-----|---------------------|
| Spring Boot     | 3.x | 基础框架                |
| Spring Security | 6.x | 安全框架                |
| Caffeine        | 3.x | 本地缓存（Token缓存）       |
| LogClient       | -   | 日志记录（Elasticsearch） |

---

## 3. 安装与配置

### 3.1 Maven依赖

```xml
<dependency>
    <groupId>com.umtone</groupId>
    <artifactId>uw-auth-service</artifactId>
    <version>${uw-auth-service.version}</version>
</dependency>
```

### 3.2 基础配置

```yaml
uw:
  auth:
    service:
      # 认证中心服务地址
      auth-center-host: http://uw-auth-center
      
      # 是否启用网关模式
      enable-gateway: true
      
      # 认证保护路径（受Token验证保护）
      auth-protected-paths: /*
      
      # IP保护路径（需要IP白名单）
      ip-protected-paths: /rpc/*,/agent/*
      
      # IP白名单列表
      ip-white-list: 127.0.0.1,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16,::1/128,fe80::/10,FC00::/7
      
      # 应用ID（由auth-center分配）
      app-id: 0
      
      # 应用显示名称
      app-label: 我的应用
      
      # 用户Token缓存大小配置
      token-cache:
        10: 100      # RPC用户缓存100
        100: 100     # ROOT用户缓存100
        110: 100     # OPS用户缓存100
        200: 1000    # ADMIN用户缓存1000
        300: 100000  # SAAS用户缓存100000
        1: 1000000   # GUEST用户缓存1000000
```

---

## 4. 核心功能详解

### 4.1 权限声明注解

`@MscPermDeclare` 用于声明接口的权限要求：

```java
@Target({ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MscPermDeclare {
    String name() default "";           // 权限名称
    String description() default "";    // 权限描述
    String uri() default "";            // 权限URI
    UserType user() default UserType.ANY;  // 用户类型要求
    AuthType auth() default AuthType.USER; // 授权类型
    ActionLog log() default ActionLog.BASE; // 日志级别
}
```

### 4.2 用户类型

| 用户类型  | 值   | 说明          |
|-------|-----|-------------|
| ANY   | 0   | 任意用户        |
| GUEST | 1   | C站用户（访客）    |
| RPC   | 10  | RPC用户（内部服务） |
| ROOT  | 100 | 超级管理员       |
| OPS   | 110 | 开发运维用户      |
| ADMIN | 200 | 平台管理员       |
| SAAS  | 300 | SAAS运营商     |
| MCH   | 310 | SAAS商户      |

### 4.3 授权类型

| 授权类型 | 值 | 说明        |
|------|---|-----------|
| NONE | 0 | 不验证鉴权     |
| TEMP | 1 | 临时授权      |
| USER | 2 | 仅验证用户类型   |
| PERM | 3 | 验证用户类型和权限 |
| SUDO | 6 | 超级用户权限    |

### 4.4 操作日志类型

| 日志类型     | 值  | 说明                |
|----------|----|-------------------|
| NONE     | -1 | 不记录               |
| BASE     | 0  | 记录基本信息            |
| REQUEST  | 1  | 记录请求参数            |
| RESPONSE | 2  | 记录返回结果            |
| ALL      | 3  | 记录全部信息            |
| CRIT     | 9  | 记录全部数据，同时写入ES和数据库 |

### 4.5 Token认证流程

```
┌─────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   客户端请求  │────▶│  AuthServiceFilter │────▶│  解析Token      │
└─────────────┘     └─────────────────┘     └─────────────────┘
                                                      │
                         ┌────────────────────────────┘
                         ▼
                    ┌─────────────────┐
                    │  Caffeine缓存    │
                    │  检查Token是否存在 │
                    └─────────────────┘
                         │
            ┌────────────┴────────────┐
            ▼                         ▼
      ┌─────────────┐           ┌─────────────┐
      │  缓存命中    │           │  缓存未命中  │
      │  直接返回    │           │  调用认证中心 │
      └─────────────┘           └──────┬──────┘
                                       │
                         ┌─────────────┴─────────────┐
                         ▼                           ▼
                   ┌─────────────┐             ┌─────────────┐
                   │  验证通过    │             │  验证失败    │
                   │  存入缓存    │             │  返回错误    │
                   └─────────────┘             └─────────────┘
```

### 4.6 全局响应处理

`GlobalResponseAdvice` 自动将所有响应包装为 `ResponseData` 格式：

- 自动包装 Controller 返回值
- 支持 `@ResponseAdviceIgnore` 注解跳过包装
- 统一响应格式便于前端处理

### 4.7 全局异常处理

`GlobalExceptionAdvice` 统一处理认证相关异常：

| 异常类型                  | HTTP状态码 | 说明       |
|-----------------------|---------|----------|
| TokenInvalidException | 401     | Token无效  |
| TokenExpiredException | 498     | Token已过期 |
| TokenPermException    | 403     | 权限不足     |
| TokenPayException     | 402     | 需要支付     |
| TokenServiceException | 503     | 服务不可用    |
| TokenSudoException    | 423     | 需要SUDO权限 |

---

## 5. API使用指南

### 5.1 声明接口权限

```java
@RestController
@RequestMapping("/admin/user")
public class UserAdminController {
    
    // 仅管理员可访问，记录关键日志
    @MscPermDeclare(user = UserType.ADMIN, log = ActionLog.CRIT)
    @PostMapping("/create")
    public ResponseData<User> createUser(@RequestBody User user) {
        return userService.create(user);
    }
    
    // 管理员和SAAS用户可访问，记录全部日志
    @MscPermDeclare(user = {UserType.ADMIN, UserType.SAAS}, log = ActionLog.ALL)
    @GetMapping("/list")
    public ResponseData<DataList<User>> listUsers(PageQueryParam param) {
        return userService.list(param);
    }
    
    // 需要权限验证（用户类型+权限码）
    @MscPermDeclare(user = UserType.ADMIN, auth = AuthType.PERM)
    @DeleteMapping("/delete/{id}")
    public ResponseData<Void> deleteUser(@PathVariable Long id) {
        return userService.delete(id);
    }
    
    // RPC调用（内部服务）
    @MscPermDeclare(user = UserType.RPC)
    @PostMapping("/rpc/update")
    public ResponseData<User> updateUser(@RequestBody User user) {
        return userService.update(user);
    }
    
    // 不验证权限（公开接口）
    @MscPermDeclare(auth = AuthType.NONE)
    @GetMapping("/public/info")
    public ResponseData<String> getPublicInfo() {
        return ResponseData.success("公开信息");
    }
}
```

### 5.2 获取当前用户信息

```java
import uw.auth.service.AuthServiceHelper;

@Service
public class OrderService {
    
    public ResponseData<Order> createOrder(Order order) {
        // 获取当前登录用户信息
        long userId = AuthServiceHelper.getUserId();        // 用户ID
        String userName = AuthServiceHelper.getUserName();  // 用户名
        int userType = AuthServiceHelper.getUserType();     // 用户类型
        long saasId = AuthServiceHelper.getSaasId();        // SAAS ID
        long mchId = AuthServiceHelper.getMchId();          // 商户ID
        long groupId = AuthServiceHelper.getGroupId();      // 用户组ID
        String realName = AuthServiceHelper.getRealName();  // 真实姓名
        String nickName = AuthServiceHelper.getNickName();  // 昵称
        String mobile = AuthServiceHelper.getMobile();      // 手机号
        String email = AuthServiceHelper.getEmail();        // 邮箱
        String loginIp = AuthServiceHelper.getLoginIp();    // 登录IP
        
        // 设置订单信息
        order.setUserId(userId);
        order.setSaasId(saasId);
        order.setMchId(mchId);
        order.setCreateUser(userName);
        
        return orderDao.save(order);
    }
}
```

### 5.3 生成Guest Token

```java
import uw.auth.service.AuthServiceHelper;
import uw.auth.client.vo.TokenResponse;

@Service
public class GuestService {
    
    public TokenResponse generateGuestToken(long saasId, long mchId, String userName) {
        // 生成用户ID
        long userId = AuthServiceHelper.getAuthServiceRpc().genUserId().getData();
        
        // 生成Guest Token
        TokenResponse tokenResponse = AuthServiceHelper.getAuthServiceRpc().genGuestToken(
            "web",                    // loginAgent
            "Mozilla/5.0",           // clientAgent
            1,                       // loginType
            userName,                // loginId
            saasId,                  // saasId
            mchId,                   // mchId
            userId,                  // userId
            userName,                // userName
            userName,                // nickName
            userName,                // realName
            "",                      // mobile
            "",                      // email
            "127.0.0.1",            // userIp
            "",                      // remark
            false                    // checkDoubleLogin
        );
        
        return tokenResponse;
    }
}
```

### 5.4 记录操作日志

```java
import uw.auth.service.AuthServiceHelper;
import uw.auth.service.vo.MscActionLog;

@Service
public class LogService {
    
    public void recordOperation() {
        // 获取当前请求日志对象
        MscActionLog actionLog = AuthServiceHelper.getContextLog();
        
        if (actionLog != null) {
            // 设置业务信息
            actionLog.setBizType("ORDER");
            actionLog.setBizId("123456");
            actionLog.setBizLog("创建订单成功");
        }
    }
}
```

---

## 6. 核心类说明

| 类名                       | 说明                            |
|--------------------------|-------------------------------|
| `AuthServiceHelper`      | 认证服务辅助类，提供获取当前用户信息、生成Token等方法 |
| `AuthServiceFilter`      | 认证过滤器，处理Token验证和权限检查          |
| `MscAuthPermService`     | 权限服务，处理权限验证逻辑                 |
| `AuthServiceRpc`         | 认证中心RPC接口，与uw-auth-center交互   |
| `AuthAppRpc`             | 应用注册和状态报告RPC接口                |
| `AuthTokenData`          | Token数据对象，包含用户认证信息            |
| `MscActionLog`           | 操作日志VO，记录请求响应信息               |
| `GlobalResponseAdvice`   | 全局响应处理器，包装ResponseData        |
| `GlobalExceptionAdvice`  | 全局异常处理器，统一处理认证异常              |
| `AuthCriticalLogStorage` | 关键日志存储接口                      |
| `MscPermDeclare`         | 权限声明注解                        |
| `UserType`               | 用户类型枚举                        |
| `AuthType`               | 授权类型枚举                        |
| `ActionLog`              | 操作日志级别枚举                      |

---

## 7. 最佳实践

1. **权限最小化**：只授予必要的权限，遵循最小权限原则
2. **日志分级**：关键操作使用 `ActionLog.CRIT`，普通操作使用 `ActionLog.BASE`
3. **用户类型选择**：根据业务场景选择合适的用户类型
4. **缓存配置**：根据实际用户量调整 `token-cache` 配置
5. **IP白名单**：RPC/Agent 路径务必配置 IP 白名单
6. **异常处理**：利用全局异常处理器统一处理认证异常

---

## 8. 常见问题

### Q1: 如何跳过权限验证？

使用 `@MscPermDeclare(auth = AuthType.NONE)` 声明接口不验证权限。

### Q2: Token缓存多久过期？

Token 缓存过期时间跟随 Token 本身的过期时间，由认证中心决定。

### Q3: 如何获取应用权限列表？

```java
Map<String, Integer> permMap = AuthServiceHelper.getAppPermMap();
```

### Q4: 如何踢出用户？

```java
AuthServiceHelper.getAuthServiceRpc().kickoutGuest(
    "web", saasId, userId, "踢出原因"
);
```

### Q5: 如何自定义关键日志存储？

实现 `AuthCriticalLogStorage` 接口：

```java
@Component
public class MyCriticalLogStorage implements AuthCriticalLogStorage {
    @Override
    public void save(MscActionLog mscActionLog) {
        // 自定义存储逻辑
    }
}
```

---

## 许可证

[Apache License 2.0](../LICENSE)
