/**
 * 认证模块 Mock 数据
 */
export function mockLogin(data) {
  return Promise.resolve({
    token: 'mock-jwt-token-' + Date.now(),
    patientId: 100,
    username: data.username,
    realName: '张三'
  })
}

export function mockRegister(data) {
  return Promise.resolve(null)
}
