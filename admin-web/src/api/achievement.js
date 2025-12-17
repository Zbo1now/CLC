import http from './http'

/**
 * 成果审核 API
 */

/**
 * 分页查询成果列表（带筛选）
 */
export function getAchievementList(params) {
  return http.get('/admin/achievements', { params })
}

/**
 * 获取成果详情
 */
export function getAchievementDetail(id) {
  return http.get(`/admin/achievements/${id}`)
}

/**
 * 审核通过成果（自动发币）
 */
export function approveAchievement(id, rewardCoins) {
  return http.post(`/admin/achievements/${id}/approve`, { rewardCoins })
}

/**
 * 审核驳回成果
 */
export function rejectAchievement(id, rejectReason) {
  return http.post(`/admin/achievements/${id}/reject`, { rejectReason })
}
