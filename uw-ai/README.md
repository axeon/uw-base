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
  - [4.4 工具元数据管理](#44-工具元数据管理)
- [5. API使用指南](#5-api使用指南)
  - [5.1 对话生成](#51-对话生成)
  - [5.2 流式对话](#52-流式对话)
  - [5.3 结构化输出](#53-结构化输出)
  - [5.4 工具定义与执行](#54-工具定义与执行)
  - [5.5 翻译调用](#55-翻译调用)
- [6. 核心类说明](#6-核心类说明)
- [7. 最佳实践](#7-最佳实践)
- [8. 常见问题](#8-常见问题)

---

## 1. 项目概述

### 1.1 简介

`uw-ai` 是 UW Base 的 AI 集成模块，提供与 AI 服务中心（uw-ai-center）的交互能力。该模块封装了对话生成、工具调用、翻译等功能，采用响应式编程模型支持流式输出，并提供工具元数据管理功能。

### 1.2 核心特性

| 特性 | 说明 |
|------|------|
| **对话生成** | 支持同步和流式对话生成，可携带工具信息和RAG知识库 |
| **AI工具扩展** | 通过实现 `AiTool` 接口定义自定义工具，供AI调用 |
| **翻译服务** | 支持列表和Map两种模式的批量翻译 |
| **结构化输出** | 支持将AI输出转换为指定类型的Java对象 |
| **工具元数据管理** | 提供工具元数据的查询和更新接口 |
| **JSON Schema生成** | 自动生成工具参数的JSON Schema |

---

## 2. 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.x | 基础框架 |
| Project Reactor | 3.x | 响应式编程（Flux流式输出） |
| Jackson | 2.x | JSON处理 |
| jsonschema-generator | 4.x | JSON Schema生成 |
| Swagger2Module | - | 支持Swagger注解生成Schema |

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

```yaml
uw:
  ai:
    # AI服务中心地址
    ai-center-host: http://uw-ai-center
```

---

## 4. 核心功能详解

### 4.1 AI对话生成

通过 `AiChatRpc` 接口提供对话生成能力：

- **同步生成**：`generate()` 方法返回完整响应
- **流式生成**：`chatGenerate()` 方法返回 `Flux<String>` 流式输出

对话参数 `AiChatGenerateParam` 支持：
- 用户输入（userPrompt）
- 系统提示（systemPrompt）
- 工具列表（toolList）
- 工具上下文（toolContext）
- RAG知识库ID列表（ragLibIds）
- 文件列表（fileList）

### 4.2 AI工具扩展

通过实现 `AiTool<P, R>` 接口定义AI可调用的工具：

```java
public interface AiTool<P extends AiToolParam, R> extends Function<P, R> {
    String toolName();      // 工具名称
    String toolDesc();      // 工具描述
    String toolVersion();   // 工具版本
    Class<?> getParamType(); // 参数类型
    P convertParam(String toolTip); // 参数转换
    R apply(P param);       // 工具执行
}
```

工具参数需继承 `AiToolParam`，包含认证信息（saasId, userId, userType, userInfo）。

### 4.3 AI翻译服务

通过 `AiTranslateRpc` 接口提供翻译能力：

- **列表翻译**：`translateList()` 翻译字符串列表
- **Map翻译**：`translateMap()` 翻译键值对Map

翻译参数 `AiTranslateBaseParam` 支持：
- 配置ID（configId）
- 系统提示（systemPrompt）
- 目标语言列表（langList）

### 4.4 工具元数据管理

通过 `AiToolRpc` 接口管理工具元数据：

- **查询工具列表**：`listToolMeta(appName)`
- **更新工具元数据**：`updateToolMeta(aiToolMeta)`

工具元数据 `AiToolMeta` 包含：
- 应用名、工具类、工具版本
- 工具名称、工具描述
- 工具输入/输出参数Schema

---

## 5. API使用指南

### 5.1 对话生成

```java
import uw.ai.AiClientHelper;
import uw.ai.vo.AiChatGenerateParam;
import uw.common.dto.ResponseData;

@Service
public class ChatService {
    
    public void chat() {
        // 构建对话参数
        AiChatGenerateParam param = AiChatGenerateParam.builder()
            .configId(1L)                    // AI配置ID
            .systemPrompt("你是一个 helpful assistant")
            .userPrompt("你好，请介绍一下自己")
            .build();
        
        // 绑定当前用户认证信息
        param.bindAuthInfo();
        
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
            .configId(1L)
            .userPrompt("请写一篇关于Spring Boot的文章")
            .build();
        param.bindAuthInfo();
        
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
}

// 使用结构化输出
AiChatGenerateParam param = AiChatGenerateParam.builder()
    .configId(1L)
    .userPrompt("北京今天天气怎么样？")
    .build();
param.bindAuthInfo();

ResponseData<WeatherInfo> response = AiClientHelper.generateEntity(param, WeatherInfo.class);
response.onSuccess(weather -> {
    System.out.println("城市: " + weather.getCity());
    System.out.println("天气: " + weather.getWeather());
});
```

### 5.4 工具定义与执行

```java
import uw.ai.tool.AiTool;
import uw.ai.tool.AiToolParam;
import uw.common.dto.ResponseData;
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

工具调用方式：

```java
// 在对话中携带工具信息
AiToolCallInfo toolInfo = new AiToolCallInfo("WeatherTool", false);

AiChatGenerateParam param = AiChatGenerateParam.builder()
    .configId(1L)
    .userPrompt("北京今天天气怎么样？")
    .toolList(Arrays.asList(toolInfo))
    .build();
param.bindAuthInfo();

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
            .textList(Arrays.asList("Hello", "World"))
            .langList(Arrays.asList("zh-CN", "ja"))
            .build();
        param.bindAuthInfo();
        
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

---

## 6. 核心类说明

| 类名 | 说明 |
|------|------|
| `AiClientHelper` | AI客户端辅助类，封装所有AI调用方法 |
| `AiChatRpc` | 对话生成RPC接口 |
| `AiToolRpc` | 工具元数据管理RPC接口 |
| `AiTranslateRpc` | 翻译服务RPC接口 |
| `AiChatGenerateParam` | 对话生成参数 |
| `AiTool<P, R>` | AI工具接口，需实现此接口定义工具 |
| `AiToolParam` | 工具参数基类，包含认证信息 |
| `AiToolMeta` | 工具元数据 |
| `AiTranslateBaseParam` | 翻译参数基类 |
| `AiTranslateResultData` | 翻译结果数据 |
| `BeanOutputConverter<T>` | 结构化输出转换器 |
| `AiToolSchemaGenerator` | JSON Schema生成工具 |
| `AiToolExecuteController` | 工具执行控制器（RPC调用） |

---

## 7. 最佳实践

1. **认证信息绑定**：调用AI服务前务必执行 `param.bindAuthInfo()` 绑定当前用户认证信息
2. **工具参数定义**：工具参数类必须继承 `AiToolParam`，使用 Swagger `@Schema` 注解描述参数
3. **流式输出**：长文本生成使用流式输出，提升用户体验并降低内存占用
4. **错误处理**：AI调用始终返回 `ResponseData`，使用 `onSuccess`/`onError` 处理结果
5. **配置管理**：不同场景使用不同的 `configId` 区分AI配置

---

## 8. 常见问题

### Q1: 如何接入自定义AI模型？

在 uw-ai-center 服务中配置对应的AI模型，本模块通过 `configId` 引用配置。

### Q2: 工具参数如何生成JSON Schema？

框架会自动扫描 `AiTool` 实现类，使用 `AiToolSchemaGenerator` 基于 Swagger 注解生成Schema。

### Q3: 流式输出如何在前端展示？

后端返回 `Flux<String>`，前端使用 SSE (Server-Sent Events) 接收流式数据。

### Q4: 工具执行失败如何处理？

工具执行返回 `ResponseData`，在 `apply()` 方法中捕获异常并返回错误信息。

### Q5: 如何更新工具元数据？

框架启动时会自动扫描并注册工具元数据，也可手动调用 `AiClientHelper.updateToolMeta()` 更新。

---

## 许可证

[Apache License 2.0](../LICENSE)
