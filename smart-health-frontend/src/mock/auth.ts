export function mockLogin(data: { username: string; loginType?: string }): Promise<{
  token: string; userId: number; username: string; realName: string; role: string; patientId: number | null; doctorId: number | null
}> {
  const loginType = data.loginType || 'PATIENT'
  if (loginType === 'STAFF') {
    return Promise.resolve({
      token: 'mock-jwt-token-' + Date.now(),
      userId: 1,
      username: data.username,
      realName: '管理员',
      role: 'ADMIN',
      patientId: null,
      doctorId: null
    })
  }
  return Promise.resolve({
    token: 'mock-jwt-token-' + Date.now(),
    userId: 100,
    username: data.username,
    realName: '张三',
    role: 'PATIENT',
    patientId: 100,
    doctorId: null
  })
}

export function mockRegister(_data: any): Promise<null> {
  return Promise.resolve(null)
}
