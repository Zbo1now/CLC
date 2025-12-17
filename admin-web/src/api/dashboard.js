import http from '@/api/http'

export async function getDashboardOverview() {
  const resp = await http.get('/admin/dashboard/overview')
  const body = resp || {}
  if (body && body.success === false) {
    const msg = body.message || '获取仪表盘数据失败'
    const err = new Error(msg)
    err.response = resp
    throw err
  }
  return body.data || {}
}
