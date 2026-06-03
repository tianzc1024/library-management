import axios from 'axios'
import { Message } from 'element-ui'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截器 —— 自动在请求头中添加 JWT Token
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器 —— 统一处理 401 未登录
request.interceptors.response.use(
  response => {
    return response
  },
  error => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        const userType = localStorage.getItem('userType')
        localStorage.removeItem('token')
        localStorage.removeItem('username')
        localStorage.removeItem('realName')
        localStorage.removeItem('role')
        localStorage.removeItem('phone')
        localStorage.removeItem('name')
        localStorage.removeItem('userId')
        localStorage.removeItem('userType')
        if (userType === 'BORROWER') {
          router.push('/borrower/login')
        } else {
          router.push('/login')
        }
        Message.warning('登录已过期，请重新登录')
      } else {
        Message.error(error.response.data.message || '请求失败')
      }
    }
    return Promise.reject(error)
  }
)

export default request
