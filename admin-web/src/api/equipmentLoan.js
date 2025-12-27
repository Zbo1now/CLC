import http from './http'

export function listEquipmentLoans(params) {
  return http.get('/admin/equipments/loans', { params })
}

export function approveEquipmentLoan(id) {
  return http.post(`/admin/equipments/loans/${id}/approved`)
}

export function rejectEquipmentLoan(id) {
  return http.post(`/admin/equipments/loans/${id}/rejected`)
}

export function markEquipmentLoanBorrowed(id) {
  return http.post(`/admin/equipments/loans/${id}/borrowed`)
}

export function markEquipmentLoanReturned(id) {
  return http.post(`/admin/equipments/loans/${id}/returned`)
}


