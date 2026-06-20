#### 1. 简述 uw-task 的主要功能和特性

uw-task 是一个分布式任务框架，基于 Spring Boot + RabbitMQ + Redis，支持定时任务（TaskCroner）与队列任务（TaskRunner），并提供运维监控、动态配置与多规则报警。配合服务端 uw-task-center 使用。

1. 基于 Spring Boot 实现，依赖 RabbitMQ（任务派发）与 Redis（全局限速 / Leader 选举 / 发号）。
2. 完全分布式，支持混合云，可指定主机或指定集群（runTarget）运行。
3. 支持定时任务（cron 表达式），支持服务端动态配置，支持全局单例（Leader）运行。
4. 支持队列任务，多维流量控制、错误重试、服务端动态配置。
5. 支持 RPC 风格调用（同步/异步），支持错误重试与服务端动态配置。
6. 支持按失败率、等待超时、运行超时、队列堆积等多规则报警。

#### 2. 服务是否开启执行任务，需要修改什么配置？

配置 `uw.task.enable-registry`：

```yaml
uw:
  task:
    # 任务执行主机设为 true；仅作为任务调用方的服务设为 false
    enable-registry: true
```

`enable-registry=false` 的服务仅能作为任务调用方（`sendToQueue`/`runTask`），不会注册主机、不执行任务、不参与 Leader 选举。

#### 3. 配置属性 task-project 和 run-target 有什么作用？

- **task-project**：任务项目，必须是包名前缀，用于扫描任务注册。框架只扫描该包下的 `TaskCroner`/`TaskRunner`。
- **run-target**：运行目标，用于识别任务执行集群，默认 `default`。

两者一起决定任务配置的同步关系：任务执行主机只拉取与自身 `taskProject + runTarget` 匹配的配置并执行。调用方也可在 `TaskData` 中临时指定 runTarget，将任务路由到特定集群。

#### 4. 设定定时任务和队列任务分别需要继承什么类？实现什么方法？

- **定时任务**：继承 `TaskCroner`，实现 `runTask(TaskCronerLog)`、`initConfig()`、`initContact()`。
- **队列任务**：继承 `TaskRunner<TP, RD>`，实现 `runTask(TaskData<TP, RD>)`、`initConfig()`、`initContact()`。

两类任务都必须加 `@Component` 注解，且类需位于 `task-project` 包路径下。

#### 5. 队列任务的线程模型是怎样的？

TaskRunner 是**单例**（Spring Bean），类的属性被多线程共享。`consumerNum` 决定并发消费线程数，多个线程并发调用同一个实例的 `runTask`。因此：

- `runTask` 内不要使用非线程安全的实例变量（如可变 Map、SimpleDateFormat）。
- 共享状态需自行保证线程安全，或使用方法局部变量 / ThreadLocal。

#### 6. uw-task 的任务入口类是哪个？sendToQueue / runQueue / runTask / runTaskLocal / runTaskAsync 有什么差别？什么样的任务优先本地执行？

入口类是 `TaskFactory`，提供 5 个派发方法（三个维度组合：执行位置 × 是否阻塞调用方 × 是否需要返回值）。

| 方法 | 执行位置 | 阻塞调用方 | 返回结果 | 本机无 runner 时 | 典型用途 |
|---|---|---|---|---|---|
| `sendToQueue` | 远程（入队） | 否 | 无 | 入队（正常） | 标准异步队列任务，不关心结果 |
| `runTask` | AUTO 自动选 | **是** | 有 | 回退远程 RPC | 需立即同步拿结果 |
| `runTaskLocal` | 仅本地 | **是** | 有 | **抛异常** | 强制本机执行，不允许远程 |
| `runTaskAsync` | AUTO 自动选 | 否（Future） | 有（get 时） | 回退远程 RPC | 异步拿结果，注意线程池 |
| `runQueue` | 本地优先，否则入队 | 否 | 无 | 入队 | 高频短任务优化，省 MQ |

**快速选型决策树**：

```
需要执行结果吗？
├─ 否 → 本机有 runner 且高频短耗时？
│        ├─ 是 → runQueue（本地优先，省 MQ）
│        └─ 否 → sendToQueue（标准入队）
│
└─ 是 → 调用方能接受阻塞吗？
         ├─ 能（且要立即拿结果）
         │    ├─ 必须本机跑  → runTaskLocal
         │    └─ 本地/远程都行 → runTask
         │
         └─ 不能（异步） → runTaskAsync（注意线程池上限）
```

**关键区别**：
- `runTask` vs `runTaskLocal`：本机没 runner 时，`runTask` **自动回退远程 RPC**，`runTaskLocal` **直接抛异常**（绝不远程）。
- `sendToQueue` vs `runQueue`：`sendToQueue` **一定入 MQ**（本机有 runner 也不本地跑）；`runQueue` **优先本地**，不行才入队。
- `runTask` vs `runTaskAsync`：前者在调用线程同步等结果（最长 180s），后者提交到 RPC 线程池返回 Future。

**计算量小、耗时短的任务应优先本地执行**（`runQueue` 或 `runTask` 的 AUTO_RPC/LOCAL 模式），降低网络 IO 开销。

> 注意：`runTask`/`runTaskLocal`/`runTaskAsync`/`runQueue` 会向传入的 taskData 写入运行期字段，**不要复用同一对象**。远程模式（`runTask`/`runTaskAsync`）默认 180 秒超时，避免在 Web 请求线程高频同步调用。

#### 7. 如果发现任务不能注册，有可能是什么原因？

1. 任务类上没有 `@Component` 注解——uw-task 通过 Spring 的 `@Component` 扫描识别。
2. 任务类不在 `task-project` 配置的包路径下。
3. `enable-registry` 未设为 true。

#### 8. uw-task 的异常抛出有几种？分别对应什么场景？会造成什么影响？

框架识别三类异常（外加框架自动生成的限速异常），据此设置任务状态：

- **`TaskPartnerException`（合作方异常）→ STATE_FAIL_PARTNER**：合作方/第三方接口错误（HTTP 超时、非 200、限流等）。需程序员手工抛出。**会触发重试**（按 `retryTimesByPartner`）。
- **`TaskDataException`（数据异常）→ STATE_FAIL_DATA**：任务入参数据本身错误（格式非法、缺失、业务规则不满足）。**不触发重试**。
- **其他未捕获异常 → STATE_FAIL_PROGRAM**：程序 bug（空指针等），框架自动捕获。**不触发重试**。
- **限速超时 → STATE_FAIL_CONFIG**：超过流量限制，框架自动生成。**会触发重试**（按 `retryTimesByOverrated`）。

正确抛出异常有助于监控与统计，发现潜在问题。非必要不要在 `runTask` 内吞掉异常。

#### 9. TaskData 类的两个泛型属性分别作为什么？

`TaskData<TP, RD>` 用于队列任务的传值与返回：

- **TP（taskParam）**：执行参数，由调用方设置。
- **RD（resultData）**：任务返回结果。

两个泛型必须与 `TaskRunner` 的泛型完全一致，否则运行时出错。

#### 10. 在哪里设置队列任务的限流类型？请详述不同限流类型的差异和使用场景。

在 `TaskRunnerConfig` 的 `rateLimitType` 中设置。分为本地（进程内 Guava 令牌桶）与全局（Redis 固定窗口）两类：

**本地限速（进程内）**：
- `RATE_LIMIT_LOCAL`：进程共享，所有任务共用一个限速器（**慎用，易卡死**）。
- `RATE_LIMIT_LOCAL_TASK`：按 taskClass 隔离。
- `RATE_LIMIT_LOCAL_TASK_TAG`：按 taskClass + tag 隔离。

**全局限速（Redis）**：
- `RATE_LIMIT_GLOBAL_TAG` / `GLOBAL_HOST` / `GLOBAL_TAG_HOST`：**跨任务共享**（限流 key 不含 taskClass），适用于多个任务对接同一第三方接口、需共享 QPS 上限。
- `RATE_LIMIT_GLOBAL_TASK` / `GLOBAL_TASK_HOST` / `GLOBAL_TASK_TAG` / `GLOBAL_TASK_TAG_HOST`：按 taskClass 隔离。

**选择建议**：
- 单实例部署 → 本地限速（开销低）。
- 多实例、对接同一第三方接口需共享配额 → `GLOBAL_TAG` 系列。
- 多实例、每个任务独立配额 → `GLOBAL_TASK*` 系列。
- 全局限速建议 `rateLimitTime` 不低于 5 秒，降低 Redis IO 开销。

#### 11. uw-task 什么情况下使用全局限流器？什么情况下使用本地限流器？全局限流器的限制是什么？

- **全局限流器**基于 Redis，跨进程共享配额；**本地限流器**基于 Guava RateLimiter，仅进程内生效。资源消耗上，全局远大于本地。
- 确定仅单实例运行的项目，建议用本地限流器，大幅降低开销。
- 多实例需要共享配额时用全局限流器。
- 全局限流器建议检测窗口（`rateLimitTime`）不低于 5 秒，减小网络 IO。
- 全局限流器在 Redis 不可用时会降级放行（优先保证任务可用），短暂抖动期间可能超限。

#### 12. uw-task 的运行模式（runType）有哪几种？

- `RUN_TYPE_LOCAL`：本地运行，不受流控限制。
- `RUN_TYPE_GLOBAL`：全局异步运行（入队），受流控限制。
- `RUN_TYPE_GLOBAL_RPC`：全局同步 RPC，不受流控限制。
- `RUN_TYPE_AUTO_RPC`（默认）：自动判定，本机有 runner 则本地运行，否则走 GLOBAL_RPC。

#### 13. uw-task 的重试机制是怎样的？

按 `retryType`（默认 `AUTO`）与失败类型决定：

- `STATE_FAIL_PARTNER`（合作方异常）按 `retryTimesByPartner` 重试。
- `STATE_FAIL_CONFIG`（超限流）按 `retryTimesByOverrated` 重试。
- `STATE_FAIL_PROGRAM` / `STATE_FAIL_DATA` 不重试。

设 `retryTimes = N` 时，**总计执行 N+1 次**（1 次初始 + N 次重试）。重试延时按执行轮次线性递增。LOCAL/RPC 模式在本地重跑，GLOBAL 模式重新入队。

#### 14. uw-task 的延时队列如何使用？

将 `TaskRunnerConfig` 的 `delayType` 设为 `1`（`TYPE_DELAY_ON`）开启。发送带 `taskDelay` 的任务时，框架会投递到 TTL 队列，到期后经死信转发到业务队列。

注意：基于死信队列实现，存在**长延时任务阻塞短延时任务**的问题。不推荐大范围使用——小负载可轮询数据库，大负载可用 `uw-cache: GlobalSortedSet`。

#### 15. 多实例任务如何配置？

- **TaskCroner** 通过 `taskParam` 区分多实例（同一 taskClass 不同 taskParam = 不同实例）。
- **TaskRunner** 通过 `taskTag` 区分多实例。

多实例唯一性由服务端按三元组（`taskClass + 区分维度 + runTarget`）保证。无法精确匹配时框架会宽松匹配最合适的配置。

#### 16. 全局单例定时任务（SINGLETON）如何保证唯一执行？

`TaskCronerConfig.RUN_TYPE_SINGLETON` 的任务通过 Redis Leader 锁选举唯一 Leader 主机执行，其余主机跳过。Leader 任期 90 秒、续期 60 秒，掉线后最长 90 秒由其他主机接管。Redis 短暂抖动期间原 Leader 按本地快照继续执行（接受极小概率双跑），**关键业务需配合幂等**。
