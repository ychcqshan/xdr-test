import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
    baseURL: '/api/v1',
    timeout: 10000,
})

// 请求拦截器
request.interceptors.request.use((config) => {
    const token = localStorage.getItem('accessToken')
    if (token) {
        config.headers.Authorization = `Bearer ${token}`
    }
    return config
})

// 响应拦截器
request.interceptors.response.use(
    (response) => {
        const res = response.data
        if (res.code !== 200) {
            ElMessage.error(res.message || '请求失败')
            return Promise.reject(new Error(res.message))
        }
        return res
    },
    (error) => {
        const isLoginAPI = error.config?.url?.includes('/auth/login')
        if (error.response?.status === 401 && !isLoginAPI) {
            localStorage.clear()
            router.push('/login')
            ElMessage.error('登录已过期，请重新登录')
        } else if (!isLoginAPI) {
            ElMessage.error(error.response?.data?.message || '网络错误')
        }
        return Promise.reject(error)
    }
)

export default request
