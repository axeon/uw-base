# uw-ai

## 目录

- [1. 项目概述](#1-项目概述)
    - [1.1 简介](#11-简介)
    - [1.2 核心特性](#12-核心特性)
- [2. 技术栈](#2-技术栈)
- [3. 安装与配置](#3-安装与配置)
    - [3.1 Maven依赖](#31-maven依赖)
    - [3.2 基础配置](#32-基础配置)
- [4. 核心功能详解](#4-核心功能详解)
    - [4.1 AI对话生成](#41-ai对话生成)
    - [4.2 AI工具扩展](#42-ai工具扩展)
    - [4.3 AI翻译服务](#43-ai翻译服务)
    - [4.4 AI图片生成](#44-ai图片生成)
    - [4.5 模型配置查询](#45-模型配置查询)
    - [4.6 工具元数据管理](#46-工具元数据管理)
- [5. API使用指南](#5-api使用指南)
    - [5.1 对话生成](#51-对话生成)
    - [5.2 流式对话](#52-流式对话)
    - [5.3 结构化输出](#53-结构化输出)
    - [5.4 工具定义与执行](#54-工具定义与执行)
    - [5.5 翻译调用](#55-翻译调用)
    - [5.6 图片生成](#56-图片生成)
    - [5.7 模型配置查询](#57-模型配置查询)
- [6. 核心类说明](#6-核心类说明)
- [7. 最佳实践](#7-最佳实践)
- [8. 常见问题](#8-常见问题)

---

## 1. 项目概述

### 1.1 简介

`uw-ai` 是 UW Base 的 AI 集成模块，提供与 AI 服务中心（uw-ai-center）的交互能力。该模块封装了对话生成、工具调用、翻译、图片生成、模型配置查询等功能，采用响应式编程模型支持流式输出，并提供工具元数据的自动注册与管理。

### 1.2 核心特性

| 特性                | 说明                                          |
|-------------------|---------------------------------------------|
| **对话生成**          | 支持同步和流式对话生成，可携带工具信息和 RAG 知识库                |
| **AI工具扩展**        | 通过实现 `AiTool` 接口定义自定义工具，供 AI 调用            |
| **翻译服务**          | 支持列表和 Map 两种模式的批量翻译                         |
| **结构化输出**         | 支持将 AI 输出转换为指定类型的 Java 对象                   |
| **图片生成**          | 基于提示词生成图片，支持会话保存                            |
| **模型配置查询**        | 按租户、API、ID、配置代码、模型类型等多种维度查询可用模型配置          |
| **工具元数据管理**       | 启动时自动扫描注册工具元数据，并提供查询和更新接口                   |
| **JSON Schema生成** | 基于 Swagger 注解自动生成工具参数的 JSON Schema          |

---

## 2. 技术栈

| 技术                   | 版本  | 用途                  |
|----------------------|-----|---------------------|
| Spring Boot          | 3.x | 基础框架                |
| Project Reactor      | 3.x | 响应式编程（Flux 流式输出）    |
| Jackson              | 2.x | JSON 处理             |
| jsonschema-generator | 4.x | JSON Schema 生成      |
| Swagger2Module       | -   | 支持 Swagger 注解生成 Schema |

---

## 3. 安装与配置

### 3.1 Maven依赖

```xml
<dependency>
    <groupId>com.umtone</groupId>
    <artifactId>uw-ai</artifactId>
    <version>${uw-ai.version}</version>
</dependency>
```

### 3.2 基础配置

引入依赖后由 `UwAiAutoConfiguration` 自动装配，默认配置如下：

```yaml
uw:
  ai:
    # AI服务中心地址（默认 http://uw-ai-center）
    ai-center-host: http://uw-ai-center
```

> 应用名通过 `project.name` 注入，用于启动时按应用维度注册/拉取工具元数据。

---

## 4. 核心功能详解

### 4.1 AI对话生成

通过 `AiChatRpc` 接口提供对话生成能力：

- **同步生成**：`generate()` 方法返回完整响应
- **流式生成**：`chatGenerate()` 方法返回 `Flux<String>` 流式输出

对话参数 `AiChatGenerateParam` 支持：

| 参数            | 说明                                          |
|---------------|---------------------------------------------|
| configId      | AI 配置 ID（与 configCode 二选一）                  |
| configCode    | AI 配置代码（与 configId 二选一）                     |
| userPrompt    | 用户输入                                        |
| systemPrompt  | 系统提示                                        |
| toolList      | 工具调用信息列表（`List<AiToolCallInfo>`）            |
| toolContext   | 工具上下文（`Map<String, Object>`）                |
| ragLibIds     | RAG 知识库 ID 列表                               |
| fileList      | 文件列表（`MultipartFile[]`）                     |

### 4.2 AI工具扩展

通过实现 `AiTool<P, R>` 接口定义 AI 可调用的工具：

```java
public interface AiTool<P extends AiToolParam, R> extends Function<P, R> {
    String toolName();       // 工具名称
    String toolDesc();       // 工具描述
    String toolVersion();    // 工具版本
    default Class<?> getParamType();              // 参数类型（默认由泛型反射获取）
    default P convertParam(String toolTip);       // 参数转换（默认 JSON 反序列化）
    R apply(P param);         // 工具执行（来自 Function）
}
```

工具参数需继承 `AiToolParam`，内置认证信息（saasId, userId, userType, userInfo）。

> 应用启动时 `UwAiAutoConfiguration` 会自动扫描容器内所有 `AiTool` Bean，并按 `toolVersion` 与服务中心对比，存在新增或版本升级时自动同步工具元数据（含输入/输出 Schema）。

### 4.3 AI翻译服务

通过 `AiTranslateRpc` 接口提供翻译能力：

- **列表翻译**：`translateList()` 翻译字符串列表（`AiTranslateListParam`）
- **Map翻译**：`translateMap()` 翻译键值对 Map（`AiTranslateMapParam`，key 为变量名，value 为待翻译文本）

翻译参数 `AiTranslateBaseParam` 支持：

| 参数            | 说明                       |
|---------------|--------------------------|
| configId      | 配置 ID                    |
| configCode    | 配置代码（与 configId 二选一，列表/Map Builder 均已支持） |
| systemPrompt  | 系统提示                     |
| langList      | 目标语言列表                   |

### 4.4 AI图片生成

通过 `AiImageRpc` 接口提供图片生成能力，`AiClientHelper.generateImage()` 接收 `AiImageGenerateParam`，返回 `AiImageResultData`（含 sessionId 和图片 URL 列表）。

`AiImageGenerateParam` 主要参数：

| 参数            | 说明                                       |
|---------------|------------------------------------------|
| configId      | 配置 ID（与 configCode 二选一）                  |
| configCode    | 配置代码                                     |
| sessionId     | 会话 ID，大于 0 保存到指定会话，否则自动创建新会话            |
| userPrompt    | 图片提示词（必填）                                |

### 4.5 模型配置查询

通过 `AiConfigRpc` 接口查询 AI 服务中心的可用模型与 API 连接配置：

**模型配置（`AiModelConfigVo`）**：

- `listModelInfoBySaas(saasId, mchId)` — 按租户/商户查询
- `listModelInfoByApi(apiId)` — 按 API 配置 ID 查询
- `listModelInfoById(id)` — 按模型配置 ID 查询
- `listModelInfoByCode(configCode)` — 按配置代码查询
- `listModelInfoByType(modelType, modelTag)` — 按模型类型/能力标签查询

**API连接配置（`AiModelApiVo`）**：

- `listModelApiBySaas(saasId, mchId)` — 按租户/商户查询 API 列表
- `getModelApiById(id)` — 按 ID 查询 API 配置
- `getModelApiByCode(apiCode)` — 按配置代码查询 API 配置

> 模型类型包括 `CHAT / EMBEDDING / RERANK / TTS / OCR`。

### 4.6 工具元数据管理

通过 `AiToolRpc` 接口管理工具元数据：

- **查询工具列表**：`listToolMeta(appName)`
- **更新工具元数据**：`updateToolMeta(aiToolMeta)`

工具元数据 `AiToolMeta` 包含：应用名、工具类、工具版本、工具名称、工具描述、工具输入/输出参数 Schema。

---

## 5. API使用指南

所有调用均通过静态门面 `AiClientHelper` 暴露，统一返回 `ResponseData`。

### 5.1 对话生成

```java
import uw.ai.AiClientHelper;
import uw.ai.vo.AiChatGenerateParam;
import uw.common.response.ResponseData;

@Service
public class ChatService {

    public void chat() {
        // 构建对话参数，configId 与 configCode 二选一
        AiChatGenerateParam param = AiChatGenerateParam.builder()
            .configCode("default-chat")            // AI配置代码
            .systemPrompt("你是一个 helpful assistant")
            .userPrompt("你好，请介绍一下自己")
            .bindAuthInfo()                         // 绑定当前用户认证信息
            .build();

        // 调用AI生成
        ResponseData<String> response = AiClientHelper.generate(param);
        response.onSuccess(answer -> {
            System.out.println("AI回复: " + answer);
        }).onError(error -> {
            System.err.println("错误: " + error.getMsg());
        });
    }
}
```

### 5.2 流式对话

```java
import reactor.core.publisher.Flux;

@Service
public class StreamChatService {

    public void streamChat() {
        AiChatGenerateParam param = AiChatGenerateParam.builder()
            .configCode("default-chat")
            .userPrompt("请写一篇关于Spring Boot的文章")
            .bindAuthInfo()
            .build();

        // 流式输出
        Flux<String> stream = AiClientHelper.chatGenerate(param);
        StringBuilder fullResponse = new StringBuilder();

        stream.subscribe(
            chunk -> {
                System.out.print(chunk);        // 实时输出片段
                fullResponse.append(chunk);
            },
            error -> log.error("流式输出错误", error),
            () -> {
                System.out.println("\n[流式输出完成]");
                saveResponse(fullResponse.toString());
            }
        );
    }
}
```

### 5.3 结构化输出

```java
// 定义输出结构
public class WeatherInfo {
    private String city;
    private String weather;
    private int temperature;
    // getter/setter 略
}

// 使用结构化输出
AiChatGenerateParam param = AiChatGenerateParam.builder()
    .configCode("default-chat")
    .userPrompt("北京今天天气怎么样？")
    .bindAuthInfo()
    .build();

ResponseData<WeatherInfo> response = AiClientHelper.generateEntity(param, WeatherInfo.class);
response.onSuccess(weather -> {
    System.out.println("城市: " + weather.getCity());
    System.out.println("天气: " + weather.getWeather());
});
```

> `generateEntity()` 会自动在系统提示后追加目标类型的格式说明，并对 AI 返回的 JSON 进行清洗与反序列化。

### 5.4 工具定义与执行

```java
import uw.ai.tool.AiTool;
import uw.ai.tool.AiToolParam;
import uw.common.response.ResponseData;
import io.swagger.v3.oas.annotations.media.Schema;

@Component
public class WeatherTool implements AiTool<WeatherTool.Param, ResponseData<String>> {

    @Override
    public String toolName() {
        return "天气查询工具";
    }

    @Override
    public String toolDesc() {
        return "查询指定城市的当前天气";
    }

    @Override
    public String toolVersion() {
        return "1.0.0";
    }

    @Override
    public ResponseData<String> apply(Param param) {
        // 调用天气API获取数据
        String weather = weatherService.getWeather(param.getCity());
        return ResponseData.success(weather);
    }

    // 工具参数定义
    public static class Param extends AiToolParam {
        @Schema(description = "城市名称", requiredMode = Schema.RequiredMode.REQUIRED)
        private String city;

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
    }
}
```

在对话中携带工具信息：

```java
// toolCode 对应工具类名，returnDirect 表示是否直接将工具结果返回给用户
AiToolCallInfo toolInfo = new AiToolCallInfo("WeatherTool", false);

AiChatGenerateParam param = AiChatGenerateParam.builder()
    .configCode("default-chat")
    .userPrompt("北京今天天气怎么样？")
    .toolList(List.of(toolInfo))
    .bindAuthInfo()
    .build();

ResponseData<String> response = AiClientHelper.generate(param);
```

### 5.5 翻译调用

```java
import uw.ai.vo.AiTranslateListParam;
import uw.ai.vo.AiTranslateResultData;

@Service
public class TranslateService {

    public void translate() {
        // 列表翻译
        AiTranslateListParam param = AiTranslateListParam.builder()
            .configId(1L)
            .textList(List.of("Hello", "World"))
            .langList(List.of("zh-CN", "ja"))
            .bindAuthInfo()
            .build();

        ResponseData<AiTranslateResultData[]> response = AiClientHelper.translateList(param);
        response.onSuccess(results -> {
            for (AiTranslateResultData result : results) {
                System.out.println("语言: " + result.getLang());
                System.out.println("翻译结果: " + result.getResultMap());
            }
        });
    }
}
```

Map 翻译示例：

```java
import uw.ai.vo.AiTranslateMapParam;
import java.util.LinkedHashMap;

LinkedHashMap<String, String> textMap = new LinkedHashMap<>();
textMap.put("greeting", "Hello");
textMap.put("bye", "Goodbye");

AiTranslateMapParam param = AiTranslateMapParam.builder()
    .configId(1L)
    .textMap(textMap)
    .langList(List.of("zh-CN"))
    .bindAuthInfo()
    .build();

ResponseData<AiTranslateResultData[]> response = AiClientHelper.translateMap(param);
```

### 5.6 图片生成

```java
import uw.ai.vo.AiImageGenerateParam;
import uw.ai.vo.AiImageResultData;

AiImageGenerateParam param = AiImageGenerateParam.builder()
    .configCode("default-image")
    .prompt("一只在月光下奔跑的猫，写实风格")
    .bindAuthInfo()
    .build();

ResponseData<AiImageResultData> response = AiClientHelper.generateImage(param);
response.onSuccess(data -> {
    System.out.println("会话ID: " + data.getSessionId());
    data.getImageUrlList().forEach(url -> System.out.println("图片URL: " + url));
});
```

> `AiImageGenerateParam` 的 Builder 使用 `prompt(...)` 设置提示词，对应字段 `userPrompt`。

### 5.7 模型配置查询

```java
import uw.ai.vo.AiModelConfigVo;
import uw.ai.vo.AiModelApiVo;

// 按配置代码查询模型配置
ResponseData<AiModelConfigVo> cfg = AiClientHelper.listModelInfoByCode("default-chat");
cfg.onSuccess(c -> System.out.println("模型名: " + c.getModelName()));

// 按租户查询模型配置列表
ResponseData<List<AiModelConfigVo>> list =
    AiClientHelper.listModelInfoBySaas(saasId, 0L);

// 按模型类型/标签查询
ResponseData<List<AiModelConfigVo>> chatModels =
    AiClientHelper.listModelInfoByType("CHAT", "vision");

// 按配置代码查询API连接配置
ResponseData<AiModelApiVo> api = AiClientHelper.getModelApiByCode("openai-prod");
```

---

## 6. 核心类说明

### 门面与配置

| 类名                    | 说明                                       |
|-----------------------|------------------------------------------|
| `AiClientHelper`      | AI 客户端静态门面，封装所有 AI 调用方法                  |
| `UwAiAutoConfiguration` | 自动配置类，装配各 RPC Bean 并在启动时注册工具元数据          |
| `UwAiProperties`      | 配置属性类（`uw.ai.*`），含 `aiCenterHost`、`appName` |

### RPC 接口

| 类名                | 说明                  |
|-------------------|---------------------|
| `AiChatRpc`       | 对话生成 RPC 接口         |
| `AiToolRpc`       | 工具元数据管理 RPC 接口      |
| `AiTranslateRpc`  | 翻译服务 RPC 接口         |
| `AiImageRpc`      | 图片生成 RPC 接口         |
| `AiConfigRpc`     | 模型/API 配置查询 RPC 接口  |

### 参数与数据对象

| 类名                        | 说明                       |
|---------------------------|--------------------------|
| `AiChatGenerateParam`     | 对话生成参数                   |
| `AiImageGenerateParam`    | 图片生成参数                   |
| `AiTranslateBaseParam`    | 翻译参数基类                   |
| `AiTranslateListParam`    | 列表翻译参数                   |
| `AiTranslateMapParam`     | Map 翻译参数                 |
| `AiTranslateResultData`   | 翻译结果数据                   |
| `AiImageResultData`       | 图片生成结果数据                 |
| `AiToolCallInfo`          | 工具调用信息（toolCode + returnDirect） |
| `AiTool<P, R>`            | AI 工具接口，需实现此接口定义工具       |
| `AiToolParam`             | 工具参数基类，包含认证信息            |
| `AiToolMeta`              | 工具元数据                    |
| `AiModelConfigVo`         | 模型配置 VO                  |
| `AiModelApiVo`            | API 连接配置 VO              |

### 工具类

| 类名                        | 说明                 |
|---------------------------|--------------------|
| `BeanOutputConverter<T>`  | 结构化输出转换器           |
| `AiToolSchemaGenerator`   | JSON Schema 生成工具   |
| `AiToolExecuteController` | 工具执行控制器（接收 RPC 调用） |

---

## 7. 最佳实践

1. **认证信息绑定**：调用 AI 服务前务必执行 `bindAuthInfo()`（或在 Builder 链中 `.bindAuthInfo()`）绑定当前用户认证信息
2. **配置引用**：优先使用 `configCode`（语义化、环境无关）引用 AI 配置；`configId` 与 `configCode` 通常二选一
3. **工具参数定义**：工具参数类必须继承 `AiToolParam`，使用 Swagger `@Schema` 注解描述参数
4. **流式输出**：长文本生成使用流式输出，提升用户体验并降低内存占用
5. **错误处理**：AI 调用始终返回 `ResponseData`，使用 `onSuccess`/`onError` 处理结果
6. **工具版本管理**：升级工具逻辑时同步递增 `toolVersion()`，框架会自动把新元数据同步到服务中心

---

## 8. 常见问题

### Q1: 如何接入自定义 AI 模型？

在 uw-ai-center 服务中配置对应的 AI 模型与 API 连接，本模块通过 `configId` 或 `configCode` 引用配置；也可通过 `AiClientHelper.listModelInfoByXxx()` 查询可用配置。

### Q2: 工具参数如何生成 JSON Schema？

框架会自动扫描 `AiTool` 实现类，使用 `AiToolSchemaGenerator` 基于方法签名与 Swagger 注解生成输入/输出 Schema。

### Q3: 流式输出如何在前端展示？

后端返回 `Flux<String>`，前端使用 SSE (Server-Sent Events) 接收流式数据。

### Q4: 工具执行失败如何处理？

工具执行返回 `ResponseData`，在 `apply()` 方法中捕获异常并返回错误信息。

### Q5: 如何更新工具元数据？

应用启动时会自动扫描并注册/升级工具元数据（按 `toolVersion` 判断），也可手动调用 `AiClientHelper.updateToolMeta()` 更新。

### Q6: configId 和 configCode 应该用哪个？

二者用于定位同一份 AI 配置。推荐使用 `configCode`，便于跨环境迁移；`configId` 适合已明确知道主键的场景。

---

## 许可证

[Apache License 2.0](../LICENSE)
