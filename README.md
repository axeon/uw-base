# UW-Base

基于Spring Boot/Cloud的企业级多子模块基础类库集合

---

## 目录

- [项目概述](#项目概述)
- [核心功能](#核心功能)
- [子模块说明](#子模块说明)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [API文档](#api文档)
- [贡献指南](#贡献指南)
- [许可证](#许可证)
- [联系方式](#联系方式)

---

## 项目概述

**UW-Base** 是一套基于Spring Boot 3.x和Spring Cloud 2025.x构建的企业级多子模块基础类库集合。该项目旨在为微服务架构提供完整、稳定、高性能的基础设施支持，涵盖了从数据访问、缓存管理、认证授权到任务调度、日志收集等全方位的功能模块。

本项目采用模块化设计，各子模块既可独立使用，也可协同工作，为构建大规模分布式系统提供坚实的基础支撑。

### 设计理念

- **模块化**：各功能模块独立封装，按需引入，降低耦合
- **高性能**：针对高并发场景进行深度优化，提供卓越的性能表现
- **易用性**：简化配置，提供开箱即用的自动配置能力
- **可扩展**：良好的扩展接口设计，支持自定义功能扩展
- **生产就绪**：内置监控、日志、限流等企业级功能

---

## 核心功能

UW-Base 提供了以下核心能力：

| 功能领域 | 核心能力 | 对应模块 |
|---------|---------|---------|
| **数据访问** | 多数据源支持、分库分表、JPA风格CRUD、SQL映射 | uw-dao |
| **缓存管理** | 本地缓存(Caffeine)、分布式缓存(Redis)、融合缓存、全局锁、计数器 | uw-cache |
| **认证授权** | Token管理、权限控制、访问限速、操作日志、多因素认证 | uw-auth-service, uw-auth-client, uw-mfa |
| **任务调度** | 定时任务、队列任务、RPC调用、流量控制、任务监控 | uw-task |
| **日志收集** | Elasticsearch日志写入、Logback集成、日志查询 | uw-log-es, uw-logback-es |
| **HTTP通信** | HTTP客户端、JSON/XML序列化、接口日志 | uw-httpclient |
| **AI集成** | Chat对话、Embedding向量、RAG检索、MCP协议、翻译 | uw-ai |
| **OAuth2登录** | 多平台OAuth2登录、扫码登录、账号绑定 | uw-oauth2-client |
| **通用工具** | 响应封装、加密解密、日期处理、JSON工具 | uw-common, uw-common-app |

---

## 子模块说明

### 基础工具模块

#### [uw-common](uw-common/README.md)
通用工具类库，提供项目基础支撑能力。

- **主要功能**：
  - `ResponseData<T>`：统一响应数据封装，支持函数式链式调用
  - `BitConfigUtils`：位运算配置工具，支持32/64位开关存储
  - `DateUtils`：灵活的日期处理工具
  - `JsonUtils`：JSON序列化/反序列化工具
  - `AESUtils/RSAUtils/DigestUtils`：加密解密与签名工具
  - `SnowflakeIdGenerator`：分布式雪花ID生成器
  - `IpMatchUtils`：IP匹配工具，支持CIDR格式
  - `ByteArrayUtils`：字节数组操作工具

- **技术栈**：Java 21, Jackson
- **使用场景**：所有需要统一响应格式和基础工具的项目

#### [uw-common-app](uw-common-app/README.md)
基于Web应用的公共类库，提供后台应用通用功能。

- **主要功能**：
  - 自动化权限注入管理（基于AuthServiceHelper和QueryParam）
  - 关键操作日志记录（SysCritLog）
  - 数据历史记录（SysDataHistory）
  - JSON配置管理（JsonConfigHelper）
  - i18n国际化支持（12种语言）

- **技术栈**：Spring Boot, uw-dao, uw-auth-service
- **使用场景**：后台管理系统、需要数据审计和配置管理的应用

---

### 数据访问模块

#### [uw-dao](uw-dao/README.md)
数据库访问层类库，比Hibernate更高效，比MyBatis更简单。

- **主要功能**：
  - 多数据库连接支持（MySQL/Oracle/SQLServer）
  - 基于表名的动态路由配置，支持分库分表
  - 自研连接池，线程数少且节省资源
  - JPA风格的CRUD操作（基于反射，带缓存优化）
  - MyBatis风格的SQL映射
  - 自动分表支持（按日期分表）
  - SQL执行监控与统计（慢查询、异常SQL）
  - 批量更新与事务支持
  - QueryParam注解自动生成查询SQL

- **技术栈**：Java 21, JDBC, Kryo
- **使用场景**：需要高性能数据访问、分库分表、复杂SQL查询的业务系统

---

### 缓存模块

#### [uw-cache](uw-cache/README.md)
基于Caffeine和Redis的融合缓存类库。

- **主要功能**：
  - **FusionCache**：融合本地（Caffeine）和全局（Redis）缓存，性能更高
  - **GlobalCache**：直接操作Redis，不占用JVM内存
  - **GlobalLocker**：基于Redis的全局分布式锁
  - **FusionCounter**：融合本地和全局的计数器
  - **GlobalCounter**：基于Redis的全局计数器
  - **GlobalHashSet**：基于Redis Set的便利实现
  - **GlobalSortedSet**：基于Redis ZSet的实现，支持延迟任务

- **技术栈**：Caffeine, Redis, Lettuce, Kryo
- **使用场景**：高并发缓存、分布式锁、计数器、延迟队列等场景

**性能数据**（MBP16 M2Max, 20线程）：
- ConcurrentHashMap: 100%（基准）
- Caffeine（无过期）: 162%
- FusionCache: 70%
- GlobalCache: 0.0002%

---

### 认证授权模块

#### [uw-auth-service](uw-auth-service/README.md)
认证授权服务的服务端模块，提供完整的权限管理能力。

- **主要功能**：
  - Token分发与管理（AccessToken/RefreshToken）
  - 12种内置用户类型（ROOT, RPC, OPS, ADMIN, SAAS, MCH, GUEST等）
  - 基于注解的权限声明（@MscPermDeclare）
  - 层级权限继承（SAAS→用户组→用户）
  - 访问限速控制（全局/本地限速器）
  - 操作日志自动记录
  - 全局异常处理与响应封装
  - 多因素认证集成

- **技术栈**：Spring Boot, Spring Cloud, Redis
- **使用场景**：需要完整权限管理的微服务应用

#### [uw-auth-client](uw-auth-client/README.md)
认证授权的客户端模块，用于内部RPC调用的鉴权。

- **主要功能**：
  - 带认证的RestTemplate（@Qualifier("authRestTemplate")）
  - 自动Token注入与刷新
  - HTTP连接池管理

- **技术栈**：Spring Boot, uw-httpclient
- **使用场景**：微服务间的内部RPC调用

#### [uw-mfa](uw-mfa/README.md)
多因素认证（MFA）类库，融合IP限制、验证码、设备码认证。

- **主要功能**：
  - IP白名单与错误限制
  - 多种验证码策略（字符串、计算、滑动拼图、点击文字、旋转拼图）
  - TOTP基于时间的一次性密码
  - 设备验证码（短信/邮件）
  - `MfaFusionHelper`：融合MFA帮助类，统一处理认证流程

- **技术栈**：Redis, Java 2D
- **使用场景**：登录认证、敏感操作验证、防暴力破解

---

### 任务调度模块

#### [uw-task](uw-task/README.md)
分布式任务框架，支持定时任务和队列任务。

- **主要功能**：
  - **定时任务（TaskCroner）**：基于Cron表达式，支持服务端动态配置
  - **队列任务（TaskRunner）**：支持流量控制、错误重试、动态配置
  - **RPC调用**：支持同步/异步远程调用，自动本地/远程判定
  - 多实例运行支持
  - 任务报警规则配置
  - 任务运维监控

- **技术栈**：Spring Boot, RabbitMQ, Redis
- **使用场景**：分布式定时任务、异步队列处理、RPC调用

---

### 日志模块

#### [uw-log-es](uw-log-es/README.md)
Elasticsearch日志客户端，支持日志的写入和查询。

- **主要功能**：
  - 日志对象注册与管理
  - 单条/批量日志写入
  - SQL转DSL查询
  - Scroll API大数据量查询
  - 分页查询支持

- **技术栈**：Elasticsearch, uw-httpclient
- **使用场景**：日志集中存储与分析

#### [uw-logback-es](uw-logback-es/README.md)
Logback的Elasticsearch Appender，直接将日志发送到ES。

- **主要功能**：
  - 无需Logstash，直接发送日志到ES
  - 批量提交，可配置刷新策略
  - JMX监控支持
  - 自定义日志字段

- **技术栈**：Logback, Elasticsearch
- **使用场景**：替代传统ELK架构中的Logstash环节

---

### HTTP通信模块

#### [uw-httpclient](uw-httpclient/README.md)
针对接口业务设计的HTTP客户端类库。

- **主要功能**：
  - 完整支持GET/POST/PUT/DELETE
  - 请求/响应对象自动序列化（JSON/XML）
  - 完整的HTTP请求日志记录
  - SSL/TLS支持（包括自签名证书）
  - 连接池管理
  - 文件上传下载

- **技术栈**：OkHttp, Jackson
- **使用场景**：第三方接口对接、微服务HTTP调用

---

### AI模块

#### [uw-ai](uw-ai/README.md)
基于AI技术的类库，集成多种AI能力。

- **主要功能**：
  - Chat对话（支持多轮对话）
  - Embedding向量生成
  - MCP（Model Context Protocol）定制版
  - RAG检索增强生成
  - 翻译功能
  - AI工具（Function Calling）

- **技术栈**：Spring AI, OpenAI/Claude等模型API
- **使用场景**：智能客服、知识库问答、内容生成

---

### OAuth2模块

#### [uw-oauth2-client](uw-oauth2-client/README.md)
轻量级OAuth2客户端库，支持多平台第三方登录。

- **主要功能**：
  - 支持Google、Apple、GitHub、微信、支付宝等平台
  - 可扩展Provider机制，支持自定义平台
  - 扫码登录功能（二维码生成、状态验证）
  - 账号绑定与解绑
  - 与现有Token系统无缝对接

- **技术栈**：Java 21, uw-httpclient
- **使用场景**：C端用户登录、第三方账号集成

---

### 其他客户端模块

#### [uw-gateway-client](uw-gateway-client/README.md)
网关客户端，提供服务网关相关功能支持。

#### [uw-notify-client](uw-notify-client/README.md)
基于SSE（Server-Sent Events）技术的Web通知库。

#### [uw-tinyurl-client](uw-tinyurl-client/README.md)
短链接客户端，提供短链接生成与解析功能。

#### [uw-mydb-client](uw-mydb-client/README.md)
MyDB服务客户端，用于访问MyDB数据服务。

---

## 环境要求

### 基础环境

| 组件 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 21+ | 必须使用Java 21或更高版本 |
| Maven | 3.8+ | 项目构建工具 |
| Spring Boot | 3.5.6 | 基础框架版本 |
| Spring Cloud | 2025.0.0 | 微服务框架版本 |
| Spring Cloud Alibaba | 2023.0.1.2 | 阿里云微服务组件 |

### 可选依赖

| 组件 | 版本 | 用途 |
|------|------|------|
| MySQL | 8.0+ | 数据存储 |
| Redis | 6.0+ | 缓存、分布式锁 |
| RabbitMQ | 3.8+ | 任务队列 |
| Elasticsearch | 8.x | 日志存储与搜索 |

---

## 快速开始

### 1. 引入依赖管理

在项目的 `pom.xml` 中添加UW-Base的依赖管理：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.umtone</groupId>
            <artifactId>uw-base</artifactId>
            <version>2025.0901.0005</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 2. 添加所需模块依赖

根据需求引入具体模块：

```xml
<dependencies>
    <!-- 基础工具 -->
    <dependency>
        <groupId>com.umtone</groupId>
        <artifactId>uw-common</artifactId>
    </dependency>
    
    <!-- 数据访问 -->
    <dependency>
        <groupId>com.umtone</groupId>
        <artifactId>uw-dao</artifactId>
    </dependency>
    
    <!-- 缓存 -->
    <dependency>
        <groupId>com.umtone</groupId>
        <artifactId>uw-cache</artifactId>
    </dependency>
    
    <!-- 认证授权 -->
    <dependency>
        <groupId>com.umtone</groupId>
        <artifactId>uw-auth-service</artifactId>
    </dependency>
</dependencies>
```

### 3. 基础配置示例

```yaml
# 数据源配置（uw-dao）
uw:
  dao:
    conn-pool:
      default:
        driver: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=Asia/Shanghai
        username: root
        password: password
        min-conn: 5
        max-conn: 20

# 缓存配置（uw-cache）
  cache:
    redis:
      host: localhost
      port: 6379
      password: 
      database: 0

# 认证配置（uw-auth-service）
  auth:
    service:
      app-label: "我的应用"
      auth-center-host: http://uw-auth-center
      auth-entry-point: "/api/*"
```

### 4. 代码示例

#### 使用ResponseData统一响应

```java
@RestController
public class UserController {
    
    @GetMapping("/user/{id}")
    public ResponseData<User> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) {
            return ResponseData.errorCode(UserResponseCode.USER_NOT_FOUND);
        }
        return ResponseData.success(user);
    }
}
```

#### 使用DaoManager进行数据访问

```java
@Service
public class UserService {
    
    private final DaoManager dao = DaoManager.getInstance();
    
    public ResponseData<User> findById(Long id) {
        return dao.load(User.class, id)
            .map(ResponseData::success)
            .orElse(ResponseData.errorCode(UserResponseCode.USER_NOT_FOUND));
    }
    
    public ResponseData<DataList<User>> listUsers(UserQueryParam param) {
        return dao.list(User.class, param);
    }
}
```

#### 使用FusionCache进行缓存

```java
@Component
public class UserCache {
    
    @PostConstruct
    public void init() {
        FusionCache.Config config = new FusionCache.Config(
            User.class, 
            1000,           // 本地缓存最大数量
            86400_000L      // 缓存过期时间（毫秒）
        );
        
        FusionCache.config(config, new CacheDataLoader<Long, User>() {
            @Override
            public User load(Long userId) {
                return userDao.findById(userId).orElse(null);
            }
        });
    }
    
    public User getUser(Long userId) {
        return FusionCache.get(User.class, userId);
    }
}
```

---

## API文档

各模块的详细API文档请参考各子模块的README.md文件：

- [uw-common API](uw-common/README.md)
- [uw-dao API](uw-dao/README.md)
- [uw-cache API](uw-cache/README.md)
- [uw-auth-service API](uw-auth-service/README.md)
- [uw-task API](uw-task/README.md)
- [uw-log-es API](uw-log-es/README.md)
- [uw-httpclient API](uw-httpclient/README.md)
- [uw-oauth2-client API](uw-oauth2-client/README.md)

---

## 贡献指南

我们欢迎社区贡献！如果您想为UW-Base做出贡献，请遵循以下步骤：

### 提交Issue

- 使用清晰的标题描述问题
- 提供详细的复现步骤
- 说明期望行为和实际行为
- 提供环境信息（JDK版本、Spring Boot版本等）

### 提交Pull Request

1. Fork本仓库
2. 创建您的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交您的更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开一个Pull Request

### 代码规范

- 遵循Java编码规范
- 添加必要的JavaDoc注释
- 保持与现有代码风格一致
- 确保所有测试通过

---

## 许可证

本项目采用 [Apache License 2.0](LICENSE) 开源许可证。

```
Copyright 2025 UW-Base Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## 联系方式

- **项目主页**: [https://github.com/axeon/uw-base](https://github.com/axeon/uw-base)
- **Issue追踪**: [https://github.com/axeon/uw-base/issues](https://github.com/axeon/uw-base/issues)
- **维护者**: zhangjin (23231269@qq.com)

---

<p align="center">
  <strong>Star ⭐ 本项目如果它对您有帮助！</strong>
</p>
