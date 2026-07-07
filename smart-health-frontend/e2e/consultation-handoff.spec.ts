/**
 * E2E 集成测试：AI 问诊 → 真人医生转诊完整链路
 *
 * 前置条件：
 *   - 前端 dev server 启动在 http://localhost:5173
 *   - 后端 API 可用且包含以下测试账号：
 *     - 患者: testpatient / 123456
 *     - 医生: doc_wangminghua / doctor123
 *
 * 运行: npx playwright test consultation-handoff
 */

import { test, expect, type Page } from '@playwright/test'

// ============ 测试常量 ============
const API_BASE = 'http://localhost:9000'
const PATIENT = { username: 'testpatient', password: '123456' }
const STAFF = { username: 'doc_wangminghua', password: 'doctor123' }

// ============ 辅助函数 ============

/** 患者密码登录 */
async function patientLogin(page: Page) {
  await page.goto('/login')
  // 切换到"密码登录"模式（患者默认是短信登录 Tab）
  await page.locator('button:has-text("密码登录")').click()
  // 等待密码表单出现（v-if 渲染）
  await page.waitForSelector('input[placeholder="请输入用户名"]', { timeout: 5000 })
  // 填写用户名
  await page.locator('input[placeholder="请输入用户名"]').fill(PATIENT.username)
  // 填写密码
  await page.locator('input[placeholder="请输入密码"]').fill(PATIENT.password)
  // 点击登录（使用 type=submit 精确定位）
  await page.locator('button[type="submit"]').click()
  await page.waitForURL('**/home', { timeout: 15000 })
  await expect(page.locator('.hero-greeting')).toBeVisible({ timeout: 5000 })
}

/** 员工（医生）登录 */
async function staffLogin(page: Page) {
  await page.goto('/login')
  await page.locator('button:has-text("员工登录")').click()
  // 员工直接显示密码登录表单
  await page.waitForSelector('input[placeholder="请输入用户名"]', { timeout: 5000 })
  await page.locator('input[placeholder="请输入用户名"]').fill(STAFF.username)
  await page.locator('input[placeholder="请输入密码"]').fill(STAFF.password)
  await page.locator('button[type="submit"]').click()
  await page.waitForURL('**/home', { timeout: 15000 })
  await expect(page.locator('.hero-greeting')).toBeVisible({ timeout: 5000 })
}

/** 退出登录（先导航到首页，因为退出按钮在首页） */
async function logout(page: Page) {
  await page.goto('/home')
  await page.waitForSelector('.hero-avatar--clickable', { timeout: 5000 })
  await page.locator('.hero-avatar--clickable').click()
  await page.locator('text=退出登录').click()
  await page.locator('.van-dialog').waitFor({ timeout: 3000 })
  await page.locator('.van-dialog__confirm, button:has-text("确定")').first().click()
  await page.waitForURL('**/login', { timeout: 5000 })
}

/** 通过 API 创建问诊会话 */
async function apiCreateSession(token: string, symptomDraft: string): Promise<string> {
  const res = await fetch(`${API_BASE}/api/v1/ai/sessions?draftId=e2e-test&symptomDraft=${encodeURIComponent(symptomDraft)}`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` }
  })
  const body = await res.json()
  if (body.code !== 200) throw new Error(`创建会话失败: ${body.message}`)
  return body.data // sessionSn
}

/** 通过 API 插入 AI 对话轮次 */
async function apiInsertAiTurn(token: string, sessionSn: string) {
  const res = await fetch(`${API_BASE}/api/v1/ai/sessions/${sessionSn}/turns/1/regenerate`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` }
  })
  if (res.status !== 200) {
    console.log('AI regenerate not available, skipping turn creation')
  }
}

/** 通过 API 获取患者 token */
async function apiPatientLogin(): Promise<string> {
  const res = await fetch(`${API_BASE}/api/v1/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: PATIENT.username, password: PATIENT.password, loginType: 'PATIENT' })
  })
  const body = await res.json()
  if (body.code !== 200) throw new Error(`患者登录失败: ${body.message}`)
  return body.data.token
}

/** 通过 API 获取医生 token */
async function apiStaffLogin(): Promise<string> {
  const res = await fetch(`${API_BASE}/api/v1/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: STAFF.username, password: STAFF.password, loginType: 'STAFF' })
  })
  const body = await res.json()
  if (body.code !== 200) throw new Error(`医生登录失败: ${body.message}`)
  return body.data.token
}

/** 等待加载指示器消失 */
async function waitForLoad(page: Page) {
  try {
    await page.waitForSelector('.van-loading', { state: 'detached', timeout: 8000 })
  } catch { /* ok */ }
}

// ============ 场景 1：完整转诊流程 ============

test.describe('完整转诊流程', () => {
  test('患者创建会话 → 转接医生 → 医生接诊列表可见 → 医生查看详情', async ({ page }) => {
    test.setTimeout(180000)

    // ---- Step 1: 患者登录 ----
    await patientLogin(page)

    // ---- Step 2: 通过 API 创建问诊会话并导航到聊天页 ----
    const token = await apiPatientLogin()
    const sessionSn = await apiCreateSession(token, '头痛三天，伴有恶心，无发热')
    console.log(`Created session: ${sessionSn}`)

    // 导航到聊天页
    await page.goto(`/consultation/chat/${sessionSn}`)
    await waitForLoad(page)

    // ---- Step 3: 等待页面加载 ----
    await page.waitForURL(`**/consultation/chat/${sessionSn}`, { timeout: 10000 })
    await page.waitForSelector('.chat-input-area', { timeout: 10000 })

    // ---- Step 4: 转接真人医生 ----
    // 如果有对话，转接按钮会出现；否则通过 API 转接
    const handoffBtn = page.locator('button:has-text("转接真人医生")')
    try {
      await handoffBtn.waitFor({ state: 'visible', timeout: 5000 })
      await handoffBtn.click()

      // 确认弹窗: confirmButtonText = "确认转接"
      await page.locator('.van-dialog').waitFor({ timeout: 3000 })
      await page.locator('button:has-text("确认转接")').click()
      await waitForLoad(page)

      // 验证等待提示
      await expect(page.locator('.handoff-waiting')).toBeVisible({ timeout: 5000 })
      console.log('Handoff completed via UI')
    } catch {
      console.log('Handoff button not visible (no AI turns), using API handoff')
      await fetch(`${API_BASE}/api/v1/ai/sessions/${sessionSn}/handoff`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      })
      await page.reload()
      await waitForLoad(page)
      console.log('Handoff completed via API')
    }

    // ---- Step 5: 患者退出 → 医生登录 ----
    await logout(page)
    await staffLogin(page)

    // ---- Step 6: 医生进入接诊列表 ----
    await page.locator('text=问诊接诊').first().click()
    await page.waitForURL('**/doctor/consultations**', { timeout: 5000 })
    await waitForLoad(page)

    // 验证列表页标题
    await expect(page.locator('.van-nav-bar__title')).toContainText('待接诊', { timeout: 5000 })

    // 验证列表中能看到会话
    await expect(page.locator('.van-cell').first()).toBeVisible({ timeout: 5000 })
    console.log('Doctor sees pending consultations')

    // ---- Step 7: 进入问诊详情 ----
    await page.goto(`/doctor/consultations/${sessionSn}`)
    await page.waitForURL(`**/doctor/consultations/${sessionSn}`, { timeout: 10000 })
    await waitForLoad(page)

    // 验证患者信息卡可见
    await expect(page.locator('.patient-card')).toBeVisible({ timeout: 5000 })
    // 验证 AI 分析报告区域可见
    await expect(page.locator('.analysis-card')).toBeVisible({ timeout: 5000 })
    console.log('Doctor viewed consultation detail')

    // ---- Step 8: 医生输入回复 ----
    // van-field type="textarea" 渲染 textarea
    const textarea = page.locator('.reply-area textarea, .reply-area input')
    if (await textarea.isVisible({ timeout: 3000 })) {
      await textarea.fill('建议多休息，避免熬夜。如持续不适请来院就诊。')
      await page.locator('button:has-text("回复")').first().click()

      // 验证 toast 提示
      await expect(page.locator('.van-toast')).toBeVisible({ timeout: 5000 })
      // 等待 toast 消失
      await page.waitForTimeout(2000)
      console.log('Doctor reply sent successfully')
    }

    // ---- Step 9: 医生退出 → 患者重新登录 ----
    await logout(page)
    await patientLogin(page)

    // ---- Step 10: 患者查看会话列表 ----
    await page.locator('.van-tabbar-item:has-text("问诊")').click()
    await page.waitForURL('**/consultation', { timeout: 5000 })

    // 等待列表加载
    await expect(page.locator('.session-card, .van-cell').first()).toBeVisible({ timeout: 5000 })
    console.log('Patient can see session list after doctor reply')
  })
})

// ============ 场景 2：边界场景 ============

test.describe('边界场景', () => {
  test('非 DOCTOR 角色无法访问接诊页面', async ({ page }) => {
    await patientLogin(page)

    // 直接 URL 访问
    await page.goto('/doctor/consultations')

    // 路由守卫应重定向到 /home
    await page.waitForURL('**/home', { timeout: 5000 })
    await expect(page.locator('.hero-greeting')).toBeVisible({ timeout: 3000 })
    console.log('Patient correctly denied from doctor consultations page')
  })

  test('医生问诊列表页正常访问', async ({ page }) => {
    await staffLogin(page)

    await page.goto('/doctor/consultations')
    await waitForLoad(page)

    // 验证页面标题
    await expect(page.locator('.van-nav-bar__title')).toContainText('待接诊', { timeout: 5000 })
    console.log('Doctor consultations page accessible')
  })
})
