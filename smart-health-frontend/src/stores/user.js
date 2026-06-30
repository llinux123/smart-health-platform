import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  getToken, setToken, removeToken,
  getPatientId, setPatientId,
  getUsername, setUsername,
  getRealName, setRealName,
  clearAuth
} from '@/utils/storage'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken() || '')
  const patientId = ref(getPatientId())
  const username = ref(getUsername() || '')
  const realName = ref(getRealName() || '')

  const isLoggedIn = computed(() => !!token.value)

  function setLoginInfo(data) {
    token.value = data.token
    patientId.value = data.patientId
    username.value = data.username
    realName.value = data.realName

    setToken(data.token)
    setPatientId(data.patientId)
    setUsername(data.username)
    setRealName(data.realName)
  }

  function logout() {
    token.value = ''
    patientId.value = null
    username.value = ''
    realName.value = ''
    clearAuth()
  }

  return {
    token,
    patientId,
    username,
    realName,
    isLoggedIn,
    setLoginInfo,
    logout
  }
})
