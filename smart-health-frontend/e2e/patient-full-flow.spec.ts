/**
 * E2E 全链路测试：患者端全功能检测
 *
 * 覆盖范围：
 *   1. 密码登录
 *   2. 首页展示（问候语、统计数据、功能入口）
 *   3. 问诊记录列表
 *   4. 挂号订单列表
 *   5. 处方列表
 *   6. 账号信息页面
 *   7. 退出登录
 *
 * 前置条件：
 *   - 前端 dev server 启动在 http://localhost:5173
 *
 * 运行: npx playwright test patient-full-flow
 */

import { test, expect } from '@playwright/test'
import type { Page, Route } from '@playwright/test'

// ============ 测试常量 ============
const PATIENT = { username: 'testpatient', password: '123456' }

// ============ Mock 响应数据 ============
const MOCK_LOGIN = {
  code: 200, message: 'success',
  data: { token: 'mock-e2e-token', userId: 100, username: 'testpatient', realName: '张三', role: 'PATIENT', patientId: 100, doctorId: null }
}

const MOCK_SMS_LOGIN = {
  code: 200, message: 'success',
  data: { token: 'mock-e2e-token-sms', userId: 101, username: '13800001111', realName: '李四', role: 'PATIENT', patientId: 101, doctorId: null }
}

const MOCK_SEND_CODE = { code: 200, message: 'success', data: null }

const MOCK_DASHBOARD_STATS = {
  code: 200, message: 'success',
  data: { consultCount: 3, appointmentCount: 2, prescriptionCount: 5 }
}

const MOCK_SESSION_LIST = {
  code: 200, message: 'success',
  data: [
    { id: 1, sessionSn: 'SESSION_001', symptomDraftSummary: '皮肤红斑分析，可能为湿疹或体癣', turnCount: 3, status: 'IN_PROGRESS', isPinned: false, createTime: '2026-06-28T14:30:00', lastChatTime: '2026-06-28T14:35:00', aiSummary: '', hasRating: false },
    { id: 2, sessionSn: 'SESSION_002', symptomDraftSummary: '头痛伴恶心，持续两天', turnCount: 5, status: 'COMPLETED', isPinned: true, createTime: '2026-06-27T09:15:00', lastChatTime: '2026-06-27T10:00:00', aiSummary: '', hasRating: true },
    { id: 3, sessionSn: 'SESSION_003', symptomDraftSummary: '咳嗽一周有黄痰', turnCount: 2, status: 'PENDING_DOCTOR', isPinned: false, createTime: '2026-06-26T16:45:00', lastChatTime: '2026-06-26T16:50:00', aiSummary: '', hasRating: false }
  ]
}

const MOCK_ORDER_LIST = {
  code: 200, message: 'success',
  data: [
    { id: 1, orderSn: 'REG_20260630_0001', patientId: 100, scheduleId: 1, doctorId: 1, deptName: '皮肤科', doctorName: '王明华', workDate: '2026-06-30', shift: 1, shiftName: '上午', fee: 50.00, status: 2, createTime: '2026-06-29T10:30:00', payTime: '2026-06-29T10:32:00' },
    { id: 2, orderSn: 'REG_20260628_0003', patientId: 100, scheduleId: 2, doctorId: 2, deptName: '内科', doctorName: '李秀英', workDate: '2026-06-28', shift: 1, shiftName: '上午', fee: 30.00, status: 3, createTime: '2026-06-27T15:20:00', payTime: '2026-06-27T15:21:00' },
    { id: 3, orderSn: 'REG_20260625_0010', patientId: 100, scheduleId: 4, doctorId: 1, deptName: '皮肤科', doctorName: '王明华', workDate: '2026-06-25', shift: 1, shiftName: '上午', fee: 50.00, status: 1, createTime: '2026-06-29T08:00:00' }
  ]
}

const MOCK_PRESCRIPTION_LIST = {
  code: 200, message: 'success',
  data: [
    { id: 1, prescriptionSn: 'RX_H001_20260628_0001', patientId: 100, doctorId: 1, diagnosis: '湿疹，建议外用糖皮质激素软膏', auditStatus: 1, status: 2, createTime: '2026-06-28 14:30:00', items: [{ id: 1, medicineName: '卤米松乳膏', spec: '15g/支', usage: '外用，每日2次', quantity: 2, unit: '支', price: 25.50 }] },
    { id: 2, prescriptionSn: 'RX_H001_20260625_0003', patientId: 100, doctorId: 2, diagnosis: '急性上呼吸道感染', auditStatus: 0, status: 0, createTime: '2026-06-25 10:00:00', items: [{ id: 3, medicineName: '阿莫西林胶囊', spec: '0.5g*24粒', usage: '口服，每日3次', quantity: 1, unit: '盒', price: 12.50 }] }
  ]
}

const MOCK_PROFILE = {
  code: 200, message: 'success',
  data: { id: 100, username: 'testpatient', realName: '张三', idCard: '310***********1234', phone: '138****5678', gender: 1, email: null, avatar: null, birthday: '1990-01-15', idCardStatus: 2, idCardFrontUrl: null, idCardBackUrl: null, faceRecognitionUrl: null, createTime: '2026-01-01T00:00:00' }
}

const MOCK_LOGOUT = { code: 200, message: 'success', data: null }

// ============ Mock API 设置 ============
// Playwright 按 LIFO（后注册优先）顺序评估路由
// 因此：通配路由先注册（作为兜底），具体路由后注册（优先匹配）
async function setupApiMocks(page: Page) {
  // ---- 通配兜底（先注册，后评估） ----
  await page.route('**/api/v1/**', async (route: Route) => {
    console.log(`[Mock Fallback] ${route.request().method()} ${route.request().url()}`)
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 200, message: 'success', data: null }) })
  })

  // ---- 具体路由（后注册，先评估） ----
  await page.route('**/api/v1/auth/login', async (route: Route) => {
    console.log(`[Mock] POST /auth/login`)
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(MOCK_LOGIN) })
  })

  await page.route('**/api/v1/auth/login/sms', async (route: Route) => {
    console.log(`[Mock] POST /auth/login/sms`)
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(MOCK_SMS_LOGIN) })
  })

  await page.route('**/api/v1/auth/send-code', async (route: Route) => {
    console.log(`[Mock] POST /auth/send-code`)
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(MOCK_SEND_CODE) })
  })

  await page.route('**/api/v1/dashboard/stats', async (route: Route) => {
    console.log(`[Mock] GET /dashboard/stats`)
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(MOCK_DASHBOARD_STATS) })
  })

  await page.route(/\/api\/v1\/ai\/sessions(\?.*)?$/, async (route: Route, request) => {
    console.log(`[Mock] ${request.method()} /ai/sessions${request.url().includes('?') ? ' (with params)' : ''}`)
    if (request.method() === 'GET') {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(MOCK_SESSION_LIST) })
    } else {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 200, message: 'success', data: 'SESSION_NEW' }) })
    }
  })

  await page.route('**/api/v1/registration/order/list', async (route: Route) => {
    console.log(`[Mock] GET /registration/order/list`)
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(MOCK_ORDER_LIST) })
  })

  await page.route('**/api/v1/prescriptions', async (route: Route) => {
    console.log(`[Mock] GET /prescriptions`)
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(MOCK_PRESCRIPTION_LIST) })
  })

  await page.route('**/api/v1/auth/profile', async (route: Route) => {
    console.log(`[Mock] GET /auth/profile`)
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(MOCK_PROFILE) })
  })

  await page.route('**/api/v1/auth/logout', async (route: Route) => {
    console.log(`[Mock] POST /auth/logout`)
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(MOCK_LOGOUT) })
  })
}

// ============ 辅助函数 ============

/** 患者密码登录 */
async function patientLogin(page: Page) {
  await page.goto('/login')
  // 切换到密码登录模式（患者默认短信登录）
  await page.locator('button:has-text("密码登录")').click()
  await page.waitForSelector('input[placeholder="请输入用户名"]', { timeout: 5000 })
  await page.locator('input[placeholder="请输入用户名"]').fill(PATIENT.username)
  await page.locator('input[placeholder="请输入密码"]').fill(PATIENT.password)
  await page.locator('button[type="submit"]').click()
  // 等待跳转到首页
  await page.waitForURL('**/home', { timeout: 15000 })
}

// ============ 测试套件 ============

test.describe('患者端全功能全链路测试', () => {

  // ============ 场景 1：登录 ============
  test.describe('登录流程', () => {
    test('短信验证码登录成功并跳转到首页', async ({ page }) => {
      await setupApiMocks(page)
      await page.goto('/login')

      // 默认已经是短信登录模式，输入手机号
      await page.waitForSelector('input[placeholder="请输入手机号"]', { timeout: 5000 })
      await page.locator('input[placeholder="请输入手机号"]').fill('13800001111')
      // 点击获取验证码
      await page.locator('button:has-text("获取验证码")').click()
      // 输入验证码
      await page.waitForSelector('input[placeholder="请输入验证码"]', { timeout: 5000 })
      await page.locator('input[placeholder="请输入验证码"]').fill('123456')
      // 点击登录
      await page.locator('button[type="submit"]').click()
      // 等待跳转到首页
      await page.waitForURL('**/home', { timeout: 15000 })

      // 验证首页问候语可见
      await expect(page.locator('.hero-greeting')).toBeVisible({ timeout: 5000 })
      await expect(page.locator('.hero-greeting')).toContainText('李四')
      console.log('[PASS] 短信验证码登录成功，首页显示李四')
    })

    test('密码登录成功并跳转到首页', async ({ page }) => {
      await setupApiMocks(page)
      await patientLogin(page)

      // 验证首页问候语可见
      await expect(page.locator('.hero-greeting')).toBeVisible({ timeout: 5000 })
      await expect(page.locator('.hero-greeting')).toContainText('你好，')
      console.log('[PASS] 密码登录成功，首页问候语可见')
    })

    test('其他登录方式入口可见', async ({ page }) => {
      await page.goto('/login')
      // 验证角色切换 Tab 可见
      await expect(page.locator('button:has-text("患者登录")')).toBeVisible()
      await expect(page.locator('button:has-text("员工登录")')).toBeVisible()
      // 验证短信/密码切换
      await expect(page.locator('button:has-text("短信登录")')).toBeVisible()
      await expect(page.locator('button:has-text("密码登录")')).toBeVisible()
      // 验证第三方登录入口
      await expect(page.locator('text=其他登录方式')).toBeVisible()
      console.log('[PASS] 登录页所有入口可见')
    })
  })

  // ============ 场景 2：首页 ============
  test.describe('首页功能', () => {
    test('首页展示问候语、统计数据、功能菜单和快捷入口', async ({ page }) => {
      await setupApiMocks(page)
      await patientLogin(page)

      // 1. 问候语
      await expect(page.locator('.hero-greeting')).toContainText('张三')
      console.log('[PASS] 问候语显示患者姓名')

      // 2. 统计数据
      const statValues = page.locator('.hero-stat-value')
      await expect(statValues.nth(0)).toHaveText('3', { timeout: 5000 })
      await expect(statValues.nth(1)).toHaveText('2')
      await expect(statValues.nth(2)).toHaveText('5')
      const statLabels = page.locator('.hero-stat-label')
      await expect(statLabels.nth(0)).toHaveText('问诊')
      await expect(statLabels.nth(1)).toHaveText('挂号')
      await expect(statLabels.nth(2)).toHaveText('处方')
      console.log('[PASS] 统计数据展示正确')

      // 3. 功能菜单（Feature Grid）
      const featureCards = page.locator('.feature-card')
      await expect(featureCards.nth(0)).toContainText('AI 智能问诊')
      await expect(featureCards.nth(1)).toContainText('挂号预约')
      await expect(featureCards.nth(2)).toContainText('我的处方')
      await expect(featureCards.nth(3)).toContainText('挂号记录')
      console.log('[PASS] 功能菜单展示完整')

      // 4. 快捷入口
      await expect(page.getByText('挂号订单', { exact: true }).first()).toBeVisible()
      await expect(page.getByText('我的处方', { exact: true }).first()).toBeVisible()
      await expect(page.getByText('问诊记录', { exact: true }).first()).toBeVisible()
      console.log('[PASS] 快捷入口展示完整')
    })

    test('首页底部 Tab 栏导航完整', async ({ page }) => {
      await setupApiMocks(page)
      await patientLogin(page)

      await page.waitForSelector('.app-tabbar', { timeout: 5000 })
      const tabItems = page.locator('.app-tabbar .van-tabbar-item')
      await expect(tabItems.nth(0)).toContainText('首页')
      await expect(tabItems.nth(1)).toContainText('问诊')
      await expect(tabItems.nth(2)).toContainText('订单')
      await expect(tabItems.nth(3)).toContainText('处方')
      await expect(tabItems.nth(4)).toContainText('我的')
      console.log('[PASS] Tab 栏 5 个导航项完整')
    })
  })

  // ============ 场景 3：问诊记录 ============
  test.describe('问诊记录', () => {
    test('通过 Tab 栏导航到问诊记录列表页', async ({ page }) => {
      await setupApiMocks(page)
      await patientLogin(page)

      // 点击问诊 Tab
      await page.locator('.van-tabbar-item:has-text("问诊")').click()
      await page.waitForURL('**/consultation', { timeout: 5000 })

      // 验证页面标题
      await expect(page.locator('.van-nav-bar__title')).toContainText('问诊记录', { timeout: 5000 })
      // 验证会话卡片列表
      await expect(page.locator('.session-card').first()).toBeVisible({ timeout: 5000 })
      // 验证具体会话内容
      await expect(page.locator('.session-card__title').first()).toContainText('皮肤红斑分析')
      console.log('[PASS] 问诊记录列表展示正确')

      // 验证置顶标记
      const pinnedCards = page.locator('.session-card--pinned')
      await expect(pinnedCards).toHaveCount(1)
      console.log('[PASS] 置顶会话标记正确')
    })
  })

  // ============ 场景 4：挂号订单 ============
  test.describe('挂号订单', () => {
    test('通过 Tab 栏导航到挂号订单列表页', async ({ page }) => {
      await setupApiMocks(page)
      await patientLogin(page)

      // 点击订单 Tab
      await page.locator('.van-tabbar-item:has-text("订单")').click()
      await page.waitForURL('**/registration/orders', { timeout: 5000 })

      // 验证页面标题
      await expect(page.locator('.van-nav-bar__title')).toContainText('我的挂号订单', { timeout: 5000 })
      // 验证 Tab 切换
      await expect(page.locator('.van-tab:has-text("有效订单")')).toBeVisible()
      await expect(page.locator('.van-tab:has-text("已取消")')).toBeVisible()

      // 验证订单卡片列表
      await expect(page.locator('.order-card').first()).toBeVisible({ timeout: 5000 })
      const orderCards = page.locator('.order-card')
      // 至少有一个订单卡片可见
      await expect(orderCards.first()).toContainText('REG_')
      console.log('[PASS] 挂号订单列表展示正确')

      // 验证已支付状态的标签
      await expect(orderCards.first()).toContainText('已支付')
      console.log('[PASS] 订单状态标签正确')
    })
  })

  // ============ 场景 5：处方 ============
  test.describe('处方列表', () => {
    test('通过 Tab 栏导航到处方列表页', async ({ page }) => {
      await setupApiMocks(page)
      await patientLogin(page)

      // 点击处方 Tab
      await page.locator('.van-tabbar-item:has-text("处方")').click()
      await page.waitForURL('**/prescriptions', { timeout: 5000 })

      // 验证页面标题
      await expect(page.locator('.van-nav-bar__title')).toContainText('我的处方', { timeout: 5000 })
      // 验证处方卡片列表
      await expect(page.locator('.rx-card').first()).toBeVisible({ timeout: 5000 })
      // 验证处方编号
      await expect(page.locator('.rx-sn').first()).toContainText('RX_')
      // 验证诊断信息
      await expect(page.locator('.rx-diagnosis').first()).toContainText('湿疹')
      console.log('[PASS] 处方列表展示正确')

      // 验证审核状态标签
      const statusTag = page.locator('.rx-card .van-tag').first()
      await expect(statusTag).toBeVisible()
      console.log('[PASS] 处方审核状态标签可见')
    })
  })

  // ============ 场景 6：账号信息 ============
  test.describe('账号信息', () => {
    test('账号信息页面展示个人信息', async ({ page }) => {
      await setupApiMocks(page)
      await patientLogin(page)

      // 点击「我的」Tab
      await page.locator('.van-tabbar-item:has-text("我的")').click()
      await page.waitForURL('**/my', { timeout: 5000 })

      // 验证页面标题
      await expect(page.locator('.van-nav-bar__title')).toContainText('我的', { timeout: 5000 })

      // 等待 profile 加载完成（头像和用户名自动等待）

      // 验证头像可见
      await expect(page.locator('.info-avatar')).toBeVisible({ timeout: 10000 })
      // 验证用户名
      await expect(page.locator('.info-item').nth(1)).toContainText('testpatient')
      // 验证手机号（脱敏）
      await expect(page.locator('.info-item').nth(2)).toContainText('138****5678')
      // 验证实名认证状态
      await expect(page.locator('.info-item').nth(4)).toContainText('张三')
      await expect(page.locator('.info-item').nth(4)).toContainText('310')
      // 验证性别
      await expect(page.locator('.info-item').nth(5)).toContainText('男')
      // 验证退出登录按钮
      await expect(page.locator('button:has-text("退出登录")')).toBeVisible()
      console.log('[PASS] 账号信息页面展示正确')
    })
  })

  // ============ 场景 7：退出登录 ============
  test.describe('退出登录', () => {
    test('从首页头像面板退出登录', async ({ page }) => {
      await setupApiMocks(page)
      await patientLogin(page)

      // 点击头像打开面板
      await page.locator('.hero-avatar--clickable').click()
      // 点击退出登录
      await page.locator('text=退出登录').click()
      // 确认弹窗
      await page.locator('.van-dialog').waitFor({ timeout: 3000 })
      await page.locator('.van-dialog__confirm, button:has-text("确定")').first().click()
      // 验证跳转到登录页
      await page.waitForURL('**/login', { timeout: 10000 })
      console.log('[PASS] 退出登录成功，跳转到登录页')
    })

    test('从账号信息页退出登录', async ({ page }) => {
      await setupApiMocks(page)
      await patientLogin(page)

      // 导航到账号信息页
      await page.locator('.van-tabbar-item:has-text("我的")').click()
      await page.waitForURL('**/my', { timeout: 5000 })
      // 等待页面加载（退出登录按钮自动等待）

      // 点击退出登录按钮
      await page.locator('button:has-text("退出登录")').click()
      // 确认弹窗
      await page.locator('.van-dialog').waitFor({ timeout: 3000 })
      await page.locator('.van-dialog__confirm, button:has-text("确定")').first().click()
      // 验证跳转到登录页
      await page.waitForURL('**/login', { timeout: 10000 })
      console.log('[PASS] 从账号信息页退出登录成功')
    })
  })
})
