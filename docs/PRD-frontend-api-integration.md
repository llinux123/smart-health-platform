# PRD: 前端 H5 对接后端真实接口

## Problem Statement

前端 H5 应用（`smart-health-frontend`）当前所有页面均依赖 Mock 静态数据（通过 `VITE_USE_MOCK=true` 控制）。Mock 数据在字段结构、命名规范、数据完整性上与后端真实 API 存在多处不一致，导致切换到真实接口后会出现页面渲染异常、字段缺失、功能不可用等问题。需要系统性地消除前后端数据契约差异，完成真实接口联调。

## Solution

1. **补齐后端 VO/DTO 缺失字段**，使后端响应满足前端页面渲染需求
2. **修正前端 API 层字段映射**，与后端 VO 字段名对齐
3. **补充缺失的后端 API 端点**（医生详情、订单取消/支付）
4. **移除 Mock 数据层**，将 `VITE_USE_MOCK` 默认设为 `false`，保留 Mock 代码作为开发降级方案
5. **端到端联调验证**，确保所有页面在真实数据下正常工作

## User Stories

1. As a **患者用户**, I want to see my real registration order details (including doctor name, department, date, shift) from the backend, so that I can confirm my appointment information accurately
2. As a **患者用户**, I want to see my real prescription details (including medicine spec, usage, price) from the backend, so that I can verify my medication information
3. As a **患者用户**, I want to see real AI consultation session history with citation sources from the backend, so that I can trust the AI's medical references
4. As a **患者用户**, I want to see real doctor information on the schedule list and doctor detail pages, so that I can make informed choices about which doctor to book
5. As a **患者用户**, I want to be able to cancel and pay for my registration orders through real API calls, so that my order status stays consistent with the backend
6. As a **运营人员**, I want to see real pending-audit prescriptions from the backend, so that I can review them accurately
7. As a **运营人员**, I want to create real schedules through the backend API, so that the schedule data persists correctly
8. As a **运营人员**, I want to issue real prescriptions and audit them through the backend API, so that the prescription workflow is complete
9. As a **developer**, I want the frontend API layer to have correct field mappings matching backend VOs, so that data renders correctly without manual transformation
10. As a **developer**, I want the environment variable `VITE_USE_MOCK` to default to `false` in development, so that I test against real APIs during development
11. As a **developer**, I want to keep the mock data layer as an optional fallback, so that frontend development can continue without a running backend

## Implementation Decisions

### Decision 1: 后端 RegistrationOrder 扩展为 OrderVO

**问题**: 后端 `RegistrationOrder` 实体只有 `id, orderSn, patientId, scheduleId, sequenceNumber, amount, status, payTime, createTime`，缺少前端订单列表/详情页需要的展示字段（科室名、医生名、出诊日期、班次、挂号费）。

**方案**: 新建 `OrderVO`（或在 `RegistrationOrder` 中增加关联查询），通过 JOIN `t_schedule` 和 `t_doctor` 表，在订单查询接口中返回以下完整字段：
- `orderSn`, `patientId`, `scheduleId`
- `deptName` (来自排班表)
- `doctorName` (来自医生表或排班关联)
- `workDate` (来自排班表)
- `shift`, `shiftName` (来自排班表)
- `fee` (即 `amount`，统一为 `fee` 或保持 `amount` 并前端适配)
- `status`, `createTime`, `payTime`

**影响接口**:
- `GET /api/v1/registration/order/detail`
- `GET /api/v1/registration/order/list`

### Decision 2: 后端 PrescriptionItemVO 补齐字段

**问题**: 后端 `PrescriptionItemVO` 只有 `medicineName, quantity, unit`，前端处方详情页需要展示 `spec`（规格）、`usage`（用法用量）、`price`（单价）。

**方案**: 在 `PrescriptionItemVO` 中增加以下字段：
- `spec` (String) — 药品规格，如 "15g/支"
- `usage` (String) — 用法用量，如 "外用，每日2次"
- `price` (BigDecimal) — 单价

对应数据库 `t_prescription_item` 表需增加 `spec`, `usage`, `price` 字段（如果尚未存在）。

**影响接口**:
- `GET /api/v1/prescriptions`
- `GET /api/v1/prescriptions/{id}`
- `GET /api/v1/prescriptions/pending-audit`
- `POST /api/v1/prescription/issue` (请求体也需接受这些字段)
- `POST /api/v1/prescriptions/{id}/audit`

### Decision 3: 后端 SessionHistoryVO 增加 citations 字段

**问题**: 后端 `SessionHistoryVO` 只有 `role, content, timestamp`，但前端 SSE 流式响应 `ConsultStreamResponse` 包含 `citations`（引用来源列表），而历史消息回放时前端也需要展示引用来源。

**方案**: 在 `SessionHistoryVO` 中增加：
- `citations` (List<Citation>) — 引用来源列表，复用 `ConsultStreamResponse.Citation` 内部类结构

**影响接口**:
- `GET /api/v1/ai/sessions/{sessionSn}/history`

### Decision 4: 前端字段名对齐

**问题**: 前端 Mock 数据使用的字段名与后端 VO 不一致。

**对齐清单**:

| 前端当前字段 | 后端字段 | 统一方案 |
|---|---|---|
| `fee` | `amount` | 后端 OrderVO 使用 `fee` 别名，或前端改用 `amount` |
| `SessionHistoryVO.createTime` | `timestamp` | 前端改用 `timestamp` |
| `ScheduleVO` 缺少医生姓名 | 无 | 前端保留 `doctorInfoMap` 或后端增加 `doctorName` 到 ScheduleVO |

**推荐方案**:
- 后端 OrderVO 直接使用 `fee` 字段名（与前端一致，语义更清晰）
- 前端 `ChatPage.vue` 和 `SessionListPage.vue` 中适配 `timestamp` 字段
- 后端 ScheduleVO 增加 `doctorName` 字段（通过关联查询），前端可移除 `doctorInfoMap`

### Decision 5: 补充缺失的后端 API 端点

**5a. 医生详情接口**

前端 `DoctorDetailPage.vue` 需要展示医生详细信息（姓名、职称、擅长、简介），但当前后端无此接口。

新增接口：
- `GET /api/v1/doctor/{id}` — 返回医生详情（name, title, avatar, specialty, intro）

可在 `smart-health-registration` 模块中新增 `DoctorController`，或复用已有的医生数据源。

**5b. 订单取消接口**

前端 `OrderDetailPage.vue` 有"取消订单"功能，但后端无对应接口。

新增接口：
- `POST /api/v1/registration/order/cancel` — 参数 `{orderSn}`，将订单状态改为"已退号"(4)

**5c. 订单支付接口**

前端 `OrderDetailPage.vue` 有"模拟支付"功能，后端需对接支付状态更新。

新增接口：
- `POST /api/v1/registration/order/pay` — 参数 `{orderSn}`，将订单状态改为"已支付"(2)，记录 `payTime`

### Decision 6: 前端 API 层清理

**方案**:
1. 所有 `src/api/*.js` 文件中移除 `if (isMockEnabled())` 分支
2. 保留 `src/mock/` 目录和文件，但不再被 API 层引用
3. `.env.development` 中 `VITE_USE_MOCK=false`
4. `useSSE.js` composable 中移除 mock 分支逻辑

### Decision 7: 前端页面适配

需要修改的页面组件：

1. **ChatPage.vue** — 历史消息中 `createTime` 改为 `timestamp`
2. **SessionListPage.vue** — 同上
3. **OrderDetailPage.vue** — 对接真实取消/支付接口（替换 Mock 按钮逻辑）
4. **DoctorDetailPage.vue** — 从真实 API 获取医生信息（替换 `doctorInfoMap`）
5. **ScheduleListPage.vue** — 如果后端 ScheduleVO 增加了 `doctorName`，移除前端映射
6. **PrescriptionDetailPage.vue** — 确认 `spec`, `usage`, `price` 字段正常渲染

## Testing Decisions

### 测试策略

- **后端单元测试**: 对新增/修改的 VO 映射、新增接口（医生详情、订单取消/支付）编写单元测试，确保 SQL 关联查询正确
- **前端构建验证**: 每次修改后运行 `vite build` 确保无编译错误
- **端到端手动验证**: 启动后端服务 + 前端 dev server，逐页面验证数据渲染

### 测试优先级

1. **P0**: 登录/注册 → 获取真实 JWT → 验证 token 存储和刷新
2. **P0**: 排班列表 → 验证科室/日期筛选参数传递
3. **P0**: 秒杀抢号 → 验证并发扣减和订单创建
4. **P1**: 订单列表/详情 → 验证关联字段（科室、医生、日期）
5. **P1**: 处方列表/详情 → 验证药品明细字段完整性
6. **P1**: SSE 流式对话 → 验证真实 AI 流式响应解析
7. **P2**: 管理后台（排班创建、处方审核、处方开具）→ 验证写操作

### 已有测试先例

- 后端：各模块已有单元测试（51 个测试全部通过），可作为回归基准
- 前端：`vite build` 编译通过（28 modules），作为构建回归基准

## Out of Scope

- **支付网关集成**: 订单支付接口仅做状态更新，不对接真实支付渠道（微信/支付宝）
- **文件上传存储**: 多模态图片上传继续使用本地存储或已有的 MinIO/OSS 配置
- **权限控制细化**: 管理后台页面暂不增加角色权限校验，后续统一处理
- **Mock 数据层删除**: 保留 Mock 代码作为离线开发降级方案，仅移除 API 层引用
- **性能优化**: 不在本次范围内，后续根据真实数据量级做分页、缓存等优化

## Further Notes

### 数据契约差异汇总表

| 模块 | 前端期望字段 | 后端实际字段 | 差异类型 |
|---|---|---|---|
| 订单 | `fee, doctorName, deptName, workDate, shift, shiftName` | `amount` (无关联字段) | 字段缺失 + 命名不一致 |
| 处方药品明细 | `spec, usage, price` | 无 | 字段缺失 |
| 会话历史 | `citations, createTime` | `timestamp` (无 citations) | 字段缺失 + 命名不一致 |
| 排班 | `doctorName` (前端用 map 补充) | 无 | 字段缺失 |

### 建议实施顺序

1. 后端：补齐 VO 字段 + 新增接口
2. 后端：运行已有 51 个单元测试确保无回归
3. 前端：API 层字段对齐 + 移除 Mock 分支
4. 前端：页面组件适配
5. 联调：端到端验证所有页面
