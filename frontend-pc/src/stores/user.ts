import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export interface UserInfo {
  accountId: number
  loginName: string
  mobile: string
  accountKind: number
  roles: string[]
  permissions: string[]
  merchantId?: number
  corpName?: string
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('soarcloud_token') || '')
  const userInfo = ref<UserInfo | null>(
    localStorage.getItem('soarcloud_user')
      ? JSON.parse(localStorage.getItem('soarcloud_user')!)
      : null
  )

  const setLoginSession = (newToken: string, user: UserInfo) => {
    token.value = newToken
    userInfo.value = user
    localStorage.setItem('soarcloud_token', newToken)
    localStorage.setItem('soarcloud_user', JSON.stringify(user))
  }

  const logout = async () => {
    try {
      await request.post('/api/v1/auth/logout')
    } catch (e) {
      // 忽略登出请求网络报错
    } finally {
      token.value = ''
      userInfo.value = null
      localStorage.removeItem('soarcloud_token')
      localStorage.removeItem('soarcloud_user')
    }
  }

  const fetchCurrentUser = async () => {
    const data = await request.get<any, UserInfo>('/api/v1/auth/current-user')
    userInfo.value = data
    localStorage.setItem('soarcloud_user', JSON.stringify(data))
    return data
  }

  return {
    token,
    userInfo,
    setLoginSession,
    logout,
    fetchCurrentUser
  }
})
