[TOC]

## 简介
uw-app-common是基于uw框架web端的公共库。
* 基于uw-auth-service的AuthServiceHelper和uw-dao的QueryParam的自动化权限注入管理。
* 基于后台应用管理中，常见的关键操作日志(SysCritLog)和数据历史(SysDataHistory)功能。
* 基于Json的应用配置管理功能。

## 后台查询的自动化权限注入管理
通过结合uw-auth-service的AuthServiceHelper和uw-dao的QueryParam，提供自动化权限注入管理。
权限自动化管理通过四个关键字段来组成，包括saasId,mchId,groupId,userId。
* saasId：saasId，默认为0，表示当前系统为单租户系统。
* mchId：mchId，默认为0，表示当前系统为单商户系统。
* groupId：groupId，默认为0，表示当前系统为单用户组系统。
* userId：userId，默认为0，表示当前系统为单用户系统。
同时，QueryParam支持链式调用，这样基本上可以消灭代码中的sql连接，消除sql注入隐患。

## 后台关键操作记录
后台操作历史已经由uw-auth-service的uw.auth.action.log日志实现，但是Es的生命周期管理可能会导致日志的周期性删除。
通过设置Log.CRIT属性，可以将关键日志中转之第三方存储系统，如默认的AuthCriticalLogStorage，可以将日志存储于mysql数据库中。

## 数据历史记录
通过SysDataHistoryHelper，可以将数据变更记录到数据库中，便于数据回滚。
同时提供了对应的QueryParam和entity，方便后期查询。

## JSON配置管理
通过JsonParamHelper和JsonParam，可以方便的将JSON配置管理到数据库中，方便后期查询。
通过对JsonParam对参数进行定义，实际存储可使用Map结构。
同时提供了配置参数读取的强类型方法，提供了强类型的校验方法。
* JsonParamHelper: 可以方便的将JSON配置管理到数据库中，方便后期查询。
* JsonParamBox: 封装一组配置数据到一个盒子中，提供参数校验和类型化读取方法。
* JsonParam: Json配置参数定义类。封装了类型，变量key，变量默认值，参数名,参数描述，校验规则等信息。
* JsonParamType: Json配置参数类型。

## i18n国际化
针对i18n国际化，提供了uw-app-common的i18n国际化支持。