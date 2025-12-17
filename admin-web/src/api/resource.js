import http from './http'

/**
 * 资源管理 API
 * 支持四类资源：workstation（工位）、device（设备）、equipment（器材）、venue（场地）
 */

/**
 * 获取资源统计数据
 */
export function getResourceStats() {
  return http.get('/admin/resources/stats')
}

/**
 * 分页查询资源列表
 * @param {String} type - 资源类型：workstation/device/equipment/venue
 * @param {Object} params - 查询参数：{ status, keyword, page, pageSize }
 */
export function getResourceList(type, params) {
  return http.get(`/admin/resources/${type}`, { params })
}

/**
 * 获取资源详情
 * @param {String} type - 资源类型
 * @param {Number} id - 资源ID
 */
export function getResourceDetail(type, id) {
  return http.get(`/admin/resources/${type}/${id}`)
}

/**
 * 创建资源
 * @param {String} type - 资源类型
 * @param {Object} data - 资源数据
 */
export function createResource(type, data) {
  return http.post(`/admin/resources/${type}`, data)
}

/**
 * 更新资源
 * @param {String} type - 资源类型
 * @param {Number} id - 资源ID
 * @param {Object} data - 更新数据
 */
export function updateResource(type, id, data) {
  return http.put(`/admin/resources/${type}/${id}`, data)
}

/**
 * 删除资源
 * @param {String} type - 资源类型
 * @param {Number} id - 资源ID
 */
export function deleteResource(type, id) {
  return http.delete(`/admin/resources/${type}/${id}`)
}

/**
 * 更新资源状态
 * @param {String} type - 资源类型
 * @param {Number} id - 资源ID
 * @param {String} status - 新状态
 */
export function updateResourceStatus(type, id, status) {
  return http.patch(`/admin/resources/${type}/${id}/status`, { status })
}

/**
 * 获取资源使用历史
 * @param {String} type - 资源类型
 * @param {Number} id - 资源ID
 * @param {Object} params - 分页参数：{ page, pageSize }
 */
export function getResourceHistory(type, id, params) {
  return http.get(`/admin/resources/${type}/${id}/history`, { params })
}
