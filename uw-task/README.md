[TOC]

## 简介

uw-task 是一个分布式任务框架，基于 Spring Boot + RabbitMQ + Redis 实现，可快速构建分布式任务体系，支持定时任务（TaskCroner）与队列任务（TaskRunner，含延时语义），并提供任务运维监控、动态配置与多规则报警。

**双协议版（5.2.0 重写，现行 5.3.2）**：AMQP 0-9-1 / 1.0（默认 091，仅对端不提供 091 时走 1.0）、队列形态 classic/quorum 可选、RabbitMQ 4.4+ 原生延迟消息、私有 Redis 连接（详见文末「双协议与队列形态」章）。

配合服务端 [uw-task-center](../../uw-task-center) 使用：任务执行主机向 task-center 注册、上报状态、拉取动态配置；task-center 负责配置管理、统计汇总与报警分发。

## 主要特性

1. 基于 Spring Boot 实现，依赖 RabbitMQ（任务派发）与 Redis（全局限速 / Leader 选举 / 序列发号）。
2. **AMQP 双协议自动判定**：启动期探测（协议头握手+版本解析，纯 amqp 端口，无管理端口依赖），默认走 091，仅对端不提供 091 而提供 1.0 时才走 1.0——自建/托管全兼容，零配置。
3. 完全分布式，支持混合云，可指定主机或指定集群（runTarget）运行。
4. 支持定时任务（cron 表达式），支持服务端动态配置，支持全局单例（Leader）运行。
5. 支持队列任务，提供多维流量控制（本地/全局、按进程/主机/TAG/TASK 组合）、错误重试、服务端动态配置。
6. 延时任务全自动化（5.2.0）：`taskDelay > 0` 即延时任务，无需独立类型——RabbitMQ 4.4+（091 / 1.0 均可）且队列形态为 quorum 时走 **MQ 消息级原生延迟**（x-opt-delivery-time，到期自动可见即执行）；否则自动降级 **Redis zset 搬运**（到期自动转投目标队列）。无队头阻塞、长延时不阻塞短延时、重启不丢存量。
7. 支持 RPC 风格调用（同步/异步），支持错误重试与服务端动态配置。
8. 支持按失败率、等待超时、运行超时、队列堆积、延迟超时等多规则的任务报警。

## 版本要求

- JDK 25+
- Spring Boot 4.2.x
- RabbitMQ 3.8+（可选：不配置时为仅定时任务档；1.0 需 4.0+，原生延迟需 4.4+，启动期自动判定）
- Redis 8.x+（必需：任务 id 发号、Leader 选举、延时搬运、全局限速）

## maven 引入

```xml
<dependency>
    <groupId>pub.xili</groupId>
    <artifactId>uw-task</artifactId>
    <version>${version}</version>
</dependency>
```

引入后由 `UwTaskAutoConfiguration` 自动装配，无需额外 `@EnableXxx`；对外只有一个 bean：`TaskFactory`（内部组件不进 Spring 容器）。
**运行档位**（装配时判定一次；**Redis 是必需项**——任务 id 发号、Leader 选举、延时搬运、全局限速都依赖它，MQ 只有队列任务的收发与延时载体需要）：

| 档位 | 条件 | 行为 |
|---|---|---|
| 完整 | 配置了 `uw.task.redis.host` 与 `uw.task.rabbitmq.host`（或 `uw.task.rabbitmq.addresses`） | 全部功能可用 |
| 仅定时任务 | 只配置了 `uw.task.redis.host` | 定时任务、队列任务的本地执行（`runTask`/`runTaskLocal`/`runTaskAsync` 落到本机 runner）可用；`sendToQueue`、远程 RPC、延时提交直接抛 `TaskRuntimeException`（未配置 MQ），不进入重试 |
| 不初始化 | 未配置 `uw.task.redis.host`，或执行主机（`enable-registry=true`）未配置 `uw.task.task-project` | 打一条 WARN 并跳过整个装配（WARN 写明缺的是哪一项）；不使用任务框架的应用无需任何配置 |

**参数校验口径**（检测逻辑集中在 `UwTaskProperties`：必填项 `missingRequiredSetting`、档位 `resolveRuntimeMode`、可选项取值由 getter 回退）：

| 参数 | 不合法时 |
|---|---|
| `task-project`（仅 `enable-registry=true` 时必填） | WARN 并不初始化；运行期被热更新成空白则忽略并保持原值 |
| `task-rpc-max-thread-num` / `task-local-max-thread-num` ≤ 0 | WARN，按默认值 60 生效 |
| `task-queue-retry-delay` / `task-rpc-retry-delay` < 0 | WARN，按默认值（2000 / 100 毫秒）生效 |
| `run-target` / `task-center-host` 空白 | WARN，按默认值（`default` / `http://uw-task-center`）生效 |

**任务配置的取值规整**（规则集中在外部类 `TaskConfigValidator`）：配置进入框架的唯一入口是 `TaskMetaInfoManager.setRunnerConfig` / `setCronerConfig`（业务代码的 `initConfig()` 返回值、task-center 下发的配置都经过这里），配置进入框架时在那里调一次 `TaskConfigValidator.normalize`，非法取值回退默认值，每份配置只打一条 WARN，列出"字段=原值→默认值"。默认配置在首次上传 task-center 之前也会规整一次（center 里存的就是合法值）。同一个类还提供只报告不修改的 `validate(config)`，供 task-center 保存配置时拒绝非法输入：

| 配置 | 不合法时 |
|---|---|
| `queueType`（只认 0=classic / 1=quorum）、`rateLimitType`、`runType` | WARN，回默认值（`queueType` 回 quorum） |
| `consumerNum` / `prefetchNum` < 1 | WARN，回默认值（1 / 10） |
| `rateLimitValue` / `rateLimitTime` < 1 | WARN，回默认值（10 / 1） |
| 重试次数、各项告警阈值、`rateLimitWait`、`logLimitSize` < 0 | WARN，回 0（`rateLimitWait` 回 30） |
| `logLevel` 不在 -1~3 | WARN，回 0（RECORD） |
| `runTarget` 空白 | WARN，回 `default` |

不在 setter 里做：uw-dao 读实体直接写字段、不经 setter，数据库加载的配置会绕过 setter。uw-task-center 保存侧目前尚未接入 `validate`。`id`、`taskClass`、`state` 不处理；cron 表达式非法仍由调度容器拒绝注册，不会悄悄换成默认表达式。

## 应用配置

直接使用 Spring Boot 的 `application.yml`：

```yaml
uw:
  task:
    # 是否启用任务注册（任务执行主机设为 true；仅作为任务调用方的服务设为 false）
    enable-registry: true
    # 任务管理服务器（uw-task-center）地址
    task-center-host: http://localhost:8080
    # 任务项目，须为任务类的包名前缀：本机只注册并执行 taskClass 以此开头的任务（首次上传、本地降级启动、服务端配置过滤同口径）；同时作为延时 zset 与 Leader 锁的隔离键。执行主机必填
    task-project: com.demo.task
    # 运行目标，用于识别任务执行集群，默认 default
    run-target: default
    # runTaskAsync 在途执行并发上限（虚拟线程背压闸门，满时提交线程自行执行）
    task-rpc-max-thread-num: 60
    # runQueue 本地执行并发上限（虚拟线程背压闸门，缓冲也满时溢出到 MQ）
    task-local-max-thread-num: 60
    # 队列任务重试延时（毫秒），默认 2 秒
    task-queue-retry-delay: 2000
    # RPC 任务重试延时（毫秒），默认 100 毫秒
    task-rpc-retry-delay: 100

    # RabbitMQ（task 使用独立的连接工厂，与业务 MQ 隔离；协议启动期自动判定、默认 091，无需配置）
    rabbitmq:
      host: 127.0.0.1
      port: 5672
      username: guest
      password: guest
      # 集群（可选）：配置 addresses 后 host/port 被忽略，探测与两协议连接均遍历地址列表
      # addresses: node1:5672,node2:5672,node3:5672
    # Redis（task 使用独立的连接工厂）
    redis:
      database: 0
      host: 127.0.0.1
      port: 6379
      password: password
      lettuce:
        pool:
          max-active: 20
          max-idle: 8
          max-wait: -1ms
          min-idle: 0
      timeout: 30s
```

> 说明：`enable-registry=false` 的服务仅能作为任务调用方（`sendToQueue`/`runTask`），不会注册主机、不执行任务、不参与 Leader 选举。

## 定时任务（TaskCroner）

定时任务使用 cron 表达式定时执行，需继承 `TaskCroner` 并实现 `runTask`、`initConfig` 两个方法（`initContact` 默认返回空联系人，可选覆写）。

```java
@Component
public class DemoCronTask extends TaskCroner {

    /**
     * 运行任务。返回值会记录到执行日志（TaskCronerLog）的 resultData 字段。
     * 业务异常请按需抛出 TaskPartnerException / TaskDataException（见"任务内异常处理"）。
     */
    @Override
    public String runTask(TaskCronerLog taskCronerLog) throws Exception {
        logger.info("just test for cron task!");
        return "";
    }

    /**
     * 初始化定时任务配置（首次注册时上传到 task-center，之后以服务端动态配置为准）。
     */
    @Override
    public TaskCronerConfig initConfig() {
        TaskCronerConfig config = new TaskCronerConfig();
        config.setTaskName("测试定时任务");
        config.setTaskDesc("这是一个测试定时任务");
        config.setTaskCron("*/5 * * * * ?");          // cron 表达式
        config.setRunType(TaskCronerConfig.RUN_TYPE_SINGLETON); // 全局单例运行（仅 Leader 执行）
        config.setAlertFailRate(10);                  // 总失败率报警阈值（百分比）
        config.setAlertFailProgramRate(10);           // 程序失败率
        config.setAlertFailPartnerRate(10);           // 接口失败率
        config.setAlertWaitTimeout(10000);            // 等待超时报警（ms）
        config.setAlertRunTimeout(1000);              // 运行超时报警（ms）
        return config;
    }

    /**
     * 初始化联系人信息（首次注册时上传，用于报警通知）。
     */
    @Override
    public TaskContact initContact() {
        return new TaskContact("姓名", "手机号", "邮箱", "微信", "im", "notifyUrl", "备注");
    }
}
```

## 队列任务（TaskRunner）

实现 `TaskRunner` 的 `runTask` 方法，通过泛型指定入参与返回类型。

```java
@Component
public class DemoTask extends TaskRunner<DemoTaskParam, String> {

    private static final Logger log = LoggerFactory.getLogger(DemoTask.class);

    @Override
    public String runTask(TaskData<DemoTaskParam, String> taskData) throws Exception {
        log.info("DemoTask: {},{}", taskData.getTaskParam().getId(), taskData.getTaskParam().getName());
        return "ok";
    }

    @Override
    public TaskRunnerConfig initConfig() {
        TaskRunnerConfig config = new TaskRunnerConfig();
        config.setTaskName("测试队列任务");
        config.setTaskDesc("测试队列任务描述");
        // 限速配置（详见"流量控制功能"）
        config.setRateLimitType(TaskRunnerConfig.RATE_LIMIT_NONE);
        config.setRateLimitTime(1);
        config.setRateLimitValue(10);
        config.setRateLimitWait(60);
        // 重试次数（设为 N 时，总计执行 N+1 次：1 次初始 + N 次重试）
        config.setRetryTimesByPartner(0);       // 合作方异常重试次数
        config.setRetryTimesByOverrated(0);     // 超限流异常重试次数
        // 消费并发
        config.setConsumerNum(5);               // 消费者并发数
        config.setPrefetchNum(50);              // 预取数：默认 10；quorum 队列吞吐敏感时建议 ≥50（逐批确认要等 WAL 落盘）
        // 报警阈值
        config.setAlertFailRate(10);
        config.setAlertFailProgramRate(10);
        config.setAlertFailPartnerRate(10);
        config.setAlertFailConfigRate(10);
        config.setAlertQueueOversize(1000);     // 队列堆积报警
        config.setAlertQueueTimeout(600000);    // 队列排队超时报警（ms）
        config.setAlertWaitTimeout(10000);
        config.setAlertRunTimeout(1000);
        return config;
    }

    @Override
    public TaskContact initContact() {
        return new TaskContact("姓名", "手机号", "邮箱", "微信", "im", "notifyUrl", "备注");
    }
}
```

## 延时任务（taskDelay，全自动化）

5.2.0 起**没有独立的延迟任务类型**（TaskDelayer/delayTask 已退役）：任何 TaskRunner 任务设置 `TaskData.taskDelay > 0` 即为延时任务，投递入口统一且自动路由：

```java
// 三种写法对延时任务完全等同（内部统一走 sendToQueue 的自动路由）
taskFactory.sendToQueue(taskData);   // taskData.taskDelay = 5000
taskFactory.runQueue(taskData);      // 同上
taskFactory.runTask(taskData);       // 同上（延时任务放弃同步 RPC 语义，异步投递后立即返回）
```

**路由规则（启动期探测一次，运行期不变）**：

| 条件 | 执行载体 |
|---|---|
| RabbitMQ ≥ 4.4（091 / 1.0 均可） 且 `queueType = QUEUE_TYPE_QUORUM` | **MQ 原生延迟**：消息级 `x-opt-delivery-time`（绝对时间戳），到期自动可见由消费者执行——零搬运、精度 ~0.5s |
| 其余（低版本 broker/classic 队列） | **Redis zset 搬运**：写 project 级单一 zset `uw-task-delay:{task-project}`（member=Kryo(TaskData)，score=到期时间），搬运器每轮 Lua 原子 pop 到期任务，经 sendToQueue 自动转投目标 MQ 队列（队列名由 taskClass 重新推导），由对应 runner 消费执行；搬运器为 Leader 单例执行（选主切换重叠窗口由 Lua 原子 pop 去重），MQ 故障时写回 zset 延迟 5s 重试（上限 20 次，超限放弃并记 error）。zset 积压量随主机上报（`delayBacklog`） |

延时任务的限速/重试/日志/统计全部由 TaskRunner 消费端统一承担（与即时任务完全一致），无独立的 Delayer 配置/监听器/日志类型。

> 启动期探测只在启动时执行一次：启动时 MQ 不可达会判为 091 且原生延迟关闭（延时任务走 zset 搬运），MQ 恢复后需重启应用才会回到 1.0 并启用原生延迟。

## 任务内异常处理

为支持监控与重试，`runTask` 抛出的异常分三类，框架据此设置任务状态：

| 异常 | 状态 | 是否重试 | 场景 |
|---|---|---|---|
| `TaskPartnerException` | `STATE_FAIL_PARTNER` | 是（按 `retryTimesByPartner`） | 合作方/第三方接口错误（超时、错误码、限流等） |
| `TaskDataException` | `STATE_FAIL_DATA` | 否 | 任务入参数据错误（格式非法、缺失、业务规则不满足） |
| 其他未捕获异常 | `STATE_FAIL_PROGRAM` | 是（按 `retryTimesByProgram`，默认 0=不重试） | 程序异常（bug、空指针等），框架自动捕获 |
| （限速超时） | `STATE_FAIL_CONFIG` | 是（按 `retryTimesByOverrated`） | 超过流量限制，框架自动生成 |

正确抛出异常有助于发现潜在问题。非必要不要在 `runTask` 内吞掉异常——不捕获的异常会被框架归为程序异常并记录。

## 任务派发（TaskFactory）

注入 `TaskFactory` 后可选择不同的派发方式：

```java
@Autowired
private TaskFactory taskFactory;
```

### 1. `sendToQueue(taskData)` — 异步入队

完全异步，无返回值。任务投递到 RabbitMQ 队列，由具备匹配 runner 的远端主机消费。

> 需"X 时间后执行"直接给任务设置 `taskDelay`（自动路由），无需独立类型。

### 2. `runQueue(taskData)` — 本地队列优先

高频短任务优先在本地线程池执行（减少 MQ 压力）；线程池满时自动降级入队。带 `taskDelay` 的任务直接入队。

> 实现细节：本地队列线程**只走本地执行或降级入队，绝不走同步 RPC**，避免阻塞。

### 3. `runTask(taskData)` — 同步执行

根据 `runType` 判定：`AUTO_RPC`（默认）自动选择本地/远程；`LOCAL` 强制本地；其他走全局同步 RPC（默认 180s 超时）。**会阻塞调用线程**。

### 4. `runTaskLocal(taskData)` — 强制本地同步

仅本地执行，本机无匹配 runner 时抛 `TaskRuntimeException`（不回退入队）。同样阻塞调用线程。

### 5. `runTaskAsync(taskData)` — 异步执行

返回 `Future<TaskData>`，通过 `future.get()` 获取结果。在途执行并发上限为 `task-rpc-max-thread-num`（默认 60，虚拟线程，仅计数不占平台线程），超过时由提交线程自行执行（CallerRuns 背压）。

### 方法对比与快速选型

五个方法本质是三个维度的组合：**执行位置（本地/远程）× 是否阻塞调用线程 × 是否需要返回值**。

| 方法 | 执行位置 | 阻塞调用方 | 返回结果 | 本机无 runner 时 | 典型用途 |
|---|---|---|---|---|---|
| `sendToQueue` | 远程（入队） | 否 | 无 | 入队（正常） | 标准异步队列任务，不关心结果 |
| `runTask` | AUTO 自动选 | **是** | 有 | 回退远程 RPC | 需立即同步拿结果 |
| `runTaskLocal` | 仅本地 | **是** | 有 | **抛异常** | 强制本机执行，不允许远程 |
| `runTaskAsync` | AUTO 自动选 | 否（Future） | 有（get 时） | 回退远程 RPC | 异步拿结果，超过并发上限时提交线程自行执行 |
| `runQueue` | 本地优先，否则入队 | 否 | 无 | 入队 | 高频短任务优化，省 MQ |

**选型决策树**：

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
         └─ 不能（异步） → runTaskAsync（注意在途并发上限）
```

> **重要**：`runTask`/`runTaskLocal`/`runTaskAsync`/`runQueue` 会向传入的 taskData 写入 id/queueDate/runType 等运行期字段，**请勿在多次调用间复用同一 taskData 对象**。远程模式（`runTask`/`runTaskAsync`）默认 sendAndReceive 超时 180 秒，**避免在 Web 请求线程高频同步调用**，以免耗尽 Tomcat 线程。

### 运行类型（runType）

| 常量 | 值 | 说明 |
|---|---|---|
| `RUN_TYPE_LOCAL` | 1 | 本地运行，不受流控限制 |
| `RUN_TYPE_GLOBAL` | 3 | 全局异步运行（入队），受流控限制 |
| `RUN_TYPE_GLOBAL_RPC` | 5 | 全局同步 RPC，不受流控限制 |
| `RUN_TYPE_AUTO_RPC` | 6 | 自动判定（默认），本机有 runner 则本地，否则走 GLOBAL_RPC |

### 6. `getListenerManager()` — 事件监听

`taskFactory.getListenerManager()` 是注册监听器的唯一入口（`TaskListenerManager` 不是 bean）：

| 监听器 | 回调 | 时点 |
|---|---|---|
| `TaskRunnerListener` | `onPreExecute` / `onPostExecute(TaskData)` | 队列任务每轮执行前/后（含 LOCAL/RPC/GLOBAL；执行后结果、状态、耗时已就位；限速等待超时未执行的任务只有 onPostExecute） |
| `TaskCronerListener` | `onPreExecute` / `onPostExecute(TaskCronerLog)` | 定时任务每轮执行前/后 |
| `TaskSendListener` | `onDelaySubmit` / `onSendFail` | 延时任务写入 zset 时 / 即时投递重试耗尽（或被中断）时 |

监听器内抛出的异常被框架吞掉（仅记日志），不影响任务流程。

## TaskData 说明

`TaskData<TP, RD>` 是任务传值与返回容器。`TP`/`RD` 必须与 `TaskRunner` 的泛型完全一致，否则运行时出错。推荐使用 builder：

```java
TaskData<MyParam, MyResult> data = TaskData.<MyParam, MyResult>builder(DemoTask.class)
        .taskParam(new MyParam(1, "x"))
        .taskTag("tag-1")
        .runTarget("default")
        .rateLimitTag("api-config-100")
        .build();
```

关键字段：

| 字段 | 设置方 | 说明 |
|---|---|---|
| `taskClass` | 调用方（必填） | 要执行的 TaskRunner 全限定名 |
| `taskParam` | 调用方（必填） | 执行参数 |
| `taskTag` | 调用方 | 任务标签，用于多实例区分 |
| `runTarget` | 调用方 | 运行目标，默认 default |
| `rateLimitTag` | 调用方 | 限速 TAG（推荐设为接口配置 ID） |
| `taskDelay` | 调用方 | 延迟毫秒数：>0 即延时任务，载体自动路由（quorum+RabbitMQ 4.4+→MQ 原生延迟，091 / 1.0 均可；否则→Redis zset 搬运） |
| `refId`/`refSubId`/`refTag` | 调用方 | 关联信息，用于第三方统计 |
| `refObject` | 调用方 | 关联对象，仅在调用方进程内有效（不入库、不跨 MQ/Redis 序列化） |
| `id`/`queueDate`/`consumeDate`/`runDate`/`finishDate`/`runType`/`ranTimes`/`state`/`errorInfo`/`resultData` | 框架 | 由框架自动设置 |

任务状态（`state`）：`0=未知 1=成功 2=程序错误 3=配置错误 4=三方接口错误 5=数据错误`。

## 流量控制功能

在 `TaskRunnerConfig` 中配置限速类型与参数。**全局限速器基于 Redis 固定窗口，建议检测窗口（`rateLimitTime`）不要低于 5 秒**，以减小网络 IO 开销。

### 限速类型（rateLimitType）

分为本地（进程内固定窗口）与全局（Redis 固定窗口）两大类，两者窗口边界都对齐墙钟网格：

| 常量 | 值 | 限流 key 维度 | 说明 |
|---|---|---|---|
| `RATE_LIMIT_NONE` | 0 | — | 不限速 |
| `RATE_LIMIT_LOCAL` | 1 | 进程（共享） | 本地进程，所有任务共用一个限速器 |
| `RATE_LIMIT_LOCAL_TASK` | 2 | 进程 + taskClass | 本地按任务隔离 |
| `RATE_LIMIT_LOCAL_TASK_TAG` | 3 | 进程 + taskClass + tag | 本地按任务+TAG 隔离 |
| `RATE_LIMIT_GLOBAL_HOST` | 4 | host | **跨任务共享**：按主机限速 |
| `RATE_LIMIT_GLOBAL_TAG` | 5 | tag | **跨任务共享**：按 TAG 限速（对接同一第三方接口的多个任务共享配额） |
| `RATE_LIMIT_GLOBAL_TASK` | 6 | taskClass | 按任务隔离 |
| `RATE_LIMIT_GLOBAL_TAG_HOST` | 7 | tag + host | **跨任务共享** |
| `RATE_LIMIT_GLOBAL_TASK_HOST` | 8 | taskClass + host | 按任务+主机隔离 |
| `RATE_LIMIT_GLOBAL_TASK_TAG` | 9 | taskClass + tag | 按任务+TAG 隔离 |
| `RATE_LIMIT_GLOBAL_TASK_TAG_HOST` | 10 | taskClass + tag + host | 全维度隔离 |

> **`GLOBAL_TAG`/`GLOBAL_HOST`/`GLOBAL_TAG_HOST` 是跨任务共享配额**（限流 key 不含 taskClass）：多个不同任务只要 TAG/HOST 相同即共享同一限流池，适用于多个任务对接同一第三方接口、需共享 QPS 上限的场景。`GLOBAL_TASK*` 系列则按任务隔离。

### 限速参数

| 参数 | 默认 | 说明 |
|---|---|---|
| `rateLimitValue` | 10 | 窗口内配额上限（次数） |
| `rateLimitTime` | 1 | 窗口长度（秒） |
| `rateLimitWait` | 30 | 触发限速时最长等待秒数，超时仍不足则放弃任务（标记 STATE_FAIL_CONFIG） |
| `retryTimesByOverrated` | 0 | 超限流重试次数（N → 总计执行 N+1 次） |

> **限速只对经 MQ 消费的队列任务生效**（`sendToQueue`/`runQueue` 降级入队的任务，以及延时任务到期转投后的消费）。以下路径**不做任何限速**：
> - `runTask` / `runTaskAsync` 走远程 RPC 模式（服务端执行时按 RPC 任务处理，跳过限速）；
> - `runTask` / `runTaskLocal` / `runTaskAsync` / `runQueue` 在本机执行的任务（LOCAL 模式）。
>
> "全局"指配额跨进程共享（Redis），不是"所有执行路径都受限"。需要限速的任务请走 `sendToQueue`。等待时长精确等于 `rateLimitWait`（不会睡过窗口）。

### 并发与背压（per-queue 单一来源）

背压控制**按队列独立设置**（TaskRunnerConfig，center 动态下发、tune 在线生效）：

- `consumerNum`：**执行并发数**（091=消费者线程弹性伸缩；1.0=Semaphore 上限 + 每任务一个虚拟线程 + pause/unpause 消费者，两协议语义对齐；执行线程名均为 `{队列名}-N`）
- `prefetchNum`：在途未确认窗口/缓冲（091=未 ack 上限；1.0=credit，settle 在执行完成后才释放——1.0 分发经 credit 可超前投递，执行通道把并发收敛回 consumerNum）

**全局并发线程数 = Σ(各队列 consumerNum)**——多队列规模下由各队列配置面规划总量贴合下游容量（配置治理问题，框架不设全局暗闸：全局信号量会造成热点队列饿死其他队列的跨队列干扰，与每任务独立配置并发的模型冲突）。

虚拟线程只解决"等待不占平台线程"（限速 sleep/park 零成本），**不做背压**——背压由 prefetch/credit 承载（broker 停发），限速超限走 CONFIG 重投退避。

## 重试机制

任务失败后，按 `retryType`（默认 `AUTO`）与失败类型决定是否重试：

- `STATE_FAIL_PARTNER`（合作方异常）：按 `retryTimesByPartner` 重试。
- `STATE_FAIL_CONFIG`（超限流）：按 `retryTimesByOverrated` 重试。
- `STATE_FAIL_PROGRAM`：按 `retryTimesByProgram` 重试（默认 0=不重试）。
- `STATE_FAIL_DATA`：不重试。

设 `retryTimes = N` 时，**总计执行 N+1 次**（1 次初始 + N 次重试）。重试延时按执行轮次线性递增：队列任务为 `ranTimes * taskQueueRetryDelay`，LOCAL 任务为 `ranTimes * taskRpcRetryDelay`。LOCAL 模式在本地重跑，GLOBAL 模式重新入队；远程 RPC 模式不在服务端重试（失败结果直接返回调用方）。

延时任务（taskDelay>0）的重试语义与队列任务完全一致（同一消费端执行）：重试延时同为 `ranTimes × taskQueueRetryDelay` 线性递增（含超限流 `STATE_FAIL_CONFIG`，`rateLimitWait` 只决定单轮等待配额的时长），经延时路由重新投递。重试次数分别受 `retryTimesByPartner` / `retryTimesByProgram` / `retryTimesByOverrated` 控制（均默认 0=不重试）。

## 多实例运行

同一套定时任务/队列任务存在多个并发运行实例时，通过服务器端多份配置实现：

- **TaskCroner** 通过配置中的 `taskParam` 标识多实例。多数场景用空值（默认），特定用户用特定 ID 作为 taskParam。
- **TaskRunner** 通过 `taskTag` 标识多实例。发送任务时指定 taskTag/runTarget。
- 若指定的 taskTag 无精确匹配的服务端配置，框架回退到同 taskClass、同 runTarget 的无 taskTag 配置（仍无则投递抛异常）。

多实例唯一性由服务端按三元组（`taskClass + 区分维度 + runTarget`）保证：RPC 自动注册时按三元组幂等去重，ops 端新增/修改时校验重复。

## 运行目标 RunTarget

`runTarget` 是非常重要的参数，与 `taskProject` 一起决定任务配置的同步关系：

- 任务执行主机只注册、执行 taskClass 以自身 `taskProject` 为包名前缀（带点号边界，`saas.mall` 不会匹配 `saas.mall2`）且 `runTarget` 匹配的任务：服务端下发的配置按此过滤，首次上传与 center 不可用时的本地降级启动也只遍历前缀内的任务 bean。
- 调用方可在 `TaskData` 中指定 runTarget，将任务路由到特定服务器集群。
- 默认 runTarget 为 `default`。

## 全局单例运行（Leader 选举）

`TaskCronerConfig.RUN_TYPE_SINGLETON` 的定时任务通过 Redis Leader 锁保证全局唯一执行：仅由 Leader 主机执行，其余主机跳过。

- Leader 任期 90 秒，续期间隔 60 秒；Leader 掉线后最长 90 秒由其他主机接管。
- Redis 短暂抖动期间，原 Leader 按本地快照继续执行（接受极小概率的双跑）。**关键业务需配合幂等**。

## center 不可用降级与恢复

uw-task 客户端在 uw-task-center 不可用时，两类任务（TaskCroner / TaskRunner）均可用 `initConfig` 本地默认配置继续运行；center 恢复后自动同步服务端动态配置。

### 降级触发（lazy，非启动即降级）

客户端启动后 `updateConfig` 首次拉取 center 配置（0s 延迟）：

- **拉取成功** → 用 center 配置启动任务，**不降级**（零浪费，不在启动阶段叠加重建）
- **拉取失败 + 首次（firstRun）** → 用 `initConfig` 本地默认配置启动（降级模式）
- **拉取成功但默认配置上传 task-center 失败** → 先用本地默认配置启动（配置 id 为 0，任务可执行），打 WARN 并每 60 秒重试上传；补传成功后自动换成 center 配置

判定依据是「是否曾成功拉取过配置」（firstRun），**不是失败次数**。一旦成功过，运行中再故障走 fail-fast（保留上次 center 配置继续跑），不重新降级。

### center 恢复同步

center 恢复后，`updateConfig`（每 60s）拉取成功 → 用 center 配置覆盖降级配置：

- **TaskCroner**：按 configKey 覆盖（停旧调度 + 启新），不清内存 map
- **TaskRunner**：销毁降级 listener（MQ 未 ack 消息自动重新入队）+ 用 center 配置重建 listener

### 不丢任务 / 不丢数据保障

| 任务 | 切换方式 | 保障 |
|---|---|---|
| TaskCroner | `stop` 用 `cancel(false)` 不中断在途 | 正在执行的 croner 跑完，新调度接管 |
| TaskRunner | 销毁 listener → 重建 | RabbitMQ 未 ack 消息重新入队（quorum 队列持久化；classic 队列消息不写盘） |
| 延时搬运器 | Leader 失去身份时 mover 完成当前批次后退出 | 任务留在 project 级 zset；新 Leader 接续搬运（Lua 原子 pop 去重） |

> **TaskCroner.runTask 需幂等**：配置覆盖/降级切换用 `cancel(false)` 让上一次执行跑完 + 新调度接管。若 cron 间隔 < 单次执行时长，old 与 new 可能短暂重叠并发执行——runTask 必须幂等（定时任务本应如此）。

### enableRegistry 与降级

`enable-registry=false` 的主机（仅任务调用方）不执行任务、不参与降级启动与 Leader 选举。

## 常见问题

**任务不能注册，无法启动任务。**

1. 任务类上有没有设置 `@Component` 注解？uw-task 通过 Spring 的 `@Component` 扫描识别并注入。
2. 任务类是否在 `task-project` 配置的包路径下？
3. `enable-registry` 是否设为 true？

**队列堵成狗，任务没按限速执行。**

是不是把限速类型设成了 `RATE_LIMIT_LOCAL`（进程内共享）？这样所有任务会共用一个限速器，必然卡死。认真阅读"流量控制功能"，按需选择按任务/全局隔离的限速类型。

**关于 uw-task 的延时队列任务**

uw-task 5.2.0 起延时与队列任务统一为 TaskRunner 一种类型（`taskDelay > 0` 即延时，自动路由 MQ 原生延迟 / Redis zset 搬运），详见「延时任务（taskDelay，全自动化）」章节。

TaskRunner 的队列任务延时：quorum 队列 + RabbitMQ 4.4+ 下为 MQ 消息级原生延迟（091 / 1.0 均适用）（无队头阻塞，任意时长可用）；其余走 zset 搬运自动转投。延时任务统一用 taskDelay（自动路由）。

**调用方线程会被阻塞的场景（重要）**

uw-task 的部分操作是同步阻塞的，直接在调用线程执行，务必避免在 Web 请求线程等敏感线程中高频/大批量调用：

1. `sendToQueue`：当 RabbitMQ 出现 `channelMax limit is reached` 等瞬时资源错误时，框架会在调用线程内重试（最多 20 次，退避递增），退避累计约 19 秒，另加每次尝试自身的连接耗时（两种协议的同步异常都适用；1.0 发布后的 broker 拒收在后台线程重试，不阻塞调用方）。
2. `runTask` / `runTaskAsync` 远程模式：通过 `sendAndReceive` 同步 RPC，默认 180 秒超时；连接类异常在调用线程内重试（最多 20 次，退避累计约 10 秒），超时不重试。回复通道：两种协议都走 Direct Reply-To（不建回复队列，回复至多一次、不落盘，调用方断线即丢）。1.0 走 `RabbitAmqpTemplate.sendAndReceive` 官方路径：模板每次调用新建并关闭一个 `Requester`，各多一次网络往返，单次 RPC 比 091 慢（VPN 实测约 89ms vs 32ms；同机房约为两次额外往返的量级），等待上限已把模板的 `publishTimeout`/`completionTimeout` 对齐到 180 秒。远程 RPC 任务的 `runTask` 实现必须幂等（重试窗口内消息可能已被执行）。
3. 队列任务全局限速（`RATE_LIMIT_GLOBAL_*`）：触发限速时，消费者线程会在 `rateLimitWait` 内轮询等待配额，期间该消费者不消费下一条消息。若限速配置激进（等待长、配额小），会导致并发消费者数虚高却实际空转、消息堆积。

建议：高频调用走 `runQueue`（本地队列线程池执行，满则降级入队）；限速场景优先评估 `rateLimitWait` 与并发消费者数的比例，必要时改用进程内限速（`RATE_LIMIT_LOCAL*`）或调小等待时间。

## 双协议与队列形态（5.2.0）

### 协议自动判定（启动期一次定死，运行期不切换）

探测全部经 **amqp 端口本身**完成（`uw.task.rabbitmq.port`），**与管理端口彻底解耦**：

| 检测项 | 手段 |
|---|---|
| AMQP 0-9-1 是否可用 | 对 amqp 端口发 0-9-1 协议头，读到 `connection.start` 帧即支持 |
| AMQP 1.0 是否可用 | 对 amqp 端口发 1.0 协议头（`AMQP\0\1\0\0`），对端回 major=1 即支持（RabbitMQ 4.x 双协议同端口协商，无独立 5673） |
| RabbitMQ 版本号 | 0-9-1 握手 `connection.start` 帧 server-properties 的 `product`/`version` 字段（非 RabbitMQ 对端判级跳过；原生延迟只看版本，与协议无关） |

决策：**默认走 091**；仅当对端不提供 091 而提供 1.0（如只开 1.0 的托管 MQ）时走 1.0。启动日志打印判定依据。

为什么默认 091（dev 实测，见下表）：常用并发与预取区间（≤ 4 × 50）091 的消费吞吐不低于 1.0，入队 091 更快；远程 RPC（`runTask` 远程模式）1.0 每次调用要新建并关闭一个请求链接、多两次往返，单次耗时约为 091 的 2.5~3 倍。1.0 只在 16 × 50 这类高并发高预取下消费更快，功能上两种协议已无差别（原生延迟都支持）。探测不能用纯 TCP 连通性（云安全组可能对任意端口 SYN 代答）。

### 队列形态与持久化语义

`TaskRunnerConfig.queueType`：`QUEUE_TYPE_QUORUM=1`（**默认**）/ `QUEUE_TYPE_CLASSIC=0`。两种队列可并存。

**修改 queueType 后同名旧队列的迁移**：RabbitMQ 不允许改队列形态，新配置去声明旧的 classic（或反过来）同名队列会报 `PRECONDITION_FAILED`。迁移逻辑在协议操作面内部（`TaskAmqpOps.declareDurableQueue`，两种协议共用一套判断），只在同时满足以下条件时删除旧队列并按新形态重建，否则不动队列、每分钟重试：

- 报错原因确认是队列形态不一致（其他声明失败照旧重试）；
- 旧队列已确认无积压消息且无消费者（无在途未确认消息）。

队列非空时打一条 ERROR 说明队列名与目标形态：把消息消费完后，下一周期自动完成迁移；需要保留这些消息又不便等待时，人工把它们转到新形态的队列再处理。迁移不依赖 Leader 身份（启动初期 Leader 尚未选出）；多实例同时启动时各实例都会走这一判断，队列此时为空，先后重建结果一致。两种协议（091 / 1.0）的报错文案不同，判定都覆盖。

**已知取舍**：判空用的是待投递消息数，未到期的原生延迟消息不计入。classic → quorum 方向没有延迟消息，是安全的；quorum → classic 方向上，队列里尚未到期的延迟消息会随旧队列一起删除——需要把 quorum 队列改回 classic 时，先确认没有未到期的延时任务。

| 队列形态 | 持久化 | 说明 |
|---|---|---|
| **quorum（默认）** | **恒持久化，Raft WAL 每批必 fsync**，无"不写盘"选项（单机亦然——保住不停机扩 3 节点能力）；`deliveryMode` 标志被 RabbitMQ 忽略 | 数据安全 + 原生延迟消息唯一载体（延时任务路由判定=queueType 单一来源） |
| **classic** | **消息默认 NON_PERSISTENT 不写盘**（驻内存，吞吐内存级；broker 崩溃/重启丢消息） | 单副本、无复制；仅接受单副本风险的高频任务选用 |

### 吞吐实测（dev 实机，RabbitMQ 4.4.1，uw-task 框架全链路）

口径（`PerfProbeMain`，同机网络、JRE 25、单容器）：走真实 `TaskSendCenter` / `TaskRunnerContainer` / Kryo 转换 / Redis 取号，任务体只计数，每轮 5 万条、3 轮取中位数。**入队**=从首条 `sendToQueue` 到队列积压达到 N 条；**消费**=从启动监听到任务体被调用 N 次。定时任务调度不经过 MQ，与协议无关（200 个每秒触发的配置连续 10 秒：三档均 100% 触发，相对计划时刻的延迟 p50=0ms、p99≤2ms）。

**入队（msg/s）**

| 队列形态 | 091 | 1.0 | 差异 |
|---|---|---|---|
| classic（消息不写盘） | 56k~59k | 41k~48k | 091 快约 25%~30% |
| quorum（Raft 持久化） | 35k~37k | 38k~41k | 1.0 快约 8%~15% |

**消费（msg/s）**——差异主要来自并发与预取配置，而不是协议本身：

| 配置（consumerNum × prefetchNum） | 队列形态 | 091 | 1.0 |
|---|---|---|---|
| 1 × 1 | classic | 6.1k | 5.8k |
| 1 × 1 | quorum | **0.56k** | **0.53k** |
| 1 × 10（**框架默认值**） | classic | 25k | 21k |
| 1 × 10（**框架默认值**） | quorum | **3.1k** | 1.6k |
| 4 × 10 | classic | 25k | 29k |
| 4 × 10 | quorum | **11.6k** | 5.9k |
| 4 × 5 | classic | 23k | 27k |
| 4 × 5 | quorum | 6.1k | 4.2k |
| 4 × 50 | classic | 31k | 41k |
| 4 × 50 | quorum | 36k | 23k |
| 16 × 50 | classic | 29k | 40k |
| 16 × 50 | quorum | 33k | **69k** |

结论：

- **quorum 队列的吞吐由预取数决定**：每条消息的确认都要走一次 Raft，`prefetchNum=1` 时每条消息一次落盘，只有约 550 msg/s（比 classic 慢一个数量级）；默认值 10 把在途消息数放大 10 倍，为 3.1k（1 个消费者）/ 11.6k（4 个消费者）；调到 50 后 33k~69k。**quorum 队列吞吐敏感时把 `prefetchNum` 调大**（建议 ≥50）。
- 091 在 quorum 上的消费吞吐约 33k~36k 封顶，加并发不再提升；1.0 在 ≤ 4 × 50 时明显不如 091（默认的 1 × 10 只有 091 的一半），16 × 50 时才升到 69k。
- classic 队列上 1.0 的消费比 091 快 18%~40%，入队则 091 更快。

### broker 侧调优（RabbitMQ 4.4.1，运维放入 rabbit 编排模板）

放入 `/etc/rabbitmq/advanced.config`（纯 Erlang 项式文件），重启生效。两项都是 **broker 流控阈值**，不改持久化语义：

```erlang
[{rabbit, [
  {max_incoming_window, 5000},       %% 1.0 协议 credit 流控窗口
  {max_link_credit, 2000},           %% 1.0 协议 link credit 上限
  {max_queue_credit, 5000},          %% 1.0 协信用队列到客户端进程的 credit 上限
  {quorum_commands_soft_limit, 512}  %% 向 quorum 队列发布的通道流控阈值（默认 32）
]}].
```

**实测增益**（dev 实机，同机临时 RabbitMQ 4.4.1 容器、默认配置对照调优配置，口径同上表；单位 msg/s，quorum 队列）：

| 场景 | 091 默认 → 调优 | 1.0 默认 → 调优 | 起作用的参数 |
|---|---|---|---|
| 入队（4 × 5 / 4 × 50 / 16 × 50，结果相近） | 33k~34k → **60k~63k** | 29k~32k → **59k~64k** | 091 靠 `quorum_commands_soft_limit`，1.0 靠三个 credit 参数 |
| 消费 16 × 50 | 35k → 37k | 47k → 51k~66k | credit 参数（仅 1.0） |
| 消费 4 × 50 / 4 × 5 / 1 × 1 | 不变 | 不变 | 无 |

- 调优后 **quorum 入队反超 classic**（quorum 60k~64k，classic 091 约 55k~63k、1.0 约 46k~51k）；消费侧 quorum 在预取 ≥50 时与 classic 同量级（16 × 50 时 1.0 的 quorum 还高于 classic）。
- **低预取的消费不受任何 broker 参数影响**：每个确认批次都要把 WAL 落盘一次才能放行下一批投递，吞吐 ≈ 在途消息数 ÷（fsync 延迟 + 开销）。dev 磁盘 `fdatasync` p50≈1.9ms，与 `prefetchNum=1` 时约 450 msg/s（2.2ms/条）吻合。**唯一杠杆是把 `prefetchNum` 调大**（在途消息数 × 并发），或换更快的盘。
- 以下参数实测**无效**（差异在噪声内，不建议配置）：`ra.wal_max_batch_size`（4096→131072）、`wal_compute_checksums`、`segment_compute_checksums`、`quorum_queue.wal_max_size_bytes`、`segment_max_entries`、关闭统计采集（`collect_statistics = none`）。早期记录的"WAL 批上限让 quorum 吞吐提升 5 倍"未能在框架全链路口径下复现，增益实际来自 credit 流控。
- quorum **不存在"不写盘"选项**（Raft 安全性本体）：`wal_sync_method` 只有 `sync`/`datasync` 两种 fsync 方式（默认 `datasync`）。挂载去 barrier（barrier=0）= 假 fsync，掉电丢 WAL 且可能损坏文件系统，**禁止**。
- 以上为单节点容器实测；多节点集群的绝对值更低（复制开销），但两项流控参数的作用方向相同。官方说明：credit 类参数以单发布者/单队列为前提调大，多发布者打同一队列时过大可能引发内存压力，需结合 `vm_memory_high_watermark` 观察。

客户端侧（已内置，无需配置）：按官方推荐形态，发送/RPC/管理共用单 Connection，复用靠 AMQP 1.0 的 link 多路复用；发送用模板长驻 Publisher（`template.getPublisher()`）。dev 同机实测（230B、单线程 fire-and-forget、端到端入队口径）：1.0 单连接约 50k~53k msg/s，2 连接约 55k~57k，4 连接反降至约 41k~43k；091 单通道约 68k msg/s。多连接在低时延下收益不足 10%，故不设发送连接池；高时延等特殊网络可用 `SendPoolSweep` 扫描评估。

两套 1.0 客户端库对照（`AmqpClientLibraryBench`，单线程 fire-and-forget、端到端入队口径）：RabbitMQ 官方 amqp-client（底层 Netty）、proton-j2 原生、spring-amqp-client（底层 proton-j2）三者单连接吞吐无差别，差异在噪声内；选库不影响性能，结果只与链路时延和连接数有关（经 VPN 约 14ms 链路单连接约 6.6k~6.9k msg/s，dev 同机约 50k msg/s）。

### 私有 Redis（与 uw-kv 完全隔离）

延时 zset / 序列发号 / Leader 选举 / 全局限速共用 uw-task **私有 Redis 连接**（`uw.task.redis.*` 嵌套 Boot 原生 DataRedisProperties，连接经 `RedisConnectionFactoryBuilder` 构建，对齐 uw-mfa/uw-kv 模式）。与 `spring.data.redis.*` 无关，不回退到业务 Redis 配置：未写的项取 DataRedisProperties 默认值，`uw.task.redis.host` 未配置则整个 uw-task 不装配。

私有组件（uw.task.util，init(factory) 注入）：`TaskSequence`（号段发号，key=uw-task-seq:）、`TaskLeaderElector`（选举，key=uw-task-leader:）、`TaskGlobalRateLimiter`（固定窗口限速，key=uw-task-rate:）——不依赖 uw-kv 的 Global* 静态家族，连接与 key 空间双隔离，故障域互不影响。

### 集群连接

`uw.task.rabbitmq.addresses: n1:5672,n2:5672,...` 配置后 host/port 被忽略——探测逐节点握手取首个响应；091 走 setAddresses；1.0 走 uris()（amqp:// URI，客户端 AddressSelector 轮询 + Recovery 故障转移）。

### 已知边界

- 启动期探测只执行一次：启动时 MQ 不可达判为 091 且原生延迟关闭（延时任务走 zset 搬运），MQ 恢复后需重启应用才重新判定
- 两种协议的 `sendToQueue` 都**不保证送达**（091 未开 publisher confirm；1.0 为异步发布）。失败处理：091 的同步异常在调用线程内退避重试；1.0 的 broker 拒收（RELEASED/REJECTED，如队列不存在）在客户端回调里转虚拟线程走同一套退避重试（上限 20 次，约 19 秒），耗尽后触发 `onSendFail`——但**调用方早已返回，不会抛异常**。连接在消息发出后、broker 应答前断开等场景两种协议都可能静默丢失；需要确认送达的场景使用 RPC（`runTask`）或业务侧回执
- AMQP 1.0 不支持事务（本框架应用层重试，无影响）
- `receive()` 拉模式在 1.0 下会静默吞消息（超时后链路仍持 credit），禁用
- Boot 4.2 starter 语义：依赖 `spring-boot-starter-rabbitmq`（经典 0-9-1）+ 显式 `spring-rabbitmq-client`（1.0）
- uw-task 不再带 `@EnableScheduling`（自身没有 `@Scheduled` 方法）：宿主应用用到 `@Scheduled` 时自行声明
- 队列里出现 content-type 不匹配的外来消息时跳过，WARN 只打消息体前 256 字节
- 任务 bean 被 CGLIB 代理（如加了 `@Transactional`、`@Async`）不影响识别：索引按真实类名建立

## 配置热更新（nacos）

`uw.task.*` 支持原地热更新：task-center-host、重试延时等**运行期读取项即时生效**；协议探测、连接工厂、执行器并发上限为启动期定死，变更需重启。
