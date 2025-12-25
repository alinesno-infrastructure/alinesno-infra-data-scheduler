# 工作流解析包

从上百个异构文件（PDF/Word/Excel/PPT/图片等）中精准提取结构化指标，
目标是：高精度、可扩展、模块解耦、便于后续集成 LLM 与文档解析能力

## 文档解析架构

 <img src="/images/llm-exe-mermaid.png" />

```properties
+------------------------------------------------------------------+
|                        [1. API 层]                               |
|  - REST Controller (Spring MVC)                                  |
|  - 接收指标定义（IndicatorDefinition）                           |
|  - 返回结构化结果（ExtractionResult）                            |
+-----------------------------+------------------------------------+
|
v
+------------------------------------------------------------------+
|                    [2. 服务编排层 (Service)]                     |
|  MetricExtractionService                                         |
|    ├─ 调用 MetadataIndex 进行关键词初筛                         |
|    ├─ 按优先级调度多个 MetricExtractor                          |
|    │   ├─ RuleBasedMetricExtractor（优先）                      |
|    │   └─ LlmStructuredMetricExtractor（兜底）                  |
|    └─ 合并结果 + 冲突检测                                        |
+-----------------------------+------------------------------------+
|
v
+------------------------------------------------------------------+
|                   [3. 抽取引擎层 (Engine)]                       |
|  ┌──────────────────────┐     ┌──────────────────────────────┐  |
|  │ RuleBasedExtractor   │     │ LLM Structured Extractor     │  |
|  │ - 正则匹配数值       │<--->│ - 强约束 Schema Prompt      │  |
|  │ - 单位/范围校验      │     │ - JSON Mode 输出解析        │  |
|  │ - 高置信度快速返回   │     │ - 调用 DeepSeek/GPT-4o API  │  |
|  └──────────────────────┘     └──────────────────────────────┘  |
+-----------------------------+------------------------------------+
|
v
+------------------------------------------------------------------+
|                   [4. 文档处理层 (Document)]                     |
|  DocumentParser (接口)                                           |
|    ├─ PdfDocumentParser (PDFBox/Tika)                            |
|    ├─ WordDocumentParser (Apache POI)                            |
|    ├─ ExcelDocumentParser (Apache POI) → 提取表格为结构化数据    |
|    ├─ PptDocumentParser (Apache POI)                             |
|    └─ ImageOcrDocumentParser (Tesseract/PaddleOCR)               |
|                                                                  |
|  输出统一中间表示：DocumentChunk（含 content + 元数据）          |
+-----------------------------+------------------------------------+
|
v
+------------------------------------------------------------------+
|                [5. 存储与索引层 (Storage)]                       |
|  ┌──────────────────────┐     ┌──────────────────────────────┐  |
|  │ DocumentChunkRepo    │     │ MetadataIndex                │  |
|  │ - 本地文件 / DB 存储 │     │ - Lucene / Elasticsearch     │  |
|  │ - 按文件名查询       │     │ - 支持字段过滤：             │  |
|  └──────────────────────┘     │   • keywords                 │  |
|                                │   • fileType                 │  |
|                                │   • sectionPath              │  |
|                                │   • sourceFile               │  |
|                                └──────────────────────────────┘  |
+-----------------------------+------------------------------------+
|
v
+------------------------------------------------------------------+
|                 [6. 基础设施层 (Infra)]                          |
|  - OCR Engine (Tesseract Java)                                   |
|  - HTTP Client (OkHttp / WebClient) → 调用 LLM API               |
|  - Configuration (YAML 加载 IndicatorDefinition)                 |
|  - Validation Utils (单位/数值校验)                              |
+------------------------------------------------------------------+
```

## 核心Java接口设计

```java
com.yourcompany.metrics.extractor
├── api                // 对外 REST API
├── service            // 服务编排
├── engine             // 抽取引擎
├── document           // 文档解析
├── storage            // 存储与索引
├── model              // 领域模型
└── infra              // 基础设施（OCR/LLM客户端等）
```