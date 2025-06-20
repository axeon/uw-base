# 2025.0601.0020版本升级说明
1. 升级spring boot 3.5.2，解决spring自身的CVE安全漏洞。
2. uw-logback-es: 优化堆栈过滤效率，优化默认堆栈过滤数据。
3. logback-spring.xml: 建议修改项目中的logback-spring.xml的excludeThrowableKeys参数为"java.base,org.spring,jakarta,org.apache,com.mysql,okhttp,com.fasterxml,uw.auth.service.filter"

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
1. uw-common: DateUtils 新增stringToDate()新增自动高效解析时间戳格式。
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
1. uw-common-app: ResponseData完善了链式函数调用语法，包括onSuccess,onNotSuccess,onError,onNotError,onWarn,onFatal，新增传入类型Runnable，减少命名负担。
2. uw-common-app: ResponseData增加了map函数，支持将ResponseData转换为特定类型返回。
3. uw-common-app: QueryParam(IdQueryParam,IdStateQueryParam...)的Id参数类型由long修改为Serializable，方便传递更多数据类型。
4. uw-dao: DaoManager对于DATA_NOT_FOUND_WARN类型的错误，返回ResponseData的state为warn。
5. uw-dao: DaoManager对于DataList,DataSet,effectedNum的返回类型，在isNotSuccess的情况下data返回默认空数值或0/-1，减少不必要的判空。
6. uw-dao: DaoManager对于异常报错的输出，将针对生产环境(prod/stab)优化，自动屏蔽sql输出信息。
7. uw-dao: DataList新增stream()方法，允许使用stream()方法进行数据流处理。
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