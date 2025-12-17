import http from './http'

/**
 * 获取所有系统配置（按分类分组）
 */
export function getSystemConfigs() {
  return http.get('/admin/system-configs')
}

/**
 * 根据分类获取配置
 */
export function getConfigsByCategory(category) {
  return http.get(`/admin/system-configs/category/${category}`)
}

/**
 * 根据配置键获取配置
 */
export function getConfigByKey(configKey) {
  return http.get(`/admin/system-configs/${configKey}`)
}

/**
 * 更新单个配置
 */
export function updateConfig(id, configValue) {
  return http.put(`/admin/system-configs/${id}`, {
    configValue
  })
}

/**
 * 批量更新配置
 */
export function batchUpdateConfigs(configs) {
  return http.post('/admin/system-configs/batch', configs)
}

/**
 * 刷新配置缓存
 */
export function refreshCache() {
  return http.post('/admin/system-configs/refresh')
}

/**
 * 创建配置
 */
export function createConfig(config) {
  return http.post('/admin/system-configs', config)
}

/**
 * 删除配置
 */
export function deleteConfig(id) {
  return http.delete(`/admin/system-configs/${id}`)
}
