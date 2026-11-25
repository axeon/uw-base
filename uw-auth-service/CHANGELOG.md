# uw-auth-service CHANGELOG

> 认证服务 SDK（应用注册/状态上报/权限装载）。历史版本变更见 git 历史；自下一版本起在此登记。

## Unreleased

### 6.2.89（2026-11-25）

- **查无数据返回 404**：`GlobalResponseAdvice` 在响应体为 `ResponseData` 且为 `warn` + `uw.dao.data.not.found.warn`（uw-dao 的 `load` / `queryForObject` 查不到、`update` / `delete` 影响 0 行）时，把 HTTP 状态码改为 404，响应体不变——此前这类响应是 200，网关无法区分"探测不存在的资源"与正常请求。
  - 服务间 RPC 接口（路径含 `/rpc/`）除外，保持 200 + warn：RPC 调用方对 4xx 抛异常、拿不到 `ResponseData`，而 `loadUser`、saas 的 `SaasInfoRpc` / `SaasMchCertRpc` / `SysAreaRpc` 等接口会把查无数据的结果直接返回。
  - 标了 `@ResponseAdviceIgnore` 的接口不经过这里，状态码不变。
  - 注意错误码是 `uw.dao.data.not.found.warn`（枚举名按下划线拆成点并转小写再加前缀），不是 `uw.dao.DATA_NOT_FOUND_WARN`。
- **新增应用注册成功事件 `AuthAppRegisteredEvent`**：`MscAppUpdateService` 在注册流程末尾、权限表与应用 ID 初始化之后发布。应用注册是异步的，启动期依赖权限表或应用 ID 的后续动作（如扫描 `@AipLicenseValidator` 建立许可证映射）应监听该事件，不要在应用就绪事件里直接读——那时注册多半尚未完成。监听方异常不影响注册流程。
- 新增 `spring-boot-starter-test`（仅测试）与两组单测：事件发布时机（3 例）、查无数据的状态码处理（4 例）。

- 新增 spring-boot-configuration-processor：IDE 配置补全/校验元数据（编译期注解处理器，无运行时影响）。
