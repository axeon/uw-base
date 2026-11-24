# 2026.1000.0020 版本升级说明

uw-task 5.3.1（全面评审修订）+ 根 pom flatten 自动清理。

## uw-task（5.3.1）
1. 协议默认值改为 091（仅对端不提供 0-9-1 而提供 1.0 时走 1.0，仍零配置）；091 + quorum 接入 RabbitMQ 4.4 原生延迟消息；`prefetchNum` 默认值 1 → 10。
2. 运行档位三档（完整 / 仅定时任务 / 不装配）与启动参数校验；task-project 前缀全链路一致。
3. 评审修复：1.0 远程 RPC 回结果、1.0 异步发布失败可重发、`getListenerManager()` 补回、`onPreExecute` 回调恢复等；javadoc 全面补齐、日志与异常文案语言对齐。详见 uw-task/CHANGELOG.md。

## 根 pom
1. flatten 插件：`flatten:clean` 追加绑定到 deploy 阶段，发布之后自动清掉各模块的 `.flattened-pom.xml`（install / compile / package 构建后仍保留，`mvn clean` 清理）。

# 2026.1000.0019 版本升级说明

uw-task 5.3.0：延时 zset 收敛为 project 单分片（`uw-task-delay:{task-project}`）、延时积压 `delayBacklog` 随主机上报、RPC 远程调用异常一律重试（上限 20 次，用户裁决）。

# 2026.1000.0018 版本升级说明

重发字面化根 pom：0017 的根 pom 误用 deploy-file 上传，`${revision}` 占位符原样进入私服，导致下游 CI 解析失败。本版只重发根 pom。

# 2026.1000.0017 版本升级说明

装配门控 + WARN 全面铺开：连接型自动配置（uw-kv 1.5.3 / uw-mfa 1.1.83 / uw-task 5.2.1）未配置必需 host 时一次性 WARN 并跳过装配。

# 2026.1000.0016 版本升级说明

uw-kv 1.5.2 装配门控（单模块改动，只发根 pom 与 uw-kv）。

# 2026.1000.0015 版本升级说明

uw-task 5.2.0 终态重构 + 全仓 bean 收敛 + nacos 热更新 + 命名一致化（19 模块全部 +1，uw-task 5.2.0 首发）。

## uw-task（5.2.0，形态重写）
1. croner 自续期调度：纯虚拟线程（每 croner 一个 sleeping VT + active 门闩 + synchronized 防孤儿续期），弃用 Spring TaskScheduler Trigger 通道；修正原版缺陷（onPostExecute 回调时 resultData/finishDate 未就位、runTask 体内 scheduleDate 不可见）。
2. 延时路由单源化：TaskAmqpOps.supportsNativeDelay() 自述能力（091 恒 false → 延时任务自动转 Redis zset 搬运），协议实现与路由不可能错位。
3. TaskAutoConfiguration 单 bean 化：对外仅 taskFactory 一个 bean（TaskListenerManager 经 TaskFactory.getListenerManager() 暴露），TaskLifecycleBootstrap 并入配置类 SmartLifecycle，Transfer↔SendCenter 互指经装配期回填破环。
4. 全量 VT 化：rpc/local 池改 LimitedVirtualThreadExecutor（VT+并发上限背压），删除 croner-thread-num / task-rpc-min-thread-num / task-local-min-thread-num 配置，两个 max 值语义改为纯并发上限。
5. TaskSendListener 投递侧监听（onDelaySubmit/onSendFail）+ 投递重试日志降噪；initContact() 默认空联系人。

## 全仓治理
1. bean 收敛：uw-kv 12 bean→1（Global* 静态门面内化 + kvRedisConnectionFactory defaultCandidate=false 防宿主冲突）；uw-auth-service AuthServiceHelper 锚点内联。
2. nacos 热更新：9 个模块内联 PropertiesRefresher（EnvironmentChangeEvent 原地重绑保实例，静态 Helper 自动生效）；uw-dao 含清路由缓存 + 连接池差异告警（查表配置即时生效、连接池参数需重启）。
3. 命名一致化：DaoConfig→UwDaoProperties、DaoConfigManager→DaoPropertiesManager（移 util 包）、conf 下 Properties/AutoConfiguration 全部 Uw 前缀（14 类 69 文件）、LimitedVirtualThreadExecutor.CallPolicy→RejectedPolicy（方法 rejectedExecution 对齐 JDK）。
4. Deprecated 清零：AllEncompassingFormHttpMessageConverter→FormHttpMessageConverter、nacos 两常量字面量化、AutoServiceRegistration 局部 suppress（上游无替代）。
5. 批量：14 模块 spring-boot-configuration-processor、16 模块 CHANGELOG、oauth2-client QrCodeTest 既有 2E 修复。

## 测试
全仓 673 run / 0 fail / 0 err / 17 skip（dev 凭据）+ FullFlowE2ETest 真实 broker 2/0/0。

# 2026.0900.0006版本升级说明
uw-common 的 MoneyUtils 与 CurrencyUtils 打通，金额计量口径由硬编码「分」升级为币种感知的「最小货币单位」，支持 ISO 4217 全部精度（0/2/3/4 位），修复跨精度币种汇率换算的量级错误、大额金额精度截断与币种代码大小写陷阱；修复 uw_common / uw_validate 国际化资源错置导致的 i18n 全面失效。

## uw-common
1. CurrencyUtils 新增最小货币单位能力：getMinorUnit(Currency/String) 返回币种小数位数（ISO 4217 exponent），getScale(Currency/String) 返回换算比例 10^minorUnit。数据源为 JDK 内置 Currency 表，随 JDK 更新维护，无需自建币种字典。黄金(XAU)等非流通代码 JDK 返回 -1，统一归一化为 0，保证比例恒为正。币种代码解析大小写不敏感并自动去除首尾空白。
2. MoneyUtils 新增币种感知的主单位/最小单位转换：toMajor(long, Currency/String) 与 fromMajor(String, Currency/String)，小数位数按币种决定——CNY 1999→"19.99"、JPY 1000→"1000"、KWD 1999→"1.999"。fromMajor 对超出币种精度的入参抛出 IllegalArgumentException（fail-closed，不静默截断，避免资金差错）。
3. MoneyUtils 新增 exchange(long, String rate, Currency/String from, Currency/String to) 跨币种换算，自动修正两币种的最小单位精度差，公式为 minorTo = minorFrom × rate × 10^(minorUnitTo − minorUnitFrom)。⚠️ 此前用 multiplyRate 直接换算在精度不同的币种间会产生量级错误：100.00 USD 按汇率 150 换日元，未修正得 1500000（150 万日元），正确为 15000 日元，相差 100 倍；两币种精度相同时修正因子为 1，与 multiplyRate 等价。
4. MoneyUtils.Chain 携带币种：新增 of(long, Currency/String)、ofMajor(String, Currency/String) 入口，新增 major() 按币种精度结算、minor() 作为 cent() 的多币种语义别名、currency() 读取币种、exchangeTo(rate, 目标币种) 链式换算（币种变化故返回新实例）。Chain 之间的 add/subtract/gt/gte/lt/lte 校验币种一致性，不一致抛出 IllegalArgumentException 防止串币；equals 对币种不一致返回 false 而非抛异常，以符合 Object.equals 通用约定。

## 兼容性说明
1. toYuan/fromYuan/ofYuan/yuan()/cent() 全部保留且语义不变（内部委托 CNY 实现），存量按「分」计量的调用无需任何改动。币种缺省一律按 CNY 处理。
2. 唯一行为变更：Chain.toString() 由 "1.99" 改为 "1.99 CNY"（追加币种代码，便于日志排查串币）。该方法仅用于调试展示，结算请使用 cent()/minor()/major()/yuan()。
3. 多币种业务请改用 toMajor/fromMajor/exchange —— toYuan/fromYuan 的「元」概念仅对 2 位币种成立，对 JPY(0 位)、KWD(3 位)等会得到错误结果。

## 缺陷修复
1. 修复 i18n 资源错置导致的国际化全面失效：uw_common（CommonResponseCode）与 uw_validate（ValidateResponseCode）两族共 26 个 properties 原置于 uw-common-app，而其归属类在 uw-common。因 ResponseCode 按 codePrefix 推导 basename 从 classpath 加载，仅依赖 uw-common 而不依赖 uw-common-app 的 16 个模块（uw-dao / uw-cache / uw-auth-client / uw-httpclient 等）在运行期找不到资源包，getLocalizedMessage() 静默回退中文兜底文案，所有语种失效且无任何报错。现将资源移至归属模块 uw-common，与 uw-dao / uw-mfa / uw-oauth2-client 各自携带自身资源的既有约定一致。修复后 en/ja/ko 等语种在仅有 uw-common 的环境下正常解析。
2. 修复大额金额精度截断（存量缺陷）：multiplyRate(long,String) 与 multiplyRate(long,double) 原用 MathContext.DECIMAL64（16 位有效数字）做乘法，而 long 最大 19 位有效数字，超过 16 位的金额被静默截断——实测 1234567890123456789 × 1.0 得 1234567890123457000，误差 211。现移除 MathContext 改为精确乘法；exchange 同源问题一并修正，其降精度分支改用 divide(divisor, 0, HALF_UP) 一次定标，既保精度又避免无终止小数抛 ArithmeticException。已补回归测试并经变异测试验证（还原旧实现即失败）。
3. 修复币种代码大小写陷阱：Currency.getInstance 仅接受大写 ISO 代码，此前传入小写（如库表取出的 "jpy"）会抛异常并被静默兜底为默认 CNY，导致 JPY 按 2 位精度换算产生 100 倍错误。现 getCurrency 统一 trim + toUpperCase(Locale.ROOT) 归一化（用 ROOT 而非默认 Locale，避免土耳其语环境下 i→İ 的误转），大小写与首尾空白均不再影响解析。

# 2026.0900.0001版本升级说明
uw-httpclient 对称补齐写方法（POST/PUT/PATCH/DELETE）的请求体能力，新增 BodyQuery（请求体 + queryParam）系列；uw-ai 新增积点计费体系与结构化输出自校正；uw-common 新增全局审批状态 AuditState；同步升级 Spring Boot 4.1.1 / Spring Cloud 2025.1.3 / nacos-client 3.3.0-BETA。

## uw-httpclient
1. 新增 BodyQuery 系列（请求体 + URL queryParam）：postBodyQueryForData/Entity、putBodyQueryForData/Entity、patchBodyQueryForData/Entity、deleteBodyQueryForData/Entity，适用于 JSON body 同时需通过 query 传递分页/筛选参数的场景（如 POST /search?page=1 + 筛选 body）。每族提供 Class/TypeReference/JavaType 三套响应类型 × 带不带 headers 重载，共 32 个公开方法 + 4 个私有底层。
2. DELETE 补齐 Body 与 Form：新增 deleteBodyForData/Entity（带 RequestBody，用于批量删除等场景）、deleteFormForData/Entity。DELETE 现与 PUT/PATCH 对称（无 body queryParam / Form / Body / BodyQuery）。
3. PUT/PATCH 补齐文件上传：新增 putFormFileForData/Entity、patchFormFileForData/Entity，与 postFormFile 同构（MultipartBody + OkHttp .put/.patch）。

## uw-ai
1. 新增 AI 积点计费体系：AiCreditRpc 支持 saas 积点充值/回收（credits 可为负=回收）、余额查询、员工限额查询（周期已过按重置后口径返回）与员工当前周期已用清零；AiClientHelper 新增 rechargeSaasCredits / getSaasCreditsBalance / getUserCreditsLimit / resetUserCreditsLimit 门面方法，配套 AiCreditBalanceVo / AiCreditLimitVo / AiCreditRechargeParam / AiCreditUserLimitParam。
2. AiModelConfigVo 新增模型计费字段：creditRateInput/Output/Cached（积点倍率，千分比，1000=1.0 倍）与 priceInput/Output/Cached（厂商定价，分/百万 token）。
3. AiConfigRpc 接口增加 saasId 首参（listModelConfigByApi / getModelConfig / getApiConfig），configCode/apiCode 按 (saas_id, code) 唯一域解析；AiClientHelper 新增带 saasId 重载（listModelInfoById / listModelInfoByCode / getModelApiByCode），原单参方法委托平台域（saasId=0），兼容存量调用。
4. 新增 StructuredOutputAdvisor 结构化输出顾问：请求侧追加目标类型 JSON Schema 格式说明，输出 JSON 解析失败时携带错误信息与上次输出自校正重试（默认最多 3 次），替代原 BeanOutputConverter 直转。

## uw-common
1. 新增 AuditState 全局审批状态枚举，统一审批状态机值域：INIT(0) 草稿 / APPLY(1) 已申请待审批 / CANCEL(-1) 已撤回 / APPROVE(2) 审批通过 / REJECT(-2) 审批拒绝 / REVOKE(-3) 已撤销 / EXPIRE(-4) 已过期；死单（终态）查询约定 audit_state < 0；与数据状态维 CommonState 正交（audit_state 表达审批生命周期，state 表达数据行生命周期）；未知值解析保守回落 REJECT。

## uw-common-app
1. AuthIdStateQueryParam 构造器自动绑定 saasId，AuthQueryParam 用户类型判断逻辑简化（saas 隔离强化）。
2. 系统日志表结构优化：sys_crit_log 删除 op_state / op_log / exception 冗余字段；sys_crit_log 新增 (saas_id,user_id)、(biz_type,biz_id)、(saas_id,request_date) 索引，sys_data_history 新增 (entity_class,entity_id)、(saas_id,user_id)、(saas_id,create_date) 索引。

## 依赖升级
1. Spring Boot 4.1.0→4.1.1，Spring Cloud 2025.1.2→2025.1.3，nacos-client 3.2.3→3.3.0-BETA。
2. 模块版本联动：uw-ai 1.1.37、uw-common 1.1.113、uw-common-app 1.1.112、uw-log-es 3.1.107。

## ⚠️ 不兼容变更
1. AiConfigRpc 三个方法签名加 saasId 首参，需与 uw-ai-center 服务端同步升级；AiChatRpc.chatGenerate 及 AiClientHelper.chatGenerate 更名为 generateStream。
2. sys_crit_log 删除 3 个冗余列并新增组合索引，存量库需同步执行 DDL 变更（删列 + 建索引）。

# 2026.0800.0002版本升级说明
全面适配 Spring Boot 4.1 + Spring Framework 7 + Jackson 3。

## 核心升级
1. Spring Boot 3.5→4.1，Spring Framework 6→7，Spring Cloud→2025.1。
2. Jackson 2→3：core/databind 包名 com.fasterxml.jackson→tools.jackson；注解包 com.fasterxml.jackson.annotation 保留不变（Jackson 3 仍依赖 annotations 2.x）。
3. nacos-client→3.2.2，fork SCA 2025.1.x 成 uw-nacos-client（脱离 Spring Cloud Alibaba）。

## uw-common-app 重构
1. 新增 CommonAppEnvPostProcessor：Swagger 禁用 + LB 缓存禁用改为免配置默认项（可被 application.yml 覆盖）。
2. 优雅停机交由 uw-nacos-client NacosGracefulShutdownDelegate，删除原 onContextClosedEvent（曾双触发）。
3. 日期输出开关 DATE_AS_ISO：true=ISO 字符串（默认，与 JsonUtils 一致），false=毫秒时间戳。
4. WebMvcConfigurer.extendMessageConverters（Spring 7 废弃）迁移到 configureMessageConverters(ServerBuilder)。

## 各 center 配置统一
1. application.yml 单处 spring.config.import + ${NACOS_*} 环境变量驱动，零 profile 文件（debug/test/uat/prod 均靠环境变量）。
2. nacos dataId 去掉 .yaml 后缀（匹配 nacos 约定）。
3. nacos 配置清除 Jackson 2 残留（write-dates-as-timestamps / 空 serialization 段）。

## 其他
1. uw-ops-agent 跟随升级 uw-common 1.1.112 + uw-httpclient 2.3.5。
2. uw-ai-center ES bean 循环依赖修复（Rest5ClientBuilderCustomizer 分离到独立 Configuration）。
3. pom 整理：module 内版本上提父 dependencyManagement，springdoc 去变量化，删除冗余 maven.compiler.*。

## ⚠️ 不兼容变更
1. Jackson 3：ObjectMapper→JsonMapper（无参构造移除），@ResponseBody 日期默认 ISO 字符串（原毫秒时间戳）。
2. EnvironmentPostProcessor 包路径 org.springframework.boot.env→org.springframework.boot（Spring Boot 4.0 迁移，旧路径 forRemoval）。
3. center 启动需设环境变量：NACOS_SERVER/NACOS_USERNAME/NACOS_PASSWORD/NACOS_NAMESPACE（IDE run config 或部署环境）。

# 2026.0701.0000版本升级说明
uw-task 新增第三种任务类型 TaskDelayer（延迟任务），与 TaskRunner / TaskCroner 对等。

## uw-task
1. 新增 TaskDelayer 延迟任务类型：基于 Redis zset 多实例竞争 poll（Lua 原子 ZRANGEBYSCORE+ZREM 取出到期任务），替代 classic TTL+DLX 延迟队列（规避 per-message TTL 队头阻塞），执行能力随实例数水平扩展、无 Leader 单点。业务方通过 TaskFactory.delayTask() 投递，配置/限速/重试/告警/统计与 Runner/Croner 完全对称。
2. TaskData 改为手工 KryoSerializable 序列化（refObject 不跨 MQ 传输），消息体更小更快。

## ⚠️ 升级注意
1. TaskData 序列化格式变更，升级前需确保 MQ 队列消费空（旧格式消息新版本无法反序列化，会被静默丢弃）。
2. TaskDelayer 依赖 Redis（zset 承载延迟任务），需确保 uw-task 接入的 Redis 可用。

# 2026.0601.0010版本升级说明
重构 ResponseCode / JsonConfigParam 的 i18n 机制，将样板代码内化到接口，涉及公共接口签名变更。

## uw-common
1. ResponseCode 改为自带 i18n 的双方法模式：`getMessage()` 抽象兜底文案（`@JsonIgnore`），`getLocalizedMessage()` default i18n 出口（`@JsonProperty("message")`，按当前 Locale 解析，缺失回退 `getMessage()`）。
2. `code`/`getCode()` 由接口从枚举名自动派生；`messageSource()` 按 `codePrefix()` 自动推导 basename（`i18n/messages/<点转下划线>`）并缓存，实现类无需手写 `ResourceBundleMessageSource`。
3. 删除 `getFullCode()` 系列与旧 `getLocalizedMessage()` 重载。ResponseData / ResponseCodeUtils 调用方同步迁移。

## uw-common-app
1. JsonConfigParam 精简实现样板：`getParamData()` 为唯一抽象方法，`getType/getValue/getTitle/getRegex` 改为 default 委托 `getParamData()`，`getKey()` 由枚举名自动派生；`ParamData` 载体去掉 `key` 字段（实现类构造器少传一个参数，不再手写 `enumNameToDotCase(name())`）。
2. `desc` 更名为 `title`，采用与 ResponseCode 同构的 i18n 双方法模式：`getTitle()`（`@JsonIgnore`）+ `getLocalizedTitle()`（`@JsonProperty("title")`）；i18n 资源走 `configPrefix()` 推导的 `i18n/config/` 目录。
3. JsonConfigHelper 删除两个 String 重载（`JsonConfigParam` 是接口，Jackson 无法反序列化，原为死代码）。
4. ValidateResponseCode / CommonResponseCode 精简为 `getMessage()` + `codePrefix()`。

## uw-dao / uw-mfa / uw-oauth2-client
DaoResponseCode / MfaResponseCode / OAuth2ClientResponseCode 同步精简为 `getMessage()` + `codePrefix()`。

## uw-auth-center / uw-gateway-center
1. AuthCenterResponseCode 的 codePrefix 由 `auth.center` 调整为 `uw.auth.center`（跟随包名命名），i18n 资源文件 `auth_center*.properties` 同步改名为 `uw_auth_center*.properties`。
2. GatewayCenterResponseCode 的 codePrefix 由 `auth.center` 调整为 `uw.gateway.center`（不再与 AuthCenter 共用响应码空间），新建独立的 `uw_gateway_center*.properties` 资源文件（13 语种）。

## ⚠️ 不兼容变更
1. ResponseCode 实现类：`@Override message()` → `getMessage()`；删除 `getCode()`/`messageSource()` 覆写（已由接口自动派生/加载）。
2. ResponseCode 调用方：`getFullCode()` → `getCode()`；取对外消息改用 `getLocalizedMessage()`（`getMessage()` 现为原始兜底文案，不经 i18n）。
3. JsonConfigParam 实现类：`ParamData` 构造器从 5 参 `(key,type,value,desc,regex)` 改为 4 参 `(type,value,title,regex)`（`key` 由接口派生，`desc`→`title`）；只需提供 `getParamData()`，4 个 getter 由接口 default 委托。
4. 前端：JsonConfigParam 序列化字段 `desc` → `title`。
5. 前端：AuthCenter 响应码 fullCode 前缀 `auth.center.*` → `uw.auth.center.*`（涉及 ops-web / root-web 的 login、password、userInfo 等页面共 58 处 `res.code === 'auth.center.xxx'` 判断，需同步改为 `uw.auth.center.xxx`，否则登录双登/多用户/MFA/密码分支判断会失效）。

# 2026.0601.0009版本升级说明
1. uw-common: 新增StringTools解决大量字符串操作相关问题。
2. uw-common: DateUtils更名DateTools，解决ai经常import报错反复修复的问题。

# 2026.0601.0006版本升级说明
这是一个巨大的版本升级，和2026之前的版本不兼容，需要执行升级脚本script/migrate-202605.py。

## 重大升级

1. DataList / ESDataList → PageList（移至 uw.common.data 包）
2. DataSet → PageRowSet（移至 uw.common.data 包）
3. QueryParam / PageQueryParam 移至 uw.common.dto 包
4. ResponseData / ResponseCode 移至 uw.common.response 包

## DaoFactory / DaoManager 方法重命名

1. queryForSingleValue → queryForValue
2. queryForSingleList → queryForValueList
3. queryForSingleObject → queryForObject
4. queryForDataSet → queryForTable
5. executeCommand → execute

## DaoFactory / DaoManager 新增方法

1. queryForList() 系列（16 个重载），委托调用 list() 方法。queryForList 与 queryForValue / queryForObject / queryForTable 构成完整的 query 系列命名。
2. list() 方法保留不变，与 save / update / delete / load 搭配组成 CRUD 动词系列。

## 其他升级

1. uw-auth-client: authRestTemplate 替换为 authRestClient/authWebClient，适配最新的 Spring 版本
2. uw-common: 新增MoneyUtils货币计算工具类，所有金额以分（long）为单位，避免浮点误差。支持加减乘除、汇率换算、分摊等场景，提供静态方法和链式调用（Chain）两套API。
3. uw-common: 新增ValidateUtils数据校验工具类，覆盖字符串、整数、小数、日期时间、网络地址等通用校验，以及中国身份证、手机号、姓名、统一社会信用代码、车牌号等业务校验。

# 2025.0901.0003版本升级说明

1. 升级springboot: 3.5.6
2. uw-common-app: 新增SchemaValidateHelper，基于@Schema标签进行数据校验。
3. uw-common-app: 优化JsonConfigHelper，使用和SchemaValidateHelper一致的校验逻辑。

# 2025.0901.0003版本升级说明

1. uw-ai: 优化代码。
2. uw-common: 优化JsonUtils代码，增强兼容性。
3. uw-httpclient: 优化json解析代码。

# 2025.0901.0002版本升级说明

1. uw-ai: 优化代码。
2. uw-auth-service: 优化依赖。

# 2025.0901.0001版本升级说明

1. 基础库依赖的统一调整，优化依赖引用。
2. uw-common-app: 新增AppBootstrap，简化引用启动代码。

# 2025.0801.0002版本升级说明

1. uw-ai: 完善RPC功能，支持内部SSE调用。
2. uw-auth-client: 支持WebClient。

# 2025.0801.0001版本升级说明

1. uw-ai: 优化代码。

# 2025.0801.0000版本升级说明

1. spring boot升级到3.5.5。
2. uw-ai: 优化代码。

# 2025.0701.0000版本升级说明

1. uw-common-app: 移除JsonConfigParam中的name字段。
2. uw-common-app: JsonConfigParam.ParamType新增textRich类型，用于存储富文本。

# 2025.0601.0033版本升级说明

1. uw-mfa: MfaDeviceCodeHelper增加对DeviceId的校验错误限制，抑制横向爆破。
2. uw-mfa: MfaTotpHelper增加对于UserId的校验错误限制，抑制横向爆破。
3. uw-mfa: 优化i18n文案输出。

# 2025.0601.0028版本升级说明

1. uw-mfa: Captcha可以通过设置ipLimitWarnTimes:0来直接输出Captcha。
2. uw-mfa: MfaFusionHelper的verifyTotpCode, verifyDeviceCode新增了强制校验Captcha的方法。
3. uw-mfa: 移除了MfaCaptchaHelper, MfaDeviceCodeHelper中未使用的userIp参数。

# 2025.0601.0026版本升级说明

1. uw-auth-service: 优化对swagger的过滤。
2. uw-common-app: 在!debug&!dev环境下，自动屏蔽swagger的访问。
3. 升级springdoc到2.8.9。

# 2025.0601.0025版本升级说明

1. uw-mfa: 白名单IP也进行登录校验检查，增强内网安全性。

# 2025.0601.0023版本升级说明

1. 升级spring boot 3.5.3，解决spring自身问题。

# 2025.0601.0020版本升级说明

1. 升级spring boot 3.5.2，解决spring自身的CVE安全漏洞。
2. uw-logback-es: 优化堆栈过滤效率，优化默认堆栈过滤数据。
3. logback-spring.xml: 建议修改项目中的logback-spring.xml的excludeThrowableKeys参数为"
   java.base,org.spring,jakarta,org.apache,com.mysql,okhttp,com.fasterxml,uw.auth.service.filter"

# 2025.0601.0018版本升级说明

1. uw-common-app: 修复nacos优雅关闭问题，让滚动升级丝滑无比。
2. uw-common-app: 修复loadbalancer双重缓存问题，让滚动升级丝滑无比。
3. uw-task: TaskServiceRegistrar优化。

# 2025.0601.0011版本升级说明

1. uw-log-es: 支持自签名ssl的es服务器，主要适配腾讯云香港ES节点。
2. uw-logback-es: 支持自签名ssl的es服务器，主要适配腾讯云香港ES节点。
3. uw-auth-service: 优化IOException的处理，仅输出warn msg。

# 2025.0601.0009版本升级说明

1. uw-common-app: JsonConfigHelper.ParamType新增TEXT长文本类型，支持多行文本，便于前端正确渲染UI。
2. uw-auth-service: 优化日志不打印的警告信息。
3. 多个项目中针对redis连接参数适配腾讯云配置。

# 2025.0601.0008版本升级说明

1. uw-dao: 修复autoCount优化过程中产生的bug。

# 2025.0601.0006版本升级说明

1. uw-dao: 优化autoCount相关方法，尽量减少数据库count的查询。优化sql执行统计相关代码。
2. uw-common: SystemClock增加了nowDate()方法，优化CPU占用，支持低速模式。
3. uw-ai,uw-auth-service,uw-gateway-client,uw-mydb-client: UriComponentsBuilder.fromUriString 替代 fromHttpUrl
4. uw-auth-service: 优化IpWebUtils.getIp()方法，返回正确的IP地址。
5. 所有模块针对SystemClock的优化。

# 2025.0601.0002版本升级说明

1. knife4j由于兼容性问题不再使用，项目已经装死两年，直接替换为springdoc，目测速度明显提升。
2. uw-common: DateUtils新增minutesDiff,hoursDiff,daysDiff,weeksDiff,monthsDiff,yearsDiff方法。

# 2025.0601.0001版本升级说明

1. 升级到spring boot 3.5.0 & spring cloud 2025.0.0
2. uw-cache: GlobalCache,GlobalCounter,GlobalHashSet,GlobalLocker,GlobalSortedSet 新增通过key前缀获取key列表功能。
3. uw-cache: 优化FusionCounter相关功能，修复了数据库回写逻辑。
4. uw-cache: 优化FusionCache相关功能，增加了keys相关方法。
5. uw-httpclient: 适配spring boot 3.5.0

# 2025.0501.1028版本升级说明

1. uw-common: DateTools 新增stringToDate()新增自动高效解析时间戳格式。
2. uw-log-es: 移除重复的@PreDestory指令。
3. uw-log-back: 增强JMXBean功能。

# 2025.0501.1026版本升级说明

1. uw-common: DateUtils新增stringToDate()新增自动高效解析日期格式的方法，可以自动识别日期格式。
2. uw-common: JsonUtils针对日期格式进行处理，使用默认时区，可以自动解析日期格式。
3. uw-common-app: 优化spring mvc的RequestParam和Json对于多种日期格式的适配处理。
4. uw-dao: DataUpdateInfo变更对象由Pair更换为UpdateInfo，解决初始值为null导致的异常。
5. uw-httpclient: 更新了ObjectMapper的初始化参数，和JsonUtils同步。
6. uw-log-es: 优化批量插入后的逻辑，增加在状态码200但逻辑报错情况下的错误日志。
7. uw-logback-es: 优化日期格式的处理，默认使用ISO8601格式。

# 2025.0501.1025版本升级说明

1. uw-auth-service: 将默认受保护路径修改为全局匹配，全面接管权限控制。
2. uw-auth-service: 重构权限验证流程，支持无权限模式。
3. uw-auth-service: 优化权限异常处理，支持抛出自定义错误信息。

# 2025.0501.1022版本升级说明

1. spring boot升级到3.3.12。
2. uw-dao: 优化DataEntity结构化变更信息。优化DataEntity代码量，增强执行性能。本次升级需要重新生成Entity代码！
3. uw-dao: 优化底层反射调用，提升性能。
4. uw-common: 优化JsonUtils对日期的处理，默认显示为ISO8601格式。
5. uw-common-app: 优化SysDataHistoryHelper，优化异常处理。

# 2025.0501.1016版本升级说明

1. uw-cache: FusionCache.Config新增autoNotifyInvalidate参数，用于优化控制数据更新通知。

# 2025.0501.1015版本升级说明

1. uw-cache: FusionCache优化加载数据成功之后，会自动发送一条invalidate通知，同步数据到整个集群中。

# 2025.0501.1012版本升级说明

1. uw-log-es: 新增了对分组聚合的若干工具方法。
2. uw-common: JsonUtils新增了convert方法，用于将map转换为vo对象。

# 2025.0501.1011版本升级说明

1. uw-log-es: 完善新版es对于多种复杂聚合类型的查询支持。

# 2025.0501.1010版本升级说明

1. uw-auth-service: 修正特定情况下，当一级菜单下没有权限的时候，需要过滤一级菜单的情况。

# 2025.0501.1009版本升级说明

1. uw-auth-service: 修正一级菜单扫描的bug，对权限列表进行与排序。

# 2025.0501.1008版本升级说明

1. uw-dao: 优化QueryParam的排序功能，添加了 CLEAR_SORT 方法，用于清除排序条件。

# 2025.0501.1005版本升级说明

1. uw-auth-service: AuthServiceFilter中过滤器异常增加堆栈输出，并完善报错信息。

# 2025.0501.1003版本升级说明

1. uw-auth-service: 新增 getSaasIdByHost 方法，用于根据 saasHost 获取 saasId。游客前端可以不强行指定saasId了。

# 2025.0501.1002版本升级说明

1. uw-auth-service: 优化权限上报机制，当菜单下无权限的时候，自动屏蔽该菜单。

# 2025.0501.1001版本升级说明

1. uw-task: 优化队列参数Enum，优化TaskData.builder()参数，简化配置项。
2. uw-httpclient: http异常抛出HttpRequestException, HttpRequestException继承TaskPartnerException。
3. uw-log-es: 日志基类完善swagger注释。

# 2025.0501.1000版本升级说明

1. uw-gateway-client: 新增uw-gateway-client模块，将原uw-auth-service中的限速控制接口迁移到uw-gateway-client中。
2. 升级spring boot 3.3.11。

# 2025.0330.1053版本升级说明

1. uw-dao: QueryParam的排序优化，支持多字段排序。新增ADD_SORT方法一次性设定排序字段和排序方法，并支持多次调用。
2. uw-common: ResponseData的warn/error/fatal方法，新增单独返回数据信息的方法。

# 2025.0330.1051版本升级说明

1. uw-dao: 优化update操作，增加实体加载标志。此功能升级需要重新生成Entity代码。
2. uw-dao: save和update之后的对象，将会清除更新标记。
3. uw-auth-service: 将UserType.ANYONE修改为UserType.ANY，并作为默认值。
4. uw-auth-service: ServiceRpc方法中的getUser改为loadUser，getUserList改为listUser，getUserGroupList改为listUserGroup。
5. uw-mfa: 增加了对IP限制类型的列表获取功能。
6. uw-common-app: 删除部分AuthIdStateQueryParam和IdStateQueryParam的构造器，避免混淆。

# 2025.0330.1038版本升级说明

1. uw-dao: QueryParam的SET_COND_SQL可以支持多次调用。
2. uw-dao: DataSet字段索引从0开始(之前从1开始)，需要特别注意，并检查所有相关代码。
3. uw-auth-service: 优化GlobalExceptionAdvice的异常处理，新增对UserIp和RequestMethod的输出，方便定位错误。

# 2025.0330.1037版本升级说明

1. uw-common-app:
   ResponseData完善了链式函数调用语法，包括onSuccess,onNotSuccess,onError,onNotError,onWarn,onFatal，新增传入类型Runnable，减少命名负担。
2. uw-common-app: ResponseData增加了map函数，支持将ResponseData转换为特定类型返回。
3. uw-common-app: QueryParam(IdQueryParam,IdStateQueryParam...)的Id参数类型由long修改为Serializable，方便传递更多数据类型。
4. uw-dao: DaoManager对于DATA_NOT_FOUND_WARN类型的错误，返回ResponseData的state为warn。
5. uw-dao: DaoManager对于PageList,DataSet,effectedNum的返回类型，在isNotSuccess的情况下data返回默认空数值或0/-1，减少不必要的判空。
6. uw-dao: DaoManager对于异常报错的输出，将针对生产环境(prod/stab)优化，自动屏蔽sql输出信息。
7. uw-dao: PageList新增stream()方法，允许使用stream()方法进行数据流处理。
8. uw-dao: DataSet新增了map方法，支持转换为vo类型。
9. uw-auth-service: 异常情况下，完善错误码和错误信息输出，同时输出RequestPath，便于定位错误。

# 2025.0330.1031版本升级说明

1. 谨慎升级！！！升级本版本需要重新生成代码！！！
2. 优化DataEntity对象，增加了ENTITY_TABLE(),ENTITY_NAME(),ENTITY_ID()直接获取表名，实体名，id。
3. 优化了SysDataHistoryHelper，简化代码。

# 2025.0330.1030版本升级说明

优化GlobalResponseAdvice，GlobalExceptionAdvice的异常情况处理。

# 2025.0330.1026版本升级说明

1. 优化操作日志中记录http请求字符集问题。
2. 在AuthServiceHelper中增加logSysInfo，用于记录系统操作，比如回调中操作库表结构。
3. 强化uw-common-app中对于通用queryParam优化，强化支持IdQueryParam和IdStateQueryParam，并可以用过SET_COND_PARAM链式调用增强额外条件。

# 2025.0330.1025版本升级说明

1. 修正特定情况下ipv6地址判定问题导致ip匹配失效。

# 2025.0330.1023版本升级说明

1. 修正密码复杂度过于严格的问题。
2. uw-mfa发送验证码默认为6位。
3. 修正http状态498输出问题。

# 2025.0330.1022版本升级说明

1. 移除了uw-auth-service的authBasePackage配置项，改用spring扫描。

# 2025.0330.1019版本升级说明

1. 降低totp加密算法级别，以自动适应免费客户端。
2. 通过自动适配权限，解决部分菜单不显示问题。

# 2025.0330.1018版本升级说明

本版本是一个较大升级，需要同步升级包括uw-auth-center和uw-gateway。

## 安全升级：

1. 全面支持IPV6校验，数据库中对IP记录的字段推荐为varchar(50)。
2. 全面支持MFA，包括手机、邮件验证码，TOTP认证。
3. 密码策略管理，可针对用户类型设定不同的密码策略。可设置密码重置策略，过期策略，复杂性策略，token生命周期等。
4. MscPermDeclare默认值策略优化，用户类型默认为ANYONE,校验类型默认为USER，日志默认不记录。
5. token类型增加TEMP_TOKEN，PAID_TOKEN,SUDO_TOKEN，提升更多场景应用的可能。

## 功能改进

1. uw-dao新增DaoManager，通过ResponseData统一返回结果集和错误信息，结合onSuccess/onError的lambda简化异常管理。后期建议全部迁移到DaoManager。
2. 底层支持i18n，通过ResponseCode结合ResponseData和AI生成技术，支持从enum到i18n多语言资源文件自动生成。
3. JsonConfigHelper类库组合，统一了数据库中json配置文件格式，支持前后端协同的动态强类型配置管理。后期建议AIS也升级过来。

## 升级注意点

1. 原有的uw-app-common组件更名为uw-common-app，对应的包名从uw.app.common变更为uw.common.app。
2. uw-common-app的StateCommon枚举更名为CommonState。
3. UserType.ANONYMOUS变更为UserType.ANYONE，并作为默认值，表示任意用户类型。
4. SysCritLog的refType,refId变更为bizType,bizId。
5. uw-dao的batchSave()方法变更为save()。
6. ResponseData的prototype()方法变更为raw()。
7. auth-client中的注入的bean:tokenRestTemplate变更为authRestTemplate。
8. QueryParam的EXT_WHERE_SQL=>EXT_COND_SQL，ADD_EXT_PARAM=>ADD_EXT_COND，新增ADD_EXT_COND_PARAM用于直接参数赋值。