# uw-common

uw-common 通用组件库。提供响应封装、分页容器、日期/货币/校验/加密/序列化等基础工具，是所有 uw 服务的底层依赖。

## Maven 引入

```xml
<dependency>
    <groupId>com.umtone</groupId>
    <artifactId>uw-common</artifactId>
    <version>${uw-common.version}</version>
</dependency>
```

## 目录

- [包结构](#包结构)
- [response 包 — 响应封装](#response-包--响应封装)
- [data 包 — 数据容器](#data-包--数据容器)
- [dto 包 — 查询参数](#dto-包--查询参数)
- [util 包 — 工具类](#util-包--工具类)

## 包结构

```
uw.common
├── response/   # ResponseData / ResponseCode 响应封装与状态码
├── data/       # PageList / PageRowSet / KryoData 数据容器
├── dto/        # QueryParam / PageQueryParam 查询参数基类
└── util/       # 20+ 工具类（详见下文）
```

---

## response 包 — 响应封装

### ResponseData

基于泛型的统一响应对象。全项目所有接口（前端返回、服务间调用）都用它封装，避免分散的异常处理。

**封装结构**：

| 字段 | 类型 | 说明 |
|------|------|------|
| time | long | 时间戳（优化后的静态构造方法已去除 time 返回） |
| state | String | 响应状态（SUCCESS / WARN / ERROR / FATAL） |
| code | String | 响应状态码 |
| msg | String | 响应消息 |
| data | T | 响应数据（泛型） |
| type | String | 响应数据类型（注意：不能正确处理嵌套泛型，仅返回集合类型） |

**四类状态**：

| 状态 | 含义 | 是否有数据 |
|------|------|-----------|
| SUCCESS | 成功 | 有 |
| WARN | 警告 | 有（有警告但仍有数据返回） |
| ERROR | 错误 | 一般无 |
| FATAL | 严重失败 | 无 |

**构造方法**（静态）：

- 基于状态：`success(data)` / `warnCode(code)` / `errorCode(code)` / `fatalCode(code)`
- 系列方法：`xxx()` / `xxxCode()` / `xxxMsg()`
- ⚠️ **泛型陷阱**：`warn("CODE","msg")` 会被推断成 `warn(T t, String code)` 导致 "CODE" 变成 data。**正确写法是 `warnCode("CODE","msg")`**。

**状态判断**：`isSuccess()` / `isNotSuccess()` / `isWarn()` / `isError()` / `isFatal()` / `isNotError()`

**链式回调**（`onSuccess` / `onWarn` / `onError` / `onFatal` / `onNotSuccess` / `onNotError`，每个有 Function/Consumer/Runnable 三种重载）：

```java
// Consumer 版：成功后做副作用（缓存失效等），返回自身
return dao.save(user).onSuccess(saved -> FusionCache.invalidate(User.class, saved.getId()));

// Function 版：成功后转换，返回新的 ResponseData<R>
return dao.queryForObject(Product.class, param)
    .onSuccess(product -> {           // 加载 → 修改 → 更新
        product.setState(1);
        return dao.update(product);    // 返回 update 的 ResponseData<Integer>
    });
```

**性能优化**：高并发场景下，系统提供了静态实例 `SUCCESS` / `WARN` / `ERROR` / `FATAL` 可直接复用，减少对象创建开销。优化后的构造方法已**去除 time 字段返回**。

**特别用法**：
- `row()` — 返回未泛型化的原始实例，适合返回错误/警告响应。
- `toString()` — 直接返回 JSON 序列化字符串。
- `map(Function)` — 将整个 ResponseData 转为任意类型。

### ResponseCode

响应状态码统一接口。业务枚举实现此接口，实现类型安全的响应码管理（替代硬编码字符串）。

```java
public enum BizResponseCode implements ResponseCode {
    USER_NOT_FOUND("用户不存在");
    private final String code;
    private final String message;
    BizResponseCode(String message) {
        this.code = EnumUtils.enumNameToDotCase(this.name());  // user.not.found
        this.message = message;
    }
    @Override public String getCode() { return code; }
    @Override public String getMessage() { return message; }
}
```

> 配套工具 `ResponseCodeUtils.toProperties(Class)` 可将枚举导出为 i18n 资源文件。

---

## data 包 — 数据容器

### PageList\<T\>

分页列表容器，组合分页信息与泛型列表数据，实现 `Iterable<T>`。

```java
PageList<User> users = dao.list(User.class, param).getData();
// 字段：startIndex / resultNum / size / sizeAll / page / pageCount / list
// 方法：get(i) / getFirst() / getLast() / size() / sizeAll() / isEmpty() / stream() / for-each
```

> JSON 序列化仅输出分页字段 + list，`@JsonIgnore` 的 getter 不输出。

### PageRowSet

行列结构的二维分页数据容器，支持游标遍历和按列名/索引取值（不依赖 `java.sql`，由 DAO 层从 ResultSet 转换）。

```java
PageRowSet rs = dao.listRowSet(...).getData();
while (rs.next()) {
    long id = rs.getLong("id");
    String name = rs.getString("user_name");
}
// 转 PageList：rs.map(row -> new UserVO(...))
```

### KryoData

Kryo 手工序列化数据接口。数据类自身负责通过 `Output/Input` 原语方法逐字段读写，配合 `KryoUtils.serializeData/deserializeData` 使用。适合作为「数据契约」的高性能序列化场景。

---

## dto 包 — 查询参数

### QueryParam\<P\>

查询参数基类。定义排序方向（不排序/正序/倒序）、like 最小长度（少于则转为 `=` 查询）等通用查询语义。

### PageQueryParam

分页查询参数，继承 QueryParam。定义分页模式（仅统计分页 / 仅返回数据 / 同时返回）、当前页码、每页大小等。

---

## util 包 — 工具类

### MoneyUtils — 货币计算

所有金额以 **分（long）** 为单位，避免浮点误差。溢出或除零抛 `ArithmeticException`。提供静态方法和链式调用（Chain）。

```java
// 静态方法
long fee = MoneyUtils.add(100, 200);                    // 300
long total = MoneyUtils.multiplyBps(10000, 850);        // 8500（85折）
long[] parts = MoneyUtils.allocate(100, new long[]{3,3,4}); // 按比例分摊，尾差兜底
String yuan = MoneyUtils.toYuan(19900);                 // "199.00"
String cn = MoneyUtils.toChinese(25550);                // "贰佰伍拾伍元伍角整"

// 链式
long r = MoneyUtils.of(10000).multiply(3).multiplyRate("0.85").add(500).cent();  // 25550
```

### ValidateUtils — 数据校验

所有方法返回 `boolean`，不抛异常，null 统一返回 false。中国特有方法以 `China` 前缀命名。

| 类别 | 方法 |
|------|------|
| 字符串 | `isNotEmpty` / `isNotBlank` / `isLengthInRange` / `isDigits` / `isLetters` / `isAlphanumeric` / `isStrongPassword` |
| 数值 | `isInteger` / `isPositiveInteger` / `isNonNegativeInteger` / `isDecimal` / `isDecimalWithScale` / `isInRange` |
| 日期时间 | `isDate` / `isTime` / `isDateTime`（含闰年校验、严格解析） |
| 网络 | `isEmail` / `isUrl` / `isIpv4` / `isIpv6` / `isIp`（IPv4 或 IPv6） |
| 中国业务 | `isChinaMobile` / `isChinaIdCard` / `isChinaName` / `isChinaUscc` / `isChinaPlateNo` |

### MaskUtils — 数据脱敏

对敏感信息做掩码处理。null 输入返回 null，不抛异常。所有方法统一 `mask` 前缀。

| 类别 | 方法 | 示例 |
|------|------|------|
| 通用（固定掩码） | `mask(input, pre, suf, maskStr)` / `maskSecret(secret)` | 凭证/密钥（不泄漏长度） |
| 通用（按原长填星） | `maskByLength(input, pre, suf)` | `13812345678 → 138****5678` |
| 个人信息 | `maskChinaMobile` / `maskTelephone` / `maskChinaIdCard` / `maskPassport` / `maskChinaName` / `maskBankCard` / `maskEmail` | 手机/座机/身份证/护照/姓名/银行卡/邮箱 |
| 机构资产 | `maskChinaUscc` / `maskChinaTaxNo` / `maskChinaPlateNo` / `maskAddress` | 信用代码/税号/车牌/地址 |
| 设备社交 | `maskImei` / `maskWechatId` / `maskIpv4` | IMEI/微信号/IP |

> 两类 API：`mask`（固定掩码串，不泄漏长度结构，适合凭证）；`maskByLength`（按原长逐位填星，适合展示）。业务语义化方法内部走 `maskByLength`。

### SystemClock — 高性能系统时钟

动态自适应时钟。调用频率低时直接读系统时钟，频率高时（>10/ms）自动切换为定时器刷新的缓存值。**`createDate` / `modifyDate` 赋值一律用 `SystemClock.nowDate()`**，不要用 `new Date()` 或 `System.currentTimeMillis()`。

| 方法 | 说明 |
|------|------|
| `now()` | 当前时间戳（毫秒），高频场景比 `System.currentTimeMillis` 快约 40 倍 |
| `nowDate()` | 当前 Date |
| `elapsedMillis(start)` / `elapsedMillis(start, end)` | 耗时计算 |

### SnowflakeIdGenerator — 分布式雪花 ID

基于 Snowflake 的全局唯一 ID 生成器（时间戳 + 机器ID + 序列号）。machineId 按优先级解析：环境变量 `MACHINE_ID` → HOSTNAME 稳定 hash → UUID 随机降级。

```java
long id = SnowflakeIdGenerator.getInstance().generateId();
```

### DateUtils — 日期工具

提供丰富日期格式常量（`DATE_TIME` / `DATE` / `TIME` / `DATE_SIMPLE` 等）与日期运算。

| 类别 | 方法 |
|------|------|
| 格式化/解析 | `dateToString(date, format)` / `stringToDate(str, format)` |
| 偏移 | `offsetDay` / `offsetMonth` / `offsetYear` / `offsetHour` / `offsetMinute` / `offsetSecond` |
| 差值 | `daysDiff` / `hoursDiff` / `minutesDiff` / `monthsDiff` / `yearsDiff` |
| 区间 | `beginOfToday` / `endOfToday` / `beginOfMonth` / `beginOfYear` / `beginOfYesterday` 等 |
| 转换 | `dateToLocalDate` / `localDateToDate` / `dayOfMonth` / `dayOfWeek` |

> ⚠️ 方法名是 `dateToString` / `stringToDate` / `offsetDay` / `beginOfToday`，不是 `format` / `parse` / `addDays` / `getDayStart`。

### JsonUtils — JSON 工具

基于 Jackson，封装常见操作，解析失败抛 `RuntimeException`（无需 try-catch）。

| 方法 | 说明 |
|------|------|
| `toString(Object)` / `toBytes(Object)` | 序列化为 JSON 字符串 / 字节数组 |
| `parse(String, Class<T>)` | 反序列化 |
| `parse(String, TypeReference<T>)` / `parse(String, JavaType)` | 泛型反序列化（如 `List<User>`） |
| `convert(Object, Class<T>)` | 对象类型转换（如 Map → POJO） |
| `write(Object, OutputStream)` | 序列化到流 |
| `getJsonMapper()` | 获取底层 ObjectMapper |

### KryoUtils — Kryo 序列化

高性能二进制序列化，三种方式：整对象反射（`serialize(Object)`）、lambda 手工（`serialize(Consumer)`）、KryoData 接口式（`serializeData/deserializeData`）。

> ⚠️ Kryo 反序列化要求**具体实现类**，不能用接口类型（List/Map/Set 要用 ArrayList/LinkedHashMap/HashSet）。

> ⚠️ **异常与对外报错安全（强制）**
>
> `KryoUtils` 的所有方法**不吞异常**：反序列化失败（字段错位、类型不符、数据损坏，如旧协议残留）时，kryo 直接抛 `KryoException` / `KryoBufferUnderflowException`，由**调用方**负责 catch。
>
> 此类异常的 message 携带序列化框架内部关键字（`kryo`、`Buffer underflow`、字段读取位置等），**禁止出现在对外响应**——否则会暴露服务端序列化实现与协议结构，构成信息泄露（攻击者可据此构造畸形 payload 探测/绕过鉴权）。规范如下：
>
> 1. **面向外部的边界（Controller / RPC / 对外响应）必须 catch**，不得让 `KryoException` 冒泡到全局异常处理器后原样回传客户端。
> 2. **对外响应用固定文案**（如 `"token invalid or corrupted."`），**不要拼接 `e.getMessage()` / `e.toString()`**；详细原因只进服务端日志。
> 3. **缓存 / MQ 等批量读取**（`GlobalHashSet` / `GlobalSortedSet` / `GlobalCache` 等）**逐条 catch 跳过**脏数据，单条损坏不得拖垮整个批量结果。
> 4. 反面教材（禁止）：`return ResponseData.errorCode(UNAUTHORIZED, "failed: " + e.getMessage())` —— 会把 `Buffer underflow.` 直接回传客户端。
>
> 正确示例见 `KryoUtils` 类注释与 `MscTokenService.verifyAuthToken` / `loadAuthTokenData` 的兜底实现。

### 加密签名类

| 类 | 核心方法 | 说明 |
|----|---------|------|
| `AESUtils` | `generateKey(keySize)` / `encryptString(key, data)` / `decryptString(key, enc)` | AES 对称加密，推荐自动 IV 版本（密文自带 IV） |
| `BizAESBox` | `getInstance(configPath)` / `encrypt(data)` / `decrypt(enc)` / `genAesConfig()` | 业务 AES 盒子，配置文件管理密钥向量，加密结果一致 |
| `RSAUtils` | `genKeyPair(keySize)` / `encrypt` / `decrypt` / `sign` / `checkSign` | RSA 非对称加密签名 |
| `DigestUtils` | `sign(msg, Algorithm)` / `signHex(msg, Algorithm)` / `bytesToHex(bytes)` | 摘要签名，`Algorithm` 枚举：MD5/SHA(SHA-1)/SHA_256/SHA_384/SHA_512/SHA3_256/SHA3_512 |
| `HmacUtils` | `sign(message, secret)` / `verify(message, secret, signature)` | HMAC-SHA256 签名验证 |

### 其他工具类

| 类 | 核心方法 | 用途 |
|----|---------|------|
| `BitConfigUtils` | `isOn(config, bit)` / `on(config, bits)` / `off(config, bit)` / `countOn(config)` | 位运算开关（int 32 位 / long 64 位） |
| `ByteArrayUtils` | `intToByteArray` / `byteArrayToInt` / `hexToByteArray` / `byteArrayToString` 等大小端转换 | 数值 ↔ 字节数组转换，底层协议开发 |
| `IpMatchUtils` | `sortList(ipList)` / `matches(sortedList, ip)` / `parseInetAddress(ip)` | IP 段匹配（先排序再二分查找） |
| `NumCodeUtils` | `confuseNum(num)` / `clarifyNum(enc)` | 数字编码混淆 |
| `EnumUtils` | `getEnumMap(basePackage)` / `enumNameToDotCase` / `enumNameToHyphenCase` / `enumNameToCamelCase` | 枚举转换（名称转点号/连字符/驼峰） |
| `ChineseUtils` | `convertToPinyin` / `getShortPinyin` / `similarDegree` / `lcsSimilarDegree` / `toSBC` / `toDBC` | 拼音、相似度（0~10000）、全半角转换 |
| `CurrencyUtils` | `getCurrency(code)` / `getAvailableCurrencies()` / `CURRENCY_DEFAULT`(CNY) | 币种工具 |
| `ExceptionUtils` | `exceptionToString(Throwable)` | 过滤框架堆栈的异常格式化 |
| `ResponseCodeUtils` | `toProperties(Class)` / `toPropertyString(Class)` | ResponseCode 枚举导出为 i18n Properties |
| `LimitedVirtualThreadExecutor` | `new LimitedVirtualThreadExecutor(maxConcurrency)` / `submit(Runnable)` | 限流虚拟线程执行器（Semaphore 背压，支持 Block/FailFast/CallerRuns/Discard 策略） |

---

## 命名约定

- 工具类统一 `XxxUtils` 后缀，`final class` + 私有构造（部分早期类未加 final）。
- `null` 输入约定：`ValidateUtils` 返回 false，`MaskUtils` 返回 null，`JsonUtils`/`DateUtils` 行为见各方法注释。
- 中国特有方法以 `China` 前缀命名（`ValidateUtils.isChinaMobile` / `MaskUtils.maskChinaMobile`），保持跨类一致。
