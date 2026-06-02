import request from '@/utils/request'

export function login(username, password) {
  return request.post('/auth/login', { username, password })
}

export function getCurrentUser() {
  return request.get('/auth/current-user')
}

export function logout() {
  return request.post('/auth/logout')
}
