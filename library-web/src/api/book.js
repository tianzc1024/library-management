import request from '@/utils/request'

const BASE = '/book'

export function getBookList() {
  return request.get(`${BASE}/list`)
}

export function getBookById(id) {
  return request.get(`${BASE}/${id}`)
}

export function addBook(data) {
  return request.post(`${BASE}/add`, data)
}

export function updateBook(data) {
  return request.put(`${BASE}/update`, data)
}

export function deleteBook(id) {
  return request.delete(`${BASE}/delete/${id}`)
}
