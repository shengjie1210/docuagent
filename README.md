<div align="center">


# DocuAgent —— 基于 RAG 的智能文档问答助手

**上传 PDF / Word / Markdown / TXT，基于 RAG 实现流式对话问答，并展示引用来源**

<br>

![JDK](https://img.shields.io/badge/JDK-21-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-green?style=flat-square&logo=springboot)
![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0-blue?style=flat-square)
![MyBatis](https://img.shields.io/badge/MyBatis-3.0.4-red?style=flat-square&logo=mybatis)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=flat-square&logo=mysql)
![Vue](https://img.shields.io/badge/Vue-3.5-brightgreen?style=flat-square&logo=vuedotjs)
![Vite](https://img.shields.io/badge/Vite-8.3-purple?style=flat-square&logo=vite)
![Element Plus](https://img.shields.io/badge/Element%20Plus-2.14-blue?style=flat-square)
![License](https://img.shields.io/badge/License-Shengjie-lightgrey?style=flat-square)

</div>

>  **项目定位**：AI 应用开发（RAG / Agent 方向）的练手项目，注重工程规范、可观测性.
> 从需求文档、概要设计、详细设计到接口测试文档齐全，不是单点 Demo，而是一个能跑通完整闭环的前后端分离系统。

---

##  功能特性

### 用户体系

- 注册 / 登录（**JWT 鉴权** + **BCrypt** 密码加密，仅引 `spring-security-crypto`，不引入整个 Spring Security）
- JWT 拦截器统一鉴权，`UserContext` 线程上下文传递当前用户
- 统一响应体 `Result<T>` + 全局异常处理器 `GlobalExceptionHandler`

###  文档管理

- 单文件 / **批量上传**（PDF / DOCX / MD / TXT，单文件上限 20MB）
- 文档列表（分页、按文件名搜索）、查看详情、预览原文、删除
- AI 自动摘要、文档标签、切分块数统计、处理状态机（`processing / ready / failed`）
- **一键重建索引**（参数调整后可对单文档重新向量化）

### RAG 知识库

- **Apache Tika** 统一解析 PDF / DOCX（TXT / MD 直接 UTF-8 读取）
- 按段落语义切分（默认 **800 字 / 块**，超长段落硬切）
- 每块调用 **Embedding（text-embedding-v3）** 生成向量，元数据携带 `docId / userId / filename / chunkIndex`
- 相似度检索：**top-K（默认 4）+ 相关度阈值（默认 0.55）过滤 + 同文档 chunk 上限（默认 2）+ 用户数据隔离**
- 向量库本地持久化（`./data/vectors.json`，启动加载 / 关闭保存）

### 对话引擎

- **SSE 流式打字机效果**，回答完成后推送引用来源卡片（文件名 + 片段 + 相关度分值）
- **多轮记忆**：自动加载最近 10 条历史消息拼入上下文
- **自动生成对话标题**（≤12 字，首条消息触发）
- **抗幻觉 System Prompt**：6 条约束规则，禁止用通用企业常识补全答案；输出兜底正则清洗 Markdown 符号
- **Function Call**：AI 可主动调用「搜索我的文档 / 列出最近文档」工具（`@Tool` 注解声明）

### 管理后台（共享管理员密码解锁）

- **AI 服务商配置管理**：多服务商（通义 / OpenAI 兼容 / Ollama）CRUD、启用切换、连通性测试、额度查询，API Key 脱敏存储（`前6位******后4位`）
- **RAG 参数热更新**：chunkSize / topK / threshold / maxChunksPerDoc 后台实时调整，**无需重启**（内存 Bean 热改）
- **系统监控**：在线查看系统信息与实时日志 / 错误日志（ECharts 可视化）

---

## 技术栈

| 层          | 技术                                                         |
| ----------- | ------------------------------------------------------------ |
| **后端**    | JDK 21、Spring Boot 3.3.5、Spring AI 1.0.0（OpenAI 兼容协议）、MyBatis（XML 手写 SQL）、MySQL 8、HikariCP、JJWT 0.12、Knife4j 4.5、Lombok |
| **前端**    | Vue 3（Composition API）、Vite 8、Element Plus、Pinia、Vue Router、Axios、marked（Markdown 渲染）、ECharts 6、原生 fetch + ReadableStream 手写 SSE 解析 |
| **AI 能力** | 阿里云 MaaS（OpenAI 兼容模式）：Chat = `qwen-plus`、Embedding = `text-embedding-v3`；Apache Tika 文档解析；`SimpleVectorStore` |

---

## 系统架构

```
┌──────────────┐   HTTPS / SSE    ┌───────────────────┐   OpenAI 兼容协议   ┌─────────────────┐
│  Vue3 前端    │ ──────────────→ │  Spring Boot :8080 │ ────────────────→ │  大模型服务商     │
│  Element Plus│ ←────────────── │   Controller 层   │                    │  qwen-plus      │
│  手写 SSE 解析 │   打字机 token    │   Service 层       │                    │  embeddings     │
└──────────────┘                  │   RAG: Tika 切分   │                    └─────────────────┘
                                  │   VectorStore 内存 │
                                  └────────┬──────────┘
                                           │
                                  ┌────────▼─────────┐
                                  │       MySQL       │
                                  │ user / document / │
                                  │ conversation /   │
                                  │ message /         │
                                  │ ai_provider_config│
                                  └───────────────────┘
```

---

## 快速启动

### 1. 环境准备

| 依赖    | 版本要求 |
| ------- | -------- |
| JDK     | 21+      |
| Maven   | 3.9+     |
| Node.js | 18+      |
| MySQL   | 8.0+     |

### 2. 初始化数据库

```bash
# 直接执行项目自带的建库建表脚本
mysql -u root -p < Business_Assistant/sql/init.sql
```

脚本会自动创建 `docuagent` 库及全部 5 张表（`user / document / conversation / message / ai_provider_config`），并把首个注册用户（id=1）置为管理员。

### 3. 启动后端

```bash
cd Business_Assistant

# 修改 src/main/resources/application.yml：
#   - spring.datasource.password   数据库密码
#   - spring.ai.openai.api-key      大模型 API Key
# （也可改用环境变量：JWT_SECRET / ADMIN_PASSWORD）

mvn spring-boot:run
# 启动成功后默认端口 8080
# 接口文档（Knife4j）：http://localhost:8080/doc.html
```

### 4. 启动前端

```bash
cd docuagent-web
npm install
npm run dev
# 访问 http://localhost:5173
```

### 5. 首次使用

1. 注册账号 → 自动登录（首个用户即管理员）
2. 进入「文档管理」上传测试文档（`docs/` 目录下有现成的企业手册 / SOP / 定价文档可直接用）
3. 回到聊天页提问，观察打字机流式回答与末尾引用来源卡片
4. 侧边栏进入管理后台（默认共享密码 `123456`），可切换服务商 / 热调 RAG 参数 / 查看日志

---

## 关键配置说明（application.yml）

```yaml
docuagent:
  jwt-secret: ${JWT_SECRET:...}        # JWT 密钥，生产环境务必通过环境变量注入
  jwt-expire: 7200000                  # Token 有效期 2 小时
  admin-password: ${ADMIN_PASSWORD:123456}  # 管理后台解锁共享密码
  upload-dir: ./upload                 # 上传文件存储目录
  vector-store-path: ./data/vectors.json # 向量库持久化文件
  chunk-size: 800                      # 切分块大小（后台可热调）
  top-k: 4                             # 最终召回片段数
  similarity-threshold: 0.55           # 相关度阈值，低于此分数丢弃
  max-chunks-per-doc: 2                # 同一文档最多召回几块，防止刷屏
```

> ​    **设计要点**：AI 服务商配置优先从数据库 `ai_provider_config` 表读取（`@Primary` Bean），库中无数据时自动回退 yml，保证系统永远可用；日志会打印当前配置来源与脱敏后的 Key。

---

## 核心流程说明

### RAG 问答全流程

1. **入库**：文档上传 → Tika 抽纯文本 → 按段落切分（800 字/块，超长硬切）
2. **向量化**：每块包装为 Spring AI `Document`，携带元数据（`docId / userId / filename / chunkIndex`）→ `vectorStore.add()` 内部批量调 Embedding → 记录 `docId -> 向量ID列表` 映射
3. **检索**：用户提问 → 问题向量化 → `SearchRequest` 带阈值 top-K 召回 → **按 userId 过滤**做数据隔离 → 按相关度降序 → 同文档 chunk 去重限流（≤2 块）→ 截断到 topK
4. **生成**：片段按 score 排序拼入 System Prompt（标注来源文档名与相关度）→ 叠加最近 10 轮历史 → `.stream()` 流式调用大模型
5. **推送**：每个 token 通过 `SseEmitter` 以 `event: message` 逐字下发；流结束后以 `event: done` 推送完整答案 + 引用来源列表
6. **落库**：用户消息、AI 回答（含 sources JSON）一并写入 `message` 表

### 流式对话时序

```
前端 fetch (POST /api/chat/stream, Header 带 Bearer Token)
   │
   ├─→ 后端 SseEmitter 建立连接
   │      ├─ 存用户消息
   │      ├─ 首条消息 → 调 LLM 生成 ≤12 字对话标题
   │      ├─ ragService.retrieve() 检索片段
   │      ├─ 加载近 10 轮历史
   │      └─ chatClient.prompt().system(抗幻觉Prompt).tools(DocumentTools).stream()
   │
   ◄─ event: message  「token1」「token2」……（打字机效果）
   │
   ◄─ event: done     { answer, sources:[{filename, score, fragment}] }
   │
前端：ReadableStream 逐块读 → 按 \n 切帧解析 event:/data: → 渲染
```

### Function Call 工具

```java
@Tool(description = "根据关键词搜索当前用户的文档列表，返回文档ID和文件名")
public List<DocumentInfo> searchDocuments(@ToolParam(description = "搜索关键词") String keyword)

@Tool(description = "列出当前用户最近上传的文档")
public List<DocumentInfo> listRecentDocuments(@ToolParam(required = false) Integer limit)
```

大模型判断需要查库时自动回调这些方法，方法内通过 `UserContext.getUserId()` 取当前登录用户，天然隔离数据。

---

## 关键工程决策

| 决策                                    | 原因                                                         |
| --------------------------------------- | ------------------------------------------------------------ |
| **MyBatis XML 而非注解**                | SQL 集中在 `resources/mapper/*.xml`，便于 review 与慢查询优化，符合企业项目习惯 |
| **手写 SSE 解析而非 EventSource**       | 需要在 Header 带 `Authorization` Token，原生 EventSource 不支持自定义 Header，故用 `fetch + ReadableStream` 手动按帧解析 `event:/data:` |
| **仅引 spring-security-crypto**         | 只需要 BCrypt 哈希，引入整个 Spring Security 过滤器链是过度设计 |
| **抗幻觉 System Prompt + 输出兜底清洗** | prompt 约束模型"只用参考资料作答"，但模型偶尔不听，再用正则把 `**粗体** / - 列表 / \| 表格` 等 Markdown 符号剥掉，保证输出干净 |
| **删文档同步清向量**                    | 维护 `docId -> 向量ID列表` 映射，删除时按清单 `vectorStore.delete()`，防止残留片段污染后续检索 |
| **RAG 参数做成可热更新 Bean**           | `RagConfig` 字段非 final，管理员后台 PUT 接口直接改内存字段，调参不用重启；同时通过日志打印召回明细（score / 文件名 / chunk 号）便于观测质量 |
| **AI 配置落库 + 回退 yml**              | `DbAiModelConfig` 提供 `@Primary` Bean 优先读数据库供应商配置，库为空回退配置文件，避免"后台改了不生效"的坑 |

---

## 数据表设计

| 表                   | 说明                                                         |
| -------------------- | ------------------------------------------------------------ |
| `user`               | 用户账号（username / BCrypt 密码 / 是否管理员）              |
| `document`           | 文档元数据（文件名 / 类型 / 路径 / 大小 / AI 摘要 / chunk 数 / 处理状态） |
| `conversation`       | 会话（用户 / 自动生成的标题）                                |
| `message`            | 消息（角色 / 内容 / sources JSON）                           |
| `ai_provider_config` | AI 服务商配置（provider / base_url / api_key / 模型名 / 是否默认） |

---

## 生产环境优化方向（Demo 未实现,可改进）

| 现状（V1 Demo）            | 生产方案                              |
| -------------------------- | ------------------------------------- |
| SimpleVectorStore 内存存储 | PGVector / Milvus / Elasticsearch     |
| 上传时同步建索引，阻塞请求 | 异步化：消息队列 + Worker 后台建索引  |
| Embedding 逐块同步调用     | 批量调用 + 限流 + 失败重试            |
| 按段落 + 固定字数切分      | 按标题层级 / 语义切分（Markdown AST） |
| 单机文件持久化（./upload） | 分布式对象存储（OSS / MinIO）         |
| 共享密码解锁管理后台       | RBAC 细粒度权限体系                   |
| API Key 单用户配置         | 租户级密钥托管（KMS 加密）            |

---

## 📁 项目结构

```
docuagent/
├── Business_Assistant/                # 后端 Spring Boot
│   ├── sql/init.sql                   # 建库建表脚本
│   ├── docs/接口测试文档.md
│   └── src/main/java/com/example/business_assistant/
│       ├── BusinessAssistantApplication.java
│       ├── Controller/                # Auth / Chat / Document / Conversation / Provider / Admin*
│       ├── Service/                   # 业务接口 + Impl 实现
│       ├── mapper/                    # MyBatis 接口（XML 在 resources/mapper/）
│       ├── bean/                      # 数据库实体
│       ├── dto/                        # 传输对象 / VO / 请求体
│       ├── common/                     # Result 统一响应 / 全局异常 / JWT / UserContext
│       ├── Config/                    # Spring AI / DbAiModelConfig / RagConfig / 跨域 / 拦截器
│       └── tool/                      # DocumentTools（Function Call）
├── docuagent-web/                     # 前端 Vue3
│   └── src/
│       ├── api/index.js              # axios 封装 + fetchChatStream 手写 SSE
│       ├── views/                    # 登录 / 聊天 / 文档管理 / 管理后台（服务商/RAG/监控）
│       ├── components/chat/           # 消息列表 / 输入框
│       ├── stores/                   # Pinia（用户态）
│       └── router/                    # 路由守卫（登录校验 + 管理员解锁校验）
└── docs/                             # 需求规格 / 概要设计 / 详细设计 / 模块进度 + 测试语料
```

---

## 接口文档

后端启动后访问 Knife4j 自动生成的在线接口文档：

```
http://localhost:8080/doc.html
```

---

## License

**Shengjie**
