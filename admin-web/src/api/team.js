import http from './http'

/**
 * 团队管理 API
 */

/**
 * 分页查询团队列表
 */
export function getTeamList(params) {
  return http.get('/admin/teams', { params })
}

/**
 * 获取团队详情
 */
export function getTeamDetail(id) {
  return http.get(`/admin/teams/${id}`)
}

/**
 * 获取团队行为时间线
 */
export function getTeamTimeline(id) {
  return http.get(`/admin/teams/${id}/timeline`)
}

/**
 * 重置团队密码
 */
export function resetTeamPassword(id, data) {
  return http.post(`/admin/teams/${id}/reset-password`, data)
}

/**
 * 手动调整虚拟币
 */
export function adjustTeamBalance(id, data) {
  return http.post(`/admin/teams/${id}/adjust-balance`, data)
}
