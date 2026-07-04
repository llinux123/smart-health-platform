import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { getToken, getRole } from '@/utils/storage'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginPage.vue'),
    meta: { requiresAuth: false, hideTab: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/login/RegisterPage.vue'),
    meta: { requiresAuth: false, hideTab: true }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/home/HomePage.vue'),
    meta: { requiresAuth: true }
  },
  // AI 问诊
  {
    path: '/consultation',
    name: 'ConsultationSessions',
    component: () => import('@/views/consultation/SessionListPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/consultation/upload',
    name: 'ConsultationUpload',
    component: () => import('@/views/consultation/UploadPage.vue'),
    meta: { requiresAuth: true, hideTab: true }
  },
  {
    path: '/consultation/analysis',
    name: 'ConsultationAnalysis',
    component: () => import('@/views/consultation/AnalysisPage.vue'),
    meta: { requiresAuth: true, hideTab: true }
  },
  {
    path: '/consultation/chat/:sessionSn',
    name: 'ConsultationChat',
    component: () => import('@/views/consultation/ChatPage.vue'),
    meta: { requiresAuth: true, hideTab: true },
    props: true
  },
  {
    path: '/consultation/recycle-bin',
    name: 'ConsultationRecycleBin',
    component: () => import('@/views/consultation/RecycleBinPage.vue'),
    meta: { requiresAuth: true, hideTab: true }
  },
  // 挂号预约
  {
    path: '/registration/schedules',
    name: 'ScheduleList',
    component: () => import('@/views/registration/ScheduleListPage.vue'),
    meta: { requiresAuth: true, hideTab: true }
  },
  {
    path: '/registration/doctor/:id',
    name: 'DoctorDetail',
    component: () => import('@/views/registration/DoctorDetailPage.vue'),
    meta: { requiresAuth: true, hideTab: true },
    props: true
  },
  {
    path: '/registration/seckill/:scheduleId',
    name: 'Seckill',
    component: () => import('@/views/registration/SeckillPage.vue'),
    meta: { requiresAuth: true, hideTab: true },
    props: true
  },
  {
    path: '/registration/orders',
    name: 'OrderList',
    component: () => import('@/views/registration/OrderListPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/registration/orders/:orderSn',
    name: 'OrderDetail',
    component: () => import('@/views/registration/OrderDetailPage.vue'),
    meta: { requiresAuth: true, hideTab: true },
    props: true
  },
  // 处方
  {
    path: '/prescriptions',
    name: 'PrescriptionList',
    component: () => import('@/views/prescription/PrescriptionListPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/prescriptions/:id',
    name: 'PrescriptionDetail',
    component: () => import('@/views/prescription/PrescriptionDetailPage.vue'),
    meta: { requiresAuth: true, hideTab: true },
    props: true
  },
  // 管理后台 — 角色限制
  {
    path: '/admin/schedule',
    name: 'AdminSchedule',
    component: () => import('@/views/admin/ScheduleManagePage.vue'),
    meta: { requiresAuth: true, hideTab: true, roles: ['ADMIN'] }
  },
  {
    path: '/admin/prescription/review',
    name: 'AdminPrescriptionReview',
    component: () => import('@/views/admin/PrescriptionReviewPage.vue'),
    meta: { requiresAuth: true, hideTab: true, roles: ['ADMIN', 'PHARMACIST'] }
  },
  {
    path: '/admin/prescription/issue',
    name: 'AdminPrescriptionIssue',
    component: () => import('@/views/admin/IssuePrescriptionPage.vue'),
    meta: { requiresAuth: true, hideTab: true, roles: ['ADMIN', 'DOCTOR'] }
  },
  {
    path: '/admin/inventory',
    name: 'AdminInventory',
    component: () => import('@/views/admin/InventoryManagePage.vue'),
    meta: { requiresAuth: true, hideTab: true, roles: ['ADMIN'] }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// 导航守卫
const whiteList = ['/login', '/register']

router.beforeEach((to, from, next) => {
  const token = getToken()
  if (whiteList.includes(to.path) || !to.meta.requiresAuth) {
    if (token && (to.path === '/login' || to.path === '/register')) {
      next('/home')
    } else {
      next()
    }
  } else {
    if (!token) {
      const loginQuery: Record<string, string> = { redirect: to.path }
      if (Object.keys(to.query).length > 0) {
        loginQuery._rq = JSON.stringify(to.query)
      }
      next({ path: '/login', query: loginQuery })
    } else {
      // 角色权限检查
      const allowedRoles = to.meta.roles as string[] | undefined
      if (allowedRoles && allowedRoles.length > 0) {
        const currentRole = getRole()
        if (!currentRole || !allowedRoles.includes(currentRole)) {
          next('/home')
          return
        }
      }
      next()
    }
  }
})

export default router
