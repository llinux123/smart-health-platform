import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  getToken, setToken, removeToken,
  getUserId, setUserId,
  getUsername, setUsername,
  getRealName, setRealName as setStoredRealName,
  getRole, setRole,
  getDoctorId, setDoctorId,
  clearAuth
} from '@/utils/storage'
import type { ProfileData } from '@/api/auth'

export interface LoginInfo {
  token: string
  userId: number
  username: string
  realName: string
  role: string
  patientId?: number | null
  doctorId?: number | null
  isNewUser?: boolean
  randomPassword?: string
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken() || '')
  const userId = ref<number | null>(getUserId())
  const username = ref<string>(getUsername() || '')
  const realName = ref<string>(getRealName() || '')
  const role = ref<string>(getRole() || '')
  const doctorId = ref<number | null>(getDoctorId())
  const profile = ref<Partial<ProfileData> | null>(null)
  
  const isLoggedIn = computed<boolean>(() => !!token.value)
  const isPatient = computed(() => role.value === 'PATIENT')
  const isDoctor = computed(() => role.value === 'DOCTOR')
  const isPharmacist = computed(() => role.value === 'PHARMACIST')
  const isAdmin = computed(() => role.value === 'ADMIN')
  const isStaff = computed(() => ['DOCTOR', 'PHARMACIST', 'ADMIN'].includes(role.value))
  
  /** 资料是否已完善（有真实姓名即视为已完善） */
  const isProfileComplete = computed(() => !!realName.value && realName.value.length > 0 && !realName.value.includes('****'))

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

  /** 更新真实姓名（身份绑定后使用） */
  function setRealName(name: string): void {
    realName.value = name
    setStoredRealName(name)
  }

  function setProfile(data: ProfileData): void {
    profile.value = data
    if (data.username && data.username !== username.value) {
      username.value = data.username
      setUsername(data.username)
    }
    if (data.realName && data.realName !== realName.value) {
      realName.value = data.realName
      setStoredRealName(data.realName)
    }
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
    profile,
    patientId,
    isLoggedIn,
    isPatient,
    isDoctor,
    isPharmacist,
    isAdmin,
    isStaff,
    isProfileComplete,
    setLoginInfo,
    setRealName,
    setProfile,
    logout
  }
})
