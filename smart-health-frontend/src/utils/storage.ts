const TOKEN_KEY = 'smart_health_token'
const USER_ID_KEY = 'smart_health_user_id'
const USERNAME_KEY = 'smart_health_username'
const REAL_NAME_KEY = 'smart_health_real_name'
const ROLE_KEY = 'smart_health_role'
const DOCTOR_ID_KEY = 'smart_health_doctor_id'
const REMEMBER_KEY = 'smart_health_remember'

const KEYS = [TOKEN_KEY, USER_ID_KEY, USERNAME_KEY, REAL_NAME_KEY, ROLE_KEY, DOCTOR_ID_KEY]

/** 是否勾选了「记住我」 */
function isRemembered(): boolean {
  return localStorage.getItem(REMEMBER_KEY) === 'true'
}

/**
 * 双模式读取：sessionStorage 优先（当前会话），localStorage 兜底（跨会话持久）
 */
export function getToken(): string | null {
  return sessionStorage.getItem(TOKEN_KEY) || localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  sessionStorage.setItem(TOKEN_KEY, token)
  if (isRemembered()) {
    localStorage.setItem(TOKEN_KEY, token)
  }
}

export function removeToken(): void {
  sessionStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(TOKEN_KEY)
}

export function getUserId(): number | null {
  const id = sessionStorage.getItem(USER_ID_KEY) || localStorage.getItem(USER_ID_KEY)
  return id ? Number(id) : null
}

export function setUserId(id: number): void {
  sessionStorage.setItem(USER_ID_KEY, String(id))
  if (isRemembered()) {
    localStorage.setItem(USER_ID_KEY, String(id))
  }
}

export function getUsername(): string | null {
  return sessionStorage.getItem(USERNAME_KEY) || localStorage.getItem(USERNAME_KEY)
}

export function setUsername(name: string): void {
  sessionStorage.setItem(USERNAME_KEY, name)
  if (isRemembered()) {
    localStorage.setItem(USERNAME_KEY, name)
  }
}

export function getRealName(): string | null {
  return sessionStorage.getItem(REAL_NAME_KEY) || localStorage.getItem(REAL_NAME_KEY)
}

export function setRealName(name: string): void {
  sessionStorage.setItem(REAL_NAME_KEY, name)
  if (isRemembered()) {
    localStorage.setItem(REAL_NAME_KEY, name)
  }
}

export function getRole(): string | null {
  return sessionStorage.getItem(ROLE_KEY) || localStorage.getItem(ROLE_KEY)
}

export function setRole(role: string): void {
  sessionStorage.setItem(ROLE_KEY, role)
  if (isRemembered()) {
    localStorage.setItem(ROLE_KEY, role)
  }
}

export function getDoctorId(): number | null {
  const id = sessionStorage.getItem(DOCTOR_ID_KEY) || localStorage.getItem(DOCTOR_ID_KEY)
  return id ? Number(id) : null
}

export function setDoctorId(id: number | null): void {
  if (id != null) {
    sessionStorage.setItem(DOCTOR_ID_KEY, String(id))
    if (isRemembered()) {
      localStorage.setItem(DOCTOR_ID_KEY, String(id))
    }
  }
}

export function clearAuth(): void {
  KEYS.forEach(key => {
    sessionStorage.removeItem(key)
    localStorage.removeItem(key)
  })
  localStorage.removeItem(REMEMBER_KEY)
}

export function getRememberMe(): boolean {
  return isRemembered()
}

export function setRememberMe(val: boolean): void {
  localStorage.setItem(REMEMBER_KEY, String(!!val))
  if (!val) {
    // 取消记住我时，清除 localStorage 中的持久数据，sessionStorage 保留当前会话
    KEYS.forEach(key => localStorage.removeItem(key))
  }
}
