[TOC]

## 简介

uw-task 是一个分布式任务框架，基于 Spring Boot + RabbitMQ + Redis 实现，可快速构建分布式任务体系，支持定时任务（TaskCroner）与队列任务（TaskRunner，含延时语义），并提供任务运维监控、动态配置与多规则报警。

**5.2.0 双协议重写版**：AMQP 0-9-1 / 1.0 启动期自动判定、队列形态 classic/quorum 可选、RabbitMQ 4.4+ 原生延迟消息、私有 Redis 连接（详见文末「双协议与队列形态」章）。

配合服务端 [uw-task-center](../../uw-task-center) 使用：任务执行主机向 task-center 注册、上报状态、拉取动态配置；task-center 负责配置管理、统计汇总与报警分发。

## 主要特性

1. 基于 Spring Boot 实现，依赖 RabbitMQ（任务派发）与 Redis（全局限速 / Leader 选举 / 序列发号）。
1b. **AMQP 双协议自动判定**：启动期探测（协议头握手+版本解析，纯 amqp 端口，无管理端口依赖），1.0 可用走 1.0（含原生延迟消息），否则 091——云托管/自建全兼容，零配置。
2. 完全分布式，支持混合云，可指定主机或指定集群（runTarget）运行。
3. 支持定时任务（cron 表达式），支持服务端动态配置，支持全局单例（Leader）运行。
4. 支持队列任务，提供多维流量控制（本地/全局、按进程/主机/TAG/TASK 组合）、错误重试、服务端动态配置。
5. 延时任务全自动化（5.2.0）：`taskDelay > 0` 即延时任务，无需独立类型——1.0+RabbitMQ 4.4+ 且队列形态为 quorum 时走 **MQ 消息级原生延迟**（x-opt-delivery-time，到期自动可见即执行）；否则自动降级 **Redis zset 搬运**（到期自动转投目标队列）。无队头阻塞、长延时不阻塞短延时、重启不丢存量。
6. 支持 RPC 风格调用（同步/异步），支持错误重试与服务端动态配置。
7. 支持按失败率、等待超时、运行超时、队列堆积、延迟超时等多规则的任务报警。

## 版本要求

- JDK 25+
- Spring Boot 4.2.x
- RabbitMQ 3.8+（091 兼容面；1.0 与原生延迟需 4.0+/4.4+，启动期自动判定）
- Redis 8.x+

## maven 引入

```xml
<dependency>
    <groupId>pub.xili</groupId>
    <artifactId>uw-task</artifactId>
    <version>${version}</version>
</dependency>
```

引入后由 `UwTaskAutoConfiguration` 自动装配，无需额外 `@EnableXxx`。

## 应用配置

直接使用 Spring Boot 的 `application.yml`：

```yaml
uw:
  task:
    # 是否启用任务注册（任务执行主机设为 true；仅作为任务调用方的服务设为 false）
    enable-registry: true
    # 任务管理服务器（uw-task-center）地址
    task-center-host: http://localhost:8080
    # 任务项目，必须是包名前缀，用于扫描任务注册（只扫描该包下的 TaskCroner/TaskRunner）
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

    # RabbitMQ（task 使用独立的连接工厂，与业务 MQ 隔离；协议启动期自动判定，无需配置版本）
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

定时任务使用 cron 表达式定时执行，需继承 `TaskCroner` 并实现三个方法。

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
        config.setPrefetchNum(1);               // 预取数，大批量任务可设为 5
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
| 1.0 可用 且 RabbitMQ ≥ 4.4 且 `queueType = QUEUE_TYPE_QUORUM` | **MQ 原生延迟**：消息级 `x-opt-delivery-time`（绝对时间戳），到期自动可见由消费者执行——零搬运、精度 ~0.5s |
| 其余（091/低版本/classic 队列） | **Redis zset 搬运**：写 `uw-task-delay:{queueName}`（score=到期时间），搬运器 Lua 原子 pop 到期任务自动转投目标 MQ 队列（delay 清零），由对应 runner 消费执行；搬运器为 Leader 单例执行（选主切换重叠窗口由 Lua 原子 pop 兜底去重），分片队列名经 Redis 注册表跨实例共享，MQ 故障时写回重试（上限 20 次） |

延时任务的限速/重试/日志/统计全部由 TaskRunner 消费端统一承担（与即时任务完全一致），无独立的 Delayer 配置/监听器/日志类型。

### 适用场景与选型
## 任务内异常处理

为支持监控与重试，`runTask` 抛出的异常分三类，框架据此设置任务状态：

| 异常 | 状态 | 是否重试 | 场景 |
|---|---|---|---|
| `TaskPartnerException` | `STATE_FAIL_PARTNER` | 是（按 `retryTimesByPartner`） | 合作方/第三方接口错误（超时、错误码、限流等） |
| `TaskDataException` | `STATE_FAIL_DATA` | 否 | 任务入参数据错误（格式非法、缺失、业务规则不满足） |
| 其他未捕获异常 | `STATE_FAIL_PROGRAM` | 否 | 程序异常（bug、空指针等），框架自动捕获 |
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

返回 `Future<TaskData>`，通过 `future.get()` 获取结果。远程模式下每个 future 占用一个线程等待 RPC 返回，**大并发下注意线程数与限速叠加可能导致线程池耗尽**。

### 方法对比与快速选型

六个方法本质是三个维度的组合：**执行位置（本地/远程）× 是否阻塞调用线程 × 是否需要返回值**。

| 方法 | 执行位置 | 阻塞调用方 | 返回结果 | 本机无 runner 时 | 典型用途 |
|---|---|---|---|---|---|
| `sendToQueue` | 远程（入队） | 否 | 无 | 入队（正常） | 标准异步队列任务，不关心结果 |
| `runTask` | AUTO 自动选 | **是** | 有 | 回退远程 RPC | 需立即同步拿结果 |
| `runTaskLocal` | 仅本地 | **是** | 有 | **抛异常** | 强制本机执行，不允许远程 |
| `runTaskAsync` | AUTO 自动选 | 否（Future） | 有（get 时） | 回退远程 RPC | 异步拿结果，注意线程池 |
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
         └─ 不能（异步） → runTaskAsync（注意线程池上限）
```

> **重要**：`runTask`/`runTaskLocal`/`runTaskAsync`/`runQueue` 会向传入的 taskData 写入 id/queueDate/runType 等运行期字段，**请勿在多次调用间复用同一 taskData 对象**。远程模式（`runTask`/`runTaskAsync`）默认 sendAndReceive 超时 180 秒，**避免在 Web 请求线程高频同步调用**，以免耗尽 Tomcat 线程。

### 运行类型（runType）

| 常量 | 值 | 说明 |
|---|---|---|
| `RUN_TYPE_LOCAL` | 1 | 本地运行，不受流控限制 |
| `RUN_TYPE_GLOBAL` | 3 | 全局异步运行（入队），受流控限制 |
| `RUN_TYPE_GLOBAL_RPC` | 5 | 全局同步 RPC，不受流控限制 |
| `RUN_TYPE_AUTO_RPC` | 6 | 自动判定（默认），本机有 runner 则本地，否则走 GLOBAL_RPC |

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
| `taskDelay` | 调用方 | 延迟毫秒数：>0 即延时任务，载体自动路由（quorum+1.0/4.4→MQ 原生延迟；否则→Redis zset 搬运） |
| `refId`/`refSubId`/`refTag` | 调用方 | 关联信息，用于第三方统计 |
| `refObject` | 调用方 | 关联对象，不入库，可通过 Listener 访问 |
| `id`/`queueDate`/`consumeDate`/`runDate`/`finishDate`/`runType`/`ranTimes`/`state`/`errorInfo`/`resultData` | 框架 | 由框架自动设置 |

任务状态（`state`）：`0=未知 1=成功 2=程序错误 3=配置错误 4=三方接口错误 5=数据错误`。

## 流量控制功能

在 `TaskRunnerConfig` 中配置限速类型与参数。**全局限速器基于 Redis 固定窗口，建议检测窗口（`rateLimitTime`）不要低于 5 秒**，以减小网络 IO 开销。

### 限速类型（rateLimitType）

分为本地（进程内 Guava 令牌桶）与全局（Redis 固定窗口）两大类：

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

### 并发与背压（per-queue 单一来源）

背压控制**按队列独立设置**（TaskRunnerConfig，center 动态下发、tune 在线生效）：

- `consumerNum`：**执行线程数**（并发执行 consumerNum 路；091=消费者线程弹性伸缩；1.0=LimitedVirtualThreadExecutor 的 Semaphore 上限 + pause/unpause 消费者，两协议语义对齐）
- `prefetchNum`：在途未确认窗口/缓冲（091=未 ack 上限；1.0=credit，settle 在执行完成后才释放——1.0 分发经 credit 可超前投递，执行通道把并发收敛回 consumerNum）

**全局并发线程数 = Σ(各队列 consumerNum)**——多队列规模下由各队列配置面规划总量贴合下游容量（配置治理问题，框架不设全局暗闸：全局信号量会造成热点队列饿死其他队列的跨队列干扰，与每任务独立配置并发的模型冲突）。

虚拟线程只解决"等待不占平台线程"（限速 sleep/park 零成本），**不做背压**——背压由 prefetch/credit 承载（broker 停发），限速超限走 CONFIG 重投退避。

## 重试机制

任务失败后，按 `retryType`（默认 `AUTO`）与失败类型决定是否重试：

- `STATE_FAIL_PARTNER`（合作方异常）：按 `retryTimesByPartner` 重试。
- `STATE_FAIL_CONFIG`（超限流）：按 `retryTimesByOverrated` 重试。
- `STATE_FAIL_PROGRAM`：按 `retryTimesByProgram` 重试（默认 0=不重试）。
- `STATE_FAIL_DATA`：不重试。

设 `retryTimes = N` 时，**总计执行 N+1 次**（1 次初始 + N 次重试）。重试延时按执行轮次线性递增：队列任务为 `ranTimes * taskQueueRetryDelay`，RPC 任务为 `ranTimes * taskRpcRetryDelay`。LOCAL/RPC 模式在本地重跑，GLOBAL 模式重新入队。

延时任务（taskDelay>0）的重试语义与队列任务完全一致（同一消费端执行）：业务失败（PARTNER / PROGRAM）重试经延时路由实现指数退避（1s/2s/4s…封顶 60s）；超限流（`STATE_FAIL_CONFIG`）按 `rateLimitWait` 秒数重新调度。重试次数分别受 `retryTimesByPartner` / `retryTimesByProgram` / `retryTimesByOverrated` 控制（均默认 0=不重试）。

## 多实例运行

同一套定时任务/队列任务存在多个并发运行实例时，通过服务器端多份配置实现：

- **TaskCroner** 通过配置中的 `taskParam` 标识多实例。多数场景用空值（默认），特定用户用特定 ID 作为 taskParam。
- **TaskRunner** 通过 `taskTag` 标识多实例。发送任务时指定 taskTag/runTarget。
- 若指定的 taskTag/runTarget 无法精确匹配服务端配置，框架会宽松匹配最合适的配置。

多实例唯一性由服务端按三元组（`taskClass + 区分维度 + runTarget`）保证：RPC 自动注册时按三元组幂等去重，ops 端新增/修改时校验重复。

## 运行目标 RunTarget

`runTarget` 是非常重要的参数，与 `taskProject` 一起决定任务配置的同步关系：

- 任务执行主机只拉取与自身 `taskProject` + `runTarget` 匹配的配置并执行。
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

判定依据是「是否曾成功拉取过配置」（firstRun），**不是失败次数**。一旦成功过，运行中再故障走 fail-fast（保留上次 center 配置继续跑），不重新降级。

### center 恢复同步

center 恢复后，`updateConfig`（每 60s）拉取成功 → 用 center 配置覆盖降级配置：

- **TaskCroner**：按 configKey 覆盖（停旧调度 + 启新），不清内存 map
- **TaskRunner**：销毁降级 listener（MQ 未 ack 消息自动重新入队）+ 用 center 配置重建 listener

### 不丢任务 / 不丢数据保障

| 任务 | 切换方式 | 保障 |
|---|---|---|
···| TaskCroner | `stop` 用 `cancel(false)` 不中断在途 | 正在执行的 croner 跑完，新调度接管 |
| TaskRunner | 销毁 listener → 重建 | RabbitMQ 未 ack 消息重新入队（broker 持久化） |
| 延时搬运器 | Leader 失去身份时 mover 完成当前批次后退出 | 任务在 Redis zset 持久化；新 Leader 接续搬运（注册表跨实例共享分片） |

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

TaskRunner 的队列任务延时：quorum 队列 + 1.0/4.4 下为 MQ 消息级原生延迟（无队头阻塞，任意时长可用）；其余走 zset 搬运自动转投。延时任务用 taskDelay（自动路由），小负载场景也可直接轮询数据库，均可有效降低资源消耗。

**调用方线程会被阻塞的场景（重要）**

uw-task 的部分操作是同步阻塞的，直接在调用线程执行，务必避免在 Web 请求线程等敏感线程中高频/大批量调用：

1. `sendToQueue` / `runTask`：当 RabbitMQ 出现 `channelMax limit is reached` 等瞬时资源错误时，框架会在调用线程内重试（最多 20 次，退避递增），最坏情况单次调用阻塞近 2 分钟。
2. `runTask` / `runTaskAsync` 远程模式：通过 `sendAndReceive` 同步 RPC，默认 180 秒超时。
3. 队列任务全局限速（`RATE_LIMIT_GLOBAL_*`）：触发限速时，消费者线程会在 `rateLimitWait` 内轮询等待配额，期间该消费者不消费下一条消息。若限速配置激进（等待长、配额小），会导致并发消费者数虚高却实际空转、消息堆积。

建议：高频调用走 `runQueue`（本地队列线程池执行，满则降级入队）；限速场景优先评估 `rateLimitWait` 与并发消费者数的比例，必要时改用进程内限速（`RATE_LIMIT_LOCAL*`）或调小等待时间。

## 双协议与队列形态（5.2.0）

### 协议自动判定（启动期一次定死，运行期不切换）

探测全部经 **amqp 端口本身**完成（`uw.task.rabbitmq.port`），**与管理端口彻底解耦**：

| 检测项 | 手段 |
|---|---|
| AMQP 1.0 是否可用 | 对 amqp 端口发 1.0 协议头（`AMQP\0\1\0\0`），对端回 major=1 即支持（RabbitMQ 4.x 双协议同端口协商，无独立 5673） |
| RabbitMQ 版本号 | 0-9-1 握手 `connection.start` 帧 server-properties 的 `product`/`version` 字段（非 RabbitMQ 对端判级跳过） |

决策：1.0 可用 → 走 1.0；仅 091 → 走 091。启动日志打印判定依据。探测不能用纯 TCP 连通性（云安全组可能对任意端口 SYN 代答）。

### 队列形态与持久化语义

`TaskRunnerConfig.queueType`：`QUEUE_TYPE_QUORUM=1`（**默认**）/ `QUEUE_TYPE_CLASSIC=0`。两种队列可并存。

| 队列形态 | 持久化 | 说明 |
|---|---|---|
| **quorum（默认）** | **恒持久化，Raft WAL 每批必 fsync**，无"不写盘"选项（单机亦然——保住不停机扩 3 节点能力）；`deliveryMode` 标志被 RabbitMQ 忽略 | 数据安全 + 原生延迟消息唯一载体（延时任务路由判定=queueType 单一来源） |
| **classic** | **消息默认 NON_PERSISTENT 不写盘**（驻内存，吞吐内存级；broker 崩溃/重启丢消息） | 单副本、无复制；仅接受单副本风险的高频任务选用 |

### 吞吐实测（RabbitMQ 4.4.1 单容器，持久化消息，单线程发送）

| 组合 | 230B | ~1KB |
|---|---|---|
| 091 + classic | **95,366 msg/s** | 6,947 |
| 1.0 + classic | 26,193 | **8,147** |
| 091 + quorum | 21,731 | 6,281 |
| 1.0 + quorum | 26,524 | 7,196 |

小消息即时消费 classic+091 最快；quorum 上 1.0 反超 091（延迟队列配 1.0+quorum 最优）；≥1KB 后磁盘带宽成为瓶颈。

### 性能调优

broker 侧（运维必做，放入 rabbit 编排模板，必须是独立 `/etc/rabbitmq/advanced.config` 纯 Erlang 项式文件）：

```erlang
[{rabbit, [
  {max_incoming_window, 5000},
  {max_link_credit, 2000},
  {max_queue_credit, 5000}
]},
{ra, [
  {wal_max_batch_size, 131072},  %% WAL 批上限（默认32768）：quorum 吞吐主杠杆
  {wal_compute_checksums, true},
  {wal_sync_method, datasync}
]}].
```

实测增益：091+quorum 230B 21.7k→**116.3k msg/s（5.4 倍，反超 classic）**。注意 `wal_max_batch_size` 调大提高内存占用，与 `vm_memory_high_watermark` 联动观察。quorum **不存在"不写盘"选项**（Raft 安全性本体），挂载去 barrier（barrier=0）= 假 fsync，掉电丢 WAL 且可能损坏文件系统，**禁止**。

客户端侧（已内置，无需配置）：proton-j2 单 Connection 单 IO 线程是发送瓶颈，Rabbit100Ops 内置发送连接池（默认 4，`-Duw.task.rabbit100.send-pool-size=N` 可调），fire-and-forget（与 091 无 confirm 语义对齐）。

### 私有 Redis（与 uw-kv 完全隔离）

DelayTask zset / 序列发号 / Leader 选举 / 全局限速共用 uw-task **私有 Redis 连接**（`uw.task.redis.*` 嵌套 Boot 原生 DataRedisProperties，默认继承 Spring Boot Redis 属性，连接经 `RedisConnectionFactoryBuilder` 构建，对齐 uw-mfa/uw-kv 模式）。

私有组件（uw.task.util，init(factory) 注入）：`TaskSequence`（号段发号，key=uw-task-seq:）、`TaskLeaderElector`（选举，key=uw-task-leader:）、`TaskGlobalRateLimiter`（固定窗口限速，key=uw-task-rate:）——不依赖 uw-kv 的 Global* 静态家族，连接与 key 空间双隔离，故障域互不影响。

### 集群连接

`uw.task.rabbitmq.addresses: n1:5672,n2:5672,...` 配置后 host/port 被忽略——探测逐节点握手取首个响应；091 走 setAddresses；1.0 走 uris()（amqp:// URI，客户端 AddressSelector 轮询 + Recovery 故障转移），发送连接池每连接独立选点。

### 已知边界

- AMQP 1.0 不支持事务（本框架应用层重试，无影响）
- `receive()` 拉模式在 1.0 下会静默吞消息（超时后链路仍持 credit），禁用
- Boot 4.2 starter 语义：依赖 `spring-boot-starter-rabbitmq`（经典 0-9-1）+ 显式 `spring-rabbitmq-client`（1.0）

## 配置热更新（nacos）

`uw.task.*` 支持原地热更新：task-center-host、重试延时等**运行期读取项即时生效**；协议探测、连接工厂、执行器并发上限为启动期定死，变更需重启。
