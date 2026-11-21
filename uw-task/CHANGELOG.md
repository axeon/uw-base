# uw-task CHANGELOG

## 5.2.0（2026-11-20）

双协议重写 + 延时任务大一统（由 uw-task2 迁回，包名回归 `uw.task`）。

### 破坏性变更（未上线无兼容负担，升级即全量对齐）

- **TaskDelayer 独立类型整体退役**：`TaskDelayer`/`delayTask()`/`TaskDelayerContainer`/`TaskDelayerListener` 及实体全删——`taskDelay>0` 即延时任务（TaskRunner 一种类型），三入口（`sendToQueue`/`runQueue`/`runTask`）完全等同。
- **delayType 概念退役**：延时路由全自动（协议探测 + queueType 判定），`RunnerDelayType` 枚举/字段全删。
- **TaskRunnerConfig.queueType 语义替换**：新 0=classic/1=quorum（默认 quorum）；组/项目共享队列概念退役，全部任务级队列。
- **持久化语义定案**：classic 队列消息默认 NON_PERSISTENT 不写盘（broker 崩溃丢失换吞吐）；quorum 恒持久化。
- **私有 Redis**：移除 uw-kv 依赖，改 `uw.task.redis.*`（Boot 原生 DataRedisProperties 结构）；序列发号/Leader 选举/全局限速/延时 zset 共用。

### 新增

- **AMQP 双协议自动探测**（`uw.task.rabbit.*`）：启动期 amqp 端口双握手（1.0 协议头 + 0-9-1 版本解析）定死协议；RabbitMQ ≥4.4 且 1.0 时延时任务走 MQ 原生延迟（x-opt-delivery-time），否则自动转 Redis zset 搬运（`TaskDelayTransferContainer`，Leader 单例 mover）。能力自述 `TaskAmqpOps.supportsNativeDelay()` 为路由单一事实源。
- **croner 自续期调度**：`TaskCronerContainer` 弃 Spring TaskScheduler Trigger 通道，改 ScheduledExecutorService 一次性自续期（active 门闩 + synchronized 防孤儿续期；框架异常不断续期链）。
- **TaskAutoConfiguration 分层装配**：组件拆独立 @Bean 四层（资源/工具/容器/顶层），静态 `TaskFactory.getInstance()` 框架内零调用（解环），生命周期归 `TaskLifecycleBootstrap`（SmartLifecycle）。
- **投递侧监听**：`TaskSendListener`（onDelaySubmit/onSendFail），投递重试日志降噪（中间 WARN 计数、最终 ERROR 全栈）。
- **消费者虚拟线程化**：091/1.0 消费模型统一为每消费者/每任务 Thread.ofVirtual；1.0 发送连接池（`-Duw.task.rabbit100.send-pool-size`，默认 4）。
- `TaskCroner`/`TaskRunner.initContact()` 默认返回空联系人（无报警需求无需覆写）。
- **全量虚拟线程化**：croner 调度改为每 croner 一个 sleeping VT（自续期单飞天然有界，无池）；`taskRpcService`/`taskQueueService` 改 `LimitedVirtualThreadExecutor`（VT+并发上限，rpc 满时 CallerRuns、local 缓冲满时溢出 MQ）。配置删除 `croner-thread-num`/`task-rpc-min-thread-num`/`task-local-min-thread-num`，两个 max 值语义改为纯并发上限（背压闸门，与线程成本无关）。
- IDE 配置元数据（spring-boot-configuration-processor，`uw.task.*` 补全与校验）。

### 修复

- croner `onPostExecute` 回调时 resultData/finishDate 未就位（原版顺序缺陷，已调整 set 顺序）。
- croner `runTask` 体内 `scheduleDate` 不可见（参数直传，提前 set）。

### 已知边界

- 091 协议收到 delay>0 记 error 后立即投递（正常不可达，防绕过路由直发）。
- classic 队列消息不写盘，broker 崩溃丢失（设计取舍，README 详述）。

## 5.1.x 及更早

见 git 历史（经典 091 单协议版本）。
