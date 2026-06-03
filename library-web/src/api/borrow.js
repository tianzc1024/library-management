import request from '@/utils/request'

const BASE = '/borrow'

export function borrowBook(data) {
  return request.post(`${BASE}/borrow`, data)
}

export function returnBook(recordId) {
  return request.post(`${BASE}/return/${recordId}`)
}

export function getBorrowList() {
  return request.get(`${BASE}/list`)
}

export function getReminders() {
  return request.get(`${BASE}/reminders`)
}

export function getBorrowStats() {
  return request.get(`${BASE}/stats`)
}
