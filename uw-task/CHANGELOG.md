# uw-task CHANGELOG

## 5.3.3（2026-11-25）

- **首次拉取配置失败先重试再降级**：间隔 1 秒、2 秒、3 秒共重试 3 次，全部失败才进入本地降级；重试受 8 秒总时长预算约束（单次尝试本身就很慢时不重试，按原节奏降级）；首次拉取由 0 秒改为延迟 2 秒。起因：uw-task-center 自身也是执行主机，它经注册中心拉自己的配置时，第一次拉取早于自身注册完成，每次启动都会先降级再在一分钟后恢复（2 条 ERROR + 6 条 WARN，监听与调度重建一遍）。两种手段叠加后正常启动无告警，注册或 center 稍慢时也能立即恢复。
- 日志文案：两处残留的 `!` 与 `!` 标点更正。

## 5.3.2（2026-11-25）

- **逐行评审修复**：①任务 bean 被 CGLIB 代理时按真实类名建索引（原用 `getClass().getName()`，代理类名带 `$$SpringCGLIB$$` 后缀会报"未找到 TaskRunner 实例"）；②默认配置上传 task-center 失败不再静默：先以本地默认配置（id=0）启动并打 WARN，之后每周期重试上传，成功后换成 center 配置；③默认配置上传前先规整取值；④去掉多余的 `@EnableScheduling`（uw-task 没有 `@Scheduled` 方法，由宿主自行声明）；⑤`runQueue` 本地执行入口失败时捕获所有异常并降级入队；⑥content-type 不匹配的外来消息只打消息体前 256 字节；⑦项目前缀匹配带点号边界；⑧未接入 nacos 时主机地址 `unknown` 用本机 IP 兜底；⑨全局限速被拒的请求不再占用窗口配额；⑩定时任务调度句柄按键加句柄删除；⑪1.0 集群地址改用 RFC 3986 百分号编码；⑫日志文案去掉 `!`、`。。。` 等旧式标点；⑬修正"裸 Binder 遇未知键会抛"的错误注释；消息转换器改为类级常量复用。
- **队列形态迁移**：判定的报错文案补上 1.0 协议（`Unexpected response code: 409 … inequivalent arg 'x-queue-type'`，此前只认 091 的 `PRECONDITION_FAILED`，1.0 下不会触发迁移）；两个方向都迁移（用户裁决），并在文档写明取舍：判空用的是待投递消息数，quorum 改回 classic 时队列里未到期的原生延迟消息会随旧队列删除。`QueueTypeMigrationProbe` 补 1.0 场景（队列形态经管理接口确认）。

- **任务配置的校验与规整**：新增外部类 `uw.task.util.TaskConfigValidator`——`normalize(config)` 把非法取值回退默认值并汇总打一条 WARN，只在配置进入框架的唯一入口调用一次（`TaskMetaInfoManager.setRunnerConfig` / `setCronerConfig`），`validate(config)` 只报告不合法项、不修改配置，供 task-center 保存配置时拒绝非法输入（center 侧尚未接入）。覆盖：`queueType` / `rateLimitType` / `runType` / `logLevel` 越界、`consumerNum` / `prefetchNum` / `rateLimitValue` / `rateLimitTime` < 1、重试次数与告警阈值为负、`runTarget` 空白。不在 setter 里做（uw-dao 读实体直接写字段会绕过）。起因：saas 侧 28 处 `setQueueType(5)`（旧常量值）在新版里被当成非 quorum 建成 classic 队列。
- **队列形态变更的有条件迁移**：收在协议操作面内部（`TaskAmqpOps.declareDurableQueue`，两种协议共用）：同名队列的 `x-queue-type` 与配置不一致（`PRECONDITION_FAILED`）时，在"队列确认无消息且无消费者"时删除旧队列并按新形态重建（不依赖 Leader 身份）；队列非空只打 ERROR、不删，下一次声明重试。`TaskAmqpOps` 新增 `isEmptyWithoutConsumers` / `deleteQueue`。真实 MQ 验证：`QueueTypeMigrationProbe`（`-Damqp.bench=true`）。

## 5.3.1（2026-11-24）

5.2.1 / 5.3.0（2026-11-22 已发布）之后的全面 code review + simplify + dev 实机性能对照修订版。

### 升级注意

- **协议默认值**：启动期探测不再"1.0 可用就走 1.0"，改为**默认 091**，仅对端不提供 0-9-1 而提供 1.0 时才走 1.0（零配置不变）。
- **`prefetchNum` 默认值 1 → 10**：未显式配置的队列任务在途消息数放大 10 倍（内存占用与失败时的重投范围同步放大）。
- **运行档位**：不配 MQ 时不再装配失败，按"仅定时任务档"启动，入队/远程 RPC/延时提交抛 `MQ_NOT_CONFIGURED`。
- **`Rabbit091Ops` 构造器增加第三个参数** `nativeDelayAvailable`（仅直接 new 该类的代码受影响；经 `TaskFactory` 使用的业务方无感）。

### 变更

- **延时 zset 收敛为 project 单分片**：一个 project 一个 zset（`uw-task-delay:{task-project}`），member=Kryo(TaskData)，mover 每轮一次 Lua 原子 pop；到期搬运经 `sendToQueue` 全路由重新推导队列名。跨队列分片与 `_queues` 注册表退役。
- **延时积压观测**：`TaskHostStats.delayBacklog` 随主机上报（project 级 zset 成员数）。
- **RPC 重试策略简化**：`runTask` 远程模式的连接类异常一律重试（上限 20 次），仅超时与中断不重试；RPC 任务的服务端实现必须幂等。重试期间各轮失败原因合并为一条 WARN 汇总（原每轮一条带堆栈的 ERROR），最终失败另记一次带堆栈的 ERROR。
- **运行档位（三档）**：装配时按依赖判定——Redis 是必需项（任务 id 发号、Leader 选举、延时搬运、全局限速），MQ 只有队列收发与延时载体需要。①完整档（Redis+MQ）全部功能可用；②**仅定时任务档**（只配 Redis）新增支持：不探测协议、不创建 MQ 操作面，定时任务与队列任务的本地执行可用，`sendToQueue`/远程 RPC/延时提交在入口直接抛 `TaskRuntimeException`（不进重试、不写 zset），注册器不再尝试声明队列；③不初始化档（未配 Redis，或执行主机未配 `task-project`）：打 WARN 并跳过整个装配。此前只配 Redis 的应用会被整个跳过。
- **装配门控实现**：`@Conditional(OnTaskSettingsCondition)`（与 uw-kv/uw-mfa 同机制，此前改过的 Properties 检测已回退），判定逻辑集中在 `UwTaskProperties.missingRequiredSetting` / `resolveRuntimeMode`；属性读取走 Binder，环境变量等宽松绑定形式同样生效。
- **修复仅作调用方的主机未配 `task-project` 时启动空指针**：Leader 参与者注册用 `task-project` 作注册表 key，为空时 `ConcurrentHashMap.compute(null)` 抛 NPE 导致 `taskFactory` 创建失败；现仅在配置了 `task-project` 时注册。
- **启动参数校验**：并发上限 ≤0、重试延时 <0、`run-target`/`task-center-host` 空白时各打一条 WARN 并按默认值生效（原为静默：执行器把非法并发收敛为 1、负延时等同不延时）。
- **`task-project` 前缀全链路一致**：首次注册上传与 center 不可用时的本地降级启动也只处理前缀内的任务（原只过滤服务端下发的配置，前缀外的任务会用本地默认配置启动且此后不受服务端管控）。

### 评审修复（破坏性，零使用方）

- **补回 `TaskFactory.getListenerManager()`**：此前方法缺失且 `TaskListenerManager` 不是 bean，业务方无法注册任何监听器。
- **`TaskRunnerListener.onPreExecute` 开始被框架回调**：此前接口声明了该方法但执行链路从未调用；现在在任务实际执行前回调（限速超时未执行的任务不触发）。
- **1.0 异步发布失败可重发**：`TaskAmqpOps.send` 增加异步失败回调；broker 拒收（RELEASED/REJECTED）时 `TaskSendCenter` 转虚拟线程退避重发（上限 20 次），耗尽后触发 `onSendFail`（此前只记日志）。091 不受影响（同步路径不变）。
- **1.0 远程 RPC 不回结果**：协议为 1.0 时服务端消费到请求后直接 settle、从不回复，调用方一律等到超时；现按请求的 `reply-to` / `messageId` 回结果（走 Direct Reply-To）。RPC 客户端走 `RabbitAmqpTemplate.sendAndReceive` 官方路径，并把模板 `publishTimeout`/`completionTimeout` 从默认 60 秒对齐到框架的 180 秒。实测（VPN 链路）091 约 32ms、1.0 约 89ms（模板每次调用新建并关闭一个 `Requester`，各多一次网络往返；这是 Spring 官方文档给出的唯一路径，池化复用属自造方案已弃）。
- **1.0 发送链路回归官方单连接形态**：退役自造的多连接发送池（含 `uw.task.rabbit100.send-pool-size` 配置），发送/RPC/管理共用单 Connection + 模板长驻 Publisher。dev 同机实测单连接约 50k~53k msg/s（091 单通道约 68k，1.0 客户端固有慢约 20%），多连接收益仅约 5% 且与官方"单连接 link 复用"设计相悖。扫描工具 `SendPoolSweep` 保留；`Rabbit100Ops` 构造器去掉已无用途的 `instanceId` 参数。
- **私有 Redis 连接工厂在应用关闭时实际未被销毁**（`RedisConnectionFactory` 不是 `AutoCloseable`，原判断恒为假），改为按 Lettuce 实现调 `destroy()`。
- **限速等待不再超过 `rateLimitWait`**：本地/全局限速的 sleep 取"到窗口结束"与"到等待截止"的较小值（原为睡到窗口结束，窗口大于等待配置时会超时等待）。
- **1.0 消费入口中断时退回消息**：等待并发许可被中断时改为 `context.requeue()`（AMQP 1.0 `released` outcome）——消息退回队列重新投递并归还 credit，classic/quorum 实测均恰好再投递一次且不进死信（原直接 return，消息悬挂并永久占用一个 credit 位）。
- **协议默认改为 091**：启动期探测仍零配置，但决策从"1.0 可用走 1.0"改为"默认 091，仅对端不提供 0-9-1 而提供 1.0 时走 1.0"。依据 dev 实测：≤ 4 × 50 的并发预取区间 091 消费不低于 1.0、入队更快，远程 RPC 少两次往返（约快 2.5~3 倍）。
- **`prefetchNum` 默认值 1 → 10**（`TaskRunnerConfig.DEFAULT_PREFETCH_NUM`）：quorum 队列默认预取 1 时每条消息一次 WAL 落盘，仅约 450~550 msg/s；默认 10 为 3.1k（1 个消费者）/ 11.6k（4 个消费者）。队列形态默认仍为 quorum。
- **遗留英文日志改中文（复查补漏）**：`TaskMessageConverter`（序列化/反序列化失败、content-type 不匹配）、`UwTaskAutoConfiguration`（三个定时任务异常）、`TaskRunnerContainer`（暂停/销毁）共 8 条；主代码日志已无英文文案。
- **`TaskAmqpOps` 及两个实现类成员顺序统一**：接口与 `Rabbit091Ops` / `Rabbit100Ops` 都按"常量 → 字段 → 构造器 → 接口声明顺序的实例方法 → 包内辅助方法 → 私有辅助方法 → 嵌套类型"排列；两个实现类的 9 个接口方法全部补 javadoc（`{@inheritDoc}` + 实现要点），`TaskAmqpOps` 的通配 import 改为显式 import。
- **091 + quorum 接入 MQ 原生延迟**：RabbitMQ 4.4 的 0-9-1 发布方同样支持 `x-opt-delivery-time` 消息头，此前 091 恒走 Redis zset 搬运属实现缺口。现 `RabbitCapabilityProbe` 的原生延迟判定与协议解耦（只看 broker 版本；版本探测不再依赖 1.0 握手成功），`Rabbit091Ops` 构造器增加原生延迟可用参数并在发送时写入消息头。dev 4.4.1 实测：延迟 3s 的消息 091 在 3045ms、1.0 在 3038ms 后收到，到期前不可见。
- **dev 实机探针**：新增两个独立可执行程序（`uw.task.probe` 包，需在目标网络内运行）——`TierProbeMain` 验证运行档位（仅定时任务档下定时任务、本地执行、入队快速失败等 9 项检查）；`PerfProbeMain` 对照 091 / 1.0 在 classic / quorum 队列上的入队与消费吞吐，以及定时任务触发精度。README 的吞吐数据据此重写，并补充"quorum 队列须调大 prefetchNum"的结论。`PerfProbeMain` 支持 `UW_PERF_QUEUE_TYPES`（只测指定队列形态）与 `UW_PERF_RUN_CRON=false`（跳过定时任务一段）。README 的 broker 侧调优按对照实测重写：起作用的是流控阈值（`quorum_commands_soft_limit`、1.0 的三个 credit 参数，quorum 入队约 33k→60k~64k msg/s），`ra.wal_max_batch_size` 等 WAL 参数实测无效，低预取消费受 fsync 延迟限制、只能靠调大 `prefetchNum`。
- **javadoc 全面补齐**：doclint（`-Xdoclint:all`）零告警——补全实体类/配置类/工具类的访问器、构造器与内部类注释，翻译自动生成的英文 Builder 注释，修复 `TaskCroner.runTask` 与 `TaskRunnerConfig.Builder` 的残缺注释。
- **文案规则对齐**：遗留英文日志改中文、中文异常文案改英文（log 中文、exception 英文）；`TaskFactory` 的 RPC 超时常量收敛到 `TaskAmqpOps.RPC_TIMEOUT_MILLIS` 单一来源；全库 import 统一分组排序，内联全限定名仅保留同名冲突的 3 处；`Serializable` 类补 `serialVersionUID`；测试中的口令默认值清除。

### 评审简化

- `TaskSendCenter`：延时/即时两套消息构造合并为 `buildQueueMessage`。
- `TaskRunnerContainer`：LOCAL/GLOBAL 两段重复的重试判定收敛为 `retryLimit`；runner 缺失与兜底异常两处失败落盘收敛为 `recordProgramFail`；删除恒为假的"共享队列 stillInUse"扫描（队列名即配置键，一对一）。
- `TaskMetaInfoManager`：`getFitQueue`/`getRunnerConfigByData` 的重复匹配逻辑收敛为 `findRunnerConfig`；runner 实例 Map 补全泛型。
- `TaskAmqpOps.sendAndReceive` 去掉恒为 180000 的 timeout 入参（统一常量 `RPC_TIMEOUT_MILLIS`）；删除不可达代码 `TaskAmqpProtocol.fromConfig`（协议强制开关已退役）、`TaskSequence` 步长 1 分支（步长恒为常量）、`TaskGlobalRateLimiter` 未用字段。`TaskRunnerListener.onPreExecute`、`TaskAmqpOps.getProtocol()`、`getRabbitmqVersion()`、`TaskData.builder(TaskData)` 等无调用点的 API 保留。
- `TaskDelayTransferContainer` 构造去掉恒传 null 的第三参；`TaskAmqpListenerContainer` 以具体类型替代 `Object` 强转；内联全限定名、通配 import 清零。
- 文档：README/FAQ 修正与代码不符处（本地限速实现、PROGRAM 重试、延时重试为线性退避、zset key 形态、Redis 不回退 `spring.data.redis`、阻塞时长），新增监听器章节与 1.0 异步发送、启动期探测两条已知边界。

## 5.2.1（2026-11-22）

装配门控：未配置必需 host 的连接型自动配置一次性 WARN 并跳过装配。

## 5.2.0（2026-11-20）

双协议重写 + 延时任务大一统（由 uw-task2 迁回，包名回归 `uw.task`）。

### 破坏性变更（未上线无兼容负担，升级即全量对齐）

- **TaskDelayer 独立类型整体退役**：`TaskDelayer`/`delayTask()`/`TaskDelayerContainer`/`TaskDelayerListener` 及实体全删——`taskDelay>0` 即延时任务（TaskRunner 一种类型），三入口（`sendToQueue`/`runQueue`/`runTask`）完全等同。
- **delayType 概念退役**：延时路由全自动（协议探测 + queueType 判定），`RunnerDelayType` 枚举/字段全删。
- **TaskRunnerConfig.queueType 语义替换**：新 0=classic/1=quorum（默认 quorum）；组/项目共享队列概念退役，全部任务级队列。
- **持久化语义定案**：classic 队列消息默认 NON_PERSISTENT 不写盘（broker 崩溃丢失换吞吐）；quorum 恒持久化。
- **私有 Redis**：移除 uw-kv 依赖，改 `uw.task.redis.*`（Boot 原生 DataRedisProperties 结构）；序列发号/Leader 选举/全局限速/延时 zset 共用。

### 新增

- **AMQP 双协议自动探测**（`uw.task.rabbit.*`）：启动期 amqp 端口双握手（1.0 协议头 + 0-9-1 版本解析）定死协议；RabbitMQ ≥4.4 时延时任务走 MQ 原生延迟（x-opt-delivery-time，091 / 1.0 均可；091 发同名消息头），否则自动转 Redis zset 搬运（`TaskDelayTransferContainer`，Leader 单例 mover）。能力自述 `TaskAmqpOps.supportsNativeDelay()` 为路由单一事实源。
- **croner 自续期调度**：`TaskCronerContainer` 弃 Spring TaskScheduler Trigger 通道，改每 croner 一个虚拟线程一次性自续期（active 门闩 + synchronized 防孤儿续期；框架异常不断续期链）。
- **TaskAutoConfiguration 单 bean 化**：对外仅 `taskFactory` 一个 bean，内部组件在 bean 方法内手工构建不进容器；静态 `TaskFactory.getInstance()` 框架内零调用（解环），生命周期归 `UwTaskAutoConfiguration`（SmartLifecycle）。
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
