import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layout/MainLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { ElMessageBox } from 'element-plus'

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

  if (isLogin) {
    if (auth.isAuthed) {
      return { path: '/dashboard' }
    }
    return true
  }

  if (!auth.isAuthed) {
    try {
      await ElMessageBox.confirm('当前未登录，是否前往登录页面？', '提示', {
        confirmButtonText: '去登录',
        cancelButtonText: '取消',
        type: 'warning'
      })
      return { path: '/login', query: { redirect: to.fullPath } }
    } catch (e) {
      return false
    }
  }

  if (!auth.hydrated) {
    try {
      await auth.fetchMe()
    } catch (e) {
      auth.clear()
      try {
        await ElMessageBox.confirm('登录已失效，是否前往登录页面重新登录？', '提示', {
          confirmButtonText: '去登录',
          cancelButtonText: '取消',
          type: 'warning'
        })
        return { path: '/login', query: { redirect: to.fullPath } }
      } catch (e2) {
        return false
      }
    }
  }

  return true
})

export default router
