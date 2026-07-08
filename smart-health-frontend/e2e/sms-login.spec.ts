/**
 * E2E 测试：短信验证码登录服务
 *
 * 覆盖范围：
 *   1. 短信验证码发送（获取验证码按钮交互）
 *   2. 短信验证码登录成功（正常手机号 + 验证码）
 *   3. 短信验证码首次注册登录（isNewUser 场景）
 *   4. 手机号格式校验（无效手机号被阻止）
 *   5. 空字段校验（手机号/验证码为空）
 *   6. 短信登录与密码登录模式切换
 *   7. 患者/员工角色切换后 UI 变化
 *   8. 验证码倒计时行为
 *   9. 登录成功后跳转与问候语验证
 *   10. 登录失败场景（错误验证码）
 *
 * 前置条件：
 *   - 前端 dev server 启动在 http://localhost:5173
 *
 * 运行: npx playwright test sms-login
 */

import { test, expect } from '@playwright/test'
import type { Page, Route } from '@playwright/test'

// ============ Mock 响应数据 ============

/** 发送验证码成功 */
const MOCK_SEND_CODE_SUCCESS = {
  code: 200, message: '验证码已发送', data: null
}

/** 发送验证码失败（手机号未注册等） */
const MOCK_SEND_CODE_FAIL = {
  code: 400, message: '手机号未注册', data: null
}

/** 短信登录成功（普通用户） */
const MOCK_SMS_LOGIN_SUCCESS = {
  code: 200, message: '登录成功',
  data: {
    token: 'mock-sms-token-' + Date.now(),
    userId: 101,
    username: '13800001111',
    realName: '李四',
    role: 'PATIENT',
    patientId: 101,
    doctorId: null,
    isNewUser: false,
    randomPassword: null
  }
}

/** 短信登录成功（新用户首次注册登录） */
const MOCK_SMS_LOGIN_NEW_USER = {
  code: 200, message: '注册成功',
  data: {
    token: 'mock-sms-token-new-' + Date.now(),
    userId: 999,
    username: '13900009999',
    realName: '王五',
    role: 'PATIENT',
    patientId: 999,
    doctorId: null,
    isNewUser: true,
    randomPassword: 'Abc12345'
  }
}

/** 短信登录失败（验证码错误） */
const MOCK_SMS_LOGIN_FAIL = {
  code: 400, message: '验证码错误', data: null
}

/** 短信登录失败（手机号不存在） */
const MOCK_SMS_LOGIN_NOT_FOUND = {
  code: 404, message: '手机号未注册', data: null
}

/** 仪表盘统计数据 */
const MOCK_DASHBOARD_STATS = {
  code: 200, message: 'success',
  data: { consultCount: 5, appointmentCount: 3, prescriptionCount: 7 }
}

/** 用户信息 */
const MOCK_PROFILE = {
  code: 200, message: 'success',
  data: {
    id: 101, username: '13800001111', realName: '李四',
    idCard: '310***********1234', phone: '138****5678',
    gender: 1, email: null, avatar: null, birthday: '1990-01-15',
    idCardStatus: 2, idCardFrontUrl: null, idCardBackUrl: null,
    faceRecognitionUrl: null, createTime: '2026-01-01T00:00:00'
  }
}

/** 登出 */
const MOCK_LOGOUT = { code: 200, message: 'success', data: null }

// ============ Mock API 设置 ============

async function setupApiMocks(page: Page, options?: {
  smsLoginResponse?: typeof MOCK_SMS_LOGIN_SUCCESS
  sendCodeResponse?: typeof MOCK_SEND_CODE_SUCCESS
}) {
  // 通配兜底（先注册，后评估）
  await page.route('**/api/v1/**', async (route: Route) => {
    console.log(`[Mock Fallback] ${route.request().method()} ${route.request().url()}`)
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, message: 'success', data: null })
    })
  })

  // 发送验证码
  const sendCodeResp = options?.sendCodeResponse ?? MOCK_SEND_CODE_SUCCESS
  await page.route('**/api/v1/auth/send-code', async (route: Route) => {
    const body = route.request().postDataJSON()
    console.log(`[Mock] POST /auth/send-code phone=${body?.phone}`)
    await route.fulfill({
      status: sendCodeResp.code === 200 ? 200 : sendCodeResp.code,
      contentType: 'application/json',
      body: JSON.stringify(sendCodeResp)
    })
  })

  // 短信验证码登录
  const smsLoginResp = options?.smsLoginResponse ?? MOCK_SMS_LOGIN_SUCCESS
  await page.route('**/api/v1/auth/login/sms', async (route: Route) => {
    const body = route.request().postDataJSON()
    console.log(`[Mock] POST /auth/login/sms phone=${body?.phone} code=${body?.code}`)
    await route.fulfill({
      status: smsLoginResp.code === 200 ? 200 : smsLoginResp.code,
      contentType: 'application/json',
      body: JSON.stringify(smsLoginResp)
    })
  })

  // 仪表盘
  await page.route('**/api/v1/dashboard/stats', async (route: Route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_DASHBOARD_STATS)
    })
  })

  // 用户信息
  await page.route('**/api/v1/auth/profile', async (route: Route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_PROFILE)
    })
  })

  // 登出
  await page.route('**/api/v1/auth/logout', async (route: Route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_LOGOUT)
    })
  })
}

// ============ 辅助函数 ============

/** 执行短信验证码登录 */
async function smsLogin(page: Page, phone: string, code: string) {
  await page.goto('/login')

  // 确保在短信登录模式（患者默认就是短信登录）
  const smsTab = page.locator('button.switch-btn:has-text("短信登录")')
  await smsTab.waitFor({ state: 'visible', timeout: 5000 })
  if (await smsTab.getAttribute('class').then(c => !c?.includes('active'))) {
    await smsTab.click()
  }

  // 输入手机号
  await page.waitForSelector('input[placeholder="请输入手机号"]', { timeout: 5000 })
  await page.locator('input[placeholder="请输入手机号"]').fill(phone)

  // 输入验证码
  await page.locator('input[placeholder="请输入验证码"]').fill(code)

  // 点击登录
  await page.locator('button[type="submit"]').click()
}

// ============ 测试套件 ============

test.describe('短信验证码登录服务', () => {

  // ============ 场景 1: 验证码发送流程 ============
  test.describe('短信验证码发送', () => {

    test('发送验证码成功并开始倒计时', async ({ page }) => {
      await setupApiMocks(page)
      await page.goto('/login')

      // 输入手机号
      await page.locator('input[placeholder="请输入手机号"]').fill('13800001111')

      // 点击获取验证码
      const codeBtn = page.locator('button:has-text("获取验证码")')
      await expect(codeBtn).toBeEnabled()
      await codeBtn.click()

      // 验证 Toast
      await expect(page.locator('.van-toast')).toContainText('验证码已发送', { timeout: 3000 })

      // 验证按钮进入倒计时状态
      await expect(codeBtn).toBeDisabled()
      await expect(codeBtn).toContainText(/^\d+s$/) // 显示 60s、59s 等

      // 等待倒计时减少
      await page.waitForTimeout(2000)
      await expect(codeBtn).toContainText(/^[0-5]\ds$/) // 倒计时在减少
      console.log('[PASS] 发送验证码成功，倒计时正常')
    })

    test('无效手机号无法发送验证码', async ({ page }) => {
      await setupApiMocks(page)
      await page.goto('/login')

      // 检查无效手机号：获取验证码按钮应为禁用状态
      await page.locator('input[placeholder="请输入手机号"]').fill('12345')
      const codeBtn = page.locator('button:has-text("获取验证码")')
      await expect(codeBtn).toBeDisabled()
      console.log('[PASS] 无效手机号时获取验证码按钮禁用')
    })

    test('空手机号无法发送验证码', async ({ page }) => {
      await setupApiMocks(page)
      await page.goto('/login')

      // 检查空手机号：获取验证码按钮应为禁用状态
      const codeBtn = page.locator('button:has-text("获取验证码")')
      await expect(codeBtn).toBeDisabled()
      console.log('[PASS] 空手机号时获取验证码按钮禁用')
    })

    test('获取验证码按钮在输入有效手机号后可用', async ({ page }) => {
      await setupApiMocks(page)
      await page.goto('/login')

      // 初始禁用
      const codeBtn = page.locator('button:has-text("获取验证码")')
      await expect(codeBtn).toBeDisabled()

      // 输入有效手机号
      await page.locator('input[placeholder="请输入手机号"]').fill('13912345678')

      // 变为可用
      await expect(codeBtn).toBeEnabled()
      console.log('[PASS] 输入有效手机号后获取验证码按钮可用')
    })
  })

  // ============ 场景 2: 短信验证码登录成功 ============
  test.describe('短信验证码登录', () => {

    test('短信验证码登录成功并跳转到首页', async ({ page }) => {
      await setupApiMocks(page)
      await smsLogin(page, '13800001111', '123456')

      // 等待跳转到首页
      await page.waitForURL('**/home', { timeout: 15000 })

      // 验证首页可见
      await expect(page.locator('.hero-greeting')).toBeVisible({ timeout: 5000 })
      await expect(page.locator('.hero-greeting')).toContainText('李四')
      console.log('[PASS] 短信验证码登录成功，跳转到首页显示用户名')
    })

    test('短信验证码登录后统计数据正常展示', async ({ page }) => {
      await setupApiMocks(page)
      await smsLogin(page, '13800001111', '123456')

      await page.waitForURL('**/home', { timeout: 15000 })

      // 验证统计数据
      const statValues = page.locator('.hero-stat-value')
      await expect(statValues.nth(0)).toHaveText('5', { timeout: 5000 })
      await expect(statValues.nth(1)).toHaveText('3')
      await expect(statValues.nth(2)).toHaveText('7')
      console.log('[PASS] 登录后首页统计数据正确')
    })
  })

  // ============ 场景 3: 新用户注册登录 ============
  test.describe('首次注册登录', () => {

    test('新用户通过短信验证码注册并登录成功', async ({ page }) => {
      await setupApiMocks(page, {
        smsLoginResponse: MOCK_SMS_LOGIN_NEW_USER
      })
      await smsLogin(page, '13900009999', '654321')

      // 等待跳转到首页
      await page.waitForURL('**/home', { timeout: 15000 })

      // 新用户注册后首页显示 realName
      await expect(page.locator('.hero-greeting')).toBeVisible({ timeout: 5000 })
      await expect(page.locator('.hero-greeting')).toContainText('王五')
      console.log('[PASS] 新用户注册登录成功，首页显示正确用户名')
    })
  })

  // ============ 场景 4: 登录验证与错误处理 ============
  test.describe('登录验证与错误处理', () => {

    test('空手机号提交时显示错误提示', async ({ page }) => {
      await setupApiMocks(page)
      await page.goto('/login')

      // 清空手机号，只填验证码
      await page.locator('input[placeholder="请输入手机号"]').fill('')
      await page.locator('input[placeholder="请输入验证码"]').fill('123456')

      // 尝试提交
      await page.locator('button[type="submit"]').click()

      // Vant Form 会显示验证提示
      await expect(page.locator('.van-field__error-message')).toBeVisible({ timeout: 3000 })
      await expect(page.locator('.van-field__error-message')).toContainText('请输入手机号')
      console.log('[PASS] 空手机号提交时显示验证错误')
    })

    test('无效手机号格式提交时显示错误提示', async ({ page }) => {
      await setupApiMocks(page)
      await page.goto('/login')

      // 输入无效手机号
      await page.locator('input[placeholder="请输入手机号"]').fill('12345')
      await page.locator('input[placeholder="请输入验证码"]').fill('123456')

      // 尝试提交
      await page.locator('button[type="submit"]').click()

      // Vant Form 会显示格式错误提示
      await expect(page.locator('.van-field__error-message')).toBeVisible({ timeout: 3000 })
      await expect(page.locator('.van-field__error-message')).toContainText('手机号格式不正确')
      console.log('[PASS] 无效手机号格式提交时显示格式错误')
    })

    test('空验证码提交时显示错误提示', async ({ page }) => {
      await setupApiMocks(page)
      await page.goto('/login')

      // 填手机号，清空验证码
      await page.locator('input[placeholder="请输入手机号"]').fill('13800001111')
      await page.locator('input[placeholder="请输入验证码"]').fill('')

      // 尝试提交
      await page.locator('button[type="submit"]').click()

      // Vant Form 会显示验证提示
      await expect(page.locator('.van-field__error-message')).toBeVisible({ timeout: 3000 })
      await expect(page.locator('.van-field__error-message')).toContainText('请输入验证码')
      console.log('[PASS] 空验证码提交时显示验证错误')
    })

    test('验证码错误时登录失败', async ({ page }) => {
      await setupApiMocks(page, {
        smsLoginResponse: MOCK_SMS_LOGIN_FAIL
      })
      await page.goto('/login')

      await page.locator('input[placeholder="请输入手机号"]').fill('13800001111')
      await page.locator('input[placeholder="请输入验证码"]').fill('000000')

      // 监听 API 请求
      const responsePromise = page.waitForResponse(
        resp => resp.url().includes('/api/v1/auth/login/sms')
      )
      await page.locator('button[type="submit"]').click()
      const response = await responsePromise

      // 验证 API 返回错误
      const body = await response.json()
      expect(body.code).toBe(400)
      expect(body.message).toContain('验证码错误')
      console.log('[PASS] 验证码错误时 API 返回正确错误信息')
    })

    test('手机号不存在时登录失败', async ({ page }) => {
      await setupApiMocks(page, {
        smsLoginResponse: MOCK_SMS_LOGIN_NOT_FOUND
      })
      await page.goto('/login')

      await page.locator('input[placeholder="请输入手机号"]').fill('18800001111')
      await page.locator('input[placeholder="请输入验证码"]').fill('123456')

      const responsePromise = page.waitForResponse(
        resp => resp.url().includes('/api/v1/auth/login/sms')
      )
      await page.locator('button[type="submit"]').click()
      const response = await responsePromise

      const body = await response.json()
      expect(body.code).toBe(404)
      expect(body.message).toContain('手机号未注册')
      console.log('[PASS] 手机号不存在时 API 返回正确错误信息')
    })
  })

  // ============ 场景 5: 登录模式切换 ============
  test.describe('登录模式切换', () => {

    test('患者模式下可在短信登录和密码登录之间切换', async ({ page }) => {
      await page.goto('/login')

      // 验证默认是短信登录模式
      const smsBtn = page.locator('button.switch-btn:has-text("短信登录")')
      const pwdBtn = page.locator('button.switch-btn:has-text("密码登录")')
      await expect(smsBtn).toHaveClass(/active/)
      await expect(pwdBtn).not.toHaveClass(/active/)

      // 切换到密码登录
      await pwdBtn.click()
      await expect(pwdBtn).toHaveClass(/active/)
      await expect(smsBtn).not.toHaveClass(/active/)

      // 短信登录表单应隐藏，密码表单应出现
      await expect(page.locator('input[placeholder="请输入用户名"]')).toBeVisible()

      // 切换回短信登录
      await smsBtn.click()
      await expect(smsBtn).toHaveClass(/active/)

      // 手机号输入框应出现
      await expect(page.locator('input[placeholder="请输入手机号"]')).toBeVisible()
      console.log('[PASS] 短信登录与密码登录模式切换正常')
    })
  })

  // ============ 场景 6: 角色切换 ============
  test.describe('角色切换', () => {

    test('切换到员工登录后短信登录选项消失', async ({ page }) => {
      await page.goto('/login')

      // 患者模式下有短信登录切换
      await expect(page.locator('button:has-text("短信登录")')).toBeVisible()

      // 切换到员工登录
      await page.locator('button:has-text("员工登录")').click()

      // 员工模式下只有密码登录，没有短信登录选项
      await expect(page.locator('button:has-text("短信登录")')).not.toBeVisible()
      await expect(page.locator('input[placeholder="请输入用户名"]')).toBeVisible()
      console.log('[PASS] 切换到员工登录后短信登录选项正确隐藏')
    })

    test('患者和员工角色切换时表单内容清空', async ({ page }) => {
      await page.goto('/login')

      // 在患者短信模式下输入内容
      await page.locator('input[placeholder="请输入手机号"]').fill('13800001111')
      await page.locator('input[placeholder="请输入验证码"]').fill('123456')

      // 切换到员工登录
      await page.locator('button:has-text("员工登录")').click()

      // 再切回患者短信登录
      await page.locator('button:has-text("患者登录")').click()

      // 表单应被清空
      await expect(page.locator('input[placeholder="请输入手机号"]')).toHaveValue('')
      await expect(page.locator('input[placeholder="请输入验证码"]')).toHaveValue('')
      console.log('[PASS] 角色切换时表单内容正确清空')
    })
  })

  // ============ 场景 7: 登录后退出 ============
  test.describe('登录后退出', () => {

    test('短信登录成功后可正常退出登录', async ({ page }) => {
      await setupApiMocks(page)
      await smsLogin(page, '13800001111', '123456')
      await page.waitForURL('**/home', { timeout: 15000 })

      // 点击头像打开面板
      await page.locator('.hero-avatar--clickable').click()
      await page.locator('text=退出登录').click()

      // 确认弹窗
      await page.locator('.van-dialog').waitFor({ timeout: 3000 })
      await page.locator('.van-dialog__confirm, button:has-text("确定")').first().click()

      // 验证跳转到登录页
      await page.waitForURL('**/login', { timeout: 10000 })
      console.log('[PASS] 短信登录后退出登录成功')
    })
  })

  // ============ 场景 8: UI 元素验证 ============
  test.describe('登录页 UI 验证', () => {

    test('登录页所有关键元素展示正确', async ({ page }) => {
      await page.goto('/login')

      // 标题和品牌
      await expect(page.locator('h1.login-title')).toContainText('智慧健康')

      // 角色切换 Tab
      await expect(page.locator('button:has-text("患者登录")')).toBeVisible()
      await expect(page.locator('button:has-text("员工登录")')).toBeVisible()

      // 患者模式下默认短信登录
      await expect(page.locator('button.switch-btn:has-text("短信登录")')).toHaveClass(/active/)

      // 表单输入框
      await expect(page.locator('input[placeholder="请输入手机号"]')).toBeVisible()
      await expect(page.locator('input[placeholder="请输入验证码"]')).toBeVisible()

      // 获取验证码按钮
      await expect(page.locator('button:has-text("获取验证码")')).toBeVisible()

      // 登录按钮
      await expect(page.locator('button[type="submit"]')).toBeVisible()
      await expect(page.locator('button[type="submit"]')).toContainText('登录')

      // 第三方登录
      await expect(page.locator('text=其他登录方式')).toBeVisible()
      await expect(page.locator('text=微信')).toBeVisible()
      await expect(page.locator('text=支付宝')).toBeVisible()
      console.log('[PASS] 登录页所有关键元素展示正确')
    })
  })
})
