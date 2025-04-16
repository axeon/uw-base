[TOC]

## 简介

uw-auth主要是用来管理分布式API，用户Token的分发，用户API的授权和鉴权。

## 主要特性

1. 基于spring boot实现，支持spring cloud的相关特性。
2. 优化的TOKEN设计，比jwt性能高，安全性强。
3. 针对行业SAAS的b2b2c营销体系专门设计，角色定义内置化。
4. 使用JAVA注解实现权限系统的定义，注册上传、鉴权日志功能，去除冗繁的管理操作。
5. 支持多重的访问限速控制功能。
6. 结合uw-codegen-center，可以辅助生成前端代码，包括api/router，可自动组织功能菜单。

## 基础概念

### 功能组成

uw-auth由uw-auth-center，uw-auth-service，uw-auth-client三部分组成。
想·
- uw-auth-center 系统鉴权中心，负责系统功能、权限；SAAS、用户组、用户的信息、权限存储；token生成和管理；提供用户权限相关业务功能的外部访问接口。
- uw-auth-service 权限服务的服务器端模块，为业务功能模块提供功能权限的注册、鉴权功能，同时提供访问限速功能。
- uw-auth-client 内部RPC调用的客户端，对于无内部调用的情况下，客户端不是必须的。

### 基本原理

1.
权限菜单注册阶段。内置uw-auth-service的业务微服务应用第一次启动时，将会自动扫描代码中带有权限注解的功能模块，向uw-auth-center注册功能权限和树形菜单结构。当业务应用增删改功能后，重启将会向uw-auth-center自动注册同步最新权限和菜单结构。
2. 用户权限授权阶段。管理员登陆uw-auth-center管理系统，可以将已注册的权限授权给SAAS系统、用户组、用户。
3. 用户使用权限。用户使用指定用户类型、用户名密码登陆后，将获得对应的权限和菜单属性结构。
4. 用户鉴权控制。管理员可以登陆uw-auth-center管理系统，查看用户访问日志和用户操作日志，包括请求ip，请求用户信息，请求内容，返回内容，请求时间，异常等相关信息。

### 权限模型

uw-auth是针对行业SAAS的b2b2c营销体系专门设计的用户权限管理系统，和典型的RBAC模型有一定的差异。最大的差异是uw-auth的角色类型为代码内置类型，一个功能出现的时候基本上就绑定了用户角色（ROLE），在uw-auth中用户角色定义为用户类型（UserType）。在此基础上定义了12种不同的用户类型/角色。使用内置的用户类型，明确了代码用途，防止错误授权，降低权限管理复杂度。
在明确了用户类型/角色的基础上，uw-auth在平行纬度实现层级权限继承功能。层级为SAAS->用户组->用户，用户的权限不能超越所属用户组权限，用户组权限不能超越SAAS系统权限。

### 用户类型

ROOT：最高管理员。一般有且只有一个，用于管理RPC，DEVOPS，ADMIN用户。
RPC：内部RPC帐号。仅限于内部RPC通讯鉴权用，在外部系统不显现，也无业务菜单功能。
OPS：开发运维人员帐号。用于内部开发运维人员使用，包括任务中心、代码生成中心、运维中心等功能。
ADMIN：管理员。可以理解为平台内部管理员，可以用于管理SAAS子系统用户。
SAAS：SAAS系统用户。可以管理本SAAS系统内的全部用户帐号。
MCH：SAAS商户。
GUEST：客户，主要用于C端会员使用。
ANONYMOUS：匿名用户，一般用于未登陆用户。

一般来说，controller中的第一级目录也应该是用户类型名，这样便于统一管理授权。
### Token

uw-auth没有照搬jwt的样式，是出于安全性和性能的考虑。
当前的token分为AccessToken和RefreshToken，登录后将同时获得AccessToken和RefreshToken，AccessToken出于安全考虑，存在一个有效期，超过有效期后需要通过RfreshToken刷新。
AccessToken的格式为：UserType$AccessToken!UserId@SaasId
RefreshToken的格式为：UserType#RfreshToken!Userid@SaasId

在token字符串中传入userType，userId，saasId，一方面是为了解析方便，还有很重要的一点，就是支持基于saas和userId的session sticky，用于提升系统性能。

### 鉴权日志

uw-auth可以自动记录用户操作日志，包括如下内容：
登陆日志：用户登录过程中产生的日志。
操作日志：用户操作过程中产生的日志。

### 多种验证支持
1.通过外部短信发送接口实现短信验证码登录。
2.通过外部Email发送接口实现验证码登录。
3.可选微信扫码、企业微信扫码，钉钉扫码登录。
4.通过Refresh接口实现同一个号码下的多账号登录。

### 访问限速

访问限速是非常重要的功能，和常见的gateway级别限速相比，uw-auth提供的限速带有更多的业务特性，比如可以支持按照saas限速，按照saas商户限速（常用于接口访问，很重要）
，按照用户限速等功能。

同时访问限速支持两种限速器，分别是全局限速器：使用redis实现，可在集群中使用；本地限速器：使用guava
ratelimiter实现，性能很高。实际使用的时候，如果gateway可以使用token来sticky到特定后端主机，则更建议使用本地限速器，可以获得几乎0损耗的性能。

## 使用方法

### maven引入

**uw-auth-service：**支持功能权限的注册和校验功能，提供权限注解、鉴权日志，限速支持等功能。

```xml
<dependency>
    <groupId>com.umtone</groupId>
    <artifactId>uw-auth-service</artifactId>
    <version>${uw-auth-service.version}</version>
</dependency>
```

**uw-auth-client：**内部RPC调用支持，如果不使用内部RPC调用，可以不引入。

```xml
<dependency>
    <groupId>com.umtone</groupId>
    <artifactId>uw-auth-client</artifactId>
    <version>${uw-auth-client.version}</version>
</dependency>
```

### 关键配置

```yaml
# uw-auth-client配置
uw:
  auth:
    client:
      # 验证服务器
      auth-center-host: http://uw-auth-center
      # RPC用户登录配置
      username: rpc
      password: 123456
      # HTTP连接池配置
      http-pool:
        max-total: 1000
        default-max-per-route: 1000
        connect-timeout: 30000
        connection-request-timeout: 30000
        socket-timeout: 30000
        keep-alive-time-if-not-present: 0
    # uw-auth-service配置    
    service:
      # 在权限系统中显示的名字
      app-label: "酒店系统"
      # 权限中心服务地址
      auth-center-host: http://uw-auth-center
      # 是否开启权限验证的过滤器，默认开启。
      auth-entry-point: "/rpc/*,/client/*,/admin/*"
      # 限速相关设置
      rateLimit:
        type: GLOBAL
        redis:
          database: 4
          host: 192.168.88.21
          port: 6380
          password: redispasswd
          lettuce:
            pool:
              max-active: 1000
              max-idle: 8
              max-wait: 5000ms
              min-idle: 1
          timeout: 30s
```

### 注解使用

#### @MscPermDeclare 权限注解

1. @MscPermDeclare所注解的API必须在auth-entry-point的保护下，否则不会被保护。
2. 在和Swagger注解联用的时候，不用重复填写name和description属性。
3. 在和Spring MVC联用的时候，不用重复填写uri属性。
4. type属性用于指定UserType用户类型。对于部份共用类型，可以使用类似*SAAS_MCH_SHARE*的类型。
5. auth属性用于指定AuthType授权类型。默认为PERM类型：验证用户类型和权限，此类型需要后台给用户授权才能访问。USER类型：仅验证用户类型，当某些功能不需要授权，仅需要限制用户类型时使用。NONE类型：不做任何验证。
6. log属性用于指定日志级别。NONE：不记录日志。REQUEST：记录请求参数。RESPONSE：记录响应参数,当数据量巨大时会有性能问题,不建议记录。ALL：记录请求参数和响应参数。

```java
/**
 * 订单管理模块
 */
@RestController
@MscPermDeclare(type = UserType.ADMIN)
@Tag(name = "订单管理", description = "订单管理")
@RequestMapping("/admin/mall/order")
public class AdminGroupController {
    /**
     * 订单查询
     * @param orderId
     * @return
     */
    @MscPermDeclare(type = UserType.ADMIN, log = ActionLog.REQUEST)
    @Operation(summary = "查看订单", description = "查看订单")
    @GetMapping("/admin/mall/order/load")
    public String getOrder(long orderId)
    {
        AuthToken authToken = AuthServiceHelper.getContextToken();
        String userId = authToken.getUserId();
        // do your bizLogic...
        AuthServerUtils.setLogInfo("用户订单查询: order_id = "+orderId);
        return userId + username;
    }
}
```

7. $PackageInfo$.java文件。这是package-info.java的替代方案，主要为了生成更全面的菜单属性结构。在此文件中定义了一级菜单结构。

```java
/**
 * 主要是提供注解支持用。
 */
@RestController
public class $PackageInfo$ {
    @MscPermDeclare(type = UserType.ADMIN)
    @Operation(summary = "销售中台", description = "销售中台")
    @GetMapping("/admin/mall")
    public void info() {}

}
```

### AuthServiceHelper

AuthServiceHelper是程序内获得用户权限信息的辅助类，此类通过ThreadLocal来传递关键的权限信息，通过AuthServiceHelper可以获得UserId，SaasId，mchId，token，log等相关信息。
AuthServiceHelper.logRef()方法可以记录操作引用信息。
AuthServiceHelper.logInfo()方法可以在日志中记录关键业务信息，用于事后日志鉴权查证。
AuthServiceHelper.logSysInfo()方法可以在日志中记录系统操作信息，用于事后日志鉴权查证。
AuthServiceHelper.getContextToken()用于获得当前用户的token信息。
AuthToken.getTokenPerm()用于获得用户的辅助权限信息，包括黑白名单，限速信息，权限表，自定义数据配置Map表（可用来存储包括业务权限信息，诸如销售地区限制表，业务授权产品id等业务数据信息）。

### 全局异常处理和全局返回结果Wrapper--ResponseData

为了前端更加方便，使用了GlobalResponseAdvice和GlobalExceptionAdvice对全局返回结果进行了封装，统一用ResponseData<>
结构返回。也因此有以下注意点：

1.某些程序内部调用的REST接口（如RPC接口），建议使用@ResponseAdviceIgnore注解来避免ResponseData<>封装。
2.ResponseData<>结构封装是框架层面的，在Swagger文档中看不到，这个也要求和前端进行说明。
3.程序内部仍然可以自主输出ResponseData格式，这种情况下，GlobalResponseAdvice将不会再次包裹。
4.GlobalExceptionAdvice将请求关键信息和堆栈信息，保存在data中，前端展示时一般展示到msg，但是提供“更多”选项，展示data。

### 对于增删改查列的参数和返回值建议。

增删改查列的基础方法，是有参数和返回值要求的，只要是为了前后端协作和代码生成一致性的要求。
对于特定情况，可以使用ResponseData返回结果，主要用于自定义错误信息。
