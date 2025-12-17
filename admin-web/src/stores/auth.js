import { defineStore } from 'pinia'
import http, { clearAdminSessionId, getAdminSessionId, setAdminSessionId } from '@/api/http'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    sessionId: getAdminSessionId(),
    username: '',
    displayName: '',
    hydrated: false
  }),
  getters: {
    isAuthed(state) {
      return !!state.sessionId
    }
  },
  actions: {
    async login({ username, password }) {
      const resp = await http.post('/admin/auth/login', { username, password })
      const data = resp?.data?.data || {}

      this.sessionId = data.sessionId || ''
      setAdminSessionId(this.sessionId)

      this.username = data.username || username || ''
      this.displayName = data.displayName || ''
      this.hydrated = true

      return data
    },

    async fetchMe() {
      if (!this.sessionId) {
        throw new Error('no session')
      }
      const resp = await http.get('/admin/auth/me')
      const data = resp?.data?.data || {}
      this.username = data.username || ''
      this.displayName = data.displayName || ''
      this.hydrated = true
      return data
    },

    async logout() {
      try {
        await http.post('/admin/auth/logout')
      } finally {
        this.sessionId = ''
        this.username = ''
        this.displayName = ''
        this.hydrated = true
        clearAdminSessionId()
      }
    },

    clear() {
      this.sessionId = ''
      this.username = ''
      this.displayName = ''
      this.hydrated = true
      clearAdminSessionId()
    }
  }
})
