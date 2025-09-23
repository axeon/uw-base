[TOC]

# 项目说明

uw-common 通用组件。

## maven引用

```xml
<dependency>
    <groupId>com.umtone</groupId>
    <artifactId>uw-common</artifactId>
    <version>${uw-common.version}</version>
</dependency>
```

# 关键类解析
## ResponseData 响应数据封装类
* ResponseData是一个基于泛型的响应数据对象，封装了响应数据，以及响应状态码。
* 在整个项目中，所有响应数据都使用ResponseData进行封装，包括返回给前端的数据，以及返回给后端的数据。
* 使用ResponseData的好处是，可以统一处理响应数据，尤其在返回数据的同时，也返回了响应状态码和消息，使得程序可以更好的处理响应数据。这样也避免了不必要的异常处理和异常捕获代码，可能获取更简单和更优雅的代码。
* 同时，ResponseData和i18n的结合，使得处理国际化的工作更加简单和方便。
* ResponseCode定义了响应状态码和消息的统一接口，通过继承关系，可以定义各种状态码和消息，使得代码更加清晰和可读。
* ResponseData通过传入ResponseCode，简化调用代码，同时更简单的支持多语言。
* ResponseData支持基于onSuccess,onError,onFatal等方法实现函数链式调用，简化代码。

### ResponseData 封装结构
* time 时间戳
* state 响应状态
* code 响应状态码
* msg 响应消息
* data 响应数据
* type 响应数据类型(请注意，不能正确处理嵌套泛型，只能返回集合类型)

### ResponseData 响应状态
* SUCCESS系列：成功响应。
* WARN系列: 警告响应。有警告，但是仍然有数据返回。
* ERROR系列：失败响应。有错误，一般没有数据返回。
* FATAL系列: 严重失败响应。严重错误，不会有数据返回。

### ResponseData 构造方法
ResponseCode主要基于static静态方法进行封装。
* 构造方法基于响应码状态,如successXXX(),warnXXX(),errorXXX(),fatalXXX()等。
* 系列方法包括xxx(),xxxCode(),xxxMsg()。

### ResponseData 状态判断
* isSuccess(): 判断是否为成功响应。
* isNotSuccess(): 判断是否为失败响应。
* isWarn(): 判断是否为警告响应。
* isError(): 判断是否为错误响应。
* isFatal(): 判断是否为严重失败响应。
* isNotError(): 判断是否既非错误状态也非严重错误状态。

### ResponseData 优化
对于高并发的后台系统，可提前创建好ResponseCode的实例，减少创建开销。
系统提供了静态实例，可直接使用，包括：SUCCESS, WARN, ERROR, FATAL。
另外，系统也提供了构造器，可用于程序中手动创建ResponseCode实例。
！！！请注意！以上的封装方法都已去除time返回！！！

### ResponseData i18n 推荐提示词
在idea的通义灵码中，选中ResponseCode定义文件，输入如下提示词。
``` prompt
!!!无需对当前文件做任何修改!!!
请将当前文件内容转换为java标准的i18n规范资源文件。
具体要求如下，请严格遵照！！！
1.请将Enum名称转换为全小写，"_"转换为"."，请不要带codePrefix前缀。
2.请将Enum的value作为需要翻译的内容。
3.要求翻译全部语种！语种列表：中文简体，中文繁体，英语，日语，德语，法语，韩语，意大利语，俄语，西班牙语，葡萄牙语，阿拉伯语。
4.翻译的内容用于用户界面展示的信息，请使用官方正式用语风格，严禁使用口语化的翻译风格。
5.内容中使用String.format的转义符号请原样保留。
6.默认资源文件语种为中文简体，同时也应该保持中文简体语言文件存在。
7.资源文件请不要保存在根目录下，我要求的保存目录为：{当前文件所在项目路径}/{当前文件中ResourceBundleMessageSource定义的资源目录}。
```


### ResponseData 特别用法
* row() 返回未泛型化的原始响应对象实例。特别适合返回错误和警告响应。
* toString() 直接返回json序列化字符串。

## BitConfigUtils 位运算配置工具
BitConfigUtils是一个非常有趣的开关配置类，通过位运算可以在很小空间保存很多开关。
BitConfigUtils，可以在一个int中存储32个开关，一个long中存储64个开关。

## DateUtils 日期工具类
DateUtils是一个非常简单，但是又非常灵活的日期工具类。

## ByteArrayUtils 字节数组工具类
ByteArrayUtils是非常有用的字节数组工具类，可以方便的进行字节数组的转换，以及字节数组的拼接。
因为极少有人有操作字节数组的需求，所以ByteArrayUtils就显得很特别和稀有。
ByteArrayUtils非常适合做底层协议的开发。

## IpUtils IP工具类
IpUtils是一个非常简单的IP工具类，主要做ip和long之间的转换。

## JsonUtils JSON工具类
JsonUtils是一个非常简单的JSON工具类，主要做json和java对象之间的转换。
JsonUtils主要是避免了不必要的异常捕获，优化代码调用。

## NumCodeUtils 数字编码工具类
NumCodeUtils是一个非常简单的数字编码工具类，主要用于数字混淆。

## RSAUtils RSA工具类
RSAUtils是一个非常简单的RSA工具类，主要用于RSA加密和解密。

## AESUtils AES工具类
AESUtils是一个非常简单的AES工具类，主要用于AES加密和解密。

## DigestUtils 签名工具类
DigestUtils是一个非常简单的摘要工具类，主要用于签名验证。

## EnumUtils 枚举工具类
EnumUtils是一个非常简单的枚举工具类，主要用于枚举的转换。

