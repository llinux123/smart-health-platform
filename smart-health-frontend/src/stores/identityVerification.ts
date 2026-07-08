import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { bindIdentity } from '@/api/auth'
import type { ProfileData } from '@/api/auth'
import { useUserStore } from './user'

/** 页面1 暂存的实名信息 */
export interface Step1Data {
  realName: string
  idCard: string
  gender: number
  consentAccepted: boolean
}

/** 页面2 采集的身份证照片数据 */
export interface Step2Data {
  idCardFrontUrl: string  // 身份证正面(base64)
  idCardBackUrl: string   // 身份证反面(base64)
}

/**
 * 实名认证两步流程的 Pinia Store
 *
 * 仅内存暂存，不写入 localStorage / sessionStorage。
 * 刷新页面后数据丢失，自动回退到页面1（合规设计）。
 */
export const useIdentityVerificationStore = defineStore('identityVerification', () => {
  // —— Step 1 暂存数据（仅内存，不持久化）——
  const step1Data = ref<Step1Data | null>(null)
  const step2Data = ref<Step2Data>({
    idCardFrontUrl: '',
    idCardBackUrl: ''
  })

  /** 是否满足提交条件 */
  const canSubmit = computed(() => {
    if (!step1Data.value) return false
    return !!step2Data.value.idCardFrontUrl && !!step2Data.value.idCardBackUrl
  })

  /** 页面1 → 暂存数据 */
  function setStep1Data(data: Step1Data): void {
    step1Data.value = data
  }

  /** 页面2 → 读取页面1数据 */
  function getStep1Data(): Step1Data | null {
    return step1Data.value
  }

  /** 页面2 → 更新身份证上传结果 */
  function setIdCardResult(frontUrl: string, backUrl: string): void {
    step2Data.value.idCardFrontUrl = frontUrl
    step2Data.value.idCardBackUrl = backUrl
  }

  /** 页面2 → 组装完整 payload 并提交到后端（保持原有单次提交 API） */
  async function submitIdentity(skipVerification = false): Promise<ProfileData> {
    if (!step1Data.value) {
      throw new Error('身份信息缺失，请返回上一步重新填写')
    }
    const s1 = step1Data.value
    const s2 = step2Data.value

    const data = await bindIdentity({
      realName: s1.realName,
      idCard: s1.idCard,
      gender: s1.gender,
      idCardFrontUrl: s2.idCardFrontUrl,
      idCardBackUrl: s2.idCardBackUrl,
      skipVerification
    })

    // 同步到 userStore
    const userStore = useUserStore()
    userStore.setProfile(data)
    // 清理暂存
    clearAll()
    return data
  }

  /** 清理所有暂存数据 */
  function clearAll(): void {
    step1Data.value = null
    step2Data.value = { idCardFrontUrl: '', idCardBackUrl: '' }
  }

  return {
    step1Data,
    step2Data,
    canSubmit,
    setStep1Data,
    getStep1Data,
    setIdCardResult,
    submitIdentity,
    clearAll
  }
})
