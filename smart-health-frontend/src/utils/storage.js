const TOKEN_KEY = 'smart_health_token'
const PATIENT_ID_KEY = 'smart_health_patient_id'
const USERNAME_KEY = 'smart_health_username'
const REAL_NAME_KEY = 'smart_health_real_name'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export function getPatientId() {
  const id = localStorage.getItem(PATIENT_ID_KEY)
  return id ? Number(id) : null
}

export function setPatientId(id) {
  localStorage.setItem(PATIENT_ID_KEY, String(id))
}

export function getUsername() {
  return localStorage.getItem(USERNAME_KEY)
}

export function setUsername(name) {
  localStorage.setItem(USERNAME_KEY, name)
}

export function getRealName() {
  return localStorage.getItem(REAL_NAME_KEY)
}

export function setRealName(name) {
  localStorage.setItem(REAL_NAME_KEY, name)
}

export function clearAuth() {
  removeToken()
  localStorage.removeItem(PATIENT_ID_KEY)
  localStorage.removeItem(USERNAME_KEY)
  localStorage.removeItem(REAL_NAME_KEY)
}
