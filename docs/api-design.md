# 接口设计文档

## 通用约定

### 基础路径

所有接口以 `/api/v1` 为前缀。

### 认证方式

使用 Bearer Token（JWT），通过 `Authorization: Bearer <token>` 请求头传递。JWT 中嵌入角色信息（`ROLE_PATIENT`、`ROLE_DOCTOR`、`ROLE_PHARMACIST`、`ROLE_ADMIN`）。

### 统一响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | `Integer` | 状态码（200=成功，其他见错误码表） |
| `message` | `String` | 描述信息 |
| `data` | `T` | 业务数据（失败时为 `null`） |

### 分页响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "list": [...],
    "total": 100,
    "page": 1,
    "size": 10
  }
}
```

### 错误码

| 码段 | 模块 |
|------|------|
| 200 | 成功 |
| 400 | 参数校验失败 |
| 401 | 未登录 / Token 无效 |
| 403 | 无权限 |
| 1001-1009 | 用户/认证 |
| 2001-2007 | 挂号/排班 |
| 3001-3004 | 处方 |
| 4001-4010 | 问诊 |

---

## 1. 认证模块

### POST `/api/v1/auth/register` — 患者注册

**权限**: 无需认证

**请求体**:

```json
{
  "username": "string, 必填",
  "password": "string, 必填",
  "realName": "string, 必填",
  "idCard": "string, 必填, 正则: ^\\d{17}[\\dXx]$",
  "phone": "string, 必填, 正则: ^1[3-9]\\d{9}$",
  "gender": "int, 可选 (0:未知 1:男 2:女)"
}
```

**响应**: `Result<Void>`

---

### POST `/api/v1/auth/login` — 统一登录

**权限**: 无需认证

**请求体**:

```json
{
  "username": "string, 必填",
  "password": "string, 必填",
  "loginType": "string, 必填 (PATIENT | STAFF)"
}
```

**响应**: `Result<LoginResponse>`

```json
{
  "token": "JWT Token",
  "userId": 1,
  "username": "zhangsan",
  "realName": "张三",
  "role": "PATIENT",
  "doctorId": null,
  "patientId": 1
}
```

> `doctorId` 仅医生角色有值；`patientId` 仅患者角色有值。

---

### GET `/api/v1/auth/profile` — 获取当前患者信息

**权限**: `PATIENT`

**响应**: `Result<ProfileResponse>`

```json
{
  "id": 1,
  "username": "zhangsan",
  "realName": "张三",
  "idCard": "110***1234",
  "phone": "138****5678",
  "gender": 1,
  "createTime": "2026-01-01T00:00:00"
}
```

---

## 2. 首页统计模块

### GET `/api/v1/dashboard/stats` — 患者首页统计

**权限**: `PATIENT`

**响应**: `Result<PatientStats>`

```json
{
  "consultCount": 5,
  "appointmentCount": 2,
  "prescriptionCount": 3
}
```

---

## 3. AI 问诊模块

### POST `/api/v1/ai/multimodal/analyze` — 多模态图片分析

**权限**: `PATIENT`  
**Content-Type**: `multipart/form-data`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `file` | `MultipartFile` | 是 | 医学图片 |
| `type` | `String` | 是 | 图片类型 |

**响应**: `Result<MultimodalAnalyzeResponse>`

```json
{
  "fileUrl": "https://...",
  "draftId": "draft_xxx",
  "symptomDraft": "症状分析草稿内容"
}
```

---

### POST `/api/v1/ai/consult/stream` — RAG 流式问诊（SSE）

**权限**: `PATIENT`  
**Content-Type**: `application/json`  
**Accept**: `text/event-stream`

**请求体**:

```json
{
  "sessionId": "session_sn",
  "draftId": "draft_id, 可选",
  "message": "用户消息"
}
```

**响应**: `SseEmitter`（流式返回 AI 回复 token）

---

### POST `/api/v1/ai/sessions` — 创建问诊会话

**权限**: `PATIENT`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `draftId` | `String` | 否 | 关联多模态草稿 ID |
| `symptomDraft` | `String` | 否 | 症状草稿内容 |

**响应**: `Result<String>`（返回 `sessionSn`）

---

### GET `/api/v1/ai/sessions` — 问诊会话列表

**权限**: `PATIENT`

**查询参数**:

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `keyword` | `String` | — | 搜索关键词（匹配症状描述或 AI 总结） |
| `startDate` | `String` | — | 开始日期（yyyy-MM-dd） |
| `endDate` | `String` | — | 结束日期 |
| `status` | `String` | — | 会话状态筛选（IN_PROGRESS / COMPLETED / PENDING_DOCTOR / DOCTOR_ACTIVE） |
| `isPinned` | `Boolean` | — | 是否置顶 |
| `page` | `Integer` | 1 | 页码 |
| `size` | `Integer` | 10 | 每页大小 |

**响应**: `Result<PageResult<SessionVO>>`

```json
{
  "id": 1,
  "sessionSn": "SN20260101...",
  "symptomDraftSummary": "前100字摘要...",
  "aiSummary": "AI 总结",
  "turnCount": 5,
  "status": "IN_PROGRESS",
  "isPinned": false,
  "createTime": "2026-01-01T00:00:00",
  "lastChatTime": "2026-01-01T10:00:00",
  "hasRating": false
}
```

---

### GET `/api/v1/ai/sessions/{sessionSn}` — 会话详情

**权限**: `PATIENT`

**路径参数**: `sessionSn` — 会话编号

**响应**: `Result<SessionVO>`

---

### GET `/api/v1/ai/sessions/{sessionSn}/turns` — 对话轮次列表

**权限**: `PATIENT`

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | `int` | 1 | 页码 |
| `size` | `int` | 5 | 每页大小 |

**响应**: `Result<PageResult<TurnVO>>`

```json
{
  "id": 1,
  "turnNumber": 1,
  "userMessage": "用户消息",
  "assistantMessage": "AI/医生回复",
  "citations": [{ "title": "来源", "url": "..." }],
  "senderType": "PATIENT",
  "createTime": "2026-01-01T00:00:00"
}
```

> `senderType` 取值：`PATIENT`（患者消息）、`AI`（AI回复）、`DOCTOR`（医生回复）

---

### POST `/api/v1/ai/sessions/{sessionSn}/complete` — 确认结束问诊

**权限**: `PATIENT`

**响应**: `Result<Void>`

---

### PUT `/api/v1/ai/sessions/{sessionSn}/pin` — 置顶/取消置顶

**权限**: `PATIENT`

**响应**: `Result<Void>`（Toggle 操作）

---

### POST `/api/v1/ai/sessions/{sessionSn}/turns/{turnNumber}/regenerate` — 重新生成 AI 回复

**权限**: `PATIENT`  
**限制**: 仅支持最后一轮

**响应**: `Result<TurnVO>`

---

### POST `/api/v1/ai/sessions/{sessionSn}/rate` — 问诊评分

**权限**: `PATIENT`

**请求体**:

```json
{
  "rating": 5,
  "feedback": "可选文字反馈"
}
```

**响应**: `Result<Void>`

---

### POST `/api/v1/ai/sessions/{sessionSn}/delete` — 删除会话

**权限**: `PATIENT`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `mode` | `String` | 是 | `recycle` = 移入回收站；`permanent` = 彻底删除 |

**响应**: `Result<Void>`

---

### POST `/api/v1/ai/sessions/{sessionSn}/handoff` — 转接真人医生

**权限**: `PATIENT`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `reason` | `String` | 否 | 转诊原因 |

**响应**: `Result<Void>`（会话状态变更为 `PENDING_DOCTOR`）

---

### GET `/api/v1/ai/sessions/recycle-bin` — 回收站列表

**权限**: `PATIENT`

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | `int` | 1 | 页码 |
| `size` | `int` | 10 | 每页大小 |

**响应**: `Result<PageResult<SessionVO>>`

---

### POST `/api/v1/ai/sessions/recycle-bin/{sessionSn}/restore` — 从回收站恢复

**权限**: `PATIENT`

**响应**: `Result<Void>`

---

### POST `/api/v1/ai/sessions/recycle-bin/{sessionSn}/permanent` — 回收站彻底删除

**权限**: `PATIENT`

**响应**: `Result<Void>`

---

### POST `/api/v1/ai/knowledge/import` — 导入医学知识

**请求体**:

```json
{
  "title": "文档标题",
  "content": "文档内容",
  "category": "分类（科室）"
}
```

**响应**: `Result<Integer>`（返回导入文档数）

---

## 4. 挂号模块

### GET `/api/v1/schedule/list` — 查看可预约排班列表

**权限**: 无需认证

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `deptName` | `String` | 否 | 科室名称筛选 |
| `workDate` | `LocalDate` | 否 | 出诊日期（yyyy-MM-dd） |

**响应**: `Result<List<ScheduleVO>>`

```json
{
  "id": 1,
  "doctorId": 1001,
  "doctorName": "李医生",
  "deptName": "内科",
  "workDate": "2026-07-01",
  "shift": 1,
  "shiftName": "上午",
  "totalCount": 30,
  "visibleCount": 15,
  "price": 50.00
}
```

---

### POST `/api/v1/schedule/create` — 创建排班

**权限**: `ADMIN`

**请求体**:

```json
{
  "doctorId": 1001,
  "deptName": "内科",
  "workDate": "2026-07-01",
  "shift": 1,
  "totalCount": 30,
  "price": 50.00
}
```

**响应**: `Result<Void>`

---

### GET `/api/v1/doctor/{id}` — 查询医生详情

**权限**: 无需认证

**响应**: `Result<DoctorVO>`

```json
{
  "id": 1001,
  "name": "李医生",
  "title": "主任医师",
  "avatar": "https://...",
  "deptName": "内科",
  "specialty": "心血管疾病",
  "intro": "..."
}
```

---

### POST `/api/v1/registration/seckill` — 秒杀抢号

**权限**: `PATIENT`

**请求体**:

```json
{
  "scheduleId": 1
}
```

**响应**: `Result<SeckillResponse>`

```json
{
  "orderSn": "ORD20260701...",
  "status": "QUEUING"
}
```

> 采用乐观锁保证并发安全，号源不足时返回错误码 `2002`。

---

### GET `/api/v1/registration/order/detail` — 订单详情

**权限**: `PATIENT` / `ADMIN` / `DOCTOR`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `orderSn` | `String` | 是 | 订单号 |

**响应**: `Result<OrderVO>`

```json
{
  "orderSn": "ORD20260701...",
  "patientId": 1,
  "scheduleId": 1,
  "deptName": "内科",
  "doctorName": "李医生",
  "workDate": "2026-07-01",
  "shift": 1,
  "shiftName": "上午",
  "fee": 50.00,
  "status": 2,
  "createTime": "2026-07-01T00:00:00",
  "payTime": "2026-07-01T00:05:00"
}
```

> PATIENT 角色仅可查看自己的订单（Service 层强制归属校验）。

---

### GET `/api/v1/registration/order/list` — 患者挂号订单列表

**权限**: `PATIENT`

**响应**: `Result<List<OrderVO>>`

---

### POST `/api/v1/registration/order/cancel` — 取消订单

**权限**: `PATIENT`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `orderSn` | `String` | 是 | 订单号 |

**响应**: `Result<Void>`

---

### POST `/api/v1/registration/order/pay` — 支付订单

**权限**: `PATIENT`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `orderSn` | `String` | 是 | 订单号 |

**响应**: `Result<Void>`

---

## 5. 处方模块

### POST `/api/v1/prescription/issue` — 开具处方

**权限**: `DOCTOR`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `doctorId` | `Long` | 否 | 医生 ID（可选，默认从 Token 获取） |

**请求体**:

```json
{
  "patientId": 1,
  "consultationId": null,
  "diagnosis": "上呼吸道感染",
  "medicines": [
    {
      "medicineId": 101,
      "medicineName": "阿莫西林",
      "pharmacyId": 1,
      "quantity": 2,
      "unit": "盒",
      "spec": "0.25g*24",
      "usage": "每日三次，每次两粒",
      "price": 15.50
    }
  ]
}
```

**响应**: `Result<PrescriptionVO>`

> 开具时原子扣减药品库存，库存不足返回错误码 `3002`。

---

### GET `/api/v1/prescriptions` — 患者处方列表

**权限**: `PATIENT`

**响应**: `Result<List<PrescriptionVO>>`

---

### GET `/api/v1/prescriptions/pending-audit` — 待审核处方列表

**权限**: `PHARMACIST`

**响应**: `Result<List<PrescriptionVO>>`

---

### GET `/api/v1/prescriptions/{id}` — 处方详情

**权限**: `PATIENT` / `DOCTOR` / `PHARMACIST` / `ADMIN`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `patientId` | `Long` | 否 | 患者 ID（PATIENT 角色自动绑定） |

**响应**: `Result<PrescriptionVO>`

```json
{
  "id": 1,
  "prescriptionSn": "RX20260701...",
  "patientId": 1,
  "doctorId": 1001,
  "diagnosis": "上呼吸道感染",
  "pdfUrl": "/prescription-pdfs/...",
  "auditStatus": 0,
  "pharmacistId": null,
  "auditComments": null,
  "auditTime": null,
  "status": 0,
  "createTime": "2026-07-01T00:00:00",
  "items": [...]
}
```

---

### POST `/api/v1/prescriptions/{id}/audit` — 审核处方

**权限**: `PHARMACIST`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `pharmacistId` | `Long` | 否 | 药师 ID（默认从 Token 获取） |

**请求体**:

```json
{
  "action": "APPROVE",
  "comments": "审核意见"
}
```

`action` 取值：`APPROVE`（通过）/ `REJECT`（驳回，自动恢复库存）

**响应**: `Result<PrescriptionVO>`

---

### GET `/api/v1/pharmacy/inventory` — 药房库存列表

**权限**: `DOCTOR` / `PHARMACIST` / `ADMIN`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `pharmacyId` | `Long` | 是 | 院区药房 ID |

**响应**: `Result<List<InventoryVO>>`

```json
{
  "pharmacyId": 1,
  "medicineName": "阿莫西林",
  "stock": 100,
  "lockStock": 5,
  "unit": "盒"
}
```

---

## 6. 员工管理模块

> 基础路径: `/api/v1/staff`  
> 权限: `ADMIN`

### POST `/api/v1/staff` — 创建员工

**请求体**: `StaffRequest`（`username`, `password`, `realName`, `phone`, `role`, `doctorId`）

**响应**: `Result<StaffVO>`

### GET `/api/v1/staff` — 员工列表

**响应**: `Result<List<StaffVO>>`

### GET `/api/v1/staff/{id}` — 员工详情

**响应**: `Result<StaffVO>`

### PUT `/api/v1/staff/{id}` — 更新员工

**请求体**: `StaffRequest`

**响应**: `Result<StaffVO>`

### DELETE `/api/v1/staff/{id}` — 删除员工（软删除）

**响应**: `Result<Void>`

---

## 7. 患者管理模块（Admin）

> 基础路径: `/api/v1/admin/patients`  
> 权限: `ADMIN`

### GET `/api/v1/admin/patients` — 患者列表

**响应**: `Result<List<Patient>>`

### GET `/api/v1/admin/patients/{id}` — 患者详情

**响应**: `Result<Patient>`

### POST `/api/v1/admin/patients` — 添加患者

**请求体**: `RegisterRequest`（与患者注册相同）

**响应**: `Result<Void>`

### PUT `/api/v1/admin/patients/{id}` — 编辑患者

**请求体**: `Patient` 对象

**响应**: `Result<Void>`

### DELETE `/api/v1/admin/patients/{id}` — 删除患者（软删除）

**响应**: `Result<Void>`

---

## 8. 医生问诊模块

> 基础路径: `/api/v1/doctor/consultations`  
> 权限: `DOCTOR`

### GET `/api/v1/doctor/consultations/pending` — 待接诊列表

**权限**: `DOCTOR`

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `keyword` | `String` | — | 搜索关键词（匹配症状描述） |
| `page` | `Integer` | 1 | 页码 |
| `size` | `Integer` | 10 | 每页大小 |

**响应**: `Result<PageResult<DoctorConsultSessionVO>>`

```json
{
  "sessionSn": "SN20260101...",
  "patientName": "张三",
  "patientGender": 1,
  "patientAge": 28,
  "symptomSummary": "头痛三天，伴有恶心...",
  "fileCount": 0,
  "turnCount": 3,
  "status": "PENDING_DOCTOR",
  "lastChatTime": "2026-01-01T10:00:00"
}
```

> `status` 取值：`PENDING_DOCTOR`（待接诊）、`DOCTOR_ACTIVE`（沟通中）

---

### GET `/api/v1/doctor/consultations/{sessionSn}` — 问诊详情

**权限**: `DOCTOR`

**路径参数**: `sessionSn` — 会话编号

**响应**: `Result<DoctorConsultDetailVO>`

```json
{
  "sessionSn": "SN20260101...",
  "patientName": "张三",
  "patientGender": 1,
  "patientAge": 28,
  "symptomDraft": "AI分析报告内容...",
  "fileUrls": ["https://..."],
  "status": "PENDING_DOCTOR",
  "turns": [
    {
      "id": 1,
      "turnNumber": 1,
      "userMessage": "头痛三天",
      "assistantMessage": "建议多休息",
      "senderType": "AI",
      "createTime": "2026-01-01T10:00:00"
    }
  ],
  "createTime": "2026-01-01T10:00:00",
  "lastChatTime": "2026-01-01T11:00:00"
}
```

---

### POST `/api/v1/doctor/consultations/{sessionSn}/reply` — 回复患者

**权限**: `DOCTOR`

**请求体**:

```json
{
  "message": "建议多休息，避免熬夜。如持续不适请来院就诊。",
  "action": "REPLY"
}
```

> `action` 取值：`REPLY`（仅回复）、`RESOLVE`（回复并标记已解决）

**响应**: `Result<DoctorConsultReplyVO>`

```json
{
  "turnNumber": 2,
  "sessionStatus": "DOCTOR_ACTIVE"
}
```

---

### POST `/api/v1/doctor/consultations/{sessionSn}/resolve` — 标记已解决

**权限**: `DOCTOR`

**响应**: `Result<Void>`（会话状态变更为 `COMPLETED`）
