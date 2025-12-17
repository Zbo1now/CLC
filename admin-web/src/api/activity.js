import http from './http'

/**
 * 分页查询活动列表
 */
export function getActivityList(params) {
  return http.get('/admin/activities', { params })
}

/**
 * 获取活动详情
 */
export function getActivityDetail(id) {
  return http.get(`/admin/activities/${id}`)
}

/**
 * 创建活动
 */
export function createActivity(data) {
  return http.post('/admin/activities', data)
}

/**
 * 更新活动
 */
export function updateActivity(id, data) {
  return http.put(`/admin/activities/${id}`, data)
}

/**
 * 更新活动状态
 */
export function updateActivityStatus(id, status) {
  return http.put(`/admin/activities/${id}/status`, null, {
    params: { status }
  })
}

/**
 * 删除活动
 */
export function deleteActivity(id) {
  return http.delete(`/admin/activities/${id}`)
}

/**
 * 分页查询参与记录列表
 */
export function getParticipationList(params) {
  return http.get('/admin/participations', { params })
}

/**
 * 获取参与记录详情
 */
export function getParticipationDetail(id) {
  return http.get(`/admin/participations/${id}`)
}

/**
 * 审核通过
 */
export function approveParticipation(id) {
  return http.post(`/admin/participations/${id}/approve`)
}

/**
 * 审核驳回
 */
export function rejectParticipation(id, rejectReason) {
  return http.post(`/admin/participations/${id}/reject`, { rejectReason })
}

/**
 * 批量审核通过
 */
export function batchApproveParticipation(ids) {
  return http.post('/admin/participations/batch-approve', { ids })
}

/**
 * 标记已完成
 */
export function completeParticipation(id, completionNotes) {
  return http.post(`/admin/participations/${id}/complete`, { completionNotes })
}

/**
 * 批量标记已完成
 */
export function batchCompleteParticipation(ids, completionNotes) {
  return http.post('/admin/participations/batch-complete', { ids, completionNotes })
}
