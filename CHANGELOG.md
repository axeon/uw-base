# 2025.0330.1050版本升级说明
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