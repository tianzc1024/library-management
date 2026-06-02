import request from '@/utils/request'

const BASE = '/user'

export function getUserList() {
  return request.get(`${BASE}/list`)
}

export function getUserById(id) {
  return request.get(`${BASE}/${id}`)
}

export function addUser(data) {
  return request.post(`${BASE}/add`, data)
}

export function updateUser(data) {
  return request.put(`${BASE}/update`, data)
}

export function deleteUser(id) {
  return request.delete(`${BASE}/delete/${id}`)
}
