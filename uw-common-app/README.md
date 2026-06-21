[TOC]

## 简介

`uw-common-app` 是基于 uw 框架的 Web 端公共库，为后台应用提供开箱即用的通用能力：

- **自动化权限注入查询参数**：基于 `uw-auth-service` 的 `AuthServiceHelper` 与 `uw-dao` 的 `QueryParam`，自动绑定当前登录用户上下文（saasId/mchId/userId/userType）。
- **关键操作日志**：将 CRIT 级别操作日志异步落库到 `sys_crit_log`，弥补 ES 生命周期管理导致的日志丢失。
- **数据历史记录**：通过 `SysDataHistoryHelper` 记录实体变更快照到 `sys_data_history`，支持数据回滚与审计。
- **JSON 配置管理**：通过 `JsonConfigHelper` / `JsonConfigBox` 实现强类型、可校验的 JSON 配置读写。
- **Schema 数据校验**：基于 `@Schema` 注解的声明式 VO 校验。
- **i18n 国际化**：内置 12 种语言的校验/响应码国际化资源。
- **应用启动引导**：`AppBootStrap` 解决多模块同后缀组件的 bean 命名冲突。

## 依赖引入

```xml
<dependency>
    <groupId>com.umtone</groupId>
    <artifactId>uw-common-app</artifactId>
</dependency>
```

> 本模块会通过 `spring.factories` / `AutoConfiguration.imports` 自动装配 `CommonAppAutoConfiguration`，无需额外 `@Enable` 注解。

## 配置项

配置前缀：`uw.common.app`（对应 `CommonAppProperties`）。

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enableCritLog` | boolean | `true` | 是否开启关键日志落库 |
| `localeDefault` | Locale | `zh_CN` | 默认语言（Accept-Language 缺失或匹配失败时使用） |
| `localeList` | List&lt;Locale&gt; | 12 种已覆盖语言 | 可选语言列表，参与 Accept-Language 匹配 |
| `shutdownTimeout` | Duration | `3s` | Nacos 反注册后预留的优雅停机等待时长 |
| `disableSwagger` | boolean | `false` | 是否禁用 Swagger 接口（/v3/api-docs、/swagger-ui） |

## 自动化权限注入查询参数

通过结合 `AuthServiceHelper` 与 `QueryParam`，提供自动化权限注入。权限通过四个关键字段组成：

| 字段 | 默认值 | 含义 |
|------|--------|------|
| `saasId` | 0 | 租户ID，0 表示单租户系统 |
| `mchId` | 0 | 商户ID，0 表示单商户系统 |
| `groupId` | 0 | 用户组ID，0 表示单用户组系统 |
| `userId` | 0 | 用户ID，0 表示单用户系统 |

### 预置 QueryParam 继承体系

```
QueryParam (uw.common.dto)
├── IdQueryParam          — id / ids（无鉴权）
├── IdStateQueryParam     — id / ids + state 系列过滤（无鉴权）
├── AuthQueryParam        — saasId / mchId / userId / userType（自动绑定）
├── AuthIdQueryParam      — Auth + id / ids
└── AuthIdStateQueryParam — Auth + id / ids + state 系列过滤

PageQueryParam (uw.common.dto)
├── AuthPageQueryParam       — saasId / mchId / userId / userType（自动绑定）
├── SysCritLogQueryParam     — 系统关键日志分页查询
└── SysDataHistoryQueryParam — 数据历史分页查询
```

### 使用示例

```java
// Controller 层：自动注入当前用户 saasId
@GetMapping("/list")
public ResponseData<PageList<User>> list(AuthQueryParam param) {
    return dao.list(User.class, param);
}

// 进一步绑定当前用户的 userId / mchId
@GetMapping("/my-orders")
public ResponseData<PageList<Order>> myOrders(AuthQueryParam param) {
    param.bindUserId().bindMchId();
    return dao.list(Order.class, param);
}

// Helper/Service 层（非 web 上下文）：显式传入 saasId
AuthIdQueryParam param = new AuthIdQueryParam(saasId, entityId);
entity = dao.load(Entity.class, param);
```

> ⚠️ `Auth*` 系列参数依赖 web 请求上下文，**仅限 Controller 层使用**；Helper/Service 层请使用带 `saasId` 参数的构造器显式传值。
> ⚠️ 子类 QueryParam（如 `SysCritLogQueryParam`）的 `mchId/userId/userType` 复用父类 `AuthPageQueryParam` 字段，请勿重复定义，否则会导致 @QueryMeta 条件被注入两次。

## 关键操作日志

后台操作历史默认由 `uw-auth-service` 的 `uw.auth.action.log` 写入 ES，但 ES 的生命周期管理可能导致日志被周期性删除。本模块提供 `SysCritLogStorageService`（实现 `AuthCriticalLogStorage`），将标注 `@MscPermDeclare(log = Log.CRIT)` 的关键操作日志**异步落库**到 `sys_crit_log`，作为持久化备份。

- 落库通过虚拟线程执行器异步进行，应用关闭时会等待（最长 10s）未完成任务落库。
- 落库失败仅记录错误日志，不影响主流程。
- 可通过 `uw.common.app.enableCritLog=false` 关闭。

## 数据历史记录

通过 `SysDataHistoryHelper` 将实体变更快照（含变更差异）落库到 `sys_data_history`，便于数据回滚与审计。

```java
// 更新前保存历史
SysDataHistoryHelper.saveHistory(user, "更新前");
ResponseData<User> result = dao.update(user);
// 更新后也可保存一份
result.onSuccess(updated -> SysDataHistoryHelper.saveHistory(updated, "更新后"));
```

- 自动记录操作人上下文（非 web 环境下用户字段填默认值）。
- 落库成功后才清除原实体的更新信息，避免失败时副作用泄漏。
- 落库失败不阻断主业务，仅记录 WARN 日志。

配套查询参数：`SysDataHistoryQueryParam`；实体：`SysDataHistory`。

## JSON 配置管理

通过 `JsonConfigParam`（通常用枚举实现）定义配置项元数据，`JsonConfigHelper` 构建强类型 `JsonConfigBox` 供业务读取。

### 定义配置参数

```java
public enum SystemConfig implements JsonConfigParam {
    SITE_NAME("siteName", ParamType.STRING, "MySite", "站点名称", null),
    MAX_UPLOAD_SIZE("maxUploadSize", ParamType.INT, "10485760", "最大上传大小(字节)", null),
    ENABLE_FEATURE("enableFeature", ParamType.BOOLEAN, "false", "是否开启特性", null);

    private final ParamData paramData;

    SystemConfig(String key, ParamType type, String value, String desc, String regex) {
        this.paramData = new ParamData(key, type, value, desc, regex);
    }

    @Override
    public ParamData getParamData() { return paramData; }
}
```

### 构建与读取

```java
// 从数据库/任意来源拿到配置 JSON 字符串
ResponseData<JsonConfigBox> box = JsonConfigHelper.buildParamBox(
        Arrays.asList(SystemConfig.values()), configDataJson);

// 校验配置数据
ResponseData<List<ValidateResult>> validateResult =
        JsonConfigHelper.validateConfigData(Arrays.asList(SystemConfig.values()), configDataJson);

// 强类型读取
if (box.isSuccess()) {
    String siteName = box.getData().getParam(SystemConfig.SITE_NAME);
    int maxSize = box.getData().getIntParam(SystemConfig.MAX_UPLOAD_SIZE);
    boolean enabled = box.getData().getBooleanParam(SystemConfig.ENABLE_FEATURE);
}
```

- `JsonConfigHelper`：构建盒子与校验配置（静态方法）。
- `JsonConfigBox`：强类型读取（支持 String/int/long/float/double/boolean 及数组、Map）。
- `JsonConfigParam`：参数定义接口（key/type/value/desc/regex）。
- `JsonConfigParam.ParamType`：参数类型枚举（含 STRING/INT/BOOLEAN/DATE/ENUM/MAP 等及其集合类型）。

## Schema 数据校验

基于 `@Schema` 注解的声明式校验，无需手写校验逻辑。

```java
@PostMapping("/save")
public ResponseData<Void> save(@RequestBody UserForm form) {
    List<ValidateResult> errors = SchemaValidateHelper.validate(form);
    if (!errors.isEmpty()) {
        return ResponseData.error(errors, "", "数据校验失败！");
    }
    // ... 业务逻辑
}
```

支持的 `@Schema` 约束：`requiredMode=REQUIRED`（必填）、`minimum/maximum`（数值范围）、`minLength/maxLength`（长度）、`pattern`（正则）。反射元数据通过 Caffeine 缓存。

## 国际化（i18n）

内置两套 i18n 资源，覆盖 12 种语言（zh_CN/zh_TW/en/ja/ko/de/fr/it/es/ru/pt/ar）：

| 资源包 | 对应枚举 | codePrefix |
|--------|---------|-----------|
| `i18n/messages/uw_common` | `CommonResponseCode` | `uw.common` |
| `i18n/messages/uw_validate` | `ValidateResponseCode` | `uw.validate` |

通过请求头 `Accept-Language` 自动切换语言，香港地区（zh-HK）自动映射为中文繁体。

## 应用启动引导

使用 `AppBootStrap.run` 替代 `SpringApplication.run`，解决多模块同后缀（Controller/Runner/Croner/SwaggerConfig）组件的 bean 命名冲突：

```java
public static void main(String[] args) {
    AppBootStrap.run(MyApplication.class, args);
}
```

## 模块结构

```
uw.common.app
├── AppBootStrap                       应用启动引导
├── conf/                              自动配置
│   ├── CommonAppAutoConfiguration     启动配置（i18n/日志/LB/Swagger/ObjectMapper）
│   └── CommonAppProperties            配置属性
├── constant/                          常量与响应码
│   ├── CommonConstants                常用字面量
│   ├── CommonState                    通用状态枚举（-1/0/1）
│   ├── CommonResponseCode             通用响应码
│   └── ValidateResponseCode           校验响应码
├── dto/                               预置查询参数
│   ├── IdQueryParam / IdStateQueryParam
│   ├── AuthQueryParam / AuthIdQueryParam / AuthIdStateQueryParam
│   ├── AuthPageQueryParam
│   ├── SysCritLogQueryParam / SysDataHistoryQueryParam
├── entity/                            实体类
│   ├── SysCritLog                     系统关键日志
│   └── SysDataHistory                 系统数据历史
├── helper/                            工具类
│   ├── SchemaValidateHelper           Schema 注解校验
│   ├── QueryParamHelper               URL 查询参数构建
│   ├── JsonConfigHelper               JSON 配置构建/校验
│   └── SysDataHistoryHelper           数据历史记录
├── service/                           服务
│   └── SysCritLogStorageService       关键日志异步落库
└── vo/                                值对象
    ├── JsonConfigBox                  JSON 配置盒子
    ├── JsonConfigParam                JSON 配置参数定义
    └── ValidateResult                 校验结果
```
