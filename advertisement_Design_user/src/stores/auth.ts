import { defineStore } from 'pinia'
import { fetchLogin, fetchLoginByEmailCode, fetchLogout, fetchMe, updateMe } from '@/api'
import type { LoginResponse, UpdateUserRequest, UserVO } from '@/models'

const TOKEN_KEY = 'ad-user-token'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: null as UserVO | null,
    bootstrapped: false
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    role: (state) => state.user?.role || null
  },
  actions: {
    setSession(payload: LoginResponse) {
      this.token = payload.token
      this.user = payload.user
      localStorage.setItem(TOKEN_KEY, payload.token)
    },
    clearSession() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
    },
    async bootstrap() {
      if (this.bootstrapped) {
        return
      }
      this.bootstrapped = true
      if (!this.token) {
        return
      }
      try {
        this.user = await fetchMe()
      } catch {
        this.clearSession()
      }
    },
    async login(email: string, password: string) {
      const payload = await fetchLogin(email, password)
      this.setSession(payload)
    },
    async loginByEmailCode(email: string, code: string) {
      const payload = await fetchLoginByEmailCode(email, code)
      this.setSession(payload)
    },
    async logout() {
      try {
        await fetchLogout()
      } finally {
        this.clearSession()
      }
    },
    async updateProfile(payload: UpdateUserRequest) {
      this.user = await updateMe(payload)
      return this.user
    }
  }
})
