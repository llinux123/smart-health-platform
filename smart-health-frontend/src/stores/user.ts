import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  getToken, setToken, removeToken,
  getUserId, setUserId,
  getUsername, setUsername,
  getRealName, setRealName,
  getRole, setRole,
  getDoctorId, setDoctorId,
  clearAuth
} from '@/utils/storage'

export interface LoginInfo {
  token: string
  userId: number
  username: string
  realName: string
  role: string
  patientId?: number | null
  doctorId?: number | null
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken() || '')
  const userId = ref<number | null>(getUserId())
  const username = ref<string>(getUsername() || '')
  const realName = ref<string>(getRealName() || '')
  const role = ref<string>(getRole() || '')
  const doctorId = ref<number | null>(getDoctorId())

  const isLoggedIn = computed<boolean>(() => !!token.value)
  const isPatient = computed(() => role.value === 'PATIENT')
  const isDoctor = computed(() => role.value === 'DOCTOR')
  const isPharmacist = computed(() => role.value === 'PHARMACIST')
  const isAdmin = computed(() => role.value === 'ADMIN')
  const isStaff = computed(() => ['DOCTOR', 'PHARMACIST', 'ADMIN'].includes(role.value))

  /** 兼容旧代码：返回患者 ID（仅当角色为 PATIENT 时） */
  const patientId = computed<number | null>(() => {
    return role.value === 'PATIENT' ? userId.value : null
  })

  function setLoginInfo(data: LoginInfo): void {
    token.value = data.token
    userId.value = data.userId
    username.value = data.username
    realName.value = data.realName
    role.value = data.role
    doctorId.value = data.doctorId ?? null

    setToken(data.token)
    setUserId(data.userId)
    setUsername(data.username)
    setRealName(data.realName)
    setRole(data.role)
    setDoctorId(data.doctorId ?? null)
  }

  function logout(): void {
    token.value = ''
    userId.value = null
    username.value = ''
    realName.value = ''
    role.value = ''
    doctorId.value = null
    clearAuth()
  }

  return {
    token,
    userId,
    username,
    realName,
    role,
    doctorId,
    patientId,
    isLoggedIn,
    isPatient,
    isDoctor,
    isPharmacist,
    isAdmin,
    isStaff,
    setLoginInfo,
    logout
  }
})
