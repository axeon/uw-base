#### 1.简述一下uw-auth的主要功能和特性

 uw-auth主要是用来管理分布式API，用户Token的分发，用户API的授权和鉴权。 

1. 基于spring boot实现，支持spring cloud的相关特性。
2. 优化的TOKEN设计，比jwt性能高，安全性强。
3. 针对行业SAAS的b2b2c营销体系专门设计，角色定义内置化。
4. 使用JAVA注解实现权限系统的定义，注册上传、鉴权日志功能，去除冗繁的管理操作。
5. 支持多重的访问限速控制功能。
6. 结合uw-codegen-center，可以辅助生成前端代码，包括api/router，可自动组织功能菜单。

#### 2.uw-auth由哪几个模块组成？每个模块都有什么作用？

uw-auth由uw-auth-center，uw-auth-service，uw-auth-client三部分组成。

- uw-auth-center 系统鉴权中心，负责系统功能、权限；SAAS、用户组、用户的信息、权限存储；token生成和管理；提供用户权限相关业务功能的外部访问接口。
- uw-auth-service 权限服务的服务器端模块，为业务功能模块提供功能权限的注册、鉴权功能，同时提供访问限速功能。
- uw-auth-client 内部RPC调用的客户端，对于无内部调用的情况下，客户端不是必须的。

#### 3.如果我要设置api仅限开发运维用户调用，该如何修改MscPermDeclare注解的属性？

```java
@MscPermDeclare(type = UserType.OPS, auth = AuthType.USER)
```

#### 5.ResponseAdviceIgnore注解有什么作用？什么时候会使用这个注解？

 某些程序内部调用的REST接口（如RPC接口），建议使用@ResponseAdviceIgnore注解来避免ResponseData<>封装，减小封装开销。  

#### 6.可以通过AuthServiceHelper的哪个方法获取用户token信息？AuthServiceHelper还有什么功能？

 AuthServiceHelper.getContextToken()用于获得当前用户的token信息。 

 AuthToken.getTokenPerm()用于获得用户的辅助权限信息，包括黑白名单，限速信息，权限表，自定义数据配置Map表（可用来存储包括业务权限信息，诸如销售地区限制表，业务授权产品id等业务数据信息）。 

#### 7.假如要编写一个c站用户用于在商品搜索栏搜索商品列表的接口，那么MscPermDeclare注解应该设置什么用户类型权限和验证方式？你认为这个接口需要限流吗？如果需要，怎么设定RateLimitDeclare注解的target属性？

如果游客不能查看商品列表的情况下，MscPermDeclare中type可以设置为GUEST，auth可以设置为USER。这个接口需要限流，因为这是一个面向用户的公开接口，可能会有用户频繁刷新、脚本疯狂刷新、ddos大量请求等情况。可以将RateLimitDeclare注解的target设置为IP，根据IP限速。

#### 8.MscPermDeclare注解中auth的不同设置有什么差异？对于菜单生成的影响？

- NONE：不验证
- USER：仅验证用户类型
- PERM：验证用户类型和权限

只有auth设置为perm的情况下，才会生成菜单项。

#### 9.MscPermDeclare注解中log设置之间有什么差异？对于不同方法的要求？

- NONE：不记录
- REQUEST：记录请求数据
- RESPONSE：记录返回数据
- ALL：记录请求和返回数据

因为性能问题,返回大数据量时不建议记录返回内容，因此对于list的话，仅记录请求就够了。对于增删改的关键操作，最好请求和返回都要记录。

#### 10. \$PackageInfo\$类的作用是什么？

用于生成更全面的菜单属性结构。在此文件中定义了一级菜单结构。

#### 11.已经设定MscPermDeclare的API，没有受到保护，是什么原因？

1.是否开了debug？调试时修改了enable-auth-filter为false？

2.api的path不在受保护的配置中。

#### 12.对于增删查改列的参数和返回为什么有要求？都有什么要求？

主要是为了前后端协作和代码生成一致性的要求。
对于特定情况，可以使用ResponseData返回结果，主要用于自定义错误信息。

- list 列表 **方法：** GET **入参要求：** 要求使用QueryParam的dto传参，可以大幅减少代码。**出参要求：** 使用DataList的方式返回。
- load 加载单条记录 **方法：** GET **入参要求：** 使用主键ID传参 **出参要求：** 直接返回Entity，可以返回null。
- save 保存记录 **方法：** POST **入参要求：** 要求使用Entity传参 **出参要求：** 要求使用Entity返回，用于前端回显。
- update 修改记录 **方法：** PUT **入参要求：** 要求使用Entity传参 **出参要求：** 要求使用Entity返回，用于前端回显。
- delete 删除记录 **方法：** DELETE **入参要求：** 使用主键ID传参 **出参要求：** 要求返回受影响行数，一般为0/1。