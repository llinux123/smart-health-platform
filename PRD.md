# 智慧医疗与大健康管理平台 — PRD

## Problem Statement

我需要一个能端到端演示的智慧医疗平台，用于技术面试、竞赛或毕设展示。该平台需要覆盖互联网医院核心业务（挂号、问诊、处方）和 AI 智能化能力（多模态分析、RAG 问诊、流式对话），但目前只有架构设计文档，没有任何可运行的代码。

核心挑战：在有限时间内，构建一条完整的核心链路（患者上传图片 → AI 问诊 → 挂号 → 处方 → 库存扣减），同时保持架构的正确性，使后续可以方便地扩展为完整的微服务系统。

## Solution

采用 Maven 多模块单体架构，按业务域分包（user/registration/consultation/prescription），所有模块在同一个 JVM 中运行。核心链路使用真实中间件（MySQL + Redis + RabbitMQ + Elasticsearch），AI 层使用 Spring AI 对接 OpenAI Compatible API，实现 RAG 检索增强生成和 SSE 流式对话。前端采用模板/低代码方案快速对接。

架构设计保留微服务边界：模块间通过接口层通信，配置文件外部化，表结构按生产标准设计。后续拆微服务时业务代码零改动。

## User Stories

### 基础设施与用户认证
1. As a **系统管理员**, I want **Docker Compose 一键启动所有中间件（MySQL/Redis/RabbitMQ/ES）**, so that **开发环境搭建不超过 5 分钟**
2. As a **患者**, I want **通过手机号注册账号**, so that **我可以使用平台服务**
3. As a **患者**, I want **通过账号密码登录并获取 JWT Token**, so that **后续请求能通过身份认证**
4. As a **系统**, I want **所有 API 请求携带 JWT Token 进行校验**, so that **未认证用户无法访问受保护接口**
5. As a **开发者**, I want **统一的 API 响应格式（code/message/data）**, so that **前端和调用方可以统一处理响应**
6. As a **系统**, I want **全局异常处理，返回结构化错误信息**, so that **调用方可以明确知道错误原因**
7. As a **开发者**, I want **Swagger/Knife4j 接口文档自动生成**, so that **可以通过 UI 调试所有 API**

### 挂号预约（高并发秒杀）
8. As a **运营人员**, I want **管理医生排班计划（日期、班次、号源量、挂号费）**, so that **患者可以看到可预约的号源**
9. As a **患者**, I want **查看可预约的医生排班列表**, so that **我可以选择合适的时间挂号**
10. As a **患者**, I want **参与专家号秒杀抢号**, so that **我能在号源紧张时抢到专家号**
11. As a **系统**, I want **使用 Redis 预扣库存 + Redisson 分布式锁防止超卖**, so that **高并发下号源不会被多卖**
12. As a **系统**, I want **抢号成功后通过 RabbitMQ 异步生成挂号订单**, so that **主流程不被数据库写入阻塞，提升吞吐量**
13. As a **患者**, I want **查看我的挂号订单状态（排队中/待支付/已支付/已就诊）**, so that **我知道就诊进度**
14. As a **系统**, I want **挂号订单号全局唯一（REG_日期_序号）**, so that **订单可追溯**

### AI 智能问诊
15. As a **患者**, I want **上传患处照片或体检报告截图**, so that **AI 能帮我初步分析症状**
16. As a **系统**, I want **调用多模态大模型识别图片并生成症状自查草稿**, so that **患者获得结构化的初步分析结果**
17. As a **患者**, I want **基于症状草稿与 AI 进行多轮追问对话**, so that **我能深入了解自己的病情**
18. As a **系统**, I want **通过 RAG 从 Elasticsearch 医学知识库中检索相关内容**, so that **AI 回答基于真实医学文献，减少幻觉**
19. As a **系统**, I want **AI 回答以 SSE 流式方式输出（打字机效果）**, so that **用户无需等待完整回答，体验流畅**
20. As a **系统**, I want **AI 回答附带引用来源（指南、说明书等）**, so that **回答可溯源、可信**
21. As a **系统**, I want **多轮对话上下文保存在会话中**, so that **AI 能理解上下文连贯追问**

### 处方与药品
22. As a **医生**, I want **查看患者的问诊记录和 AI 生成的病情草稿**, so that **我能做出诊断决策**
23. As a **医生**, I want **为患者开具电子处方（含诊断结论和药品列表）**, so that **患者可以凭处方取药**
24. As a **系统**, I want **开处方时扣减对应院区药房库存**, so that **库存数据实时准确**
25. As a **系统**, I want **处方生成全国唯一编码和 PDF 存根**, so that **处方可追溯、可存档**
26. As a **药师**, I want **审核处方（通过/驳回）**, so that **合理用药得到保障**
27. As a **患者**, I want **查看我的处方列表和详情**, so that **我能了解自己的用药情况**

### 前端展示
28. As a **患者**, I want **通过 H5 页面完成注册、登录、上传、问诊、挂号、查看处方的完整流程**, so that **面试/答辩时能端到端演示**
29. As a **演示者**, I want **前端页面简洁美观、流程清晰**, so that **给面试官留下好印象**

## Implementation Decisions

### 技术栈
- JDK 17 + Spring Boot 3.2 + Spring Cloud 2023.x
- Spring AI（对接 OpenAI Compatible API，支持 SSE 流式）
- MyBatis + MyBatis Generator（ORM 层）
- SpringDoc + Knife4j（API 文档）
- SpringSecurity + JWT（认证授权）
- Redisson（分布式锁）
- RabbitMQ（异步消息）
- Elasticsearch（RAG 向量/文本检索）

### 项目结构
Maven 多模块，单 JVM 运行：
- `smart-health-common`：通用工具、统一响应体、全局异常处理、常量定义
- `smart-health-user`：用户注册/登录、JWT 签发、SpringSecurity 配置
- `smart-health-registration`：排班管理、秒杀抢号、Redis 预扣、MQ 异步出单
- `smart-health-consultation`：多模态图片分析、RAG 问诊、SSE 流式对话、会话管理
- `smart-health-prescription`：处方开具、库存扣减、处方审核、PDF 生成
- `smart-health-app`：Spring Boot 启动模块，聚合所有业务模块

### 数据库设计
MySQL 核心表（按设计文档）：
- `t_patient`：患者用户表
- `t_doctor_schedule`：医生排班号源表（含乐观锁 version 字段）
- `t_registration_order`：挂号订单表
- `t_prescription`：电子处方表
- `t_pharmacy_inventory`：药房库存表（多院区通过 pharmacy_id 区分）

Elasticsearch 索引：
- `idx_medical_knowledge`：医学知识库，支持 ik 分词 + dense_vector 向量检索（1536 维，cosine 相似度）

### API 设计
RESTful 风格，统一响应格式 `{code, message, data}`，JWT Bearer Token 认证。核心接口：
- POST `/api/v1/ai/multimodal/analyze` — 多模态图片分析（Multipart）
- POST `/api/v1/ai/consult/stream` — RAG 流式问诊（SSE）
- POST `/api/v1/registration/seckill` — 秒杀抢号
- POST `/api/v1/prescription/issue` — 开具处方

### 关键架构决策
- 单体运行但按业务域严格分包，模块间通过 Java interface 通信，为后续拆微服务预留边界
- 挂号秒杀采用 Redis 预扣库存 + Redisson 分布式锁 + RabbitMQ 异步写库，而非数据库直接扣减
- AI 层通过 Spring AI 抽象，切换大模型只需修改配置，不改代码
- RAG 检索使用 Elasticsearch 双路检索（文本 + 向量），不引入 Neo4j（后续可扩展）
- 会话日志暂存 MySQL text 字段（JSON 格式），不引入 MongoDB（后续可扩展）
- 分布式事务（Seata）不在第一阶段，使用本地事务 + 注释标记扩展点

### 前端方案
采用 Vue + Element 管理模板/低代码方案，对接核心链路 API，实现患者端 H5 页面。

### Docker Compose 组件
- MySQL 8.x
- Redis 7.x
- RabbitMQ 3.x（含 management 插件）
- Elasticsearch 8.x

## Testing Decisions

### 测试策略
- 以 REST API 集成测试为主（使用 MockMvc 或 TestRestClient），验证端到端行为
- 单元测试覆盖核心业务逻辑（库存扣减、分布式锁、RAG 检索编排）
- 不测试框架本身的行为（Spring Security 过滤器链、MyBatis 映射等）

### 测试重点
- 挂号秒杀：并发场景下库存不超卖（模拟多线程抢号）
- AI 问诊：SSE 流式响应格式正确、RAG 检索返回相关文档
- 处方开具：库存扣减与处方创建的本地事务一致性
- 用户认证：Token 签发、过期、无效 Token 拒绝

### 测试 seam
主要 seam 在 REST Controller 层——每个业务模块的 Controller 是测试入口，覆盖从 HTTP 请求到数据库/MQ/缓存的完整链路。

## Out of Scope

以下内容明确不在第一阶段范围内，后续可根据时间余力扩展：

- **微服务拆分**：不部署 Nacos 注册中心、不使用 Feign 远程调用、不配置 Gateway 路由
- **Seata 分布式事务**：处方扣减使用本地事务，不引入 AT 模式
- **Sentinel 限流降级**：不集成流量防护
- **Neo4j 知识图谱**：RAG 仅使用 ES 检索，不做图谱验证
- **MongoDB**：会话日志用 MySQL 替代
- **ELK + AIOps**：日志使用 Logback 文件输出，不搭建日志链路
- **MinIO/OSS 对象存储**：图片暂存本地文件系统，后续可切换为 OSS
- **多端前端**：只做患者端，医生端和运营端不做
- **医保/支付对接**：支付流程 mock，不接入真实支付渠道
- **WebSocket 实时沟通**：问诊使用 SSE 流式，不做实时音视频
- **多院区真实部署**：单库通过 pharmacy_id 字段区分院区

## Further Notes

### 构建顺序
1. **地基**：Docker Compose + 项目骨架 + 建表 + 统一响应/异常 → 验收：中间件启动、项目运行
2. **用户认证**：注册/登录 + JWT + Security → 验收：Swagger 可调通
3. **挂号秒杀**：排班 + Redis 预扣 + Redisson + RabbitMQ → 验收：能抢号、库存正确
4. **AI 问诊**：多模态 + RAG + SSE → 验收：能上传图片、流式追问
5. **处方药品**：开处方 + 库存扣减 → 验收：处方生成、库存变化
6. **前端对接**：模板对接 + 核心链路 → 验收：完整演示

### 扩展路径
每个"Out of Scope"项都有明确的扩展入口：模块间接口已定义、配置文件已外部化、表结构按生产标准设计。后续扩展时业务代码改动最小化。
