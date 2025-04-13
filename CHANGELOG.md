# 2025.0330.1011版本升级说明
本版本是一个较大升级，需要同步升级包括uw-auth-center和uw-gateway。

## 安全升级：
1. 全面支持IPV6校验。
2. 全面支持MFA，包括手机、邮件验证码，TOTP认证。
3. 密码策略管理，可针对用户类型设定不同的密码策略。可设置密码重置策略，过期策略，复杂性策略，token生命周期等。
4. MscPermDeclare默认值策略优化，用户类型默认为ANYONE,校验类型默认为USER，日志默认不记录。
5. token类型增加TEMP_TOKEN，PAID_TOKEN,SUDO_TOKEN，提升更多场景应用的可能。

## 功能改进
1. uw-dao新增DaoManager，通过ResponseData来统一返回结果集和错误信息，结合onSuccess/onError函数调用简化异常管理。后期建议全部迁移到DaoManager。
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