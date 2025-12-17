import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layout/MainLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '仪表盘', icon: 'Odometer' }
      },
      {
        path: 'team',
        name: 'Team',
        component: () => import('@/views/team/index.vue'),
        meta: { title: '团队管理', icon: 'UserFilled' }
      },
      {
        path: 'team/:id',
        name: 'TeamDetail',
        component: () => import('@/views/team/detail.vue'),
        meta: { title: '团队详情', hidden: true }
      },
      {
        path: 'transaction',
        name: 'Transaction',
        component: () => import('@/views/transaction/index.vue'),
        meta: { title: '虚拟币流水', icon: 'Money' }
      },
      {
        path: 'audit',
        name: 'Audit',
        component: () => import('@/views/audit/index.vue'),
        meta: { title: '成果审核', icon: 'Stamp' }
      },
      {
        path: 'resource',
        name: 'Resource',
        component: () => import('@/views/resource/index.vue'),
        meta: { title: '资源管理', icon: 'Box' }
      },
      {
        path: 'activity',
        name: 'Activity',
        component: () => import('@/views/activity/index.vue'),
        meta: { title: '活动管理', icon: 'Calendar' }
      },
      {
        path: 'config',
        name: 'Config',
        component: () => import('@/views/config/index.vue'),
        meta: { title: '系统配置', icon: 'Setting' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  const isLogin = to.path === '/login'

  console.log('[路由守卫] 目标路径:', to.path, '认证状态:', auth.isAuthed, 'hydrated:', auth.hydrated, 'sessionId:', auth.sessionId)

  if (isLogin) {
    if (auth.isAuthed) {
      return { path: '/dashboard' }
    }
    return true
  }

  if (!auth.isAuthed) {
    console.log('[路由守卫] 未登录，跳转登录页')
    ElMessage.warning('请先登录')
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 每次路由切换时验证 session 是否有效
  if (!auth.hydrated) {
    console.log('[路由守卫] 验证 session 有效性...')
    try {
      await auth.fetchMe()
      console.log('[路由守卫] session 有效')
    } catch (e) {
      console.error('[路由守卫] session 无效:', e)
      auth.clear()
      ElMessage.warning('登录已过期，请重新登录')
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }

  return true
})

export default router
