import request from '@/utils/request'

export function borrowerLogin(phone, password) {
  return request({
    url: '/auth/borrower/login',
    method: 'post',
    data: { phone, password }
  })
}

export function getMyRecords() {
  return request({
    url: '/borrower/my-records',
    method: 'get'
  })
}
