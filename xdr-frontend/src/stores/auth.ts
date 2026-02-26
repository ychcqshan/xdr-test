import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, logout as logoutApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
    const token = ref(localStorage.getItem('accessToken') || '')
    const refreshToken = ref(localStorage.getItem('refreshToken') || '')
    const username = ref(localStorage.getItem('username') || '')
    const role = ref(localStorage.getItem('role') || '')

    async function login(user: string, password: string) {
        const res = await loginApi(user, password)
        const data = res.data
        token.value = data.accessToken
        refreshToken.value = data.refreshToken
        username.value = data.username
        role.value = data.role
        localStorage.setItem('accessToken', data.accessToken)
        localStorage.setItem('refreshToken', data.refreshToken)
        localStorage.setItem('username', data.username)
        localStorage.setItem('role', data.role)
    }

    async function logout() {
        try {
            await logoutApi()
        } finally {
            token.value = ''
            refreshToken.value = ''
            username.value = ''
            role.value = ''
            localStorage.clear()
        }
    }

    return { token, refreshToken, username, role, login, logout }
})
